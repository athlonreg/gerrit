@PLUGIN@ Configuration
======================

By configuration it is possible to specify which download schemes and
commands are enabled.

The configuration must be done in the `gerrit.config` of the Gerrit
server.

### <a id="download">Section download</a>

```
[download]
  command = checkout
  command = cherry_pick
  command = pull
  command = format_patch
  scheme = ssh
  scheme = http
  scheme = anon_http
  scheme = anon_git
  scheme = repo_download
```

The download section configures the allowed download methods.

<a id="download.command">download.command</a>
:	Commands that should be offered to download changes.

	Multiple commands are supported:

	* `checkout`: Command to fetch and checkout the patch set.

	* `cherry_pick`: Command to fetch the patch set and cherry-pick
	it onto the current commit.

	* `pull`: Command to pull the patch set.

	* `format_patch`: Command to fetch the patch set and feed it
	into the `format-patch` command.

	If `download.command` is not specified, all download commands are
	offered.

<a id="download.scheme">download.scheme</a>
:	Schemes that should be used to download changes.

	Multiple schemes are supported:

	* `http`: Authenticated HTTP download is allowed.

	* `ssh`: Authenticated SSH download is allowed.

	* `anon_http`: Anonymous HTTP download is allowed.

	* `anon_git`: Anonymous Git download is allowed.  This is not
	default, it is also necessary to set [gerrit.canonicalGitUrl]
	(../../../Documentation/config-gerrit.html#gerrit.canonicalGitUrl)
	variable.

	* `repo_download`: Gerrit advertises patch set downloads with the
	`repo download` command, assuming that all projects managed by this
	instance are generally worked on with the repo multi-repository
	tool.  This is not default, as not all instances will deploy repo.

	If `download.scheme` is not specified, SSH, HTTP and Anonymous HTTP
	downloads are allowed.

<a id="download.checkForHiddenChangeRefs">download.checkForHiddenChangeRefs</a>
:	Whether the download commands should be adapted when the change
	refs are hidden.

	Git has a configuration option to hide refs from the initial
	advertisement (`uploadpack.hideRefs`). This option can be used to
	hide the change refs from the client. As consequence fetching
	changes by change ref does not work anymore. However by setting
	`uploadpack.allowTipSHA1InWant` to `true` fetching changes by
	commit ID is possible. If `download.checkForHiddenChangeRefs` is
	set to `true` the git download commands use the commit ID instead
	of the change ref when a project is configured like this.

	Example git configuration on a project:

		[uploadpack]
		  hideRefs = refs/changes/
		  hideRefs = refs/cache-automerge/
		  allowTipSHA1InWant = true

	By default `false`.


### <a id="gerrit">Section gerrit</a>

```
[gerrit]
  installCommitMsgHookCommand = command
  installCommitExtraCommand = command
```

<a id="gerrit.installCommitMsgHookCommand">gerrit.installCommitMsgHookCommand</a>
  Optional command to install the commit-msg hook. Typically of the form:
  `fetch-cmd some://url/to/commit-msg .git/hooks/commit-msg ; chmod +x .git/hooks/commit-msg`
  By default unset; falls back to using scp from the canonical SSH host,
  or curl from the canonical HTTP URL for the server. Only necessary
  if a proxy or other server/network configuration prevents clients
  from fetching from the default location.

<a id="gerrit.installCommitExtraCommand">gerrit.installCommitExtraCommand</a>
  Optional command to complete the commit-msg hook. For example:
  `git submodule update --init --recursive && git review -s`
  would initialize the submodules and setup git review.