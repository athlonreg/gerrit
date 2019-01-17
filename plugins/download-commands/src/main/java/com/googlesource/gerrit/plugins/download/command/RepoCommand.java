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

package com.googlesource.gerrit.plugins.download.command;

import com.google.gerrit.extensions.config.DownloadCommand;
import com.google.gerrit.extensions.config.DownloadScheme;
import com.google.gerrit.reviewdb.client.RefNames;
import com.googlesource.gerrit.plugins.download.scheme.RepoScheme;

public class RepoCommand extends DownloadCommand {
  @Override
  public String getCommand(DownloadScheme scheme, String project, String ref) {
    if (scheme instanceof RepoScheme) {
      String id = trim(ref);
      if (id != null) {
        return "repo download " + QuoteUtil.quote(scheme.getUrl(project)) + " " + id;
      }
    }
    return null;
  }

  private static String trim(String ref) {
    if (ref.startsWith(RefNames.REFS_CHANGES)) {
      int s1 = ref.lastIndexOf('/');
      if (s1 > 0) {
        int s2 = ref.lastIndexOf('/', s1 - 1);
        if (s2 > 0) {
          return ref.substring(s2 + 1);
        }
      }
    }
    return null;
  }
}
