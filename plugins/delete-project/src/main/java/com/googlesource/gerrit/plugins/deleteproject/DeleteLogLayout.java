// Copyright (C) 2015 The Android Open Source Project
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jgit.util.QuotedString;

final class DeleteLogLayout extends Layout {
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("'['yyyy-MM-dd HH:mm:ss,SSS xxxx']'");

  /**
   * Formats the events in the delete log.
   *
   * <p>A successful project deletion will result in a log entry like this: [2015-03-05 09:13:28,912
   * +0100] INFO 1000000 admin OK \ myProject {"preserve":false,"force":false}
   *
   * <p>The log entry for a failed project deletion will look like this: [2015-03-05 12:14:30,180
   * +0100] ERROR 1000000 admin FAIL \ myProject {"preserve":false,"force":false}
   * com.google.gwtorm.server.OrmException: \ Failed to access the database
   */
  @Override
  public String format(LoggingEvent event) {
    final StringBuffer buf = new StringBuffer(128);

    buf.append(formatDate(event.getTimeStamp()));

    buf.append(' ');
    buf.append(event.getLevel().toString());

    req(DeleteLog.ACCOUNT_ID, buf, event);
    req(DeleteLog.USER_NAME, buf, event);

    buf.append(' ');
    buf.append(event.getMessage());

    req(DeleteLog.PROJECT_NAME, buf, event);
    opt(DeleteLog.OPTIONS, buf, event);
    opt(DeleteLog.ERROR, buf, event);

    buf.append('\n');
    return buf.toString();
  }

  private String formatDate(long now) {
    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault());
    return zdt.format(DATE_FORMATTER);
  }

  private void req(String key, StringBuffer buf, LoggingEvent event) {
    Object val = event.getMDC(key);
    buf.append(' ');
    if (val != null) {
      String s = val.toString();
      if (0 <= s.indexOf(' ')) {
        buf.append(QuotedString.BOURNE.quote(s));
      } else {
        buf.append(val);
      }
    } else {
      buf.append('-');
    }
  }

  private void opt(String key, StringBuffer buf, LoggingEvent event) {
    Object val = event.getMDC(key);
    if (val != null) {
      buf.append(' ');
      buf.append(val);
    }
  }

  @Override
  public boolean ignoresThrowable() {
    return true;
  }

  @Override
  public void activateOptions() {}
}
