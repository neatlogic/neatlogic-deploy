package codedriver.module.deploy.service;

import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.dto.DeployJobVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionJobNotFoundException;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class DeployVersionServiceImp implements DeployVersionService {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Override
    public String getVersionRunnerUrl(JSONObject paramObj, DeployVersionVo version, String envName) {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        if (buildNo == null && envId == null) {
            throw new ParamNotExistsException("buildNo", "envId");
        }
        Long runnerMapId;
        if (buildNo != null) {
            Long jobId = deployVersionMapper.getJobIdByDeployVersionIdAndBuildNo(id, buildNo);
            DeployJobVo job = deployJobMapper.getDeployJobByJobId(jobId);
            if (job == null) {
                throw new DeployVersionJobNotFoundException(version.getVersion(), buildNo);
            }
            runnerMapId = job.getRunnerMapId();
        } else {
            Long jobId = deployVersionMapper.getJobIdByDeployVersionIdAndEnvId(id, envId);
            DeployJobVo job = deployJobMapper.getDeployJobByJobId(jobId);
            if (job == null) {
                throw new DeployVersionJobNotFoundException(version.getVersion(), envName);
            }
            runnerMapId = job.getRunnerMapId();
        }
        RunnerMapVo runner = runnerMapper.getRunnerByRunnerMapId(runnerMapId);
        if (runner == null) {
            throw new RunnerNotFoundByRunnerMapIdException(runnerMapId);
        }
        String url = runner.getUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    @Override
    public String getVersionResourceFullPath(DeployVersionVo version, String resourceType, Integer buildNo, String envName, String customPath) {
        // todo 路径待定
        return version.getAppSystemId() + "/" + version.getAppModuleId() + "/"
                + version.getVersion() + "/" + (buildNo != null ? "build" + "/" + buildNo : "env" + "/" + envName) + "/"
                + resourceType + "/" + customPath;
    }

    @Override
    public String getVersionEnvNameByEnvId(Long envId) {
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo env = ciEntityCrossoverMapper.getCiEntityBaseInfoById(envId);
        if (env != null) {
            return env.getName();
        }
        return null;
    }
}
