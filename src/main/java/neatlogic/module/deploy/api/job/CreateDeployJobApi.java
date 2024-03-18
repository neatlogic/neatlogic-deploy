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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Transactional
@Deprecated
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateDeployJobApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(CreateDeployJobApi.class);
    @Resource
    DeployVersionMapper deployVersionMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    private DeployJobService deployJobService;

    @Override
    public String getName() {
        return "创建单个模块发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "场景名, 如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "appSystemAbbrName", type = ApiParamType.STRING, desc = "应用系统简称，如果入参也有appSystemId，则会以appSystemName为准"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "应用模块id"),
            @Param(name = "appModuleAbbrName", type = ApiParamType.STRING, desc = "应用模块简称"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境id，如果入参也有envId，则会以envName为准"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "来源 itsm|human|deploy   ITSM|人工发起的等，不传默认是发布发起的"),
            @Param(name = "isNeedFire", type = ApiParamType.INTEGER, desc = "创建完作业是否激活，默认1 激活"),

    })
    @Description(desc = "创建单个模块发布作业接口")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Integer isNeedFire = jsonObj.getInteger("isNeedFire");
        JSONObject result = new JSONObject();
        DeployJobVo jobVo = JSON.toJavaObject(jsonObj, DeployJobVo.class);
        if (isNeedFire == null || isNeedFire == 1) {
            deployJobService.createJobAndFire(jobVo);
        } else {
            deployJobService.createJob(jobVo);
        }
        result.put("id", jobVo.getId());
        result.put("name", jobVo.getName());
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/create";
    }

}
