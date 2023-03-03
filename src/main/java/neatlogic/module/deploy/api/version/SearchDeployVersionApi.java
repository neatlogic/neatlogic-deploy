package neatlogic.module.deploy.api.version;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
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
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return "查询发布版本列表";
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
            @Param(name = "defaultValue", desc = "默认值", type = ApiParamType.JSONARRAY),
            @Param(name = "keyword", desc = "关键词", type = ApiParamType.STRING),
            @Param(name = "startTimeRange", type = ApiParamType.JSONOBJECT, desc = "上传时间范围 (入参：{startTime（开始时间）与endTime（结束时间）}，或者{timeRange（时间范围）与timeUnit（时间范围参数）})"),
            @Param(name = "appSystemIdList", desc = "应用系统id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "appModuleIdList", desc = "应用模块id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "statusList", desc = "状态", type = ApiParamType.JSONARRAY),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "tbodyList", explode = DeployVersionVo[].class, desc = "发布版本列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询发布版本列表")
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
                        }
                    }
                    returnVersion.setEnvList(returnVersionEnvList);
                }
            }
        }
        return TableResultUtil.getResult(returnVersionList, paramVersionVo);
    }
}
