Commit Message Length Configuration
===================================

The maximum lengths of the subject and message body can be
configured in the standard Gerrit config file `gerrit.config`.
The defaults are 50 characters for the summary and 72 characters
for the description, as recommended by the
[git tutorial](https://kernel.googlesource.com/pub/scm/git/git/+/927a503cd07718ea0f700052043f383253904a56/Documentation/tutorial.txt#64)
and [expanded upon by Tim Pope](http://www.tpope.net/node/106).

commitmessage.maxSubjectLength
:	Maximum length of the commit message's subject line.  Defaults
	to 50 if not specified or less than 0.

commitmessage.maxLineLength
:	Maximum length of a line in the commit message's body.  Defaults
	to 72 if not specified or less than 0.

commitmessage.longLinesThreshold
:	Percentage of commit message lines allowed to exceed the
	maximum length before a warning or error is generated.  Defaults
	to 33 if not specified or less than 0.

commitmessage.rejectTooLong
:	If set to `true`, reject commits whose subject or line
	length exceeds the maximum allowed length.  If not
	specified, defaults to `false`.
