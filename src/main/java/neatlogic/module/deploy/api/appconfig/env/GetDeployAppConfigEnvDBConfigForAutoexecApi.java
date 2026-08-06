package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.*;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.systemuser.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/7/18 11:29
 */
@Service
@AuthUser(SystemUser.AUTOEXEC)
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppConfigEnvDBConfigForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.getname";
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
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.runnerid"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.runnergroup"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.jobid"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.phasename"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.sysid"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.moduleid"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.envid"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.sysname"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.modulename"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.input.param.desc.envname")
    })
    @Output({
            @Param(name = "tbodyList", desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.output.param.desc.tbodylist")
    })
    @Description(desc = "nmdaae.getdeployappconfigenvdbconfigforautoexecapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnDBUserObject = new JSONObject();

        //获取环境下的所有db配置
        List<DeployAppConfigEnvDBConfigVo> allDBConfigVoList = deployAppConfigMapper.getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"));
        if (CollectionUtils.isEmpty(allDBConfigVoList)) {
            return null;
        }
        Set<Long> dbResourceIdSet = allDBConfigVoList.stream().map(DeployAppConfigEnvDBConfigVo::getDbResourceId).collect(Collectors.toSet());
        //获取db属性
        List<CiEntityVo> allDBResourceInfoList = null;
        IResourceEntityCrossoverService resourceEntityCrossoverService = CrossoverServiceFactory.getApi(IResourceEntityCrossoverService.class);
        CiVo rootCiVo = resourceEntityCrossoverService.getViewRootCi("scence_database_ip_port_env_appmodule");
        if (rootCiVo != null) {
            Long ciId = rootCiVo.getId();
            if (ciId != null) {
                CiEntityVo paramCiEntityVo = new CiEntityVo();
                List<Long> attrIdList = new ArrayList<>();
                List<String> attrNameList = Arrays.asList("ip", "name", "service_addr", "port");
                IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
                List<AttrVo> attrList = attrCrossoverMapper.getAttrByCiId(ciId);
                for (AttrVo attrVo : attrList) {
                    if (attrNameList.contains(attrVo.getName())) {
                        attrIdList.add(attrVo.getId());
                    }
                }
                paramCiEntityVo.setAttrIdList(attrIdList);
                paramCiEntityVo.setRelIdList(new ArrayList<>());
                paramCiEntityVo.setCiId(ciId);
                ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
                paramCiEntityVo.setIdList(new ArrayList<>(dbResourceIdSet));
                allDBResourceInfoList = ciEntityCrossoverService.searchCiEntity(paramCiEntityVo);
            }
        }
        if (CollectionUtils.isEmpty(allDBResourceInfoList)) {
            return null;
        }

        Map<Long, CiEntityVo> allDBciEntityVoMap = allDBResourceInfoList.stream().collect(Collectors.toMap(CiEntityVo::getId, e -> e));

        for (DeployAppConfigEnvDBConfigVo dbConfigVo : allDBConfigVoList) {
            CiEntityVo dbCiEntityVo = allDBciEntityVoMap.get(dbConfigVo.getDbResourceId());
            if (dbCiEntityVo == null) {
                continue;
            }
            JSONObject nodeObj = new JSONObject();
            JSONObject dbResourceObj = new JSONObject();

            nodeObj.put("resourceId", dbCiEntityVo.getId());
            nodeObj.put("nodeName", dbCiEntityVo.getName());
            nodeObj.put("nodeType", dbCiEntityVo.getCiName());
            nodeObj.put("username", dbConfigVo.getAccount());
            nodeObj.put("password", dbConfigVo.getPasswordCipher());
            List<AttrEntityVo> attrEntityList = dbCiEntityVo.getAttrEntityList();
            for (AttrEntityVo attrEntityVo : attrEntityList) {
                if (StringUtils.equals("ip", attrEntityVo.getAttrName())) {
                    nodeObj.put("host", attrEntityVo.getValueList().get(0));
                } else if (StringUtils.equals("name", attrEntityVo.getAttrName())) {
                    nodeObj.put("name", attrEntityVo.getValueList().get(0));
                } else if (StringUtils.equals("service_addr", attrEntityVo.getAttrName())) {
                    nodeObj.put("serviceAddr", attrEntityVo.getValueList().get(0));
                } else if (StringUtils.equals("port", attrEntityVo.getAttrName())) {
                    nodeObj.put("port", attrEntityVo.getValueList().get(0));
                }
            }
            dbResourceObj.put("node", nodeObj);
            dbResourceObj.put("args", dbConfigVo.getConfig());
            returnDBUserObject.put(dbConfigVo.getDbSchema(), dbResourceObj);
        }
        return returnDBUserObject;
    }
}
