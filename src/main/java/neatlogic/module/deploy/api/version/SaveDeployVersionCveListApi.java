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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import neatlogic.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.version.DeployVersionCvePackageVo;
import neatlogic.framework.deploy.dto.version.DeployVersionCveVo;
import neatlogic.framework.deploy.dto.version.DeployVersionCveVulnerabilityVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveDeployVersionCveListApi extends PrivateApiComponentBase {

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "nmdav.savedeployversioncvelistapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appmoduleid"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.appsystemid"),
            @Param(name = "version", type = ApiParamType.STRING, isRequired = true, desc = "common.versionname"),
            @Param(name = "cveList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "term.deploy.cvelist")
    })
    @Output({})
    @Description(desc = "nmdav.savedeployversioncvelistapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long appSystemId = paramObj.getLong("appSystemId");
        Long appModuleId = paramObj.getLong("appModuleId");
        String version = paramObj.getString("version");
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(appSystemId);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(appSystemId);
        }
        ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
        if (appModule == null) {
            throw new AppModuleNotFoundException(appModuleId);
        }
        DeployVersionVo deployVersionVo = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(version, appSystemId, appModuleId));
        if (deployVersionVo == null) {
            throw new DeployVersionNotFoundException(appSystem.getName(), appModule.getName(), version);
        }
        DeployVersionCveVo searchVo = new DeployVersionCveVo();
        searchVo.setVersionId(deployVersionVo.getId());
        int rowNum = deployVersionMapper.searchDeployVersionCveCount(searchVo);
        if (rowNum > 0) {
            List<Long> cveIdList = new ArrayList<>();
            searchVo.setRowNum(rowNum);
            searchVo.setPageSize(100);
            int pageCount = searchVo.getPageCount();
            for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                searchVo.setCurrentPage(currentPage);
                List<DeployVersionCveVo> tbodyList = deployVersionMapper.searchDeployVersionCveList(searchVo);
                cveIdList.addAll(tbodyList.stream().map(DeployVersionCveVo::getId).collect(Collectors.toList()));
            }
            deployVersionMapper.deleteDeployVersionCveByVersionId(deployVersionVo.getId());
            if (CollectionUtils.isNotEmpty(cveIdList)) {
                deployVersionMapper.deleteDeployVersionCveVulnerabilityByCveIdList(cveIdList);
                deployVersionMapper.deleteDeployVersionCvePackageByCveIdList(cveIdList);
            }
        }

        JSONArray cveArray = paramObj.getJSONArray("cveList");
        List<DeployVersionCveVo> deployVersionCveList = cveArray.toJavaList(DeployVersionCveVo.class);
        for (DeployVersionCveVo deployVersionCveVo : deployVersionCveList) {
            deployVersionCveVo.setVersionId(deployVersionVo.getId());
            deployVersionMapper.insertDeployVersionCve(deployVersionCveVo);
            List<DeployVersionCveVulnerabilityVo> vulnerabilityIds = deployVersionCveVo.getVulnerabilityIds();
            if (CollectionUtils.isNotEmpty(vulnerabilityIds)) {
                for (DeployVersionCveVulnerabilityVo vulnerabilityVo : vulnerabilityIds) {
                    vulnerabilityVo.setCveId(deployVersionCveVo.getId());
                    deployVersionMapper.insertDeployVersionCveVulnerability(vulnerabilityVo);
                }
            }
            List<DeployVersionCvePackageVo> packageList = deployVersionCveVo.getPackageList();
            if (CollectionUtils.isNotEmpty(packageList)) {
                for (DeployVersionCvePackageVo packageVo : packageList) {
                    packageVo.setCveId(deployVersionCveVo.getId());
                    deployVersionMapper.insertDeployVersionCvePackage(packageVo);
                }
            }
        }
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/version/cvelist/save";
    }
}
