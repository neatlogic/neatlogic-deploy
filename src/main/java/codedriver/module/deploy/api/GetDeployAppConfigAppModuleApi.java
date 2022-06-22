package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/6/22 11:44 上午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigAppModuleApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "查询应用模块详细配置信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/app/module/get";
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, isRequired = true, desc = "应用模块id")
    })
    @Output({
//            @Param(explode = AppEnvironmentVo[].class, desc = "发布应用配置应用模块信息"),
    })
    @Description(desc = "查询应用模块详细配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验应用系统id、模块id是否存在
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("appSystemId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("appSystemId"));
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("moduleId")) == null) {
            throw new CiEntityNotFoundException(paramObj.getLong("moduleId"));
        }

        //TODO 根据appSystemId获取阶段信息

        //获取模块基础信息
        CiEntityVo ciEntityVo = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(paramObj.getLong("moduleId"));
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo appSystemInfo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), paramObj.getLong("moduleId"));

        //获取runner组信息
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(paramObj.getLong("appSystemId"),paramObj.getLong("moduleId"));

        JSONObject appSystemInfoObject = new JSONObject();
        appSystemInfoObject.put("appModuleInfo", appSystemInfo);
        appSystemInfoObject.put("runnerGroupVo", runnerGroupVo);
        return appSystemInfoObject;
    }
}
