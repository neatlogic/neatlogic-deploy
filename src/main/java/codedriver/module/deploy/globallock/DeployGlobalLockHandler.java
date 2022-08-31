/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.globallock;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.dto.globallock.GlobalLockVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.globallock.GlobalLockManager;
import codedriver.framework.globallock.core.GlobalLockHandlerBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.framework.util.TimeUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeployGlobalLockHandler extends GlobalLockHandlerBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getHandler() {
        return JobSourceType.DEPLOY.getValue();
    }

    @Override
    public String getHandlerName() {
        return "发布 fileLock";
    }

    @Override
    public boolean getIsCanLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        String lockMode = globalLockVo.getHandlerParam().getString("lockMode");
        if (StringUtils.isBlank(lockMode)) {
            throw new ParamIrregularException("lockMode");
        }
        Optional<GlobalLockVo> lockedGlobalLockOptional = globalLockVoList.stream().filter(o -> Objects.equals(o.getIsLock(), 1)).findFirst();
        if (lockedGlobalLockOptional.isPresent()) {
            GlobalLockVo lockedGlobalLock = lockedGlobalLockOptional.get();
            if (!Objects.equals(lockedGlobalLock.getHandlerParam().getString("lockMode"), lockMode)) {
                globalLockVo.setWaitReason("your mode is '" + lockMode + "',already has '" + lockedGlobalLock.getHandlerParam().getString("lockMode") + "' lock");
                return false;
            }
            if (StringUtils.isNotBlank(lockMode) && Objects.equals("write", lockMode) && Objects.equals(lockedGlobalLock.getHandlerParam().getString("lockMode"), lockMode)) {
                globalLockVo.setWaitReason("your mode is '" + lockMode + "',already has '" + lockedGlobalLock.getHandlerParam().getString("lockMode") + "' lock");
                return false;
            }
        }
        return true;
    }

    @Override
    public JSONObject getLock(JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockVo globalLockVo = new GlobalLockVo(JobSourceType.DEPLOY.getValue(), paramJson.getString("jobId") + "/" + paramJson.getString("runnerId") + "/" + paramJson.getString("lockOwner") + "/" + paramJson.getString("lockTarget"), paramJson.toJSONString(), paramJson.getString("lockOwnerName"));
        GlobalLockManager.getLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("lockId", globalLockVo.getId());
            jsonObject.put("wait", 0);
        } else {
            jsonObject.put("wait", 1);
            jsonObject.put("message", globalLockVo.getWaitReason());
        }
        return jsonObject;
    }

    @Override
    public JSONObject retryLock(Long lockId, JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        if (lockId == null) {
            throw new ParamIrregularException("lockId");
        }
        //预防如果不存在，需重新insert lock
        String jobId = paramJson.getString("jobId");
        GlobalLockVo globalLockVo = new GlobalLockVo(lockId, JobSourceType.DEPLOY.getValue(), paramJson.getString("lockOwner") + "/" + paramJson.getString("lockTarget"), paramJson.toJSONString(), paramJson.getString("lockOwnerName"));
        GlobalLockManager.retryLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("lockId", globalLockVo.getId());
        } else {
            throw new ApiRuntimeException(globalLockVo.getWaitReason());
        }
        return jsonObject;
    }

    @Override
    protected boolean getMyIsCanInsertLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        //如果uuid存在则共享lockId
        if (CollectionUtils.isNotEmpty(globalLockVoList)) {
            Optional<GlobalLockVo> globalLockVoOptional = globalLockVoList.stream().filter(g -> Objects.equals(g.getHandlerParam().getString("lockOwner"), globalLockVo.getHandlerParam().getString("lockOwner"))
                    && Objects.equals(g.getHandlerParam().getString("lockTarget"), globalLockVo.getHandlerParam().getString("lockTarget"))
                    && Objects.equals(g.getHandlerParam().getLong("pid"), globalLockVo.getHandlerParam().getLong("pid"))
                    && g.getIsLock() == 1).findFirst();
            if (globalLockVoOptional.isPresent()) {
                globalLockVo.setId(globalLockVoOptional.get().getId());
                globalLockVo.setIsLock(1);
                return false;
            }
        }
        return true;
    }

    @Override
    public JSONObject getSearchResult(List<GlobalLockVo> globalLockList, GlobalLockVo globalLockVo) {
        JSONArray tbody = new JSONArray();
        if (CollectionUtils.isEmpty(globalLockList)) {
            return TableResultUtil.getResult(tbody, globalLockVo);
        }
        List<Long> jobIdList = globalLockList.stream().map(o -> o.getHandlerParam().getLong("jobId")).collect(Collectors.toList());
        List<AutoexecJobVo> jobVoList = autoexecJobMapper.getJobListByIdList(jobIdList);
        Map<Long, AutoexecJobVo> jobMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(jobVoList)) {
            jobMap = jobVoList.stream().collect(Collectors.toMap(AutoexecJobVo::getId, o -> o));
        }
        for (GlobalLockVo globalLock : globalLockList) {
            tbody.add(JSONObject.parseObject(JSONObject.toJSONString(globalLock)));
        }
        JSONObject result = TableResultUtil.getResult(tbody, globalLockVo);
        for (int i = 0; i < tbody.size(); i++) {
            JSONObject data = tbody.getJSONObject(i);
            AutoexecJobVo jobVo = jobMap.get(data.getJSONObject("handlerParam").getLong("jobId"));
            data.put("jobStatusName", jobVo.getStatusName());
            data.put("jobStatus", jobVo.getStatus());
            data.put("jobName", jobVo.getName());
            data.put("jobId", jobVo.getId());
            if (data.getInteger("isLock") == 1) {
                data.put("lockCostTime", TimeUtil.millisecondsTransferMaxTimeUnit(System.currentTimeMillis() - data.getLong("fcd")));
            }
            data.put("lockTarget",data.getJSONObject("handlerParam").getString("lockTarget"));
        }
        return result;
    }

}
