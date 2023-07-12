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
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionCveVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Component
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveDeployVersionCveListApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.savedeployversioncvelistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "common.versionid"),
            @Param(name = "cveList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "term.deploy.cvelist")
    })
    @Output({})
    @Description(desc = "nmdav.savedeployversioncvelistapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long versionId = paramObj.getLong("versionId");
        DeployVersionVo deployVersionVo = deployVersionMapper.getDeployVersionById(versionId);
        if (deployVersionVo == null) {
            throw new DeployVersionNotFoundException(versionId);
        }
        deployVersionMapper.deleteDeployVersionCveByVersionId(versionId);
        JSONArray cveArray = paramObj.getJSONArray("cveList");
        List<DeployVersionCveVo> deployVersionCveList = cveArray.toJavaList(DeployVersionCveVo.class);
        for (DeployVersionCveVo deployVersionCveVo : deployVersionCveList) {
            deployVersionCveVo.setVersionId(versionId);
            deployVersionMapper.insertDeployVersionCve(deployVersionCveVo);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/version/cvelist/save";
    }
}
