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

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.DownloadCommand;
import com.google.inject.AbstractModule;

public class DownloadCommandsModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DownloadCommand.class).annotatedWith(Exports.named("Checkout")).to(CheckoutCommand.class);

    bind(DownloadCommand.class)
        .annotatedWith(Exports.named("Cherry Pick"))
        .to(CherryPickCommand.class);

    bind(DownloadCommand.class)
        .annotatedWith(Exports.named("Format Patch"))
        .to(FormatPatchCommand.class);

    bind(DownloadCommand.class).annotatedWith(Exports.named("Pull")).to(PullCommand.class);

    bind(DownloadCommand.class).annotatedWith(Exports.named("repo")).to(RepoCommand.class);
  }
}
