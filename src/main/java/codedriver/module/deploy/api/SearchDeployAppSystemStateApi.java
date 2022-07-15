package codedriver.module.deploy.api;

import codedriver.framework.cmdb.crossover.IAttrCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiCrossoverMapper;
import codedriver.framework.cmdb.crossover.ICiEntityCrossoverService;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/7/14 4:57 下午
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppSystemStateApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "查询发布添加应用时的状态列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/state/search";
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字", xss = true),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显状态列表")
    })
    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {

        //获取应用系统的模型id
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        ICiCrossoverMapper ciCrossoverMapper = CrossoverServiceFactory.getApi(ICiCrossoverMapper.class);
        CiVo appCiVo = ciCrossoverMapper.getCiByName("APP");
        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        AttrVo stateAttrVo = attrCrossoverMapper.getAttrByCiIdAndName(appCiVo.getId(),"state");

        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(stateAttrVo.getTargetCiId());

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