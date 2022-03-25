package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_PROFILE_MODIFY;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployProfileService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/18 10:08 上午
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_PROFILE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployProfileSaveApi extends PrivateApiComponentBase {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Resource
    DeployProfileService deployProfileService;

    @Override
    public String getName() {
        return "保存自动化工具profile";
    }

    @Override
    public String getToken() {
        return "deploy/profile/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "profile id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "profile 名称"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "fromSystemId", type = ApiParamType.LONG, desc = "所属系统id"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, desc = "工具参数"),
            @Param(name = "autoexecToolAndScriptVoList", type = ApiParamType.JSONARRAY, desc = "关联的工具和脚本列表")
    })
    @Output({
    })
    @Description(desc = "自动化工具profile保存接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long paramProfileId = paramObj.getLong("id");
        Map<String, List<AutoexecToolAndScriptVo>> toolAndScriptMap = new HashMap<>();
        DeployProfileVo profileVo = JSON.toJavaObject(paramObj, DeployProfileVo.class);

        List<AutoexecToolAndScriptVo> autoexecToolAndScriptVoList = profileVo.getAutoexecToolAndScriptVoList();

        //分类 类型(tool:工具;script:脚本)
        if (CollectionUtils.isNotEmpty(autoexecToolAndScriptVoList)) {
            toolAndScriptMap = profileVo.getAutoexecToolAndScriptVoList().stream().collect(Collectors.groupingBy(AutoexecToolAndScriptVo::getType));
        }
        //删除关系
        deployProfileMapper.deleteProfileOperateByProfileId(paramProfileId);

        //tool
        List<Long> toolIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolAndScriptMap.get(ToolType.TOOL.getValue()))) {
            toolIdList = toolAndScriptMap.get(ToolType.TOOL.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            deployProfileMapper.insertDeployProfileOperationByProfileIdAndOperateIdListAndType(profileVo.getId(), toolIdList, ToolType.TOOL.getValue());
        }
        //script
        List<Long> scriptIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolAndScriptMap.get(ToolType.SCRIPT.getValue()))) {
            scriptIdList = toolAndScriptMap.get(ToolType.SCRIPT.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            deployProfileMapper.insertDeployProfileOperationByProfileIdAndOperateIdListAndType(profileVo.getId(), scriptIdList, ToolType.SCRIPT.getValue());
        }
        if (paramProfileId != null) {
            if (deployProfileMapper.checkProfileIsExists(paramProfileId) == 0) {
                throw new DeployProfileIsNotFoundException(paramProfileId);
            }
            deployProfileMapper.updateProfile(profileVo);
        } else {
            deployProfileMapper.insertProfile(profileVo);
        }
        return null;
    }
}
