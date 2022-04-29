package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.sql.DeployJobSqlVo;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author longrf
 * @date 2022/4/28 12:19 下午
 */
public interface DeploySqlMapper extends IDeploySqlCrossoverMapper {

    DeploySqlDetailVo getAutoexecJobIdByDeploySqlDetailVo(DeploySqlDetailVo paramDeploySqlVo);

    List<DeploySqlDetailVo> getJobDeploySqlDetailList(@Param("sqlFileDetailVoList") List<DeploySqlDetailVo> sqlFileDetailVoList);

    List<DeploySqlDetailVo> getAllJobDeploySqlDetailList(DeploySqlDetailVo deployVersionSql);

    void updateJobDeploySqlDetailIsDeleteAndStatusAndMd5AndLcdById(@Param("status") String status, @Param("md5") String md5, @Param("id") Long id);

    void updateJobDeploySqlIsDeleteByIdList(@Param("idList") List<Long> idList);

    void insertDeploySqlDetail(DeploySqlDetailVo paramDeploySqlVo);

    void insertDeploySql(DeployJobSqlVo jobId);
}
