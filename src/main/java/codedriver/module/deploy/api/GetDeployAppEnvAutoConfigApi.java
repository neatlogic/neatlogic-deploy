package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取环境autoConfig（对接专用）";
    }

    @Override
    public String getToken() {
        return "deploy/app/env/config/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用系统id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "envId", desc = "版本号", isRequired = true, type = ApiParamType.LONG),
    })
    @Description(desc = "获取环境autoConfig（对接专用）")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        JSONObject autoCfg = new JSONObject();
        result.put("autoCfg", autoCfg);
        JSONArray insCfgList = new JSONArray();
        result.put("insCfgList", insCfgList);
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        List<DeployAppEnvAutoConfigVo> configVoList = deployAppConfigMapper.getAppEnvAutoConfigBySystemIdAndModuleIdAndEnvId(sysId, moduleId, envId);
        if (configVoList.size() > 0) {
            Optional<List<DeployAppEnvAutoConfigKeyValueVo>> envConfigOpt = configVoList.stream().filter(o -> Objects.equals(o.getInstanceId(), 0L))
                    .map(DeployAppEnvAutoConfigVo::getKeyValueList).findFirst();
            if (envConfigOpt.isPresent()) {
                List<DeployAppEnvAutoConfigKeyValueVo> envConfigList = envConfigOpt.get();
                for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : envConfigList) {
                    autoCfg.put(keyValueVo.getKey(), keyValueVo.getValue());
                }
            }
            Map<Long, Map<String, String>> configMap = configVoList.stream().filter(o -> !Objects.equals(o.getInstanceId(), 0L))
                    .collect(Collectors.toMap(DeployAppEnvAutoConfigVo::getInstanceId, o -> o.getKeyValueList().stream().collect(Collectors.toMap(DeployAppEnvAutoConfigKeyValueVo::getKey, DeployAppEnvAutoConfigKeyValueVo::getValue))));
            if (MapUtils.isNotEmpty(configMap)) {
                List<ResourceVo> instanceList = resourceCenterMapper.getResourceListByIdList(new ArrayList<>(configMap.keySet()), TenantContext.get().getDataDbName());
                if (instanceList.size() > 0) {
                    for (ResourceVo vo : instanceList) {
                        JSONObject insCfg = new JSONObject();
                        insCfg.put("nodeName", vo.getName());
                        insCfg.put("host", vo.getIp());
                        insCfg.put("port", vo.getPort());
                        insCfg.put("autoCfg", configMap.get(vo.getId()) != null ? configMap.get(vo.getId()) : new JSONObject());
                        insCfgList.add(insCfg);
                    }
                }
            }
        }
        return result;
    }

}
