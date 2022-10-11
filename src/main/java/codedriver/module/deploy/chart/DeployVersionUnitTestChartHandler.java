package codedriver.module.deploy.chart;

import codedriver.framework.deploy.chart.DeployVersionChartHandlerBase;
import codedriver.framework.deploy.constvalue.DeployVersionChartMenu;
import codedriver.framework.deploy.dto.version.DeployVersionUnitTestVo;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DeployVersionUnitTestChartHandler extends DeployVersionChartHandlerBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    private final static Map<String, Function<Long, Object>> chartMap = new HashMap<>();

    final static String LAST_CODE_TEST_RESULT = "LAST_CODE_TEST_RESULT"; //最近一次代码测试结果
    final static String LAST_FIVE_INCREMENTAL_COVERAGE_RATE = "last_five_incremental_coverage_rate"; //最近五次增量覆盖率
    final static String LAST_FIVE_FULL_COVERAGE_RATE = "last_five_full_coverage_rate"; //最近五次全量覆盖率

    @PostConstruct
    public void init() {

        chartMap.put(LAST_CODE_TEST_RESULT, (versionId) -> {
            JSONObject data = new JSONObject();
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionUnitTestVo> list = deployVersionMapper.getDeployVersionUnitTestListByVersionIdWithLimit(versionId, 1);
            if (list.size() > 0) {
                DeployVersionUnitTestVo unitTestVo = list.get(0);
                dataList.add(new JSONObject() {
                    {
                        this.put("value", Math.round(unitTestVo.getTests() * unitTestVo.getTestSuccessDensity() / 100));
                        this.put("name", "成功");
                    }
                });
                dataList.add(new JSONObject() {
                    {
                        this.put("value", unitTestVo.getTestErrors());
                        this.put("name", "失败");
                    }
                });
            }
            return data;
        });

        chartMap.put(LAST_FIVE_INCREMENTAL_COVERAGE_RATE, (versionId) -> {
            JSONObject data = new JSONObject();
            data.put("legend", new JSONObject() {
                {
                    this.put("newBranchCoverage", "分支");
                    this.put("newLineCoverage", "行");
                }
            });
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionUnitTestVo> list = deployVersionMapper.getDeployVersionUnitTestListByVersionIdWithLimit(versionId, 1);
            if (list.size() > 0) {
                for (DeployVersionUnitTestVo vo : list) {
                    dataList.add(new JSONObject() {
                        {
                            this.put("buildTime", vo.getBuildTime());
                            this.put("newBranchCoverage", vo.getNewBranchCoverage());
                            this.put("newLineCoverage", vo.getNewLineCoverage());
                        }
                    });
                }
            }
            return data;
        });

        chartMap.put(LAST_FIVE_FULL_COVERAGE_RATE, (versionId) -> {
            JSONObject data = new JSONObject();
            data.put("legend", new JSONObject() {
                {
                    this.put("branchCoverage", "分支");
                    this.put("lineCoverage", "行");
                }
            });
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionUnitTestVo> list = deployVersionMapper.getDeployVersionUnitTestListByVersionIdWithLimit(versionId, 1);
            if (list.size() > 0) {
                for (DeployVersionUnitTestVo vo : list) {
                    dataList.add(new JSONObject() {
                        {
                            this.put("buildTime", vo.getBuildTime());
                            this.put("branchCoverage", vo.getBranchCoverage());
                            this.put("lineCoverage", vo.getLineCoverage());
                        }
                    });
                }
            }
            return data;
        });
    }

    @Override
    protected Object myGetChartData(String chartType, Long versionId) {
        return chartMap.get(chartType).apply(versionId);
    }

    @Override
    public String getMenu() {
        return DeployVersionChartMenu.UNIT_TEST.getValue();
    }
}
