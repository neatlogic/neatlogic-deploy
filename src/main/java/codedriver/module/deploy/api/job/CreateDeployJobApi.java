/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployJobService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Transactional
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class CreateDeployJobApi extends PrivateApiComponentBase {

    @Resource
    private DeployJobService deployJobService;

    @Override
    public String getName() {
        return "创建并执行发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "scenarioId", type = ApiParamType.LONG, desc = "场景id"),
            @Param(name = "scenarioName", type = ApiParamType.STRING, desc = "场景名, 如果入参也有scenarioId，则会以scenarioName为准"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用系统id"),
            @Param(name = "appSystemName", type = ApiParamType.STRING, desc = "应用系统名，如果入参也有appSystemId，则会以appSystemName为准"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "应用系统名，如果入参也有appSystemId，则会以sysName为准"),
            @Param(name = "moduleList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "模块列表"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境id，如果入参也有envId，则会以envName为准"),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行参数"),
            @Param(name = "source", type = ApiParamType.STRING, desc = "来源 itsm|human|deploy   ITSM|人工发起的等，不传默认是发布发起的"),
            @Param(name = "roundCount", type = ApiParamType.LONG, isRequired = true, desc = "分组数 "),
            @Param(name = "executeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标"),
    })
    @Description(desc = "创建并执行发布作业接口")
    @ResubmitInterval(value = 2)
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray result = new JSONArray();
        if(jsonObj.containsKey("sysName")) {
            jsonObj.put("appSystemName",jsonObj.getString("sysName"));
        }
        deployJobService.initDeployParam(jsonObj);
        JSONArray moduleArray = jsonObj.getJSONArray("moduleList");
        BatchRunner<Object> runner = new BatchRunner<>();
        runner.execute(moduleArray, 3, module -> {
            JSONObject moduleJson = JSONObject.parseObject(module.toString());
            if (MapUtils.isNotEmpty(moduleJson)) {
                deployJobService.convertModule(jsonObj, moduleJson);
                result.add(deployJobService.createJob(jsonObj));
            }
        }, "DEPLOY-JOB-CREATE");
        return result;
    }

    @Override
    public String getToken() {
        return "/deploy/job/create";
    }
}
