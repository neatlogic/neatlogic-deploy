package neatlogic.module.deploy.api.version;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.chart.DeployVersionChartHandlerFactory;
import neatlogic.framework.deploy.chart.IDeployVersionChartHandler;
import neatlogic.framework.deploy.constvalue.DeployVersionChart;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployVersionChartApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "nmdav.getdeployversionchartapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/versoin/chart/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", desc = "common.versionid", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "name", desc = "nmdav.getdeployversionchartapi.input.param.desc.name", member = DeployVersionChart.class, isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "chart", desc = "nmdav.getdeployversionchartapi.input.param.desc.chart", isRequired = true, type = ApiParamType.STRING),
    })
    @Output({
    })
    @Description(desc = "nmdav.getdeployversionchartapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        IDeployVersionChartHandler handler = DeployVersionChartHandlerFactory.getHandler(paramObj.getString("name"));
        if (handler != null) {
            return handler.getChartData(paramObj.getString("chart"), paramObj.getLong("versionId"));
        }
        return null;
    }
}
