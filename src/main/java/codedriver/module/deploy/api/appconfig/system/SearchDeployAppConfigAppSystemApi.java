/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.DeployAppConfigActionType;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/20 5:13 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

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
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
            @Param(name = "pageRange", type = ApiParamType.JSONARRAY, desc = "分页范围")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppSystemVo[].class, desc = "发布应用配置的应用系统列表")
    })
    @Description(desc = "查询发布应用配置的应用系统列表（含关键字过滤）")
    @Override
    public Object myDoService(JSONObject paramObj) {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        List<DeployAppSystemVo> returnAppSystemList = new ArrayList<>();
        Integer count = deployAppConfigMapper.getAppSystemIdListCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);

            List<Long> appSystemIdList = deployAppConfigMapper.getAppSystemIdList(searchVo, UserContext.get().getUserUuid());
            if (CollectionUtils.isEmpty(appSystemIdList)) {
                return TableResultUtil.getResult(returnAppSystemList, searchVo);
            }
            if (StringUtils.isNotEmpty(searchVo.getKeyword())) {
                returnAppSystemList = deployAppConfigMapper.getAppSystemListIncludeModuleByIdList(appSystemIdList, TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
            } else {
                returnAppSystemList = deployAppConfigMapper.getAppSystemListByIdList(appSystemIdList, TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
            }

            /*补充系统是否有模块,补充模块是否配置、是否有环境、以及系统的权限列表*/
            TenantContext.get().switchDataDatabase();
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<Long> hasModuleAppSystemIdList = resourceCrossoverMapper.getHasModuleAppSystemIdListByAppSystemIdList(appSystemIdList);
            TenantContext.get().switchDefaultDatabase();
            List<Long> hasEnvAppSystemIdList = deployAppConfigMapper.getHasEnvAppSystemIdListByAppSystemIdList(appSystemIdList, TenantContext.get().getDataDbName());

            //批量获取系统的环境列表
            List<DeployAppSystemVo> appSystemVoListIncludeEnvIdList = deployAppConfigMapper.getDeployAppSystemListIncludeEnvIdListByAppSystemIdList(appSystemIdList, TenantContext.get().getDataDbName());
            Map<Long, List<Long>> appSystemIdEnvIdListMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(appSystemVoListIncludeEnvIdList)) {
                appSystemIdEnvIdListMap = appSystemVoListIncludeEnvIdList.stream().collect(Collectors.toMap(DeployAppSystemVo::getId, DeployAppSystemVo::getEnvIdList));
            }

            for (DeployAppSystemVo returnSystemVo : returnAppSystemList) {
                //补充系统是否有模块、是否有环境、是否有配置权限
                if (hasModuleAppSystemIdList.contains(returnSystemVo.getId())) {
                    returnSystemVo.setIsHasModule(1);
                }
                if (hasEnvAppSystemIdList.contains(returnSystemVo.getId())) {
                    returnSystemVo.setIsHasEnv(1);
                }

                //补充模块是否配置、是否有环境
                if (CollectionUtils.isNotEmpty(returnSystemVo.getAppModuleList())) {
                    int isHasConfig = 0;
                    if (CollectionUtils.isNotEmpty(deployAppConfigMapper.getAppConfigListByAppSystemId(returnSystemVo.getId()))) {
                        isHasConfig = 1;
                    }
                    List<Long> appModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(returnSystemVo.getId(), returnSystemVo.getAppModuleList().stream().map(DeployAppModuleVo::getId).collect(Collectors.toList()), TenantContext.get().getDataDbName());
                    for (DeployAppModuleVo appModuleVo : returnSystemVo.getAppModuleList()) {
                        if (appModuleIdList.contains(appModuleVo.getId())) {
                            appModuleVo.setIsHasEnv(1);
                        }
                        appModuleVo.setIsConfig(isHasConfig);
                    }
                }

                 /* 拼接权限数据结构：
                    1、循环权限类型，将可能拥有一个类型所有权限的类型优先拼接
                    2、循环已有权限，将其余散存的权限拼接
                 */
                if (CollectionUtils.isNotEmpty(returnSystemVo.getAuthActionVoList())) {
                    List<String> authActionStringList = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(returnSystemVo.getAuthActionVoList())) {

                        // map<权限类型，权限列表>
                        Map<String, List<DeployAppConfigAuthorityActionVo>> authTypeAuthActionListMap = returnSystemVo.getAuthActionVoList().stream().collect(Collectors.groupingBy(DeployAppConfigAuthorityActionVo::getType));
                        List<String> allActionTypeList = new ArrayList<>();

                        //循环权限类型
                        for (String actionType : DeployAppConfigActionType.getValueList()) {
                            List<DeployAppConfigAuthorityActionVo> actionTypeActionVoList = authTypeAuthActionListMap.get(actionType);
                            if (CollectionUtils.isEmpty(actionTypeActionVoList)) {
                                continue;
                            }

                            //拥有当前类型的所有权限
                            if (CollectionUtils.isNotEmpty(actionTypeActionVoList.stream().filter(e -> StringUtils.equals(e.getAction(), "all")).collect(Collectors.toList()))) {

                                allActionTypeList.add(actionType);
                                if (StringUtils.equals(actionType, DeployAppConfigActionType.OPERATION.getValue())) {
                                    for (String operationString : DeployAppConfigAction.getValueList()) {
                                        authActionStringList.add(DeployAppConfigActionType.OPERATION.getValue() + "#" + operationString);
                                    }
                                } else if (StringUtils.equals(actionType, DeployAppConfigActionType.ENV.getValue())) {
                                    List<Long> envIdList = appSystemIdEnvIdListMap.get(returnSystemVo.getId());
                                    if (CollectionUtils.isNotEmpty(envIdList)) {
                                        for (Long envId : envIdList) {
                                            authActionStringList.add(DeployAppConfigActionType.ENV.getValue() + "#" + envId.toString());
                                        }
                                    }
                                } else if (StringUtils.equals(actionType, DeployAppConfigActionType.SCENARIO.getValue())) {
                                    DeployPipelineConfigVo pipelineConfigVo = DeployPipelineConfigManager.init(returnSystemVo.getId()).getConfig();
                                    if (pipelineConfigVo == null) {
                                        continue;
                                    }
                                    for (AutoexecCombopScenarioVo scenarioVo : pipelineConfigVo.getScenarioList()) {
                                        authActionStringList.add(DeployAppConfigActionType.SCENARIO.getValue() + "#" + scenarioVo.getScenarioId().toString());
                                    }
                                }
                            }
                        }

                        //循环拥有的权限，将其余散存的权限拼接
                        for (DeployAppConfigAuthorityActionVo actionVo : returnSystemVo.getAuthActionVoList()) {
                            if (!allActionTypeList.contains(actionVo.getType())) {
                                if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.OPERATION.getValue())) {
                                    authActionStringList.add(DeployAppConfigActionType.OPERATION.getValue() + "#" + actionVo.getAction());
                                } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.ENV.getValue())) {
                                    authActionStringList.add(DeployAppConfigActionType.ENV.getValue() + "#" + actionVo.getAction());
                                } else if (StringUtils.equals(actionVo.getType(), DeployAppConfigActionType.SCENARIO.getValue())) {
                                    authActionStringList.add(DeployAppConfigActionType.SCENARIO.getValue() + "#" + actionVo.getAction());
                                }
                            }
                        }
                    }
                    returnSystemVo.setAuthActionSet(new HashSet<>(authActionStringList));
                }
            }
        }
        return TableResultUtil.getResult(returnAppSystemList, searchVo);
    }
}


