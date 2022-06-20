package codedriver.module.deploy.dao.mapper;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import codedriver.framework.deploy.dto.app.*;
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

    String getAppConfig(DeployAppConfigVo deployAppConfigVo);

    DeployAppConfigVo getAppConfigVo(DeployAppConfigVo deployAppConfigVo);

    String getAppConfigOverrideConfig(DeployAppConfigOverrideVo deployAppConfigOverrideVo);

    List<DeployAppConfigOverrideVo> getAppConfigOverrideListByAppSystemId(Long appSystemId);

    DeployAppConfigVo getAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    List<AppEnvironmentVo> getDeployAppEnvListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList, @Param("schemaName") String schemaName);

    Integer insertAppConfigAuthority(DeployAppConfigAuthorityVo deployAppConfigAuthorityVo);

    Integer insertAppModuleRunnerGroup(@Param("appSystemId") Long appSystemId, @Param("moduleId") Long moduleId, @Param("runnerGroupId") Long runnerGroupId);

    Integer insertAppEnvAutoConfig(DeployAppEnvAutoConfigVo appEnvAutoConfigVo);

    Integer insertAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer insertAppConfigOverride(DeployAppConfigOverrideVo deployAppOverrideOverrideVo);

    Integer insertAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    void insertAppConfigEnv(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    Integer updateAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer updateAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    Integer deleteAppConfigAuthorityByAppIdAndEnvIdAndAuthUuidAndLcd(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId, @Param("authUuid") String uuid, @Param("lcd") Date nowTime);

    Integer deleteAppEnvAutoConfig(DeployAppEnvAutoConfigVo deployAppEnvAutoConfigVo);

    Integer deleteAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    void deleteAppConfigEnvByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);
}
