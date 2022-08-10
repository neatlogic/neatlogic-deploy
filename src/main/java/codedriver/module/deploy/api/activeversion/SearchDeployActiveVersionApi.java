package codedriver.module.deploy.api.activeversion;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.ModuleVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployEnvVersionStatus;
import codedriver.framework.deploy.dto.app.DeployAppModuleEnvVo;
import codedriver.framework.deploy.dto.app.DeployAppSystemVo;
import codedriver.framework.deploy.dto.app.DeployResourceSearchVo;
import codedriver.framework.deploy.dto.env.DeployEnvVersionAuditVo;
import codedriver.framework.deploy.dto.env.DeployEnvVersionVo;
import codedriver.framework.deploy.dto.version.DeployActiveVersionVo;
import codedriver.framework.deploy.dto.version.DeployModuleActiveVersionVo;
import codedriver.framework.deploy.dto.version.DeploySystemActiveVersionVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployEnvVersionMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployActiveVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployEnvVersionMapper deployEnvVersionMapper;

    @Override
    public String getName() {
        return "查询发布活动版本";
    }

    @Override
    public String getToken() {
        return "deploy/activeversion/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认应用系统ID列表(为空则代表查看所有应用)"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
    })
    @Output({
    })
    @Description(desc = "查询发布活动版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        // 按系统分页
        Integer systemIdListCount = deployAppConfigMapper.getAppSystemIdListCount(searchVo);
        List<DeploySystemActiveVersionVo> result = new ArrayList<>();
        if (systemIdListCount > 0) {
            List<DeployAppSystemVo> systemList = deployAppConfigMapper.searchAppSystemList(searchVo);
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            ResourceSearchVo moduleSearchVo = new ResourceSearchVo();
            moduleSearchVo.setAppSystemIdList(systemList.stream().map(DeployAppSystemVo::getId).collect(Collectors.toList()));
            TenantContext.get().switchDataDatabase();
            // 每个系统各自的模块
            List<ModuleVo> systemModuleList = resourceCrossoverMapper.getAppModuleListByAppSystemIdList(moduleSearchVo);
            TenantContext.get().switchDefaultDatabase();
            if (systemModuleList.size() > 0) {
                Map<Long, List<ModuleVo>> systemModuleMap = systemModuleList.stream().collect(Collectors.groupingBy(ModuleVo::getResourceId));
                for (DeployAppSystemVo systemVo : systemList) {
                    DeploySystemActiveVersionVo system = new DeploySystemActiveVersionVo(systemVo.getId(), systemVo.getAbbrName(), systemVo.getName());
                    result.add(system);
                    List<ModuleVo> moduleList = systemModuleMap.get(systemVo.getId());
                    if (CollectionUtils.isEmpty(moduleList)) {
                        continue;
                    }
                    List<DeployModuleActiveVersionVo> moduleActiveVersionList = new ArrayList<>();
                    system.setModuleList(moduleActiveVersionList);
                    // 当前系统所有的版本
                    List<DeployVersionVo> systemVersionList = deployVersionMapper.getDeployVersionBySystemId(systemVo.getId());
                    Map<Long, List<DeployVersionVo>> moduleVersionMap = null;
                    if (systemVersionList.size() > 0) {
                        // 按模块给版本分类
                        moduleVersionMap = systemVersionList.stream().collect(Collectors.groupingBy(DeployVersionVo::getAppModuleId));
                    }
                    // 当前系统所有模块各自所拥有的环境
                    List<DeployAppModuleEnvVo> moduleEnvList = deployAppConfigMapper.getDeployAppModuleEnvListByAppSystemId(systemVo.getId(), TenantContext.get().getDataDbName());
                    Map<Long, List<AppEnvironmentVo>> moduleEnvListMap = null;
                    if (moduleEnvList.size() > 0) {
                        // 按模块给环境分类
                        moduleEnvListMap = moduleEnvList.stream().collect(Collectors.toMap(DeployAppModuleEnvVo::getId, DeployAppModuleEnvVo::getEnvList));
                    }
                    // 当前系统所有的audit
                    List<DeployEnvVersionAuditVo> systemAuditList = deployEnvVersionMapper.getDeployEnvVersionAuditBySystemId(systemVo.getId());
                    Map<Long, List<DeployEnvVersionAuditVo>> moduleEnvVersionAuditMap = null;
                    if (systemAuditList.size() > 0) {
                        moduleEnvVersionAuditMap = systemAuditList.stream().collect(Collectors.groupingBy(DeployEnvVersionAuditVo::getAppModuleId));
                    }
                    /**
                     * 查出每个模块下的活动版本与最新非活动版本
                     * 活动版本：尚有环境未发布的版本
                     * 非活动版本：全部环境都已发布的版本
                     *
                     * 以环境为角度，取出当前模块下的所有环境的audit（每个环境的记录按时间排序），
                     * 用版本循环，找出每个环境中当前版本在audit中最后一次出现的位置（index），
                     * 如果index存在，则认为当前版本在当前环境发布了，以index的时间作为发布时间
                     * 如果index不存在，则认为当前版本未在当前环境发布
                     * 版本在所有环境都发布了，即可称之为非活动版本，如果有多个非活动版本，只取最后一个
                     */
                    for (ModuleVo moduleVo : moduleList) {
                        DeployModuleActiveVersionVo moduleActiveVersion = new DeployModuleActiveVersionVo(systemVo.getId(), moduleVo.getAppModuleId(), moduleVo.getAppModuleAbbrName(), moduleVo.getAppModuleName());
                        moduleActiveVersionList.add(moduleActiveVersion);
                        if (moduleEnvListMap == null || moduleVersionMap == null) {
                            continue;
                        }
                        // 当前模块所有的环境
                        List<AppEnvironmentVo> moduleAllEnv = moduleEnvListMap.get(moduleVo.getAppModuleId());
                        moduleActiveVersion.setEnvList(moduleAllEnv);
                        // 当前模块所有的版本
                        List<DeployVersionVo> moduleVersionList = moduleVersionMap.get(moduleVo.getAppModuleId());
                        if (moduleAllEnv == null || moduleVersionList == null) {
                            continue;
                        }
                        // 当前模块所有audit
                        List<DeployEnvVersionAuditVo> envVersionAuditList = null;
                        if (moduleEnvVersionAuditMap != null) {
                            envVersionAuditList = moduleEnvVersionAuditMap.get(moduleVo.getAppModuleId());
                        }
                        // 当前模块所有的audit
                        List<DeployActiveVersionVo> activeVersionList = new ArrayList<>();
                        moduleActiveVersion.setVersionList(activeVersionList);
                        // 非活动版本
                        DeployActiveVersionVo inactive = null;
                        // 如果当前系统没有audit记录，那么所有模块的所有版本都认为未发布
                        if (moduleEnvVersionAuditMap == null || CollectionUtils.isEmpty(envVersionAuditList)) {
                            // 当前模块没有任何audit，则认为所有版本在所有环境都未发布
                            for (DeployVersionVo versionVo : moduleVersionList) {
                                // todo 环境有顺序
                                DeployActiveVersionVo activeVersion = new DeployActiveVersionVo(versionVo);
                                activeVersionList.add(activeVersion);
                                List<DeployEnvVersionVo> envStatusList = new ArrayList<>();
                                activeVersion.setEnvList(envStatusList);
                                moduleAllEnv.forEach(o -> envStatusList.add(new DeployEnvVersionVo(o.getEnvId(), o.getEnvName(), DeployEnvVersionStatus.PENDING.getValue())));
                            }
                        } else {
                            Map<Long, List<DeployEnvVersionAuditVo>> envVersionAuditMap = envVersionAuditList.stream().collect(Collectors.groupingBy(DeployEnvVersionAuditVo::getEnvId));
                            // 没有audit记录的环境
                            List<Long> noAuditEnvIdList = moduleAllEnv.stream().map(AppEnvironmentVo::getEnvId).filter(envId -> !envVersionAuditMap.containsKey(envId)).collect(Collectors.toList());
                            for (DeployVersionVo versionVo : moduleVersionList) {
                                // todo 环境有顺序
                                DeployActiveVersionVo activeVersion = new DeployActiveVersionVo(versionVo);
                                List<DeployEnvVersionVo> envStatusList = new ArrayList<>();
                                activeVersion.setEnvList(envStatusList);
                                for (Map.Entry<Long, List<DeployEnvVersionAuditVo>> map : envVersionAuditMap.entrySet()) {
                                    // 以当前模块的环境为准
                                    if (moduleAllEnv.stream().noneMatch(o -> Objects.equals(o.getEnvId(), map.getKey()))) {
                                        continue;
                                    }
                                    Long envId = map.getKey();
                                    List<DeployEnvVersionAuditVo> auditList = map.getValue();
                                    DeployEnvVersionVo envStatus = new DeployEnvVersionVo();
                                    envStatus.setEnvId(envId);
                                    envStatus.setStatus(DeployEnvVersionStatus.PENDING.getValue());
                                    envStatusList.add(envStatus);
                                    Optional<AppEnvironmentVo> first = moduleAllEnv.stream().filter(o -> Objects.equals(o.getEnvId(), envId)).findFirst();
                                    first.ifPresent(appEnvironmentVo -> envStatus.setEnvName(appEnvironmentVo.getEnvName()));
                                    if (auditList != null) {
                                        // 按时间排序
                                        auditList.sort(Comparator.comparing(DeployEnvVersionAuditVo::getId));
                                        // 当前版本在当前环境的audit中最后一次出现的位置
                                        Integer index = null;
                                        Date deployTime = null;
                                        for (int i = 0; i < auditList.size(); i++) {
                                            DeployEnvVersionAuditVo auditVo = auditList.get(i);
                                            if (Objects.equals(auditVo.getNewVersionId(), versionVo.getId())) {
                                                index = i;
                                                deployTime = auditVo.getFcd();
                                            }
                                        }
                                        if (index != null) {
                                            envStatus.setStatus(DeployEnvVersionStatus.DEPLOYED.getValue());
                                            envStatus.setDeployTime(deployTime);
                                        }
                                    }
                                }
                                // 没有audit记录的环境都认为未发布
                                if (noAuditEnvIdList.size() > 0) {
                                    for (Long envId : noAuditEnvIdList) {
                                        Optional<AppEnvironmentVo> first = moduleAllEnv.stream().filter(_o -> Objects.equals(_o.getEnvId(), envId)).findFirst();
                                        String envName = null;
                                        if (first.isPresent()) {
                                            envName = first.get().getEnvName();
                                        }
                                        envStatusList.add(new DeployEnvVersionVo(envId, envName, DeployEnvVersionStatus.PENDING.getValue()));
                                    }
                                }
                                // 当前版本的所有环境都没有audit记录，则认为都未发布
                                if (envStatusList.size() == 0) {
                                    moduleAllEnv.forEach(o -> envStatusList.add(new DeployEnvVersionVo(o.getEnvId(), o.getEnvName(), DeployEnvVersionStatus.PENDING.getValue())));
                                    activeVersionList.add(activeVersion);
                                    continue;
                                }
                                // 如果所有环境的状态都是已发布，那么当前版本为非活动版本
                                // allMatch的坑：当list为空集时默认返回true
                                if (envStatusList.stream().allMatch(o -> Objects.equals(o.getStatus(), DeployEnvVersionStatus.DEPLOYED.getValue()))) {
                                    // 取最新的非活动版本
                                    if (inactive == null || activeVersion.getVersionId() > inactive.getVersionId()) {
                                        inactive = activeVersion;
                                    }
                                } else { // 取全部活动版本
                                    activeVersionList.add(activeVersion);
                                }
                            }
                        }
                        activeVersionList.sort(Comparator.comparing(DeployActiveVersionVo::getVersionId).reversed());
                        // 保证非活动版本始终在末尾
                        if (inactive != null) {
                            activeVersionList.add(inactive);
                        }
                    }
                }
            }
        }
        return TableResultUtil.getResult(result, searchVo);
    }
}
