// Copyright (C) 2012 The Android Open Source Project
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

import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import java.io.IOException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@CommandMetaData(name = "delete", description = "Delete specific project")
public final class DeleteCommand extends SshCommand {
  private static final String FORCE_DELETE =
      "%s - To really delete '%s', re-run with the --force flag.";
  private static final String REALLY_DELETE =
      "Really delete '%s'?\n"
          + "This is an operation which permanently deletes data. This cannot be undone!\n"
          + "If you are sure you wish to delete this project, re-run with the "
          + "--yes-really-delete flag.\n";

  @Argument(index = 0, required = true, metaVar = "NAME", usage = "project to delete")
  private ProjectState projectState;

  @Option(name = "--yes-really-delete", usage = "confirmation to delete the project")
  private boolean yesReallyDelete;

  @Option(name = "--force", usage = "delete the project even if it has open changes")
  private boolean force = false;

  @Option(name = "--preserve-git-repository", usage = "don't delete git repository directory")
  private boolean preserveGitRepository = false;

  private final Configuration cfg;
  private final DeleteProject deleteProject;
  private final DeletePreconditions preConditions;

  @Inject
  protected DeleteCommand(
      Configuration cfg, DeleteProject deleteProject, DeletePreconditions preConditions) {
    this.cfg = cfg;
    this.deleteProject = deleteProject;
    this.preConditions = preConditions;
  }

  @Override
  public void run() throws Failure {
    try {
      if (preserveGitRepository && !cfg.enablePreserveOption()) {
        throw new UnloggedFailure(
            "Given the enablePreserveOption is configured to be false, "
                + "the --preserve-git-repository option is not allowed.\n"
                + "Please remove this option and retry.");
      }

      DeleteProject.Input input = new DeleteProject.Input();
      input.force = force;
      input.preserve = preserveGitRepository;

      ProjectResource rsrc = new ProjectResource(projectState, user);
      preConditions.assertDeletePermission(rsrc);

      if (!yesReallyDelete) {
        throw new UnloggedFailure(String.format(REALLY_DELETE, rsrc.getName()));
      }

      if (!force) {
        try {
          preConditions.assertHasOpenChanges(rsrc.getNameKey(), false);
        } catch (CannotDeleteProjectException e) {
          throw new UnloggedFailure(String.format(FORCE_DELETE, e.getMessage(), rsrc.getName()));
        }
      }

      preConditions.assertCanBeDeleted(rsrc, input);
      deleteProject.doDelete(rsrc, input);
    } catch (RestApiException | IOException e) {
      throw die(e);
    }
  }
}
