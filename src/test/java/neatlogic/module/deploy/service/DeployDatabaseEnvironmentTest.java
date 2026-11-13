package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
import neatlogic.framework.deploy.exception.DeployDatabaseEnvironmentConflictException;
import neatlogic.framework.dao.plugin.CompressHandler;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.crossover.ICrossoverService;
import neatlogic.framework.util.I18nUtils;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** 使用独立 MySQL 测试库验证真实 Mapper SQL、动态 SQL 与保存前校验。 */
public class DeployDatabaseEnvironmentTest {
    private final Configuration configuration = new Configuration();
    private Connection adminConnection;
    private Connection connection;
    private String testDatabase;
    private boolean databaseCreated;

    /** 未配置测试地址时跳过；已配置时在本用例的独立库中构造各种环境关联。 */
    @Before
    public void setUp() throws Exception {
        String url = System.getenv("NEATLOGIC_TEST_MYSQL_URL");
        assumeTrue("未配置 NEATLOGIC_TEST_MYSQL_URL，跳过 MySQL 数据库测试", url != null && !url.trim().isEmpty());
        assertTrue("NEATLOGIC_TEST_MYSQL_URL 必须是不带数据库名和凭据的 jdbc:mysql:// 实例地址",
                url.matches("jdbc:mysql://[^/?#@]+/?(?:\\?[^#]*)?")
                        && !url.matches("(?i).*[?&](?:user|password)=.*"));
        String username = System.getenv("NEATLOGIC_TEST_MYSQL_USERNAME");
        assertTrue("必须配置 NEATLOGIC_TEST_MYSQL_USERNAME", username != null && !username.trim().isEmpty());
        String password = System.getenv("NEATLOGIC_TEST_MYSQL_PASSWORD");
        adminConnection = DriverManager.getConnection(url, username, password == null ? "" : password);
        adminConnection.setAutoCommit(true);
        testDatabase = "nl_test_deploy_env_" + UUID.randomUUID().toString().replace("-", "");
        try (Statement statement = adminConnection.createStatement()) {
            // 不使用 IF NOT EXISTS，只有成功创建的唯一测试库才归本用例清理。
            statement.execute("CREATE DATABASE `" + testDatabase + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            databaseCreated = true;
        }
        connection = DriverManager.getConnection(url, username, password == null ? "" : password);
        connection.setCatalog(testDatabase);
        connection.setAutoCommit(true);
        configuration.getTypeAliasRegistry().registerAlias("CompressHandler", CompressHandler.class);
        String resource = "neatlogic/module/deploy/dao/mapper/DeployAppConfigMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE scence_database_ip_port_env_appmodule(id BIGINT, name VARCHAR(50), ip VARCHAR(50), port INT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.execute("CREATE TABLE cmdb_global_attr(id BIGINT, name VARCHAR(50), is_active INT, is_multiple INT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.execute("CREATE TABLE cmdb_global_attritem(id BIGINT, attr_id BIGINT, `value` VARCHAR(50)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.execute("CREATE TABLE cmdb_cientity_globalattritem(cientity_id BIGINT, attr_id BIGINT, item_id BIGINT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
            statement.execute("INSERT INTO cmdb_global_attr VALUES(10, 'app_environment', 1, 0), (11, 'other', 1, 0)");
            statement.execute("INSERT INTO cmdb_global_attritem VALUES(101, 10, 'DEV'), (102, 10, 'TEST'), (201, 11, 'OTHER')");
            statement.execute("INSERT INTO scence_database_ip_port_env_appmodule VALUES"
                    + "(1, 'db-1', '127.0.0.1', 3306), (2, 'db-2', '127.0.0.2', 3306),"
                    + "(2, 'db-2', '127.0.0.2', 3306), (3, 'db-3', '127.0.0.3', 3306),"
                    + "(4, 'db-4', '127.0.0.4', 3306), (5, 'db-5', '127.0.0.5', 3306)");
            statement.execute("INSERT INTO cmdb_cientity_globalattritem VALUES(2, 10, 101), (3, 10, 102),"
                    + "(4, 10, 101), (4, 10, 102), (5, 11, 201)");
        }
    }

    /** 初始化失败或测试结束时均关闭连接，仅删除本用例成功创建的数据库。 */
    @After
    public void tearDown() throws Exception {
        try {
            if (connection != null) connection.close();
        } finally {
            if (adminConnection != null) {
                try (Connection admin = adminConnection) {
                    if (databaseCreated) {
                        try (Statement statement = admin.createStatement()) {
                            statement.execute("DROP DATABASE `" + testDatabase + "`");
                        }
                    }
                }
            }
        }
    }

    /** 单选过滤依据真实关联，数量不受资源视图重复行影响。 */
    @Test
    public void singleSelectFiltersConflictsBeforePaging() throws Exception {
        DeployResourceSearchVo search = search(true);
        assertEquals(Arrays.asList(1L, 2L, 5L), mapperIds(search));
        assertEquals(Collections.singletonList(3L), query("getAppConfigEnvDatabaseCount", search));
        search.setNeedPage(true);
        search.setPageSize(2);
        search.setCurrentPage(1);
        assertEquals(2, mapperIds(search).size());
        search.setCurrentPage(2);
        assertEquals(1, mapperIds(search).size());
        assertEquals(Collections.singletonList(3L), query("getAppConfigEnvDatabaseCount", search));
    }

    /** 关键词和默认值不能绕过过滤，历史回显查询仍返回原数据库。 */
    @Test
    public void searchAndDefaultValuesRespectSelectionMode() throws Exception {
        DeployResourceSearchVo search = search(true);
        search.setKeyword("db-3");
        assertTrue(mapperIds(search).isEmpty());
        search.setKeyword(null);
        search.setDefaultValue(new JSONArray(Collections.singletonList(3L)));
        assertTrue(mapperIds(search).isEmpty());
        search.setOnlySelectable(false);
        assertEquals(Collections.singletonList(3L), mapperIds(search));
        assertFalse(new DeployResourceSearchVo().getOnlySelectable());
        search.setOnlySelectable(null);
        assertEquals(Collections.singletonList(3L), mapperIds(search));
    }

    /** 多选或停用应用环境属性时不限制跨环境数据库。 */
    @Test
    public void multipleAndInactiveAttributesAllowCrossEnvironment() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("UPDATE cmdb_global_attr SET is_multiple = 1 WHERE id = 10");
            assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), mapperIds(search(true)));
            assertTrue(conflicts(3L).isEmpty());
            statement.execute("UPDATE cmdb_global_attr SET is_multiple = 0, is_active = 0 WHERE id = 10");
            assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), mapperIds(search(true)));
            assertTrue(conflicts(3L).isEmpty());
        }
    }

    /** 动态构建器与 Mapper 对每种配置返回一致结果，并保留已有 OR 条件语义。 */
    @Test
    public void dynamicSqlMatchesMapperFiltering() throws Exception {
        for (boolean onlySelectable : Arrays.asList(false, true)) {
            for (int mode = 0; mode < 3; mode++) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("UPDATE cmdb_global_attr SET is_active = " + (mode == 2 ? 0 : 1)
                            + ", is_multiple = " + (mode == 1 ? 1 : 0) + " WHERE id = 10");
                }
                DeployResourceSearchVo search = search(onlySelectable);
                PlainSelect select = (PlainSelect) ((Select) CCJSqlParserUtil.parse(
                        "SELECT DISTINCT a.id FROM scence_database_ip_port_env_appmodule a WHERE a.name LIKE 'db-%' OR a.ip = 'unused'"
                )).getSelectBody();
                new DeployResourceBuildSqlServiceImpl().addSelectableDatabaseFilter(select, new Column("a.id"), search);
                List<Long> dynamicIds = new ArrayList<>();
                try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(select.toString())) {
                    while (rows.next()) dynamicIds.add(rows.getLong(1));
                }
                Collections.sort(dynamicIds);
                assertEquals(mapperIds(search), dynamicIds);
            }
        }
    }

    /** 选择后新增其他环境关联时，保存校验重新读取数据并返回领域异常。 */
    @Test
    public void saveValidationRechecksChangedAssociations() throws Exception {
        DeployEnvironmentWriter writer = new DeployEnvironmentWriter();
        Field mapperField = DeployEnvironmentWriter.class.getDeclaredField("deployAppConfigMapper");
        mapperField.setAccessible(true);
        mapperField.set(writer, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{DeployAppConfigMapper.class},
                (proxy, method, args) -> {
                    assertEquals("getDatabaseConflictingEnvList", method.getName());
                    assertEquals(101L, args[1]);
                    return conflicts((Long) args[0]);
                }));
        CiEntityVo database = new CiEntityVo();
        database.setId(2L);
        database.setName("db-2");
        ResourceVo env = new ResourceVo();
        env.setId(101L);
        env.setName("DEV");
        writer.checkDatabaseEnvironment(database, env);
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO cmdb_cientity_globalattritem VALUES(2, 10, 102)");
        }
        DeployDatabaseEnvironmentConflictException failure = assertThrows(DeployDatabaseEnvironmentConflictException.class,
                () -> writer.checkDatabaseEnvironment(database, env));
        assertTrue(failure.getMessage().contains("db-2"));
        assertTrue(failure.getMessage().contains("TEST"));
        assertTrue(failure.getMessage().contains("DEV"));
        assertFalse(failure.getMessage().contains("nfde."));
        assertEquals("TEST", conflicts(2L).get(0).getEnvName());
    }

    /** 实际保存入口在冲突时终止，不进入 Schema 写入或 CMDB 修改。 */
    @Test
    @SuppressWarnings("unchecked")
    public void saveRejectsConflictBeforeAnyWrite() throws Exception {
        DeployEnvironmentWriter writer = new DeployEnvironmentWriter();
        Field mapperField = DeployEnvironmentWriter.class.getDeclaredField("deployAppConfigMapper");
        mapperField.setAccessible(true);
        mapperField.set(writer, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{DeployAppConfigMapper.class},
                (proxy, method, args) -> {
                    // 任何写入或后续校验均意味着环境冲突没有在入口处被阻止。
                    assertEquals("getDatabaseConflictingEnvList", method.getName());
                    return conflicts((Long) args[0]);
                }));
        Field authorityField = DeployEnvironmentWriter.class.getDeclaredField("deployAppAuthorityService");
        authorityField.setAccessible(true);
        authorityField.set(writer, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{DeployAppAuthorityService.class},
                (proxy, method, args) -> null));
        Field apiField = CrossoverServiceFactory.class.getDeclaredField("apiMap");
        apiField.setAccessible(true);
        Map<Class<? extends ICrossoverService>, ICrossoverService> apiMap =
                (Map<Class<? extends ICrossoverService>, ICrossoverService>) apiField.get(null);
        Map<Class<? extends ICrossoverService>, ICrossoverService> previous = new HashMap<>(apiMap);
        try {
            apiMap.clear();
            apiMap.put(ICiEntityCrossoverMapper.class, (ICrossoverService) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ICiEntityCrossoverMapper.class}, (proxy, method, args) -> {
                        assertEquals("getCiEntityBaseInfoById", method.getName());
                        CiEntityVo ciEntity = new CiEntityVo();
                        ciEntity.setId((Long) args[0]);
                        ciEntity.setName("db-3");
                        return ciEntity;
                    }));
            apiMap.put(IResourceCrossoverMapper.class, (ICrossoverService) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IResourceCrossoverMapper.class}, (proxy, method, args) -> {
                        assertEquals("getAppEnvById", method.getName());
                        ResourceVo env = new ResourceVo();
                        env.setId(101L);
                        env.setName("DEV");
                        return env;
                    }));
            JSONObject input = new JSONObject();
            input.put("appSystemId", 20L);
            input.put("appModuleId", 21L);
            input.put("envId", 101L);
            input.put("dbResourceId", 3L);
            assertThrows(DeployDatabaseEnvironmentConflictException.class, () -> writer.saveSchema(input));
        } finally {
            apiMap.clear();
            apiMap.putAll(previous);
        }
    }

    /** 中英文均插入数据库及环境上下文，不向调用方泄露未翻译的 key。 */
    @Test
    public void conflictMessagesIncludeContextInBothLanguages() {
        for (Locale locale : Arrays.asList(Locale.CHINESE, Locale.ENGLISH)) {
            String message = I18nUtils.getStaticMessage(locale, "nfde.deploydatabaseenvironmentconflictexception.message",
                    "db-3", 3L, "TEST", "102", "DEV", 101L);
            for (String context : Arrays.asList("db-3", "TEST", "102", "DEV", "101")) {
                assertTrue(message, message.contains(context));
            }
            assertFalse(message.contains("nfde."));
        }
    }

    /** 创建默认查询条件，分页场景由测试单独开启。 */
    private DeployResourceSearchVo search(boolean onlySelectable) {
        DeployResourceSearchVo search = new DeployResourceSearchVo();
        search.setEnvId(101L);
        search.setOnlySelectable(onlySelectable);
        search.setNeedPage(false);
        return search;
    }

    /** 排序比较结果，不要求业务接口承诺额外排序规则。 */
    private List<Long> mapperIds(DeployResourceSearchVo search) throws Exception {
        List<Long> ids = query("getAppConfigEnvDatabaseResourceIdList", search);
        Collections.sort(ids);
        return ids;
    }

    /** 执行真实 MyBatis 参数绑定后的数量或 ID 查询。 */
    private List<Long> query(String name, Object parameter) throws Exception {
        MappedStatement mapped = configuration.getMappedStatement(DeployAppConfigMapper.class.getName() + "." + name);
        BoundSql bound = mapped.getBoundSql(parameter);
        try (PreparedStatement statement = connection.prepareStatement(bound.getSql().replace("@{DATA_SCHEMA}.", ""))) {
            new DefaultParameterHandler(mapped, parameter, bound).setParameters(statement);
            try (ResultSet rows = statement.executeQuery()) {
                List<Long> result = new ArrayList<>();
                while (rows.next()) result.add(rows.getLong(1));
                return result;
            }
        }
    }

    /** 读取与候选过滤共用 SQL 片段的冲突环境及展示名称。 */
    private List<ResourceVo> conflicts(Long databaseId) throws Exception {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("dbResourceId", databaseId);
        parameter.put("envId", 101L);
        MappedStatement mapped = configuration.getMappedStatement(DeployAppConfigMapper.class.getName() + ".getDatabaseConflictingEnvList");
        BoundSql bound = mapped.getBoundSql(parameter);
        try (PreparedStatement statement = connection.prepareStatement(bound.getSql())) {
            new DefaultParameterHandler(mapped, parameter, bound).setParameters(statement);
            try (ResultSet rows = statement.executeQuery()) {
                List<ResourceVo> result = new ArrayList<>();
                while (rows.next()) {
                    ResourceVo env = new ResourceVo();
                    env.setEnvId(rows.getLong("envId"));
                    env.setEnvName(rows.getString("envName"));
                    result.add(env);
                }
                return result;
            }
        }
    }
}
