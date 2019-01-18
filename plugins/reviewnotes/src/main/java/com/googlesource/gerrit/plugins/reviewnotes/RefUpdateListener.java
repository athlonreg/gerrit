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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.ProjectRunnable;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.update.RetryHelper;
import com.google.gerrit.server.update.UpdateException;
import com.google.inject.Inject;
import java.util.concurrent.Future;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

class RefUpdateListener implements GitReferenceUpdatedListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final CreateReviewNotes.Factory reviewNotesFactory;
  private final GitRepositoryManager repoManager;
  private final WorkQueue workQueue;
  private final RetryHelper retryHelper;
  private final boolean async;

  @Inject
  RefUpdateListener(
      CreateReviewNotes.Factory reviewNotesFactory,
      GitRepositoryManager repoManager,
      WorkQueue workQueue,
      RetryHelper retryHelper,
      @GerritServerConfig Config config) {
    this.reviewNotesFactory = reviewNotesFactory;
    this.repoManager = repoManager;
    this.workQueue = workQueue;
    this.retryHelper = retryHelper;
    this.async = config.getBoolean("reviewnotes", null, "async", false);
  }

  @Override
  public void onGitReferenceUpdated(Event e) {
    Runnable task =
        new ProjectRunnable() {
          @Override
          public void run() {
            createReviewNotes(e);
          }

          @Override
          public Project.NameKey getProjectNameKey() {
            return new Project.NameKey(e.getProjectName());
          }

          @Override
          public String getRemoteName() {
            return null;
          }

          @Override
          public boolean hasCustomizedPrint() {
            return true;
          }

          @Override
          public String toString() {
            return "create-review-notes";
          }
        };
    if (async) {
      @SuppressWarnings("unused") // No assurance this completes.
      Future<?> possiblyIgnoredError = workQueue.getDefaultQueue().submit(task);
    } else {
      task.run();
    }
  }

  private void createReviewNotes(Event e) {
    if (!e.getRefName().startsWith(RefNames.REFS_HEADS)) {
      return;
    }
    try {
      retryHelper.execute(
          updateFactory -> {
            Project.NameKey projectName = new Project.NameKey(e.getProjectName());
            try (Repository git = repoManager.openRepository(projectName)) {
              CreateReviewNotes crn = reviewNotesFactory.create(projectName, git);
              crn.createNotes(
                  e.getRefName(),
                  ObjectId.fromString(e.getOldObjectId()),
                  ObjectId.fromString(e.getNewObjectId()),
                  null);
              crn.commitNotes();
            }
            return null;
          });
    } catch (RestApiException | UpdateException x) {
      logger.atSevere().withCause(x).log(x.getMessage());
    }
  }
}
