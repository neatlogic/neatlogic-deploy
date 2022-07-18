package codedriver.module.deploy.api;

import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigAccountVo;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/7/18 11:29
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigEnvDBConfigForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取某个环境的DBConfig配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/get/forautoexec";
    }

    @Input({
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "Runner的ID"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "runner组信息"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "应用名"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "模块名"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境名")
    })
    @Output({
            @Param(name = "tbodyList", desc = "DB配置")
    })
    @Description(desc = "发布作业专用-获取某个环境的DBConfig配置")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnDBUserObject = new JSONObject();

        //获取环境下的所有db配置
        List<DeployAppConfigEnvDBConfigVo> configVoList = deployAppConfigMapper.getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"));
        if (CollectionUtils.isEmpty(configVoList)) {
            return null;
        }
        Map<Long, DeployAppConfigEnvDBConfigVo> configVoMap = configVoList.stream().collect(Collectors.toMap(DeployAppConfigEnvDBConfigVo::getDbResourceId, e -> e));

        //获取db属性
        List<Long> allDBResourceIdList = new ArrayList<>(configVoMap.keySet());
        ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo paramCiEntityVo = new CiEntityVo();
        paramCiEntityVo.setIdList(allDBResourceIdList);
        List<CiEntityVo> allDBResourceInfoList = ciEntityCrossoverService.getCiEntityByIdList(paramCiEntityVo);

        for (CiEntityVo ciEntityVo : allDBResourceInfoList) {
            DeployAppConfigEnvDBConfigVo dbConfigVo = configVoMap.get(ciEntityVo.getId());
            //db的账号列表
            List<DeployAppConfigEnvDBConfigAccountVo> accountList = dbConfigVo.getAccountList();
            if (CollectionUtils.isEmpty(accountList)) {
                continue;
            }
            for (DeployAppConfigEnvDBConfigAccountVo accountVo : accountList) {
                JSONObject dbResourceObj = new JSONObject();
                JSONObject nodeObj = new JSONObject();

                nodeObj.put("resourceId", ciEntityVo.getId());
                nodeObj.put("nodeName", ciEntityVo.getName());
                nodeObj.put("nodeType", ciEntityVo.getCiName());
                nodeObj.put("username", accountVo.getAccount());
                nodeObj.put("password", "{ENCRYPTED}" + RC4Util.encrypt(AutoexecJobVo.AUTOEXEC_RC4_KEY, RC4Util.decrypt(accountVo.getPasswordCipher().replace("RC4:", StringUtils.EMPTY))));
                List<AttrEntityVo> attrEntityList = ciEntityVo.getAttrEntityList();
                for (AttrEntityVo attrEntityVo : attrEntityList) {
                    if (StringUtils.equals("ip", attrEntityVo.getAttrName())) {
                        nodeObj.put("host", attrEntityVo.getValueList().get(0));
                        continue;
                    }
                    if (StringUtils.equals("name", attrEntityVo.getAttrName())) {
                        nodeObj.put("name", attrEntityVo.getValueList().get(0));
                        continue;
                    }
                    if (StringUtils.equals("service_addr", attrEntityVo.getAttrName())) {
                        nodeObj.put("serviceAddr", attrEntityVo.getValueList().get(0));
                        continue;
                    }
                    if (StringUtils.equals("port", attrEntityVo.getAttrName())) {
                        nodeObj.put("port", attrEntityVo.getValueList().get(0));
                        continue;
                    }
                    dbResourceObj.put("node", nodeObj);
                    dbResourceObj.put("args", dbConfigVo.getConfig());
                    returnDBUserObject.put(dbConfigVo.getDbAlias() + "." + accountVo.getAccountAlias(), dbResourceObj);
                }
            }
        }
        return returnDBUserObject;
    }
}
