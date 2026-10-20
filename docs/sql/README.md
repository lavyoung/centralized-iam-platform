# 数据库 SQL 目录

本目录保存 IAM 数据库结构和后续升级 SQL 的唯一源文件，不在各 Maven 模块中维护重复副本。

## 目录约定

```text
docs/sql
└── migration
    ├── common       # MySQL 与 PostgreSQL 完全兼容的迁移
    ├── mysql        # MySQL 专用迁移
    └── postgresql   # PostgreSQL 专用迁移
```

`centralized-iam-adapter-outbound` 在 Maven 构建时将 `docs/sql/migration` 映射到 JAR 的
`db/migration` 目录。应用仍通过标准 Flyway classpath locations 执行迁移，源码仓库则以本目录为唯一事实源。

新增迁移时必须遵守以下规则：

1. 已发布的版本化迁移不得修改，只能新增更高版本迁移。
2. 公共迁移与方言迁移不得使用会在同一 Profile 中冲突的版本号。
3. MySQL 与 PostgreSQL 的同一业务变更应使用相同版本和描述，便于发布审计。
4. 涉及不可逆 DDL、数据回填或停机窗口时，必须在 `docs/deploy/database` 增加升级说明。
5. 不得直接修改 `target/classes/db/migration`，该目录仅为 Maven 构建产物。

当前客户端注册 V1 迁移的部署步骤见
[`docs/deploy/database/CLIENT_REGISTRATION_V1_UPGRADE.md`](../deploy/database/CLIENT_REGISTRATION_V1_UPGRADE.md)。
