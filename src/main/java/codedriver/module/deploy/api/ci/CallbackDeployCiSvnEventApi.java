package codedriver.module.deploy.api.ci;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.deploy.constvalue.DeployCiActionType;
import codedriver.framework.deploy.constvalue.DeployCiRepoType;
import codedriver.framework.deploy.constvalue.DeployCiTriggerType;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployCiVersionRegexIllegalException;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployCiService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.OPERATE)
public class CallbackDeployCiSvnEventApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CallbackDeployCiSvnEventApi.class);

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployCiService deployCiService;

    @Override
    public String getName() {
        return "svn hook回调api";
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
            @Param(name = "repo", desc = "仓库名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "event", desc = "事件", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "dirsChanged", desc = "受影响的目录", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "revision", desc = "提交id", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "author", desc = "提交者", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "date", desc = "提交日期，格式yyyy-MM-dd hh:mm:ss", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "message", desc = "提交信息，包含'--nodeploy'时不会执行动作", type = ApiParamType.STRING),
            @Param(name = "added", desc = "本次提交新增的文件，多个“,”分割", type = ApiParamType.STRING),
            @Param(name = "modified", desc = "本次提交修改的文件，多个“,”分割", type = ApiParamType.STRING),
            @Param(name = "deleted", desc = "本次提交删除的文件，多个“,”分割", type = ApiParamType.STRING)
    })
    @Description(desc = "svn hook回调api")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String repo = paramObj.getString("repo");
        String event = paramObj.getString("event");
        String dirsChanged = paramObj.getString("dirsChanged");
        String revision = paramObj.getString("revision").trim();
        /*
            1、根据event和repo确定要触发的ci（todo 是否要根据仓库服务器地址过滤）
            2、根据dirsChanged和revision确定版本号
            3、创建作业
         */
        List<DeployCiVo> ciVoList = new ArrayList<>();
        // 所有属于当前仓库与事件的ci配置
        List<DeployCiVo> ciList = deployCiMapper.getDeployCiListByRepoNameAndEvent(repo, event);
        if (ciList.size() > 0) {
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
                try {
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
                    UserContext.init(SystemUser.SYSTEM.getUserVo(), SystemUser.SYSTEM.getTimezone());
                    UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
                    if (DeployCiActionType.CREATE_JOB.getValue().equals(ci.getAction())) {
                        deployCiService.createJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.SVN);
                    } else if (DeployCiActionType.CREATE_BATCH_JOB.getValue().equals(ci.getAction())) {
                        deployCiService.createBatchJobForVCSCallback(paramObj, ci, versionName, deployVersion, DeployCiRepoType.SVN);
                    }
                } catch (Exception ex) {
                    logger.error("Svn callback error. Deploy ci:{} has been ignored, callback params: {}", ci.getId(), paramObj.toJSONString());
                    logger.error(ex.getMessage(), ex);
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
