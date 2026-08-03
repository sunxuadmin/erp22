# 2026-07-31 TEST顺序镜像构建

## 状态

- 需求：`REQ-007`、`REQ-010`
- 当前状态：`ARCHIVED / SUPERSEDED_BY_0.1.9 / BUILD_CHAIN_CLOSED`
- 用户已授权：在TEST通过正式入口顺序构建CREHN镜像；仅此 `build-local` 动作使用已验证的 `sudo -n`
- 容器启动、SQL、部署、生产操作、Git提交和推送：未授权

## 目标

在229 TEST主机使用已暂存的源码和真实运行环境文件，按 `db → backend → web` 顺序构建CREHN镜像，生成不可变镜像归档与发布清单，不启动任何CREHN容器。

## 范围

- `deploy/scripts/invoke-deployment.ps1`：仅对TEST `build-local` 的远端执行增加 `sudo -n` 前缀；
- `deploy/linux/install-assets.sh`：仅在TEST源码暂存时规范化部署入口脚本为LF，修复229实际回报的CRLF shebang阻断；
- 使用确认串 `BUILD:crehn-test:0.1.0-test` 执行 `BuildLocal`；
- 回读发布产物、归档校验、镜像标签和CREHN/项目A运行状态。

## 禁止范围

- 不扩展至 `deploy-local`、数据库、回滚、备份、生产或其他远端动作；
- 不启动、停止、重建或部署任何容器；
- 不执行SQL，不提交或推送Git，不读取或输出运行环境中的秘密。

## 验收

- [x] TEST `build-local` 仅以 `sudo -n` 运行；
- [ ] `db`、`backend`、`web` 顺序构建完成（被Docker Hub连接超时阻断）；
- [ ] 发布目录包含已校验归档、清单与 `SUCCESS` 标记（未生成）；
- [x] 项目A保持健康；

## 实际变更与证据

首次构建在镜像步骤前退出127：229实际读取到暂存部署入口的CRLF shebang，`/usr/bin/env` 无法执行 `bash\\r`。本机已确认当前Git对象中的同文件原始字节为LF，故将修复限制在TEST暂存落盘环节；使用同一修订重新暂存后，入口返回 `assets_staged mode=test-source`，未构建、启动容器或执行SQL。

通过正式入口执行两次 `BuildLocal`，确认串均为 `BUILD:crehn-test:0.1.0-test`。两次均已通过Compose配置检查并输出 `sequential_build_started`，随后在第一个 `db` 镜像的 `FROM mariadb:11.4.5` 元数据读取阶段失败：Docker无法在10秒内连接 `https://auth.docker.io/token` 获取匿名令牌。第一次超时目标为IPv4地址，第二次为IPv6地址；两次返回码均为1。失败发生在任何应用镜像构建完成及发布目录创建之前，未生成发布归档、清单或 `SUCCESS` 标记。

构建后执行229只读预检：主机为4核、6063 MiB内存、4095 MiB Swap、根分区可用40695 MiB；Docker 29.6.2、Compose 5.3.1；项目A `dyz-current-shadow` 的5个容器均 `running/healthy`、重启0、`OOMKilled=false`，Web和MinIO入口健康。未调用容器启动、SQL、部署、Git提交或推送。

后续只读网络诊断显示：`auth.docker.io` 的IPv4、IPv6 DNS与默认路由均存在，但两族到443端口均在10秒TCP连接超时；公共HTTPS端点返回200，`registry-1.docker.io` 同样超时。宿主机IPv4、IPv6 OUTPUT默认策略均为ACCEPT。Docker已配置HTTP/HTTPS代理且没有镜像加速器；`NO_PROXY` 不匹配 Docker Hub。以当前代理进行匿名令牌请求返回200，但BuildKit构建日志仍显示直连Docker Hub，说明构建器未继承该可用代理路径。

用户随后授权检查并配置BuildKit代理继承，且在必要时受控重启Docker。229当前Buildx默认构建器为Docker内置 `docker` 驱动（BuildKit v0.31.2）；该驱动不能传入额外BuildKit参数。尝试创建独立 `docker-container` 驱动构建器时，带逗号的 `NO_PROXY` 不能被Buildx driver option接受；移除该非必要选项后，构建器的BuildKit基础镜像在五分钟内仍无法拉取，未产生运行中的构建器容器。root默认构建器已恢复为 `default`，失败的 `crehn-test-proxy` 定义及容器状态均已精确清理。

随后以不输出代理地址或凭据的方式，将已验证可用的代理安全合并到Docker `daemon.json`、通过 `dockerd --validate` 校验并受控重启Docker；项目A五个容器均恢复 `running/healthy`、重启0、`OOMKilled=false`。再次执行正式 `BuildLocal` 仍在Docker内置BuildKit解析 `mariadb:11.4.5` 元数据时直连Docker Hub IPv6并超时，证明该修改不能解决内置BuildKit代理继承问题。配置已从本轮创建的root受限备份精确回滚，再次受控重启后项目A五个容器仍全部健康、重启0、未OOM。

注意：在首次独立构建器创建期间，发现项目A Backend和Web容器已在08:58:58至09:00:00重新创建；Docker服务日志没有对应重启记录，Docker事件不能提供调用者信息，因此无法归因。本轮未执行项目A Compose、容器启动/停止或重建命令。所有后续受控Docker重启均已实测项目A恢复健康。

后续可选方案需要用户单独选择并授权：提供已可访问且可信的Docker镜像镜像源/私有缓存仓库，或调整229的网络策略使Docker内置BuildKit可直接访问Docker Hub。完成后再单独授权构建重试。

用户指定代理 `http://192.168.2.2:7897` 后再次核验：Docker当前HTTP与HTTPS代理已与该地址完全一致；通过该代理访问 `auth.docker.io` 返回200，访问 `registry-1.docker.io/v2/` 返回预期的未认证401，连接和响应均成功，因此代理服务本身可用，不属于普通网络抖动。随后仅对单次远端 `build-local` 进程显式注入 `HTTP_PROXY`、`HTTPS_PROXY` 与 `ALL_PROXY`，不修改持久配置。该构建在 `db` 阶段持续约14分钟，相关Compose进程CPU接近0，三个CREHN镜像和发布目录均未生成，判定仍卡在BuildKit外部访问路径。

对已核对命令行的本次构建PID发送TERM后，三个目标进程均退出；末尾一次宽泛 `pgrep` 被自身命令行命中而误报仍有进程，改用不自匹配的进程检查后确认构建进程为0。最终回读：`crehn-db:0.1.0-test`、`crehn-backend:0.1.0-test`、`crehn-web:0.1.0-test` 均不存在，`/srv/crehn-test/releases/0.1.0-test` 不存在；项目A五个容器均 `running/healthy`、重启0、`OOMKilled=false`。
