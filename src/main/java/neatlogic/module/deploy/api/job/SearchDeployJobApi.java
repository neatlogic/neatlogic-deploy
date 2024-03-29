/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.service.DeployJobService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployJobApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private DeployJobService deployJobService;


    @Override
    public String getName() {
        return "查询发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/job/search";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "父作业id"),
            @Param(name = "pipelineId", type = ApiParamType.LONG, desc = "超级流水线id"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"),
            @Param(name = "invokeIdList", type = ApiParamType.JSONARRAY, desc = "引用id列表"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "组合工具类型"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于精确查找作业刷新状态"),
            @Param(name = "excludeIdList", type = ApiParamType.JSONARRAY, desc = "排除id列表"),
            @Param(name = "confId", type = ApiParamType.LONG, desc = "自动发现配置id"),
            @Param(name = "startTimeRange", type = ApiParamType.JSONARRAY, desc = "开始时间范围"),
            @Param(name = "endTimeRange", type = ApiParamType.JSONARRAY, desc = "结束时间范围"),
            @Param(name = "planStartTimeRange", type = ApiParamType.JSONARRAY, desc = "计划时间范围"),
            @Param(name = "execUserList", type = ApiParamType.JSONARRAY, desc = "操作人"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "hasParent", type = ApiParamType.BOOLEAN, desc = "是否拥有父作业"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "来源，默认是deploy，batchdeploy"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "父作业id"),
            @Param(name = "isNeedNameAndAbbrName", type = ApiParamType.INTEGER, desc = "是否需要查询系统、模块名称简称（1：查询，0：不查询）")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        DeployJobVo deployJobVo = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        deployJobVo.setSourceType(JobSourceType.DEPLOY.getValue());
        if (deployJobVo.getParentId() != null) {
            List<Long> idList = deployJobMapper.getJobIdListByParentId(deployJobVo.getParentId());
            deployJobVo.setIdList(idList);
        }
        List<DeployJobVo> deployJobList = deployJobService.searchDeployJob(deployJobVo);
        return TableResultUtil.getResult(deployJobList, deployJobVo);
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

}
