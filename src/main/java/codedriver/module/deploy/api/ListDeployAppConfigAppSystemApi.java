/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigResourceVo;
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

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
            @Param(name = "tbodyList", explode = DeployAppConfigResourceVo[].class, desc = "发布应用配置的应用系统列表")
    })
    @Description(desc = "查询发布应用配置的应用系统列表（没有关键字过滤）")
    @Override
    public Object myDoService(JSONObject paramObj) {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        List<DeployAppConfigResourceVo> resourceVoList = new ArrayList<>();

        int count = deployAppConfigMapper.getCiEntityIdListCount(paramObj.getInteger("isConfig"));
        if (count > 0) {
            searchVo.setRowNum(count);
            resourceVoList = deployAppConfigMapper.getAppSystemListByUserUuid(UserContext.get().getUserUuid(), searchVo);
            if (CollectionUtils.isNotEmpty(resourceVoList)) {
                //补充当前系统的模块个数
                for (DeployAppConfigResourceVo resourceVo : resourceVoList) {
                    List<Long> moduleIdList = resourceCenterMapper.getAppSystemModuleIdListByAppSystemId(resourceVo.getAppSystemId(), TenantContext.get().getDataDbName());
                    resourceVo.setAppModuleCount(moduleIdList.size());
                    if (CollectionUtils.isNotEmpty(moduleIdList)) {
                        resourceVo.setIsHasModule(1);
                    }
                }
            }
        }
        return TableResultUtil.getResult(resourceVoList, searchVo);
    }
}
