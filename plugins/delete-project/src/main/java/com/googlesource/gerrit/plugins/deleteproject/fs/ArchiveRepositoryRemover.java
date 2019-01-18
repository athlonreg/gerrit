// Copyright (C) 2018 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.deleteproject.fs;

import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.io.MoreFiles;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.deleteproject.Configuration;
import com.googlesource.gerrit.plugins.deleteproject.TimeMachine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ArchiveRepositoryRemover implements LifecycleListener {

  private final WorkQueue queue;
  private final Provider<RepositoryCleanupTask> repositoryCleanupTaskProvider;
  private ScheduledFuture<?> scheduledCleanupTask;

  @Inject
  ArchiveRepositoryRemover(
      WorkQueue queue, Provider<RepositoryCleanupTask> repositoryCleanupTaskProvider) {
    this.queue = queue;
    this.repositoryCleanupTaskProvider = repositoryCleanupTaskProvider;
  }

  @Override
  public void start() {
    scheduledCleanupTask =
        queue
            .getDefaultQueue()
            .scheduleAtFixedRate(
                repositoryCleanupTaskProvider.get(),
                SECONDS.toMillis(1),
                TimeUnit.DAYS.toMillis(1),
                MILLISECONDS);
  }

  @Override
  public void stop() {
    if (scheduledCleanupTask != null) {
      scheduledCleanupTask.cancel(true);
      scheduledCleanupTask = null;
    }
  }
}

class RepositoryCleanupTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(RepositoryCleanupTask.class);

  private final Configuration config;
  private final String pluginName;

  @Inject
  RepositoryCleanupTask(Configuration config, @PluginName String pluginName) {
    this.config = config;
    this.pluginName = pluginName;
  }

  @Override
  public void run() {
    logger.info("Cleaning up expired git repositories...");
    cleanUpOverdueRepositories();
    logger.info("Cleaning up expired git repositories... Done");
  }

  @Override
  public String toString() {
    return String.format(
        "[%s]: Clean up expired git repositories from the archive [%s]",
        pluginName, config.getArchiveFolder());
  }

  private void cleanUpOverdueRepositories() {
    for (Path path : listOverdueFiles(config.getArchiveDuration())) {
      try {
        MoreFiles.deleteRecursively(path, ALLOW_INSECURE);
      } catch (IOException e) {
        logger.warn("Error trying to clean the archived git repository: {}", path, e);
      }
    }
  }

  private List<Path> listOverdueFiles(long duration) {
    List<Path> files = new ArrayList<>();
    File targetDir = config.getArchiveFolder().toFile();
    FileTime nowTime = FileTime.fromMillis(TimeMachine.now().toEpochMilli());

    for (File repo : targetDir.listFiles()) {
      try {
        FileTime lastModifiedTime = Files.getLastModifiedTime(repo.toPath());
        FileTime expires = FileTime.fromMillis(lastModifiedTime.toMillis() + duration);
        if (nowTime.compareTo(expires) > 0) {
          files.add(repo.toPath());
        }
      } catch (IOException e) {
        logger.warn("Error trying to get last modified time for file: {} ", repo.toPath(), e);
      }
    }
    return files;
  }
}
