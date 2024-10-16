package neatlogic.module.deploy.api.ci;

import com.alibaba.fastjson.JSON;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.*;
import neatlogic.framework.deploy.dto.ci.DeployCiAuditVo;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.framework.deploy.dto.version.DeployVersionVo;
import neatlogic.framework.deploy.exception.*;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.file.core.Event;
import neatlogic.framework.file.core.appender.AppenderManager;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.audit.DeployCiCallbackAuditAppendPostProcessor;
import neatlogic.module.deploy.audit.DeployCiCallbackAuditAppendPreProcessor;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import neatlogic.module.deploy.dao.mapper.DeployVersionMapper;
import neatlogic.module.deploy.service.DeployCiService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class CallbackDeployCiGitlabEventApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CallbackDeployCiGitlabEventApi.class);

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployCiService deployCiService;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "nmdac.callbackdeploycigitlabeventapi.getname";
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
            @Param(name = "ciId", desc = "term.deploy.ciid", isRequired = true, type = ApiParamType.LONG),
    })
    @Description(desc = "nmdac.callbackdeploycigitlabeventapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        logger.info("Gitlab callback triggered, callback param: {}", paramObj.toJSONString());
        DeployCiAuditVo auditVo = new DeployCiAuditVo();
        String ciName = null;
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
            DeployCiVo ci = deployCiMapper.getDeployCiLockById(ciId);
            if (ci == null) {
                logger.error("Gitlab callback error. Deploy ci not found, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiNotFoundException(ciId);
            }
            ciName = ci.getName();
            auditVo.setAction(ci.getAction());
            //如果是延迟触发且同一集成触发的情况，n秒内不再创建作业
            if (Objects.equals(ci.getTriggerType(), DeployCiTriggerType.DELAY.getValue())) {
                if (ci.getDelayTime() == null) {
                    throw new ParamIrregularException("delayTime");
                }
                AutoexecJobVo autoexecJobVo = autoexecJobMapper.getLatestJobByInvokeId(ci.getId());
                if (autoexecJobVo != null && autoexecJobVo.getFcd() != null && (System.currentTimeMillis() - autoexecJobVo.getFcd().getTime()) <= ci.getDelayTime() * 1000) {
                    auditVo.setStatus(DeployCiAuditStatus.IGNORED.getValue());
                    JSONObject result = new JSONObject();
                    result.put("msg", "in " + ci.getDelayTime() + "s , ignored");
                    return result;
                }
            }
            if (!Objects.equals(ci.getIsActive(), 1)) {
                logger.info("Gitlab callback stop. Deploy ci is not active, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                return null;
            }
            if (StringUtils.isBlank(ci.getTriggerType())) {
                logger.error("Gitlab callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiTriggerTypeLostException();
            }
            if (Arrays.asList(DeployCiTriggerType.AUTO.getValue(), DeployCiTriggerType.MANUAL.getValue()).contains(ci.getTriggerType()) && StringUtils.isBlank(ci.getTriggerTime())) {
                logger.error("Gitlab callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiTriggerTimeLostException();
            }
            JSONObject versionRule = ci.getVersionRule();
            String versionPrefix = versionRule.getString("versionPrefix");
            String versionRegex = versionRule.getString("versionRegex");
            int useCommitId = versionRule.getInteger("useCommitId") != null ? versionRule.getInteger("useCommitId") : 0;
            String versionName = getVersionName(branchName, versionRegex, versionPrefix, commitId, useCommitId);
            DeployVersionVo deployVersion = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId()));
            UserContext.init(SystemUser.SYSTEM);
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
            String resultStr =StringUtils.EMPTY;
            JSONObject result;
            if (DeployCiActionType.CREATE_JOB.getValue().equals(ci.getAction())) {
                resultStr = deployCiService.createJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.GITLAB);
            } else if (DeployCiActionType.CREATE_BATCH_JOB.getValue().equals(ci.getAction())) {
                resultStr = deployCiService.createBatchJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.GITLAB);
            }
            result = JSON.parseObject(resultStr);
            auditVo.setJobId(result.getLong("id"));
            auditVo.setStatus(DeployCiAuditStatus.SUCCEED.getValue());
            auditVo.setResult(result);
        } catch (Exception ex) {
            auditVo.setStatus(DeployCiAuditStatus.FAILED.getValue());
            auditVo.setError(ExceptionUtils.getStackTrace(ex));
            throw new ApiRuntimeException(ex);
        } finally {
            if (StringUtils.isNotBlank(ciName)) {
                JSONObject auditData = new JSONObject();
                auditData.put("deployCiAudit", auditVo);
                DeployCiCallbackAuditAppendPostProcessor appendPostProcessor = CrossoverServiceFactory.getApi(DeployCiCallbackAuditAppendPostProcessor.class);
                DeployCiCallbackAuditAppendPreProcessor appendPreProcessor = CrossoverServiceFactory.getApi(DeployCiCallbackAuditAppendPreProcessor.class);
                AppenderManager.execute(new Event(ciName, System.currentTimeMillis(), auditData, appendPreProcessor, appendPostProcessor, AuditType.DEPLOY_CI_CALLBACK_AUDIT));
            }
        }
        return auditVo;
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
