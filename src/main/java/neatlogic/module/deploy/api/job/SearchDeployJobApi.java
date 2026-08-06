/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.deploy.api.job;

import com.alibaba.fastjson.JSON;
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
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
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
        return "nmdaj.searchdeployjobapi.getname";
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
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "term.cmdb.appsystemid"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "term.cmdb.appmoduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "term.cmdb.envid"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "nmdaj.searchdeployjobapi.input.param.desc.parentid"),
            @Param(name = "pipelineId", type = ApiParamType.LONG, desc = "nmdaj.searchdeployjobapi.input.param.desc.pipelineid"),
            @Param(name = "statusList", type = ApiParamType.JSONARRAY, desc = "term.autoexec.jobstatuslabel"),
            @Param(name = "invokeIdList", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.invokeidlist"),
            @Param(name = "typeIdList", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.typeidlist"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.idlist"),
            @Param(name = "excludeIdList", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.excludeidlist"),
            @Param(name = "confId", type = ApiParamType.LONG, desc = "nmdaj.searchdeployjobapi.input.param.desc.confid"),
            @Param(name = "startTimeRange", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.starttimerange"),
            @Param(name = "endTimeRange", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.endtimerange"),
            @Param(name = "planStartTimeRange", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.planstarttimerange"),
            @Param(name = "execUserList", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.execuserlist"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "hasParent", type = ApiParamType.BOOLEAN, desc = "nmdaj.searchdeployjobapi.input.param.desc.hasparent"),
            @Param(name = "sourceList", type = ApiParamType.JSONARRAY, desc = "nmdaj.searchdeployjobapi.input.param.desc.sourcelist"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "nmdaj.searchdeployjobapi.input.param.desc.parentid"),
            @Param(name = "isNeedNameAndAbbrName", type = ApiParamType.INTEGER, desc = "nmdaj.searchdeployjobapi.input.param.desc.isneednameandabbrname")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecJobVo[].class, desc = "nmdaj.searchdeployjobapi.output.param.desc.tbodylist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmdaj.searchdeployjobapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        DeployJobVo deployJobVo = JSON.toJavaObject(jsonObj, DeployJobVo.class);
        deployJobVo.setSourceType(JobSourceType.DEPLOY.getValue());
        if (deployJobVo.getParentId() != null) {
            List<Long> idList = deployJobMapper.getJobIdListByParentId(deployJobVo.getParentId());
            if (CollectionUtils.isEmpty(idList)) {
                return TableResultUtil.getResult(new ArrayList<>(), deployJobVo);
            }
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
