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

import static com.google.gerrit.reviewdb.client.CoreDownloadSchemes.HTTP;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.config.DownloadScheme;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.DownloadConfig;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.jgit.lib.Config;

public class HttpScheme extends DownloadScheme {

  private final String gitHttpUrl;
  private final String canonicalWebUrl;
  private final Provider<CurrentUser> userProvider;
  private final boolean schemeAllowed;

  @Inject
  public HttpScheme(
      @GerritServerConfig Config cfg,
      @CanonicalWebUrl @Nullable Provider<String> urlProvider,
      Provider<CurrentUser> userProvider,
      DownloadConfig downloadConfig) {
    this.gitHttpUrl = ensureSlash(cfg.getString("gerrit", null, "gitHttpUrl"));
    this.canonicalWebUrl = urlProvider != null ? urlProvider.get() : null;
    this.userProvider = userProvider;
    this.schemeAllowed = downloadConfig.getDownloadSchemes().contains(HTTP);
  }

  @Override
  public String getUrl(String project) {
    if (!isEnabled() || !userProvider.get().isIdentifiedUser()) {
      return null;
    }

    final StringBuilder r = new StringBuilder();
    if (gitHttpUrl != null) {
      r.append(gitHttpUrl);
    } else if (canonicalWebUrl != null) {
      String base = canonicalWebUrl;
      int p = base.indexOf("://");
      int s = base.indexOf('/', p + 3);
      if (s < 0) {
        s = base.length();
      }
      String host = base.substring(p + 3, s);
      r.append(base.substring(0, p + 3));
      if (userProvider.get().getUserName().isPresent()) {
        r.append(userProvider.get().getUserName().get());
        r.append("@");
      }
      r.append(host);
      r.append(base.substring(s));
      r.append("a/");
    } else {
      return null;
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
    return true;
  }

  @Override
  public boolean isAuthSupported() {
    return true;
  }

  private static String ensureSlash(String in) {
    if (in != null && !in.endsWith("/")) {
      return in + "/";
    }
    return in;
  }
}
