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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.server.config.RepositoryConfig;
import com.google.gerrit.server.config.SitePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeleteTrashFoldersTest {

  @Mock private RepositoryConfig repositoryCfg;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private Path basePath;
  private DeleteTrashFolders trashFolders;

  @Before
  public void setUp() throws Exception {
    SitePaths sitePaths = new SitePaths(tempFolder.newFolder("gerrit_site").toPath());
    basePath = sitePaths.resolve("base");
    Config cfg = new Config();
    cfg.setString("gerrit", null, "basePath", basePath.toString());
    when(repositoryCfg.getAllBasePaths()).thenReturn(ImmutableList.of());
    trashFolders = new DeleteTrashFolders(sitePaths, cfg, repositoryCfg);
  }

  @Test
  public void testStart() throws Exception {
    FileRepository repoToDelete = createRepository("repo.1234567890123.deleted");
    FileRepository repoToKeep = createRepository("anotherRepo.git");
    trashFolders.start();
    trashFolders.getWorkerThread().join();
    assertThat(repoToDelete.getDirectory().exists()).isFalse();
    assertThat(repoToKeep.getDirectory().exists()).isTrue();
  }

  private FileRepository createRepository(String repoName) throws IOException {
    Path repoPath = Files.createDirectories(basePath.resolve(repoName));
    Repository repository = new FileRepository(repoPath.toFile());
    repository.create(true);
    return (FileRepository) repository;
  }
}
