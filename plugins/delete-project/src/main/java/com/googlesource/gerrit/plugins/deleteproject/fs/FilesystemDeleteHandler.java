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

package com.googlesource.gerrit.plugins.deleteproject.fs;

import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;

import com.google.common.io.MoreFiles;
import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.deleteproject.Configuration;
import com.googlesource.gerrit.plugins.deleteproject.TimeMachine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilesystemDeleteHandler {
  private static final Logger log = LoggerFactory.getLogger(FilesystemDeleteHandler.class);
  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("YYYYMMddHHmmss").withZone(ZoneId.of("UTC"));

  private final GitRepositoryManager repoManager;
  private final DynamicSet<ProjectDeletedListener> deletedListeners;
  private final Configuration config;

  @Inject
  public FilesystemDeleteHandler(
      GitRepositoryManager repoManager,
      DynamicSet<ProjectDeletedListener> deletedListeners,
      Configuration config) {
    this.repoManager = repoManager;
    this.deletedListeners = deletedListeners;
    this.config = config;
  }

  public void delete(Project project, boolean preserveGitRepository)
      throws IOException, RepositoryNotFoundException {
    // Remove from the jgit cache
    Repository repository = repoManager.openRepository(project.getNameKey());
    cleanCache(repository);
    if (!preserveGitRepository) {
      Path repoPath = repository.getDirectory().toPath();
      String projectName = project.getNameKey().get();
      if (config.shouldArchiveDeletedRepos()) {
        archiveGitRepository(projectName, repoPath);
      } else {
        deleteGitRepository(projectName, repoPath);
      }
    }
  }

  private void cleanCache(Repository repository) {
    repository.close();
    RepositoryCache.close(repository);
  }

  private void archiveGitRepository(String projectName, Path repoPath) throws IOException {
    Path basePath = getBasePath(repoPath, projectName);
    Path renamedProjectDir = renameRepository(repoPath, basePath, projectName, "archived");
    try {
      Path archive = getArchivePath(renamedProjectDir, basePath);
      FileUtils.copyDirectory(renamedProjectDir.toFile(), archive.toFile());
      MoreFiles.deleteRecursively(renamedProjectDir, ALLOW_INSECURE);
    } catch (IOException e) {
      log.warn("Error trying to archive {}", renamedProjectDir, e);
    }
  }

  private Path getArchivePath(Path renamedProjectDir, Path basePath) {
    Path configArchiveRepo = config.getArchiveFolder().toAbsolutePath();
    Path relativePath = basePath.relativize(renamedProjectDir);
    return configArchiveRepo.resolve(relativePath);
  }

  private void deleteGitRepository(String projectName, Path repoPath) throws IOException {
    // Delete the repository from disk
    Path basePath = getBasePath(repoPath, projectName);
    Path trash = renameRepository(repoPath, basePath, projectName, "deleted");
    try {
      MoreFiles.deleteRecursively(trash, ALLOW_INSECURE);
      recursivelyDeleteEmptyParents(repoPath.toFile().getParentFile(), basePath.toFile());
    } catch (IOException e) {
      // Only log if delete failed - repo already moved to trash.
      log.warn("Error trying to delete {} or its parents", trash, e);
    } finally {
      sendProjectDeletedEvent(projectName);
    }
  }

  private Path getBasePath(Path repo, String projectName) {
    Path projectPath = Paths.get(projectName);
    return repo.getRoot()
        .resolve(repo.subpath(0, repo.getNameCount() - projectPath.getNameCount()));
  }

  private Path renameRepository(Path directory, Path basePath, String projectName, String option)
      throws IOException {
    Path newRepo =
        basePath.resolve(
            projectName + "." + FORMAT.format(TimeMachine.now()) + ".%" + option + "%.git");
    return Files.move(directory, newRepo, StandardCopyOption.ATOMIC_MOVE);
  }

  /**
   * Recursively delete the specified file and its parent files until we hit the file {@code Until}
   * or the parent file is populated. This is used when we have a tree structure such as a/b/c/d.git
   * and a/b/e.git - if we delete a/b/c/d.git, we no longer need a/b/c/.
   */
  private void recursivelyDeleteEmptyParents(File file, File until) throws IOException {
    if (file.equals(until)) {
      return;
    }
    if (file.listFiles().length == 0) {
      File parent = file.getParentFile();
      Files.delete(file.toPath());
      recursivelyDeleteEmptyParents(parent, until);
    }
  }

  private void sendProjectDeletedEvent(String projectName) {
    if (!deletedListeners.iterator().hasNext()) {
      return;
    }
    ProjectDeletedListener.Event event =
        new ProjectDeletedListener.Event() {
          @Override
          public String getProjectName() {
            return projectName;
          }

          @Override
          public NotifyHandling getNotify() {
            return NotifyHandling.NONE;
          }
        };
    for (ProjectDeletedListener l : deletedListeners) {
      try {
        l.onProjectDeleted(event);
      } catch (RuntimeException e) {
        log.warn("Failure in ProjectDeletedListener", e);
      }
    }
  }
}
