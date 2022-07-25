package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigInstanceVo;
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
 * @author longrf
 * @date 2022/6/29 3:44 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigInstanceApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "查询发布应用配置的应用实例下的无模块无环境、无模块同环境、同模块无环境的实例列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/instance/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境id"),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "needPage", desc = "是否分页", type = ApiParamType.BOOLEAN),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "实例列表"),
    })
    @Description(desc = "查询发布应用配置的应用实例下的无模块无环境、无模块同环境、同模块无环境的实例")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigInstanceVo searchVo = paramObj.toJavaObject(DeployAppConfigInstanceVo.class);
        List<DeployAppConfigInstanceVo> instanceList = new ArrayList<>();

        TenantContext.get().switchDataDatabase();
        int count = deployAppConfigMapper.getAppConfigEnvInstanceCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);
            instanceList = deployAppConfigMapper.searchAppConfigEnvInstanceList(searchVo);
        }
        TenantContext.get().switchDefaultDatabase();
        return TableResultUtil.getResult(instanceList, searchVo);
    }
}
