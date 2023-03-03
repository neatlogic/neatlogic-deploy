package neatlogic.module.deploy.api.appconfig.system;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IAttrCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.service.DeployAppConfigService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/7/14 6:00 下午
 */
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppAttrApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigService deployAppConfigService;

    @Override
    public String getName() {
        return "查询发布应用属性下拉列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/attr/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "attrName", type = ApiParamType.STRING, desc = "属性名称",isRequired = true),
            @Param(name = "ciName", type = ApiParamType.STRING, desc = "模型名称",isRequired = true),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字", xss = true),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显列表")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class)
    })
    @Description(desc = "查询发布应用属性下拉列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //获取应用系统的模型id
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo ciVo = ciCrossoverMapper.getCiByName(paramObj.getString("ciName"));
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        AttrVo attrVo = attrCrossoverMapper.getAttrByCiIdAndName(ciVo.getId(), paramObj.getString("attrName"));

        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(attrVo.getTargetCiId());

        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = new ArrayList<>();
            for (int i = 0; i < defaultValue.size(); i++) {
                try {
                    idList.add(defaultValue.getLong(i));
                } catch (Exception ignored) {

                }
            }
            if (CollectionUtils.isNotEmpty(idList)) {
                ciEntityVo.setIdList(idList);
            }
        }
        if (StringUtils.isNotBlank(paramObj.getString("keyword"))) {
            ciEntityVo.setName(paramObj.getString("keyword"));
        }
        //不需要多余的属性和关系
        ciEntityVo.setAttrIdList(new ArrayList<Long>() {{
            this.add(0L);
        }});
        ciEntityVo.setRelIdList(new ArrayList<Long>() {{
            this.add(0L);
        }});
        ICiEntityCrossoverService ciEntityCrossoverService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        List<CiEntityVo> ciEntityList = ciEntityCrossoverService.searchCiEntity(ciEntityVo);
        JSONArray jsonList = new JSONArray();
        for (CiEntityVo ciEntity : ciEntityList) {
            JSONObject obj = new JSONObject();
            obj.put("id", ciEntity.getId());
            obj.put("name", StringUtils.isNotBlank(ciEntity.getName()) ? ciEntity.getName() : "-");
            jsonList.add(obj);
        }
        return jsonList;
    }
}
