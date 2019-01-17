load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "reviewnotes",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: reviewnotes",
        "Gerrit-Module: com.googlesource.gerrit.plugins.reviewnotes.ReviewNotesModule",
        "Gerrit-SshModule: com.googlesource.gerrit.plugins.reviewnotes.SshModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
)
