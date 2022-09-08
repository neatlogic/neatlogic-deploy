/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.schedule;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.deploy.exception.DeployScheduleNameRepeatException;
import codedriver.framework.deploy.exception.DeployScheduleNotFoundException;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;

    @Override
    public String getToken() {
        return "deploy/schedule/save";
    }

    @Override
    public String getName() {
        return "保存定时作业信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "定时作业id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "定时作业名称"),
            @Param(name = "beginTime", type = ApiParamType.LONG, desc = "开始时间"),
            @Param(name = "endTime", type = ApiParamType.LONG, desc = "结束时间"),
            @Param(name = "cron", type = ApiParamType.STRING, isRequired = true, desc = "corn表达式"),
            @Param(name = "isActive", type = ApiParamType.ENUM, isRequired = true, rule = "0,1", desc = "是否激活(0:禁用，1：激活)"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "执行配置信息"),
            @Param(name = "type", type = ApiParamType.ENUM, member = ScheduleType.class, isRequired = true, desc = "作业类型"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "pipelineId", type = ApiParamType.LONG, desc = "流水线id"),
            @Param(name = "pipelineType", type = ApiParamType.ENUM, member = PipelineType.class, desc = "流水线类型")
    })
    @Output({
            @Param(name = "id", type = ApiParamType.STRING, isRequired = true, desc = "定时作业id")
    })
    @Description(desc = "保存定时作业信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String userUuid = UserContext.get().getUserUuid(true);
        DeployScheduleVo scheduleVo = paramObj.toJavaObject(DeployScheduleVo.class);
        Long id = paramObj.getLong("id");
        if (id != null) {
            DeployScheduleVo oldScheduleVo = deployScheduleMapper.getScheduleById(id);
            if (oldScheduleVo == null) {
                throw new DeployScheduleNotFoundException(id);
            }
            scheduleVo.setLcu(userUuid);
            scheduleVo.setUuid(oldScheduleVo.getUuid());
            deployScheduleMapper.updateSchedule(scheduleVo);
        } else {
            scheduleVo.setFcu(userUuid);
            deployScheduleMapper.insertSchedule(scheduleVo);
        }

//        String type = scheduleVo.getType();
//        if (type.equals(ScheduleType.GENERAL.getValue())) {
//            if (id != null) {
//                DeployScheduleVo oldScheduleVo = deployScheduleMapper.getScheduleById(id);
//                if (oldScheduleVo == null) {
//                    throw new DeployScheduleNotFoundException(id);
//                }
//                scheduleVo.setLcu(userUuid);
//                scheduleVo.setUuid(oldScheduleVo.getUuid());
//                deployScheduleMapper.updateSchedule(scheduleVo);
//            } else {
//                scheduleVo.setFcu(userUuid);
//                deployScheduleMapper.insertSchedule(scheduleVo);
//            }
//        } else if (type.equals(ScheduleType.PIPELINE.getValue())) {
//            if (id != null) {
//                DeployScheduleVo oldScheduleVo = deployScheduleMapper.getPipelineScheduleById(id);
//                if (oldScheduleVo == null) {
//                    throw new DeployScheduleNotFoundException(id);
//                }
//                scheduleVo.setLcu(userUuid);
//                scheduleVo.setUuid(oldScheduleVo.getUuid());
//                deployScheduleMapper.updatePipelineSchedule(scheduleVo);
//            } else {
//                scheduleVo.setFcu(userUuid);
//                deployScheduleMapper.insertPipelineSchedule(scheduleVo);
//            }
//        }

        JSONObject resultObj = new JSONObject();
        resultObj.put("id", scheduleVo.getId());
        return resultObj;
    }

    public IValid name() {
        return value -> {
            DeployScheduleVo vo = JSONObject.toJavaObject(value, DeployScheduleVo.class);
            if (deployScheduleMapper.checkScheduleNameIsExists(vo) > 0) {
                return new FieldValidResultVo(new DeployScheduleNameRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
