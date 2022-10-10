package codedriver.module.deploy.api.version;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployVersionUnitTestApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "获取发布版本单元测试记录列表";
    }

    @Override
    public String getToken() {
        return "deploy/versoin/unittest/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "limitCount", desc = "限制查询的记录数量", isRequired = true, type = ApiParamType.INTEGER),
    })
    @Output({
    })
    @Description(desc = "获取发布版本单元测试记录列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return deployVersionMapper.getDeployVersionUnitTestListByVersionIdWithLimit(paramObj.getLong("versionId"), paramObj.getInteger("limitCount"));
    }
}
