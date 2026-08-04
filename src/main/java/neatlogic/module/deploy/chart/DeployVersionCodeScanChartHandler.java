package neatlogic.module.deploy.chart;

import neatlogic.framework.deploy.chart.DeployVersionChartHandlerBase;
import neatlogic.framework.deploy.constvalue.DeployVersionChart;
import neatlogic.framework.deploy.dto.version.DeployVersionBuildQualityVo;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import neatlogic.framework.util.$;
@Component
public class DeployVersionCodeScanChartHandler extends DeployVersionChartHandlerBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    private final static Map<String, Function<Long, JSONObject>> chartMap = new HashMap<>();

    final static String LAST_CODE_ISSUE = "last_code_issue"; //最近一次代码问题
    final static String LAST_FIVE_ANNOTATION_RATE = "last_five_annotation_rate"; //最近五次注释率
    final static String LAST_FIVE_CODE_STATISTICS = "last_five_code_statistics"; //最近五次代码统计
    final static String CODE_QUALITY_TREND = "code_quality_trend"; //bug/漏洞/代码异味趋势图
    final static String CODE_REPEATABILITY = "code_repeatability"; //重复度趋势图

    @PostConstruct
    public void init() {

        chartMap.put(LAST_CODE_ISSUE, (versionId) -> {
            JSONObject data = new JSONObject();
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionBuildQualityVo> list = deployVersionMapper.getDeployVersionBuildQualityListByVersionIdWithLimit(versionId, 1);
            if (list.size() > 0) {
                DeployVersionBuildQualityVo qualityVo = list.get(0);
                dataList.add(new JSONObject() {
                    {
                        this.put("value", qualityVo.getBlockerViolations());
                        this.put("name", "blocker issues");
                    }
                });
                dataList.add(new JSONObject() {
                    {
                        this.put("value", qualityVo.getCriticalViolations());
                        this.put("name", "critical issues");
                    }
                });
            }
            return data;
        });

        chartMap.put(LAST_FIVE_ANNOTATION_RATE, (versionId) -> {
            JSONObject data = new JSONObject();
            data.put("legend", new JSONObject() {
                {
                    this.put("commentLinesDensity", $.t("nmdc.deployversioncodescancharthandler.runtime.commentlinesdensity"));
                    this.put("publicDocumentedApiDensity", $.t("nmdc.deployversioncodescancharthandler.runtime.publicdocumentedapidensity"));
                }
            });
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionBuildQualityVo> list = deployVersionMapper.getDeployVersionBuildQualityListByVersionIdWithLimit(versionId, 5);
            if (list.size() > 0) {
                list.sort(Comparator.comparingLong(DeployVersionBuildQualityVo::getId));
                for (DeployVersionBuildQualityVo vo : list) {
                    dataList.add(new JSONObject() {
                        {
                            this.put("buildTime", vo.getBuildTime());
                            this.put("commentLinesDensity", vo.getCommentLinesDensity());
                            this.put("publicDocumentedApiDensity", vo.getPublicDocumentedApiDensity());
                        }
                    });
                }
            }
            return data;
        });

        chartMap.put(LAST_FIVE_CODE_STATISTICS, (versionId) -> {
            JSONObject data = new JSONObject();
            data.put("legend", new JSONObject() {
                {
                    this.put("lines", $.t("nmdc.deployversioncodescancharthandler.runtime.lines"));
                    this.put("ncloc", $.t("nmdc.deployversioncodescancharthandler.runtime.ncloc"));
                }
            });
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionBuildQualityVo> list = deployVersionMapper.getDeployVersionBuildQualityListByVersionIdWithLimit(versionId, 5);
            if (list.size() > 0) {
                list.sort(Comparator.comparingLong(DeployVersionBuildQualityVo::getId));
                for (DeployVersionBuildQualityVo vo : list) {
                    dataList.add(new JSONObject() {
                        {
                            this.put("buildTime", vo.getBuildTime());
                            this.put("lines", vo.getLines());
                            this.put("ncloc", vo.getNcloc());
                        }
                    });
                }
            }
            return data;
        });

        chartMap.put(CODE_QUALITY_TREND, (versionId) -> {
            JSONObject data = new JSONObject();
            data.put("legend", new JSONObject() {
                {
                    this.put("bugs", "Bugs");
                    this.put("vulnerabilities", $.t("nmdc.deployversioncodescancharthandler.runtime.vulnerabilities"));
                    this.put("codeSmells", $.t("nmdc.deployversioncodescancharthandler.runtime.codesmells"));
                }
            });
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionBuildQualityVo> list = deployVersionMapper.getDeployVersionBuildQualityListByVersionIdWithLimit(versionId, 5);
            if (list.size() > 0) {
                list.sort(Comparator.comparingLong(DeployVersionBuildQualityVo::getId));
                for (DeployVersionBuildQualityVo vo : list) {
                    dataList.add(new JSONObject() {
                        {
                            this.put("buildTime", vo.getBuildTime());
                            this.put("bugs", vo.getBugs());
                            this.put("vulnerabilities", vo.getVulnerabilities());
                            this.put("codeSmells", vo.getCodeSmells());
                        }
                    });
                }
            }
            return data;
        });

        chartMap.put(CODE_REPEATABILITY, (versionId) -> {
            JSONObject data = new JSONObject();
            data.put("legend", new JSONObject() {
                {
                    this.put("duplicatedLinesDensity", $.t("nmdc.deployversioncodescancharthandler.runtime.duplicatedlinesdensity"));
                }
            });
            JSONArray dataList = new JSONArray();
            data.put("data", dataList);
            List<DeployVersionBuildQualityVo> list = deployVersionMapper.getDeployVersionBuildQualityListByVersionIdWithLimit(versionId, 5);
            if (list.size() > 0) {
                list.sort(Comparator.comparingLong(DeployVersionBuildQualityVo::getId));
                for (DeployVersionBuildQualityVo vo : list) {
                    dataList.add(new JSONObject() {
                        {
                            this.put("buildTime", vo.getBuildTime());
                            this.put("duplicatedLinesDensity", vo.getDuplicatedLinesDensity());
                        }
                    });
                }
            }
            return data;
        });
    }

    @Override
    protected JSONObject myGetChartData(String chartName, Long versionId) {
        Function<Long, JSONObject> function = chartMap.get(chartName);
        if (function != null) {
            return function.apply(versionId);
        }
        return null;
    }

    @Override
    public String getName() {
        return DeployVersionChart.CODE_SCAN.getValue();
    }
}
