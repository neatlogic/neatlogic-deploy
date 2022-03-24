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

//         说明：
//         新的参数列表：工具和脚本参数的去重集合（name唯一键）
//         旧的参数列表：数据库存的
//         新旧名称和类型都相同时，将继续使用旧参数值，不做值是否存在的校验，前端回填失败提示即可
//

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

        //实时的参数信息
        Map<String, AutoexecParamVo> newOperationParamMap = toolAndScriptParamVoList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));

        //旧的参数信息
        Map<String, AutoexecParamVo> oldOperationParamMap = new HashMap<>();
        if (CollectionUtils.isEmpty(paramList)) {
            List<AutoexecParamVo> oldParamList = paramList.toJavaList(AutoexecParamVo.class);
            oldOperationParamMap = oldParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));
        }

        //找出需要替换值的参数
        List<String> replaceNameList = new ArrayList<>();
        if (MapUtils.isNotEmpty(newOperationParamMap) && MapUtils.isNotEmpty(oldOperationParamMap)) {
            for (String newParamName : newOperationParamMap.keySet()) {
                if (oldOperationParamMap.containsKey(newParamName) && StringUtils.equals(oldOperationParamMap.get(newParamName).getType(), newOperationParamMap.get(newParamName).getType())) {
                    replaceNameList.add(newParamName);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(replaceNameList)) {
            for (String name : replaceNameList) {
                newOperationParamMap.get(name).setConfig(oldOperationParamMap.get(name).getConfigStr());
            }
        }

        List<AutoexecParamVo> returnList = new ArrayList<>();
        for (String name : newOperationParamMap.keySet()){
            returnList.add(newOperationParamMap.get(name));
        }

            return returnList;
    }

}
