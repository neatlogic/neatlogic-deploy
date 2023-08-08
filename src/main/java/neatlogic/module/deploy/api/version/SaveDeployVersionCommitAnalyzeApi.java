package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_MODIFY;
import neatlogic.framework.deploy.dto.codehub.RepositoryServiceVo;
import neatlogic.framework.deploy.dto.codehub.RepositoryVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.DeployVersionNotFoundException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@AuthAction(action = DEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveDeployVersionCommitAnalyzeApi extends PrivateApiComponentBase {
    static Pattern pattern =  Pattern.compile("(https|http?://[^/]+/)([^.]+)");


    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "保存版本分析数据";
    }

    @Override
    public String getToken() {
        return "deploy/version/commit/analyze/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "repo", desc = "项目源地址", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "user", desc = "账号", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "password", desc = "密码", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "type", desc = "类型", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "appSystemId", desc = "密码", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "appModuleId", desc = "密码", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "version", desc = "版本", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "commitList", desc = "提交列表", isRequired = true, type = ApiParamType.JSONARRAY),
            @Param(name = "fileAddCount", desc = "文件增加数", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "fileDeleteCount", desc = "文件删除数", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "fileModifyCount", desc = "文件修改数", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "lineDeleteCount", desc = "代码行减少数", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "lineAddCount", desc = "代码行增加数", isRequired = true, type = ApiParamType.INTEGER)
    })
    @Output({
    })
    @Description(desc = "保存版本分析数据")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        //更新版本commit统计数据
        DeployVersionVo versionVo = JSONObject.toJavaObject(paramObj, DeployVersionVo.class);
        DeployVersionVo versionTmp = deployVersionMapper.getDeployVersionBySystemIdAndModuleIdAndVersion(versionVo.getAppSystemId(), versionVo.getAppModuleId(), versionVo.getVersion());
        if (versionTmp == null) {
            throw new DeployVersionNotFoundException(versionVo.getAppSystemId().toString(), versionVo.getAppModuleId().toString(), versionVo.getVersion());
        }
        versionVo.setId(versionTmp.getId());
        deployVersionMapper.updateDeployVersionAnalyzeCount(versionVo);
        //跟新仓库服务和仓库
        String repo = paramObj.getString("repo");
        Matcher matcher = pattern.matcher(repo);
        RepositoryVo repositoryVo = null;
        if (!matcher.find()) {
            throw new ParamIrregularException("repo");
        }
        String repoServiceAddress = matcher.group(1);
        String repoAddress = matcher.group(2);
        if (StringUtils.isBlank(repoServiceAddress) || StringUtils.isBlank(repoAddress)) {
            throw new ParamIrregularException("repo");
        }
        //更新仓库服务
        RepositoryServiceVo repositoryServiceVo = deployVersionMapper.getRepositoryServiceByAddress(repoServiceAddress);
        if (repositoryServiceVo == null) {
            repositoryServiceVo = new RepositoryServiceVo();
            repositoryServiceVo.setName(repoAddress);
            repositoryServiceVo.setType(paramObj.getString("type"));
            repositoryServiceVo.setAddress(repoServiceAddress);
            repositoryServiceVo.setUsername(paramObj.getString("user"));
            repositoryServiceVo.setPassword(paramObj.getString("password"));
            repositoryServiceVo.setStatus("none");
            deployVersionMapper.insertRepositoryService(repositoryServiceVo);
        }
        //更新仓库
        repositoryVo = deployVersionMapper.getRepositoryByAppModuleId(versionVo.getAppModuleId());
        if (repositoryVo == null) {
            repositoryVo = new RepositoryVo();
            repositoryVo.setRepoServiceId(repositoryServiceVo.getId());
            repositoryVo.setName(repoAddress);
            repositoryVo.setAddress(repoAddress);
            repositoryVo.setCreateMode("import");
            repositoryVo.setSyncStatus("none");
            repositoryVo.setAppModuleId(versionVo.getAppModuleId());
            deployVersionMapper.insertRepository(repositoryVo);
        }

        //更新 版本与需求关联&&版本与commit关联
        JSONArray commitArray = paramObj.getJSONArray("commitList");
        if (CollectionUtils.isNotEmpty(commitArray)) {
            for (int i = 0; i < commitArray.size(); i++) {
                JSONObject commit = commitArray.getJSONObject(i);
                String issueNo = commit.getString("issueNo");
                deployVersionMapper.insertDeployVersionIssue(versionTmp.getId(), issueNo);
                deployVersionMapper.insertDeployVersionCommit(versionTmp.getId(), commit.getString("commitId"), repositoryVo.getId());
            }
        }
        //
        return null;
    }
}
