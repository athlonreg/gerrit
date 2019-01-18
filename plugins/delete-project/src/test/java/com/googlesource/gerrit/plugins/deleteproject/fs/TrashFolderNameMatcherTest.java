// Copyright (C) 2016 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.deleteproject.fs;

import static com.google.common.truth.Truth.assertThat;
import static com.googlesource.gerrit.plugins.deleteproject.fs.DeleteTrashFolders.TrashFolderPredicate.match;

import org.junit.Test;

public class TrashFolderNameMatcherTest {

  @Test
  public void matchingNames() {
    matches("a.1234567890123.deleted");
    matches("aa.1234567890123.deleted");
    matches("a.b.c.1234567890123.deleted");

    matches("a.1234567890123.%deleted%.git");
    matches("aa.1234567890123.%deleted%.git");
    matches("a.b.c.1234567890123.%deleted%.git");

    matches("a.20181010120101.%deleted%.git");
    matches("aa.20181010120101.%deleted%.git");
    matches("a.b.c.20181010120101.%deleted%.git");
  }

  @Test
  public void nonMatchingNames() {
    doesNotMatch("a.git");
    doesNotMatch("a.1234567890123.git");
    doesNotMatch("a.1234567890123.deleted.git");

    // timestamp one digit shorter
    doesNotMatch("a.123456789012.deleted");

    // additional characters after the "deleted" suffix
    doesNotMatch("a.1234567890123.deleted.");
    doesNotMatch("a.1234567890123.deleted.git");

    // missing .git suffix
    doesNotMatch("a.1234567890123.%deleted%");
    doesNotMatch("a.20181010120101.%deleted%");

    // additional characters after the "git" suffix
    doesNotMatch("a.1234567890123.%deleted%.git.");
    doesNotMatch("a.1234567890123.%deleted%.git.git");
    doesNotMatch("a.20181010120101.%deleted%.git.");
    doesNotMatch("a.20181010120101.%deleted%.git.git");
  }

  private void matches(String name) {
    assertThat(match(name)).isTrue();
  }

  private void doesNotMatch(String name) {
    assertThat(match(name)).isFalse();
  }
}
