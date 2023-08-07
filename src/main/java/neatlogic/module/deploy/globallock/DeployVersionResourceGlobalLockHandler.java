/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import neatlogic.framework.globallock.GlobalLockManager;
import neatlogic.framework.globallock.core.GlobalLockHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 制品文件锁
 * 如果同一个runner的同一个资源正在执行打包下载，那么除了读文件外，所有文件操作都不允许
 */
@Service
public class DeployVersionResourceGlobalLockHandler extends GlobalLockHandlerBase {
    @Override
    public String getHandler() {
        return JobSourceType.DEPLOY_VERSION_RESOURCE.getValue();
    }

    @Override
    public String getHandlerName() {
        return "发布-版本中心文件打包下载Lock";
    }

    @Override
    public boolean getIsCanLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        Optional<GlobalLockVo> lockedGlobalLockOptional = globalLockVoList.stream().filter(o -> Objects.equals(o.getIsLock(), 1)).findFirst();
        if (lockedGlobalLockOptional.isPresent()) {
            globalLockVo.setWaitReason("deploy version resource has already locked.the runner url is '" + globalLockVo.getHandlerParam().getString("runnerUrl") + "',the path is '" + globalLockVo.getHandlerParam().getString("path") + "'.");
            return false;
        }
        return true;
    }

    @Override
    public JSONObject getLock(JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockVo globalLockVo = new GlobalLockVo(JobSourceType.DEPLOY_VERSION_RESOURCE.getValue(), paramJson.getString("runnerUrl") + "/" + paramJson.getString("path"), paramJson.toJSONString(), null);
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
        return null;
    }

    @Override
    protected JSONObject myUnLock(Long lockId, JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockManager.unLock(lockId, paramJson);
        jsonObject.put("lockId", lockId);
        return jsonObject;
    }

    @Override
    protected boolean getMyIsCanInsertLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        if (CollectionUtils.isNotEmpty(globalLockVoList)) {
            Optional<GlobalLockVo> globalLockVoOptional = globalLockVoList.stream()
                    .filter(g -> Objects.equals(g.getUuid(), globalLockVo.getUuid()) && Objects.equals(g.getIsLock(), 1)).findFirst();
            return !globalLockVoOptional.isPresent();
        }
        return true;
    }

    @Override
    protected boolean getMyIsBeenLocked(JSONObject paramJson) {
        return GlobalLockManager.getIsBeenLocked(new GlobalLockVo(JobSourceType.DEPLOY_VERSION_RESOURCE.getValue(), paramJson.getString("runnerUrl") + "/" + paramJson.getString("path"), paramJson.toJSONString(), null));
    }
}
