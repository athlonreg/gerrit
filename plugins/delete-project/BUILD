load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:js.bzl", "polygerrit_plugin")

gerrit_plugin(
    name = "delete-project",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: delete-project",
        "Gerrit-Module: com.googlesource.gerrit.plugins.deleteproject.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.deleteproject.HttpModule",
        "Gerrit-SshModule: com.googlesource.gerrit.plugins.deleteproject.SshModule",
    ],
    resource_jars = [":gr-delete-repo-static"],
    resources = glob(["src/main/resources/**/*"]),
    deps = ["@commons-io//jar"],
)

genrule2(
    name = "gr-delete-repo-static",
    srcs = [":gr-delete-repo"],
    outs = ["gr-delete-repo-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :gr-delete-repo) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

polygerrit_plugin(
    name = "gr-delete-repo",
    srcs = glob([
        "gr-delete-repo/*.html",
        "gr-delete-repo/*.js",
    ]),
    app = "plugin.html",
)

junit_tests(
    name = "delete_project_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["delete-project"],
    deps = [
        ":delete-project__plugin_test_deps",
    ],
)

java_library(
    name = "delete-project__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":delete-project__plugin",
        "@commons-io//jar",
        "@mockito//jar",
    ],
)
