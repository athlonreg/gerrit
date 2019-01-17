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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.client.GeneralPreferencesInfo;
import com.google.gerrit.extensions.config.DownloadCommand;
import com.google.gerrit.extensions.config.DownloadScheme;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.config.DownloadConfig;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.googlesource.gerrit.plugins.download.scheme.RepoScheme;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

abstract class GitDownloadCommand extends DownloadCommand {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DOWNLOAD = "download";
  private static final String UPLOADPACK = "uploadpack";
  private static final String KEY_ALLOW_TIP_SHA1_IN_WANT = "allowTipSHA1InWant";
  private static final String KEY_ALLOW_REACHABLE_SHA1_IN_WANT = "allowReachableSHA1InWant";
  private static final String KEY_CHECK_FOR_HIDDEN_CHANGE_REFS = "checkForHiddenChangeRefs";
  private static final String KEY_HIDE_REFS = "hideRefs";

  private final boolean commandAllowed;
  private final GitRepositoryManager repoManager;
  private final boolean checkForHiddenChangeRefs;

  GitDownloadCommand(
      @GerritServerConfig Config cfg,
      DownloadConfig downloadConfig,
      GeneralPreferencesInfo.DownloadCommand cmd,
      GitRepositoryManager repoManager) {
    this.commandAllowed = downloadConfig.getDownloadCommands().contains(cmd);
    this.repoManager = repoManager;
    this.checkForHiddenChangeRefs =
        cfg.getBoolean(DOWNLOAD, KEY_CHECK_FOR_HIDDEN_CHANGE_REFS, false);
  }

  @Override
  public final String getCommand(DownloadScheme scheme, String project, String ref) {
    if (commandAllowed && isRecognizedScheme(scheme)) {
      String url = scheme.getUrl(project);
      if (url != null && isValidUrl(url)) {
        if (checkForHiddenChangeRefs) {
          ref = resolveRef(project, ref);
        }
        if (ref != null) {
          return getCommand(url, ref);
        }
      }
    }
    return null;
  }

  private static boolean isRecognizedScheme(DownloadScheme scheme) {
    return !(scheme instanceof RepoScheme);
  }

  private static boolean isValidUrl(String url) {
    try {
      new URIish(url);
      return true;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  private String resolveRef(String project, String ref) {
    if (project.startsWith("$") || ref.startsWith("$")) {
      // No real value but placeholders are being used.
      return ref;
    }

    try (Repository repo = repoManager.openRepository(new Project.NameKey(project))) {
      Config cfg = repo.getConfig();
      boolean allowSha1InWant =
          cfg.getBoolean(UPLOADPACK, KEY_ALLOW_TIP_SHA1_IN_WANT, false)
              || cfg.getBoolean(UPLOADPACK, KEY_ALLOW_REACHABLE_SHA1_IN_WANT, false);
      if (allowSha1InWant
          && Arrays.asList(cfg.getStringList(UPLOADPACK, null, KEY_HIDE_REFS))
              .contains(RefNames.REFS_CHANGES)) {
        ObjectId id = repo.resolve(ref);
        if (id != null) {
          return id.name();
        }
        logger.atSevere().log("Cannot resolve ref %s in project %s", ref, project);
        return null;
      }
      return ref;
    } catch (RepositoryNotFoundException e) {
      logger.atSevere().withCause(e).log("Missing project: %s", project);
      return null;
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to lookup project %s from cache", project);
      return null;
    }
  }

  abstract String getCommand(String url, String ref);
}
