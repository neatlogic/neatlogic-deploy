/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
