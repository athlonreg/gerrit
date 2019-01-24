# Gerrit Code Review

[Gerrit](https://www.gerritcodereview.com) 是一个基于 Git 的代码审查和项目管理工具。

[![官网编译状态](https://gerrit-ci.gerritforge.com/job/Gerrit-master/badge/icon)](https://gerrit-ci.gerritforge.com/job/Gerrit-master/)

## 文档

了解更多，点击[官网文档](https://gerrit-review.googlesource.com/Documentation/index.html)查看。

## Source

官方源码托管于 [googlesource.com](https://gerrit.googlesource.com/gerrit)。

## Bug 

使用过程中发现的 Bug 请在 [issue tracker](https://bugs.chromium.org/p/gerrit/issues/list)提出。


## 许可

Gerrit 遵循 Apache License 2.0 许可。

## 编译

安装 [Bazel](https://bazel.build/versions/master/docs/install.html) 然后运行下面的命令:

```bash
git clone --recurse-submodules https://gerrit.googlesource.com/gerrit
cd gerrit && bazel build release
```

## 安装 二进制包 (Deb/Rpm)

下面的文章介绍了如何配置 GerritForge/BinTray 仓库。
[https://gitenterprise.me/2015/02/27/gerrit-2-10-rpm-and-debian-packages-available/](https://gitenterprise.me/2015/02/27/gerrit-2-10-rpm-and-debian-packages-available/)

Debian/Ubuntu:

```bash
apt-get update & apt-get install gerrit=<version>-<release>
```

CentOS/RedHat:

```bash
yum clean all && yum install gerrit-<version>[-<release>]
```

Fedora:

```bash
dnf clean all && dnf install gerrit-<version>[-<release>]
```

## Docker

Gerrit 汉化版 的 Docker 镜像 在这里 [DockerHub](https://hub.docker.com/u/canvas1996/)

CentOS:

```bash
docker run -p 8080:8080 canvas1996/gerrit-2.16.3-chinese:tagname
```

## Credits
- Gerrit Code Review team for their source code
