package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigDbConfigNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/7/7 10:49 上午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteDeployAppConfigEnvDbConfigApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "删除发布应用配置的DB配置";
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/delete";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")
    })
    @Description(desc = "删除发布应用配置的DB配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployAppConfigEnvDBConfigVo dbConfigVo = deployAppConfigMapper.getAppConfigEnvDBConfigById(id);
        if (dbConfigVo == null) {
            throw new DeployAppConfigDbConfigNotFoundException(id);
        }

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(dbConfigVo.getAppSystemId(), dbConfigVo.getEnvId());
        deployAppAuthorityService.checkOperationAuth(dbConfigVo.getAppSystemId(), DeployAppConfigAction.EDIT);


        deployAppConfigMapper.deleteAppConfigDBConfigById(id);
        return null;
    }
}
