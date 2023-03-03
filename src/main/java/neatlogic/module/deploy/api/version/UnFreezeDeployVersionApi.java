package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/6/13 7:04 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UnFreezeDeployVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getName() {
        return "解/封发布版本";
    }

    @Override
    public String getToken() {
        return "deploy/version/unlock";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "isFreeze", desc = "是否封版(0：解版，1：封版)", isRequired = true, type = ApiParamType.INTEGER)
    })
    @Description(desc = "解/封发布版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long versionId = paramObj.getLong("id");
        DeployVersionVo deployVersionVo = deployVersionMapper.getDeployVersionById(versionId);
        if (deployVersionVo == null) {
            throw new DeployVersionNotFoundException(versionId);
        }

        //校验版本&制品管理的操作权限
        deployAppAuthorityService.checkOperationAuth(deployVersionVo.getAppSystemId(), DeployAppConfigAction.VERSION_AND_PRODUCT_MANAGER);

        deployVersionMapper.unFreezeDeployVersionById(versionId, paramObj.getLong("isFreeze"));
        return null;
    }
}
