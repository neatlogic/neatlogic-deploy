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
import neatlogic.framework.deploy.exception.DeployCiVersionRegexIllegalException;
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
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class CallbackDeployCiSvnEventApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CallbackDeployCiSvnEventApi.class);

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
        return "nmdac.callbackdeploycisvneventapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/ci/svn/event/callback";
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
            @Param(name = "ip", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.ip", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "repo", desc = "nmdac.savedeployciapi.input.param.desc.reponame", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "event", desc = "common.event", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "dirsChanged", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.dirschanged", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "revision", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.revision", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "author", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.author", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "date", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.date", help = "格式yyyy-MM-dd hh:mm:ss", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "message", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.message", help = "包含'--nodeploy'时不会执行动作", type = ApiParamType.STRING),
            @Param(name = "added", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.added", help = "多个“,”分割", type = ApiParamType.STRING),
            @Param(name = "modified", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.modified", help = "多个“,”分割", type = ApiParamType.STRING),
            @Param(name = "deleted", desc = "nmdac.callbackdeploycisvneventapi.input.param.desc.deleted", help = "多个“,”分割", type = ApiParamType.STRING)
    })
    @Description(desc = "nmdac.callbackdeploycisvneventapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        logger.info("Svn callback triggered, callback param: {}", paramObj.toJSONString());
        String ip = paramObj.getString("ip");
        String repo = paramObj.getString("repo");
        String event = paramObj.getString("event");
        String dirsChanged = paramObj.getString("dirsChanged");
        String revision = paramObj.getString("revision").trim();
        String message = paramObj.getString("message");
        /*
            1、根据ip、event和repo确定要触发的ci
            2、根据dirsChanged和revision确定版本号
            3、创建作业
         */
        //提交信息中包含"--nodeploy"关键字时,不触发动作
        if (StringUtils.isNotBlank(message) && message.contains("--nodeploy")) {
            return null;
        }
        List<String> ipList = Collections.singletonList(ip);
        if (ip.contains(",")) {
            ipList = Arrays.asList(ip.split(","));
        }
        List<DeployCiVo> ciVoList = new ArrayList<>();
        // 所有属于当前仓库与事件的ci配置
        List<DeployCiVo> ciList = deployCiMapper.getDeployCiListByRepoServerAddressAndRepoNameAndEvent(ipList, repo, event);
        if (ciList.size() > 0) {
            /*
                当有多个svn集成配置，某个commit同时满足多个配置的条件时，使用最长匹配
                比如有3个svn配置，过滤分支分别为：
                配置1：branches/v1.0.0/
                配置2：branches/*
                配置3：branches/v1.0.0/*
                则对 branches/v1.0.0/ 分支的修改代码提交时触发配置3，对branches/v1.0.0/doc/ 分支的修改代码提交时触发配置3，对 branches/3.0.0/ 分支的修改触发配置2
             */
            ciList.sort((o1, o2) -> {
                int i1 = o1.getBranchFilter() == null ? 0 : o1.getBranchFilter().length();
                int i2 = o2.getBranchFilter() == null ? 0 : o2.getBranchFilter().length();
                return i2 - i1;
            });
            List<String> dirList = Arrays.asList(dirsChanged.split(","));
            if (CollectionUtils.isNotEmpty(dirList)) {
                for (String dir : dirList) {
                    for (DeployCiVo civo : ciList) {
                        String filter = civo.getBranchFilter();
                        if (StringUtils.isNotEmpty(filter)) {
                            filter = StringUtils.removeEnd(StringUtils.removeEnd(civo.getBranchFilter(), "\\"), "/");
                            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filter);
                            if (matcher.matches(Paths.get(dir))) {
                                if (ciVoList.stream().anyMatch(o -> Objects.equals(o.getId(), civo.getId()))) {
                                    continue;
                                }
                                ciVoList.add(civo);
                                break;
                            }
                        }
                    }
                }
            }
            // 如果变更的分支没有与之匹配的ci，那么触发所有分支过滤规则为"/"的ci
            if (ciVoList.size() == 0) {
                ciVoList = ciList.stream().filter(o -> Objects.equals(o.getBranchFilter(), "/")).collect(Collectors.toList());
            }
        }
        if (ciVoList.size() > 0) {
            for (DeployCiVo ci : ciVoList) {
                DeployCiAuditVo auditVo = new DeployCiAuditVo(ci.getId(), revision, ci.getAction(), paramObj.toJSONString());
                JSONObject auditData = new JSONObject();
                auditData.put("deployCiAudit", auditVo);
                DeployCiCallbackAuditAppendPostProcessor appendPostProcessor = CrossoverServiceFactory.getApi(DeployCiCallbackAuditAppendPostProcessor.class);
                DeployCiCallbackAuditAppendPreProcessor appendPreProcessor = CrossoverServiceFactory.getApi(DeployCiCallbackAuditAppendPreProcessor.class);
                try {
                    //如果是延迟触发且同一集成触发的情况，n秒内不再创建作业
                    if (Objects.equals(ci.getTriggerType(), DeployCiTriggerType.DELAY.getValue())) {
                        if (ci.getDelayTime() == null) {
                            throw new ParamIrregularException("delayTime");
                        }
                        AutoexecJobVo autoexecJobVo = autoexecJobMapper.getLatestJobByInvokeId(ci.getId());
                        if (autoexecJobVo != null && autoexecJobVo.getFcd() != null && (System.currentTimeMillis() - autoexecJobVo.getFcd().getTime()) <= ci.getDelayTime() * 1000) {
                            auditVo.setCiId(ci.getId());
                            JSONObject result = new JSONObject();
                            result.put("msg", "in " + ci.getDelayTime() + "s , ignored");
                            auditVo.setResult(result);
                            auditVo.setStatus(DeployCiAuditStatus.IGNORED.getValue());
                            AppenderManager.execute(new Event(ci.getName(), System.currentTimeMillis(), auditData, appendPreProcessor, appendPostProcessor, AuditType.DEPLOY_CI_CALLBACK_AUDIT));
                            continue;
                        }
                    }

                    String triggerType = ci.getTriggerType();
                    if (StringUtils.isBlank(triggerType)) {
                        logger.error("Svn callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ci.getId(), paramObj.toJSONString());
                        continue;
                    }
                    String triggerTimeStr = ci.getTriggerTime();
                    if (!DeployCiTriggerType.INSTANT.getValue().equals(ci.getTriggerType()) && StringUtils.isBlank(triggerTimeStr)) {
                        logger.error("Svn callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ci.getId(), paramObj.toJSONString());
                        continue;
                    }
                    JSONObject versionRule = ci.getVersionRule();
                    String versionPrefix = versionRule.getString("versionPrefix");
                    String versionRegex = versionRule.getString("versionRegex");
                    int useCommitId = versionRule.getInteger("useCommitId") != null ? versionRule.getInteger("useCommitId") : 0;
                    String versionName = getVersionName(repo, dirsChanged, revision, versionRegex, versionPrefix, useCommitId);
                    DeployVersionVo deployVersion = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId()));
                    UserContext.init(SystemUser.SYSTEM);
                    UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
                    Long jobId = null;
                    String resultStr =StringUtils.EMPTY;
                    JSONObject result;
                    deployCiMapper.getDeployCiLockById(ci.getId());
                    if (DeployCiActionType.CREATE_JOB.getValue().equals(ci.getAction())) {
                        resultStr = deployCiService.createJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.SVN);
                    } else if (DeployCiActionType.CREATE_BATCH_JOB.getValue().equals(ci.getAction())) {
                        resultStr = deployCiService.createBatchJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.SVN);
                    }
                    result = JSON.parseObject(resultStr);
                    auditVo.setJobId(result.getLong("id"));
                    auditVo.setStatus(DeployCiAuditStatus.SUCCEED.getValue());
                    auditVo.setResult(jobId);
                } catch (Exception ex) {
                    logger.error("Svn callback error. Deploy ci:{} has been ignored, callback params: {}", ci.getId(), paramObj.toJSONString());
                    logger.error(ex.getMessage(), ex);
                    auditVo.setStatus(DeployCiAuditStatus.FAILED.getValue());
                    auditVo.setError(ExceptionUtils.getStackTrace(ex));
                    throw new ApiRuntimeException(ex);
                } finally {
                    AppenderManager.execute(new Event(ci.getName(), System.currentTimeMillis(), auditData, appendPreProcessor, appendPostProcessor, AuditType.DEPLOY_CI_CALLBACK_AUDIT));
                }
            }
        }

        return null;
    }

    /**
     * 计算版本号
     *
     * @param repoName      仓库名称
     * @param dirsChanged   受影响的目录
     * @param revision      提交id
     * @param versionRegex  分支名截取规则
     * @param versionPrefix 版本前缀
     * @param useCommitId   是否拼接commitId
     * @return
     */
    private String getVersionName(String repoName, String dirsChanged, String revision, String versionRegex, String versionPrefix, Integer useCommitId) {
        String[] dirsChanges = dirsChanged.split(",");
        String branchName = "";
        if (dirsChanges.length == 1) {
            branchName = dirsChanged;
        } else if (dirsChanges.length > 1) {
            branchName = getMaxSubString(dirsChanges);
        }
        if (StringUtils.isBlank(branchName) || "/".equals(branchName)) {
            branchName = repoName.substring(repoName.lastIndexOf("/") + 1);
        } else {
            if (branchName.endsWith("/")) {
                branchName = branchName.substring(0, branchName.length() - 1);
            }
            branchName = branchName.substring(branchName.lastIndexOf("/") + 1);
        }

        String versionName = StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(versionPrefix)) {
            versionName += versionPrefix + "_";
        }
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
            versionName += branchName;
        } else {
            Pattern r = Pattern.compile(regex);
            Matcher m = r.matcher(dirsChanged);
            if (m.find()) {
                versionName += m.group();
            } else {
                versionName += branchName;
            }
        }
        if (Objects.equals(useCommitId, 1)) {
            versionName += ("_" + revision);
        }
        return versionName;
    }

    /**
     * 获取多个文件目录下最后相同的目录
     *
     * @param args such as:{"/a/b/c","/a/b/","/a/b/d"} getMaxSubString(a,b)=>"/a/b/"
     * @return
     */
    private String getMaxSubString(String[] args) {
        String common = "";
        if (args == null || args.length == 0) {
            return null;
        }
        String[] shortArr = args[0].split("/"), longArr = args[0].split("/");
        int shortSize = shortArr.length, longSize = longArr.length;
        for (int i = 0; i < args.length; i++) {
            String[] _this = args[i].split("/");
            if (_this.length >= longSize && !_this.toString().equals(shortArr.toString())) {
                longSize = _this.length;
                longArr = _this;
            }
            if (_this.length <= shortSize && !_this.toString().equals(longArr.toString())) {
                shortSize = _this.length;
                shortArr = _this;
            }
        }
        if (shortSize == 0) {
            return null;
        }
        for (int i = 0; i < shortArr.length; i++) {
            if (shortArr[i].equals(longArr[i])) {
                common = shortArr[i];
            }
        }
        return common;
    }


}
