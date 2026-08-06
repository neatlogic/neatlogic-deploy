package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.version.DeployVersionUnitTestVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployVersionUnitTestApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.savedeployversionunittestapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/versoin/unittest/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "term.cmdb.appsystemid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "term.cmdb.appmoduleid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "common.versionnum", isRequired = true, type = ApiParamType.STRING),
            @Param(explode = DeployVersionUnitTestVo.class, desc = "nmdav.savedeployversionunittestapi.input.param.desc.deployversionunittestvo", type = ApiParamType.JSONOBJECT),
    })
    @Output({
    })
    @Description(desc = "nmdav.savedeployversionunittestapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        DeployVersionUnitTestVo qualityVo = paramObj.toJavaObject(DeployVersionUnitTestVo.class);
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        qualityVo.setVersionId(versionVo.getId());
        deployVersionMapper.insertDeployVersionUnitTest(qualityVo);
        return null;
    }
}
