/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.appconfig.module;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IGlobalAttrCrossoverMapper;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppEnvironmentVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
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
        return "nmdaam.listdeployappconfigappenvapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid"),
            @Param(name = "isHasEnv", type = ApiParamType.INTEGER, desc = "term.cmdb.isexistingenv", help = "0:查找现没有的环境，1：查找现有的环境"),
    })
    @Output({
            @Param(explode = AppEnvironmentVo[].class, desc = "common.tbodylist"),
    })
    @Description(desc = "nmdaam.listdeployappconfigappenvapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        List<DeployAppEnvironmentVo> returnEnvList = new ArrayList<>();

        //查找发布的环境
        List<DeployAppEnvironmentVo> deployEnvList = deployAppConfigMapper.getDeployAppEnvListByAppSystemIdAndModuleId(appSystemId, appModuleId);
        //查找cmdb的环境
        List<DeployAppEnvironmentVo> cmdbEnvList = deployAppConfigMapper.getCmdbEnvListByAppSystemIdAndModuleId(appSystemId, appModuleId);
        List<Long> cmdbEnvIdList = cmdbEnvList.stream().map(DeployAppEnvironmentVo::getId).collect(Collectors.toList());
        List<Long> deployEnvIdList = deployEnvList.stream().map(DeployAppEnvironmentVo::getId).collect(Collectors.toList());

        if (paramObj.getInteger("isHasEnv") != null && paramObj.getInteger("isHasEnv") == 0) {
            IGlobalAttrCrossoverMapper globalAttrCrossoverMapper = CrossoverServiceFactory.getApi(IGlobalAttrCrossoverMapper.class);
            List<GlobalAttrItemVo> globalAttrItemList = new ArrayList<>();
            GlobalAttrVo globalAttrVo = new GlobalAttrVo();
            globalAttrVo.setIsActive(1);
            globalAttrVo.setName("app_environment");
            List<GlobalAttrVo> globalAttrList = globalAttrCrossoverMapper.searchGlobalAttr(globalAttrVo);
            if (CollectionUtils.isNotEmpty(globalAttrList)) {
                globalAttrVo = globalAttrList.get(0);
                GlobalAttrItemVo globalAttrItemVo = new GlobalAttrItemVo();
                globalAttrItemVo.setAttrId(globalAttrVo.getId());
                globalAttrItemList = globalAttrCrossoverMapper.searchGlobalAttrItem(globalAttrItemVo);
            }
            for (GlobalAttrItemVo globalAttrItemVo : globalAttrItemList) {
                Long id = globalAttrItemVo.getId();
                if (cmdbEnvIdList.contains(id) || deployEnvIdList.contains(id)) {
                    continue;
                }
                returnEnvList.add(new DeployAppEnvironmentVo(id, globalAttrItemVo.getValue()));
            }
        } else {
            //如果有交集，则删除发布多余的环境idList
            if (CollectionUtils.isNotEmpty(cmdbEnvList) && CollectionUtils.isNotEmpty(deployEnvList)) {
                List<Long> sameEnvIdList = new ArrayList<>(cmdbEnvIdList);
                sameEnvIdList.retainAll(deployEnvIdList);
                if (sameEnvIdList.size() > 0) {
                    deployAppConfigMapper.deleteAppConfigEnvByAppSystemIdAndAppModuleIdAndEnvIdList(paramObj.getLong("appSystemId"), appModuleId, sameEnvIdList);
                    for (int i = deployEnvList.size() - 1; i >= 0; i--) {
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
