Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    allowDeletionOfReposWithTags = true
```

<a id="allowDeletionOfReposWithTags">
`plugin.@PLUGIN@.allowDeletionOfReposWithTags`
:	Whether repositories that contain tags can be deleted.

	In some organizations repositories that contain tags must not be
	deleted due to legal reasons. This is the case when tags are used
	to mark releases which are shipped to customers and hence the
	source code must be kept to ensure build reproducibility of the
	releases.

	By default true.

<a id="hideProjectOnPreserve">
`plugin.@PLUGIN@.hideProjectOnPreserve`
:	Whether projects should be hidden when the preserve option is used
	for the deletion.

	Hiding the project means that the project state is set to `HIDDEN`,
	all access rights are removed and the project is reparented to the
	project defined by [parentForDeletedProjects](#parentForDeletedProjects).
	This parent project is automatically created under the root project
	when it does not exist yet.

	By default false.

<a id="enablePreserveOption">
`plugin.@PLUGIN@.enablePreserveOption`
:	Whether the "Preserve git repository" option is enabled for the user on the
    UI and ssh delete-project command.

	Disabling the preserve option means the user does not have access to the
	preserve option on the UI and ssh delete-project command.

	If this is set to false, then preserving deleted git repositories is
	disabled.

	By default true.

<a id="parentForDeletedProjects">
`plugin.@PLUGIN@.parentForDeletedProjects`
:	The name of the project that is used as parent for all deleted
	projects that were preserved by hiding them.

	This project is only used when [hideProjectOnPreserve](#hideProjectOnPreserve)
	is set to true.

	By default `Deleted-Projects`.

<a id="protectedProject">
`plugin.@PLUGIN@.protectedProject`
:	The name of a project that is protected against deletion. May be an exact
	name or a regular expression.

	May be specified more than once to specify multiple project names or
	patterns.

	By default not set.

<a id="archiveDeletedRepos">
`plugin.@PLUGIN@.archiveDeletedRepos`
:	Whether to archive repositories instead of deleting them.

	Archiving the git repository means that the repository is stored
	in a folder which is not visible to the users which, from the user
	perspective, is equivalent to the repository being deleted. The
	advantage is, archived repositories can be later recovered if, needed,
	just by moving them back to their former name and location.
	The target folder used to archive the repositories can be set by
	[archiveFolder](#archiveFolder). Archived repositories are moved
	under the [archiveFolder](#archiveFolder) and renamed to add a
	timestamp and the %archived% suffix to the original name.
	Archived repositories are kept in the archive for a time period which
	can be set by [deleteArchivedReposAfter](#deleteArchivedReposAfter).

	If this option is enabled, the project will not be deleted but rather
	renamed and moved into the archive folder.

	If the repository has been archived for a time period longer than
	[deleteArchivedReposAfter](#deleteArchivedReposAfter), it will be
	deleted from the archive by a periodic task which runs once a day.

	By default false.

<a id="archiveFolder">
`plugin.@PLUGIN@.archiveFolder`
:	The absolute path of the archive folder to store archived repositories.

	The git repository is archived to this target folder only if
	[archiveDeletedRepos](#archiveDeletedRepos) is set to true.

	By default `$site_path/data/delete-project`.

<a id="deleteArchivedReposAfter">
`plugin.@PLUGIN@.deleteArchivedReposAfter`
:	The time duration for the git repository to be archived.

	The following suffixes are supported to define the time unit:\n
		1. d, day, days\n
		2. w, week, weeks (1 week is treated as 7 days)\n
		3. mon, month, months (1 month is treated as 30 days)\n
		4. y, year, years (1 year is treated as 365 days)\n

	If not specified, the default time unit is in days.

	The project git repository is archived to this target folder only if
	[archiveDeletedRepos](#archiveDeletedRepos) is set to true.

	If [archiveDeletedRepos](#archiveDeletedRepos) is set to true but this
	option is set to zero, the periodic task will not be executed and the
	archived repositories need to be deleted manually or using an external
	task.

	By default 180 (days).
