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
package neatlogic.module.deploy.api.bluegreen;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployBlueGreenVo;
import neatlogic.framework.deploy.dto.type.DeployTypeVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployBlueGreenMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/12/8 14:42
 */

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchBlueGreenApi extends PrivateApiComponentBase {

    @Resource
    DeployBlueGreenMapper deployBlueGreenMapper;

    @Override
    public String getName() {
        return "nmdab.searchbluegreenapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/bluegreen/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "nmdab.searchbluegreenapi.input.param.desc.keyword", xss = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "nmdab.searchbluegreenapi.input.param.desc.isactive"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "nmdab.searchbluegreenapi.input.param.desc.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "nmdab.searchbluegreenapi.input.param.desc.needpage")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = DeployTypeVo[].class),
            @Param(explode = DeployBlueGreenVo.class)
    })
    @Description(desc = "nmdab.searchbluegreenapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployBlueGreenVo deployBlueGreenVo = paramObj.toJavaObject(DeployBlueGreenVo.class);
        List<DeployBlueGreenVo> deployBlueGreenList = new ArrayList<>();
        int rowNum = deployBlueGreenMapper.getBlueGreenCount(deployBlueGreenVo);
        if (rowNum > 0) {
            deployBlueGreenVo.setRowNum(rowNum);
            deployBlueGreenList = deployBlueGreenMapper.searchBlueGreen(deployBlueGreenVo);
        }
        return TableResultUtil.getResult(deployBlueGreenList, deployBlueGreenVo);
    }
}
