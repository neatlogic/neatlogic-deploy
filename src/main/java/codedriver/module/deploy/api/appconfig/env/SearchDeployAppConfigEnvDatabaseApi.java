package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
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
import java.util.*;

/**
 * @author longrf
 * @date 2022/7/1 12:18 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigEnvDatabaseApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getName() {
        return "查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境且发布没配置的数据库";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/database/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = ResourceVo[].class, desc = "数据库资产列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境且发布没配置的数据库")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);

        List<ResourceVo> deployDBResourceVoList = new ArrayList<>();

        if (CollectionUtils.isEmpty(paramObj.getJSONArray("defaultValue"))) {
            //查询当前模块的环境且发布已配置的数据库
            List<Long> DBResourceIdList = deployAppConfigMapper.getAppConfigEnvDBConfigResourceIdByAppSystemIdAndAppModuleIdAndEnvId(searchVo.getAppSystemId(), searchVo.getAppModuleId(), searchVo.getEnvId());
            if (CollectionUtils.isNotEmpty(DBResourceIdList)) {
                searchVo.setNotInIdList(DBResourceIdList);
            }
        }
        int count = deployAppConfigMapper.getAppConfigEnvDatabaseCount(searchVo, TenantContext.get().getDataDbName());
        if (count > 0) {
            searchVo.setRowNum(count);
            List<Long> databaseIdList = deployAppConfigMapper.getAppConfigEnvDatabaseResourceIdList(searchVo, TenantContext.get().getDataDbName());
            deployDBResourceVoList=resourceCenterMapper.getResourceListByIdList(databaseIdList, TenantContext.get().getDataDbName());
        }
        return TableResultUtil.getResult(deployDBResourceVoList, searchVo);
    }
}
