package codedriver.module.deploy.api.job;

import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author longrf
 * @date 2022/7/7 12:05 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployJobModuleApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return null;
    }

    @Override
    public String getToken() {
        return null;
    }
}
