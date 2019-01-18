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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.ConfigUtil;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Configuration {
  private static final Logger log = LoggerFactory.getLogger(Configuration.class);
  private static final String DELETED_PROJECTS_PARENT = "Deleted-Projects";
  private static final long DEFAULT_ARCHIVE_DURATION_DAYS = 180;

  private final boolean allowDeletionWithTags;
  private final boolean archiveDeletedRepos;
  private final boolean enablePreserveOption;
  private final boolean hideProjectOnPreserve;
  private final long deleteArchivedReposAfter;
  private final String deletedProjectsParent;
  private final Path archiveFolder;
  private final List<Pattern> protectedProjects;
  private final PluginConfig cfg;

  private File pluginData;

  @Inject
  public Configuration(
      PluginConfigFactory pluginConfigFactory,
      @PluginName String pluginName,
      @PluginData File pluginData) {
    this.cfg = pluginConfigFactory.getFromGerritConfig(pluginName);
    this.pluginData = pluginData;
    this.allowDeletionWithTags = cfg.getBoolean("allowDeletionOfReposWithTags", true);
    this.hideProjectOnPreserve = cfg.getBoolean("hideProjectOnPreserve", false);
    this.deletedProjectsParent = cfg.getString("parentForDeletedProjects", DELETED_PROJECTS_PARENT);
    this.archiveDeletedRepos = cfg.getBoolean("archiveDeletedRepos", false);
    this.enablePreserveOption = cfg.getBoolean("enablePreserveOption", true);
    this.archiveFolder =
        getArchiveFolderFromConfig(cfg.getString("archiveFolder", pluginData.toString()));
    this.deleteArchivedReposAfter =
        getArchiveDurationFromConfig(
            Strings.nullToEmpty(cfg.getString("deleteArchivedReposAfter")));
    this.protectedProjects =
        Arrays.asList(cfg.getStringList("protectedProject"))
            .stream()
            .map(Pattern::compile)
            .collect(toList());
  }

  public boolean deletionWithTagsAllowed() {
    return allowDeletionWithTags;
  }

  public boolean projectOnPreserveHidden() {
    return hideProjectOnPreserve;
  }

  public String getDeletedProjectsParent() {
    return deletedProjectsParent;
  }

  public List<Pattern> protectedProjects() {
    return protectedProjects;
  }

  public boolean shouldArchiveDeletedRepos() {
    return archiveDeletedRepos;
  }

  public Path getArchiveFolder() {
    return archiveFolder;
  }

  public long getArchiveDuration() {
    return deleteArchivedReposAfter;
  }

  public boolean enablePreserveOption() {
    return enablePreserveOption;
  }

  private Path getArchiveFolderFromConfig(String configValue) {
    try {
      return Files.createDirectories(Paths.get(configValue));
    } catch (Exception e) {
      log.warn(
          "Failed to create folder {}: {}; using default path: {}",
          configValue,
          e.getMessage(),
          pluginData);
      return pluginData.toPath();
    }
  }

  private long getArchiveDurationFromConfig(String configValue) {
    try {
      return ConfigUtil.getTimeUnit(
          configValue, DAYS.toMillis(DEFAULT_ARCHIVE_DURATION_DAYS), MILLISECONDS);
    } catch (IllegalArgumentException e) {
      log.warn(
          "The configured archive duration is not valid: {}; using the default value: {} days",
          e.getMessage(),
          DEFAULT_ARCHIVE_DURATION_DAYS);
      return DAYS.toMillis(DEFAULT_ARCHIVE_DURATION_DAYS);
    }
  }
}
