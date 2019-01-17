The refs/notes/review namespace
===============================

Summary
-------

`refs/notes/review` is a special reference that this plugin creates on
repositories to store information about code reviews.

When a repository is cloned from Gerrit, the `refs/notes/review` reference is
not included by default.  It has to be manually fetched:

```
  $ git fetch origin refs/notes/review:refs/notes/review
```

It is also possible to [configure git][1] to always fetch `refs/notes/review`:

```
  $ git config --add remote.origin.fetch refs/notes/review:refs/notes/review
  $ git fetch
```

[1]: http://www.kernel.org/pub/software/scm/git/docs/git-config.html

When `refs/notes/review` is fetched on a repository, the review notes
information can be included in the git log output:

```
   $ git log --show-notes=review
```

Content of refs/notes/review
----------------------------

For each commit, this plugin stores the following review information in
`refs/notes/review`:

### Submitted-by

The name and email address of the Gerrit user that submitted the change in
[RFC 2822][2] format.

[2]: http://www.ietf.org/rfc/rfc2822.txt

```
  Submitted-by: Random J Developer <random@developer.example.org>
```

### Submitted-at

The time the commit was submitted in RFC 2822 time stamp format.

```
  Submitted-at: Mon, 25 Jun 2012 16:15:57 +0200
```

### Reviewed-on

The URL to the change on the Gerrit server.

```
  Reviewed-on: http://path.to.gerrit/12345
```

### Review Labels and Scores

Review label and score, and the name and email address of the Gerrit user that
gave it in RFC 2822 format:

```
  Code-Review+2: A. N. Other <another@developer.example.org>
  Verified+1: A. N. Other <another@developer.example.org>
```

Commonly used review labels are "Code-Review" and "Verified", but any label
configured in Gerrit can be included.

All review labels and scores present on the change at the time of submit are
included.

### Project

The name of the project in which the commit was made.

```
  Project: kernel/common
```

### Branch

The name of the branch on which the commit was made.

```
  Branch: refs/heads/master
```
