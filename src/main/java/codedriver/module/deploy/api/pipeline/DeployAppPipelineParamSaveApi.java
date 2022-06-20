/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CiphertextPrefix;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dependency.handler.AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler;
import codedriver.module.deploy.dependency.handler.Matrix2DeployAppPipelineParamDependencyHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployAppPipelineParamSaveApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/pipeline/param/save";
    }

    @Override
    public String getName() {
        return "保存应用流水线作业参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "参数列表[{\"key\": \"参数名\", \"name\": \"中文名\", \"defaultValue\": \"默认值\", \"description\": \"描述\", \"isRequired\": \"是否必填\", \"type\": \"参数类型\"}]")
    })
    @Description(desc = "保存应用流水线作业参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long appSystemId = jsonObj.getLong("appSystemId");
        DeployAppConfigVo searchVo = new DeployAppConfigVo(appSystemId);
        DeployAppConfigVo deployAppConfigVo = deployAppConfigMapper.getAppConfigVo(searchVo);
        if (deployAppConfigVo != null) {
            DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
            if (config != null) {
                List<AutoexecParamVo> runtimeParamList = config.getRuntimeParamList();
                if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                    for (AutoexecParamVo autoexecParamVo : runtimeParamList) {
                        DependencyManager.delete(Matrix2DeployAppPipelineParamDependencyHandler.class, autoexecParamVo.getId());
                    }
                }
            }
        }
        Pattern keyPattern = Pattern.compile("^[A-Za-z_\\d]+$");
        Pattern namePattern = Pattern.compile("^[A-Za-z_\\d\\u4e00-\\u9fa5]+$");
        JSONArray paramList = jsonObj.getJSONArray("paramList");
        if (CollectionUtils.isEmpty(paramList)) {
            return null;
        }
        List<AutoexecParamVo> runtimeParamList = paramList.toJavaList(AutoexecParamVo.class);
        for (int i = 0; i < runtimeParamList.size(); i++) {
            AutoexecParamVo autoexecParamVo = runtimeParamList.get(i);
            if (autoexecParamVo != null) {
                String key = autoexecParamVo.getKey();
                int index = i + 1;
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException(index, "英文名");
                }
                if (!keyPattern.matcher(key).matches()) {
                    throw new ParamIrregularException(key);
                }
                String name = autoexecParamVo.getName();
                if (StringUtils.isBlank(name)) {
                    throw new ParamNotExistsException(index, key, "中文名");
                }
                if (!namePattern.matcher(name).matches()) {
                    throw new ParamIrregularException(index, key, name);
                }
                Integer isRequired = autoexecParamVo.getIsRequired();
                if (isRequired == null) {
                    throw new ParamNotExistsException(index, key, "是否必填");
                }
                String type = autoexecParamVo.getType();
                if (StringUtils.isBlank(type)) {
                    throw new ParamNotExistsException(index, key, "控件类型");
                }
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == null) {
                    throw new ParamIrregularException(index, key, type);
                }
                Object value = autoexecParamVo.getDefaultValue();
                // 如果默认值不以"RC4:"开头，说明修改了密码，则重新加密
                if (paramType == ParamType.PASSWORD && value != null && !value.toString().startsWith(CiphertextPrefix.RC4.getValue())) {
                    autoexecParamVo.setDefaultValue(CiphertextPrefix.RC4.getValue() + RC4Util.encrypt((String) value));
                } else if (paramType == ParamType.SELECT || paramType == ParamType.MULTISELECT || paramType == ParamType.CHECKBOX || paramType == ParamType.RADIO) {
                    JSONObject config = autoexecParamVo.getConfig();
                    if (MapUtils.isNotEmpty(config)) {
                        String matrixUuid = config.getString("matrixUuid");
                        if (StringUtils.isNotBlank(matrixUuid)) {
                            JSONObject dependencyConfig = new JSONObject();
                            dependencyConfig.put("appSystemId", appSystemId);
                            DependencyManager.insert(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, matrixUuid, autoexecParamVo.getId(), dependencyConfig);
                        }
                    }
                }
                autoexecParamVo.setSort(i);
            }
        }
        if (deployAppConfigVo == null) {
            deployAppConfigVo = new DeployAppConfigVo(appSystemId);
            DeployPipelineConfigVo config = new DeployPipelineConfigVo();
            config.setRuntimeParamList(runtimeParamList);
            deployAppConfigVo.setConfig(config);
            deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
        } else {
            DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
            if (config == null) {
                config = new DeployPipelineConfigVo();
                deployAppConfigVo.setConfig(config);
            }
            config.setRuntimeParamList(runtimeParamList);
            deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
        }
        return null;
    }

}
