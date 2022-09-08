/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.schedule;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;

    @Override
    public String getToken() {
        return "deploy/schedule/list";
    }

    @Override
    public String getName() {
        return "查询定时作业列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", desc = "是否启用"),
            @Param(name = "appSystemId", type = ApiParamType.LONG, desc = "应用id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "页大小")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployScheduleVo[].class, desc = "定时作业列表"),
    })
    @Description(desc = "查询定时作业列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployScheduleVo searchVo = JSONObject.toJavaObject(paramObj, DeployScheduleVo.class);
        List<DeployScheduleVo> tbodyList = new ArrayList<>();
        int rowNum = deployScheduleMapper.getScheduleCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                tbodyList = deployScheduleMapper.getScheduleList(searchVo);
            }
        }
//        int scheduleCount = deployScheduleMapper.getScheduleCount(searchVo);
//        int pipelineScheduleCount = deployScheduleMapper.getPipelineScheduleCount(searchVo);
//        int rowNum = scheduleCount + pipelineScheduleCount;
//        if (rowNum > 0) {
//            searchVo.setRowNum(rowNum);
//            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
//                if (scheduleCount > 0 && pipelineScheduleCount > 0) {
//                    List<Long> idList = deployScheduleMapper.getMergeScheduleIdList(searchVo);
//                    if (CollectionUtils.isNotEmpty(idList)) {
//                        List<DeployScheduleVo> scheduleList = deployScheduleMapper.getScheduleListByIdList(idList);
//                        tbodyList.addAll(scheduleList);
//                        List<DeployScheduleVo> pipelineScheduleList = deployScheduleMapper.getPipelineScheduleByIdList(idList);
//                        tbodyList.addAll(pipelineScheduleList);
//                        //排序
//                        List<DeployScheduleVo> tempList = new ArrayList<>();
//                        for (Long id : idList) {
//                            for (DeployScheduleVo scheduleVo : tbodyList) {
//                                if (Objects.equals(id, scheduleVo.getId())) {
//                                    tempList.add(scheduleVo);
//                                    break;
//                                }
//                            }
//                        }
//                        tbodyList = tempList;
//
//                    }
//                } else if (scheduleCount > 0) {
//                    tbodyList = deployScheduleMapper.getScheduleList(searchVo);
//                } else if (pipelineScheduleCount > 0) {
//                    tbodyList = deployScheduleMapper.getPipelineScheduleList(searchVo);
//                }
//            }
//        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}
