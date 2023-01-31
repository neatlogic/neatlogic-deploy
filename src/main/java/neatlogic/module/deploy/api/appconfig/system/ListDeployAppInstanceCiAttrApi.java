/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package neatlogic.module.deploy.api.appconfig.system;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.service.DeployAppConfigService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/11/7 11:10
 */

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppInstanceCiAttrApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;


    @Override
    public String getName() {
        return "获取发布应用实例模型的属性列表";
    }

    @Override
    public String getToken() {
            return "deploy/app/instance/ci/attr/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "isAll", type = ApiParamType.INTEGER, isRequired = true, desc = "是否返回全部属性"),
            @Param(name = "attrNameList", type = ApiParamType.JSONARRAY, desc = "需要返回的属性列表")
    })
    @Output({@Param(type = ApiParamType.JSONOBJECT, desc = "发布应用模块实例的属性列表")})
    @Description(desc = "获取发布应用模块实例的属性列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = ciCrossoverMapper.getCiByName("AppIns");
        if (appCiVo == null) {
            throw new CiNotFoundException("AppIns");
        }
        return deployAppConfigService.getDeployCiAttrList(appCiVo.getId(), paramObj.getInteger("isAll"), paramObj.getJSONArray("attrNameList"));
    }
}
