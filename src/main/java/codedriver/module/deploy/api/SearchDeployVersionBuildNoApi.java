package codedriver.module.deploy.api;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppModuleVo;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.exception.util.StartTimeAndEndTimeCanNotFoundException;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.framework.util.TimeUtil;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author longrf
 * @date 2022/7/12 2:29 下午
 */
@Service
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
            @Param(name = "startTime", type = ApiParamType.LONG, desc = "开始时间"),
            @Param(name = "endTime", type = ApiParamType.LONG, desc = "结束时间"),
            @Param(name = "timeRange", type = ApiParamType.INTEGER, desc = "时间范围"),
            @Param(name = "timeUnit", type = ApiParamType.ENUM, rule = "year,month,week,day,hour", desc = "时间范围单位"),
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

        //将时间范围转为 开始时间、结束时间
        if (versionBuildNoVo.getStartTime() == null && versionBuildNoVo.getEndTime() == null) {
            Integer timeRange = paramObj.getInteger("timeRange");
            String timeUnit = paramObj.getString("timeUnit");
            if (timeRange != null && StringUtils.isNotBlank(timeUnit)) {
                versionBuildNoVo.setStartTime(TimeUtil.recentTimeTransfer(timeRange, timeUnit));
                versionBuildNoVo.setEndTime(new Date());
            }
        }

        if (versionBuildNoVo.getStartTime() == null || versionBuildNoVo.getEndTime() == null) {
            throw new StartTimeAndEndTimeCanNotFoundException();
        }

        int count = deployVersionMapper.getDeployVersionBuildNoListCount(versionBuildNoVo);
        if (count > 0) {
            versionBuildNoVo.setRowNum(count);
            returnList = deployVersionMapper.searchDeployVersionBuildNoList(versionBuildNoVo);
        }
        return TableResultUtil.getResult(returnList, versionBuildNoVo);
    }
}
