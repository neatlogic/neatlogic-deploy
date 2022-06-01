package codedriver.module.deploy.api.job.source.action;

import codedriver.framework.asynchronization.threadlocal.UserContext;
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
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.module.deploy.dao.mapper.DeploySqlMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author longrf
 * @date 2022/5/31 5:22 下午
 */
@Service
public class DeployJobSourceHandler extends AutoexecJobSourceActionHandlerBase {

    @Resource
    DeploySqlMapper deploySqlMapper;

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
        IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
        if (!Objects.isNull(paramObj.getInteger("isAll")) && paramObj.getInteger("isAll") == 1) {
            //重置phase的所有sql文件状态
            List<Long> resetSqlIdList = iDeploySqlCrossoverMapper.getJobSqlIdListByJobIdAndJobPhaseName(paramObj.getLong("jobId"), paramObj.getString("phaseName"));
            if (CollectionUtils.isNotEmpty(resetSqlIdList)) {
                iDeploySqlCrossoverMapper.resetDeploySqlStatusBySqlIdList(resetSqlIdList);
            }
        } else if (CollectionUtils.isNotEmpty(sqlIdArray)) {
            //批量重置sql文件状态
            iDeploySqlCrossoverMapper.resetDeploySqlStatusBySqlIdList(sqlIdArray.toJavaList(Long.class));
        }
    }
}
