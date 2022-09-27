/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.apppipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.autoexec.dto.AutoexecOperationBaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelinePhaseVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateDeployAppPipelinePhaseOperationDescriptionFieldApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/phase/operation/description/update";
    }

    @Override
    public String getName() {
        return "更新应用流水线阶段操作的描述字段值";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Description(desc = "更新应用流水线阶段操作的描述字段值")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray resultList = new JSONArray();
        int rowNum = deployAppConfigMapper.getAllDeployAppConfigCount();
        if (rowNum > 0) {
            IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
            BasePageVo searchVo = new BasePageVo();
            searchVo.setRowNum(rowNum);
            searchVo.setPageSize(100);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<DeployAppConfigVo> deployAppConfigList = deployAppConfigMapper.getAllAppConfigListByPage(searchVo);
                for (DeployAppConfigVo deployAppConfigVo : deployAppConfigList) {
                    DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
                    if (config != null) {
                        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
                        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
                            boolean flag = false;
                            for (DeployPipelinePhaseVo deployPipelinePhaseVo : combopPhaseList) {
                                AutoexecCombopPhaseConfigVo phaseConfig = deployPipelinePhaseVo.getConfig();
                                if (phaseConfig != null) {
                                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                                        for (AutoexecCombopPhaseOperationVo phaseOperationVo : phaseOperationList) {
                                            if (StringUtils.isNotBlank(phaseOperationVo.getDescription())) {
                                                continue;
                                            }
                                            AutoexecOperationBaseVo autoexecOperationBaseVo = autoexecServiceCrossoverService.getAutoexecOperationBaseVoByIdAndType(deployPipelinePhaseVo.getName(), phaseOperationVo, false);
                                            if (autoexecOperationBaseVo == null) {
                                                continue;
                                            }
                                            String description = autoexecOperationBaseVo.getDescription();
                                            if (StringUtils.isBlank(description)) {
                                                continue;
                                            }
                                            phaseOperationVo.setDescription(description);
                                            phaseOperationVo.setOperation(null);
                                            flag = true;
                                        }
                                    }
                                }
                            }
                            if (flag) {
                                deployAppConfigVo.setConfig(config);
                                deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
                                JSONObject obj = new JSONObject();
                                obj.put("appSystemId", deployAppConfigVo.getAppSystemId());
                                obj.put("appModuleId", deployAppConfigVo.getAppModuleId());
                                obj.put("envId", deployAppConfigVo.getEnvId());
                                resultList.add(obj);
                            }
                        }
                    }
                }
            }
        }
        return resultList;
    }

}
