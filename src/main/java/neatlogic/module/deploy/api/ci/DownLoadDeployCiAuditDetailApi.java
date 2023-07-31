/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.deploy.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.crossover.IFileCrossoverService;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.exception.file.FilePathIllegalException;
import neatlogic.framework.file.dto.AuditFilePathVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownLoadDeployCiAuditDetailApi extends PrivateBinaryStreamApiComponentBase {

	@Override
	public String getToken() {
		return "deploy/ci/audit/detail/download";
	}

	@Override
	public String getName() {
		return "nmdac.downloaddeployciauditdetailapi.getname";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
			@Param(name = "filePath", type = ApiParamType.STRING, desc = "common.filepath", isRequired = true)
	})
	@Output({})
	@Description(desc = "nmdac.downloaddeployciauditdetailapi.getname")
	@Override
	public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String filePath = paramObj.getString("filePath");
		if (!filePath.contains("deploycicallbackaudit")) {
			throw new FilePathIllegalException(filePath);
		}
		AuditFilePathVo auditFilePathVo = new AuditFilePathVo(filePath);
		IFileCrossoverService fileCrossoverService = CrossoverServiceFactory.getApi(IFileCrossoverService.class);
		if (Objects.equals(auditFilePathVo.getServerId(), Config.SCHEDULE_SERVER_ID)) {
			fileCrossoverService.downloadLocalFile(auditFilePathVo.getPath(), auditFilePathVo.getStartIndex(), auditFilePathVo.getOffset(), response);
		} else {
			fileCrossoverService.downloadRemoteFile(paramObj, auditFilePathVo.getServerId(), request, response);
		}
		return null;
	}

}
