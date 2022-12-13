/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.type;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.type.DeployTypeVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployTypeMapper;
import com.alibaba.fastjson.JSONObject;
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
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = AutoexecTypeVo[].class, desc = "类型列表"),
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
