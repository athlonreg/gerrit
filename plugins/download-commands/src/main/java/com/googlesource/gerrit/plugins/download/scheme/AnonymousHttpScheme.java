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

import static com.google.gerrit.reviewdb.client.CoreDownloadSchemes.ANON_HTTP;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.config.DownloadScheme;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.DownloadConfig;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.jgit.lib.Config;

public class AnonymousHttpScheme extends DownloadScheme {

  private final String gitHttpUrl;
  private final String canonicalWebUrl;
  private final boolean schemeAllowed;

  @Inject
  public AnonymousHttpScheme(
      @GerritServerConfig Config cfg,
      @CanonicalWebUrl @Nullable Provider<String> provider,
      DownloadConfig downloadConfig) {
    this.gitHttpUrl = ensureSlash(cfg.getString("gerrit", null, "gitHttpUrl"));
    this.canonicalWebUrl = provider != null ? provider.get() : null;
    this.schemeAllowed = downloadConfig.getDownloadSchemes().contains(ANON_HTTP);
  }

  @Override
  public String getUrl(String project) {
    if (!isEnabled()) {
      return null;
    }

    final StringBuilder r = new StringBuilder();
    if (gitHttpUrl != null) {
      r.append(gitHttpUrl);
    } else if (canonicalWebUrl != null) {
      r.append(canonicalWebUrl);
    } else {
      throw new IllegalStateException("No HTTP URL");
    }
    r.append(project);
    return r.toString();
  }

  @Override
  public boolean isEnabled() {
    return schemeAllowed && (gitHttpUrl != null || canonicalWebUrl != null);
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
