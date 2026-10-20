# Centralized IAM Platform 最终调整与迭代计划

## 1. 最终决策

| 项目          | 决策                                                       |
|---------------|------------------------------------------------------------|
| 项目名称      | `centralized-iam-platform`                                 |
| Maven GroupId | `com.lavyoung`                                             |
| Java 根包     | `com.lavyoung.iam`                                         |
| 架构形态      | DDD + 六边形架构 + Maven 多模块的模块化单体                |
| 认证实现      | Spring Authorization Server，自建认证中心，不依赖 Keycloak |
| 部署形态      | 首期单个可执行 JAR；前端静态资源随应用发布                 |
| Java 基线     | 迁移阶段保持 Java 17，框架版本通过统一 BOM 管理            |
| 数据库        | PostgreSQL 与 MySQL 同等支持                               |
| 租户策略      | 首期单租户，领域模型、表结构和上下文预留 `tenant_id`       |
| 授权策略      | 粗粒度角色、权限和 scope 进入 JWT；敏感决策调用在线 PDP    |
| 前端          | React + TypeScript + Vite，实施时锁定相互兼容的稳定版本    |
| SDK           | 提供纯 Java SDK 和 Spring Boot Starter                     |

项目目标是完整掌握认证中心的领域、协议和工程实现。OAuth2/OIDC 的密码学与协议细节继续使用经过验证的 Spring Security/Spring
Authorization Server，不重复实现底层安全算法。

## 2. 目标 Maven 模块

```text
centralized-iam-platform
├── centralized-iam-shared-kernel
├── centralized-iam-api-contract
├── centralized-iam-domain
├── centralized-iam-application
├── centralized-iam-adapter-inbound
├── centralized-iam-adapter-outbound
├── centralized-iam-bootstrap
├── centralized-iam-sdk-java-core
├── centralized-iam-sdk-spring-boot-starter
└── web
```

| 模块                      | 职责                                             | 允许依赖                      |
|---------------------------|--------------------------------------------------|-------------------------------|
| `shared-kernel`           | 通用标识、基础值对象、稳定错误语义               | JDK 和少量基础库              |
| `api-contract`            | 管理 API、开放 API 的请求与响应契约              | `shared-kernel`               |
| `domain`                  | 聚合、实体、值对象、领域服务、领域事件、仓储端口 | `shared-kernel`               |
| `application`             | 用例、命令、查询、事务边界、输入与输出端口       | `domain`、`shared-kernel`     |
| `adapter-inbound`         | REST、登录、授权确认、OAuth2/OIDC 协议入口       | `api-contract`、`application` |
| `adapter-outbound`        | 数据库、缓存、密码、密钥、审计及消息适配器       | `domain`、`application`       |
| `bootstrap`               | Spring Boot 启动、配置和模块装配                 | 两类 adapter                  |
| `sdk-java-core`           | 与框架无关的令牌、用户信息及 PDP 客户端          | 必要的公开契约                |
| `sdk-spring-boot-starter` | Resource Server 与方法鉴权自动配置               | `sdk-java-core`               |

强制依赖方向：

```text
shared-kernel <- domain <- application <- adapter-inbound
                         <- adapter-outbound
api-contract  -------------------------> adapter-inbound
adapter-inbound + adapter-outbound ----> bootstrap
```

`domain` 和 `application` 不得依赖 Spring Authorization Server、Web、MyBatis、Redis 或具体数据库驱动。架构边界由 Maven 依赖和
ArchUnit 双重校验。

## 3. Java 包结构

所有生产代码统一以 `com.lavyoung.iam` 为根包。

```text
com.lavyoung.iam
├── shared
├── contract
├── domain
│   ├── identity
│   ├── credential
│   ├── clientregistration
│   ├── authentication
│   ├── authorization
│   ├── consent
│   └── audit
├── application
│   └── <context>/{command,query,port,service}
├── adapter
│   ├── inbound/{rest,oauth2,oidc,web}
│   └── outbound/{persistence,cache,crypto,audit,messaging}
└── bootstrap
```

Maven 模块负责架构隔离，模块内部包负责限界上下文和具体适配器分类。首期不为每个业务域复制一整套 Maven
模块；当某个限界上下文需要独立部署时，再从模块化单体中提取。

## 4. 核心能力范围

