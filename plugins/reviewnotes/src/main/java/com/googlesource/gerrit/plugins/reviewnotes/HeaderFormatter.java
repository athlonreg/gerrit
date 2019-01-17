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

package com.googlesource.gerrit.plugins.reviewnotes;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.gerrit.common.data.LabelType;
import com.google.gerrit.common.data.LabelValue;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.UrlFormatter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Formatters for code review note headers.
 *
 * <p>This class provides a builder like interface for building the content of a code review note.
 * After instantiation, call as many as necessary <code>append...(...)</code> methods and, at the
 * end, call the {@link #toString()} method to get the built note content.
 */
class HeaderFormatter {

  private final DateFormat rfc2822DateFormatter;
  private final String anonymousCowardName;
  private final StringBuilder sb = new StringBuilder();

  HeaderFormatter(TimeZone tz, String anonymousCowardName) {
    rfc2822DateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    rfc2822DateFormatter.setCalendar(Calendar.getInstance(tz, Locale.US));
    this.anonymousCowardName = anonymousCowardName;
  }

  /**
   * Appends a header for an approval.
   *
   * @param label the label on which the approval was done
   * @param value the voting value
   * @param accountId the account ID of the approver
   * @param account the account of the approver, can be {@link Optional#empty} if the account is
   *     missing
   */
  void appendApproval(
      LabelType label, short value, Account.Id accountId, Optional<Account> account) {
    sb.append(label.getName());
    sb.append(LabelValue.formatValue(value));
    sb.append(": ");
    appendUserData(accountId, account);
    sb.append("\n");
  }

  /**
   * Appends user data.
   *
   * @param accountId the ID of the account
   * @param account the account, can be {link Optional#empty} if the account is missing
   */
  private void appendUserData(Account.Id accountId, Optional<Account> account) {
    checkState(
        !account.isPresent() || accountId.equals(account.get().getId()), "mismatching account IDs");

    boolean needSpace = false;
    boolean wroteData = false;

    if (account.isPresent()) {
      String fullName = account.get().getFullName();
      if (!Strings.isNullOrEmpty(fullName)) {
        sb.append(fullName);
        needSpace = true;
        wroteData = true;
      }

      String preferredEmail = account.get().getPreferredEmail();
      if (!Strings.isNullOrEmpty(preferredEmail)) {
        if (needSpace) {
          sb.append(" ");
        }
        sb.append("<").append(preferredEmail).append(">");
        wroteData = true;
      }
    }

    if (!wroteData) {
      sb.append(anonymousCowardName).append(" #").append(accountId);
    }
  }

  void appendProject(String projectName) {
    sb.append("Project: ").append(projectName).append("\n");
  }

  void appendBranch(String branch) {
    sb.append("Branch: ").append(branch).append("\n");
  }

  /**
   * Appends a header with the submitter information.
   *
   * @param accountId the account ID of the submitter
   * @param account the account of the submitter, can be {@link Optional#empty()} if the account is
   *     missing
   */
  void appendSubmittedBy(Account.Id accountId, Optional<Account> account) {
    sb.append("Submitted-by: ");
    appendUserData(accountId, account);
    sb.append("\n");
  }

  void appendSubmittedAt(Date date) {
    sb.append("Submitted-at: ").append(rfc2822DateFormatter.format(date)).append("\n");
  }

  void appendReviewedOn(UrlFormatter urlFormatter, Project.NameKey project, Change.Id changeId) {
    sb.append("Reviewed-on: ")
        .append(urlFormatter.getChangeViewUrl(project, changeId).get())
        .append("\n");
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}
