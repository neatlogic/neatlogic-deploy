package codedriver.module.deploy.api.version;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/5/26 2:33 下午
 */
@Service
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
            @Param(name = "appSystemIdList", desc = "应用系统id", type = ApiParamType.JSONARRAY),
            @Param(name = "appModuleIdList", desc = "应用模块id", type = ApiParamType.JSONARRAY),
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
        DeployVersionVo versionVo = paramObj.toJavaObject(DeployVersionVo.class);
        List<DeployVersionVo> returnList = new ArrayList<>();

        int count = deployVersionMapper.searchDeployVersionCount(versionVo);
        if (count > 0) {
            versionVo.setRowNum(count);
            returnList = deployVersionMapper.searchDeployVersion(versionVo);
        }
        return TableResultUtil.getResult(returnList, versionVo);
    }
}
