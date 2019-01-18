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
import static org.mockito.Mockito.when;

import com.google.gerrit.extensions.events.ProjectDeletedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.googlesource.gerrit.plugins.deleteproject.Configuration;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.stream.Stream;
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
public class FilesystemDeleteHandlerTest {

  @Mock private GitRepositoryManager repoManager;
  @Mock private ProjectDeletedListener projectDeleteListener;
  @Mock private Configuration config;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private DynamicSet<ProjectDeletedListener> deletedListener;
  private FilesystemDeleteHandler fsDeleteHandler;
  private Path basePath;

  @Before
  public void setUp() throws Exception {
    basePath = tempFolder.newFolder().toPath().resolve("base");
    deletedListener = new DynamicSet<>();
    deletedListener.add("", projectDeleteListener);
  }

  @Test
  public void shouldDeleteRepository() throws Exception {
    String repoName = "testRepo";
    Repository repository = createRepository(repoName);
    Project.NameKey nameKey = new Project.NameKey(repoName);
    Project project = new Project(nameKey);
    when(repoManager.openRepository(nameKey)).thenReturn(repository);
    when(config.shouldArchiveDeletedRepos()).thenReturn(false);
    fsDeleteHandler = new FilesystemDeleteHandler(repoManager, deletedListener, config);
    fsDeleteHandler.delete(project, false);
    assertThat(repository.getDirectory().exists()).isFalse();
  }

  @Test
  public void shouldDeleteEmptyParentFolders() throws Exception {
    String repoName = "a/b/c";
    Repository repository = createRepository(repoName);
    Project.NameKey nameKey = new Project.NameKey(repoName);
    Project project = new Project(nameKey);
    when(repoManager.openRepository(nameKey)).thenReturn(repository);
    fsDeleteHandler = new FilesystemDeleteHandler(repoManager, deletedListener, config);
    fsDeleteHandler.delete(project, false);
    assertThat(repository.getDirectory().exists()).isFalse();
  }

  @Test
  public void shouldKeepCommonFolders() throws Exception {
    String repoToDeleteName = "a/b/c/d";
    Repository repoToDelete = createRepository(repoToDeleteName);

    String repoToKeepName = "a/b/e";
    Repository repoToKeep = createRepository(repoToKeepName);

    Project.NameKey nameKey = new Project.NameKey(repoToDeleteName);
    Project project = new Project(nameKey);
    when(repoManager.openRepository(nameKey)).thenReturn(repoToDelete);
    fsDeleteHandler = new FilesystemDeleteHandler(repoManager, deletedListener, config);
    fsDeleteHandler.delete(project, false);
    assertThat(repoToDelete.getDirectory().exists()).isFalse();
    assertThat(repoToKeep.getDirectory().exists()).isTrue();
  }

  @Test
  public void shouldPreserveRepository() throws Exception {
    String repoName = "preservedRepo";
    Repository repository = createRepository(repoName);
    Project.NameKey nameKey = new Project.NameKey(repoName);
    Project project = new Project(nameKey);
    when(repoManager.openRepository(nameKey)).thenReturn(repository);
    fsDeleteHandler = new FilesystemDeleteHandler(repoManager, deletedListener, config);
    fsDeleteHandler.delete(project, true);
    assertThat(repository.getDirectory().exists()).isTrue();
  }

  private FileRepository createRepository(String repoName) throws IOException {
    Path repoPath = Files.createDirectories(basePath.resolve(repoName));
    Repository repository = new FileRepository(repoPath.toFile());
    repository.create(true);
    return (FileRepository) repository;
  }

  @Test
  public void archiveRepository() throws Exception {
    String repoName = "parent_project/p3";
    Repository repository = createRepository(repoName);
    Path archiveFolder = basePath.resolve("test_archive");
    when(config.shouldArchiveDeletedRepos()).thenReturn(true);
    when(config.getArchiveFolder()).thenReturn(archiveFolder);
    Project.NameKey nameKey = new Project.NameKey(repoName);
    Project project = new Project(nameKey);
    when(repoManager.openRepository(nameKey)).thenReturn(repository);
    fsDeleteHandler = new FilesystemDeleteHandler(repoManager, deletedListener, config);
    fsDeleteHandler.delete(project, false);
    assertThat(repository.getDirectory().exists()).isFalse();
    String patternToVerify = archiveFolder.resolve(repoName).toString() + "*%archived%.git";
    assertThat(pathExistsWithPattern(archiveFolder, patternToVerify)).isTrue();
  }

  private boolean pathExistsWithPattern(Path archiveFolder, String patternToVerify)
      throws IOException {
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + patternToVerify);
    try (Stream<Path> stream = Files.walk(archiveFolder)) {
      return stream.anyMatch(matcher::matches);
    }
  }
}
