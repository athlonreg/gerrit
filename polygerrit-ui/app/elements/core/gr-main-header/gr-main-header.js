/**
 * @license
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
  'use strict';

  const DEFAULT_LINKS = [{
    title: '提交变更',
    links: [
      {
        url: '/q/status:open',
        name: '已打开',
      },
      {
        url: '/q/status:merged',
        name: '已合并',
      },
      {
        url: '/q/status:abandoned',
        name: '已丢弃',
      },
    ],
  }];

  const DOCUMENTATION_LINKS = [
    {
      url: '/index.html',
      name: '文档列表',
    },
    {
      url: '/user-search.html',
      name: '搜索',
    },
    {
      url: '/user-upload.html',
      name: '上传',
    },
    {
      url: '/access-control.html',
      name: '访问控制',
    },
    {
      url: '/rest-api.html',
      name: 'REST API',
    },
    {
      url: '/intro-project-owner.html',
      name: '项目所有者指南',
    },
  ];

  // Set of authentication methods that can provide custom registration page.
  const AUTH_TYPES_WITH_REGISTER_URL = new Set([
    'LDAP',
    'LDAP_BIND',
    'CUSTOM_EXTENSION',
  ]);

  Polymer({
    is: 'gr-main-header',

    hostAttributes: {
      role: 'banner',
    },

    properties: {
      searchQuery: {
        type: String,
        notify: true,
      },
      loggedIn: {
        type: Boolean,
        reflectToAttribute: true,
      },
      loading: {
        type: Boolean,
        reflectToAttribute: true,
      },

      /** @type {?Object} */
      _account: Object,
      _adminLinks: {
        type: Array,
        value() { return []; },
      },
      _defaultLinks: {
        type: Array,
        value() {
          return DEFAULT_LINKS;
        },
      },
      _docBaseUrl: {
        type: String,
        value: null,
      },
      _links: {
        type: Array,
        computed: '_computeLinks(_defaultLinks, _userLinks, _adminLinks, ' +
            '_topMenus, _docBaseUrl)',
      },
      _loginURL: {
        type: String,
        value: '/login',
      },
      _userLinks: {
        type: Array,
        value() { return []; },
      },
      _topMenus: {
        type: Array,
        value() { return []; },
      },
      _registerText: {
        type: String,
        value: 'Sign up',
      },
      _registerURL: {
        type: String,
        value: null,
      },
    },

    behaviors: [
      Gerrit.AdminNavBehavior,
      Gerrit.BaseUrlBehavior,
      Gerrit.DocsUrlBehavior,
    ],

    observers: [
      '_accountLoaded(_account)',
    ],

    attached() {
      this._loadAccount();
      this._loadConfig();
      this.listen(window, 'location-change', '_handleLocationChange');
    },

    detached() {
      this.unlisten(window, 'location-change', '_handleLocationChange');
    },

    reload() {
      this._loadAccount();
    },

    _handleLocationChange(e) {
      const baseUrl = this.getBaseUrl();
      if (baseUrl) {
        // Strip the canonical path from the path since needing canonical in
        // the path is uneeded and breaks the url.
        this._loginURL = baseUrl + '/login/' + encodeURIComponent(
            '/' + window.location.pathname.substring(baseUrl.length) +
            window.location.search +
            window.location.hash);
      } else {
        this._loginURL = '/login/' + encodeURIComponent(
            window.location.pathname +
            window.location.search +
            window.location.hash);
      }
    },

    _computeRelativeURL(path) {
      return '//' + window.location.host + this.getBaseUrl() + path;
    },

    _computeLinks(defaultLinks, userLinks, adminLinks, topMenus, docBaseUrl) {
      const links = defaultLinks.map(menu => {
        return {
          title: menu.title,
          links: menu.links.slice(),
        };
      });
      if (userLinks && userLinks.length > 0) {
        links.push({
          title: '个人相关',
          links: userLinks.slice(),
        });
      }
      const docLinks = this._getDocLinks(docBaseUrl, DOCUMENTATION_LINKS);
      if (docLinks.length) {
        links.push({
          title: '文档',
          links: docLinks,
          class: 'hideOnMobile',
        });
      }
      links.push({
        title: '综合浏览',
        links: adminLinks.slice(),
      });
      const topMenuLinks = [];
      links.forEach(link => { topMenuLinks[link.title] = link.links; });
      for (const m of topMenus) {
        const items = m.items.map(this._fixCustomMenuItem);
        if (m.name in topMenuLinks) {
          items.forEach(link => { topMenuLinks[m.name].push(link); });
        } else {
          links.push({
            title: m.name,
            links: topMenuLinks[m.name] = items,
          });
        }
      }
      return links;
    },

    _getDocLinks(docBaseUrl, docLinks) {
      if (!docBaseUrl || !docLinks) {
        return [];
      }
      return docLinks.map(link => {
        let url = docBaseUrl;
        if (url && url[url.length - 1] === '/') {
          url = url.substring(0, url.length - 1);
        }
        return {
          url: url + link.url,
          name: link.name,
          target: '_blank',
        };
      });
    },

    _loadAccount() {
      this.loading = true;
      const promises = [
        this.$.restAPI.getAccount(),
        this.$.restAPI.getTopMenus(),
        Gerrit.awaitPluginsLoaded(),
      ];

      return Promise.all(promises).then(result => {
        const account = result[0];
        this._account = account;
        this.loggedIn = !!account;
        this.loading = false;
        this._topMenus = result[1];

        return this.getAdminLinks(account,
            this.$.restAPI.getAccountCapabilities.bind(this.$.restAPI),
            this.$.jsAPI.getAdminMenuLinks.bind(this.$.jsAPI))
            .then(res => {
              this._adminLinks = res.links;
            });
      });
    },

    _loadConfig() {
      this.$.restAPI.getConfig()
          .then(config => {
            this._retrieveRegisterURL(config);
            return this.getDocsBaseUrl(config, this.$.restAPI);
          })
          .then(docBaseUrl => { this._docBaseUrl = docBaseUrl; });
    },

    _accountLoaded(account) {
      if (!account) { return; }

      this.$.restAPI.getPreferences().then(prefs => {
        this._userLinks =
            prefs.my.map(this._fixCustomMenuItem).filter(this._isSupportedLink);
      });
    },

    _retrieveRegisterURL(config) {
      if (AUTH_TYPES_WITH_REGISTER_URL.has(config.auth.auth_type)) {
        this._registerURL = config.auth.register_url;
        if (config.auth.register_text) {
          this._registerText = config.auth.register_text;
        }
      }
    },

    _computeIsInvisible(registerURL) {
      return registerURL ? '' : 'invisible';
    },

    _fixCustomMenuItem(linkObj) {
      // Normalize all urls to PolyGerrit style.
      if (linkObj.url.startsWith('#')) {
        linkObj.url = linkObj.url.slice(1);
      }

      // Delete target property due to complications of
      // https://bugs.chromium.org/p/gerrit/issues/detail?id=5888
      //
      // The server tries to guess whether URL is a view within the UI.
      // If not, it sets target='_blank' on the menu item. The server
      // makes assumptions that work for the GWT UI, but not PolyGerrit,
      // so we'll just disable it altogether for now.
      delete linkObj.target;

      // Because the user provided links may be arbitrary URLs, we don't know
      // whether they correspond to any client routes. Mark all such links as
      // external.
      linkObj.external = true;

      return linkObj;
    },

    _isSupportedLink(linkObj) {
      // Groups are not yet supported.
      return !linkObj.url.startsWith('/groups');
    },

    _generateSettingsLink() {
      return this.getBaseUrl() + '/settings/';
    },
  });
})();
