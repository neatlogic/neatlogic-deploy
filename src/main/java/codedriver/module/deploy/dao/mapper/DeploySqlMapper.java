package codedriver.module.deploy.dao.mapper;

import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/28 12:19 下午
 */
public interface DeploySqlMapper extends IDeploySqlCrossoverMapper {

    DeploySqlDetailVo getDeploySqlDetail(DeploySqlDetailVo deploySqlDetailVo);

    int searchDeploySqlCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<DeploySqlDetailVo> searchDeploySql(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<DeploySqlDetailVo> getDeploySqlDetailList(@Param("sqlFileDetailVoList") List<DeploySqlDetailVo> sqlFileDetailVoList);

    List<DeploySqlDetailVo> getAllDeploySqlDetailList(DeploySqlDetailVo deployVersionSql);

    List<DeploySqlDetailVo> getDeployJobSqlDetailByExceptStatusListAndRunnerMapId(@Param("jobId")Long jobId, @Param("jobPhaseName") String name, @Param("statusList") List<String> statusList, @Param("runnerMapId") Long runnerMapId);

    DeploySqlDetailVo getDeployJobSqlDetailById(Long id);

    List<Long> getDeployJobSqlIdListByJobIdAndJobPhaseNameList(@Param("jobId") Long jobId, @Param("jobPhaseNameList") List<String> jobPhaseNameList);

    List<Long> getDeployJobSqlIdListByJobIdAndJobPhaseName(@Param("jobId") Long jobId, @Param("phaseName") String phaseName);

    List<AutoexecJobPhaseNodeVo> getDeployJobPhaseNodeListBySqlIdList(@Param("sqlIdList") List<Long> sqlIdList);

    void updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(@Param("status") String status, @Param("md5") String md5, @Param("id") Long id);

    void deleteDeploySqlIsDeleteByIdList(@Param("idList") List<Long> idList);

    void updateDeploySqlSortList(@Param("needDeleteSqlIdList") List<Long> needDeleteSqlIdList, @Param("jobId") Long jobId, @Param("phaseId") Long phaseId);

    void updateDeploySqlDetail(DeploySqlDetailVo deploySqlDetailVo);

    void insertDeploySqlDetail(@Param("sqlVo") DeploySqlDetailVo paramDeploySqlVo, @Param("sysId") Long sysId, @Param("envId") Long envId, @Param("moduleId") Long moduleId, @Param("version") String version, @Param("runnerId") Long runnerId);

    void insertDeploySql(DeploySqlJobPhaseVo deploySqlVo);

    void insertDeploySqlList(List<DeploySqlJobPhaseVo> sqlJobPhaseVoList);

    void resetDeploySqlStatusBySqlIdList(@Param("idList") List<Long> idList);

}
