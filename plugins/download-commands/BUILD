load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "download-commands",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: download-commands",
        "Gerrit-Module: com.googlesource.gerrit.plugins.download.Module",
    ],
    resources = glob(["src/main/resources/**/*"]),
)
