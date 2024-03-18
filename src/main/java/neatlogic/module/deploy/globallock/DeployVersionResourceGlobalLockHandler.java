/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.globallock;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import neatlogic.framework.globallock.GlobalLockManager;
import neatlogic.framework.globallock.core.GlobalLockHandlerBase;
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
