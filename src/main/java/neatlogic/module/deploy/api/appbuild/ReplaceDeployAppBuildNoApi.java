/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.deploy.api.appbuild;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.deploy.constvalue.BuildNoStatus;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployJobNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.AuthUser;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Transactional
@AuthUser(SystemUser.AUTOEXEC)
@Component
public class ReplaceDeployAppBuildNoApi extends PrivateApiComponentBase {
    @Resource
    DeployJobMapper deployJobMapper;
    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getToken() {
        return "/deploy/appbuild/buildNo/replace";
    }

    @Override
    public String getName() {
        return "nmdaa.replacedeployappbuildapi.getname";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "nmdaa.replacedeployappbuildapi.input.param.desc.jobid"),
            @Param(name = "buildNo", isRequired = true, type = ApiParamType.INTEGER, desc = "nmdaa.replacedeployappbuildapi.input.param.desc.newbuildno")

    })
    @Description(desc = "nmdaa.replacedeployappbuildapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long jobId = paramObj.getLong("jobId");
        Integer newBuildNo = paramObj.getInteger("buildNo");
        DeployJobVo jobVo = deployJobMapper.getDeployJobByJobId(jobId);
        if (jobVo == null) {
            throw new DeployJobNotFoundException(jobId);
        }
        deployJobMapper.updateDeployJobBuildNoById(jobId, newBuildNo.toString());
        DeployVersionVo deployVersionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersion(jobVo.getAppSystemId(), jobVo.getAppModuleId(), jobVo.getVersion());
        if (deployVersionVo == null) {
            throw new DeployVersionNotFoundException(jobVo.getAppSystemId().toString(), jobVo.getAppModuleId().toString(), jobVo.getVersion());
        }
        deployVersionMapper.insertDeployVersionBuildNo(new DeployVersionBuildNoVo(deployVersionVo.getId(), newBuildNo, jobId, BuildNoStatus.PENDING.getValue()));
        return null;
    }
}
