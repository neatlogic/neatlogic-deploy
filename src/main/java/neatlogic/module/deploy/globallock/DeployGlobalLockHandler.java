/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.deploy.globallock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.exception.runner.RunnerNotFoundByRunnerMapIdException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.globallock.GlobalLockManager;
import neatlogic.framework.globallock.core.GlobalLockHandlerBase;
import neatlogic.framework.globallock.dao.mapper.GlobalLockMapper;
import neatlogic.framework.integration.authentication.enums.AuthenticateType;
import neatlogic.framework.util.HttpRequestUtil;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.util.TimeUtil;
import neatlogic.framework.util.$;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import neatlogic.framework.autoexec.globallock.AutoexecJobGlobalLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Service
public class DeployGlobalLockHandler extends GlobalLockHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(DeployGlobalLockHandler.class);
    @Resource
    private AutoexecJobGlobalLockService jobLockService;

    /** 在每次获锁事务内，通过作业行锁校验当前作业状态。 */
    @Override
    public void validateAcquisition(GlobalLockVo lock) { jobLockService.validate(lock); }

    /** 作业身份和归属规则由自动化业务层统一提供。 */
    @Override public void validateIdentity(GlobalLockVo existing, GlobalLockVo request) { jobLockService.validateIdentity(existing, request); }

    /** 兼容尚未回填归属字段的历史作业锁。 */
    @Override public boolean ownsLock(GlobalLockVo lock, String ownerId) { return jobLockService.ownsLock(lock, ownerId); }

    /** 提供作业及执行实例展示信息，不要求框架理解这些字段。 */
    @Override public JSONObject getLockIdentity(GlobalLockVo lock) { return jobLockService.identity(lock); }


    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    RunnerMapper runnerMapper;
    @Resource
    GlobalLockMapper globalLockMapper;

    @Override
    public String getHandler() {
        return JobSourceType.DEPLOY.getValue();
    }

    @Override
    public String getHandlerName() {
        return $.t("nmdgl.deploygloballockhandler.gethandlername");
    }

    /** 检查读写冲突，等待原因同时标明申请锁和阻塞它的持有锁 ID。 */
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
                globalLockVo.setWaitReason("your mode is '" + lockMode + "' (lockId=" + globalLockVo.getId()
                        + "), already has '" + lockedGlobalLock.getHandlerParam().getString("lockMode")
                        + "' lock (lockId=" + lockedGlobalLock.getId() + ")");
                return false;
            }
            if (StringUtils.isNotBlank(lockMode) && Objects.equals("write", lockMode) && Objects.equals(lockedGlobalLock.getHandlerParam().getString("lockMode"), lockMode)) {
                globalLockVo.setWaitReason("your mode is '" + lockMode + "' (lockId=" + globalLockVo.getId()
                        + "), already has '" + lockedGlobalLock.getHandlerParam().getString("lockMode")
                        + "' lock (lockId=" + lockedGlobalLock.getId() + ")");
                return false;
            }
        }
        return true;
    }

    @Override
    public JSONObject getLock(JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockVo globalLockVo = new GlobalLockVo(JobSourceType.DEPLOY.getValue(), paramJson.getString("lockOwner") + "/" + paramJson.getString("lockTarget"), paramJson.toJSONString(), paramJson.getString("lockOwnerName"));
        GlobalLockManager.getLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("wait", 0);
        } else {
            jsonObject.put("wait", 1);
            jsonObject.put("message", globalLockVo.getWaitReason());
        }
        jsonObject.put("lockId", globalLockVo.getId());
        return jsonObject;
    }

    @Override
    public JSONObject retryLock(Long lockId, JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        if (lockId == null) {
            throw new ParamIrregularException("lockId");
        }
        //预防如果不存在，需重新insert lock
        GlobalLockVo globalLockVo = new GlobalLockVo(lockId, JobSourceType.DEPLOY.getValue(), paramJson.getString("lockOwner") + "/" + paramJson.getString("lockTarget"), paramJson.toJSONString(), paramJson.getString("lockOwnerName"));
        globalLockVo = GlobalLockManager.retryLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("wait", 0);
            jsonObject.put("lockId", globalLockVo.getId());
        } else {
            jsonObject.put("wait", 1);
            jsonObject.put("message", globalLockVo.getWaitReason());
        }
        return jsonObject;
    }

    @Override
    protected boolean getMyIsCanInsertLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        // 仅同一作业执行实例和 Runner 可复用，PID 可能被不同进程重复使用。
        if (CollectionUtils.isNotEmpty(globalLockVoList)) {
            Optional<GlobalLockVo> globalLockVoOptional = globalLockVoList.stream().filter(g -> Objects.equals(g.getHandlerParam().getString("lockOwner"), globalLockVo.getHandlerParam().getString("lockOwner"))
                    && Objects.equals(g.getHandlerParam().getString("lockTarget"), globalLockVo.getHandlerParam().getString("lockTarget"))
                    && Objects.equals(g.getHandlerParam().getLong("pid"), globalLockVo.getHandlerParam().getLong("pid"))
                    && sameExecution(g, globalLockVo)
                    && g.getIsLock() == 1).findFirst();
            if (globalLockVoOptional.isPresent()) {
                globalLockVo.setId(globalLockVoOptional.get().getId());
                globalLockVo.setIsLock(1);
                return false;
            }
        }
        return true;
    }

    /** 缺少执行归属时不推断为同一实例，避免旧锁被错误复用。 */
    private boolean sameExecution(GlobalLockVo existing, GlobalLockVo request) {
        for (String field : new String[]{"jobId", "execId", "runnerId"}) {
            String value = request.getHandlerParam().getString(field);
            if (value == null || !value.equals(existing.getHandlerParam().getString(field))) return false;
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
            tbody.add(JSON.parseObject(JSON.toJSONString(globalLock)));
        }
        JSONObject result = TableResultUtil.getResult(tbody, globalLockVo);
        for (int i = 0; i < tbody.size(); i++) {
            JSONObject data = tbody.getJSONObject(i);
            data.put("jobId", data.getJSONObject("handlerParam").getString("jobId"));
            AutoexecJobVo jobVo = jobMap.get(data.getJSONObject("handlerParam").getLong("jobId"));
            if (jobVo != null) {
                data.put("jobStatusName", jobVo.getStatusName());
                data.put("jobStatus", jobVo.getStatus());
                data.put("jobName", jobVo.getName());
                data.put("jobId", jobVo.getId().toString());
            }
            if (data.getInteger("isLock") == 1) {
                data.put("lockCostTime", TimeUtil.millisecondsTransferMaxTimeUnit(System.currentTimeMillis() - data.getLong("fcd")));
            }
            data.put("lockTarget", data.getJSONObject("handlerParam").getString("lockTarget"));
        }
        return result;
    }

    @Override
    public void initSearchParam(GlobalLockVo globalLockVo) {
        JSONObject filter = globalLockVo.getKeywordParam();
        if (filter == null || filter.isEmpty()) return;
        Long start = filter.getLong("startTime"), end = filter.getLong("endTime");
        if (start != null && end != null && start > end) throw new ParamIrregularException("startTime/endTime");
        String mode = filter.getString("lockMode");
        if (mode != null && !"read".equals(mode) && !"write".equals(mode)) throw new ParamIrregularException("lockMode");
        List<GlobalLockVo> candidates = filter.getLong("jobId") == null
                ? globalLockMapper.getLockCandidates(getHandler())
                : globalLockMapper.getOwnerLockCandidates(Collections.singletonList(getHandler()), filter.getLong("jobId").toString());
        List<Long> ids = new ArrayList<>();
        for (GlobalLockVo candidate : candidates) {
            try { if (DeployGlobalLockFilter.matches(candidate, filter)) ids.add(candidate.getId()); }
            catch (Exception ex) { logger.error("Filter deploy lock {} failed", candidate.getId(), ex); }
        }
        globalLockVo.setIdList(ids.isEmpty() ? Collections.singletonList(-1L) : ids);
    }

    @Override
    public void myDoNotify(GlobalLockVo globalLockVo, JSONObject paramJson) {
        Long jobId = globalLockVo.getHandlerParam().getLong("jobId");
        Long runnerMapId = globalLockVo.getHandlerParam().getLong("runnerId");
        RunnerMapVo runnerVo = runnerMapper.getRunnerMapByRunnerMapId(runnerMapId);
        if (runnerVo == null) {
            throw new RunnerNotFoundByRunnerMapIdException(runnerMapId);
        }
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("jobId", jobId);
        jsonObj.put("socketFileName", "job" + globalLockVo.getHandlerParam().getLong("execId"));
        JSONObject informParam = new JSONObject();
        informParam.put("action", "globalLockNotify");
        informParam.put("lockId", globalLockVo.getId());
        jsonObj.put("informParam", informParam);
        String url = String.format("%s/api/rest/job/phase/socket/write", runnerVo.getUrl());
        String result = HttpRequestUtil.post(url)
                .setPayload(jsonObj.toJSONString()).setAuthType(AuthenticateType.BUILDIN).setConnectTimeout(5000).setReadTimeout(5000)
                .sendRequest().getError();
        if (StringUtils.isNotBlank(result)) {
            //如果是进程不存在导致没法写入的问题，则跳过，直接解锁
            // socket 不存在也属于通知失败，保留等待锁并继续通知下一个。
            throw new RunnerHttpRequestException(url + ":" + result);
        }

    }

    @Override
    public boolean getIsHasLockByKey(JSONObject param) {
        GlobalLockVo globalLockVo = new GlobalLockVo();
        Long appSystemId = param.getLong("appSystemId");
        Long appModuleId = param.getLong("appModuleId");
        List<String> uuidList = globalLockMapper.getGlobalLockUuidByKey(JobSourceType.DEPLOY.getValue(), String.format("%s/%s/", appSystemId, appModuleId),param.getString("jobId"));
        if (CollectionUtils.isNotEmpty(uuidList)) {
            globalLockVo.setUuidList(uuidList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(r -> r))), ArrayList::new)));
        } else {
            //不存在则没有资源锁
            globalLockVo.setUuidList(Collections.singletonList("-1"));
        }
        globalLockVo.setHandler(JobSourceType.DEPLOY.getValue());
        int count = globalLockMapper.getLockCount(globalLockVo);
        return count > 0;
    }

}
