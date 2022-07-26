/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppSystemVo;
import codedriver.framework.deploy.dto.app.DeployResourceSearchVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/list";
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
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppSystemVo[].class, desc = "发布应用配置的应用系统列表")
    })
    @Description(desc = "查询发布应用配置的应用系统列表（没有关键字过滤）")
    @Override
    public Object myDoService(JSONObject paramObj) {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        List<DeployAppSystemVo> resourceVoList = new ArrayList<>();

        int count = deployAppConfigMapper.getCiEntityIdListCount(paramObj.getInteger("isConfig"));
        if (count > 0) {
            searchVo.setRowNum(count);
            resourceVoList = deployAppConfigMapper.getAppSystemListByUserUuid(UserContext.get().getUserUuid(), searchVo);
            if (CollectionUtils.isNotEmpty(resourceVoList)) {

                //补充系统是否有模块、有环境
                TenantContext.get().switchDataDatabase();
                IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
                List<Long> hasModuleAppSystemIdList = resourceCrossoverMapper.getHasModuleAppSystemIdListByAppSystemIdList(resourceVoList.stream().map(DeployAppSystemVo::getId).collect(Collectors.toList()));
                List<Long> hasEnvAppSystemIdList = resourceCrossoverMapper.getHasEnvAppSystemIdListByAppSystemIdList(resourceVoList.stream().map(DeployAppSystemVo::getId).collect(Collectors.toList()));
                TenantContext.get().switchDefaultDatabase();
                for (DeployAppSystemVo appResourceVo : resourceVoList) {
                    if (hasModuleAppSystemIdList.contains(appResourceVo.getId())) {
                        appResourceVo.setIsHasModule(1);
                    }
                    if (hasEnvAppSystemIdList.contains(appResourceVo.getId())) {
                        appResourceVo.setIsHasEnv(1);
                    }
                }
            }
        }
        return TableResultUtil.getResult(resourceVoList, searchVo);
    }
}
