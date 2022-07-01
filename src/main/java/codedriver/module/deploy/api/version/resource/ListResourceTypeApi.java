package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.deploy.constvalue.DeployResourceType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
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
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListResourceTypeApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(ListResourceTypeApi.class);

    @Override
    public String getName() {
        return "获取资源类型列表";
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
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID", type = ApiParamType.LONG),
            @Param(name = "hasMirror", desc = "是否有镜像制品", rule = "0,1", type = ApiParamType.ENUM),
    })
    @Output({
            @Param(explode = ValueTextVo[].class)
    })
    @Description(desc = "获取资源类型列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<ValueTextVo> list = new ArrayList<>();
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        Integer hasMirror = paramObj.getInteger("hasMirror");
        if (buildNo == null && envId == null) {
            throw new ParamNotExistsException("buildNo", "envId");
        }
        if (buildNo != null) {
            list.add(new ValueTextVo(DeployResourceType.VERSION_PRODUCT.getValue(), DeployResourceType.VERSION_PRODUCT.getText()));
        } else {
            if (Objects.equals(hasMirror, 1)) {
                list.add(new ValueTextVo(DeployResourceType.MIRROR_PRODUCT.getValue(), DeployResourceType.MIRROR_PRODUCT.getText()));
                list.add(new ValueTextVo(DeployResourceType.DIFF_MIRROR.getValue(), DeployResourceType.DIFF_MIRROR.getText()));
            } else {
                list.add(new ValueTextVo(DeployResourceType.ENV_PRODUCT.getValue(), DeployResourceType.ENV_PRODUCT.getText()));
                list.add(new ValueTextVo(DeployResourceType.DIFF_DIRECTORY.getValue(), DeployResourceType.DIFF_DIRECTORY.getText()));
            }
        }
        list.add(new ValueTextVo(DeployResourceType.SQL_SCRIPT.getValue(), DeployResourceType.SQL_SCRIPT.getText()));
        return list;
    }
}
