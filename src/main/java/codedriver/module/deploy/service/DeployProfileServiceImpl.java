package codedriver.module.deploy.service;


import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecOperationVo;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
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
     * 根据profileId 获取profile参数
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
        return getProfileConfig(profileVo.getAutoexecOperationVoList(), deployProfileMapper.getProfileVoById(id).getParamList());
    }

    /**
     * 获取工具参数并去重
     * <p>
     * 新的参数列表newOperationParamVoList：工具和脚本参数的去重集合（name唯一键）
     * 旧的参数列表oldOperationParamList：数据库存的
     * 新旧名称和类型都相同时，将继续使用旧参数值，不做值是否存在的校验，前端回填失败提示即可
     * @param paramAutoexecOperationVoList
     * @param oldOperationParamList
     * @return
     */
    @Override
    public List<AutoexecParamVo> getProfileConfig(List<AutoexecOperationVo> paramAutoexecOperationVoList, List<AutoexecParamVo> oldOperationParamList) {

        List<Long> toolIdList = paramAutoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        List<Long> scriptIdList = paramAutoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        //获取新的参数列表
        List<AutoexecParamVo> newOperationParamVoList = new ArrayList<>();
        List<AutoexecOperationVo> autoexecOperationVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            autoexecOperationVoList.addAll(autoexecToolMapper.getAutoexecOperationListByIdList(toolIdList));
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            autoexecOperationVoList.addAll(autoexecScriptMapper.getAutoexecOperationListByIdList(scriptIdList));
        }
        for (AutoexecOperationVo operationVo : autoexecOperationVoList) {
            if (CollectionUtils.isNotEmpty(operationVo.getParamVoList())) {
                newOperationParamVoList.addAll(operationVo.getParamVoList());
            }
        }

        //根据name（唯一键）去重
        newOperationParamVoList = newOperationParamVoList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(AutoexecParamVo::getName))), ArrayList::new));

        //实时的参数信息
        Map<String, AutoexecParamVo> newOperationParamMap = newOperationParamVoList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));

        //旧的参数信息
        Map<String, AutoexecParamVo> oldOperationParamMap = null;
        if (CollectionUtils.isNotEmpty(oldOperationParamList)) {
            oldOperationParamMap = oldOperationParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));
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
                newOperationParamMap.get(name).setDefaultValue(oldOperationParamMap.get(name).getDefaultValue());
            }
        }

        List<AutoexecParamVo> returnList = new ArrayList<>();
        for (String name : newOperationParamMap.keySet()) {
            returnList.add(newOperationParamMap.get(name));
        }

        return returnList;
    }

    /**
     * 保存profile和tool、script的关系
     *
     * @param profileId
     * @param autoexecOperationVoList
     */
    @Override
    public void saveProfileOperationByProfileIdAndAutoexecOperationVoList(Long profileId, List<AutoexecOperationVo> autoexecOperationVoList) {
        List<Long> toolIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.TOOL.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        List<Long> scriptIdList = autoexecOperationVoList.stream().filter(e -> StringUtils.equals(ToolType.SCRIPT.getValue(), e.getType())).map(AutoexecOperationVo::getId).collect(Collectors.toList());
        //tool
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            deployProfileMapper.insertDeployProfileOperationByProfileIdAndOperateIdListAndType(profileId, toolIdList, ToolType.TOOL.getValue());
        }
        //script
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            deployProfileMapper.insertDeployProfileOperationByProfileIdAndOperateIdListAndType(profileId, scriptIdList, ToolType.SCRIPT.getValue());
        }
    }
}
