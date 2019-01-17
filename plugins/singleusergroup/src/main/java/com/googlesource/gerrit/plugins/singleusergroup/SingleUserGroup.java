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

package com.googlesource.gerrit.plugins.singleusergroup;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AbstractGroupBackend;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.account.ListGroupMembership;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.query.account.AccountPredicates;
import com.google.gerrit.server.query.account.AccountQueryBuilder;
import com.google.gerrit.server.query.account.AccountQueryProcessor;
import com.google.gwtorm.server.OrmException;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Makes a group out of each user.
 *
 * <p>UUIDs for the groups are derived from the unique username attached to the account. A user can
 * only be used as a group if it has a username.
 */
@Singleton
public class SingleUserGroup extends AbstractGroupBackend {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String UUID_PREFIX = "user:";
  private static final String NAME_PREFIX = "user/";
  private static final String ACCOUNT_PREFIX = "userid/";
  private static final String ACCOUNT_ID_PATTERN = "[1-9][0-9]*";
  private static final int MAX = 10;

  public static class Module extends AbstractModule {
    @Override
    protected void configure() {
      DynamicSet.bind(binder(), GroupBackend.class).to(SingleUserGroup.class);
    }
  }

  private final AccountCache accountCache;
  private final AccountQueryBuilder queryBuilder;
  private final Provider<AccountQueryProcessor> queryProvider;

  @Inject
  SingleUserGroup(
      AccountCache accountCache,
      AccountQueryBuilder queryBuilder,
      Provider<AccountQueryProcessor> queryProvider) {
    this.accountCache = accountCache;
    this.queryBuilder = queryBuilder;
    this.queryProvider = queryProvider;
  }

  @Override
  public boolean handles(AccountGroup.UUID uuid) {
    return uuid.get().startsWith(UUID_PREFIX);
  }

  @Override
  public GroupMembership membershipsOf(IdentifiedUser user) {
    ImmutableList.Builder<AccountGroup.UUID> groups = ImmutableList.builder();
    groups.add(uuid(user.getAccountId()));
    if (user.getUserName().isPresent()) {
      groups.add(uuid(user.getUserName().get()));
    }
    return new ListGroupMembership(groups.build());
  }

  @Override
  public GroupDescription.Basic get(AccountGroup.UUID uuid) {
    String ident = username(uuid);
    Optional<AccountState> state;
    if (ident.matches(ACCOUNT_ID_PATTERN)) {
      state = accountCache.get(new Account.Id(Integer.parseInt(ident)));
    } else if (ExternalId.isValidUsername(ident)) {
      state = accountCache.getByUsername(ident);
    } else {
      return null;
    }
    if (state.isPresent()) {
      String name = nameOf(uuid, state.get());
      String email = Strings.emptyToNull(state.get().getAccount().getPreferredEmail());
      return new GroupDescription.Basic() {
        @Override
        public AccountGroup.UUID getGroupUUID() {
          return uuid;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        @Nullable
        public String getEmailAddress() {
          return email;
        }

        @Override
        @Nullable
        public String getUrl() {
          return null;
        }
      };
    }
    return null;
  }

  @Override
  public Collection<GroupReference> suggest(String name, @Nullable ProjectState project) {
    try {
      return queryProvider
          .get()
          .setUserProvidedLimit(MAX)
          .query(AccountPredicates.andActive(queryBuilder.defaultQuery(name)))
          .entities()
          .stream()
          .map(SingleUserGroup::accountToGroup)
          .collect(toList());
    } catch (OrmException | QueryParseException err) {
      logger.atWarning().withCause(err).log("Cannot suggest users");
      return Collections.emptyList();
    }
  }

  private static GroupReference accountToGroup(AccountState s) {
    AccountGroup.UUID uuid =
        s.getUserName().isPresent() ? uuid(s.getUserName().get()) : uuid(s.getAccount().getId());
    return new GroupReference(uuid, nameOf(uuid, s));
  }

  private static String username(AccountGroup.UUID uuid) {
    checkUUID(uuid);
    return uuid.get().substring(UUID_PREFIX.length());
  }

  private static AccountGroup.UUID uuid(Account.Id ident) {
    return uuid(Integer.toString(ident.get()));
  }

  private static AccountGroup.UUID uuid(String username) {
    return new AccountGroup.UUID(UUID_PREFIX + username);
  }

  private static void checkUUID(AccountGroup.UUID uuid) {
    checkArgument(
        uuid.get().startsWith(UUID_PREFIX), "SingleUserGroup does not handle %s", uuid.get());
  }

  private static String nameOf(AccountGroup.UUID uuid, AccountState accountState) {
    StringBuilder buf = new StringBuilder();
    if (accountState.getAccount().getFullName() != null) {
      buf.append(accountState.getAccount().getFullName());
    }
    if (accountState.getUserName().isPresent()) {
      if (buf.length() > 0) {
        buf.append(" (").append(accountState.getUserName().get()).append(")");
      } else {
        buf.append(accountState.getUserName().get());
      }
    } else if (buf.length() > 0) {
      buf.append(" (").append(accountState.getAccount().getId().get()).append(")");
    } else {
      buf.append(accountState.getAccount().getId().get());
    }

    String ident = username(uuid);
    if (ident.matches(ACCOUNT_ID_PATTERN)) {
      buf.insert(0, ACCOUNT_PREFIX);
    } else {
      buf.insert(0, NAME_PREFIX);
    }
    return buf.toString();
  }
}
