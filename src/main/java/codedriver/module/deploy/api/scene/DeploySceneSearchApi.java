package codedriver.module.deploy.api.scene;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.dto.scene.DeploySceneVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeploySceneMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/4/15 3:04 下午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeploySceneSearchApi extends PrivateApiComponentBase {

    @Resource
    DeploySceneMapper deploySceneMapper;

    @Override
    public String getName() {
        return "查询发布场景列表";
    }

    @Override
    public String getToken() {
        return "deploy/scene/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键词", xss = true),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Description(desc = "查询发布场景列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeploySceneVo paramSceneVo = paramObj.toJavaObject(DeploySceneVo.class);
        List<DeploySceneVo> returnList = new ArrayList<>();
        int sceneCount = deploySceneMapper.getSceneCount(paramSceneVo);
        if (sceneCount > 0) {
            paramSceneVo.setRowNum(sceneCount);
            List<Long> idList = deploySceneMapper.getSceneIdList(paramSceneVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                returnList = deploySceneMapper.getSceneListByIdList(idList);
            }
        }
        return TableResultUtil.getResult(returnList, paramSceneVo);
    }
}
