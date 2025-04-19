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
