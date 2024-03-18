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
