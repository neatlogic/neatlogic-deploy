package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
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
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/16 11:23 上午
 */
@AuthAction(action = DEPLOY_PROFILE_MODIFY.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployProfileGetApi extends PrivateApiComponentBase {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Resource
    DeployProfileService deployProfileService;

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;


    @Override
    public String getName() {
        return "获取自动化工具profile";
    }

    @Override
    public String getToken() {
        return "deploy/profile/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "profile id", type = ApiParamType.LONG)
    })
    @Output({
            @Param(explode = DeployProfileVo[].class, desc = "工具profile")
    })
    @Description(desc = "获取自动化工具profile接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployProfileVo profileVo = deployProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new DeployProfileIsNotFoundException(id);
        }


        List<AutoexecToolAndScriptVo> toolAndScriptVoList = profileVo.getAutoexecToolAndScriptVoList();
        List<AutoexecToolAndScriptVo> returnToolAndScriptVoList = new ArrayList<>();


        Map<String, List<AutoexecToolAndScriptVo>> toolAndScriptMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolAndScriptVoList)) {
            toolAndScriptMap = toolAndScriptVoList.stream().collect(Collectors.groupingBy(AutoexecToolAndScriptVo::getType));
        }
        //tool
        List<Long> toolIdList = toolAndScriptMap.get(ToolType.TOOL.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            returnToolAndScriptVoList.addAll(autoexecToolMapper.getToolListByIdList(toolIdList));
        }
        //script
        List<Long> scriptIdList = toolAndScriptMap.get(ToolType.SCRIPT.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            returnToolAndScriptVoList.addAll(autoexecScriptMapper.getScriptListByIdList(scriptIdList));
        }


        profileVo.setAutoexecToolAndScriptVoList(returnToolAndScriptVoList);
        //获取profile参数
        profileVo.setInputParamList(deployProfileService.getProfileParamById(id));
        return profileVo;
    }
}
