/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.app;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigResourceVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
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
public class AppListApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/list";
    }

    @Override
    public String getName() {
        return "查询应用系统列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索-应用名|模块名"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppConfigResourceVo[].class, desc = "应用模块列表")
    })
    @Description(desc = "查询应用系统列表")
    @Override
    public Object myDoService(JSONObject paramObj) {
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        List<DeployAppConfigResourceVo> resourceVoList = new ArrayList<>();
        Integer count = deployAppConfigMapper.getAppIdListCount(searchVo);
        if (count > 0) {
            List<Long> appIdList = deployAppConfigMapper.getAppSystemIdList(searchVo, UserContext.get().getUserUuid());
            searchVo.setRowNum(count);
            resourceVoList = deployAppConfigMapper.getAppListByIdList(appIdList, TenantContext.get().getDataDbName(), UserContext.get().getUserUuid());
        }
        return TableResultUtil.getResult(resourceVoList, searchVo);
    }
}
