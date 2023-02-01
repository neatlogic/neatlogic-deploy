package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildQualityVo;
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
public class SaveDeployVersionBuildQualityApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "保存发布版本构建质量";
    }

    @Override
    public String getToken() {
        return "deploy/versoin/build/quality/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sysId", desc = "应用ID", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "moduleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本号", isRequired = true, type = ApiParamType.STRING),
            @Param(explode = DeployVersionBuildQualityVo.class, desc = "代码质量指标", type = ApiParamType.JSONOBJECT),
    })
    @Output({
    })
    @Description(desc = "保存发布版本构建质量")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        Long sysId = paramObj.getLong("sysId");
        Long moduleId = paramObj.getLong("moduleId");
        String version = paramObj.getString("version");
        DeployVersionBuildQualityVo qualityVo = paramObj.toJavaObject(DeployVersionBuildQualityVo.class);
        DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, sysId, moduleId));
        if (versionVo == null) {
            throw new DeployVersionNotFoundException(version);
        }
        qualityVo.setVersionId(versionVo.getId());
        deployVersionMapper.insertDeployVersionBuildQuality(qualityVo);
        return null;
    }
}
