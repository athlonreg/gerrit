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

package com.googlesource.gerrit.plugins.deleteproject;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllProjectsNameProvider;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProtectedProjects {
  private final AllProjectsName allProjectsName;
  private final AllUsersName allUsersName;
  private final Configuration config;

  @Inject
  ProtectedProjects(
      AllProjectsNameProvider allProjectsNameProvider,
      AllUsersNameProvider allUsersNameProvider,
      Configuration config) {
    this.allProjectsName = allProjectsNameProvider.get();
    this.allUsersName = allUsersNameProvider.get();
    this.config = config;
  }

  private boolean isDefaultProtected(Project.NameKey name) {
    return name.equals(allProjectsName) || name.equals(allUsersName);
  }

  private boolean isCustomProtected(Project.NameKey name) {
    return config.protectedProjects().stream().anyMatch(p -> p.matcher(name.get()).matches());
  }

  @VisibleForTesting
  public boolean isProtected(Project.NameKey name) {
    return isDefaultProtected(name) || isCustomProtected(name);
  }

  public boolean isProtected(ProjectResource rsrc) {
    return isProtected(rsrc.getNameKey());
  }

  public void assertIsNotProtected(ProjectResource rsrc) throws CannotDeleteProjectException {
    if (isProtected(rsrc)) {
      throw new CannotDeleteProjectException(
          "Cannot delete project because it is protected against deletion");
    }
  }
}
