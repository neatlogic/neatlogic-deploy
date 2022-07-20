/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job.source.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.job.AutoexecSqlDetailVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerGroupRunnerNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.constvalue.BuildNoStatus;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.DeployJobContentVo;
import codedriver.framework.deploy.dto.DeployJobVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.deploy.exception.DeployPipelineConfigNotFoundException;
import codedriver.framework.deploy.exception.DeploySqlJobPhaseNotFoundException;
import codedriver.framework.dto.runner.RunnerGroupVo;
import codedriver.framework.dto.runner.RunnerMapVo;
import codedriver.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeploySqlMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/5/31 5:22 下午
 */
@Service
public class DeployJobSourceHandler extends AutoexecJobSourceActionHandlerBase {

    @Resource
    DeploySqlMapper deploySqlMapper;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Resource
    RunnerMapper runnerMapper;

    @Resource
    DeployAppPipelineService deployAppPipelineService;

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return JobSourceType.DEPLOY.getValue();
    }

    @Override
    public void saveJobPhase(AutoexecCombopPhaseVo combopPhaseVo) {

    }

    @Override
    public JSONObject getJobSqlContent(AutoexecJobVo jobVo) {
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        return JSONObject.parseObject(AutoexecUtil.requestRunner(nodeVo.getRunnerUrl() + "/api/rest/job/phase/node/sql/content/get", paramObj));
    }

    @Override
    public void downloadJobSqlFile(AutoexecJobVo jobVo) throws Exception {
        JSONObject paramObj = jobVo.getActionParam();
        DeploySqlDetailVo sqlDetailVo = deploySqlMapper.getDeployJobSqlDetailById(paramObj.getLong("sqlId"));
        paramObj.put("sysId", sqlDetailVo.getSysId());
        paramObj.put("moduleId", sqlDetailVo.getModuleId());
        paramObj.put("envId", sqlDetailVo.getEnvId());
        paramObj.put("version", sqlDetailVo.getVersion());
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo envCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(sqlDetailVo.getEnvId());
        paramObj.put("envName", envCiEntity.getName());
        UserContext.get().getResponse().setContentType("text/plain");
        UserContext.get().getResponse().setHeader("Content-Disposition", " attachment; filename=\"" + paramObj.getString("sqlName") + "\"");
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        String url = nodeVo.getRunnerUrl() + "/api/binary/deploy/sql/file/download";
        String result = HttpRequestUtil.download(url, "POST", UserContext.get().getResponse().getOutputStream()).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest().getError();

        if (StringUtils.isNotBlank(result)) {
            throw new AutoexecJobRunnerHttpRequestException(url + ":" + result);
        }
    }

    @Override
    public void resetSqlStatus(JSONObject paramObj, AutoexecJobVo jobVo) {
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        if (!Objects.isNull(paramObj.getInteger("isAll")) && paramObj.getInteger("isAll") == 1) {
            //重置phase的所有sql文件状态
            List<Long> resetSqlIdList = deploySqlMapper.getDeployJobSqlIdListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
            if (CollectionUtils.isNotEmpty(resetSqlIdList)) {
                deploySqlMapper.resetDeploySqlStatusBySqlIdList(resetSqlIdList);
            }
        } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
            //批量重置sql文件状态
            deploySqlMapper.resetDeploySqlStatusBySqlIdList(sqlIdArray.toJavaList(Long.class));
        }
    }

    @Override
    public JSONObject searchJobPhaseSql(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        List<DeploySqlDetailVo> returnList = new ArrayList<>();
        jobPhaseNodeVo.setJobPhaseName(autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseNodeVo.getJobPhaseId()).getName());
        int sqlCount = deploySqlMapper.searchDeploySqlCount(jobPhaseNodeVo);
        if (sqlCount > 0) {
            jobPhaseNodeVo.setRowNum(sqlCount);
            returnList = deploySqlMapper.searchDeploySql(jobPhaseNodeVo);
        }
        return TableResultUtil.getResult(returnList, jobPhaseNodeVo);
    }

    @Override
    public void checkinSqlList(JSONObject paramObj) {
        Long jobId = paramObj.getLong("jobId");
        JSONArray paramSqlVoArray = paramObj.getJSONArray("sqlInfoList");

        List<DeploySqlDetailVo> oldDeploySqlList = deploySqlMapper.getAllDeploySqlDetailList(new DeploySqlDetailVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), paramObj.getString("version")));

        Map<String, DeploySqlDetailVo> jobPhaseANdSqlDetailMap = new HashMap<>();
        Map<String, DeploySqlDetailVo> sqlDetailMap = new HashMap<>();
        List<Long> needDeleteSqlIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(oldDeploySqlList)) {
            jobPhaseANdSqlDetailMap = oldDeploySqlList.stream().collect(Collectors.toMap(e -> e.getJobId().toString() + e.getPhaseName() + e.getResourceId().toString() + e.getSqlFile(), e -> e));
            for (DeploySqlDetailVo detailVo : oldDeploySqlList) {
                sqlDetailMap.putIfAbsent(detailVo.getResourceId().toString() + detailVo.getSqlFile(), detailVo);
            }
            needDeleteSqlIdList = oldDeploySqlList.stream().map(DeploySqlDetailVo::getId).collect(Collectors.toList());
        }
        List<DeploySqlDetailVo> insertSqlList = new ArrayList<>();
        List<Long> reEnabledSqlList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {

            for (DeploySqlDetailVo newSqlVo : paramSqlVoArray.toJavaList(DeploySqlDetailVo.class)) {
                DeploySqlDetailVo oldSqlVo = jobPhaseANdSqlDetailMap.get(jobId.toString() + newSqlVo.getPhaseName() + newSqlVo.getResourceId().toString() + newSqlVo.getSqlFile());
                //不存在则新增
                if (oldSqlVo == null) {
                    DeploySqlDetailVo deploySqlDetailVo = sqlDetailMap.get(newSqlVo.getResourceId().toString() + newSqlVo.getSqlFile());
                    if (deploySqlDetailVo != null) {
                        newSqlVo.setId(deploySqlDetailVo.getId());
                    }
                    insertSqlList.add(newSqlVo);
                    continue;
                }
                if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
                    //旧数据 - 需要更新的数据 = 需要删除的数据
                    needDeleteSqlIdList.remove(oldSqlVo.getId());
                }
                if (oldSqlVo.getIsDelete() == 1) {
                    //需要重新启用的数据
                    reEnabledSqlList.add(oldSqlVo.getId());
                }
            }

        }
        if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
            deploySqlMapper.updateDeploySqlIsDeleteByIdList(needDeleteSqlIdList, 1);
        }
        if (CollectionUtils.isNotEmpty(insertSqlList)) {
            for (DeploySqlDetailVo insertSqlVo : insertSqlList) {
                deploySqlMapper.insertDeploySql(new DeploySqlJobPhaseVo(paramObj.getLong("jobId"), paramObj.getString("targetPhaseName"), insertSqlVo.getId()));
                deploySqlMapper.insertDeploySqlDetail(insertSqlVo, paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramObj.getLong("runnerId"));
            }
        }
        if (CollectionUtils.isNotEmpty(reEnabledSqlList)) {
            deploySqlMapper.updateDeploySqlIsDeleteByIdList(reEnabledSqlList, 0);
        }
    }

    @Override
    public void updateSqlStatus(JSONObject paramObj) {
        DeploySqlDetailVo paramDeploySqlVo = new DeploySqlDetailVo(paramObj.getJSONObject("sqlStatus"));
        DeploySqlDetailVo oldDeploySqlVo = deploySqlMapper.getDeploySqlDetail(new DeploySqlDetailVo(paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramDeploySqlVo.getSqlFile(), paramObj.getLong("jobId"), paramObj.getString("phaseName")));
        if (oldDeploySqlVo != null) {
            deploySqlMapper.updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(paramDeploySqlVo.getStatus(), paramDeploySqlVo.getMd5(), oldDeploySqlVo.getId());
        } else {
            deploySqlMapper.insertDeploySql(new DeploySqlJobPhaseVo(paramObj.getLong("jobId"), paramObj.getString("phaseName"), paramDeploySqlVo.getId()));
            deploySqlMapper.insertDeploySqlDetail(paramDeploySqlVo, paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramObj.getLong("runnerId"));
        }
    }

    @Override
    public AutoexecSqlDetailVo getSqlDetail(AutoexecJobVo jobVo) {
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        List<Long> sqlIdList = deploySqlMapper.getDeployJobSqlIdListByJobIdAndJobPhaseNameList(jobPhaseVo.getJobId(), Collections.singletonList(jobPhaseVo.getName()));
        if (CollectionUtils.isEmpty(sqlIdList)) {
            throw new DeploySqlJobPhaseNotFoundException(jobPhaseVo.getName());
        }
        Long sqlId = sqlIdList.get(0);
        DeploySqlDetailVo deploySqlDetailVo = deploySqlMapper.getDeployJobSqlDetailById(sqlId);
        AutoexecSqlDetailVo autoexecSqlDetailVo = null;
        if (deploySqlDetailVo != null) {
            autoexecSqlDetailVo = new AutoexecSqlDetailVo();
            autoexecSqlDetailVo.setJobId(jobVo.getId());
            autoexecSqlDetailVo.setRunnerId(deploySqlDetailVo.getRunnerId());
            autoexecSqlDetailVo.setPhaseName(jobPhaseVo.getName());
            autoexecSqlDetailVo.setHost(deploySqlDetailVo.getHost());
            autoexecSqlDetailVo.setPort(deploySqlDetailVo.getPort());
            autoexecSqlDetailVo.setResourceId(deploySqlDetailVo.getResourceId());
        }
        jobVo.getActionParam().put("sqlId",sqlId);
        return autoexecSqlDetailVo;
    }

    @Override
    public List<RunnerMapVo> getRunnerMapList(AutoexecJobVo jobVo) {
        List<RunnerMapVo> runnerMapVos = null;
        AutoexecJobPhaseVo jobPhaseVo = jobVo.getCurrentPhase();
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobVo.getId());
        //如果是sqlfile ｜ local ，则保证一个作业使用同一个runner
        if (Arrays.asList(ExecMode.SQL.getValue(), ExecMode.RUNNER.getValue()).contains(jobPhaseVo.getExecMode()) && deployJobVo.getRunnerMapId() != null) {
            RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(deployJobVo.getRunnerMapId());
            if (runnerMapVo == null) {
                throw new RunnerNotFoundByRunnerMapIdException(deployJobVo.getRunnerMapId());
            }
            runnerMapVos = Collections.singletonList(runnerMapVo);
        } else {
            //其它则根据模块均衡分配runner
            ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
            CiEntityVo appSystemEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getAppSystemId());
            if (appSystemEntity == null) {
                throw new CiEntityNotFoundException(deployJobVo.getAppSystemId());
            }
            CiEntityVo appModuleEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getAppModuleId());
            if (appModuleEntity == null) {
                throw new CiEntityNotFoundException(deployJobVo.getAppModuleId());
            }
            RunnerGroupVo appModuleRunnerGroup = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId());
            if (appModuleRunnerGroup == null) {
                throw new DeployAppConfigModuleRunnerGroupNotFoundException(appSystemEntity.getName() + "(" + deployJobVo.getAppSystemId() + ")", appModuleEntity.getName() + "(" + deployJobVo.getAppModuleId() + ")");
            }
            RunnerGroupVo groupVo = runnerMapper.getRunnerMapGroupById(appModuleRunnerGroup.getId());
            if (groupVo == null) {
                throw new AutoexecJobRunnerGroupRunnerNotFoundException(appModuleRunnerGroup.getId().toString());
            }
            if (CollectionUtils.isEmpty(groupVo.getRunnerMapList())) {
                throw new AutoexecJobRunnerGroupRunnerNotFoundException(groupVo.getName() + "(" + groupVo.getId() + ") ");
            }
            runnerMapVos = groupVo.getRunnerMapList();
        }
        return runnerMapVos;
    }

    @Override
    public void updateJobRunnerMap(Long jobId, Long runnerMapId) {
        DeployJobVo deployJobVo = new DeployJobVo(jobId, runnerMapId);
        deployJobMapper.updateDeployJobRunnerMapId(deployJobVo);
    }

    @Override
    public AutoexecCombopVo getAutoexecCombop(JSONObject paramJson) {
        Long appSystemId = paramJson.getLong("appSystemId");
        Long appModuleId = paramJson.getLong("appModuleId");
        Long envId = paramJson.getLong("envId");
        //获取最终流水线
        DeployPipelineConfigVo deployPipelineConfigVo = deployAppPipelineService.getDeployPipelineConfigVo(new DeployAppConfigVo(appSystemId, appModuleId, envId));
        if (deployPipelineConfigVo == null) {
            throw new DeployPipelineConfigNotFoundException();
        }
        AutoexecCombopVo combopVo = new AutoexecCombopVo();
        combopVo.setConfigStr(JSONObject.toJSONString(deployPipelineConfigVo));
        return combopVo;
    }

    @Override
    public void updateInvokeJob(JSONObject paramJson, AutoexecJobVo jobVo) {
        DeployJobVo deployJobVo = new DeployJobVo(paramJson);
        deployJobVo.setJobId(jobVo.getId());
        deployJobVo.setConfigHash(jobVo.getConfigHash());
        deployJobMapper.insertIgnoreDeployJobContent(new DeployJobContentVo(deployJobVo.getConfigHash(), jobVo.getConfigStr()));
        if (paramJson.getInteger("buildNo") != null) {
            deployJobVo.setBuildNo(paramJson.getInteger("buildNo"));
        } else {
            //获取最新buildNo
            DeployVersionVo deployVersionVo = deployVersionMapper.getVersionByAppSystemIdAndAppModuleIdAndVersion(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId(), deployJobVo.getVersion());
            Integer maxBuildNo = deployVersionMapper.getDeployVersionMaxBuildNoByVersionIdLock(deployVersionVo.getId());
            if (maxBuildNo == null) {
                deployJobVo.setBuildNo(1);
            } else {
                deployJobVo.setBuildNo(maxBuildNo + 1);
            }
            deployJobMapper.insertDeployVersionBuildNo(new DeployVersionBuildNoVo(deployVersionVo.getId(), deployJobVo.getBuildNo(), deployJobVo.getJobId(), BuildNoStatus.PENDING.getValue()));
        }
        deployJobMapper.insertDeployJob(deployJobVo);
    }

    @Override
    public void getMyFireParamJson(JSONObject jsonObject, AutoexecJobVo jobVo) {
        JSONObject environment = new JSONObject();
        jsonObject.put("environment", environment);
        //_DEPLOY_RUNNERGROUP
        JSONObject runnerMap = new JSONObject();
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobVo.getId());
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo appSystemEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getAppSystemId());
        if (appSystemEntity == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppSystemId());
        }
        CiEntityVo appModuleEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getAppModuleId());
        if (appModuleEntity == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppModuleId());
        }
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId());
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(appSystemEntity.getName() + "(" + deployJobVo.getAppSystemId() + ")", appModuleEntity.getName() + "(" + deployJobVo.getAppModuleId() + ")");
        }
        if (CollectionUtils.isEmpty(runnerGroupVo.getRunnerMapList())) {
            throw new AutoexecJobRunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        for (RunnerMapVo runnerMapVo : runnerGroupVo.getRunnerMapList()) {
            runnerMap.put(runnerMapVo.getRunnerMapId().toString(), runnerMapVo.getHost());
        }
        environment.put("DEPLOY_RUNNERGROUP", runnerMap);
        //_DEPLOY_PATH
        CiEntityVo envEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getEnvId());
        if (envEntity == null) {
            throw new CiEntityNotFoundException(deployJobVo.getEnvId());
        }
        environment.put("DEPLOY_PATH", appSystemEntity.getName() + "/" + appModuleEntity.getName() + "/" + envEntity.getName());
        environment.put("DEPLOY_ID_PATH", deployJobVo.getAppSystemId() + "/" + deployJobVo.getAppModuleId() + "/" + deployJobVo.getEnvId());
        environment.put("VERSION", deployJobVo.getVersion());
        environment.put("BUILD_NO", deployJobVo.getBuildNo());
    }
}
