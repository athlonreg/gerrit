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

package com.googlesource.gerrit.plugins.deleteproject;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.webui.JavaScriptPlugin;
import com.google.gerrit.extensions.webui.WebUiPlugin;
import com.google.gerrit.httpd.plugins.HttpPluginModule;
import com.google.inject.Inject;

public class HttpModule extends HttpPluginModule {
  private final Configuration cfg;

  @Inject
  HttpModule(Configuration cfg) {
    this.cfg = cfg;
  }

  @Override
  protected void configureServlets() {
    if (cfg.enablePreserveOption()) {
      DynamicSet.bind(binder(), WebUiPlugin.class)
          .toInstance(new JavaScriptPlugin("delete-project.js"));
      DynamicSet.bind(binder(), WebUiPlugin.class)
          .toInstance(new JavaScriptPlugin("gr-delete-repo.html"));
    } else {
      DynamicSet.bind(binder(), WebUiPlugin.class)
          .toInstance(new JavaScriptPlugin("delete-project-with-preserve-disabled.js"));
    }
  }
}
