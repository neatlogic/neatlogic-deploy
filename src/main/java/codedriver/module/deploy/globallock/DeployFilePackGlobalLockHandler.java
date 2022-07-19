/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.globallock;

import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.dto.globallock.GlobalLockVo;
import codedriver.framework.globallock.GlobalLockManager;
import codedriver.framework.globallock.core.GlobalLockHandlerBase;
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
public class DeployFilePackGlobalLockHandler extends GlobalLockHandlerBase {
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
        return null;
    }

    @Override
    protected JSONObject myCancelLock(Long lockId, JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockManager.cancelLock(lockId, paramJson);
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

}
