package codedriver.module.deploy.dao.mapper;

import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeploySqlVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/28 12:19 下午
 */
public interface DeploySqlMapper extends IDeploySqlCrossoverMapper {

    DeploySqlDetailVo getAutoexecJobIdByDeploySqlDetailVo(DeploySqlDetailVo paramDeploySqlVo);

    List<DeploySqlDetailVo> getDeploySqlDetailList(@Param("sqlFileDetailVoList") List<DeploySqlDetailVo> sqlFileDetailVoList);

    List<DeploySqlDetailVo> getAllDeploySqlDetailList(DeploySqlDetailVo deployVersionSql);

    void updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(@Param("status") String status, @Param("md5") String md5, @Param("id") Long id);

    void updateDeploySqlIsDeleteByIdList(@Param("idList") List<Long> idList);

    void insertDeploySqlDetail(DeploySqlDetailVo paramDeploySqlVo);

    void insertDeploySql(DeploySqlVo jobId);

    int searchDeploySqlCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo);

    List<DeploySqlDetailVo> searchDeploySql(AutoexecJobPhaseNodeVo jobPhaseNodeVo);


}
