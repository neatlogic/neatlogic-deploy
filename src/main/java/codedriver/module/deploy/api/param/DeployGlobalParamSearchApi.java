package codedriver.module.deploy.api.param;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CiphertextPrefix;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.deploy.auth.DEPLOY_MODIFY;
import codedriver.framework.deploy.constvalue.DeployGlobalParamType;
import codedriver.framework.deploy.dto.param.DeployGlobalParamVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployGlobalParamMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/4/19 10:01 上午
 */
@Service
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployGlobalParamSearchApi extends PrivateApiComponentBase {

    @Resource
    DeployGlobalParamMapper deployGlobalParamMapper;

    @Override
    public String getName() {
        return "查询发布全局参数列表";
    }

    @Override
    public String getToken() {
        return "deploy/global/param/search";
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
    @Description(desc = "查询发布全局参数列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployGlobalParamVo globalParamVo = paramObj.toJavaObject(DeployGlobalParamVo.class);
        List<DeployGlobalParamVo> GlobalParamList = new ArrayList<>();
        int paramCount = deployGlobalParamMapper.getGlobalParamCount(globalParamVo);
        if (paramCount > 0) {
            globalParamVo.setRowNum(paramCount);
            GlobalParamList = deployGlobalParamMapper.getGlobalParam(globalParamVo);
            for (DeployGlobalParamVo paramVo : GlobalParamList) {
                if (StringUtils.equals(DeployGlobalParamType.PASSWORD.getValue(), paramVo.getType()) && StringUtils.isNotBlank(paramVo.getValue()) && paramVo.getValue().startsWith(CiphertextPrefix.RC4.getValue())) {
                    paramVo.setValue(RC4Util.decrypt(paramVo.getValue().substring(4)));
                }
            }
        }
        return TableResultUtil.getResult(GlobalParamList, globalParamVo);
    }
}
