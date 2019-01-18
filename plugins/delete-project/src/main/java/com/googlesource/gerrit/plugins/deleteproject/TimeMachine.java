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

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * This class consists of static methods that allow the user to get the time instant from the system
 * clock. It also provides mechanisms to set the system clock and default zone clock for testing
 * purposes only.
 */
public class TimeMachine {

  private static Clock clock = Clock.systemDefaultZone();

  /**
   * Obtain the current instant on the time-line from the system clock using the system default time
   * zone.
   *
   * @return the current instant using the system clock.
   */
  public static Instant now() {
    return Instant.now(clock);
  }

  /**
   * Set the clock to be the specific instant.
   *
   * @param instant the specific instant used to set the clock.
   */
  @VisibleForTesting
  public static void useFixedClockAt(Instant instant) {
    clock = Clock.fixed(instant, ZoneId.systemDefault());
  }

  /** Reset the clock to be the system default. */
  @VisibleForTesting
  public static void useSystemDefaultZoneClock() {
    clock = Clock.systemDefaultZone();
  }
}
