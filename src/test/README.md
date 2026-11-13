# 发布模块 MySQL 测试

`DeployDatabaseEnvironmentTest` 和商业模块的 `OnboardingDatabaseBindingTest` 使用父 POM 提供的 MySQL Connector/J 8.4.0。

运行前通过环境变量指定独立 MySQL 测试实例：

- `NEATLOGIC_TEST_MYSQL_URL`：不带数据库名或凭据的实例 URL，例如 `jdbc:mysql://127.0.0.1:3306/?useSSL=false&characterEncoding=UTF-8`。
- `NEATLOGIC_TEST_MYSQL_USERNAME`：测试账号，需有创建、删除测试库及表读写权限。
- `NEATLOGIC_TEST_MYSQL_PASSWORD`：测试账号密码；未设置时使用空密码。不要将实际密码写入仓库或命令行参数。

每个用例创建唯一数据库，表使用 InnoDB、utf8mb4 和 `utf8mb4_general_ci`。测试结束或初始化失败后，关闭连接并清理本用例创建的库。测试进程被强制终止时，应在独立测试实例中确认并清理遗留的 `nl_test_deploy_env_`、`nl_test_onboarding_binding_` 前缀数据库。

从工作区运行两个 MySQL 测试及其余接入测试：

```bash
mvn -f neatlogic-build-root/pom.xml -Pdevelop,commercial \
  -pl ../neatlogic-deploy-commercial -am \
  -Dtest=DeployDatabaseEnvironmentTest,Onboarding*Test \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

未配置测试 URL 时，两个数据库测试类明确跳过；已配置 URL 后，配置错误、连接失败、权限不足或初始化失败均使测试失败。跳过不代表 MySQL SQL 和事务断言已验证。
