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

import static com.google.gerrit.reviewdb.client.CoreDownloadSchemes.SSH;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.config.DownloadScheme;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.DownloadConfig;
import com.google.gerrit.server.ssh.SshAdvertisedAddresses;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class SshScheme extends DownloadScheme {
  private final String sshdAddress;
  private final String sshdHost;
  private final int sshdPort;
  private final Provider<CurrentUser> userProvider;
  private final boolean schemeAllowed;

  @Inject
  SshScheme(
      @SshAdvertisedAddresses List<String> sshAddresses,
      @CanonicalWebUrl @Nullable Provider<String> urlProvider,
      Provider<CurrentUser> userProvider,
      DownloadConfig downloadConfig) {
    String sshAddr = !sshAddresses.isEmpty() ? sshAddresses.get(0) : null;
    if (sshAddr != null
        && (sshAddr.startsWith("*:") || "".equals(sshAddr))
        && urlProvider != null) {
      try {
        sshAddr = (new URL(urlProvider.get())).getHost() + sshAddr.substring(1);
      } catch (MalformedURLException e) {
        // ignore, then this scheme will be disabled
      }
    }

    int port = 29418;
    String host = sshAddr;
    if (sshAddr != null) {
      int p = sshAddr.indexOf(":");
      if (p > 0) {
        host = sshAddr.substring(0, p);
        try {
          port = Integer.parseInt(sshAddr.substring(p + 1));
        } catch (NumberFormatException e) {
          // use default port
        }
        if (port == 22) {
          sshAddr = host;
        }
      } else {
        host = sshAddr;
      }
    }
    this.sshdAddress = sshAddr;
    this.sshdHost = host;
    this.sshdPort = port;

    this.userProvider = userProvider;
    this.schemeAllowed = downloadConfig.getDownloadSchemes().contains(SSH);
  }

  @Override
  public String getUrl(String project) {
    if (!isEnabled() || !userProvider.get().isIdentifiedUser()) {
      return null;
    }

    Optional<String> username = userProvider.get().getUserName();
    if (!username.isPresent()) {
      return null;
    }

    StringBuilder r = new StringBuilder();
    r.append("ssh://");
    r.append(username.get());
    r.append("@");
    r.append(ensureSlash(sshdAddress));
    r.append(project);
    return r.toString();
  }

  @Override
  public boolean isEnabled() {
    return schemeAllowed && sshdAddress != null;
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

  public String getSshdHost() {
    return sshdHost;
  }

  public int getSshdPort() {
    return sshdPort;
  }
}
