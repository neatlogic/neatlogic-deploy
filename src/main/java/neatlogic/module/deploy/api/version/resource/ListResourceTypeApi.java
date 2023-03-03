package neatlogic.module.deploy.api.version.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployResourceType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListResourceTypeApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(ListResourceTypeApi.class);

    @Override
    public String getName() {
        return "获取制品类型列表";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/type/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "type", rule = "build,env", desc = "查看制品的来源(build:从buildNo查看;env:从环境查看)", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "isMirror", desc = "是否有镜像制品(环境才可能有镜像制品，type=env时才需要此参数)", rule = "0,1", type = ApiParamType.ENUM),
    })
    @Output({
            @Param(explode = ValueTextVo[].class)
    })
    @Description(desc = "获取制品类型列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<ValueTextVo> list = new ArrayList<>();
        String type = paramObj.getString("type");
        Integer hasMirror = paramObj.getInteger("hasMirror");
        if ("build".equals(type)) {
            list.add(new ValueTextVo(DeployResourceType.BUILD_PRODUCT.getValue(), DeployResourceType.BUILD_PRODUCT.getText()));
            list.add(new ValueTextVo(DeployResourceType.BUILD_SQL_SCRIPT.getValue(), DeployResourceType.BUILD_SQL_SCRIPT.getText()));
        } else {
            if (Objects.equals(hasMirror, 1)) {
                list.add(new ValueTextVo(DeployResourceType.MIRROR_PRODUCT.getValue(), DeployResourceType.MIRROR_PRODUCT.getText()));
                list.add(new ValueTextVo(DeployResourceType.MIRROR_DIFF.getValue(), DeployResourceType.MIRROR_DIFF.getText()));
            } else {
                list.add(new ValueTextVo(DeployResourceType.ENV_PRODUCT.getValue(), DeployResourceType.ENV_PRODUCT.getText()));
                list.add(new ValueTextVo(DeployResourceType.ENV_DIFF_DIRECTORY.getValue(), DeployResourceType.ENV_DIFF_DIRECTORY.getText()));
            }
            list.add(new ValueTextVo(DeployResourceType.ENV_SQL_SCRIPT.getValue(), DeployResourceType.ENV_SQL_SCRIPT.getText()));
        }
        return list;
    }
}
