// Copyright (C) 2012 The Android Open Source Project
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
package com.googlesource.gerrit.plugins.validators;

import com.google.common.base.Splitter;
import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;

@Listen
@Singleton
public class CommitMessageLengthValidation implements CommitValidationListener {
  private static final int DEFAULT_MAX_SUBJECT_LENGTH = 50;
  private static final int DEFAULT_MAX_LINE_LENGTH = 72;
  private static final int DEFAULT_LONG_LINES_THRESHOLD = 33;
  private static final boolean DEFAULT_REJECT_TOO_LONG = false;
  private static final String COMMIT_MESSAGE_SECTION = "commitmessage";
  private static final String MAX_SUBJECT_LENGTH_KEY = "maxSubjectLength";
  private static final String MAX_LINE_LENGTH_KEY = "maxLineLength";
  private static final String REJECT_TOO_LONG_KEY = "rejectTooLong";
  private static final String LONG_LINES_THRESHOLD_KEY = "longLinesThreshold";

  private final Config config;
  private final int maxSubjectLength;
  private final int maxLineLength;
  private final int longLinesThreshold;
  private boolean rejectTooLong;

  @Inject
  public CommitMessageLengthValidation(@GerritServerConfig Config gerritConfig) {
    this.config = gerritConfig;
    this.maxSubjectLength = nonNegativeInt(MAX_SUBJECT_LENGTH_KEY, DEFAULT_MAX_SUBJECT_LENGTH);
    this.maxLineLength = nonNegativeInt(MAX_LINE_LENGTH_KEY, DEFAULT_MAX_LINE_LENGTH);
    this.rejectTooLong =
        config.getBoolean(COMMIT_MESSAGE_SECTION, REJECT_TOO_LONG_KEY, DEFAULT_REJECT_TOO_LONG);
    this.longLinesThreshold =
        nonNegativeInt(LONG_LINES_THRESHOLD_KEY, DEFAULT_LONG_LINES_THRESHOLD);
  }

  private int nonNegativeInt(String name, int defaultValue) {
    int value = this.config.getInt(COMMIT_MESSAGE_SECTION, null, name, defaultValue);
    return value >= 0 ? value : defaultValue;
  }

  private void onLineTooLong(List<CommitValidationMessage> messagesList, final String message)
      throws CommitValidationException {
    if (rejectTooLong) {
      messagesList.add(new CommitValidationMessage(message, true));
      throw new CommitValidationException("Commit length validation failed", messagesList);
    }
    messagesList.add(new CommitValidationMessage("warning: " + message, false));
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    final RevCommit commit = receiveEvent.commit;
    List<CommitValidationMessage> messages = new ArrayList<>();

    if (this.maxSubjectLength < commit.getShortMessage().length()) {
      onLineTooLong(
          messages,
          "subject >" + this.maxSubjectLength + " characters; use shorter first paragraph");
    }

    int longLineCnt = 0;
    int nonEmptyCnt = 0;
    for (String line : Splitter.on('\n').split(commit.getFullMessage())) {
      if (!line.trim().isEmpty()) {
        nonEmptyCnt++;
      }
      if (this.maxLineLength < line.length()) {
        longLineCnt++;
      }
    }

    if (longLineCnt > (longLinesThreshold * nonEmptyCnt) / 100) {
      onLineTooLong(
          messages,
          "too many message lines longer than "
              + this.maxLineLength
              + " characters; manually wrap lines");
    }

    return messages;
  }
}
