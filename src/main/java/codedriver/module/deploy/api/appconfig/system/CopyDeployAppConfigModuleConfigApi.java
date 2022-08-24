/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/8/23 16:06
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class CopyDeployAppConfigModuleConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "复制模块层配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/module/config/copy";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "fromAppModuleId", type = ApiParamType.LONG, desc = "来源模块id"),
            @Param(name = "toAppModuleId", type = ApiParamType.LONG, desc = "目标模块id")
    })
    @Output({
            @Param(explode = DeployAppModuleVo[].class, desc = "复制模块层配置")
    })
    @Description(desc = "复制模块层配置(复制模块配置，以及来源模块和目标模块拥有相同的环境的配置)")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long appSystemId = paramObj.getLong("appSystemId");
        Long fromAppModuleId = paramObj.getLong("fromAppModuleId");
        Long toAppModuleId = paramObj.getLong("toAppModuleId");

        if (toAppModuleId != null) {
            /* 1、给目标模块挂上来源模块的环境
                2、复制模块配置和所有环境的配置*/

            List<DeployAppConfigVo> insertConfigList = new ArrayList<>();
            List<DeployAppConfigVo> updateConfigList = new ArrayList<>();
            List<DeployAppConfigVo> deleteConfigList = new ArrayList<>();

            List<DeployAppConfigVo> appConfigVoList = deployAppConfigMapper.getAppConfigListByAppSystemId(appSystemId);
            if (CollectionUtils.isEmpty(appConfigVoList)) {
                return null;
            }
            boolean hasFromModuleConfig = false;
            boolean hasToModuleConfig = false;
            List<Long> hasConfigEnvIdList = new ArrayList<>();
            for (DeployAppConfigVo appConfigVo : appConfigVoList) {
                if (appConfigVo.getAppModuleId() == 0L) {
                    //应用层配置
                    continue;
                }
                if (appConfigVo.getEnvId() == 0L) {
                    //模块层独有一份配置
                    if (Objects.equals(fromAppModuleId, appConfigVo.getAppModuleId())) {
                        hasFromModuleConfig = true;
                    } else  if (Objects.equals(toAppModuleId, appConfigVo.getAppModuleId())) {
                        hasToModuleConfig = true;
                    }
                } else {
                    //环境层独有一份配置
                    hasConfigEnvIdList.add(appConfigVo.getEnvId());
                }
            }

            //复制环境配置
            List<DeployAppEnvironmentVo> fromModuleEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleId(appSystemId, fromAppModuleId);
            List<DeployAppEnvironmentVo> toModuleEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleId(appSystemId, toAppModuleId);



            /*复制模块流水线配置、模块层runnerGroup信息*/
            //复制模块流水线配置
            if (hasFromModuleConfig) {
                DeployAppConfigVo toModuleConfigVo = new DeployAppConfigVo(appSystemId, toAppModuleId, DeployPipelineConfigManager.init(appSystemId).withAppModuleId(fromAppModuleId).getConfig());
                if (hasToModuleConfig) {
                    updateConfigList.add(toModuleConfigVo);
                } else {
                    insertConfigList.add(toModuleConfigVo);
                }
            } else if (hasToModuleConfig){
                deleteConfigList.add(new DeployAppConfigVo(appSystemId, toAppModuleId));
            }
            //复制模块层runnerGroup信息
            RunnerGroupVo fromModuleRunnerGroup = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(appSystemId, fromAppModuleId);
            deployAppConfigMapper.insertAppModuleRunnerGroup(appSystemId, toAppModuleId, fromModuleRunnerGroup.getId());


//
//            if (CollectionUtils.isNotEmpty(insertConfigList)) {
//                deployAppConfigMapper.insertBatchAppConfig(insertConfigList);
//            }
//            if (CollectionUtils.isNotEmpty(updateConfigList)) {
//                deployAppConfigMapper.updateBatchAppConfig(updateConfigList);
//            }
//            if (CollectionUtils.isNotEmpty(deleteConfigList)) {
//                deleteConfigList.deleteBatchAppConfig(deleteConfigList);
//            }



        } else {
            /*  1、新增目标模块，
                2、给目标模块挂上来源模块的环境
                3、复制模块配置和所有环境的配置*/


        }


        return null;
    }
}
