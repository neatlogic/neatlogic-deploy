package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import neatlogic.framework.deploy.dto.sql.DeploySqlNodeDetailVo;
import neatlogic.framework.deploy.dto.sql.DeploySqlJobPhaseVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/28 12:19 下午
 */
public interface DeploySqlMapper extends IDeploySqlCrossoverMapper {

    DeploySqlNodeDetailVo getDeploySqlDetail(DeploySqlNodeDetailVo deploySqlDetailVo);

    int searchDeploySqlCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<DeploySqlNodeDetailVo> searchDeploySql(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<DeploySqlNodeDetailVo> getDeploySqlDetailList(@Param("sqlFileDetailVoList") List<DeploySqlNodeDetailVo> sqlFileDetailVoList);

    List<DeploySqlNodeDetailVo> getAllDeploySqlDetailList(DeploySqlNodeDetailVo deployVersionSql);

    List<DeploySqlNodeDetailVo> getDeployJobSqlDetailByExceptStatusListAndRunnerMapId(@Param("jobId") Long jobId, @Param("jobPhaseName") String name, @Param("statusList") List<String> statusList, @Param("runnerMapId") Long runnerMapId);

    DeploySqlNodeDetailVo getDeployJobSqlDetailById(Long id);

    List<Long> getDeployJobSqlIdListByJobIdAndJobPhaseNameList(@Param("jobId") Long jobId, @Param("jobPhaseNameList") List<String> jobPhaseNameList);

    List<Long> getDeployJobSqlIdListByJobIdAndJobPhaseName(@Param("jobId") Long jobId, @Param("phaseName") String phaseName);

    List<AutoexecJobPhaseNodeVo> getDeployJobPhaseNodeListBySqlIdList(@Param("sqlIdList") List<Long> sqlIdList);

    void updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(@Param("status") String status, @Param("md5") String md5, @Param("id") Long id);

    void updateDeploySqlIsDeleteByIdList(@Param("idList") List<Long> idList);

    void updateDeploySqlSortList(@Param("needDeleteSqlIdList") List<Long> needDeleteSqlIdList, @Param("jobId") Long jobId, @Param("phaseId") Long phaseId);

    void updateDeploySqlDetail(DeploySqlNodeDetailVo deploySqlDetailVo);

    void insertDeploySqlDetail(@Param("sqlVo") DeploySqlNodeDetailVo paramDeploySqlVo, @Param("sysId") Long sysId, @Param("envId") Long envId, @Param("moduleId") Long moduleId, @Param("version") String version, @Param("runnerId") Long runnerId);

    void insertDeploySql(DeploySqlJobPhaseVo deploySqlVo);

    void resetDeploySqlStatusBySqlIdList(@Param("idList") List<Long> idList);

    void updateDeploySqlStatusByIdList(@Param("idList") List<Long> sqlIdList, @Param("status") String status);

    void deleteDeploySqlDetailByJobId(Long id);
}
