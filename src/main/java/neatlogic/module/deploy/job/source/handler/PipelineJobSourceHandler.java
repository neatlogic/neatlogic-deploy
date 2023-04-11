/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.job.source.handler;

import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.deploy.constvalue.JobSource;
import org.springframework.stereotype.Component;

import java.util.List;
@Deprecated
@Component
public class PipelineJobSourceHandler implements IAutoexecJobSource {

    @Override
    public String getValue() {
        return JobSource.PIPELINE.getValue();
    }

    @Override
    public String getText() {
        return JobSource.PIPELINE.getText();
    }

    @Override
    public List<AutoexecJobRouteVo> getListByUniqueKeyList(List<String> uniqueKeyList) {
//        if (CollectionUtils.isEmpty(idList)) {
//            return null;
//        }
//        List<ValueTextVo> resultList = new ArrayList<>();
//        List<PipelineVo> list = deployPipelineMapper.getPipelineListByIdList(idList);
//        for (PipelineVo pipelineVo : list) {
//            resultList.add(new ValueTextVo(pipelineVo.getId(), pipelineVo.getName()));
//        }
//        return resultList;
        return null;
    }
}
