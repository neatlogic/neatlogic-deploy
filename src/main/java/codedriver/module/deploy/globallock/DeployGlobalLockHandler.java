/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.globallock;

import codedriver.framework.deploy.constvalue.JobSourceType;
import codedriver.framework.dto.globallock.GlobalLockVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.globallock.GlobalLockManager;
import codedriver.framework.globallock.core.GlobalLockHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DeployGlobalLockHandler extends GlobalLockHandlerBase {
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
        if(StringUtils.isBlank(lockMode)){
            throw new ParamIrregularException("lockMode");
        }
        Optional<GlobalLockVo> lockedGlobalLockOptional = globalLockVoList.stream().filter(o-> Objects.equals(o.getIsLock(),1)).findFirst();
        if(lockedGlobalLockOptional.isPresent()) {
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
        GlobalLockVo globalLockVo = new GlobalLockVo(JobSourceType.DEPLOY.getValue(),paramJson.getString("lockOwner")+"/"+paramJson.getString("lockTarget"),paramJson.toJSONString(),paramJson.getString("lockOwnerName"));
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
        if(lockId == null){
            throw new ParamIrregularException("lockId");
        }
        //预防如果不存在，需重新insert lock
        String jobId = paramJson.getString("jobId");
        GlobalLockVo globalLockVo = new GlobalLockVo(lockId, JobSourceType.DEPLOY.getValue(),paramJson.getString("lockOwner")+"/"+paramJson.getString("lockTarget"),paramJson.toJSONString(),paramJson.getString("lockOwnerName"));
        GlobalLockManager.retryLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("lockId", globalLockVo.getId());
        } else {
            throw new ApiRuntimeException(globalLockVo.getWaitReason());
        }
        return jsonObject;
    }

}
