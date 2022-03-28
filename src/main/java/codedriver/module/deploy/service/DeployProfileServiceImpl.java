package codedriver.module.deploy.service;


import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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
        Map<String, List<Long>> toolIdListAndScriptIdListMap = getAutoexecToolIdListAndScriptIdListByProfileId(id);
        List<Long> toolIdList = null;
        List<Long> scriptIdList = null;
        if (toolIdListAndScriptIdListMap.containsKey(ToolType.TOOL.getValue())) {
            toolIdList = toolIdListAndScriptIdListMap.get(ToolType.TOOL.getValue());
        }
        if (toolIdListAndScriptIdListMap.containsKey(ToolType.SCRIPT.getValue())) {
            scriptIdList = toolIdListAndScriptIdListMap.get(ToolType.SCRIPT.getValue());
        }
        return getProfileConfig(toolIdList, scriptIdList, deployProfileMapper.getProfileVoById(id).getParamList());
    }

    /**
     * 获取工具参数并去重
     *
     * 新的参数列表：工具和脚本参数的去重集合（name唯一键）
     * 旧的参数列表：数据库存的
     * 新旧名称和类型都相同时，将继续使用旧参数值，不做值是否存在的校验，前端回填失败提示即可
     *
     * @param toolIdList   工具id
     * @param scriptIdList 脚本id
     * @param paramList    工具参数
     * @return
     */
    @Override
    public List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList, List<AutoexecParamVo> paramList) {

        List<AutoexecParamVo> newOperationParamVoList = new ArrayList<>();
        List<AutoexecToolAndScriptVo> ToolAndScriptVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            ToolAndScriptVoList.addAll(autoexecToolMapper.getToolListByIdList(toolIdList));
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            ToolAndScriptVoList.addAll(autoexecScriptMapper.getScriptListByIdList(scriptIdList));
        }
        for (AutoexecToolAndScriptVo toolAndScriptVo : ToolAndScriptVoList) {
            if (CollectionUtils.isNotEmpty(toolAndScriptVo.getParamList())) {
                newOperationParamVoList.addAll(toolAndScriptVo.getParamList());
            }
        }

        //根据name（唯一键）去重
        newOperationParamVoList = newOperationParamVoList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(AutoexecParamVo::getName))), ArrayList::new));

        //实时的参数信息
        Map<String, AutoexecParamVo> newOperationParamMap = newOperationParamVoList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));

        //旧的参数信息
        Map<String, AutoexecParamVo> oldOperationParamMap = null;
        if (CollectionUtils.isNotEmpty(paramList)) {
            oldOperationParamMap = paramList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));
        }

        //找出需要替换值的参数名称name
        List<String> replaceNameList = new ArrayList<>();
        if (MapUtils.isNotEmpty(newOperationParamMap) && MapUtils.isNotEmpty(oldOperationParamMap)) {
            for (String newParamName : newOperationParamMap.keySet()) {
                if (oldOperationParamMap.containsKey(newParamName) && StringUtils.equals(oldOperationParamMap.get(newParamName).getType(), newOperationParamMap.get(newParamName).getType())) {
                    replaceNameList.add(newParamName);
                }
            }
        }

        //根据参数名称name替换对应的值
        if (CollectionUtils.isNotEmpty(replaceNameList)) {
            for (String name : replaceNameList) {
                newOperationParamMap.get(name).setConfig(oldOperationParamMap.get(name).getConfigStr());
            }
        }

        List<AutoexecParamVo> returnList = new ArrayList<>();
        for (String name : newOperationParamMap.keySet()) {
            returnList.add(newOperationParamMap.get(name));
        }

        return returnList;
    }

    /**
     * 根据profileId查询关联的tool、script工具
     *
     * @param id
     * @return
     */
    @Override
    public List<AutoexecToolAndScriptVo> getAutoexecToolAndScriptVoListByProfileId(Long id) {
        List<AutoexecToolAndScriptVo> returnToolAndScriptVoList = new ArrayList<>();
        Map<String, List<Long>> toolIdListAndScriptIdListMap = getAutoexecToolIdListAndScriptIdListByProfileId(id);
        //tool
        if (toolIdListAndScriptIdListMap.containsKey(ToolType.TOOL.getValue())) {
            returnToolAndScriptVoList.addAll(autoexecToolMapper.getToolListByIdList(toolIdListAndScriptIdListMap.get(ToolType.TOOL.getValue())));
        }
        //script
        if (toolIdListAndScriptIdListMap.containsKey(ToolType.SCRIPT.getValue())) {
            returnToolAndScriptVoList.addAll(autoexecScriptMapper.getScriptListByIdList(toolIdListAndScriptIdListMap.get(ToolType.SCRIPT.getValue())));
        }
        return returnToolAndScriptVoList;
    }


    /**
     * 根据profileId查询关联的toolIdList、scriptIdList
     * @param id
     * @return
     */
    public Map<String,List<Long>> getAutoexecToolIdListAndScriptIdListByProfileId(Long id) {
        Map<String, List<Long>> returnMap = new HashMap<>();
        DeployProfileVo profileVo = deployProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new DeployProfileIsNotFoundException(id);
        }
        List<AutoexecToolAndScriptVo> toolAndScriptVoList = profileVo.getAutoexecToolAndScriptVoList();

        Map<String, List<AutoexecToolAndScriptVo>> toolAndScriptMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolAndScriptVoList)) {
            toolAndScriptMap = toolAndScriptVoList.stream().collect(Collectors.groupingBy(AutoexecToolAndScriptVo::getType));
        }
        //tool
        List<Long> toolIdList = null;
        if (toolAndScriptMap.containsKey(ToolType.TOOL.getValue())) {
            toolIdList = toolAndScriptMap.get(ToolType.TOOL.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            returnMap.put(ToolType.TOOL.getValue(), toolIdList);
        }
        //script
        List<Long> scriptIdList = null;
        if (toolAndScriptMap.containsKey(ToolType.SCRIPT.getValue())) {
            scriptIdList = toolAndScriptMap.get(ToolType.SCRIPT.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            returnMap.put(ToolType.SCRIPT.getValue(), scriptIdList);
        }
        return returnMap;
    }

}
