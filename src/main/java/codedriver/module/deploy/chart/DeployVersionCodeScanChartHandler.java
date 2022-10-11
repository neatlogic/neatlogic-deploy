package codedriver.module.deploy.chart;

import codedriver.framework.deploy.chart.DeployVersionChartHandlerBase;
import codedriver.framework.deploy.constvalue.DeployVersionChartMenu;

public class DeployVersionCodeScanChartHandler extends DeployVersionChartHandlerBase {

    @Override
    protected Object myGetChartData(String chartType, Long versionId) {
        return null;
    }

    @Override
    public String getMenu() {
        return DeployVersionChartMenu.CODE_SCAN.getValue();
    }
}
