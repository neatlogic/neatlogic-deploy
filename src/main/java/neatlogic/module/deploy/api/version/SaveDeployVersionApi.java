package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.exception.cientity.CiEntityNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployAppConfigModuleRunnerGroupNotFoundException;
import neatlogic.framework.deploy.exception.DeployVersionIsRepeatException;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerGroupRunnerNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/5/26 2:33 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "保存发布版本";
    }

    @Override
    public String getToken() {
        return "deploy/version/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "version", desc = "版本", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "appSystemId", desc = "应用系统id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "appSystemName", desc = "应用名称", type = ApiParamType.STRING),
            @Param(name = "appModuleId", desc = "应用模块id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "appModuleName", desc = "应用模块名称", type = ApiParamType.STRING),
            @Param(name = "isFreeze", desc = "是否封版", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "description", desc = "描述", type = ApiParamType.STRING)
    })
    @Description(desc = "保存发布版本")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.VERSION_AND_PRODUCT_MANAGER);

        DeployVersionVo versionVo = paramObj.toJavaObject(DeployVersionVo.class);
        if (deployVersionMapper.checkDeployVersionIsRepeat(versionVo) > 0) {
            throw new DeployVersionIsRepeatException(versionVo.getVersion());
        }
        ICiEntityCrossoverMapper iCiEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(versionVo.getAppSystemId()) == null) {
            throw new CiEntityNotFoundException(versionVo.getAppSystemId());
        }
        if (iCiEntityCrossoverMapper.getCiEntityBaseInfoById(versionVo.getAppModuleId()) == null) {
            throw new CiEntityNotFoundException(versionVo.getAppModuleId());
        }

        //分配runner group和runner
        RunnerGroupVo runnerGroupVo = deployAppConfigMapper.getAppModuleRunnerGroupByAppSystemIdAndModuleId(versionVo.getAppSystemId(), versionVo.getAppModuleId());
        if (runnerGroupVo == null) {
            throw new DeployAppConfigModuleRunnerGroupNotFoundException(versionVo.getAppSystemName() + "(" + versionVo.getAppSystemId() + ")", versionVo.getAppModuleName() + "(" + versionVo.getAppModuleId() + ")");
        }
        if (CollectionUtils.isEmpty(runnerGroupVo.getRunnerMapList())) {
            throw new RunnerGroupRunnerNotFoundException(runnerGroupVo.getName() + ":" + runnerGroupVo.getId());
        }
        JSONObject runnerMap = new JSONObject();
        for (RunnerMapVo runnerMapVo : runnerGroupVo.getRunnerMapList()) {
            runnerMap.put(runnerMapVo.getRunnerMapId().toString(), runnerMapVo.getHost());
        }
        versionVo.setRunnerGroup(runnerMap);
        int runnerMapIndex = (int) (Math.random() * runnerGroupVo.getRunnerMapList().size());
        RunnerMapVo runnerMapVo = runnerGroupVo.getRunnerMapList().get(runnerMapIndex);
        versionVo.setRunnerMapId(runnerMapVo.getId());
        deployVersionMapper.insertDeployVersion(versionVo);
        return null;
    }

    public IValid version() {
        return value -> {
            DeployVersionVo vo = JSON.toJavaObject(value, DeployVersionVo.class);
            if (deployVersionMapper.checkDeployVersionIsRepeat(vo) > 0) {
                return new FieldValidResultVo(new DeployVersionIsRepeatException(vo.getVersion()));
            }
            return new FieldValidResultVo();
        };
    }
}
