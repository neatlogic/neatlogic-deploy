package codedriver.module.deploy.api.ci;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployBatchJobService;
import codedriver.module.deploy.service.DeployJobService;
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
    DeployPipelineMapper deployPipelineMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Resource
    DeployJobService deployJobService;

    @Resource
    DeployBatchJobService deployBatchJobService;

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
        String revision = paramObj.getString("revision");
        /* todo
            1、根据event和repo确定要触发的ci（是否要根据仓库服务器地址过滤）
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
            // 如果变更的分支没有与之匹配的ci，那么触发所有分支过滤规则为"\"的ci
            if (ciVoList.size() == 0) {
                ciVoList = ciList.stream().filter(o -> Objects.equals(o.getBranchFilter(), "\\")).collect(Collectors.toList());
            }
        }
        if (ciVoList.size() > 0) {
            for (DeployCiVo ci : ciVoList) {
                try {

                } catch (Exception ex) {
                }
            }
        }

        return null;
    }


}
