/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job.source.type;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.JobNodeStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.dto.job.AutoexecSqlDetailVo;
import codedriver.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerGroupRunnerNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.dao.mapper.runner.RunnerMapper;
import codedriver.framework.deploy.constvalue.BuildNoStatus;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.job.DeployJobContentVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import codedriver.framework.deploy.dto.version.DeployVersionBuildNoVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import codedriver.framework.deploy.exception.DeployPipelineConfigNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
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
public class DeployJobSourceTypeHandler extends AutoexecJobSourceTypeHandlerBase {

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
        List<Long> resetSqlIdList = null;
        if (!Objects.isNull(paramObj.getInteger("isAll")) && paramObj.getInteger("isAll") == 1) {
            List<AutoexecJobPhaseNodeVo> jobPhaseNodeVos = new ArrayList<>();
            //重置phase的所有sql文件状态
            resetSqlIdList = deploySqlMapper.getDeployJobSqlIdListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
        } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
            //批量重置sql文件状态
            resetSqlIdList = sqlIdArray.toJavaList(Long.class);
        }
        jobVo.setExecuteJobNodeVoList(deploySqlMapper.getDeployJobPhaseNodeListBySqlIdList(resetSqlIdList));
        deploySqlMapper.resetDeploySqlStatusBySqlIdList(resetSqlIdList);
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
        //TODO 逻辑还需要优化
        AutoexecJobPhaseVo targetPhaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(paramObj.getLong("jobId"), paramObj.getString("targetPhaseName"));
        if (targetPhaseVo == null) {
            return;
            //防止作业不包含"回退SQL"阶段，跳过
//            throw new AutoexecJobPhaseNotFoundException(paramObj.getString("targetPhaseName"));
        }

        Long jobId = paramObj.getLong("jobId");
        JSONArray paramSqlVoArray = paramObj.getJSONArray("sqlInfoList");

        List<DeploySqlDetailVo> oldDeploySqlList = deploySqlMapper.getAllDeploySqlDetailList(new DeploySqlDetailVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), paramObj.getString("version")));

        Map<String, DeploySqlDetailVo> jobPhaseAndSqlDetailMap = new HashMap<>();
        Map<String, DeploySqlDetailVo> sqlDetailMap = new HashMap<>();
        List<Long> needDeleteSqlIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(oldDeploySqlList)) {
            jobPhaseAndSqlDetailMap = oldDeploySqlList.stream().collect(Collectors.toMap(e -> e.getJobId().toString() + e.getPhaseName() + e.getResourceId().toString() + e.getSqlFile(), e -> e));
            for (DeploySqlDetailVo detailVo : oldDeploySqlList) {
                sqlDetailMap.putIfAbsent(detailVo.getResourceId().toString() + detailVo.getSqlFile(), detailVo);
            }
            needDeleteSqlIdList = oldDeploySqlList.stream().map(DeploySqlDetailVo::getId).collect(Collectors.toList());
        }
        List<DeploySqlDetailVo> insertSqlDetailList = new ArrayList<>();
        List<DeploySqlDetailVo> updateSqlList = new ArrayList<>();
        List<Long> insertSqlIdList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {
            List<DeploySqlDetailVo> sqlDetailVoList = paramSqlVoArray.toJavaList(DeploySqlDetailVo.class);
            for (int i = 0; i < sqlDetailVoList.size(); i++) {

                DeploySqlDetailVo newSqlVo = sqlDetailVoList.get(i);
                newSqlVo.setSort(i);
                DeploySqlDetailVo oldSqlVo = jobPhaseAndSqlDetailMap.get(jobId.toString() + targetPhaseVo.getName() + newSqlVo.getResourceId().toString() + newSqlVo.getSqlFile());
                //不存在则新增
                if (oldSqlVo == null) {
                    DeploySqlDetailVo deploySqlDetailVo = sqlDetailMap.get(newSqlVo.getResourceId().toString() + newSqlVo.getSqlFile());
                    if (deploySqlDetailVo != null) {
                        newSqlVo.setId(deploySqlDetailVo.getId());
                        updateSqlList.add(newSqlVo);
                        insertSqlIdList.add(newSqlVo.getId());
                        continue;
                    } else {
                        insertSqlDetailList.add(newSqlVo);
                        continue;
                    }
                }

                if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
                    //旧数据 - 需要更新的数据 = 需要删除的数据
                    needDeleteSqlIdList.remove(oldSqlVo.getId());
                }
                newSqlVo.setId(oldSqlVo.getId());
                updateSqlList.add(newSqlVo);
            }

        }
        if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
            deploySqlMapper.updateDeploySqlIsDeleteByIdList(needDeleteSqlIdList);
        }
        if (CollectionUtils.isNotEmpty(insertSqlDetailList)) {
            for (DeploySqlDetailVo insertSqlVo : insertSqlDetailList) {
                deploySqlMapper.insertDeploySql(new DeploySqlJobPhaseVo(paramObj.getLong("jobId"), paramObj.getString("targetPhaseName"), targetPhaseVo.getId(), insertSqlVo.getId()));
                deploySqlMapper.insertDeploySqlDetail(insertSqlVo, paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramObj.getLong("runnerId"));
            }
        }
        if (CollectionUtils.isNotEmpty(insertSqlIdList)) {
            for (Long sqlId : insertSqlIdList) {
                deploySqlMapper.insertDeploySql(new DeploySqlJobPhaseVo(paramObj.getLong("jobId"), paramObj.getString("targetPhaseName"), targetPhaseVo.getId(), sqlId));
            }
        }
        if (CollectionUtils.isNotEmpty(updateSqlList)) {
            for (DeploySqlDetailVo sqlDetailVo : updateSqlList) {
                deploySqlMapper.updateDeploySqlDetail(sqlDetailVo);
            }
        }
    }

    @Override
    public void updateSqlStatus(JSONObject paramObj) {
        DeploySqlDetailVo paramDeploySqlVo = new DeploySqlDetailVo(paramObj.getJSONObject("sqlStatus"));
        DeploySqlDetailVo oldDeploySqlVo = deploySqlMapper.getDeploySqlDetail(new DeploySqlDetailVo(paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramDeploySqlVo.getSqlFile(), paramObj.getLong("jobId"), paramObj.getString("phaseName")));
        if (oldDeploySqlVo != null) {
            paramDeploySqlVo.setId(oldDeploySqlVo.getId());
            deploySqlMapper.updateDeploySqlDetail(paramDeploySqlVo);
        } else {
            AutoexecJobPhaseVo phaseVo = autoexecJobMapper.getJobPhaseByJobIdAndPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
            if (phaseVo == null) {
                throw new AutoexecJobPhaseNotFoundException(paramObj.getString("phaseName"));
            }
            deploySqlMapper.insertDeploySql(new DeploySqlJobPhaseVo(paramObj.getLong("jobId"), paramObj.getString("phaseName"), phaseVo.getId(), paramDeploySqlVo.getId()));
            deploySqlMapper.insertDeploySqlDetail(paramDeploySqlVo, paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramObj.getLong("runnerId"));
        }
    }

    @Override
    public AutoexecSqlDetailVo getSqlDetail(AutoexecJobVo jobVo) {
        Long sqlId = jobVo.getActionParam().getLong("nodeId");
        DeploySqlDetailVo deploySqlDetailVo = deploySqlMapper.getDeployJobSqlDetailById(sqlId);
        AutoexecSqlDetailVo autoexecSqlDetailVo = null;
        if (deploySqlDetailVo != null) {
            autoexecSqlDetailVo = new AutoexecSqlDetailVo();
            autoexecSqlDetailVo.setJobId(jobVo.getId());
            autoexecSqlDetailVo.setRunnerId(deploySqlDetailVo.getRunnerId());
            autoexecSqlDetailVo.setPhaseName(jobVo.getCurrentPhase().getName());
            autoexecSqlDetailVo.setHost(deploySqlDetailVo.getHost());
            autoexecSqlDetailVo.setPort(deploySqlDetailVo.getPort());
            autoexecSqlDetailVo.setResourceId(deploySqlDetailVo.getResourceId());
        }
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
        deployJobMapper.updateDeployJobRunnerMapId(jobId, runnerMapId);
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
        combopVo.setConfig(deployPipelineConfigVo.getAutoexecCombopConfigVo());
        return combopVo;
    }

    @Override
    public void updateInvokeJob(JSONObject paramJson, AutoexecJobVo jobVo) {
        DeployJobVo deployJobVo = new DeployJobVo(paramJson);
        deployJobVo.setId(jobVo.getId());
        deployJobVo.setConfigHash(jobVo.getConfigHash());
        deployJobMapper.insertIgnoreDeployJobContent(new DeployJobContentVo(jobVo.getConfigStr()));
        Integer buildNo = paramJson.getInteger("buildNo");
        //如果buildNo是-1，表示新建buildNo
        if (buildNo != null ) {
            DeployVersionVo deployVersionVo = deployVersionMapper.getVersionByAppSystemIdAndAppModuleIdAndVersion(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId(), deployJobVo.getVersion());
            if (deployVersionVo == null) {
                throw new DeployVersionNotFoundException(deployJobVo.getVersion());
            }
            //获取最新buildNo
            if (buildNo == -1) {
                Integer maxBuildNo = deployVersionMapper.getDeployVersionMaxBuildNoByVersionIdLock(deployVersionVo.getId());
                if (maxBuildNo == null) {
                    deployJobVo.setBuildNo(1);
                } else {
                    deployJobVo.setBuildNo(maxBuildNo + 1);
                }
                deployJobMapper.insertDeployVersionBuildNo(new DeployVersionBuildNoVo(deployVersionVo.getId(), deployJobVo.getBuildNo(), deployJobVo.getId(), BuildNoStatus.PENDING.getValue()));
            } else if (buildNo > 0) {
                deployJobVo.setBuildNo(buildNo);
            }
            deployJobMapper.insertDeployVersionBuildNo(new DeployVersionBuildNoVo(deployVersionVo.getId(), deployJobVo.getBuildNo(), deployJobVo.getId(), BuildNoStatus.PENDING.getValue()));
        }
        deployJobMapper.insertDeployJob(deployJobVo);
        jobVo.setInvokeId(deployJobVo.getId());
        jobVo.setOperationId(deployJobVo.getId());
    }

    @Override
    public List<AutoexecJobPhaseNodeVo> getJobNodeListBySqlIdList(List<Long> sqlIdList) {
        return deploySqlMapper.getDeployJobPhaseNodeListBySqlIdList(sqlIdList);
    }

    @Override
    public boolean getIsCanUpdatePhaseRunner(AutoexecJobPhaseVo jobPhaseVo, Long runnerMapId) {
        List<DeploySqlDetailVo> deploySqlDetailVos = deploySqlMapper.getDeployJobSqlDetailByExceptStatusListAndRunnerMapId(jobPhaseVo.getJobId(), jobPhaseVo.getName(), Arrays.asList(JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.IGNORED.getValue()), runnerMapId);
        return deploySqlDetailVos.size() == 0;
    }

    @Override
    public void getMyFireParamJson(JSONObject jsonObject, AutoexecJobVo jobVo) {
        JSONObject environment = new JSONObject();
        jsonObject.put("environment", environment);
        //_DEPLOY_RUNNER GROUP
        JSONObject runnerMap = new JSONObject();
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobVo.getId());
        IResourceCrossoverMapper iResourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = iResourceCrossoverMapper.getAppSystemById(deployJobVo.getAppSystemId(), TenantContext.get().getDataDbName());
        if (appSystem == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppSystemId());
        }
        ResourceVo appModule = iResourceCrossoverMapper.getAppModuleById(deployJobVo.getAppModuleId(),TenantContext.get().getDataDbName());
        if (appModule == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppModuleId());
        }
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId());
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(appSystem.getName() + "(" + deployJobVo.getAppSystemId() + ")", appModule.getName() + "(" + deployJobVo.getAppModuleId() + ")");
        }
        if (CollectionUtils.isEmpty(runnerGroupVo.getRunnerMapList())) {
            throw new AutoexecJobRunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        for (RunnerMapVo runnerMapVo : runnerGroupVo.getRunnerMapList()) {
            runnerMap.put(runnerMapVo.getRunnerMapId().toString(), runnerMapVo.getHost());
        }
        environment.put("DEPLOY_RUNNERGROUP", runnerMap);
        //_DEPLOY_PATH
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo envEntity = iCiEntityCrossoverMapper.getCiEntityBaseInfoById(deployJobVo.getEnvId());
        if (envEntity == null) {
            throw new CiEntityNotFoundException(deployJobVo.getEnvId());
        }
        environment.put("DEPLOY_PATH", appSystem.getAbbrName() + "/" + appModule.getAbbrName() + "/" + envEntity.getName());
        environment.put("DEPLOY_ID_PATH", deployJobVo.getAppSystemId() + "/" + deployJobVo.getAppModuleId() + "/" + deployJobVo.getEnvId());
        environment.put("VERSION", deployJobVo.getVersion());
        environment.put("BUILD_NO", deployJobVo.getBuildNo());
    }
}
