/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigKeyValueVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/8/22 16:38
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployAppConfigAutoConfigForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "保存某个环境的AutoConfig配置的key";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/autoCfgKeys/save/forautoexec";
    }

    @Input({
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "Runner的ID"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "runner组信息"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "应用名"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "模块名"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境名"),
            @Param(name = "autoCfgKeys", type = ApiParamType.JSONARRAY, desc = "key列表"),
    })
    @Output({
    })
    @Description(desc = "发布作业专用-保存某个环境的AutoConfig配置的key")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray autoCfgKeyArray = paramObj.getJSONArray("autoCfgKeys");
        if (CollectionUtils.isEmpty(autoCfgKeyArray)) {
            return null;
        }
        List<String> autoCfgKeyList = autoCfgKeyArray.toJavaList(String.class);
        List<DeployAppEnvAutoConfigKeyValueVo> insertAutoConfigKeyValueVoList = new ArrayList<>();
        for (String autoCfgKey : autoCfgKeyList) {
            insertAutoConfigKeyValueVoList.add(new DeployAppEnvAutoConfigKeyValueVo(autoCfgKey));
        }
        deployAppConfigMapper.insertAppEnvAutoConfigNew(new DeployAppEnvAutoConfigVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), insertAutoConfigKeyValueVoList));
        return null;
    }
}
