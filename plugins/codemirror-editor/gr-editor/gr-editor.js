// Copyright (C) 2017 The Android Open Source Project
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
(function() {
  'use strict';

  // we need to be on codemirror 5.33.0+ to get the support for
  // text/x-php in CodeMirror.findModeByMIME
  const LANGUAGE_MAP = {
    'text/x-php': 'php',
  };

  Polymer({
    is: 'gr-editor',
    /**
     * Fired when the content of the editor changes.
     *
     * @event content-change
     */

    properties: {
      fileContent: String,
      fileType: String,
      mirror: Object,
      prefs: Object,
      plugin: Object,
    },

    attached() {
      this._initializeMirror();
    },

    _initializeMirror() {
      this.scopeSubtree(this.$.wrapper, true);
      return new Promise((resolve, reject) => {
        this._importCodeMirror().then(() => {
          const params = this.getCodeMirrorParams(this.fileType,
              this.fileContent, this.prefs);
          this.mirror = window.CodeMirror(this.$.wrapper, params);
          this.async(() => {
            this.mirror.refresh();
            this.mirror.focus();
          }, 1);
          this.addEventListeners();
          resolve();
        });
      });
    },

    _importCodeMirror() {
      const url = this.plugin.url('/static/codemirror-assets.html');
      return new Promise((resolve, reject) => {
        this.importHref(url, resolve, reject);
      });
    },

    addEventListeners() {
      this.mirror.on('change', e => {
        this.dispatchEvent(new CustomEvent('content-change',
            {detail: {value: e.getValue()}, bubbles: true}));
      });
    },

    getCodeMirrorParams(type, value, prefs) {
      const params = {value, viewportMargin: Infinity};

      if (prefs) {
        // TODO: Add gerrit's customizations from java codemirror to javascript
        // gerrit-gwtui/src/main/java/net/codemirror/lib/Extras.java
        params.autoCloseBrackets = prefs.auto_close_brackets;
        params.cursorHeight = 0.85;
        params.indentUnit = prefs.indent_unit;
        params.indentWithTabs = prefs.indent_with_tabs;
        // TODO(kaspern): Add support for keymaps.
        // params.keyMap = prefs.key_map_type.toLowerCase();
        params.lineNumbers = true;
        params.lineWrapping = prefs.line_wrapping;
        params.matchBrackets = prefs.match_brackets;
        // TODO: Add support for a new commit msg MIME type
        // Support for this is somewhere in gerrit's codebase
        // needs backporting to javascript
        params.mode = prefs.syntax_highlighting ? this._mapFileType(type) : '';
        params.showTabs = prefs.show_tabs;
        params.showTrailingSpace = prefs.show_whitespace_errors;
        params.tabSize = prefs.tab_size;
        // TODO(kaspern): Add support for themes.
        // params.theme = prefs.theme.toLowerCase();

        params.rulers = [{column: prefs.line_length}];
        if (value && value.includes('\r\n')) {
          params.lineSeparator = '\r\n';
        }
      }

      return params;
    },

    _mapFileType(type) {
      return LANGUAGE_MAP[type] || type || '';
    },
  });
})();
