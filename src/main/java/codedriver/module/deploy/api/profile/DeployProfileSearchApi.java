package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_PROFILE_MODIFY;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/3/16 11:23 上午
 */
@AuthAction(action = DEPLOY_PROFILE_MODIFY.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployProfileSearchApi extends PrivateApiComponentBase {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Override
    public String getName() {
        return "查询发布工具profile列表";
    }

    @Override
    public String getToken() {
        return "deploy/profile/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "operateId", desc = "工具id", type = ApiParamType.LONG),
            @Param(name = "type", desc = "工具类型", type = ApiParamType.STRING),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, desc = "关联配置项id"),
            @Param(name = "keyword", desc = "关键词（名称、描述）", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "needPage", desc = "是否分页", type = ApiParamType.BOOLEAN),
            @Param(name = "pageSize", desc = "每页最大数", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "tbodyList", explode = DeployProfileVo[].class, desc = "工具profile列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "发布profile列表查询接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployProfileVo paramProfileVo = JSON.toJavaObject(paramObj, DeployProfileVo.class);
        List<DeployProfileVo> returnList = null;
        int profileCount = deployProfileMapper.searchDeployProfileCount(paramProfileVo);
        if (profileCount > 0) {
            paramProfileVo.setRowNum(profileCount);
            List<Long> profileIdList = deployProfileMapper.getDeployProfileIdList(paramProfileVo);
            returnList = deployProfileMapper.getProfileListByIdList(profileIdList);
//            Map<Object, Integer> toolAndScriptReferredCountMap = DependencyManager.getBatchDependencyCount(DeployFromType.DEPLOY_PROFILE_OPERATION, profileIdList);
//            if (!toolAndScriptReferredCountMap.isEmpty()) {
//                for (DeployProfileVo profileVo : returnList) {
//                    profileVo.setAutoexecToolAndScriptCount(toolAndScriptReferredCountMap.get(profileVo.getId()));
//                }
//            }
        }
        if (CollectionUtils.isEmpty(returnList)) {
            returnList = new ArrayList<>();
        }
        return TableResultUtil.getResult(returnList, paramProfileVo);
    }
}
