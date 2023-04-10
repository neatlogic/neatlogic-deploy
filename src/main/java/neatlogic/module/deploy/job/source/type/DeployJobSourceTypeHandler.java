/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.job.source.type;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.ISqlNodeDetail;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.job.*;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobPhaseNotFoundException;
import neatlogic.framework.autoexec.job.source.type.AutoexecJobSourceTypeHandlerBase;
import neatlogic.framework.autoexec.util.AutoexecUtil;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.auth.BATCHDEPLOY_MODIFY;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.constvalue.BuildNoStatus;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.job.DeployJobContentVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import neatlogic.framework.deploy.dto.sql.DeploySqlNodeDetailVo;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import neatlogic.framework.deploy.exception.DeployJobCannotExecuteException;
import neatlogic.framework.deploy.exception.DeployPipelineConfigNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.globallock.core.GlobalLockHandlerFactory;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.dao.mapper.DeploySqlMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        JSONObject paramObj = jobVo.getActionParam();
        paramObj.put("jobId", nodeVo.getJobId());
        paramObj.put("phase", nodeVo.getJobPhaseName());
        UserContext.get().getResponse().setContentType("text/plain");
        UserContext.get().getResponse().setHeader("Content-Disposition", " attachment; filename=\"" + paramObj.getString("sqlName") + "\"");
        String url = nodeVo.getRunnerUrl() + "/api/binary/job/phase/node/sql/file/download";
        String result = HttpRequestUtil.download(url, "POST", UserContext.get().getResponse().getOutputStream()).setPayload(paramObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest().getError();

        if (StringUtils.isNotBlank(result)) {
            throw new RunnerHttpRequestException(url + ":" + result);
        }
    }

    @Override
    public List<Long> getSqlIdsAndExecuteJobNodes(JSONObject paramObj, AutoexecJobVo jobVo) {
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        List<Long> resetSqlIdList = null;
        if (paramObj.getInteger("isAll") != null && paramObj.getInteger("isAll") == 1) {
            List<AutoexecJobPhaseNodeVo> jobPhaseNodeVos = new ArrayList<>();
            //重置phase的所有sql文件状态
            resetSqlIdList = deploySqlMapper.getDeployJobSqlIdListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
        } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
            //批量重置sql文件状态
            resetSqlIdList = sqlIdArray.toJavaList(Long.class);
        }
        jobVo.setExecuteJobNodeVoList(deploySqlMapper.getDeployJobPhaseNodeListBySqlIdList(resetSqlIdList));
        return resetSqlIdList;
    }

    @Override
    public void resetSqlStatus(JSONObject paramObj, AutoexecJobVo jobVo) {
        List<Long> resetSqlIdList = getSqlIdsAndExecuteJobNodes(paramObj, jobVo);
        deploySqlMapper.resetDeploySqlStatusBySqlIdList(resetSqlIdList);
    }

    @Override
    public int searchJobPhaseSqlCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        jobPhaseNodeVo.setJobPhaseName(autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseNodeVo.getJobPhaseId()).getName());
        return deploySqlMapper.searchDeploySqlCount(jobPhaseNodeVo);
    }

    @Override
    public JSONObject searchJobPhaseSql(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        List<DeploySqlNodeDetailVo> returnList = new ArrayList<>();
        jobPhaseNodeVo.setJobPhaseName(autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseNodeVo.getJobPhaseId()).getName());
        int sqlCount = deploySqlMapper.searchDeploySqlCount(jobPhaseNodeVo);
        if (sqlCount > 0) {
            jobPhaseNodeVo.setRowNum(sqlCount);
            returnList = deploySqlMapper.searchDeploySql(jobPhaseNodeVo);
        }
        return TableResultUtil.getResult(returnList, jobPhaseNodeVo);
    }

    @Override
    public List<ISqlNodeDetail> searchJobPhaseSqlForExport(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        List<ISqlNodeDetail> result = new ArrayList<>();
        jobPhaseNodeVo.setJobPhaseName(autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseNodeVo.getJobPhaseId()).getName());
        List<DeploySqlNodeDetailVo> list = deploySqlMapper.searchDeploySql(jobPhaseNodeVo);
        if (list.size() > 0) {
            list.forEach(o -> result.add(o));
        }
        return result;
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

        List<DeploySqlNodeDetailVo> oldDeploySqlList = deploySqlMapper.getAllDeploySqlDetailListWithJob(new DeploySqlNodeDetailVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), paramObj.getString("version")));

        Map<String, DeploySqlNodeDetailVo> jobPhaseAndSqlDetailMap = new HashMap<>();
        Map<String, DeploySqlNodeDetailVo> sqlDetailMap = new HashMap<>();
        List<Long> needDeleteSqlIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(oldDeploySqlList)) {
            jobPhaseAndSqlDetailMap = oldDeploySqlList.stream().collect(Collectors.toMap(e -> e.getJobId().toString() + e.getPhaseName() + e.getResourceId().toString() + e.getSqlFile(), e -> e));
            for (DeploySqlNodeDetailVo detailVo : oldDeploySqlList) {
                sqlDetailMap.putIfAbsent(detailVo.getResourceId().toString() + detailVo.getSqlFile(), detailVo);
            }
            needDeleteSqlIdList = oldDeploySqlList.stream().map(DeploySqlNodeDetailVo::getId).collect(Collectors.toList());
        }
        List<DeploySqlNodeDetailVo> insertSqlDetailList = new ArrayList<>();
        List<DeploySqlNodeDetailVo> updateSqlList = new ArrayList<>();
        List<Long> insertSqlIdList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {
            List<DeploySqlNodeDetailVo> sqlDetailVoList = paramSqlVoArray.toJavaList(DeploySqlNodeDetailVo.class);
            for (int i = 0; i < sqlDetailVoList.size(); i++) {

                DeploySqlNodeDetailVo newSqlVo = sqlDetailVoList.get(i);
                newSqlVo.setSort(i);
                DeploySqlNodeDetailVo oldSqlVo = jobPhaseAndSqlDetailMap.get(jobId.toString() + targetPhaseVo.getName() + newSqlVo.getResourceId().toString() + newSqlVo.getSqlFile());
                //不存在则新增
                if (oldSqlVo == null) {
                    DeploySqlNodeDetailVo deploySqlDetailVo = sqlDetailMap.get(newSqlVo.getResourceId().toString() + newSqlVo.getSqlFile());
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
            for (DeploySqlNodeDetailVo insertSqlVo : insertSqlDetailList) {
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
            for (DeploySqlNodeDetailVo sqlDetailVo : updateSqlList) {
                deploySqlMapper.updateDeploySqlDetail(sqlDetailVo);
            }
        }
    }

    @Override
    public void updateSqlStatus(JSONObject paramObj) {
        DeploySqlNodeDetailVo paramDeploySqlVo = new DeploySqlNodeDetailVo(paramObj.getJSONObject("sqlStatus"));
        DeploySqlNodeDetailVo oldDeploySqlVo = deploySqlMapper.getDeploySqlDetail(new DeploySqlNodeDetailVo(paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramDeploySqlVo.getSqlFile(), paramObj.getLong("jobId"), paramObj.getString("phaseName"), paramDeploySqlVo.getResourceId()));
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
    public AutoexecSqlNodeDetailVo getSqlDetail(AutoexecJobVo jobVo) {
        Long sqlId = jobVo.getActionParam().getLong("nodeId");
        if (sqlId == null) {
            throw new ParamIrregularException("nodeId");
        }
        DeploySqlNodeDetailVo deploySqlDetailVo = deploySqlMapper.getDeployJobSqlDetailById(sqlId);
        AutoexecSqlNodeDetailVo autoexecSqlDetailVo = null;
        if (deploySqlDetailVo != null) {
            autoexecSqlDetailVo = new AutoexecSqlNodeDetailVo();
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
        Long runnerId = null;
        //如果是sqlfile,优先使用历史执行sql 的runner
        if (Objects.equals(ExecMode.SQL.getValue(), jobPhaseVo.getExecMode())) {
            DeploySqlNodeDetailVo detailVo = new DeploySqlNodeDetailVo(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId(), deployJobVo.getEnvId(), deployJobVo.getVersion());
            List<DeploySqlNodeDetailVo> sqlList = deploySqlMapper.getAllDeploySqlDetailList(detailVo);
            if (CollectionUtils.isNotEmpty(sqlList)) {
                runnerId = sqlList.get(0).getRunnerId();
            }
        } else if (Objects.equals(ExecMode.RUNNER.getValue(), jobPhaseVo.getExecMode()) && deployJobVo.getRunnerMapId() != null) {
            runnerId = deployJobVo.getRunnerMapId();
        }

        if (runnerId != null) {
            RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(runnerId);
            if (runnerMapVo == null) {
                throw new RunnerNotFoundByRunnerMapIdException(deployJobVo.getRunnerMapId());
            }
            return Collections.singletonList(runnerMapVo);
        }

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
            throw new RunnerGroupRunnerNotFoundException(appModuleRunnerGroup.getId().toString());
        }
        if (CollectionUtils.isEmpty(groupVo.getRunnerMapList())) {
            throw new RunnerGroupRunnerNotFoundException(groupVo.getName() + "(" + groupVo.getId() + ") ");
        }
        runnerMapVos = groupVo.getRunnerMapList();

        return runnerMapVos;
    }

    @Override
    public void updateJobRunnerMap(Long jobId, Long runnerMapId) {
        deployJobMapper.updateDeployJobRunnerMapId(jobId, runnerMapId);
    }

    @Override
    public AutoexecCombopVo getAutoexecCombop(AutoexecJobVo autoexecJobParam) {
        DeployJobVo deployJobVo = (DeployJobVo) autoexecJobParam;
        //获取最终流水线
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(deployJobVo.getAppSystemId())
                .withAppModuleId(deployJobVo.getAppModuleId())
                .withEnvId(deployJobVo.getEnvId())
                .withDeleteDisabledPhase(true) // 删除禁用阶段
                .getConfig();
        if (deployPipelineConfigVo == null) {
            throw new DeployPipelineConfigNotFoundException();
        }
        AutoexecCombopVo combopVo = new AutoexecCombopVo();
        combopVo.setConfig(deployPipelineConfigVo.getAutoexecCombopConfigVo());
        return combopVo;
    }

    @Override
    public AutoexecCombopVo getSnapshotAutoexecCombop(AutoexecJobVo autoexecJobParam) {
        AutoexecCombopVo combopVo = new AutoexecCombopVo();
        combopVo.setConfig(autoexecJobParam.getConfig());
        return combopVo;
    }

    @Override
    public void updateSqlStatus(List<Long> sqlIdList, String status) {
        deploySqlMapper.updateDeploySqlStatusByIdList(sqlIdList, status);
    }

    @Override
    public void updateInvokeJob(AutoexecJobVo jobVo) {
        DeployJobVo deployJobVo = (DeployJobVo) jobVo;
        deployJobMapper.insertIgnoreDeployJobContent(new DeployJobContentVo(jobVo.getConfigStr()));
        if (deployJobVo.getBuildNo() != null || StringUtils.isNotBlank(deployJobVo.getVersion())) {
            DeployVersionVo deployVersionVo = deployVersionMapper.getVersionByAppSystemIdAndAppModuleIdAndVersion(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId(), deployJobVo.getVersion());
            if (deployVersionVo == null) {
                throw new DeployVersionNotFoundException(deployJobVo.getVersion());
            }
            deployJobVo.setVersionId(deployVersionVo.getId());
            //如果存在version 但buildNo 为null，则需要根据流水线是否含有build工具决定是否新建buildNo，如果没有deploy工具和build工具则version设置为null
            if (deployJobVo.getBuildNo() == null) {
                PipelineJobTemplateVo jobTemplateVo = new PipelineJobTemplateVo(deployJobVo);
                DeployPipelineConfigManager.setIsJobTemplateVoHasBuildDeployType(jobTemplateVo, new HashMap<>());
                if (jobTemplateVo.getIsHasBuildTypeTool() == 1) {
                    deployJobVo.setBuildNo(-1);
                }
                if (jobTemplateVo.getIsHasBuildTypeTool() != 1 && jobTemplateVo.getIsHasDeployTypeTool() != 1) {
                    deployJobVo.setVersionId(null);
                    deployJobVo.setVersion(null);
                }
            }
            if (deployJobVo.getBuildNo() != null) {
                //如果buildNo是-1，表示新建buildNo
                if (deployJobVo.getBuildNo() == -1) {
                    Integer maxBuildNo = deployVersionMapper.getDeployVersionMaxBuildNoByVersionIdLock(deployVersionVo.getId());
                    if (maxBuildNo == null) {
                        deployJobVo.setBuildNo(1);
                    } else {
                        deployJobVo.setBuildNo(maxBuildNo + 1);
                    }
                    deployVersionMapper.insertDeployVersionBuildNo(new DeployVersionBuildNoVo(deployVersionVo.getId(), deployJobVo.getBuildNo(), deployJobVo.getId(), BuildNoStatus.PENDING.getValue()));
                } else if (deployJobVo.getBuildNo() > 0) {
                    deployJobVo.setBuildNo(deployJobVo.getBuildNo());
                }
                deployVersionMapper.insertDeployVersionBuildNo(new DeployVersionBuildNoVo(deployVersionVo.getId(), deployJobVo.getBuildNo(), deployJobVo.getId(), BuildNoStatus.PENDING.getValue()));
            }
        }
        deployJobVo.setName(deployJobVo.getAppSystemAbbrName() + "/" + deployJobVo.getAppModuleAbbrName() + "/" + deployJobVo.getEnvName() + (StringUtils.isBlank(deployJobVo.getVersion()) ? StringUtils.EMPTY : "/" + deployJobVo.getVersion()));
        deployJobMapper.insertDeployJob(deployJobVo);
        if (jobVo.getInvokeId() == null) {
            jobVo.setInvokeId(deployJobVo.getId());
        }
        jobVo.setOperationId(deployJobVo.getId());
    }

    @Override
    public List<AutoexecJobPhaseNodeVo> getJobNodeListBySqlIdList(List<Long> sqlIdList) {
        return deploySqlMapper.getDeployJobPhaseNodeListBySqlIdList(sqlIdList);
    }

    @Override
    public boolean getIsCanUpdatePhaseRunner(AutoexecJobPhaseVo jobPhaseVo, Long runnerMapId) {
        List<DeploySqlNodeDetailVo> deploySqlDetailVos = deploySqlMapper.getDeployJobSqlDetailByExceptStatusListAndRunnerMapId(jobPhaseVo.getJobId(), jobPhaseVo.getName(), Arrays.asList(JobNodeStatus.SUCCEED.getValue(), JobNodeStatus.IGNORED.getValue()), runnerMapId);
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
        ResourceVo appSystem = iResourceCrossoverMapper.getAppSystemById(deployJobVo.getAppSystemId());
        if (appSystem == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppSystemId());
        }
        ResourceVo appModule = iResourceCrossoverMapper.getAppModuleById(deployJobVo.getAppModuleId());
        if (appModule == null) {
            throw new CiEntityNotFoundException(deployJobVo.getAppModuleId());
        }
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId());
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(appSystem.getName() + "(" + deployJobVo.getAppSystemId() + ")", appModule.getName() + "(" + deployJobVo.getAppModuleId() + ")");
        }
        if (CollectionUtils.isEmpty(runnerGroupVo.getRunnerMapList())) {
            throw new RunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
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

    @Override
    public void myExecuteAuthCheck(AutoexecJobVo jobVo) {
        if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), BATCHDEPLOY_MODIFY.class.getSimpleName()) || Objects.equals(UserContext.get().getUserUuid(), SystemUser.SYSTEM.getUserUuid())) {
            return;
        }
        DeployJobVo deployJobVo;
        if (jobVo instanceof DeployJobVo) {
            deployJobVo = (DeployJobVo) jobVo;
        } else {
            DeployJobVo deployJobTmp = deployJobMapper.getDeployJobByJobId(jobVo.getId());
            if (deployJobTmp == null) {
                throw new AutoexecJobNotFoundException(jobVo.getId());
            }
            deployJobVo = deployJobTmp;
        }
        //包含BATCHJOB_MODIFY 则拥有所有应用的执行权限
        if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), BATCHDEPLOY_MODIFY.class.getSimpleName()) && !Objects.equals(UserContext.get().getUserUuid(), SystemUser.SYSTEM.getUserUuid())) {
            Set<String> authSet = DeployAppAuthChecker.builder(deployJobVo.getAppSystemId()).addEnvAction(deployJobVo.getEnvId()).addScenarioAction(deployJobVo.getScenarioId()).check();
            if (!authSet.containsAll(Arrays.asList(deployJobVo.getEnvId().toString(), deployJobVo.getScenarioId().toString()))) {
                throw new DeployJobCannotExecuteException(deployJobVo);
            }
        }
    }

    @Override
    public List<String> getModifyAuthList() {
        return Collections.singletonList(DEPLOY_MODIFY.class.getSimpleName());
    }

    @Override
    public void getJobActionAuth(AutoexecJobVo jobVo) {
        boolean isHasAuth = false;
        //包含BATCHJOB_MODIFY 则拥有所有应用的执行权限
        if (AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), BATCHDEPLOY_MODIFY.class.getSimpleName())) {
            isHasAuth = true;
        } else {
            if (!Objects.equals(jobVo.getSource(), JobSource.BATCHDEPLOY.getValue())) {
                DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobVo.getId());
                Set<String> authSet = DeployAppAuthChecker.builder(deployJobVo.getAppSystemId()).addEnvAction(deployJobVo.getEnvId()).addScenarioAction(deployJobVo.getScenarioId()).check();
                if (authSet.containsAll(Arrays.asList(deployJobVo.getEnvId().toString(), deployJobVo.getScenarioId().toString()))) {
                    isHasAuth = true;
                }
            }
        }
        if (isHasAuth) {
            if (UserContext.get().getUserUuid().equals(jobVo.getExecUser())) {
                jobVo.setIsCanExecute(1);
            } else {
                jobVo.setIsCanTakeOver(1);
            }
        }
    }

    @Override
    public void getCreatePayload(AutoexecJobVo jobVo, JSONObject result) {
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobVo.getId());
        if (deployJobVo != null) {
            result.put("appSystemId", deployJobVo.getAppSystemId());
            result.put("envId", deployJobVo.getEnvId());
            result.put("scenarioId", deployJobVo.getScenarioId());
            JSONArray moduleList = new JSONArray();
            result.put("moduleList", moduleList);
            JSONObject module = new JSONObject();
            moduleList.add(module);
            module.put("id", deployJobVo.getAppModuleId());
            module.put("version", deployJobVo.getVersion());
            module.put("buildNo", deployJobVo.getBuildNo());
            AutoexecJobContentVo configContentVo = autoexecJobMapper.getJobContent(jobVo.getConfigHash());
            JSONObject jobConfig = JSONObject.parseObject(configContentVo.getContent());
            JSONObject executeConfig = jobConfig.getJSONObject("executeConfig");
            if (MapUtils.isNotEmpty(executeConfig)) {
                JSONObject executeNodeConfig = executeConfig.getJSONObject("executeNodeConfig");
                if (MapUtils.isNotEmpty(executeNodeConfig)) {
                    module.put("selectNodeList", executeNodeConfig.getJSONArray("selectNodeList"));
                }
            }
            if (!module.containsKey("selectNodeList")) {
                module.put("selectNodeList", new JSONArray());
            }
        }
    }

    @Override
    public JSONObject getExtraJobInfo(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobVo.getId());
        if (deployJobVo != null) {
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            result.put("appSystemId", deployJobVo.getAppSystemId());
            ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(deployJobVo.getAppSystemId());
            if (appSystem != null) {
                result.put("appSystemAbbrName", appSystem.getAbbrName());
                result.put("appSystemName", appSystem.getName());
            }
            result.put("appModuleId", deployJobVo.getAppModuleId());
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(deployJobVo.getAppModuleId());
            if (appModule != null) {
                result.put("appModuleAbbrName", appModule.getAbbrName());
                result.put("appModuleName", appModule.getName());
            }
            result.put("envId", deployJobVo.getEnvId());
            ResourceVo env = resourceCrossoverMapper.getAppEnvById(deployJobVo.getEnvId());
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
            boolean isHasLock = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY.getValue()).getIsHasLockByKey(jobVo.getId().toString());
            result.put("isHasLock", isHasLock ? 1 : 0);

        }
        return result;
    }

    @Override
    public void deleteJob(AutoexecJobVo jobVo) {
        deploySqlMapper.deleteDeploySqlDetailByJobId(jobVo.getId());
        deployJobMapper.deleteJobById(jobVo.getId());
    }

    @Override
    public JSONObject getExtraRefreshJobInfo(AutoexecJobVo jobVo) {
        JSONObject result = new JSONObject();
        //补充是否有资源锁
        boolean isHasLock = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY.getValue()).getIsHasLockByKey(jobVo.getId().toString());
        result.put("isHasLock", isHasLock ? 1 : 0);
        return result;
    }

}
