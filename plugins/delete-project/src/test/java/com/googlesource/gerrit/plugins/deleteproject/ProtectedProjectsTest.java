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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.config.AllProjectsNameProvider;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.config.AllUsersNameProvider;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import java.io.File;
import java.util.List;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProtectedProjectsTest {
  private static final String PLUGIN_NAME = "delete-project";

  @Mock private AllProjectsNameProvider allProjectsMock;
  @Mock private AllUsersNameProvider allUsersMock;
  @Mock private PluginConfigFactory pluginConfigFactoryMock;

  private PluginConfig pluginConfig;
  private Configuration deleteConfig;
  private ProtectedProjects protectedProjects;
  private File pluginData = new File("data");

  @Before
  public void setup() throws Exception {
    when(allProjectsMock.get()).thenReturn(new AllProjectsName("All-Projects"));
    when(allUsersMock.get()).thenReturn(new AllUsersName("All-Users"));
    pluginConfig = new PluginConfig(PLUGIN_NAME, new Config());
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    deleteConfig = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME, pluginData);
    protectedProjects = new ProtectedProjects(allProjectsMock, allUsersMock, deleteConfig);
  }

  @Test
  public void allProjectsIsProtected() throws Exception {
    assertProtected("All-Projects");
  }

  @Test
  public void allUsersIsProtected() throws Exception {
    assertProtected("All-Users");
  }

  @Test
  public void otherProjectIsNotProtected() throws Exception {
    assertNotProtected("test-project");
  }

  @Test
  public void customProjectIsProtected() throws Exception {
    List<String> projects = ImmutableList.of("Custom-Parent", "^protected-.*");
    pluginConfig.setStringList("protectedProject", projects);
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    deleteConfig = new Configuration(pluginConfigFactoryMock, PLUGIN_NAME, pluginData);
    assertThat(deleteConfig.protectedProjects()).hasSize(projects.size());
    protectedProjects = new ProtectedProjects(allProjectsMock, allUsersMock, deleteConfig);

    assertProtected("protected-project-1");
    assertProtected("protected-project-2");
    assertProtected("Custom-Parent");
    assertNotProtected("test-project");
    assertNotProtected("protected");
    assertNotProtected("my-protected-project");
    assertNotProtected("Another-Custom-Parent");
    assertNotProtected("Custom-Parent-2");
  }

  private void assertProtected(String name) {
    assertThat(protectedProjects.isProtected(new Project.NameKey(name))).isTrue();
  }

  private void assertNotProtected(String name) {
    assertThat(protectedProjects.isProtected(new Project.NameKey(name))).isFalse();
  }
}
