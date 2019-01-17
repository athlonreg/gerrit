load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")

gerrit_plugin(
    name = "singleusergroup",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: singleusergroup",
        "Gerrit-Module: com.googlesource.gerrit.plugins.singleusergroup.SingleUserGroup$Module",
    ],
    resources = glob(["src/main/resources/**/*"]),
)

junit_tests(
    name = "singleusergroup_tests",
    srcs = glob(["src/test/java/**/*Test.java"]),
    tags = ["replication"],
    visibility = ["//visibility:public"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":singleusergroup__plugin",
    ],
)
