package codedriver.module.deploy.service;


import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.deploy.dto.profile.DeployProfileOperationVo;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
@Service
public class DeployProfileServiceImpl implements DeployProfileService {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;

    /**
     * 获取profile参数
     *
     * @param id
     * @return
     */
    @Override
    public List<AutoexecParamVo> getProfileParamById(Long id) {
        DeployProfileVo profileVo = deployProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new DeployProfileIsNotFoundException(id);
        }
        //获取关联的工具
        List<DeployProfileOperationVo> profileOperationVoList = deployProfileMapper.getProfileOperationVoListByProfileId(id);
        Map<String, List<DeployProfileOperationVo>> toolAndScriptMap = profileOperationVoList.stream().collect(Collectors.groupingBy(DeployProfileOperationVo::getType));
        return getProfileConfig(toolAndScriptMap.get(ToolType.TOOL.getValue()).stream().map(DeployProfileOperationVo::getOperateId).collect(Collectors.toList()), toolAndScriptMap.get(ToolType.SCRIPT.getValue()).stream().map(DeployProfileOperationVo::getOperateId).collect(Collectors.toList()), profileVo.getConfig().getJSONArray("paramList"));
    }

    @Override
    public List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList, JSONArray paramList) {


        List<AutoexecParamVo> toolAndScriptParamVoList = new ArrayList<>();
        List<AutoexecToolAndScriptVo> ToolAndScriptVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            ToolAndScriptVoList.addAll(autoexecToolMapper.getToolListByIdList(toolIdList));
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            ToolAndScriptVoList.addAll(autoexecScriptMapper.getScriptListByIdList(scriptIdList));
        }
        for (AutoexecToolAndScriptVo toolAndScriptVo : ToolAndScriptVoList) {
            toolAndScriptParamVoList.addAll(toolAndScriptVo.getParamList());
        }

        //根据name（唯一键）去重
        toolAndScriptParamVoList = toolAndScriptParamVoList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(AutoexecParamVo::getName))), ArrayList::new));
        Map<String, String> newOperationNameAndTypeMap = toolAndScriptParamVoList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, AutoexecParamVo::getType));

        if (CollectionUtils.isEmpty(paramList)) {
            List<AutoexecParamVo> oldParamList = paramList.toJavaList(AutoexecParamVo.class);
            Map<String, String> oldOperationNameAndTypeMap = oldParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, AutoexecParamVo::getType));
        }
        if (MapUtils.isNotEmpty(newOperationNameAndTypeMap)) {
            for (AutoexecParamVo paramVo : toolAndScriptParamVoList) {

            }
        }


        return toolAndScriptParamVoList;
    }

}
