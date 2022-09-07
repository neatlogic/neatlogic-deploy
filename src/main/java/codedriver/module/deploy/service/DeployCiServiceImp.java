package codedriver.module.deploy.service;

import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerVo;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DeployCiServiceImp implements DeployCiService {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    public RunnerVo getRandomRunnerBySystemIdAndModuleId(CiEntityVo system, CiEntityVo module) {
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(system.getId(), module.getId());
        if (runnerGroupVo == null || CollectionUtils.isEmpty(runnerGroupVo.getRunnerList())) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(system.getName() + "(" + system.getId() + ")", module.getName() + "(" + module.getId() + ")");
        }
        List<RunnerVo> runnerList = runnerGroupVo.getRunnerList();
        return runnerList.get((int) (Math.random() * runnerList.size()));
    }

}
