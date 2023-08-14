/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package neatlogic.module.deploy.api.version;


import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2023/8/14 10:31
 **/
@Component
public class GetDeployVersionCommitDiffApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    RunnerMapper runnerMapper;
    @Override
    public String getName() {
        return "获取发布对应版本commit diff内容";
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "版本id")

    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long versionId = jsonObj.getLong("versionId");
        DeployVersionVo version = deployVersionMapper.getDeployVersionBaseInfoById(versionId);
        if (version == null) {
            throw new DeployVersionNotFoundException(versionId);
        }
        RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(version.getRunnerMapId());
        if(runnerMapVo == null){
            throw new RunnerNotFoundByRunnerMapIdException(version.getRunnerMapId());
        }
        HttpRequestUtil requestUtil = HttpRequestUtil.post(runnerMapVo.getUrl()).setPayload(JSONObject.toJSONString(version)).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(5000).setReadTimeout(5000).sendRequest();
        if (StringUtils.isNotBlank(requestUtil.getError())) {
            throw new RunnerHttpRequestException(runnerMapVo.getUrl() + ":" + requestUtil.getError());
        }
        JSONObject resultJson = requestUtil.getResultJson();
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new RunnerHttpRequestException(runnerMapVo.getUrl() + ":" + requestUtil.getError());
        }
        return resultJson.getJSONObject("Return");
    }

    @Override
    public String getToken() {
        return "/deploy/version/commit/diff/get";
    }
}
