load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "commit-message-length-validator",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: commit-message-length-validator",
    ],
    resources = glob(["src/main/resources/**/*"]),
)
