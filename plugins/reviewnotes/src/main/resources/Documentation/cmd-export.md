@PLUGIN@ export
===============

NAME
----
@PLUGIN@ export - Export successful reviews to [refs/notes/review](refs-notes-review.md)

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ export
  [--threads <N>]
```

DESCRIPTION
-----------
Scans every submitted change and creates an initial notes
branch detailing the previous submission information for
each merged change.

This task can take quite some time, but can run in the background
concurrently to the server.

ACCESS
------
Caller must be a member of the privileged 'Administrators' group.

OPTIONS
-------

`--threads <N>`
: Number of concurrent threads to run. By default 2.

CONTEXT
-------
This command can only be run on a server which has direct
connectivity to the metadata database, and local access to the
managed Git repositories.

EXAMPLES
--------
To generate all review information:

```
  $ ssh -p 29418 user@review reviewnotes export --threads 16
```
