package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionEnvVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/5/26 2:33 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.searchdeployversionapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/version/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "defaultValue", desc = "common.defaultvalue", type = ApiParamType.JSONARRAY),
            @Param(name = "keyword", desc = "common.keyword", type = ApiParamType.STRING),
            @Param(name = "startTimeRange", type = ApiParamType.JSONOBJECT, desc = "common.planstarttime", help = "入参：{startTime（开始时间）与endTime（结束时间）}，或者{timeRange（时间范围）与timeUnit（时间范围参数）}"),
            @Param(name = "appSystemIdList", desc = "term.appsystemidlist", type = ApiParamType.JSONARRAY),
            @Param(name = "appModuleIdList", desc = "term.cmdb.appmoduleidlist", type = ApiParamType.JSONARRAY),
            @Param(name = "statusList", desc = "common.status", type = ApiParamType.JSONARRAY),
            @Param(name = "envId", desc = "term.cmdb.envid", type = ApiParamType.LONG),
            @Param(name = "currentPage", desc = "common.currentpage", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "common.pagesize", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "tbodyList", explode = DeployVersionVo[].class, desc = "common.tbodylist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmdav.searchdeployversionapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployVersionVo paramVersionVo = paramObj.toJavaObject(DeployVersionVo.class);
        List<DeployVersionVo> returnVersionList = new ArrayList<>();

        int count = deployVersionMapper.searchDeployVersionCount(paramVersionVo);
        if (count > 0) {
            paramVersionVo.setRowNum(count);
            List<Long> idList = deployVersionMapper.getDeployVersionIdList(paramVersionVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                returnVersionList = deployVersionMapper.getDeployVersionByIdList(idList);
                List<DeployVersionVo> versionVoListIncludeEnvList = deployVersionMapper.getDeployVersionIncludeEnvListByVersionIdList(idList);
                Map<Long, List<DeployVersionEnvVo>> allEnvListMap = versionVoListIncludeEnvList.stream().collect(Collectors.toMap(DeployVersionVo::getId, DeployVersionVo::getEnvList));
                Map<Long, Map<String, Long>> versionId2Map = new HashMap<>();
                List<Map<String, Object>> versionHighestSeverityCveCountList = deployVersionMapper.getVersionHighestSeverityCveCountListByVersionIdListGroupByVersionIdAndHighestSeverity(idList);
                for (Map<String, Object> map : versionHighestSeverityCveCountList) {
                    Long versionId = (Long) map.get("versionId");
                    String highestSeverity = (String) map.get("highestSeverity");
                    Long cveCount = (Long) map.get("cveCount");
                    versionId2Map.computeIfAbsent(versionId, key -> new HashMap<>()).put(highestSeverity, cveCount);
                }
                //补充版本的环境
                for (DeployVersionVo returnVersion : returnVersionList) {
                    List<DeployVersionEnvVo> returnVersionEnvList = new ArrayList<>();
                    List<DeployVersionEnvVo> versionEnvVoList = returnVersion.getEnvList();
                    Map<Long, DeployVersionEnvVo> envListIncludeStatus = versionEnvVoList.stream().collect(Collectors.toMap(DeployVersionEnvVo::getEnvId, e -> e));

                    List<DeployVersionEnvVo> allVersionEnvVoList = allEnvListMap.get(returnVersion.getId());
                    if (CollectionUtils.isNotEmpty(allVersionEnvVoList)) {
                        for (DeployVersionEnvVo envVo : allVersionEnvVoList) {
                            if (envListIncludeStatus.containsKey(envVo.getEnvId())) {
                                envVo = envListIncludeStatus.get(envVo.getEnvId());
                            }
                            returnVersionEnvList.add(envVo);
                            if(Objects.equals(envVo.getEnvId(), paramVersionVo.getEnvId())){
                                returnVersion.setCurrentEnvBuildNo(envVo.getBuildNo());
                            }
                        }
                    }
                    returnVersion.setEnvList(returnVersionEnvList);
                    Map<String, Long> HighestSeverity2CveCountMap = versionId2Map.get(returnVersion.getId());
                    if (MapUtils.isNotEmpty(HighestSeverity2CveCountMap)) {
                        Long highCveCount = HighestSeverity2CveCountMap.get("HIGH");
                        Long criticalCveCount = HighestSeverity2CveCountMap.get("CRITICAL");
                        Long criticalStarCveCount = HighestSeverity2CveCountMap.get("CRITICAL*");
                        returnVersion.setHighCveCount(highCveCount == null ? 0 : highCveCount);
                        returnVersion.setCriticalCveCount(criticalCveCount == null ? 0 : criticalCveCount);
                        returnVersion.setCriticalStarCveCount(criticalStarCveCount == null ? 0 : criticalStarCveCount);
                    }
                }
            }
        }
        return TableResultUtil.getResult(returnVersionList, paramVersionVo);
    }
}
