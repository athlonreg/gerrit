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

package com.googlesource.gerrit.plugins.download.scheme;

import static com.google.gerrit.reviewdb.client.CoreDownloadSchemes.ANON_GIT;

import com.google.gerrit.extensions.config.DownloadScheme;
import com.google.gerrit.server.config.DownloadConfig;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import org.eclipse.jgit.lib.Config;

public class GitScheme extends DownloadScheme {

  private final String gitDaemonUrl;
  private final boolean schemeAllowed;

  @Inject
  public GitScheme(@GerritServerConfig Config cfg, DownloadConfig downloadConfig) {
    this.gitDaemonUrl = ensureSlash(cfg.getString("gerrit", null, "canonicalGitUrl"));
    this.schemeAllowed = downloadConfig.getDownloadSchemes().contains(ANON_GIT);
  }

  @Override
  public String getUrl(String project) {
    StringBuilder r = new StringBuilder();
    r.append(gitDaemonUrl);
    r.append(project);
    return r.toString();
  }

  @Override
  public boolean isEnabled() {
    return schemeAllowed && gitDaemonUrl != null;
  }

  @Override
  public boolean isAuthRequired() {
    return false;
  }

  @Override
  public boolean isAuthSupported() {
    return false;
  }

  private static String ensureSlash(String in) {
    if (in != null && !in.endsWith("/")) {
      return in + "/";
    }
    return in;
  }
}
