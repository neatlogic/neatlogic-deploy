/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.service.DeployJobService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Input({@Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"), @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"), @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"), @Param(name = "parentId", type = ApiParamType.LONG, desc = "父作业id"), @Param(name = "pipelineId", type = ApiParamType.LONG, desc = "超级流水线id"), @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "作业状态"), @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "组合工具类型"), @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "id列表，用于精确查找作业刷新状态"), @Param(name = "excludeIdList", type = ApiParamType.JSONARRAY, desc = "排除id列表"), @Param(name = "confId", type = ApiParamType.LONG, desc = "自动发现配置id"), @Param(name = "startTimeRange", type = ApiParamType.JSONARRAY, desc = "开始时间范围"), @Param(name = "endTimeRange", type = ApiParamType.JSONARRAY, desc = "结束时间范围"), @Param(name = "planStartTimeRange", type = ApiParamType.JSONARRAY, desc = "计划时间范围"), @Param(name = "execUserList", type = ApiParamType.JSONARRAY, desc = "操作人"), @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true), @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"), @Param(name = "hasParent", type = ApiParamType.BOOLEAN, desc = "是否拥有父作业"), @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "来源，默认是deploy，batchdeploy"), @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"), @Param(name = "parentId", type = ApiParamType.LONG, desc = "父作业id")})
    @Output({@Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "列表"), @Param(explode = BasePageVo.class)})
    @Description(desc = "查询发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long appSystemId = jsonObj.getLong("appSystemId");
        Long appModuleId = jsonObj.getLong("appModuleId");
        Long envId = jsonObj.getLong("envId");
        Long parentId = jsonObj.getLong("parentId");
        DeployJobVo deployJobVo = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        //根据appSystemId和appModuleId 获取invokeIdList
        if (appSystemId != null || appModuleId != null || envId != null) {
            List<DeployJobVo> deployJobVos = deployJobMapper.getDeployJobListByAppSystemIdAndAppModuleIdAndEnvId(appSystemId, appModuleId, envId);
            if (CollectionUtils.isNotEmpty(deployJobVos)) {
                deployJobVo.setInvokeIdList(deployJobVos.stream().map(DeployJobVo::getId).collect(Collectors.toSet()));
            } else {
                return TableResultUtil.getResult(new ArrayList<>(), deployJobVo);
            }
        }

        if (parentId != null) {
            List<Long> idList = deployJobMapper.getJobIdListByParentId(parentId);
            deployJobVo.setIdList(idList);
            deployJobVo.setSourceList(new ArrayList<String>() {{
                this.add(JobSource.DEPLOY.getValue());
            }});
        }
        List<DeployJobVo> deployJobList = deployJobService.searchDeployJob(deployJobVo);
        //TODO 补简称系统模块的简称
        return TableResultUtil.getResult(deployJobList, deployJobVo);
    }

}
