package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.constvalue.ParamType;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取指定环境下所有实例的autoConfig";
    }

    @Override
    public String getToken() {
        return "deploy/app/env/all/autoconfig/get";
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
    @Description(desc = "获取指定环境下所有实例的autoConfig")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject result = new JSONObject();
        JSONObject autoCfg = new JSONObject();
        result.put("autoCfg", autoCfg);
        JSONArray insCfgList = new JSONArray();
        List<String> passwordKeyList = new ArrayList<>();
        result.put("passwordKeys", passwordKeyList);
        result.put("insCfgList", insCfgList);
        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        Long envId = paramObj.getLong("envId");
        List<DeployAppEnvAutoConfigVo> configVoList = deployAppConfigMapper.getAppEnvAutoConfigBySystemIdAndModuleIdAndEnvId(sysId, moduleId, envId);
        // env配置
        Optional<List<DeployAppEnvAutoConfigKeyValueVo>> envConfigOpt = configVoList.stream().filter(o -> Objects.equals(o.getInstanceId(), 0L))
                .map(DeployAppEnvAutoConfigVo::getKeyValueList).findFirst();
        if (envConfigOpt.isPresent()) {
            List<DeployAppEnvAutoConfigKeyValueVo> envConfigList = envConfigOpt.get();
            for (DeployAppEnvAutoConfigKeyValueVo keyValueVo : envConfigList) {
                autoCfg.put(keyValueVo.getKey(), keyValueVo.getValue());
                if (Objects.equals(ParamType.PASSWORD.getValue(), keyValueVo.getType())) {
                    passwordKeyList.add(keyValueVo.getKey());
                }
            }
        }
        // 实例配置
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        List<Long> instanceIdList = resourceCrossoverMapper.getResourceIdListByAppSystemIdAndModuleIdAndEnvId(new ResourceVo(sysId, moduleId, envId));
        if (CollectionUtils.isNotEmpty(instanceIdList)) {
            List<ResourceVo> instanceList = resourceCrossoverMapper.getResourceListByIdList(instanceIdList);
            Map<Long, Map<String, String>> configMap = configVoList.stream().filter(o -> !Objects.equals(o.getInstanceId(), 0L))
                    .collect(Collectors.toMap(DeployAppEnvAutoConfigVo::getInstanceId, o -> o.getKeyValueList().stream().collect(Collectors.toMap(DeployAppEnvAutoConfigKeyValueVo::getKey, DeployAppEnvAutoConfigKeyValueVo::getValue))));
            for (ResourceVo vo : instanceList) {
                JSONObject insCfg = new JSONObject();
                insCfg.put("nodeName", vo.getName());
                insCfg.put("host", vo.getIp());
                insCfg.put("port", vo.getPort());
                insCfg.put("autoCfg", configMap.get(vo.getId()) != null ? configMap.get(vo.getId()) : new JSONObject());
                insCfgList.add(insCfg);
            }
        }
        return result;
    }

}
