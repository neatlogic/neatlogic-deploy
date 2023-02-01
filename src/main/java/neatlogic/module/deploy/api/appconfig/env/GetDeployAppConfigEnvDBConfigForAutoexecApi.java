package neatlogic.module.deploy.api.appconfig.env;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
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
@AuthAction(action = DEPLOY_BASE.class)
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
        List<DeployAppConfigEnvDBConfigVo> allDBConfigVoList = deployAppConfigMapper.getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"));
        if (CollectionUtils.isEmpty(allDBConfigVoList)) {
            return null;
        }
        Set<Long> dbResourceIdSet = allDBConfigVoList.stream().map(DeployAppConfigEnvDBConfigVo::getDbResourceId).collect(Collectors.toSet());
        //获取db属性
        ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo paramCiEntityVo = new CiEntityVo();
        paramCiEntityVo.setIdList(new ArrayList<>(dbResourceIdSet));
        List<CiEntityVo> allDBResourceInfoList = ciEntityCrossoverService.getCiEntityByIdList(paramCiEntityVo);
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
                returnDBUserObject.put(dbConfigVo.getDbSchema(), dbResourceObj);
            }
        }
        return returnDBUserObject;
    }
}
