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


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2023/8/14 10:31
 **/
@Component
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployVersionCommitDiffApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    RunnerMapper runnerMapper;
    @Override
    public String getName() {
        return "nmdav.getdeployversioncommitdiffapi.getname";
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "common.versionid"),
            @Param(name = "commitId", type = ApiParamType.STRING, desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.revision")

    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String commitId = jsonObj.getString("commitId");
        Long versionId = jsonObj.getLong("versionId");
        DeployVersionVo version = deployVersionMapper.getDeployVersionBaseInfoById(versionId);
        if (version == null) {
            throw new DeployVersionNotFoundException(versionId);
        }
        RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(version.getRunnerMapId());
        if(runnerMapVo == null){
            throw new RunnerNotFoundByRunnerMapIdException(version.getRunnerMapId());
        }
        HttpRequestUtil requestUtil = HttpRequestUtil.post(runnerMapVo.getUrl()+"api/rest/deploy/version/commit/diff/get").setPayload(JSONObject.toJSONString(version)).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(5000).setReadTimeout(5000).sendRequest();
        if (StringUtils.isNotBlank(requestUtil.getError())) {
            throw new RunnerHttpRequestException(runnerMapVo.getUrl() + ":" + requestUtil.getError());
        }
        JSONObject resultJson = requestUtil.getResultJson();
        if (!resultJson.containsKey("Status") || !"OK".equals(resultJson.getString("Status"))) {
            throw new RunnerHttpRequestException(runnerMapVo.getUrl() + ":" + requestUtil.getError());
        }

        JSONObject diffJson = resultJson.getJSONObject("Return");
        //如果有commitId入参,则仅返回这个commitId的数据
        if(StringUtils.isNotBlank(commitId)) {
            JSONArray commitList = diffJson.getJSONArray("commitList");
            for (int i = 0; i < commitList.size(); i++) {
                JSONObject commit = commitList.getJSONObject(i);
                if (Objects.equals(commitId,commit.getString("commitId"))){
                    diffJson.put("commitList", Collections.singletonList(commit));
                    break;
                }
            }
        }
        return diffJson;
    }

    @Override
    public String getToken() {
        return "/deploy/version/commit/diff/get";
    }
}
