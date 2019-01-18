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

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Joiner;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.deleteproject.Configuration;
import com.googlesource.gerrit.plugins.deleteproject.TimeMachine;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveRepositoryRemoverTest {

  private static final long ARCHIVE_DURATION = 1;
  private static final long CLEANUP_INTERVAL = TimeUnit.DAYS.toMillis(1);
  private static final int NUMBER_OF_REPOS = 10;
  private static final String PLUGIN_NAME = "delete-project";

  @Mock private ScheduledExecutorService executorMock;
  @Mock private ScheduledFuture<?> scheduledFutureMock;
  @Mock private WorkQueue workQueueMock;
  @Mock private Provider<RepositoryCleanupTask> cleanupTaskProviderMock;
  @Mock private Configuration configMock;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private ArchiveRepositoryRemover remover;
  private Path archiveRepo;

  @Before
  public void setUp() throws Exception {
    when(cleanupTaskProviderMock.get()).thenReturn(new RepositoryCleanupTask(null, null));
    when(workQueueMock.getDefaultQueue()).thenReturn(executorMock);
    doReturn(scheduledFutureMock)
        .when(executorMock)
        .scheduleAtFixedRate(
            isA(RepositoryCleanupTask.class), anyLong(), anyLong(), isA(TimeUnit.class));
    remover = new ArchiveRepositoryRemover(workQueueMock, cleanupTaskProviderMock);
    archiveRepo = tempFolder.newFolder("archive").toPath();
    when(configMock.getArchiveFolder()).thenReturn(archiveRepo);
    when(configMock.getArchiveDuration()).thenReturn(ARCHIVE_DURATION);
  }

  @Test
  public void cleanUpOverdueRepositoriesTest() throws IOException {
    setupArchiveFolder();
    try {
      TimeMachine.useFixedClockAt(
          Instant.ofEpochMilli(Files.getLastModifiedTime(archiveRepo).toMillis())
              .plusMillis(TimeUnit.DAYS.toMillis(ARCHIVE_DURATION) + 10));

      RepositoryCleanupTask task = new RepositoryCleanupTask(configMock, PLUGIN_NAME);
      task.run();
      assertThat(task.toString())
          .isEqualTo(
              String.format(
                  "[%s]: Clean up expired git repositories from the archive [%s]",
                  PLUGIN_NAME, archiveRepo));
      assertDirectoryContents(archiveRepo, true);
    } finally {
      TimeMachine.useSystemDefaultZoneClock();
    }
  }

  @Test
  public void testRepositoryCleanupTaskIsScheduledOnStart() {
    remover.start();
    verify(executorMock, times(1))
        .scheduleAtFixedRate(
            isA(RepositoryCleanupTask.class),
            eq(SECONDS.toMillis(1)),
            eq(CLEANUP_INTERVAL),
            eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void testRepositoryCleanupTaskIsCancelledOnStop() {
    remover.start();
    remover.stop();
    verify(scheduledFutureMock, times(1)).cancel(true);
  }

  private void setupArchiveFolder() throws IOException {
    for (int i = 0; i < NUMBER_OF_REPOS; i++) {
      createRepository("Repo_" + i);
    }
    assertDirectoryContents(archiveRepo, false);
  }

  private FileRepository createRepository(String repoName) throws IOException {
    Path repoPath = Files.createDirectories(archiveRepo.resolve(repoName));
    Repository repository = new FileRepository(repoPath.toFile());
    repository.create(true);
    return (FileRepository) repository;
  }

  private void assertDirectoryContents(Path dir, boolean expectEmpty) throws IOException {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
      List<Path> paths = StreamSupport.stream(dirStream.spliterator(), false).collect(toList());
      if (expectEmpty && !paths.isEmpty()) {
        fail(
            String.format(
                "Expected dir [%s] to be empty but it contains: %s",
                dir, Joiner.on(", ").join(paths)));
      } else if (!expectEmpty && paths.isEmpty()) {
        fail(String.format("Expected dir [%s] to be non-empty but it is empty", dir));
      }
    }
  }
}
