/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job.source;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.source.AutoexecJobSourceHandlerBase;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.version.DeployVersionEnvVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.dto.globallock.GlobalLockVo;
import codedriver.framework.globallock.dao.mapper.GlobalLockMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Service
public class DeployJobSourceHandler extends AutoexecJobSourceHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    DeployJobMapper deployJobMapper;
    @Resource
    DeployVersionMapper deployVersionMapper;
    @Resource
    GlobalLockMapper globalLockMapper;

    @Override
    public String getName() {
        return JobSource.DEPLOY.getValue();
    }

    @Override
    public JSONObject getExtraJobInfo(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        AutoexecJobInvokeVo jobInvokeVo = autoexecJobMapper.getJobInvokeByJobId(jobVo.getId());
        if (jobInvokeVo != null) {
            DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobInvokeVo.getInvokeId());
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            result.put("appSystemId", deployJobVo.getAppSystemId());
            ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(deployJobVo.getAppSystemId(), TenantContext.get().getDataDbName());
            if (appSystem != null) {
                result.put("appSystemAbbrName", appSystem.getAbbrName());
                result.put("appSystemName", appSystem.getName());
            }
            result.put("appModuleId", deployJobVo.getAppModuleId());
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(deployJobVo.getAppModuleId(), TenantContext.get().getDataDbName());
            if (appModule != null) {
                result.put("appModuleAbbrName", appModule.getAbbrName());
                result.put("appModuleName", appModule.getName());
            }
            result.put("envId", deployJobVo.getEnvId());
            ResourceVo env = resourceCrossoverMapper.getAppEnvById(deployJobVo.getEnvId(), TenantContext.get().getDataDbName());
            if (env != null) {
                result.put("envAbbrName", env.getAbbrName());
                result.put("envName", env.getName());
            }
            DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersionId(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId(), deployJobVo.getVersionId());
            if (versionVo != null) {
                result.put("version", versionVo);
                result.put("buildNo", deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(), deployJobVo.getBuildNo()));
                DeployVersionEnvVo versionEnvVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(versionVo.getId(), deployJobVo.getEnvId());
                if (versionEnvVo != null) {
                    result.put("env", versionEnvVo);
                }
            }
            result.put("roundCount", jobVo.getRoundCount());

            //补充是否有资源锁
            GlobalLockVo globalLockVo = new GlobalLockVo();
            List<String> uuidList = globalLockMapper.getGlobalLockUuidByKey(JobSourceType.DEPLOY.getValue(), jobVo.getId().toString());
            if (CollectionUtils.isNotEmpty(uuidList)) {
                globalLockVo.setUuidList(uuidList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(r -> r))), ArrayList::new)));
            } else {
                //不存在则没有资源锁
                globalLockVo.setUuidList(Collections.singletonList("-1"));
            }
            globalLockVo.setHandler(JobSourceType.DEPLOY.getValue());
            int count = globalLockMapper.getLockCount(globalLockVo);
            if (count > 0) {
                result.put("isHasLock", 1);
            }
        }
        return result;
    }
}
