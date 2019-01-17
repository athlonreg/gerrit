// Copyright (C) 2015 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.download.command;

import com.google.gerrit.extensions.config.DownloadScheme;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.download.scheme.AnonymousHttpScheme;
import com.googlesource.gerrit.plugins.download.scheme.HttpScheme;
import com.googlesource.gerrit.plugins.download.scheme.SshScheme;
import java.util.Optional;
import org.eclipse.jgit.lib.Config;

public class CloneWithCommitMsgHook extends CloneCommand {
  private static final String HOOK = "hooks/commit-msg";
  private static final String TARGET = " `git rev-parse --git-dir`/";

  private final String configCommand;
  private final String extraCommand;
  private final SshScheme sshScheme;
  private final Provider<CurrentUser> userProvider;

  @Inject
  CloneWithCommitMsgHook(
      @GerritServerConfig Config config, SshScheme sshScheme, Provider<CurrentUser> userProvider) {
    this.configCommand = config.getString("gerrit", null, "installCommitMsgHookCommand");
    this.extraCommand = config.getString("gerrit", null, "installCommitExtraCommand");
    this.sshScheme = sshScheme;
    this.userProvider = userProvider;
  }

  @Override
  public String getCommand(DownloadScheme scheme, String project) {
    Optional<String> username = userProvider.get().getUserName();
    if (!username.isPresent()) {
      return null;
    }
    String projectName = getBaseName(project);

    if (configCommand != null) {
      return new StringBuilder()
          .append(super.getCommand(scheme, project))
          .append(" && (cd ")
          .append(QuoteUtil.quote((projectName)))
          .append(" && ")
          .append(configCommand)
          .append(")")
          .toString();
    }

    if (scheme instanceof SshScheme) {
      StringBuilder b =
          new StringBuilder().append(super.getCommand(scheme, project)).append(" && scp -p");

      if (sshScheme.getSshdPort() != 22) {
        b.append(" -P ").append(sshScheme.getSshdPort());
      }

      b.append(" ")
          .append(username.get())
          .append("@")
          .append(sshScheme.getSshdHost())
          .append(":")
          .append(HOOK)
          .append(" ")
          .append(QuoteUtil.quote(projectName + "/.git/hooks/"));
      if (extraCommand != null) {
        b.append(" && (cd ").append(QuoteUtil.quote(projectName)).append(" && ").append(extraCommand).append(")");
      }
      return b.toString();
    }

    if (scheme instanceof HttpScheme || scheme instanceof AnonymousHttpScheme) {
      return new StringBuilder()
          .append(super.getCommand(scheme, project))
          .append(" && (cd ")
          .append(QuoteUtil.quote(projectName))
          .append(" && mkdir -p .git/hooks")
          .append(" && curl -Lo")
          .append(TARGET)
          .append(HOOK)
          .append(" ")
          .append(getHttpHost(scheme, project))
          .append("tools/")
          .append(HOOK)
          .append("; chmod +x")
          .append(TARGET)
          .append(HOOK)
          .append(")")
          .toString();
    }
    return null;
  }

  private String getHttpHost(DownloadScheme scheme, String project) {
    String host = scheme.getUrl(project);
    host = host.substring(0, host.lastIndexOf(project));
    int auth = host.lastIndexOf("/a/");
    if (auth > -1) {
      host = host.substring(0, auth + 1);
    }
    return host;
  }

  private static String getBaseName(String project) {
    return project.substring(project.lastIndexOf('/') + 1);
  }
}
