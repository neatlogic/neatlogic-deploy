package neatlogic.module.deploy.api.version;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVersionVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployPipelineJobTemplateVersionApi extends PrivateApiComponentBase {

    @Resource
    DeployPipelineMapper deployPipelineMapper;

    @Override
    public String getName() {
        return "nmdav.listdeploypipelinejobtemplateversionapi.getname";
    }

    @Override
    public String getToken() {
        return "deploy/pipeline/job/template/version/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobTemplateIdList", desc = "nmdav.listdeploypipelinejobtemplateversionapi.input.param.desc.jobtemplateidlist", isRequired = true, type = ApiParamType.JSONARRAY),
            @Param(name = "version", desc = "nmdav.listdeploypipelinejobtemplateversionapi.input.param.desc.version", isRequired = true, type = ApiParamType. STRING),
    })
    @Description(desc = "nmdav.listdeploypipelinejobtemplateversionapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<Long> jobTemplateIdList = paramObj.getJSONArray("jobTemplateIdList").toJavaList(Long.class);
        String version = paramObj.getString("version");
        List<PipelineJobTemplateVo>  jobTemplateVos =  deployPipelineMapper.getJobTemplateListByIdList(jobTemplateIdList);
        Map<String,Long> jobTemplateIdVersionIdMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(jobTemplateVos)) {
            List<PipelineJobTemplateVersionVo> jobTemplateVersionVos = deployPipelineMapper.getVersionByJobTemplateIdListAndVersionName(jobTemplateIdList,version);
            if(CollectionUtils.isNotEmpty(jobTemplateVersionVos)) {
                jobTemplateIdVersionIdMap = jobTemplateVersionVos.stream().collect(Collectors.toMap(o->o.getId().toString(), PipelineJobTemplateVersionVo::getVersionId));
            }
//            for (PipelineJobTemplateVo pipelineJobTemplateVo : jobTemplateVos) {
//                if(!jobTemplateIdVersionIdMap.containsKey(pipelineJobTemplateVo.getId().toString())) {
//                    IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
//                    AppSystemVo appSystemVo  = appSystemMapper.getAppSystemById(pipelineJobTemplateVo.getAppSystemId());
//                    if(appSystemVo == null) {
//                        throw new AppSystemNotFoundException(pipelineJobTemplateVo.getAppSystemId());
//                    }
//                    AppModuleVo appModuleVo = appSystemMapper.getAppModuleById(pipelineJobTemplateVo.getAppModuleId());
//                    if(appModuleVo == null) {
//                        throw new AppModuleNotFoundException(pipelineJobTemplateVo.getAppModuleId());
//                    }
//                    throw new DeployVersionNotFoundException(appSystemVo.getAbbrName(),appModuleVo.getAbbrName());
//                }
//            }
        }
        return jobTemplateIdVersionIdMap;
    }
}
