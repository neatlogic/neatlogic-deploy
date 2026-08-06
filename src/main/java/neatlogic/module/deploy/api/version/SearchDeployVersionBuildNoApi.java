package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppModuleVo;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/7/12 2:29 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployVersionBuildNoApi extends PrivateApiComponentBase {

    @Resource
    private DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.searchdeployversionbuildnoapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/job/version/buildno/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmdav.searchdeployversionbuildnoapi.input.param.desc.keyword", xss = true),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "nmdav.searchdeployversionbuildnoapi.input.param.desc.appsystemidlist"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "nmdav.searchdeployversionbuildnoapi.input.param.desc.appmoduleidlist"),
            @Param(name = "startTimeRange", type = ApiParamType.JSONOBJECT, desc = "nmdav.searchdeployversionbuildnoapi.input.param.desc.starttimerange"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "nmdav.searchdeployversionbuildnoapi.input.param.desc.needpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "nmdav.searchdeployversionbuildnoapi.input.param.desc.pagesize"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "nmdav.searchdeployversionbuildnoapi.input.param.desc.currentpage")
    })
    @Output({
            @Param(explode = DeployAppModuleVo[].class, desc = "nmdav.searchdeployversionbuildnoapi.output.param.desc.return")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployVersionBuildNoVo versionBuildNoVo = JSONObject.toJavaObject(paramObj, DeployVersionBuildNoVo.class);
        List<DeployVersionBuildNoVo> returnList = null;

        int count = deployVersionMapper.getDeployVersionBuildNoListCount(versionBuildNoVo);
        if (count > 0) {
            versionBuildNoVo.setRowNum(count);
            returnList = deployVersionMapper.searchDeployVersionBuildNoList(versionBuildNoVo);
        }
        return TableResultUtil.getResult(returnList, versionBuildNoVo);
    }
}
