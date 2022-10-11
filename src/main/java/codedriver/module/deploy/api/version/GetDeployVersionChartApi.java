package codedriver.module.deploy.api.version;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.chart.DeployVersionChartHandlerFactory;
import codedriver.framework.deploy.chart.IDeployVersionChartHandler;
import codedriver.framework.deploy.constvalue.DeployVersionChart;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
            @Param(name = "chartName", desc = "图表名称", isRequired = true, type = ApiParamType.STRING),
    })
    @Output({
    })
    @Description(desc = "获取发布版本图表数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        IDeployVersionChartHandler handler = DeployVersionChartHandlerFactory.getHandler(paramObj.getString("name"));
        if (handler != null) {
            return handler.getChartData(paramObj.getString("chartName"), paramObj.getLong("versionId"));
        }
        return null;
    }
}
