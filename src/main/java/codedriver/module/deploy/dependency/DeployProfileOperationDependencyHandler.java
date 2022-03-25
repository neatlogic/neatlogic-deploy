package codedriver.module.deploy.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.crossover.IAutoexecScriptServiceCrossoverService;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.deploy.constvalue.DeployFromType;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 工具profile引用工具库工具处理器
 *
 * @author longrf
 * @date 2022/3/18 10:36 上午
 */
@Service
public class DeployProfileOperationDependencyHandler extends CustomTableDependencyHandlerBase {

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    /**
     * 表名
     *
     * @return
     */
    @Override
    protected String getTableName() {
        return "deploy_profile_operation";
    }

    /**
     * 被引用者（上游）字段
     *
     * @return
     */
    @Override
    protected String getFromField() {
        return "profile_id";
    }

    /**
     * 引用者（下游）字段
     *
     * @return
     */
    @Override
    protected String getToField() {
        return "option_id";
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    /**
     * 解析数据，拼装跳转url，返回引用下拉列表一个选项数据结构
     *
     * @param dependencyObj 调用者值
     * @return
     */
    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj instanceof Map) {
            Map<String, Object> map = (Map) dependencyObj;
            Long operateId = (Long) map.get("operate_id");
            String type = (String) map.get("type");
            if (operateId == null) {
                return null;
            }
            if (StringUtils.equals(type, ToolType.TOOL.getValue())) {
                AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(operateId);
                if (autoexecToolVo != null) {
                    JSONObject dependencyInfoConfig = new JSONObject();
                    dependencyInfoConfig.put("toolId", autoexecToolVo.getId());
                    dependencyInfoConfig.put("toolName", autoexecToolVo.getName());
                    String pathFormat =  DeployFromType.DEPLOY_PROFILE_OPERATION.getText() + "-${DATA.toolName}";
                    String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/autoexec.html#/tool-detail?id=#{DATA.toolId}";
                    return new DependencyInfoVo(autoexecToolVo.getId(), dependencyInfoConfig, pathFormat, urlFormat, this.getGroupName());
                }
            } else if (StringUtils.equals(type, ToolType.SCRIPT.getValue())) {
                IAutoexecScriptServiceCrossoverService iAutoexecScriptServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecScriptServiceCrossoverService.class);
                return iAutoexecScriptServiceCrossoverService.getScriptDependencyPageUrl(map, operateId, this.getGroupName(), DeployFromType.DEPLOY_PROFILE_OPERATION.getText());
            }
        }
        return null;
    }

    @Override
    public IFromType getFromType() {
        return  DeployFromType.DEPLOY_PROFILE_OPERATION;
    }
}
