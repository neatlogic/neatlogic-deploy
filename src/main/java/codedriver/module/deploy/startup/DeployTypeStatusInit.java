/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.startup;

import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.deploy.constvalue.DeployWhiteType;
import codedriver.framework.startup.StartupBase;
import codedriver.module.deploy.dao.mapper.DeployTypeMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author longrf
 * @date 2022/12/13 15:02
 */

@Service
public class DeployTypeStatusInit extends StartupBase {

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private DeployTypeMapper deployTypeMapper;

    @Override
    public String getName() {
        return "初始化发布工具分类";
    }

    @Override
    public void executeForCurrentTenant() {
        List<Long> hadDeployTypeIdList = autoexecTypeMapper.getTypeIdListByNameList(DeployWhiteType.getValueList());
        if (CollectionUtils.isNotEmpty(hadDeployTypeIdList)) {
            deployTypeMapper.insertDeployActiveList(hadDeployTypeIdList, 1);
        }
    }

    @Override
    public void executeForAllTenant() {
    }

    @Override
    public int sort() {
        return 1;
    }
}
