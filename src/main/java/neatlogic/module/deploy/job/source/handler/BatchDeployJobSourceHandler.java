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

package neatlogic.module.deploy.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.deploy.constvalue.JobSource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BatchDeployJobSourceHandler implements IAutoexecJobSource {

    @Override
    public String getValue() {
        return JobSource.BATCHDEPLOY.getValue();
    }

    @Override
    public String getText() {
        return JobSource.BATCHDEPLOY.getText();
    }

    @Override
    public List<AutoexecJobRouteVo> getListByUniqueKeyList(List<String> uniqueKeyList) {
        List<AutoexecJobRouteVo> resultList = new ArrayList<>();

        if (CollectionUtils.isEmpty(uniqueKeyList)) {
            String label = "直接创建";
            resultList.add(new AutoexecJobRouteVo(null, label, new JSONObject()));
        } else {
            String label = "超级流水线";
            for (String str : uniqueKeyList) {
                resultList.add(new AutoexecJobRouteVo(str, label, new JSONObject()));
            }
        }
        return resultList;
    }
}
