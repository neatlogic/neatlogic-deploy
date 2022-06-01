package codedriver.module.deploy.api.version;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/5/26 2:33 下午
 */
@Service
public class DeployVersionSaveApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;
//
//    @Resource
//    CiEntityMapper ciEntityMapper;

    @Override
    public String getName() {
        return "保存发布版本列表";
    }

    @Override
    public String getToken() {
        return "deploy/version/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "version", desc = "版本", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "appId", desc = "应用id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "appName", desc = "应用名称", type = ApiParamType.STRING),
            @Param(name = "appModuleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "appModuleName", desc = "应用模块名称", type = ApiParamType.STRING),
            @Param(name = "isUnLock", desc = "是否封版", isRequired = true, type = ApiParamType.INTEGER)
    })
    @Description(desc = "保存发布版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
//        DeployVersionVo paramVersion = paramObj.toJavaObject(DeployVersionVo.class);
//        if (deployVersionMapper.checkDeployVersionIsRepeat(paramVersion)) {
//            throw new DeployVersionIsRepeat(paramVersion);
//        }
//        if (ciEntityMapper.getCiEntityBaseInfoById(paramVersion.getAppId()) == null) {
//            throw new CiEntityNotFoundException(paramVersion.getAppId());
//        }
//        if (ciEntityMapper.getCiEntityBaseInfoById(paramVersion.getAppModuleId()) == null) {
//            throw new CiEntityNotFoundException(paramVersion.getAppModuleId());
//        }
//
//        deployVersionMapper.insertDeployVersion(paramVersion);
        return null;
    }

    public IValid name() {
        return value -> {
            DeployVersionVo vo = JSON.toJavaObject(value, DeployVersionVo.class);
//            if (deployVersionMapper.checkDeployVersionIsRepeat(vo) > 0) {
//                return new FieldValidResultVo(new DeployVersionIsRepeat(vo));
//            }
            return new FieldValidResultVo();
        };
    }
}
