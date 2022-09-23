/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.thread;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.deploy.dto.ci.DeployCiAuditVo;
import codedriver.framework.util.AuditUtil;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeployCiAuditSaveThread extends CodeDriverThread {

    private static DeployCiMapper deployCiMapper;

    @Autowired
    public void setDeployCiMapper(DeployCiMapper _deployCiMapper) {
        deployCiMapper = _deployCiMapper;
    }

    private DeployCiAuditVo deployCiAuditVo;

    public DeployCiAuditSaveThread(DeployCiAuditVo _deployCiAuditVo) {
        super("DEPLOY-CI-AUDIT-SAVER");
        deployCiAuditVo = _deployCiAuditVo;
    }

    public DeployCiAuditSaveThread() {
        super("DEPLOY-CI-AUDIT-SAVER");
    }

    @Override
    protected void execute() {
        if (deployCiAuditVo != null) {
            AuditUtil.saveAuditDetail(deployCiAuditVo, "deploy_ci_audit");
            deployCiMapper.insertDeployCiAudit(deployCiAuditVo);
        }
    }

}
