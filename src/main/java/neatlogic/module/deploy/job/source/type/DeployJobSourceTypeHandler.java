/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.job.source.type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobNodeStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
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
import neatlogic.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.auth.BATCHDEPLOY_MODIFY;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.framework.deploy.constvalue.*;
import neatlogic.framework.deploy.dto.app.DeployAppConfigAuthorityActionVo;
import neatlogic.framework.deploy.dto.app.DeployPipelineConfigVo;
import neatlogic.framework.deploy.dto.app.DeployProfileVo;
import neatlogic.framework.deploy.dto.instance.DeployInstanceVersionVo;
import neatlogic.framework.deploy.dto.job.DeployJobContentVo;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import neatlogic.framework.deploy.dto.sql.DeploySqlNodeDetailVo;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildNoVo;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.globallock.core.GlobalLockHandlerFactory;
import neatlogic.framework.globallock.core.IGlobalLockHandler;
import neatlogic.framework.globallock.dao.mapper.GlobalLockMapper;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.*;
import neatlogic.module.deploy.util.DeployPipelineConfigManager;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @since 2022/5/31 5:22 下午
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

    @Resource
    GlobalLockMapper globalLockMapper;

    @Resource
    DeployInstanceVersionMapper deployInstanceVersionMapper;

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
    public void resetSqlStatus(JSONObject paramObj, AutoexecJobVo jobVo) {
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        AutoexecJobPhaseVo currentPhase = jobVo.getCurrentPhase();
        if (paramObj.getInteger("isAll") != null && paramObj.getInteger("isAll") == 1) {
            deploySqlMapper.updateDeploySqlStatusByJobIdAndPhaseId(currentPhase.getJobId(), currentPhase.getId(), JobNodeStatus.PENDING.getValue());
        } else {
            List<Long> sqlIdList = sqlIdArray.toJavaList(Long.class);
            //批量重置sql文件状态
            if (CollectionUtils.isNotEmpty(sqlIdList)) {
                jobVo.setJobPhaseNodeSqlList(deploySqlMapper.getDeployJobPhaseNodeListBySqlIdList(sqlIdList));
                deploySqlMapper.resetDeploySqlStatusBySqlIdList(sqlIdList);
            }
        }
    }

    @Override
    public void ignoreSql(JSONObject paramObj, AutoexecJobVo jobVo) {
        JSONArray sqlIdArray = paramObj.getJSONArray("sqlIdList");
        AutoexecJobPhaseVo currentPhase = jobVo.getCurrentPhase();
        if (paramObj.getInteger("isAll") != null && paramObj.getInteger("isAll") == 1) {
            deploySqlMapper.updateDeploySqlStatusByJobIdAndPhaseId(currentPhase.getJobId(), currentPhase.getId(), JobNodeStatus.IGNORED.getValue());
        } else {
            List<Long> sqlIdList = sqlIdArray.toJavaList(Long.class);
            //批量重置sql文件状态
            if (CollectionUtils.isNotEmpty(sqlIdList)) {
                jobVo.setJobPhaseNodeSqlList(deploySqlMapper.getDeployJobPhaseNodeListBySqlIdList(sqlIdList));
                deploySqlMapper.updateDeploySqlStatusByIdList(sqlIdList, JobNodeStatus.IGNORED.getValue());
            }
        }
    }

    @Override
    public void overrideProfile(AutoexecJobVo autoexecJobVo, Map<String, AutoexecParamVo> autoexecProfileParamVoMap, Long profileId) {
        if (MapUtils.isEmpty(autoexecProfileParamVoMap)) {
            return;
        }
        DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(autoexecJobVo.getId());
        if (deployJobVo == null) {
            throw new DeployJobNotFoundException(autoexecJobVo.getId());
        }
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(deployJobVo.getAppSystemId())
                .withAppModuleId(deployJobVo.getAppModuleId())
                .withEnvId(deployJobVo.getEnvId())
                .withDeleteDisabledPhase(true) // 删除禁用阶段
                .getConfig();
        if (deployPipelineConfigVo == null) {
            throw new DeployPipelineConfigNotFoundException();
        }
        List<DeployProfileVo> overrideProfileList = deployPipelineConfigVo.getOverrideProfileList();
        if (CollectionUtils.isNotEmpty(overrideProfileList)) {
            Optional<DeployProfileVo> optionalDeployProfileVo = overrideProfileList.stream().filter(p -> Objects.equals(p.getProfileId(), profileId)).findFirst();
            if (optionalDeployProfileVo.isPresent() && CollectionUtils.isNotEmpty(optionalDeployProfileVo.get().getParamList())) {
                Map<String, AutoexecParamVo> overrideProfileParamMap = optionalDeployProfileVo.get().getParamList().stream().collect(Collectors.toMap(e -> Objects.equals(e.getType(), "argument") ? ("argument" + e.getKey()) : e.getKey(), e -> e));
                for (Map.Entry<String, AutoexecParamVo> entry : autoexecProfileParamVoMap.entrySet()) {
                    String key = entry.getKey();
                    if (overrideProfileParamMap.containsKey(key)) {
                        autoexecProfileParamVoMap.put(key, overrideProfileParamMap.get(key));
                    }
                }
            }
        }
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

    /**
     * 工具库工具deploy/dpsqlimport会调用此方法
     * <p>
     * 1、根据系统id、模块id、环境id、版本获取旧sqlList
     * 2、新旧对比，不存在则新增（注：节点信息新增到deploy_sql_job_phase表），存在则跟新deploy_sql_detail信息，需要删除的逻辑删（is_delete为1、sort为999999）
     * 新增时：
     * status默认pending
     * isModified默认0
     * wornCount默认为0
     * sort为插入的顺序
     * 更新时：
     * status为pending时，start_time为null、end_time为null
     * status为running时，start_time为now(3)、end_time为null
     * status为aborted、succeed、failed、ignored时，end_time为now(3)
     *
     * @param paramObj 当前系统的当前模块的当前环境的当前版本的所有sql信息
     */
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

    /**
     * 工具库工具deploy/dpsqlexec会调用此方法
     * 若sql不存在，则新增，存在则更新
     * 新增时：
     * status默认pending
     * isModified默认0
     * wornCount默认为0
     * sort为插入的顺序
     * 更新时：
     * status为pending时，start_time为null、end_time为null
     * status为running时，start_time为now(3)、end_time为null
     * status为aborted、succeed、failed、ignored时，end_time为now(3)
     *
     * @param paramObj 单条sql的信息
     */
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
        //如果是sqlfile ｜ local ，则保证一个作业使用同一个runner
        if (Arrays.asList(ExecMode.SQL.getValue(), ExecMode.RUNNER.getValue()).contains(jobPhaseVo.getExecMode())) {
            Long runnerMapId = deployJobVo.getRunnerMapId();
            if (runnerMapId == null) {
                //sql 优先使用历史runner TODO 如果切换runner组会有问题
                List<DeploySqlNodeDetailVo> sqlNodeDetailVos = deploySqlMapper.getAllDeploySqlDetailList(new DeploySqlNodeDetailVo(deployJobVo.getAppSystemId(), deployJobVo.getAppModuleId(), deployJobVo.getEnvId(), deployJobVo.getVersion()));
                if (CollectionUtils.isNotEmpty(sqlNodeDetailVos)) {
                    runnerMapId = sqlNodeDetailVos.get(0).getRunnerId();
                }
            }
            if (runnerMapId != null) {
                RunnerMapVo runnerMapVo = runnerMapper.getRunnerMapByRunnerMapId(runnerMapId);
                if (runnerMapVo == null) {
                    throw new RunnerNotFoundByRunnerMapIdException(deployJobVo.getRunnerMapId());
                }
                //如果runner没有被删除则沿用历史runner
                if (runnerMapVo.getIsDelete() == 0) {
                    return Collections.singletonList(runnerMapVo);
                }
            }
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
                } else {
                    if (jobTemplateVo.getIsHasDeployTypeTool() != 1) {
                        deployJobVo.setVersionId(null);
                        deployJobVo.setVersion(null);
                    } else {
                        //根据版本和环境获取buildNo,比如：制品部署
                        DeployVersionEnvVo versionEnvVo = deployVersionMapper.getDeployVersionEnvByVersionIdAndEnvId(deployVersionVo.getId(), deployJobVo.getEnvId());
                        if (versionEnvVo != null) {
                            deployJobVo.setBuildNo(versionEnvVo.getBuildNo());
                        }
                    }
                }
            }
            //如果buildNo是-1，表示新建buildNo
            if (deployJobVo.getBuildNo() != null && deployJobVo.getBuildNo() == -1) {
                Integer maxBuildNo = deployVersionMapper.getDeployVersionMaxBuildNoByVersionIdLock(deployVersionVo.getId());
                if (maxBuildNo == null) {
                    deployJobVo.setBuildNo(1);
                } else {
                    deployJobVo.setBuildNo(maxBuildNo + 1);
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
        return deploySqlDetailVos.isEmpty();
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
        ResourceVo env = iResourceCrossoverMapper.getAppEnvById(deployJobVo.getEnvId());
        if (env == null) {
            throw new AppEnvNotFoundException(deployJobVo.getEnvId());
        }
        environment.put("DEPLOY_PATH", appSystem.getAbbrName() + "/" + appModule.getAbbrName() + "/" + env.getName());
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
            JSONObject jobConfig = JSON.parseObject(configContentVo.getContent());
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
            result.put("jobId", jobVo.getId());
            //补充是否有资源锁
            boolean isHasLock = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY.getValue()).getIsHasLockByKey(result);
            result.put("isHasLock", isHasLock ? 1 : 0);
            // 判断当前用户是否拥有“版本&制品管理”权限
            boolean hasOperationVersionAndProductManagerAuth = false;
            if (AuthActionChecker.check(DEPLOY_MODIFY.class)) {
                hasOperationVersionAndProductManagerAuth = true;
            } else {
                AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
                List<String> authUuidList = new ArrayList<>();
                authUuidList.add(authenticationInfoVo.getUserUuid());
                authUuidList.addAll(authenticationInfoVo.getTeamUuidList());
                authUuidList.addAll(authenticationInfoVo.getRoleUuidList());
                List<DeployAppConfigAuthorityActionVo> actionList = deployAppConfigMapper.getDeployAppAllAuthorityActionListByAppSystemIdAndAuthUuidList(deployJobVo.getAppSystemId(), authUuidList);
                for (DeployAppConfigAuthorityActionVo actionVo : actionList) {
                    if (Objects.equals(actionVo.getType(), DeployAppConfigActionType.OPERATION.getValue())) {
                        if (Objects.equals(actionVo.getAction(), "all")
                                || Objects.equals(actionVo.getAction(), DeployAppConfigAction.VERSION_AND_PRODUCT_MANAGER.getValue())) {
                            hasOperationVersionAndProductManagerAuth = true;
                            break;
                        }
                    }
                }
            }
            result.put("hasOperationVersionAndProductManagerAuth", hasOperationVersionAndProductManagerAuth);
        }
        return result;
    }

    @Override
    public void deleteJob(AutoexecJobVo jobVo) {
        deploySqlMapper.deleteDeploySqlDetailByJobId(jobVo.getId());
        deployJobMapper.deleteJobById(jobVo.getId());
        GlobalLockVo globalLockVo = new GlobalLockVo();
        JSONObject keywordParam = getExtraJobInfo(jobVo);
        globalLockVo.setKeywordParam(keywordParam);
        IGlobalLockHandler globalLockHandler = GlobalLockHandlerFactory.getHandler(JobSourceType.DEPLOY.getValue());
        globalLockHandler.initSearchParam(globalLockVo);
        globalLockMapper.deleteLockByIdList(globalLockVo.getIdList());
    }

    @Override
    public JSONObject getExtraRefreshJobInfo(AutoexecJobVo jobVo) {
        return getExtraJobInfo(jobVo);
    }

    @Override
    public void addExtraJobPhaseNodeInfoByList(Long jobId, List<AutoexecJobPhaseNodeVo> jobPhaseNodeVoList) {
        if (jobId != null && CollectionUtils.isNotEmpty(jobPhaseNodeVoList)) {
            DeployJobVo deployJobVo = deployJobMapper.getDeployJobByJobId(jobId);
            if (deployJobVo != null) {
                Long appSystemId = deployJobVo.getAppSystemId();
                Long appModuleId = deployJobVo.getAppModuleId();
                Long envId = deployJobVo.getEnvId();
                List<Long> instanceIdList = jobPhaseNodeVoList.stream().map(AutoexecJobPhaseNodeVo::getResourceId).filter(Objects::nonNull).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(instanceIdList)) {
                    List<DeployInstanceVersionVo> instanceVersionVoList = deployInstanceVersionMapper.getDeployInstanceVersionByEnvIdAndInstanceIdList(appSystemId, appModuleId, envId, instanceIdList);
                    Map<Long, DeployInstanceVersionVo> versionMap = instanceVersionVoList.stream().collect(Collectors.toMap(DeployInstanceVersionVo::getResourceId, e -> e));
                    JSONObject extraInfo = null;
                    for (AutoexecJobPhaseNodeVo jobPhaseNodeVo : jobPhaseNodeVoList) {
                        extraInfo = jobPhaseNodeVo.getExtraInfo();
                        if (extraInfo == null) {
                            extraInfo = new JSONObject();
                        }
                        extraInfo.put("version", versionMap.containsKey(jobPhaseNodeVo.getResourceId()) ? versionMap.get(jobPhaseNodeVo.getResourceId()).getVersion() : "");
                        extraInfo.put("instanceVersion", versionMap.get(jobPhaseNodeVo.getResourceId()));
                        jobPhaseNodeVo.setExtraInfo(extraInfo);
                    }
                }
            }
        }
    }
}
