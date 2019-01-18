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

package com.googlesource.gerrit.plugins.deleteproject;

import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.deleteproject.DeleteProject.Input;
import com.googlesource.gerrit.plugins.deleteproject.cache.CacheDeleteHandler;
import com.googlesource.gerrit.plugins.deleteproject.fs.FilesystemDeleteHandler;
import java.io.IOException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

@Singleton
class DeleteProject implements RestModifyView<ProjectResource, Input> {
  static class Input {
    boolean preserve;
    boolean force;
  }

  protected final DeletePreconditions preConditions;

  private final FilesystemDeleteHandler fsHandler;
  private final CacheDeleteHandler cacheHandler;
  private final Provider<CurrentUser> userProvider;
  private final DeleteLog deleteLog;
  private final Configuration cfg;
  private final HideProject hideProject;

  @Inject
  DeleteProject(
      FilesystemDeleteHandler fsHandler,
      CacheDeleteHandler cacheHandler,
      Provider<CurrentUser> userProvider,
      DeleteLog deleteLog,
      DeletePreconditions preConditions,
      Configuration cfg,
      HideProject hideProject) {
    this.fsHandler = fsHandler;
    this.cacheHandler = cacheHandler;
    this.userProvider = userProvider;
    this.deleteLog = deleteLog;
    this.preConditions = preConditions;
    this.cfg = cfg;
    this.hideProject = hideProject;
  }

  @Override
  public Object apply(ProjectResource rsrc, Input input) throws IOException, RestApiException {
    preConditions.assertDeletePermission(rsrc);
    preConditions.assertCanBeDeleted(rsrc, input);

    doDelete(rsrc, input);
    return Response.none();
  }

  public void doDelete(ProjectResource rsrc, Input input) throws IOException, RestApiException {
    Project project = rsrc.getProjectState().getProject();
    boolean preserve = input != null && input.preserve;
    Exception ex = null;
    try {
      if (!preserve || !cfg.projectOnPreserveHidden()) {
        try {
          fsHandler.delete(project, preserve);
        } catch (RepositoryNotFoundException e) {
          throw new ResourceNotFoundException();
        }
        cacheHandler.delete(project);
      } else {
        hideProject.apply(rsrc);
      }
    } catch (Exception e) {
      ex = e;
      throw e;
    } finally {
      deleteLog.onDelete((IdentifiedUser) userProvider.get(), project.getNameKey(), input, ex);
    }
  }
}
