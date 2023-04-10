/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.deploy.globallock;

import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.dao.mapper.runner.RunnerMapper;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import neatlogic.framework.dto.runner.RunnerMapVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Service
public class DeployGlobalLockHandler extends GlobalLockHandlerBase {
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
            data.put("lockTarget", data.getJSONObject("handlerParam").getString("lockTarget"));
        }
        return result;
    }

    @Override
    public void initSearchParam(GlobalLockVo globalLockVo) {
        JSONObject keywordParam = globalLockVo.getKeywordParam();
        if (MapUtils.isNotEmpty(keywordParam)) {
            if (keywordParam.containsKey("appSystemId")) {
                globalLockVo.setKeyword(keywordParam.getString("appSystemId") + "/");
                if (keywordParam.containsKey("appModuleId")) {
                    globalLockVo.setKeyword(globalLockVo.getKeyword() + keywordParam.getString("appModuleId"));
                }
            }
            if (keywordParam.containsKey("jobId")) {
                List<String> uuidList = globalLockMapper.getGlobalLockUuidByKey(getHandler(), keywordParam.getString("jobId"));
                if (CollectionUtils.isNotEmpty(uuidList)) {
                    globalLockVo.setUuidList(uuidList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(r -> r))), ArrayList::new)));
                } else {
                    //不存在则没有资源锁
                    globalLockVo.setUuidList(Collections.singletonList("-1"));
                }
            }
        }
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
        jsonObj.put("socketFileName", "client" + globalLockVo.getHandlerParam().getLong("pid"));
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
            if(!result.contains("No such file")) {
                throw new RunnerHttpRequestException(url + ":" + result);
            }
        }

    }

    @Override
    public boolean getIsHasLockByKey(String key){
        GlobalLockVo globalLockVo = new GlobalLockVo();
        List<String> uuidList = globalLockMapper.getGlobalLockUuidByKey(JobSourceType.DEPLOY.getValue(), key);
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