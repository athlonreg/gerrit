// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.reviewnotes;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.update.RetryHelper;
import com.google.gerrit.server.update.UpdateException;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.lib.ThreadSafeProgressMonitor;
import org.kohsuke.args4j.Option;

/** Export review notes for all submitted changes in all projects. */
public class ExportReviewNotes extends SshCommand {
  @Option(name = "--threads", usage = "Number of concurrent threads to run")
  private int threads = 2;

  @Inject private GitRepositoryManager gitManager;

  @Inject private CreateReviewNotes.Factory reviewNotesFactory;

  @Inject private ChangeNotes.Factory notesFactory;

  @Inject private RetryHelper retryHelper;

  private ListMultimap<Project.NameKey, ChangeNotes> changes;
  private ThreadSafeProgressMonitor monitor;

  @Override
  protected void run() throws Failure, InterruptedException {
    if (threads <= 0) {
      threads = 1;
    }

    changes = mergedChanges();

    monitor = new ThreadSafeProgressMonitor(new TextProgressMonitor(stdout));
    monitor.beginTask("Scanning merged changes", changes.size());
    monitor.startWorkers(threads);
    for (int tid = 0; tid < threads; tid++) {
      new Worker().start();
    }
    monitor.waitForCompletion();
    monitor.endTask();
  }

  private ListMultimap<Project.NameKey, ChangeNotes> mergedChanges() {
    try {
      return MultimapBuilder.hashKeys()
          .arrayListValues()
          .build(
              notesFactory.create(notes -> notes.getChange().getStatus() == Change.Status.MERGED));
    } catch (IOException e) {
      stderr.println("Cannot read changes from database " + e.getMessage());
      return ImmutableListMultimap.of();
    }
  }

  private void export(Project.NameKey project, List<ChangeNotes> notes)
      throws RestApiException, UpdateException {
    retryHelper.execute(
        updateFactory -> {
          try (Repository git = gitManager.openRepository(project)) {
            CreateReviewNotes crn = reviewNotesFactory.create(project, git);
            crn.createNotes(notes, monitor);
            crn.commitNotes();
          } catch (RepositoryNotFoundException e) {
            stderr.println("Unable to open project: " + project.get());
          }
          return null;
        },
        RetryHelper.options()
            .listener(
                new RetryListener() {
                  @Override
                  public <V> void onRetry(Attempt<V> attempt) {
                    monitor.update(-notes.size());
                  }
                })
            .build());
  }

  private Map.Entry<Project.NameKey, List<ChangeNotes>> next() {
    synchronized (changes) {
      if (changes.isEmpty()) {
        return null;
      }

      Project.NameKey name = changes.keySet().iterator().next();
      return Maps.immutableEntry(name, changes.removeAll(name));
    }
  }

  private class Worker extends Thread {
    @Override
    public void run() {
      try {
        for (; ; ) {
          Map.Entry<Project.NameKey, List<ChangeNotes>> next = next();
          if (next != null) {
            try {
              export(next.getKey(), next.getValue());
            } catch (RestApiException | UpdateException e) {
              stderr.println(e.getMessage());
            }
          } else {
            break;
          }
        }
      } finally {
        monitor.endWorker();
      }
    }
  }
}
