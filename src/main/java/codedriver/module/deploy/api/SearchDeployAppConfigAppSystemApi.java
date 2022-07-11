package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.crossover.IResourceCenterCommonGenerateSqlCrossoverService;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.deploy.dto.app.DeployAppSystemVo;
import codedriver.framework.deploy.dto.app.DeployResourceSearchVo;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.service.AuthenticationInfoService;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/20 5:13 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/search";
    }

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索-应用名|模块名"),
            @Param(name = "isFavorite", type = ApiParamType.ENUM, rule = "0,1", desc = "是否只显示已收藏的"),
            @Param(name = "isConfig", type = ApiParamType.ENUM, rule = "0,1", desc = "是否只显示已配置的"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppSystemVo[].class, desc = "发布应用配置的应用系统列表")
    })
    @Description(desc = "查询发布应用配置的应用系统列表（含关键字过滤）")
    @Override
    public Object myDoService(JSONObject paramObj) {
//        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
//        List<DeployAppConfigResourceVo> returnAppSystemList = new ArrayList<>();
//        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
//        Integer count = deployAppConfigMapper.getAppSystemIdListCount(searchVo);
//        if (count > 0) {
//            searchVo.setRowNum(count);
//
//            //查询包含关键字的 returnAppSystemList
//            List<Long> appSystemIdList = deployAppConfigMapper.getAppSystemIdList(searchVo, UserContext.get().getUserUuid());
//            if (CollectionUtils.isEmpty(appSystemIdList)) {
//                return TableResultUtil.getResult(returnAppSystemList, searchVo);
//            }
//            returnAppSystemList = deployAppConfigMapper.getAppSystemListByIdList(appSystemIdList, TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
//            Map<Long, DeployAppConfigResourceVo> returnAppSystemMap = returnAppSystemList.stream().collect(Collectors.toMap(DeployAppConfigResourceVo::getAppSystemId, e -> e));
//
//            //补充系统是否有模块
//            List<Long> hasModuleAppSystemIdList = resourceCenterMapper.getHasModuleAppSystemIdListByAppSystemIdList(returnAppSystemList.stream().map(DeployAppConfigResourceVo::getAppSystemId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
//            for (DeployAppConfigResourceVo resourceVo : returnAppSystemList) {
//                if (hasModuleAppSystemIdList.contains(resourceVo.getAppSystemId())) {
//                    resourceVo.setIsHasModule(1);
//                }
//            }
//
//            //查询包含关键字的 appSystemModuleList，再将信息模块信息补回 returnAppSystemList
//            List<Long> appSystemIdListByAppModuleName = appSystemMapper.getAppSystemIdListByAppModuleName(searchVo.getKeyword(), TenantContext.get().getDataDbName());
//            if (CollectionUtils.isNotEmpty(appSystemIdListByAppModuleName)) {
//                List<DeployAppConfigResourceVo> appSystemModuleList = deployAppConfigMapper.getAppSystemModuleListBySystemIdList(appSystemIdListByAppModuleName, paramObj.getInteger("isConfig"), TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
//                if (CollectionUtils.isNotEmpty(appSystemModuleList)) {
//                    for (DeployAppConfigResourceVo appSystemInfoVo : appSystemModuleList) {
//                        DeployAppConfigResourceVo returnAppSystemVo = returnAppSystemMap.get(appSystemInfoVo.getAppSystemId());
//                        if (returnAppSystemVo != null) {
//
//                            //补充模块是否有环境（有实例的环境）
//                            List<Long> appModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(returnAppSystemVo.getAppSystemId(),appSystemInfoVo.getAppModuleList().stream().map(DeployAppModuleVo::getId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
//                            for (DeployAppModuleVo appModuleVo : appSystemInfoVo.getAppModuleList()) {
//                                if (appModuleIdList.contains(appModuleVo.getId())) {
//                                    appModuleVo.setIsHasEnv(1);
//                                }
//                            }
//                            //补充系统下的模块列表
//                            returnAppSystemVo.setAppModuleList(appSystemInfoVo.getAppModuleList());
//                        }
//                    }
//                }
//            }
//        }
//        return TableResultUtil.getResult(returnAppSystemList, searchVo);
        List<DeployAppSystemVo> deployAppSystemList = new ArrayList<>();
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        //如果当前用户没有”自动发布管理员权限“，先查出当前用户拥有查看或编辑权限的应用系统ID列表
        if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(), DEPLOY_MODIFY.class.getSimpleName())) {
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(UserContext.get().getUserUuid(true));
            Set<Long> appSystemIdSet = deployAppConfigMapper.getViewableAppSystemIdList(authenticationInfoVo);
            if (CollectionUtils.isEmpty(appSystemIdSet)) {
                return TableResultUtil.getResult(deployAppSystemList, searchVo);
            }
            JSONArray defaultValue = new JSONArray(new ArrayList<>(appSystemIdSet));
            searchVo.setDefaultValue(defaultValue);
        }
        //只查出已收藏的
        Integer isFavorite = paramObj.getInteger("isFavorite");
        if (Objects.equals(isFavorite, 1)) {
            List<Long> idList = new ArrayList<>();
            JSONArray defaultValue = searchVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                idList = defaultValue.toJavaList(Long.class);
            }
            List<Long> userAppSystemIdList = deployAppConfigMapper.getAppConfigUserAppSystemIdList(UserContext.get().getUserUuid(), idList);
            searchVo.setDefaultValue(new JSONArray(new ArrayList<>(userAppSystemIdList)));
        }
        //只查出已配置的
        Integer isConfig = paramObj.getInteger("isConfig");
        if (Objects.equals(isConfig, 1)) {
            List<Long> idList = new ArrayList<>();
            JSONArray defaultValue = searchVo.getDefaultValue();
            if (CollectionUtils.isNotEmpty(defaultValue)) {
                idList = defaultValue.toJavaList(Long.class);
            }
            List<Long> hasPipelineAppSystemIdList = deployAppConfigMapper.getAppConfigAppSystemIdListByAppSystemIdList(idList);
            searchVo.setDefaultValue(new JSONArray(new ArrayList<>(hasPipelineAppSystemIdList)));
        }
        //查出资源中心数据初始化配置信息来创建ResourceSearchGenerateSqlUtil对象
        IResourceCenterCommonGenerateSqlCrossoverService resourceCenterCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCommonGenerateSqlCrossoverService.class);
        List<ResourceEntityVo> resourceEntityList = resourceCenterCrossoverService.getResourceEntityList();
        ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil = new ResourceSearchGenerateSqlUtil(resourceEntityList);

        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        String mainResourceId = "resource_appsystem";
        IResourceCenterCommonGenerateSqlCrossoverService resourceCenterCommonGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCommonGenerateSqlCrossoverService.class);

        List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList = new ArrayList<>();
        JSONObject commonConditionObj = new JSONObject();
        biConsumerList.add(getBiConsumerByDefaultValue(searchVo.getDefaultValue(), unavailableResourceInfoList));
        biConsumerList.add(getBiConsumerByKeyword(searchVo.getKeyword(), unavailableResourceInfoList));

        List<ResourceVo> resourceList = resourceCenterCommonGenerateSqlCrossoverService.getResourceList(mainResourceId, getTheadList(), biConsumerList, searchVo, unavailableResourceInfoList);
        if (CollectionUtils.isEmpty(resourceList)) {
            TableResultUtil.getResult(resourceList, searchVo);
        }
        List<Long> idList = new ArrayList<>();
        for (ResourceVo resourceVo : resourceList) {
            idList.add(resourceVo.getId());
            deployAppSystemList.add(new DeployAppSystemVo(resourceVo.getId(), resourceVo.getName(), resourceVo.getAbbrName()));
        }
