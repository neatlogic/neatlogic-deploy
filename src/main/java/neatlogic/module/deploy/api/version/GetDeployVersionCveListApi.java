/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionCvePackageVo;
import neatlogic.framework.deploy.dto.version.DeployVersionCveVo;
import neatlogic.framework.deploy.dto.version.DeployVersionCveVulnerabilityVo;
import neatlogic.framework.deploy.exception.verison.DeployVersionNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployVersionCveListApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.getdeployversioncvelistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "common.versionid"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployVersionCveVo.class, desc = "common.tbodylist")
    })
    @Description(desc = "nmdav.getdeployversioncvelistapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployVersionCveVo searchVo = paramObj.toJavaObject(DeployVersionCveVo.class);
        if(deployVersionMapper.getDeployVersionBaseInfoById(searchVo.getVersionId()) == null) {
            throw new DeployVersionNotFoundEditTargetException(searchVo.getVersionId());
        }
        int rowNum = deployVersionMapper.searchDeployVersionCveCount(searchVo);
        if (rowNum == 0) {
            return TableResultUtil.getResult(new ArrayList(), searchVo);
        }
        searchVo.setRowNum(rowNum);
        List<DeployVersionCveVo> tbodyList = deployVersionMapper.searchDeployVersionCveList(searchVo);
        List<Long> cveIdList = tbodyList.stream().map(DeployVersionCveVo::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(cveIdList)) {
            List<DeployVersionCveVulnerabilityVo> vulnerabilityList = deployVersionMapper.getDeployVersionCveVulnerabilityListByCveIdList(cveIdList);
            if (CollectionUtils.isNotEmpty(vulnerabilityList)) {
                Map<Long, List<DeployVersionCveVulnerabilityVo>> cveIdToVulnerabilityListMap = new HashMap<>();
                for (DeployVersionCveVulnerabilityVo vulnerabilityVo : vulnerabilityList) {
                    cveIdToVulnerabilityListMap.computeIfAbsent(vulnerabilityVo.getCveId(), key -> new ArrayList<>()).add(vulnerabilityVo);
                }
                for (DeployVersionCveVo deployVersionCveVo : tbodyList) {
                    deployVersionCveVo.setVulnerabilityIds(cveIdToVulnerabilityListMap.get(deployVersionCveVo.getId()));
                }
            }
            List<DeployVersionCvePackageVo> packageList = deployVersionMapper.getDeployVersionCvePackageListByCveIdList(cveIdList);
            if (CollectionUtils.isNotEmpty(packageList)) {
                Map<Long, List<DeployVersionCvePackageVo>> cveIdToPackageListMap = new HashMap<>();
                for (DeployVersionCvePackageVo packageVo : packageList) {
                    cveIdToPackageListMap.computeIfAbsent(packageVo.getCveId(), key -> new ArrayList<>()).add(packageVo);
                }
                for (DeployVersionCveVo deployVersionCveVo : tbodyList) {
                    deployVersionCveVo.setPackageList(cveIdToPackageListMap.get(deployVersionCveVo.getId()));
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }

    @Override
    public String getToken() {
        return "deploy/version/cvelist/get";
    }
}
