package codedriver.module.deploy.dao.mapper;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigResourceVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author lvzk
 * @date 2022/5/23 12:19 下午
 */
public interface DeployAppConfigMapper {

    List<Long> getAppSystemIdList(@Param("searchVo") ResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppSystemListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName, @Param("userUuid") String userUuid);

    Integer getAppConfigAuthorityCount(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityList(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityDetailList(@Param("appConfigAuthList") List<DeployAppConfigAuthorityVo> appConfigAuthList);

    DeployAppConfigVo getAppConfigByAppSystemId(Long appSystemId);

    Integer insertAppConfigAuthority(DeployAppConfigAuthorityVo deployAppConfigAuthorityVo);

    Integer insertAppModuleRunnerGroup(@Param("appSystemId") Long appSystemId, @Param("moduleId") Long moduleId, @Param("runnerGroupId") Long runnerGroupId);

    Integer insertAppEnvAutoConfig(DeployAppEnvAutoConfigVo appEnvAutoConfigVo);

    Integer insertAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer updateAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer deleteAppConfigAuthorityByAppIdAndEnvIdAndAuthUuidAndLcd(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId, @Param("authUuid") String uuid, @Param("lcd") Date nowTime);

    Integer deleteAppEnvAutoConfig(DeployAppEnvAutoConfigVo deployAppEnvAutoConfigVo);
}