//        PlainSelect filterPlainSelect = getPlainSelectBySearchCondition(searchVo, resourceSearchGenerateSqlUtil, unavailableResourceInfoList, mainResourceId);
//        String sql = getResourceCountSql(filterPlainSelect);
//        if (StringUtils.isBlank(sql)) {
//            return TableResultUtil.getResult(deployAppSystemList, searchVo);
//        }
//        int count = deployAppConfigMapper.getAppSystemCountNew(sql);
//        if (count == 0) {
//            return TableResultUtil.getResult(deployAppSystemList, searchVo);
//        }
//        searchVo.setRowNum(count);
//        sql = getResourceIdListSql(filterPlainSelect, searchVo);
//        if (StringUtils.isBlank(sql)) {
//            return TableResultUtil.getResult(deployAppSystemList, searchVo);
//        }
//        List<Long> idList = deployAppConfigMapper.getAppSystemIdListNew(sql);
//        if (CollectionUtils.isEmpty(idList)) {
//            return TableResultUtil.getResult(deployAppSystemList, searchVo);
//        }
//        sql = getResourceListByIdListSql(idList, resourceSearchGenerateSqlUtil, unavailableResourceInfoList, mainResourceId);
//        if (StringUtils.isBlank(sql)) {
//            return TableResultUtil.getResult(deployAppSystemList, searchVo);
//        }
//        deployAppSystemList = deployAppConfigMapper.getAppSystemListByIdListNew(sql);
//        if (CollectionUtils.isEmpty(deployAppSystemList)) {
//            return TableResultUtil.getResult(deployAppSystemList, searchVo);
//        }
        //补充是否有配置流水线
        List<Long> hasPipelineAppSystemIdList = deployAppConfigMapper.getAppConfigAppSystemIdListByAppSystemIdList(idList);
        //补充是否有已收藏
        List<Long> userAppSystemIdList = deployAppConfigMapper.getAppConfigUserAppSystemIdList(UserContext.get().getUserUuid(), idList);
        JSONArray defaultValue = new JSONArray(new ArrayList<>(idList));
        searchVo.setDefaultValue(defaultValue);
        //如果是模糊搜索，补充模块信息
        Map<Long, List<DeployAppModuleVo>> appSystemModuleListMap = new HashMap<>();
        String keyword = searchVo.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            List<DeployAppSystemVo> list = getAppSystemModuleListSql(searchVo, resourceSearchGenerateSqlUtil, unavailableResourceInfoList);
            appSystemModuleListMap = list.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getAppModuleList()));
        }

        //补充系统是否有模块
        List<DeployAppSystemVo> list = getAppSystemModuleCountSql(idList, resourceSearchGenerateSqlUtil, unavailableResourceInfoList, mainResourceId);
        Map<Long, Integer> appSystemModuleCountMap = list.stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getModuleCount()));
        for (DeployAppSystemVo deployAppSystemVo : deployAppSystemList) {
            Long id = deployAppSystemVo.getId();
            if (hasPipelineAppSystemIdList.contains(id)) {
                deployAppSystemVo.setIsConfig(1);
            }
            if (userAppSystemIdList.contains(id)) {
                deployAppSystemVo.setIsFavorite(1);
            }
            deployAppSystemVo.setAppModuleList(appSystemModuleListMap.get(id));
            deployAppSystemVo.setModuleCount(appSystemModuleCountMap.get(id));
        }
        return TableResultUtil.getResult(deployAppSystemList, searchVo);
    }

    /**
     * 查询应用的模块个数
     * @param idList
     * @param resourceSearchGenerateSqlUtil
     * @param unavailableResourceInfoList
     * @param mainResourceId
     * @return
     */
    private List<DeployAppSystemVo> getAppSystemModuleCountSql(
            List<Long> idList,
            ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil,
            List<ResourceInfo> unavailableResourceInfoList,
            String mainResourceId) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }
        List<SelectItem> selectItems = new ArrayList<>();
        List<Expression> groupByExpressions = new ArrayList<>();
        ResourceInfo idResourceInfo = new ResourceInfo("resource_appsystem","id", false);
        if (resourceSearchGenerateSqlUtil.additionalInformation(idResourceInfo)) {
            Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(idResourceInfo, plainSelect);
            groupByExpressions.add(column);
            selectItems.add(new SelectExpressionItem(column).withAlias(new Alias("id")));
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
            unavailableResourceInfoList.add(idResourceInfo);
        }
        ResourceInfo appModuleIdResourceInfo = new ResourceInfo("resource_appsystem_appmodule", "app_module_id", false);
        if (resourceSearchGenerateSqlUtil.additionalInformation(appModuleIdResourceInfo)) {
            Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(appModuleIdResourceInfo, plainSelect);
            selectItems.add(new SelectExpressionItem(new Function().withName("COUNT").withDistinct(true).withParameters(new ExpressionList(Arrays.asList(column)))).withAlias(new Alias("moduleCount")));
        } else {
            unavailableResourceInfoList.add(appModuleIdResourceInfo);
        }
        GroupByElement groupByElement = new GroupByElement();
        groupByElement.withGroupByExpressions(groupByExpressions);
        plainSelect.setGroupByElement(groupByElement);
        plainSelect.setSelectItems(selectItems);
        return deployAppConfigMapper.getAppSystemListByIdListNew(plainSelect.toString());
    }

    /**
     * 查询应用的模块列表
     * @param searchVo
     * @param resourceSearchGenerateSqlUtil
     * @param unavailableResourceInfoList
     * @return
     */
    private List<DeployAppSystemVo> getAppSystemModuleListSql(DeployResourceSearchVo searchVo, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList) {
        List<DeployAppSystemVo> resultList = new ArrayList<>();
        //        List<ResourceInfo> unavailableResourceInfoList = new ArrayList<>();
        IResourceCenterCommonGenerateSqlCrossoverService resourceCenterCommonGenerateSqlCrossoverService = CrossoverServiceFactory.getApi(IResourceCenterCommonGenerateSqlCrossoverService.class);
//        JSONObject paramObj = (JSONObject) JSONObject.toJSON(searchVo);
        List<BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>> biConsumerList = new ArrayList<>();
        biConsumerList.add(getBiConsumerByDefaultValue(searchVo.getDefaultValue(), unavailableResourceInfoList));
        biConsumerList.add(getBiConsumerByKeyword2(searchVo.getKeyword(), unavailableResourceInfoList));

        List<ResourceInfo> theadList = new ArrayList<>();
        theadList.add(new ResourceInfo("resource_appsystem", "id"));
        theadList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_id"));
        theadList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_name"));
        theadList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_abbr_name"));
        String sql  = resourceCenterCommonGenerateSqlCrossoverService.getResourceListSql("resource_appsystem", theadList, biConsumerList, unavailableResourceInfoList);
        if (StringUtils.isNotBlank(sql)) {
            return deployAppConfigMapper.getAppSystemListByIdListNew(sql);
        }
        return new ArrayList<>();
//        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
//        if (plainSelect == null) {
//            return null;
//        }
//        JSONArray defaultValue = searchVo.getDefaultValue();
//        if (CollectionUtils.isNotEmpty(defaultValue)) {
//            List<Long> idList = defaultValue.toJavaList(Long.class);
//            ResourceInfo resourceInfo = new ResourceInfo("resource_appsystem","id", false);
//            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
//                Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
//                InExpression inExpression = new InExpression();
//                inExpression.setLeftExpression(column);
//                ExpressionList expressionList = new ExpressionList();
//                for (Long id : idList) {
//                    expressionList.addExpressions(new LongValue(id));
//                }
//                inExpression.setRightItemsList(expressionList);
//                Expression where = plainSelect.getWhere();
//                if (where == null) {
//                    plainSelect.setWhere(inExpression);
//                } else {
//                    plainSelect.setWhere(new AndExpression(where, inExpression));
//                }
//            } else {
//                unavailableResourceInfoList.add(resourceInfo);
//            }
//        }
//        {
//            ResourceInfo resourceInfo = new ResourceInfo("resource_appsystem_appmodule", "app_module_id", false);
//            if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
//                resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
//            } else {
//                unavailableResourceInfoList.add(resourceInfo);
//            }
//        }
//        String keyword = searchVo.getKeyword();
//        if (StringUtils.isNotBlank(keyword)) {
//            List<ResourceInfo> keywordList = new ArrayList<>();
//            keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_name"));
//            keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_abbr_name"));
//            keyword = "%" + keyword + "%";
//            List<Expression> expressionList = new ArrayList<>();
//            for (ResourceInfo resourceInfo : keywordList) {
//                if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
//                    Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
//                    expressionList.add(new LikeExpression().withLeftExpression(column).withRightExpression(new StringValue(keyword)));
//                } else {
//                    unavailableResourceInfoList.add(resourceInfo);
//                }
//            }
//            MultiOrExpression multiOrExpression = new MultiOrExpression(expressionList);
//            Expression where = plainSelect.getWhere();
//            if (where == null) {
//                plainSelect.setWhere(multiOrExpression);
//            } else {
//                plainSelect.setWhere(new AndExpression(where, multiOrExpression));
//            }
//        }
//        return plainSelect.toString();
    }

    /**
     * 拼装查询总数sql语句
     * @param filterPlainSelect
     * @return
     */
    private String getResourceCountSql(PlainSelect filterPlainSelect) {
        Table mainTable = (Table)filterPlainSelect.getFromItem();
        filterPlainSelect.setSelectItems(Arrays.asList(new SelectExpressionItem(new Function().withName("COUNT").withDistinct(true).withParameters(new ExpressionList(Arrays.asList(new Column(mainTable, "id")))))));
        return filterPlainSelect.toString();
    }

    /**
     * 拼装查询当前页id列表sql语句
     * @param filterPlainSelect
     * @param searchVo
     * @return
     */
    private String getResourceIdListSql(PlainSelect filterPlainSelect, DeployResourceSearchVo searchVo) {
        Table mainTable = (Table)filterPlainSelect.getFromItem();
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
     * @param searchVo
     * @param resourceSearchGenerateSqlUtil
     * @return
     */
    private PlainSelect getPlainSelectBySearchCondition(DeployResourceSearchVo searchVo, ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, List<ResourceInfo> unavailableResourceInfoList, String mainResourceId) {
        PlainSelect plainSelect = resourceSearchGenerateSqlUtil.initPlainSelectByMainResourceId(mainResourceId);
        if (plainSelect == null) {
            return null;
        }

        Map<String, ResourceInfo> searchConditionMappingMap = new HashMap<>();
        searchConditionMappingMap.put("defaultValue", new ResourceInfo("resource_appsystem","id", false));
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
        String keyword = searchVo.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            List<ResourceInfo> keywordList = new ArrayList<>();
            keywordList.add(new ResourceInfo("resource_appsystem", "name"));
            keywordList.add(new ResourceInfo("resource_appsystem", "abbr_name"));
            keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_name"));
            keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_abbr_name"));
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
        theadList.add(new ResourceInfo("resource_appsystem", "id"));
        theadList.add(new ResourceInfo("resource_appsystem", "name"));
        theadList.add(new ResourceInfo("resource_appsystem", "abbr_name"));
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
                expressionList.addExpressions(new LongValue((Long)id));
            } else if (id instanceof String) {
                expressionList.addExpressions(new StringValue((String)id));
            }
        }
        inExpression.setRightItemsList(expressionList);
        plainSelect.setWhere(inExpression);
        return plainSelect.toString();
    }

    private List<ResourceInfo> getTheadList() {
        List<ResourceInfo> theadList = new ArrayList<>();
        theadList.add(new ResourceInfo("resource_appsystem", "id"));
        theadList.add(new ResourceInfo("resource_appsystem", "name"));
        theadList.add(new ResourceInfo("resource_appsystem", "abbr_name"));
        return theadList;
    }

    private BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByDefaultValue(JSONArray defaultValue, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (CollectionUtils.isNotEmpty(defaultValue)) {
                    ResourceInfo resourceInfo = new ResourceInfo("resource_appsystem","id", false);
                    if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                        Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                        resourceSearchGenerateSqlUtil.addWhere(plainSelect, column, new InExpression(), defaultValue);
                    } else {
                        unavailableResourceInfoList.add(resourceInfo);
                    }
                }
            }
        };
        return biConsumer;
    }

    private BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByKeyword(String keyword, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (StringUtils.isNotBlank(keyword)) {
                    List<ResourceInfo> keywordList = new ArrayList<>();
                    keywordList.add(new ResourceInfo("resource_appsystem", "name"));
                    keywordList.add(new ResourceInfo("resource_appsystem", "abbr_name"));
                    keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_name"));
                    keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_abbr_name"));
                    StringValue stringValue = new StringValue("%" + keyword + "%");
                    List<Expression> expressionList = new ArrayList<>();
                    for (ResourceInfo resourceInfo : keywordList) {
                        if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                            Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                            expressionList.add(new LikeExpression().withLeftExpression(column).withRightExpression(stringValue));
                        } else {
                            unavailableResourceInfoList.add(resourceInfo);
                        }
                    }
                    MultiOrExpression multiOrExpression = new MultiOrExpression(expressionList);
                    resourceSearchGenerateSqlUtil.addWhere(plainSelect, multiOrExpression);
                }
            }
        };
        return biConsumer;
    }

    private BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByKeyword2(String keyword, List<ResourceInfo> unavailableResourceInfoList) {
        BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> biConsumer = new BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect>() {
            @Override
            public void accept(ResourceSearchGenerateSqlUtil resourceSearchGenerateSqlUtil, PlainSelect plainSelect) {
                if (StringUtils.isNotBlank(keyword)) {
                    List<ResourceInfo> keywordList = new ArrayList<>();
                    keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_name"));
                    keywordList.add(new ResourceInfo("resource_appsystem_appmodule", "app_module_abbr_name"));
                    StringValue stringValue = new StringValue("%" + keyword + "%");
                    List<Expression> expressionList = new ArrayList<>();
                    for (ResourceInfo resourceInfo : keywordList) {
                        if (resourceSearchGenerateSqlUtil.additionalInformation(resourceInfo)) {
                            Column column = resourceSearchGenerateSqlUtil.addJoinTableByResourceInfo(resourceInfo, plainSelect);
                            expressionList.add(new LikeExpression().withLeftExpression(column).withRightExpression(stringValue));
                        } else {
                            unavailableResourceInfoList.add(resourceInfo);
                        }
                    }
                    MultiOrExpression multiOrExpression = new MultiOrExpression(expressionList);
                    resourceSearchGenerateSqlUtil.addWhere(plainSelect, multiOrExpression);
                }
            }
        };
        return biConsumer;
    }
}


