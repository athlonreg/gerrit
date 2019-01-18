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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationTest {
  private static final long DEFAULT_ARCHIVE_DURATION_MS = TimeUnit.DAYS.toMillis(180);
  private static final String CUSTOM_DURATION = "100";
  private static final String CUSTOM_PARENT = "customParent";
  private static final String INVALID_CUSTOM_FOLDER = "\0";
  private static final String INVALID_ARCHIVE_DURATION = "180weeks180years";
  private static final String PLUGIN_NAME = "delete-project";

  @Mock private PluginConfigFactory pluginConfigFactoryMock;
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private Path customArchiveFolder;
  private File pluginDataDir;
  private Configuration deleteConfig;

  @Before
  public void setUp() throws Exception {
    pluginDataDir = tempFolder.newFolder("data");
    customArchiveFolder = tempFolder.newFolder("archive").toPath();
  }

  @Test
  public void defaultValuesAreLoaded() {
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(new PluginConfig(PLUGIN_NAME, new Config()));
    deleteConfig = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME, pluginDataDir);

    assertThat(deleteConfig.getDeletedProjectsParent()).isEqualTo("Deleted-Projects");
    assertThat(deleteConfig.deletionWithTagsAllowed()).isTrue();
    assertThat(deleteConfig.projectOnPreserveHidden()).isFalse();
    assertThat(deleteConfig.shouldArchiveDeletedRepos()).isFalse();
    assertThat(deleteConfig.getArchiveDuration()).isEqualTo(DEFAULT_ARCHIVE_DURATION_MS);
    assertThat(deleteConfig.getArchiveFolder().toString()).isEqualTo(pluginDataDir.toString());
    assertThat(deleteConfig.enablePreserveOption()).isTrue();
  }

  @Test
  public void customValuesAreLoaded() {
    PluginConfig pluginConfig = new PluginConfig(PLUGIN_NAME, new Config());
    pluginConfig.setString("parentForDeletedProjects", CUSTOM_PARENT);
    pluginConfig.setBoolean("allowDeletionOfReposWithTags", false);
    pluginConfig.setBoolean("hideProjectOnPreserve", true);
    pluginConfig.setBoolean("archiveDeletedRepos", true);
    pluginConfig.setBoolean("enablePreserveOption", false);
    pluginConfig.setString("deleteArchivedReposAfter", CUSTOM_DURATION);
    pluginConfig.setString("archiveFolder", customArchiveFolder.toString());

    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    deleteConfig = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME, pluginDataDir);

    assertThat(deleteConfig.getDeletedProjectsParent()).isEqualTo(CUSTOM_PARENT);
    assertThat(deleteConfig.deletionWithTagsAllowed()).isFalse();
    assertThat(deleteConfig.projectOnPreserveHidden()).isTrue();
    assertThat(deleteConfig.shouldArchiveDeletedRepos()).isTrue();
    assertThat(deleteConfig.enablePreserveOption()).isFalse();
    assertThat(deleteConfig.getArchiveDuration()).isEqualTo(Long.parseLong(CUSTOM_DURATION));
    assertThat(deleteConfig.getArchiveFolder().toString())
        .isEqualTo(customArchiveFolder.toString());
  }

  @Test
  public void archiveDurationWithUnitIsLoaded() {
    PluginConfig pluginConfig = new PluginConfig(PLUGIN_NAME, new Config());
    pluginConfig.setString("deleteArchivedReposAfter", CUSTOM_DURATION + "years");

    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    deleteConfig = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME, pluginDataDir);

    assertThat(deleteConfig.getArchiveDuration())
        .isEqualTo(TimeUnit.DAYS.toMillis(Long.parseLong(CUSTOM_DURATION)) * 365);
  }

  @Test
  public void invalidArchiveDuration() {
    PluginConfig pluginConfig = new PluginConfig(PLUGIN_NAME, new Config());
    pluginConfig.setString("deleteArchivedReposAfter", INVALID_ARCHIVE_DURATION);

    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    deleteConfig = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME, pluginDataDir);

    assertThat(deleteConfig.getArchiveDuration()).isEqualTo(DEFAULT_ARCHIVE_DURATION_MS);
  }

  @Test
  public void invalidTargetArchiveFolder() {
    PluginConfig pluginConfig = new PluginConfig(PLUGIN_NAME, new Config());
    pluginConfig.setString("archiveFolder", INVALID_CUSTOM_FOLDER);

    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    deleteConfig = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME, pluginDataDir);

    assertThat(deleteConfig.getArchiveFolder().toString()).isEqualTo(pluginDataDir.toString());
  }
}
