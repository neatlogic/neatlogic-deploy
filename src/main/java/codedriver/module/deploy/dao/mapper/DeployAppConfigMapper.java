package codedriver.module.deploy.dao.mapper;

import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.dto.runner.RunnerGroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author lvzk
 * @date 2022/5/23 12:19 下午
 */
public interface DeployAppConfigMapper {

    List<Long> getAppSystemIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppSystemListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName, @Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppSystemModuleListBySystemIdList(@Param("idList") List<Long> idList, @Param("isConfig") Integer isConfig, @Param("schemaName") String schemaName, @Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppSystemListByUserUuid(@Param("userUuid") String userUuid, @Param("searchVo") DeployResourceSearchVo searchVo);

    Integer getAppConfigAuthorityCount(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityList(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityDetailList(@Param("appConfigAuthList") List<DeployAppConfigAuthorityVo> appConfigAuthList);

    List<DeployAppEnvAutoConfigVo> getAppEnvAutoConfigListBySystemIdAndModuleIdAndEnvIdAndInstanceIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceIdList") List<Long> instanceIdList);

    List<DeployAppEnvAutoConfigKeyValueVo> getAppEnvAutoConfigKeyValueList(DeployAppEnvAutoConfigVo envAutoConfigVo);

    String getAppConfig(DeployAppConfigVo deployAppConfigVo);

    DeployAppConfigVo getAppConfigVo(DeployAppConfigVo deployAppConfigVo);

    DeployAppConfigVo getAppConfigByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    List<DeployAppConfigVo> getAppConfigListByAppSystemId(Long appSystemId);

    String getAppConfigOverrideConfig(DeployAppConfigOverrideVo deployAppConfigOverrideVo);

    List<DeployAppConfigOverrideVo> getAppConfigOverrideListByAppSystemId(Long appSystemId);

    DeployAppConfigVo getAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    List<DeployAppEnvironmentVo> getDeployAppEnvListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList, @Param("schemaName") String schemaName);

    List<Long> getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList, @Param("schemaName") String schemaName);

    List<Long> getAppModuleEnvAutoConfigInstanceIdList(@Param("searchVo") DeployAppEnvAutoConfigVo searchVo, @Param("schemaName") String schemaName);

    RunnerGroupVo getAppModuleRunnerGroupByAppSystemIdAndModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    Integer insertAppConfigAuthority(DeployAppConfigAuthorityVo deployAppConfigAuthorityVo);

    Integer insertAppModuleRunnerGroup(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("runnerGroupId") Long runnerGroupId);

    Integer insertAppEnvAutoConfig(DeployAppEnvAutoConfigVo appEnvAutoConfigVo);

    Integer insertAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer insertAppConfigOverride(DeployAppConfigOverrideVo deployAppOverrideOverrideVo);

    Integer insertAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    void insertAppConfigEnv(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    void insertAppConfigSystemFavorite(@Param("appSystemId") Long appSystemId, @Param("userUuid") String userUuid);

    Integer updateAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer updateAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    Integer deleteAppConfigAuthorityByAppIdAndEnvIdAndAuthUuidAndLcd(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId, @Param("authUuid") String uuid, @Param("lcd") Date nowTime);

    Integer deleteAppEnvAutoConfig(DeployAppEnvAutoConfigVo deployAppEnvAutoConfigVo);

    Integer deleteAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    Integer getAppSystemIdListCount(DeployResourceSearchVo searchVo);

    int getCiEntityIdListCount(Integer isConfig);

    int getAppModuleEnvAutoConfigInstanceIdCount(@Param("searchVo") DeployAppEnvAutoConfigVo searchVo, @Param("schemaName") String schemaName);

    void deleteAppConfigEnvByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    void deleteAppConfigSystemFavoriteByAppSystemIdAndUserUuid(@Param("appSystemId") Long appSystemId, @Param("userUuid") String userUuid);

}
