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
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.version.DeployVersionEnvVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class DeployJobSourceHandler extends AutoexecJobSourceHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    DeployJobMapper deployJobMapper;
    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return JobSource.DEPLOY.getValue();
    }

    @Override
    public JSONObject getExtraJobInfo(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        AutoexecJobInvokeVo jobInvokeVo = autoexecJobMapper.getJobInvokeByJobId(jobVo.getId());
        if(jobInvokeVo != null){
            DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobInvokeVo.getInvokeId());
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            result.put("appSystemId",deployJobVo.getAppSystemId());
            ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(deployJobVo.getAppSystemId(), TenantContext.get().getDataDbName());
            if (appSystem != null) {
                result.put("appSystemAbbrName",appSystem.getAbbrName());
                result.put("appSystemName",appSystem.getName());
            }
            result.put("appModuleId",deployJobVo.getAppModuleId());
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(deployJobVo.getAppModuleId(), TenantContext.get().getDataDbName());
            if (appModule != null) {
                result.put("appModuleAbbrName",appModule.getAbbrName());
                result.put("appModuleName",appModule.getName());
            }
            result.put("envId",deployJobVo.getEnvId());
            ResourceVo env = resourceCrossoverMapper.getAppEnvById(deployJobVo.getEnvId(), TenantContext.get().getDataDbName());
            if (env != null) {
                result.put("envAbbrName",env.getAbbrName());
                result.put("envName",env.getName());
            }
            DeployVersionVo versionVo = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersion(deployJobVo.getAppSystemId(),deployJobVo.getAppModuleId(),deployJobVo.getVersion());
            if(versionVo != null) {
                result.put("version", versionVo);
                result.put("buildNo", deployVersionMapper.getDeployVersionBuildNoByVersionIdAndBuildNo(versionVo.getId(),deployJobVo.getBuildNo()));
                DeployVersionEnvVo versionEnvVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(versionVo.getId(),deployJobVo.getEnvId());
                if(versionEnvVo != null) {
                    result.put("env", versionEnvVo);
                }
            }
            result.put("roundCount",jobVo.getRoundCount());
        }
        return result;
    }
}
