/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppPipelineParamApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/param/list";
    }

    @Override
    public String getName() {
        return "查询应用流水线作业参数列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID")
    })
    @Output({
            @Param(explode = AutoexecParamVo[].class, desc = "参数列表")
    })
    @Description(desc = "查询应用流水线作业参数列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long appSystemId = jsonObj.getLong("appSystemId");
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId);
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo == null) {
            return new ArrayList<>();
        }
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return new ArrayList<>();
        }
        List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
        if (CollectionUtils.isEmpty(runtimeParamList)) {
            return new ArrayList<>();
        }
        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
            autoexecServiceCrossoverService.mergeConfig(autoexecParamVo);
        }
        return runtimeParamList;
    }
}
