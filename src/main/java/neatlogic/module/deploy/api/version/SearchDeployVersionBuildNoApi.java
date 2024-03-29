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
        return "查询发布作业模块下的版本编译号列表";
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
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索", xss = true),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "startTimeRange", type = ApiParamType.JSONOBJECT, desc = "上传时间范围 (入参：{startTime（开始时间）与endTime（结束时间）}，或者{timeRange（时间范围）与timeUnit（时间范围参数）})"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({
            @Param(explode = DeployAppModuleVo[].class, desc = "查询发布作业模块下的版本编译号列表")
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
