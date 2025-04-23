/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
