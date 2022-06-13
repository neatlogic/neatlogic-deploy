package codedriver.module.deploy.api.job.source.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.AutoexecJobRunnerHttpRequestException;
import codedriver.framework.autoexec.job.source.action.AutoexecJobSourceActionHandlerBase;
import codedriver.framework.autoexec.util.AutoexecUtil;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeploySqlMapper;
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

    @Override
    public String getName() {
        return DeployOperType.DEPLOY.getValue();
    }

    @Override
    public void saveJobPhase(AutoexecCombopPhaseVo combopPhaseVo) {

    }

    @Override
    public String getJobSqlContent(AutoexecJobVo jobVo) {
        JSONObject paramObj = jobVo.getActionParam();
        DeploySqlDetailVo sqlDetailVo = deploySqlMapper.getJobSqlDetailById(paramObj.getLong("sqlId"));
        paramObj.put("sysId", sqlDetailVo.getSysId());
        paramObj.put("moduleId", sqlDetailVo.getModuleId());
        paramObj.put("envId", sqlDetailVo.getEnvId());
        paramObj.put("version", sqlDetailVo.getVersion());
        ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        CiEntityVo envCiEntity = ciEntityCrossoverMapper.getCiEntityBaseInfoById(sqlDetailVo.getEnvId());
        paramObj.put("envName", envCiEntity.getName());
        AutoexecJobPhaseNodeVo nodeVo = jobVo.getCurrentNode();
        return AutoexecUtil.requestRunner(nodeVo.getRunnerUrl() + "/api/rest/deploy/sql/content/get", paramObj);
    }

    @Override
    public void downloadJobSqlFile(AutoexecJobVo jobVo) throws Exception {
        JSONObject paramObj = jobVo.getActionParam();
        DeploySqlDetailVo sqlDetailVo = deploySqlMapper.getJobSqlDetailById(paramObj.getLong("sqlId"));
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
            List<Long> resetSqlIdList = deploySqlMapper.getJobSqlIdListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
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
        return TableResultUtil.getResult(returnList, jobPhaseNodeVo);    }

    @Override
    public void checkinSqlList(JSONObject paramObj) {
        JSONArray paramSqlVoArray = paramObj.getJSONArray("sqlInfoList");

        List<DeploySqlDetailVo> oldDeploySqlList = deploySqlMapper.getAllDeploySqlDetailList(new DeploySqlDetailVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), paramObj.getString("version"), paramObj.getString("targetPhaseName")));
        Map<String, DeploySqlDetailVo> oldDeploySqlMap = new HashMap<>();
        List<Long> needDeleteSqlIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(oldDeploySqlList)) {
            oldDeploySqlMap = oldDeploySqlList.stream().collect(Collectors.toMap(DeploySqlDetailVo::getSqlFile, e -> e));
            needDeleteSqlIdList = oldDeploySqlList.stream().map(DeploySqlDetailVo::getId).collect(Collectors.toList());
        }
        List<DeploySqlDetailVo> insertSqlList = new ArrayList<>();
        List<Long> reEnabledSqlList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {
            for (DeploySqlDetailVo newSqlVo : paramSqlVoArray.toJavaList(DeploySqlDetailVo.class)) {
                DeploySqlDetailVo oldSqlVo = oldDeploySqlMap.get(newSqlVo.getSqlFile());
                //不存在则新增
                if (oldSqlVo == null) {
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
    }

    @Override
    public void updateSqlStatus(JSONObject paramObj) {
        DeploySqlDetailVo paramDeploySqlVo = new DeploySqlDetailVo(paramObj.getJSONObject("sqlStatus"));
        DeploySqlDetailVo oldDeploySqlVo = deploySqlMapper.getDeploySqlBySysIdAndModuleIdAndEnvIdAndVersionAndSqlFile(paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramDeploySqlVo.getSqlFile());
        if (oldDeploySqlVo != null) {
            deploySqlMapper.updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(paramDeploySqlVo.getStatus(), paramDeploySqlVo.getMd5(), oldDeploySqlVo.getId());
        } else {
            deploySqlMapper.insertDeploySql(new DeploySqlJobPhaseVo(paramObj.getLong("jobId"), paramObj.getString("phaseName"), paramDeploySqlVo.getId()));
            deploySqlMapper.insertDeploySqlDetail(paramDeploySqlVo, paramObj.getLong("sysId"), paramObj.getLong("envId"), paramObj.getLong("moduleId"), paramObj.getString("version"), paramObj.getLong("runnerId"));
        }
    }
}
