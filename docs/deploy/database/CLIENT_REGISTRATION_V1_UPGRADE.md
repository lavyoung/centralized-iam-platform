# 客户端注册数据库 V1 升级说明

## 适用范围

本次升级创建客户端注册聚合所需的五张新表，适用于尚未建立目标 IAM 客户端注册表的数据库：

- `iam_client_registration`
- `iam_client_authentication_method`
- `iam_client_authorization_grant_type`
- `iam_client_redirect_uri`
- `iam_client_scope`

MySQL 脚本位于 [
`docs/sql/migration/mysql/V1__create_client_registration_schema.sql`](../../sql/migration/mysql/V1__create_client_registration_schema.sql)
，PostgreSQL 脚本位于 [
`docs/sql/migration/postgresql/V1__create_client_registration_schema.sql`](../../sql/migration/postgresql/V1__create_client_registration_schema.sql)。

## 升级前检查

1. 确认目标数据库为 MySQL 8.0+ 或 PostgreSQL 16+。
2. 确认当前数据库不存在同名 IAM 表或未完成的手工建表结果。
3. 对已有数据库完成备份，并验证备份可以恢复。
4. 使用具备建表、建索引和创建外键权限的迁移账号。
5. 数据库连接与会话时区统一使用 UTC，避免 `Instant` 写入产生环境偏移。

## 自动迁移

应用启用 `mysql` Profile 时，Flyway 加载：

```text
classpath:db/migration/common
classpath:db/migration/mysql
```

应用启用 `postgresql` Profile 时，Flyway 加载：

```text
classpath:db/migration/common
classpath:db/migration/postgresql
```

部署前应先构建 `centralized-iam-adapter-outbound`，确认迁移文件已进入 JAR 的 `db/migration` 目录。生产环境不得同时加载两个方言目录。

## 约束与索引

- `(tenant_id, client_id)` 保证租户内客户端标识唯一。
- 子表通过 `(tenant_id, client_registration_id)` 复合外键引用主表，数据库直接拒绝跨租户关联。
- 认证方式、授权类型、回调地址和 scope 在单个客户端内保持唯一。
- 回调地址使用数据库生成的 SHA-256 摘要建立唯一索引，避免 MySQL 长 URI 索引超限。
- `(tenant_id, status)` 支撑租户内按状态分页查询客户端。
- 主表检查约束限制状态值和令牌有效期之间的大小关系。

## 升级后验证

1. 检查 Flyway 历史表中 V1 状态为成功。
2. 确认五张业务表、主键、唯一键和外键均已创建。
3. 插入同租户重复 `client_id`，数据库应拒绝写入。
4. 使用不同 `tenant_id` 向已存在客户端写入关联记录，数据库应拒绝写入。
5. 删除客户端主表记录，关联表记录应通过外键级联删除。

项目已提供 MySQL 与 PostgreSQL Testcontainers 集成测试。具备 Docker 的环境执行：

```bash
mvn -pl centralized-iam-adapter-outbound -am verify
```

## 回滚策略

V1 只创建新表，不迁移遗留业务数据。Flyway 不提供自动向下迁移，生产回滚应优先恢复升级前备份。

若迁移失败且确认五张表中没有业务数据，可在停止应用后按“关联表优先、主表最后”的顺序删除本次创建对象，再修复原因并重新执行迁移。不得在已有客户端数据后直接删表回滚。
