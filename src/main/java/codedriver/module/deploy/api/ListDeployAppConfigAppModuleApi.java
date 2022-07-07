package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/24 10:09 上午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppModuleApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统模块列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/module/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "envIdList", type = ApiParamType.JSONARRAY, desc = "环境id列表")
    })
    @Output({
            @Param(name = "appModuleVoList", explode = DeployAppModuleVo[].class, desc = "发布应用配置的应用系统模块列表")
    })
    @Description(desc = "查询发布应用配置的应用系统模块列表(树的模块下拉、一键发布页面根据系统环境查询模块列表)")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<CiEntityVo> moduleCiEntityList = new ArrayList<>();
        List<DeployAppModuleVo> returnAppModuleVoList = new ArrayList<>();

        List<Long> envIdList = null;
        JSONArray envIdArray = paramObj.getJSONArray("envIdList");
        if (CollectionUtils.isEmpty(envIdArray)) {
            //查询系统下模块列表
            //TODO 补模块简称、考虑权限问题 优化为动态sql时补充
            List<Long> idList = resourceCenterMapper.getAppSystemModuleIdListByAppSystemId(paramObj.getLong("appSystemId"), TenantContext.get().getDataDbName());
            if (CollectionUtils.isNotEmpty(idList)) {
                ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                moduleCiEntityList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(idList);
            }

            //补充模块是否有环境（有实例的环境）
            List<Long> hasEnvAppModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(paramObj.getLong("appSystemId"), moduleCiEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
            for (CiEntityVo ciEntityVo : moduleCiEntityList) {
                DeployAppModuleVo returnAppModuleVo = new DeployAppModuleVo(ciEntityVo.getId(), ciEntityVo.getName());
                returnAppModuleVoList.add(returnAppModuleVo);
                if (hasEnvAppModuleIdList.contains(ciEntityVo.getId())) {
                    returnAppModuleVo.setIsHasEnv(1);
                }
            }
        } else {
            envIdList = envIdArray.toJavaList(Long.class);

            //查出资源中心数据初始化配置信息来创建ResourceSearchGenerateSqlUtil对象
            IResourceCenterResourceCrossoverService resourceCenterResourceCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterResourceCrossoverService.class);
            List<ResourceEntityVo> resourceEntityList = resourceCenterResourceCrossoverService.getResourceEntityList();
            ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);


            List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
            String mainResourceId = "resource_appmodule";
            PlainSelect filterPlainSelect = getPlainSelectBySearchCondition(paramObj.getLong("appSystemId"), envIdList, resourceSearchGenerateSqlUtil, unavailableResourceInfoList, mainResourceId);
            String sql = filterPlainSelect.toString();
            System.out.println(sql);
            if (StringUtils.isNotBlank(sql)) {
                returnAppModuleVoList = deployAppConfigMapper.getAppModuleListByIdList(sql);
            }
        }


        return returnAppModuleVoList;
    }


    /**
     * 根据查询过滤条件，生成对应的sql语句
     *
     * @param appSystemId                   应用id
     * @param envIdList                     环境id列表
     * @param resourceSearchGenerateSqlUtil ResourceSearchGenerateSqlUtil对象
     * @param unavailableResourceInfoList   不可用的视图对象
     * @param mainResourceId                主表
     * @return
     */
    private PlainSelect getPlainSelectBySearchCondition(Long appSystemId, List<Long> envIdList, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }

        Map<String, ResourceInfo> searchConditionMappingMap = new HashMap<>();

        //appSystemId
        searchConditionMappingMap.put("app_system_id", new ResourceInfo("resource_appmodule_appsystem", "app_system_id", false));
        ResourceInfo appSystemResourceInfo = searchConditionMappingMap.get("app_system_id");
        if (resourceSearchGenerateSqlUtil.additionalInformation(appSystemResourceInfo)) {
            Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(appSystemResourceInfo, plainSelect);
            EqualsTo equalsTo = new EqualsTo().withLeftExpression(column).withRightExpression(new LongValue(appSystemId));
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(equalsTo);
            } else {
                plainSelect.setWhere(equalsTo);
            }
        } else {
            unavailableResourceInfoList.add(appSystemResourceInfo);
        }
