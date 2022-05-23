package codedriver.module.deploy.dao.mapper;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.app.DeployAppConfigResourceVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lvzk
 * @date 2022/5/23 12:19 下午
 */
public interface DeployAppConfigMapper extends IDeploySqlCrossoverMapper {

    List<Long> getAppIdList(@Param("searchVo") ResourceSearchVo searchVo,@Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName,@Param("userUuid") String userUuid);

    Integer getAppIdListCount(ResourceSearchVo searchVo);

}
