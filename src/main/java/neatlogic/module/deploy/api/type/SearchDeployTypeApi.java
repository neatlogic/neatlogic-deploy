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
package neatlogic.module.deploy.api.type;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.type.DeployTypeVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployTypeMapper;
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
public class SearchDeployTypeApi extends PrivateApiComponentBase {

    @Resource
    DeployTypeMapper deployTypeMapper;

    @Override
    public String getName() {
        return "查询发布工具类型列表";
    }

    @Override
    public String getToken() {
        return "deploy/type/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活(0:禁用，1：激活)"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = DeployTypeVo[].class, desc = "类型列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询发布工具类型列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployTypeVo deployTypeSearchVo = paramObj.toJavaObject(DeployTypeVo.class);
        List<DeployTypeVo> deployTypeVoList = new ArrayList<>();
        int rowNum = deployTypeMapper.searchTypeCount(deployTypeSearchVo);
        if (rowNum > 0) {
            deployTypeSearchVo.setRowNum(rowNum);
            deployTypeVoList = deployTypeMapper.searchType(deployTypeSearchVo);
        }
        return TableResultUtil.getResult(deployTypeVoList, deployTypeSearchVo);
    }
}
