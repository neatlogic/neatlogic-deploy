package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.IResourceCenterCommonGenerateSqlCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployResourceSearchVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author longrf
 * @date 2022/7/1 12:18 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigEnvDatabaseApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境且发布没配置的数据库";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/database/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = ResourceVo[].class, desc = "数据库资产列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境且发布没配置的数据库")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);

        List<ResourceVo> deployDBResourceVoList = new ArrayList<>();

        //查询当前模块的环境且发布已配置的数据库
        List<Long> DBResourceIdList = deployAppConfigMapper.getAppConfigEnvDBConfigResourceIdByAppSystemIdAndAppModuleIdAndEnvId(searchVo.getAppSystemId(), searchVo.getAppModuleId(), searchVo.getEnvId());
        if (CollectionUtils.isNotEmpty(DBResourceIdList)) {
            searchVo.setNotInIdList(DBResourceIdList);
        }

        //查出资源中心数据初始化配置信息来创建ResourceSearchGenerateSqlUtil对象
        IResourceCenterCommonGenerateSqlCrossoverService resourceCenterCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCommonGenerateSqlCrossoverService.class);
        List<ResourceEntityVo> resourceEntityList = resourceCenterCrossoverService.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);
        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        String mainResourceId = "resource_database";

        PlainSelect filterPlainSelect = getPlainSelectBySearchCondition(searchVo, resourceSearchGenerateSqlUtil, unavailableResourceInfoList, mainResourceId);
        String sql = getResourceCountSql(filterPlainSelect);
        if (StringUtils.isBlank(sql)) {
            return TableResultUtil.getResult(deployDBResourceVoList, searchVo);
        }
        int count = deployAppConfigMapper.getAppConfigEnvDatabaseCount(sql);
        if (count > 0) {
            searchVo.setRowNum(count);
            sql = getResourceIdListSql(filterPlainSelect, searchVo);
            if (StringUtils.isBlank(sql)) {
                return TableResultUtil.getResult(deployDBResourceVoList, searchVo);
            }
            List<Long> idList = deployAppConfigMapper.getAppConfigEnvDatabaseResourceIdList(sql);
            if (CollectionUtils.isEmpty(idList)) {
                return TableResultUtil.getResult(deployDBResourceVoList, searchVo);
            }
            sql = getResourceListByIdListSql(idList, resourceSearchGenerateSqlUtil, unavailableResourceInfoList, mainResourceId);
            if (StringUtils.isBlank(sql)) {
                return TableResultUtil.getResult(deployDBResourceVoList, searchVo);
            }
            deployDBResourceVoList = deployAppConfigMapper.getAppConfigEnvDatabaseResourceListByIdList(sql);
            if (CollectionUtils.isEmpty(deployDBResourceVoList)) {
                return TableResultUtil.getResult(deployDBResourceVoList, searchVo);
            }
        }
        return TableResultUtil.getResult(deployDBResourceVoList, searchVo);
    }

    /**
     * 拼装查询总数sql语句
     *
     * @param filterPlainSelect
     * @return
     */
    private String getResourceCountSql(PlainSelect filterPlainSelect) {
        Table mainTable = (Table) filterPlainSelect.getFromItem();
        filterPlainSelect.setSelectItems(Arrays.asList(new SelectExpressionItem(new Function().withName("COUNT").withDistinct(true).withParameters(new ExpressionList(Arrays.asList(new Column(mainTable, "id")))))));
        return filterPlainSelect.toString();
    }

    /**
     * 拼装查询当前页id列表sql语句
     *
     * @param filterPlainSelect
     * @param searchVo
     * @return
     */
    private String getResourceIdListSql(PlainSelect filterPlainSelect, DeployResourceSearchVo searchVo) {
        Table mainTable = (Table) filterPlainSelect.getFromItem();
        List<OrderByElement> orderByElements = new ArrayList<>();
        OrderByElement orderByElement = new OrderByElement();
        orderByElement.withExpression(new Column(mainTable, "id")).withAsc(true);
        orderByElements.add(orderByElement);
        filterPlainSelect.withOrderByElements(orderByElements);
        filterPlainSelect.withDistinct(new Distinct()).setSelectItems(Arrays.asList((new SelectExpressionItem(new Column(mainTable, "id")))));
        filterPlainSelect.withLimit(new Limit().withOffset(new LongValue(searchVo.getStartNum())).withRowCount(new LongValue(searchVo.getPageSize())));
        return filterPlainSelect.toString();
    }

    /**
     * 根据查询过滤条件，生成对应的sql语句
     *
     * @param searchVo
     * @param resourceSearchGenerateSqlUtil
     * @return
     */
    private PlainSelect getPlainSelectBySearchCondition(DeployResourceSearchVo searchVo, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }

        //defaultValue
        Map<String, ResourceInfo> searchConditionMappingMap = new HashMap<>();
        searchConditionMappingMap.put("defaultValue", new ResourceInfo("resource_database", "id", false));
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            ResourceInfo resourceInfo = searchConditionMappingMap.get("defaultValue");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (Long id : idList) {
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
                unavailableResourceInfoList.add(resourceInfo);
            }
        }

        //条件：（envId同 or moduleId is null) and (envId同 or moduleId同)
        searchConditionMappingMap.put("env_id", new ResourceInfo("resource_softwareservice_env", "env_id", false));
        searchConditionMappingMap.put("app_module_id", new ResourceInfo("resource_database_appmodule", "app_module_id", false));
        if (searchVo.getAppModuleId() != null && searchVo.getEnvId() != null) {
            ResourceInfo envResourceInfo = searchConditionMappingMap.get("env_id");
            ResourceInfo moduleResourceInfo = searchConditionMappingMap.get("app_module_id");

            if (resourceSearchGenerateSqlUtil.additionalInformation(envResourceInfo) && resourceSearchGenerateSqlUtil.additionalInformation(moduleResourceInfo)) {
                Column envColumn = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(envResourceInfo, plainSelect);
                Column moduleColumn = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(moduleResourceInfo, plainSelect);

                //条件1：（envId同 or moduleId is null)
                List<Expression> expressionList1 = new ArrayList<>();
                expressionList1.add(new EqualsTo().withLeftExpression(envColumn).withRightExpression(new LongValue(searchVo.getEnvId())));
                expressionList1.add(new IsNullExpression().withLeftExpression(moduleColumn));
                MultiOrExpression multiOrExpression = new MultiOrExpression(expressionList1);
                Expression where1 = plainSelect.getWhere();
                if (where1 == null) {
                    plainSelect.setWhere(multiOrExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where1, multiOrExpression));
                }

                //条件2： (envId同 or moduleId同)
                List<Expression> expressionList2 = new ArrayList<>();
                expressionList2.add(new EqualsTo().withLeftExpression(envColumn).withRightExpression(new LongValue(searchVo.getEnvId())));
                expressionList2.add(new EqualsTo().withLeftExpression(moduleColumn).withRightExpression(new LongValue(searchVo.getAppModuleId())));
                MultiOrExpression multiOrExpression2 = new MultiOrExpression(expressionList2);
                Expression where2 = plainSelect.getWhere();
                plainSelect.setWhere(new AndExpression(where2, multiOrExpression2));

            } else {
                unavailableResourceInfoList.add(envResourceInfo);
            }
        }

        //notInList
        searchConditionMappingMap.put("notInList", new ResourceInfo("resource_database", "id", false));
        if (CollectionUtils.isNotEmpty(searchVo.getNotInIdList())) {
            List<Long> idList = searchVo.getNotInIdList();
            ResourceInfo resourceInfo = searchConditionMappingMap.get("notInList");
            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(column);
                ExpressionList expressionList = new ExpressionList();
                for (Long id : idList) {
                    expressionList.addExpressions(new LongValue(id));
                }
                inExpression.setRightItemsList(expressionList);
                inExpression.setNot(true);
                Expression where = plainSelect.getWhere();
                if (where == null) {
                    plainSelect.setWhere(inExpression);
                } else {
                    plainSelect.setWhere(new AndExpression(where, inExpression));
                }
            } else {
                unavailableResourceInfoList.add(resourceInfo);
            }
        }

        //keyword
        String keyword = searchVo.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            List<ResourceInfo> keywordList = new ArrayList<>();
            keywordList.add(new ResourceInfo("resource_database", "ip"));
            keywordList.add(new ResourceInfo("resource_database", "port"));
            keywordList.add(new ResourceInfo("resource_database", "name"));
            keyword = "%" + keyword + "%";
            List<Expression> expressionList = new ArrayList<>();
            for (ResourceInfo resourceInfo : keywordList) {
                if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                    Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                    expressionList.add(new LikeExpression().withLeftExpression(column).withRightExpression(new StringValue(keyword)));
                } else {
                    unavailableResourceInfoList.add(resourceInfo);
                }
            }
            MultiOrExpression multiOrExpression = new MultiOrExpression(expressionList);
            Expression where = plainSelect.getWhere();
            if (where == null) {
                plainSelect.setWhere(multiOrExpression);
            } else {
                plainSelect.setWhere(new AndExpression(where, multiOrExpression));
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