### 4.1 身份与认证

- 用户生命周期、账号状态、密码策略、失败锁定和凭证变更。
- 登录会话、退出、全局会话撤销和安全事件审计。
- 首期支持用户名密码；MFA、外部身份源和社交登录列入后续版本。
- 密码、客户端密钥、令牌及授权码禁止明文持久化或记录日志。

### 4.2 OAuth2 与 OIDC

- Authorization Code + PKCE。
- Client Credentials、Refresh Token 和 Token Revocation。
- OIDC Discovery、UserInfo、JWK Set、ID Token 和标准 claims。
- 不实现已不推荐的 Resource Owner Password Credentials 流程。
- Spring Authorization Server 仅存在于入站协议和外部适配器边界，领域模型不暴露框架类型。

### 4.3 权限中心

- 基础模型：用户、用户组、角色、权限、资源、角色绑定和策略。
- JWT 仅携带稳定且体积可控的 `tenant_id`、`roles`、`scope` 和粗粒度权限。
- 金额、数据归属、动态上下文等敏感授权统一调用在线 PDP。
- PDP 输入至少包括主体、租户、资源、动作和上下文；输出包括允许结果、原因码和决策标识。
- PDP 超时、异常或策略版本不一致时默认拒绝，并记录可追踪但不泄密的审计事件。

## 5. 数据与兼容性设计

- 新表统一使用 `iam_` 前缀；所有租户归属表包含 `tenant_id`，首期使用默认租户。
- 标识由应用生成，避免依赖数据库自增、序列或方言特性。
- Flyway 脚本分为公共脚本与 `mysql`、`postgresql` 方言脚本。
- SQL 禁止依赖单一数据库的分页、JSON、布尔值或时间隐式转换。
- MySQL 与 PostgreSQL 均建立 Testcontainers 集成测试流水线。
- 高频索引至少覆盖用户登录标识、`client_id`、授权状态、token 标识、用户角色和角色权限关系。
- 令牌、授权和同意数据采用明确的数据模型与序列化版本，避免直接长期持久化不可控的 Java 对象图。

## 6. 前端与部署

- `web` 包含管理控制台、登录页和授权确认页，构建产物由 `bootstrap` 模块打入最终 JAR。
- 管理端包含用户、客户端、角色权限、会话、审计和系统密钥状态等工作视图。
- 首期运行依赖为 IAM JAR 加一种数据库，不要求额外部署 Keycloak。
- Redis 作为多实例会话、撤销和限流增强能力，首期设计接口但不设为单机必需依赖。
- RocketMQ 不进入首期必需链路；审计事件先保证本地事务一致性，后续再引入 outbox 与消息发布。
- 生产环境的签名私钥通过外部文件、环境密钥或 KMS 提供，并支持 `kid` 与密钥轮换。

## 7. Java SDK

`centralized-iam-sdk-java-core` 提供令牌获取、UserInfo、令牌检查、在线授权决策、超时与错误模型，不依赖 Spring。

`centralized-iam-sdk-spring-boot-starter` 提供属性配置、Resource Server 自动配置、JWT claim 映射、PDP 客户端、方法鉴权扩展和健康检查。SDK
不依赖服务端实现模块，也不复制服端认证逻辑。

## 8. 现有模块迁移映射

| 当前模块                       | 迁移目标                                                                                                |
|--------------------------------|---------------------------------------------------------------------------------------------------------|
| `centralized-authorize-common` | 按职责拆入 `shared-kernel`、`api-contract`                                                              |
| `centralized-authorize-entity` | API DTO 进入 `api-contract`，领域对象进入 `domain`，PO 进入 outbound persistence                        |
| `centralized-authorize-server` | 拆入 `domain`、`application`、两类 adapter 和 `bootstrap`                                               |
| `centralized-authorize-client` | 原模块仅含占位入口，已在迭代 0 移除；客户端能力由 `sdk-java-core` 与 `sdk-spring-boot-starter` 重新实现 |

迁移采用并行骨架和逐用例搬迁。旧接口在入站适配器中保留有限兼容期；禁止一次性移动全部代码后再集中修复。

## 9. 迭代计划

### 迭代 0：基线与护栏

