package codedriver.module.deploy.api.ci;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.deploy.constvalue.DeployCiActionType;
import codedriver.framework.deploy.constvalue.DeployCiRepoType;
import codedriver.framework.deploy.constvalue.DeployCiTriggerType;
import codedriver.framework.deploy.dto.ci.DeployCiAuditVo;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.*;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.restful.enums.ApiInvokedStatus;
import codedriver.framework.transaction.util.TransactionUtil;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployCiService;
import codedriver.module.deploy.thread.DeployCiAuditSaveThread;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

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

    @Resource
    DeployCiService deployCiService;

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
        logger.info("Gitlab callback triggered, callback param: {}", paramObj.toJSONString());
        DeployCiAuditVo auditVo = new DeployCiAuditVo();
        TransactionStatus transactionStatus = null;
        try {
            /*
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
            auditVo.setCiId(ciId);
            auditVo.setCommitId(commitId);
            auditVo.setParam(paramObj.toJSONString());
            if (StringUtils.isBlank(branchName)) {
                logger.error("Gitlab callback error. Missing branchName in callback params, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiGitlabCallbackBranchNameLostException();
            }
            if (StringUtils.isBlank(commitId)) {
                logger.error("Gitlab callback error. Missing commitId in callback params, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiGitlabCallbackCommitIdLostException();
            }
            DeployCiVo ci = deployCiMapper.getDeployCiById(ciId);
            if (ci == null) {
                logger.error("Gitlab callback error. Deploy ci not found, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiNotFoundException(ciId);
            }
            auditVo.setAction(ci.getAction());
            if (!Objects.equals(ci.getIsActive(), 1)) {
                logger.info("Gitlab callback stop. Deploy ci is not active, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                return null;
            }
            if (StringUtils.isBlank(ci.getTriggerType())) {
                logger.error("Gitlab callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiTriggerTypeLostException();
            }
            if (!DeployCiTriggerType.INSTANT.getValue().equals(ci.getTriggerType()) && StringUtils.isBlank(ci.getTriggerTime())) {
                logger.error("Gitlab callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiTriggerTimeLostException();
            }
            JSONObject versionRule = ci.getVersionRule();
            String versionPrefix = versionRule.getString("versionPrefix");
            String versionRegex = versionRule.getString("versionRegex");
            int useCommitId = versionRule.getInteger("useCommitId") != null ? versionRule.getInteger("useCommitId") : 0;
            String versionName = getVersionName(branchName, versionRegex, versionPrefix, commitId, useCommitId);
            DeployVersionVo deployVersion = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId()));
            UserContext.init(SystemUser.SYSTEM.getUserVo(), SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
            Long jobId = null;
            transactionStatus = TransactionUtil.openTx();
            if (DeployCiActionType.CREATE_JOB.getValue().equals(ci.getAction())) {
                jobId = deployCiService.createJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.GITLAB);
            } else if (DeployCiActionType.CREATE_BATCH_JOB.getValue().equals(ci.getAction())) {
                jobId = deployCiService.createBatchJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.GITLAB);
            }
            TransactionUtil.commitTx(transactionStatus);
            auditVo.setJobId(jobId);
            auditVo.setStatus(ApiInvokedStatus.SUCCEED.getValue());
            auditVo.setResult(jobId);
        } catch (Exception ex) {
            if (transactionStatus != null) {
                TransactionUtil.rollbackTx(transactionStatus);
            }
            auditVo.setStatus(ApiInvokedStatus.FAILED.getValue());
            auditVo.setError(ExceptionUtils.getStackTrace(ex));
        } finally {
            CodeDriverThread thread = new DeployCiAuditSaveThread(auditVo);
            thread.setThreadName("DEPLOY-CI-AUDIT-SAVER-" + auditVo.getId());
            CachedThreadPool.execute(thread);
        }
        return null;
    }

    /**
     * 计算版本号
     *
     * @param branchName    分支名
     * @param versionRegex  分支名截取规则
     * @param versionPrefix 版本前缀
     * @param commitId      commitId
     * @param useCommitId   是否拼接commitId
     * @return
     */
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
            if (StringUtils.isBlank(regex)) {
                throw new DeployCiVersionRegexIllegalException();
            }
            regex = regex.substring(1, regex.lastIndexOf(")"));
        }
        if (StringUtils.isEmpty(regex)) {
            versionName += branchName + "_";
        } else {
            Pattern r = Pattern.compile(regex);
            Matcher m = r.matcher(branchName);
            if (m.find()) {
                versionName += m.group();
            } else {
                versionName += branchName;
            }
        }
        if (StringUtils.isNotBlank(versionPrefix)) {
            versionName = versionPrefix + versionName;
        }
        if (StringUtils.isNotBlank(commitId) && Objects.equals(useCommitId, 1)) {
            if (commitId.length() > 8) {
                versionName += ("_" + commitId.substring(0, 8));
            } else {
                versionName += ("_" + commitId);
            }
        }
        return versionName;
    }

}
