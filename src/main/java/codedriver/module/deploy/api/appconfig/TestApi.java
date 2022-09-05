/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig;

import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.auth.core.DeployAppAuthChecker;
import codedriver.module.deploy.service.DeployAppAuthorityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TestApi extends PrivateApiComponentBase {


    @Resource
    DeployAppAuthorityService deployAppAuthorityService;


    @Override
    public String getName() {
        return "testdeploy";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
//        return DeployAppAuthChecker.checkAuthorityActionList(paramObj.getLong("appSystemId"), paramObj.getJSONArray("authorityActionList").toJavaList(String.class));
        //

//
//        List<DeployAppAuthCheckVo> checkVoList = paramObj.getJSONArray("checkVoList").toJavaList(DeployAppAuthCheckVo.class);
//
//        Map<Long, Set<String>> paramMap = new HashMap<>();
//        for (DeployAppAuthCheckVo checkVo : checkVoList) {
//            paramMap.put(checkVo.getAppSystemId(), new HashSet<>(checkVo.getAuthorityActionList()));
//        }
//        return DeployAppAuthChecker.checkBatchAuthorityActionList(paramMap);


//        return  DeployAppAuthBuilder.getAppConfigAuthorityList(paramObj.getLong("appSystemId"));


//        List<DeployAppAuthCheckVo> checkVoList = paramObj.getJSONArray("checkVoList").toJavaList(DeployAppAuthCheckVo.class);
//        DeployAppAuthBuilder builder = new DeployAppAuthBuilder();
//        for (int i = 0; i < checkVoList.size(); i++) {
//            DeployAppAuthCheckVo checkVo = checkVoList.get(i);
//            if (i == 0) {
//                return   builder.addEnvActionMap(checkVo.getAppSystemId(), paramObj.getJSONArray("envIdList").toJavaList(Long.class)).batchBuilder();
//
//            } else {
//                builder.addOperationActionMap(checkVo.getAppSystemId(), checkVo.getAuthorityActionList());
//            }
//        }
//
//        return builder.batchBuilder();
//
        return DeployAppAuthChecker.builder(paramObj.getLong("appSystemId"))
                .addEnvAction(481856650534925L)
                .addScenarioAction(638977954340864L)
//                .addOperationAction("view")
                .addOperationAction("versionAndProductManager")
                .addEnvActionList(paramObj.getJSONArray("envIdList").toJavaList(Long.class))
                .addOperationActionList(paramObj.getJSONArray("authorityActionList").toJavaList(String.class))
                .addScenarioActionList(paramObj.getJSONArray("scenarioIdList").toJavaList(Long.class))
                .check()
                ;

//        return DeployAppAuthChecker.batchbuilder().addEnvActionMap(paramObj.getLong("appSystemId"), paramObj.getJSONArray("envIdList").toJavaList(Long.class))
//                .addOperationActionMap(paramObj.getLong("appSystemId"), paramObj.getJSONArray("operationList").toJavaList(String.class))
//                .addOperationActionMap(paramObj.getLong("appSystemId1"), paramObj.getJSONArray("operationList").toJavaList(String.class))
//                .addScenarioActionMap(paramObj.getLong("appSystemId"), paramObj.getJSONArray("scenarioIdList").toJavaList(Long.class))
//                .addScenarioActionMap(paramObj.getLong("appSystemId1"), paramObj.getJSONArray("scenarioIdList").toJavaList(Long.class))
//                .batchCheck()
//                ;


//        return DeployAppAuthBuilder.addEnvActionList( paramObj.getJSONArray("envIdList").toJavaList(Long.class)).builder(paramObj.getLong("appSystemId"));


//        return null;

    }


    @Override
    public String getToken() {
        return "deploy/test";
    }
}