- 建立目标父 POM、模块目录、`com.lavyoung.iam` 根包和依赖管理。
- 引入 ArchUnit，锁定 domain/application/adapter/bootstrap 依赖规则。
- 补齐现有登录和发令牌流程的基线测试，清理默认跳过测试配置。

验收：全模块编译，架构违规能够使构建失败，现有核心流程有可重复基线。

### 迭代 1：共享契约与领域骨架

- 建立 TenantId、UserId、ClientId 等值对象和统一错误语义。
- 建立 identity、clientregistration、authorization、consent 聚合边界。
- 将 DTO、领域对象与数据库 PO 分离，新增 DTO/VO 使用 `record`。

验收：领域层可脱离 Spring 运行单元测试，无框架类型泄漏。

### 迭代 2：双数据库持久化

- 设计 `iam_` 表结构、索引、Flyway 迁移和 MyBatis 映射。
- 实现 MySQL/PostgreSQL 兼容仓储适配器与默认租户数据。
- 建立两种数据库的 Testcontainers 测试矩阵。

验收：同一组仓储和迁移测试在 MySQL、PostgreSQL 上通过。

### 迭代 3：身份认证闭环

- 完成用户管理、密码策略、账号锁定、登录、退出、会话撤销和审计。
- 建立统一异常映射、安全日志脱敏和基本限流。

验收：正常与异常登录、锁定、退出和撤销场景均有自动化测试。

### 迭代 4：OAuth2/OIDC 标准能力

- 完成客户端注册、Authorization Code + PKCE、Client Credentials 和 Refresh Token。
- 完成 Discovery、JWK、UserInfo、ID Token、撤销和密钥轮换。
- 增加协议成功、错误响应、重放、过期和非法 redirect URI 测试。

验收：标准客户端可以完成端到端授权，协议错误保持标准状态码与字段。

### 迭代 5：RBAC、JWT 与在线 PDP

- 完成角色、权限、资源和绑定管理。
- 实现 JWT 粗粒度权限投影、权限版本和撤销策略。
- 实现在线 decision API、审计、超时、拒绝降级和短时缓存。

验收：普通接口可离线校验，敏感接口必须经过 PDP，拒绝路径完整可审计。

### 迭代 6：React 管理端

- 完成登录授权页面与用户、客户端、角色权限、会话、审计管理界面。
- 接入 PKCE、CSRF 防护、表单校验和权限控制。
- 将前端产物集成到单 JAR 构建。

验收：管理员可通过浏览器完成完整配置，登录和授权流程在桌面与移动视口可用。

### 迭代 7：Java SDK

- 发布 core SDK 与 Spring Boot Starter。
- 提供示例 Resource Server，覆盖 JWT 与 PDP 两种鉴权方式。
- 验证超时、错误映射、密钥轮换和服务不可用场景。

验收：示例业务服务只需配置 issuer、client 和 PDP 地址即可接入。

### 迭代 8：迁移与生产加固

- 完成旧包和旧模块退出、数据迁移、兼容接口下线及仓库重命名。
- 完成安全基线、性能压测、备份恢复、可观测性和运维文档。
- 验证单实例最小部署和多实例 Redis 增强部署。

验收：无 `com.archie.sso.authorize` 残留，生产构建、双数据库、SDK 示例和关键安全测试全部通过。

## 10. 全局完成标准

- 最终仅产生一个服务端可执行 JAR，不依赖 Keycloak。
- Java 生产代码根包全部为 `com.lavyoung.iam`。
- Maven 和 ArchUnit 均能阻止领域层反向依赖适配器。
- MySQL 与 PostgreSQL 的迁移、仓储和核心授权测试全部通过。
- OAuth2/OIDC 标准端点不套普通业务响应包装。
- JWT 不承载敏感动态决策所需的完整策略；敏感操作强制在线判定。
- Java SDK、Spring Boot Starter 和示例服务可独立构建发布。
- 默认配置不包含真实密钥、密码、内网地址或调试级安全日志。

## 11. 首期明确不做

- SAML、SCIM、社交登录和复杂身份联合。
- 面向多个组织的完整租户管理界面和租户计费。
- 自研密码算法、JWT 签名算法或 OAuth2 协议栈。
- 将每个限界上下文提前拆成独立微服务。
- 将 Redis、RocketMQ 或外部策略引擎设为单实例部署的强制依赖。