//
//        //中间表
//        searchConditionMappingMap.put("env_resource_id", new ResourceInfo("resource_softwareservice_env", "resource_id", false));
//        searchConditionMappingMap.put("module_resource_id", new ResourceInfo("resource_ipobject_appmodule", "resource_id", false));
//        ResourceInfo appModuleResourceInfo = searchConditionMappingMap.get("module_resource_id");
//        ResourceInfo envResourceInfo = searchConditionMappingMap.get("env_resource_id");
//        if (resourceSearchGenerateSqlUtil.additionalInformation(appModuleResourceInfo) && resourceSearchGenerateSqlUtil.additionalInformation(envResourceInfo)) {
//            Column appModuleResourceColumn = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(appModuleResourceInfo, plainSelect);
//            Column envResourceColumn = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(envResourceInfo, plainSelect);
//
////            EqualsTo equalsTo = new EqualsTo().withLeftExpression(envResourceColumn).withRightExpression(appModuleResourceColumn);
////            Expression where = plainSelect.getWhere();
////            if (where == null) {
////                plainSelect.setWhere(equalsTo);
////            } else {
////                plainSelect.setWhere(equalsTo);
////            }
//        } else {
//            unavailableResourceInfoList.add(appSystemResourceInfo);
//        }


        //envIdList
        searchConditionMappingMap.put("env_id", new ResourceInfo("resource_softwareservice_env", "env_id", false));
        searchConditionMappingMap.put("resource_id", new ResourceInfo("resource_ipobject_appmodule", "resource_id", false));
        ResourceInfo appModuleInfo = searchConditionMappingMap.get("resource_id");
        ResourceInfo envInfo = searchConditionMappingMap.get("env_id");
        if (resourceSearchGenerateSqlUtil.additionalInformation(envInfo) && resourceSearchGenerateSqlUtil.additionalInformation(appModuleInfo)) {
            Column appModuleColumn = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(appModuleInfo, plainSelect);
            Column envColumn = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(envInfo, plainSelect);


            InExpression inExpression = new InExpression();
//            inExpression.withLeftExpression(envColumn).withRightExpression(appModuleColumn);
            inExpression.setLeftExpression(envColumn);
            ExpressionList expressionList = new ExpressionList();
            for (Long id : envIdList) {
                expressionList.addExpressions(new LongValue(id));
            }
            inExpression.setRightItemsList(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(inExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, inExpression));
            }
        } else {
            unavailableResourceInfoList.add(envInfo);
        }

        Table mainTable = (Table)plainSelect.getFromItem();
        List<OrderByElement> orderByElements = new ArrayList<>();
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.withExpression(new Column(mainTable, "id")).withAsc(true);
        orderByElements.add(orderByElement);
        plainSelect.withOrderByElements(orderByElements);
        plainSelect.withDistinct(new Distinct()).setSelectItems(Arrays.asList((new SelectExpressionItem(new Column(mainTable, "id"))),(new SelectExpressionItem(new Column(mainTable, "name")))));
//        filterPlainSelect.withLimit(new Limit().withOffset(new LongValue(searchVo.getStartNum())).withRowCount(new LongValue(searchVo.getPageSize())));



        List<ResourceInfo> theadList = new ArrayList<>();
        theadList.add(new ResourceInfo("resource_appmodule", "id"));
        theadList.add(new ResourceInfo("resource_appmodule", "name"));
        theadList.add(new ResourceInfo("resource_appmodule", "abbr_name"));
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        return plainSelect;
    }


    /**
     * 根据需要查询的列，生成对应的sql语句
     *
     * @param idList
     * @param resourceSearchGenerateSqlUtil
     * @return
     */
    public String getResourceListByIdListSql(List<Long> idList, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }
        List<ResourceInfo> theadList = new ArrayList<>();
        theadList.add(new ResourceInfo("resource_database", "ip"));
        theadList.add(new ResourceInfo("resource_database", "port"));
        theadList.add(new ResourceInfo("resource_database", "name"));
        theadList.add(new ResourceInfo("resource_database", "type_name"));
        for (ResourceInfo resourceInfo : theadList) {
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(new Column((Table) plainSelect.getFromItem(), "id"));
        ExpressionList expressionList = new ExpressionList();
        for (Object id : idList) {
            if (id instanceof Long) {
                expressionList.addExpressions(new LongValue((Long) id));
            } else if (id instanceof String) {
                expressionList.addExpressions(new StringValue((String) id));
            }
        }
        inExpression.setRightItemsList(expressionList);
        plainSelect.setWhere(inExpression);
        return plainSelect.toString();
    }
}
