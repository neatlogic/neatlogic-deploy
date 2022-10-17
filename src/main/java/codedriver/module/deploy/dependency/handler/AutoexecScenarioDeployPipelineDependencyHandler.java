/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.dependency.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.constvalue.AutoexecFromType;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dependency.core.FixedTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.framework.dependency.dto.DependencyVo;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/10/14 18:31
 */

@Component
public class AutoexecScenarioDeployPipelineDependencyHandler extends FixedTableDependencyHandlerBase {
    @Override
    protected DependencyInfoVo parse(DependencyVo dependencyVo) {

        //由于删除配置项不会删除其关联的引用关系，因此查询时需要判断配置项是否还存在

        JSONObject config = dependencyVo.getConfig();
        if (MapUtils.isEmpty(config)) {
            return null;
        }
        Long configId = Long.valueOf(dependencyVo.getTo());

        Long appSystemId = config.getLong("appSystemId");
        Long moduleId = config.getLong("moduleId") != 0L ? config.getLong("moduleId") : null;
        Long envId = config.getLong("envId") != 0L ? config.getLong("envId") : null;
        List<Long> ciEntityIdList = new ArrayList<>();
        ciEntityIdList.add(appSystemId);
        if (Objects.nonNull(moduleId)) {
            ciEntityIdList.add(moduleId);
        }
        if (Objects.nonNull(envId)) {
            ciEntityIdList.add(envId);
        }

        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);

        //判断配置项是否仍存在
        List<CiEntityVo> ciEntityVoList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(ciEntityIdList);
        if (CollectionUtils.isEmpty(ciEntityVoList) || ciEntityVoList.size() != ciEntityIdList.size()) {
            //如果系统、模块、环境被删除，而这层关系也需要删除
            delete(configId);
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/");
        stringBuilder.append(TenantContext.get().getTenantUuid());
        stringBuilder.append("/deploy.html#/application-config-pipeline-detail?appSystemId=${DATA.appSystemId}");
        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("appSystemId", appSystemId);
        if (moduleId != null && moduleId != 0L) {
            dependencyInfoConfig.put("moduleId", moduleId);
            stringBuilder.append("&appModuleId=${DATA.moduleId}");
            if (envId != null && envId != 0L) {
                dependencyInfoConfig.put("envId", envId);
                stringBuilder.append("&envId=${DATA.envId}");
            }
        }
        List<String> pathList = new ArrayList<>();
        pathList.add("应用配置");
        if (appSystemId != null && appSystemId != 0) {
            CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(appSystemId);
            if (ciEntityVo != null) {
                pathList.add(ciEntityVo.getName());
            }
        }
        if (moduleId != null && moduleId != 0) {
            CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(moduleId);
            if (ciEntityVo != null) {
                pathList.add(ciEntityVo.getName());
            }
        }
        if (envId != null && envId != 0) {
            CiEntityVo ciEntityVo = ciEntityCrossoverMapper.getCiEntityBaseInfoById(envId);
            if (ciEntityVo != null) {
                pathList.add(ciEntityVo.getName());
            }
        }

        String urlFormat = stringBuilder.toString();
        return new DependencyInfoVo(configId, dependencyInfoConfig, "发布流水线", pathList, urlFormat, this.getGroupName());

    }

    @Override
    public IFromType getFromType() {
        return AutoexecFromType.SCENARIO;
    }
}
