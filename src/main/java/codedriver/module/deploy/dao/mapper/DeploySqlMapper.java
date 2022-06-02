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

    DeploySqlDetailVo getDeploySqlBySysIdAndModuleIdAndEnvIdAndVersionAndSqlFile(@Param("sysId") Long sysId, @Param("envId") Long envId, @Param("moduleId") Long moduleId, @Param("version") String version, @Param("sqlFile") String sqlFile);

    int searchDeploySqlCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<DeploySqlDetailVo> searchDeploySql(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<DeploySqlDetailVo> getDeploySqlDetailList(@Param("sqlFileDetailVoList") List<DeploySqlDetailVo> sqlFileDetailVoList);

    List<DeploySqlDetailVo> getAllDeploySqlDetailList(DeploySqlDetailVo deployVersionSql);

    DeploySqlDetailVo getJobSqlDetailById(Long id);

    List<Long> getJobSqlIdListByJobIdAndJobPhaseNameList(@Param("jobId") Long jobId, @Param("jobPhaseNameList") List<String> jobPhaseNameList);

    void updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(@Param("status") String status, @Param("md5") String md5, @Param("id") Long id);

    void updateDeploySqlIsDeleteByIdList(@Param("idList") List<Long> idList);

    void insertDeploySqlDetail(@Param("sqlVo") DeploySqlDetailVo paramDeploySqlVo, @Param("sysId") Long sysId, @Param("envId") Long envId, @Param("moduleId") Long moduleId, @Param("version") String version, @Param("runnerId") Long runnerId);

    void insertDeploySql(DeploySqlJobPhaseVo deploySqlVo);

    void resetDeploySqlStatusBySqlIdList(@Param("idList") List<Long> idList);

    List<Long> getJobSqlIdListByJobIdAndJobPhaseName(@Param("jobId") Long jobId, @Param("phaseName") String phaseName);

    void reEnabledDeploySqlDetailById(@Param("idList") List<Long> idList);

}
