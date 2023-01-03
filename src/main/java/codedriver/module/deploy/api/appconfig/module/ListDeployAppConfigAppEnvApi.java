/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.module;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppEnvironmentVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @since 2022/6/17 11:00
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppEnvApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/env/list";
    }

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统环境列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "isHasEnv", type = ApiParamType.INTEGER, desc = "是否拥有环境 (0:查找现没有的环境，1：查找现有的环境)"),
    })
    @Output({
            @Param(explode = AppEnvironmentVo[].class, desc = "发布应用配置的应用系统环境列表"),
    })
    @Description(desc = "查询发布应用配置的应用系统环境列表(用于应用树的环境下拉、发布作业时通过模块列表查询环境列表)")
    @Override
    public Object myDoService(JSONObject paramObj) {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        List<DeployAppEnvironmentVo> returnEnvList = null;
        if (paramObj.getInteger("isHasEnv") != null && paramObj.getInteger("isHasEnv") == 0) {
            returnEnvList = deployAppConfigMapper.getDeployAppHasNotEnvListByAppSystemIdAndModuleIdList(appSystemId, appModuleId);
        } else {

            //查找发布的环境
            List<DeployAppEnvironmentVo> deployEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleId(appSystemId, appModuleId);

            //查找cmdb的环境
            List<DeployAppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbEnvListByAppSystemIdAndModuleId(appSystemId, appModuleId);

            //如果有交集，则删除发布多余的环境idList
            if (CollectionUtils.isNotEmpty(cmdbEnvList) && CollectionUtils.isNotEmpty(deployEnvList)) {
                List<Long> cmdbEnvIdList = cmdbEnvList.stream().map(DeployAppEnvironmentVo::getId).collect(Collectors.toList());
                List<Long> deployEnvIdList = deployEnvList.stream().map(DeployAppEnvironmentVo::getId).collect(Collectors.toList());

                List<Long> sameEnvIdList = new ArrayList<>(cmdbEnvIdList);
                sameEnvIdList.retainAll(deployEnvIdList);
                if (sameEnvIdList.size() > 0) {
                    deployAppConfigMapper.deleteAppConfigEnvByAppSystemIdAndAppModuleIdAndEnvIdList(paramObj.getLong("appSystemId"), appModuleId, sameEnvIdList);
                    for (int i = 0; i < deployEnvList.size(); i++) {
                        DeployAppEnvironmentVo deployAppEnvironmentVo = deployEnvList.get(i);
                        if (sameEnvIdList.contains(deployAppEnvironmentVo.getId())) {
                            deployEnvList.remove(i);
                        }
                    }
                }
                cmdbEnvList.addAll(deployEnvList);
                returnEnvList = cmdbEnvList;

            } else {
                returnEnvList = CollectionUtils.isNotEmpty(cmdbEnvList) ? cmdbEnvList : deployEnvList;
            }
            int isHasConfig = 0;
            if (CollectionUtils.isNotEmpty(deployAppConfigMapper.getAppConfigListByAppSystemId(paramObj.getLong("appSystemId")))) {
                isHasConfig = 1;
            }
            if (CollectionUtils.isNotEmpty(returnEnvList)) {
                for (DeployAppEnvironmentVo env : returnEnvList) {
                    env.setIsConfig(isHasConfig);
                }
            }
        }
        return returnEnvList.stream().sorted(Comparator.comparing(DeployAppEnvironmentVo::getId)).collect(Collectors.toList());
    }
}
