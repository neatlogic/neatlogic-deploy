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
        return "获取发布版本图表数据";
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
            @Param(name = "versionId", desc = "版本号", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "name", desc = "DeployVersionChart定义的图表tab名称", member = DeployVersionChart.class, isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "chart", desc = "图表名称", isRequired = true, type = ApiParamType.STRING),
    })
    @Output({
    })
    @Description(desc = "获取发布版本图表数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        IDeployVersionChartHandler handler = DeployVersionChartHandlerFactory.getHandler(paramObj.getString("name"));
        if (handler != null) {
            return handler.getChartData(paramObj.getString("chart"), paramObj.getLong("versionId"));
        }
        return null;
    }
}
