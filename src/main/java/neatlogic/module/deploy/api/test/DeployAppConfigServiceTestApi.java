package neatlogic.module.deploy.api.test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceBuildSqlCrossoverService;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.deploy.dto.app.DeployAppModuleEnvVo;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.service.DeployAppConfigService;
import neatlogic.module.deploy.service.DeployResourceBuildSqlService;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployAppConfigServiceTestApi extends PrivateApiComponentBase {
    private final Logger logger = LoggerFactory.getLogger(DeployAppConfigServiceTestApi.class);

    @Resource
    private DeployAppConfigService deployAppConfigService;

    @Resource
    private DeployResourceBuildSqlService deployResourceBuildSqlService;

    @Override
    public String getName() {
        return "测试DeployAppConfigService方法";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/deployappconfigservice/test";
    }

    @Input({})
    @Output({})
    @Description(desc = "测试DeployAppConfigService方法")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        getDatabaseByIdTest();
        getAppConfigEnvDatabaseCountTest();
        getAppConfigEnvDatabaseResourceIdListTest();
        getCmdbDeployAppEnvListByAppSystemIdAndModuleIdListTest();
        getCmdbDeployAppModuleEnvListByAppSystemIdTest();
        getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListTest();
        return null;
    }

    private void getDatabaseByIdTest() {
        getDatabaseByIdTestForId();
    }
    private void getDatabaseByIdTestForId() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            String sql = deployResourceBuildSqlService.buildGetDatabaseByIdSql(1L);
            Column groupedColumn = new Column("cientity_Database.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long id = ((Number) fieldValue).longValue();
                    Long count = ((Number) rowMap.get("count")).longValue();
                    ResourceVo resourceVo = deployAppConfigService.getDatabaseById(id);
                    if (resourceVo == null || !id.equals(resourceVo.getId())) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("count", count);
                        itemObj.put("id", id);
                        itemObj.put("resourceVo", resourceVo);
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getCmdbDeployAppEnvListByAppSystemIdAndModuleIdListTest() {
        getCmdbDeployAppEnvListByAppSystemIdAndModuleIdListTestForAppSystemId();
        getCmdbDeployAppEnvListByAppSystemIdAndModuleIdListTestForAppModuleIdList();
    }

    private void getCmdbDeployAppEnvListByAppSystemIdAndModuleIdListTestForAppSystemId() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppEnvListByAppSystemIdAndModuleIdListSql(-1L, List.of(-1L));
            Column groupedColumn = new Column("cientity_APP.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long appSystemId = ((Number) fieldValue).longValue();
                    Long count = ((Number) rowMap.get("count")).longValue();
                    List<DeployAppEnvironmentVo> envList = deployAppConfigService.getCmdbDeployAppEnvListByAppSystemIdAndModuleIdList(appSystemId, null);
//                    System.out.println("envList.size() = " + envList.size() + ", count = " + count);
                    if (envList == null || envList.isEmpty()) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("envList", envList);
                        itemObj.put("count", count);
                        itemObj.put("appSystemId", appSystemId);
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getCmdbDeployAppEnvListByAppSystemIdAndModuleIdListTestForAppModuleIdList() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppEnvListByAppSystemIdAndModuleIdListSql(-1L, List.of(-1L));
            Column groupedColumn = new Column("cientity_APPComponent.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long appModuleId = ((Number) fieldValue).longValue();
                    Long count = ((Number) rowMap.get("count")).longValue();
                    List<DeployAppEnvironmentVo> envList = deployAppConfigService.getCmdbDeployAppEnvListByAppSystemIdAndModuleIdList(null, List.of(appModuleId));
//                    System.out.println("envList.size() = " + envList.size() + ", count = " + count);
                    if (envList == null || envList.isEmpty()) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("envList", envList);
                        itemObj.put("count", count);
                        itemObj.put("appModuleIdList", List.of(appModuleId));
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getCmdbDeployAppModuleEnvListByAppSystemIdTest() {
        getCmdbDeployAppModuleEnvListByAppSystemIdTestForAppSystemId();
    }

    private void getCmdbDeployAppModuleEnvListByAppSystemIdTestForAppSystemId() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppModuleEnvListByAppSystemIdSql(-1L);
//            System.out.println("sql = " + sql);
            Column groupedColumn = new Column("cientity_APP.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
//            System.out.println("groupBySql = " + groupBySql);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
//            System.out.println("mapList = " + JSONObject.toJSONString(mapList));
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long appSystemId = ((Number) fieldValue).longValue();
                    Long count = ((Number) rowMap.get("count")).longValue();
                    List<DeployAppModuleEnvVo> moduleEnvList = deployAppConfigService.getCmdbDeployAppModuleEnvListByAppSystemId(appSystemId);
//                    System.out.println("moduleEnvList = " + JSONObject.toJSONString(moduleEnvList));
                    if (moduleEnvList == null || moduleEnvList.isEmpty()) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("moduleEnvList", moduleEnvList);
                        itemObj.put("count", count);
                        itemObj.put("appSystemId", appSystemId);
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListTest() {
        getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListTestForAppSystemId();
        getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListTestForAppModuleIdList();
    }

    private void getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListTestForAppSystemId() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListSql(-1L, List.of(-1L));
//            System.out.println("sql = " + sql);
            Column groupedColumn = new Column("cientity_APP.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
//            System.out.println("groupBySql = " + groupBySql);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
//            System.out.println("mapList = " + JSONObject.toJSONString(mapList));
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long appSystemId = ((Number) fieldValue).longValue();
//                    System.out.println("appSystemId = " + appSystemId);
                    Long count = ((Number) rowMap.get("count")).longValue();
//                    System.out.println("count = " + count);
                    List<Long> appModuleIdList = new ArrayList<>();
                    List<DeployAppModuleEnvVo> allModuleEnvList = deployAppConfigService.getCmdbDeployAppModuleEnvListByAppSystemId(appSystemId);
//                    System.out.println("allModuleEnvList = " + JSONObject.toJSONString(allModuleEnvList));
                    if (allModuleEnvList != null) {
                        for (DeployAppModuleEnvVo moduleEnvVo : allModuleEnvList) {
                            if (moduleEnvVo != null && moduleEnvVo.getId() != null) {
                                appModuleIdList.add(moduleEnvVo.getId());
                            }
                        }
                    }
                    if (appModuleIdList.isEmpty()) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("count", count);
                        itemObj.put("appSystemId", appSystemId);
                        itemObj.put("appModuleIdList", appModuleIdList);
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                        continue;
                    }
//                    System.out.println("appModuleIdList = " + JSONObject.toJSONString(appModuleIdList));
                    List<DeployAppModuleEnvVo> moduleEnvList = deployAppConfigService.getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, appModuleIdList);
//                    System.out.println("moduleEnvList = " + JSONObject.toJSONString(moduleEnvList));
                    if (moduleEnvList == null || moduleEnvList.isEmpty()) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("moduleEnvList", moduleEnvList);
                        itemObj.put("count", count);
                        itemObj.put("appSystemId", appSystemId);
                        itemObj.put("appModuleIdList", appModuleIdList);
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListTestForAppModuleIdList() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            String sql = deployResourceBuildSqlService.buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListSql(-1L, List.of(-1L));
//            System.out.println("sql = " + sql);
            Column groupedColumn = new Column("cientity_APPComponent.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
//            System.out.println("groupBySql = " + groupBySql);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
//            System.out.println("mapList = " + JSONObject.toJSONString(mapList));
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long appModuleId = ((Number) fieldValue).longValue();
//                    System.out.println("appModuleId = " + appModuleId);
                    Long count = ((Number) rowMap.get("count")).longValue();
//                    System.out.println("count = " + count);
                    Long appSystemId = null;
                    String appSystemSql = deployResourceBuildSqlService.buildGetCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdListSql(null, List.of(appModuleId));
//                    System.out.println("appSystemSql = " + appSystemSql);
                    String groupByAppSystemSql = buildGroupBySql(appSystemSql, new Column("cientity_APP.id"), true);
//                    System.out.println("groupByAppSystemSql = " + groupByAppSystemSql);
                    List<Map<String, Object>> appSystemMapList = resourceCrossoverMapper.getMapListBySql(groupByAppSystemSql);
//                    System.out.println("appSystemMapList = " + JSONObject.toJSONString(appSystemMapList));
                    if (appSystemMapList != null && !appSystemMapList.isEmpty()) {
                        Object appSystemIdValue = appSystemMapList.get(0).get("fieldValue");
                        if (appSystemIdValue != null) {
                            appSystemId = ((Number) appSystemIdValue).longValue();
                        }
                    }
                    if (appSystemId == null) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("count", count);
                        itemObj.put("appSystemId", appSystemId);
                        itemObj.put("appModuleIdList", List.of(appModuleId));
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                        continue;
                    }
//                    System.out.println("appSystemId = " + appSystemId);
                    List<DeployAppModuleEnvVo> moduleEnvList = deployAppConfigService.getCmdbDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(appSystemId, List.of(appModuleId));
//                    System.out.println("moduleEnvList = " + JSONObject.toJSONString(moduleEnvList));
                    if (moduleEnvList == null || moduleEnvList.isEmpty()) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("moduleEnvList", moduleEnvList);
                        itemObj.put("count", count);
                        itemObj.put("appSystemId", appSystemId);
                        itemObj.put("appModuleIdList", List.of(appModuleId));
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getAppConfigEnvDatabaseCountTest() {
        getAppConfigEnvDatabaseCountTestForKeyword();
        getAppConfigEnvDatabaseCountTestForDefaultValue();
    }

    private void getAppConfigEnvDatabaseCountTestForKeyword() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            DeployResourceSearchVo baseSearchVo = new DeployResourceSearchVo();
            baseSearchVo.setKeyword("test");
            String sql = deployResourceBuildSqlService.buildGetAppConfigEnvDatabaseCountSql(baseSearchVo);
            Column groupedColumn = null;
            {
                groupedColumn = new Column("cmdb_441087512551424_Database.`478701787553792`");
                String groupBySql = buildGroupBySql(sql, groupedColumn);
                List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
                for (Map<String, Object> rowMap : mapList) {
                    String fieldValue = (String) rowMap.get("fieldValue");
                    if (StringUtils.isNotBlank(fieldValue)) {
                        Long count = ((Number) rowMap.get("count")).longValue();
                        DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                        searchVo.setKeyword(fieldValue);
                        int resourceCount = deployAppConfigService.getAppConfigEnvDatabaseCount(searchVo);
                        if (resourceCount <= 0) {
                            JSONObject itemObj = new JSONObject(true);
                            itemObj.put("resourceCount", resourceCount);
                            itemObj.put("count", count);
                            itemObj.put("keyword", fieldValue);
                            itemObj.put("groupedColumn", groupedColumn.toString());
                            logger.info(itemObj.toJSONString());
                        }
                    }
                }
            }
            {
                groupedColumn = new Column("cmdb_442011534499840_Database.`486022852698112`");
                String groupBySql = buildGroupBySql(sql, groupedColumn);
                List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
                for (Map<String, Object> rowMap : mapList) {
                    String fieldValue = (String) rowMap.get("fieldValue");
                    if (StringUtils.isNotBlank(fieldValue)) {
                        Long count = ((Number) rowMap.get("count")).longValue();
                        DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                        searchVo.setKeyword(fieldValue);
                        int resourceCount = deployAppConfigService.getAppConfigEnvDatabaseCount(searchVo);
                        if (resourceCount <= 0) {
                            JSONObject itemObj = new JSONObject(true);
                            itemObj.put("resourceCount", resourceCount);
                            itemObj.put("count", count);
                            itemObj.put("keyword", fieldValue);
                            itemObj.put("groupedColumn", groupedColumn.toString());
                            logger.info(itemObj.toJSONString());
                        }
                    }
                }
            }
            {
                groupedColumn = new Column("cmdb_478816686317568_Database.`478816971530240`");
                String groupBySql = buildGroupBySql(sql, groupedColumn);
                List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
                for (Map<String, Object> rowMap : mapList) {
                    Object fieldValue = rowMap.get("fieldValue");
                    if (fieldValue != null) {
                        String keyword = String.valueOf(fieldValue);
                        if (StringUtils.isNotBlank(keyword)) {
                            Long count = ((Number) rowMap.get("count")).longValue();
                            DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                            searchVo.setKeyword(keyword);
                            int resourceCount = deployAppConfigService.getAppConfigEnvDatabaseCount(searchVo);
                            if (resourceCount <= 0) {
                                JSONObject itemObj = new JSONObject(true);
                                itemObj.put("resourceCount", resourceCount);
                                itemObj.put("count", count);
                                itemObj.put("keyword", keyword);
                                itemObj.put("groupedColumn", groupedColumn.toString());
                                logger.info(itemObj.toJSONString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getAppConfigEnvDatabaseCountTestForDefaultValue() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            DeployResourceSearchVo baseSearchVo = new DeployResourceSearchVo();
            baseSearchVo.setDefaultValue(new JSONArray().fluentAdd(12345L));
            String sql = deployResourceBuildSqlService.buildGetAppConfigEnvDatabaseCountSql(baseSearchVo);
            Column groupedColumn = new Column("cientity_Database.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long id = ((Number) fieldValue).longValue();
                    Long count = ((Number) rowMap.get("count")).longValue();
                    DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                    searchVo.setDefaultValue(new JSONArray().fluentAdd(id));
                    int resourceCount = deployAppConfigService.getAppConfigEnvDatabaseCount(searchVo);
                    if (resourceCount <= 0) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("resourceCount", resourceCount);
                        itemObj.put("count", count);
                        itemObj.put("defaultValue", id);
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getAppConfigEnvDatabaseResourceIdListTest() {
        getAppConfigEnvDatabaseResourceIdListForKeyword();
        getAppConfigEnvDatabaseResourceIdListTestForDefaultValue();
    }

    private void getAppConfigEnvDatabaseResourceIdListForKeyword() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            DeployResourceSearchVo baseSearchVo = new DeployResourceSearchVo();
            baseSearchVo.setKeyword("test");
            String sql = deployResourceBuildSqlService.buildGetAppConfigEnvDatabaseResourceIdListSql(baseSearchVo);
            Column groupedColumn = null;
            {
                groupedColumn = new Column("cmdb_441087512551424_Database.`478701787553792`");
                String groupBySql = buildGroupBySql(sql, groupedColumn);
                List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
                for (Map<String, Object> rowMap : mapList) {
                    String fieldValue = (String) rowMap.get("fieldValue");
                    if (StringUtils.isNotBlank(fieldValue)) {
                        Long count = ((Number) rowMap.get("count")).longValue();
                        DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                        searchVo.setKeyword(fieldValue);
                        List<Long> idList = deployAppConfigService.getAppConfigEnvDatabaseResourceIdList(searchVo);
                        if (idList == null || idList.isEmpty()) {
                            JSONObject itemObj = new JSONObject(true);
                            itemObj.put("idList", idList);
                            itemObj.put("count", count);
                            itemObj.put("keyword", fieldValue);
                            itemObj.put("groupedColumn", groupedColumn.toString());
                            logger.info(itemObj.toJSONString());
                        }
                    }
                }
            }
            {
                groupedColumn = new Column("cmdb_442011534499840_Database.`486022852698112`");
                String groupBySql = buildGroupBySql(sql, groupedColumn);
                List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
                for (Map<String, Object> rowMap : mapList) {
                    String fieldValue = (String) rowMap.get("fieldValue");
                    if (StringUtils.isNotBlank(fieldValue)) {
                        Long count = ((Number) rowMap.get("count")).longValue();
                        DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                        searchVo.setKeyword(fieldValue);
                        List<Long> idList = deployAppConfigService.getAppConfigEnvDatabaseResourceIdList(searchVo);
                        if (idList == null || idList.isEmpty()) {
                            JSONObject itemObj = new JSONObject(true);
                            itemObj.put("idList", idList);
                            itemObj.put("count", count);
                            itemObj.put("keyword", fieldValue);
                            itemObj.put("groupedColumn", groupedColumn.toString());
                            logger.info(itemObj.toJSONString());
                        }
                    }
                }
            }
            {
                groupedColumn = new Column("cmdb_478816686317568_Database.`478816971530240`");
                String groupBySql = buildGroupBySql(sql, groupedColumn);
                List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
                for (Map<String, Object> rowMap : mapList) {
                    Object fieldValue = rowMap.get("fieldValue");
                    if (fieldValue != null) {
                        String keyword = String.valueOf(fieldValue);
                        if (StringUtils.isNotBlank(keyword)) {
                            Long count = ((Number) rowMap.get("count")).longValue();
                            DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                            searchVo.setKeyword(keyword);
                            List<Long> idList = deployAppConfigService.getAppConfigEnvDatabaseResourceIdList(searchVo);
                            if (idList == null || idList.isEmpty()) {
                                JSONObject itemObj = new JSONObject(true);
                                itemObj.put("idList", idList);
                                itemObj.put("count", count);
                                itemObj.put("keyword", keyword);
                                itemObj.put("groupedColumn", groupedColumn.toString());
                                logger.info(itemObj.toJSONString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getAppConfigEnvDatabaseResourceIdListTestForDefaultValue() {
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        try {
            IResourceBuildSqlCrossoverService resourceBuildSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceBuildSqlCrossoverService.class);
            DeployResourceSearchVo baseSearchVo = new DeployResourceSearchVo();
            baseSearchVo.setDefaultValue(new JSONArray().fluentAdd(12345L));
            String sql = deployResourceBuildSqlService.buildGetAppConfigEnvDatabaseResourceIdListSql(baseSearchVo);
            Column groupedColumn = new Column("cientity_Database.id");
            String groupBySql = buildGroupBySql(sql, groupedColumn);
            List<Map<String, Object>> mapList = resourceCrossoverMapper.getMapListBySql(groupBySql);
            for (Map<String, Object> rowMap : mapList) {
                Object fieldValue = rowMap.get("fieldValue");
                if (fieldValue != null) {
                    Long id = ((Number) fieldValue).longValue();
                    Long count = ((Number) rowMap.get("count")).longValue();
                    DeployResourceSearchVo searchVo = new DeployResourceSearchVo();
                    searchVo.setDefaultValue(new JSONArray().fluentAdd(id));
                    List<Long> idList = deployAppConfigService.getAppConfigEnvDatabaseResourceIdList(searchVo);
                    if (idList == null || idList.isEmpty()) {
                        JSONObject itemObj = new JSONObject(true);
                        itemObj.put("idList", idList);
                        itemObj.put("count", count);
                        itemObj.put("defaultValue", id);
                        itemObj.put("groupedColumn", groupedColumn.toString());
                        logger.info(itemObj.toJSONString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String buildGroupBySql(String sql, Column groupByColumn) throws Exception {
        return buildGroupBySql(sql, groupByColumn, false);
    }

    private String buildGroupBySql(String sql, Column groupByColumn, boolean isRetainOtherConditions) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) ((Select) statement).getSelectBody();

        List<SelectItem> selectItemList = new ArrayList<>();
        selectItemList.add(new SelectExpressionItem(groupByColumn).withAlias(new Alias("fieldValue")));
        Function function = new Function();
        function.setName("COUNT");
        function.setParameters(new ExpressionList(new LongValue(1)));
        selectItemList.add(new SelectExpressionItem(function).withAlias(new Alias("count")));
        plainSelect.setSelectItems(selectItemList);
        if (isRetainOtherConditions) {
            Expression otherConditionExpression = resetWhereGroupByColumnToTrue(plainSelect.getWhere(), groupByColumn);
            plainSelect.setWhere(new AndExpression(otherConditionExpression, new IsNullExpression().withLeftExpression(groupByColumn).withNot(true)));
        } else {
            Parenthesis expiredExpression = getWhereFirstExpiredExpression(plainSelect.getWhere());
            plainSelect.setWhere(new AndExpression(expiredExpression, new IsNullExpression().withLeftExpression(groupByColumn).withNot(true)));
        }
        GroupByElement groupByElement = new GroupByElement();
        List<Expression> groupByExpressions = new ArrayList<>();
        groupByExpressions.add(groupByColumn);
        groupByElement.addGroupByExpressions(groupByExpressions);
        plainSelect.setGroupByElement(groupByElement);

        OrderByElement orderByElement = new OrderByElement();
        orderByElement.setExpression(new Column("count"));
        orderByElement.setAsc(false);
        plainSelect.setOrderByElements(List.of(orderByElement));

        Limit limit = new Limit();
        limit.setRowCount(new LongValue(10));
        plainSelect.setLimit(limit);
        return plainSelect.toString();
    }

    private Parenthesis getWhereFirstExpiredExpression(Expression where) {
        if (where != null) {
            if (where instanceof AndExpression andExpr) {
                Expression leftExpression = andExpr.getLeftExpression();
                if (leftExpression instanceof Parenthesis parenthesis) {
                    if (parenthesis.getExpression() instanceof OrExpression orExpr) {
                        if ((orExpr.getLeftExpression() instanceof NotExpression)
                                && (orExpr.getRightExpression() instanceof ExistsExpression)) {
                            return parenthesis;
                        }
                    }
                }
                Expression rightExpression = andExpr.getRightExpression();
                if (rightExpression instanceof Parenthesis parenthesis) {
                    if (parenthesis.getExpression() instanceof OrExpression orExpr) {
                        if ((orExpr.getLeftExpression() instanceof NotExpression)
                                && (orExpr.getRightExpression() instanceof ExistsExpression)) {
                            return parenthesis;
                        }
                    }
                }
                if (leftExpression instanceof AndExpression) {
                    return getWhereFirstExpiredExpression(leftExpression);
                }
                if (rightExpression instanceof AndExpression) {
                    return getWhereFirstExpiredExpression(rightExpression);
                }
            } else if (where instanceof OrExpression orExpr) {
                Expression leftExpression = orExpr.getLeftExpression();
                if (leftExpression instanceof Parenthesis parenthesis) {
                    if (parenthesis.getExpression() instanceof OrExpression orExpr2) {
                        if ((orExpr2.getLeftExpression() instanceof NotExpression)
                                && (orExpr2.getRightExpression() instanceof ExistsExpression)) {
                            return parenthesis;
                        }
                    }
                }
                Expression rightExpression = orExpr.getRightExpression();
                if (rightExpression instanceof Parenthesis parenthesis) {
                    if (parenthesis.getExpression() instanceof OrExpression orExpr2) {
                        if ((orExpr2.getLeftExpression() instanceof NotExpression)
                                && (orExpr2.getRightExpression() instanceof ExistsExpression)) {
                            return parenthesis;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 将where条件中分组字段的条件设置为true，例如将 appSystemId = -1 设置为 true 或 1=1
     * @param where
     * @param groupByColumn
     * @return
     */
    private Expression resetWhereGroupByColumnToTrue(Expression where, Column groupByColumn) {
        if (where == null) {
            return buildTrueExpression();
        }
        if (where instanceof AndExpression andExpr) {
            return new AndExpression(
                    resetWhereGroupByColumnToTrue(andExpr.getLeftExpression(), groupByColumn),
                    resetWhereGroupByColumnToTrue(andExpr.getRightExpression(), groupByColumn)
            );
        }
        if (where instanceof OrExpression orExpr) {
            return new OrExpression(
                    resetWhereGroupByColumnToTrue(orExpr.getLeftExpression(), groupByColumn),
                    resetWhereGroupByColumnToTrue(orExpr.getRightExpression(), groupByColumn)
            );
        }
        if (where instanceof Parenthesis parenthesis) {
            return new Parenthesis(resetWhereGroupByColumnToTrue(parenthesis.getExpression(), groupByColumn));
        }
        if (isExpressionContainsColumn(where, groupByColumn)) {
            return buildTrueExpression();
        }
        return where;
    }

    private boolean isExpressionContainsColumn(Expression expression, Column column) {
        if (expression == null || column == null) {
            return false;
        }
        String expressionText = normalizeColumnExpression(expression.toString());
        String columnText = normalizeColumnExpression(column.toString());
        return StringUtils.isNotBlank(columnText) && expressionText.contains(columnText);
    }

    private String normalizeColumnExpression(String expression) {
        if (StringUtils.isBlank(expression)) {
            return StringUtils.EMPTY;
        }
        return expression.replace("`", "")
                .replace("\"", "")
                .replace("[", "")
                .replace("]", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    private Expression buildTrueExpression() {
        return new EqualsTo(new LongValue(1), new LongValue(1));
    }
}
