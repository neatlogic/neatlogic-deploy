package codedriver.module.deploy.api.ci;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.constvalue.DeployCiActionType;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployCiNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class CallbackDeployCiGitlabEventApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CallbackDeployCiGitlabEventApi.class);

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Override
    public String getName() {
        return "gitlab webhook回调api";
    }

    @Override
    public String getToken() {
        return "deploy/ci/gitlab/event/callback";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_WITHOUT_ENCRYPTION;
    }

    @Input({
            @Param(name = "ciId", desc = "持续集成配置id", isRequired = true, type = ApiParamType.LONG),
    })
    @Description(desc = "gitlab webhook回调api")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        /* todo
            1、查出ciId对应的配置
            2、获取参数中的commits和ref，确定commitId、分支
            3、根据版本号规则拼接版本号
            4、根据场景决定是否需要生成版本号和新建builNo
            5、根据触发方式决定作业如何执行
            6、记录audit
         */
        Long ciId = paramObj.getLong("ciId");
        JSONArray commits = paramObj.getJSONArray("commits");
        String branchName = paramObj.getString("ref");
        String commitId = StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(commits)) {
            commitId = commits.getJSONObject(0).getString("id");
        }
        if (StringUtils.isNotBlank(branchName)) {
            branchName = branchName.substring(branchName.lastIndexOf('/') + 1);
        }
        DeployCiVo ci = deployCiMapper.getDeployCiById(ciId);
        if (ci == null) {
            logger.error("Gitlab callback error. Deploy ci not found, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
            throw new DeployCiNotFoundException(ciId);
        }
        JSONObject versionRule = ci.getVersionRule();
        String versionPrefix = versionRule.getString("versionPrefix");
        String versionRegex = versionRule.getString("versionRegex");
        int useCommitId = versionRule.getInteger("useCommitId") != null ? versionRule.getInteger("useCommitId") : 0;
        String versionName = getVersionName(branchName, versionRegex, versionPrefix, commitId, useCommitId);
        DeployVersionVo deployVersion = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId()));
        // todo 根据场景判断是否需要新建版本
        if (DeployCiActionType.CREATE_JOB.getValue().equals(ci.getAction())) {

        }
        return null;
    }

    private String getVersionName(String branchName, String versionRegex, String versionPrefix, String commitId, Integer useCommitId) {
        String versionName = StringUtils.EMPTY;
        String regex = StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(versionRegex)) {
            String pattern = "\\(.*\\)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(versionRegex);
            if (m.find()) {
                regex = m.group();
            }
            regex = regex.substring(1, regex.lastIndexOf(")"));
        }
        if (StringUtils.isEmpty(regex)) {
            versionName += branchName + "_";
        } else {
            Pattern r = Pattern.compile(regex);
            Matcher m = r.matcher(branchName);
            if (m.find()) {
                versionName += m.group() + "_";
            } else {
                versionName += branchName + "_";
            }
        }
        if (StringUtils.isNotBlank(versionPrefix)) {
            versionName = versionPrefix + versionName;
        }
        if (StringUtils.isNotBlank(commitId) && Objects.equals(useCommitId, 1)) {
            if (commitId.length() > 8) {
                versionName += commitId.substring(0, 8);
            } else {
                versionName += commitId;
            }
        }
        return versionName;
    }

}
