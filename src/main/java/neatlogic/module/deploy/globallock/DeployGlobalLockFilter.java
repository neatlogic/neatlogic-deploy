package neatlogic.module.deploy.globallock;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import java.util.Objects;

/** 在数据库分页前，对可选筛选条件进行精确匹配。 */
public final class DeployGlobalLockFilter {
    private DeployGlobalLockFilter() { }
    /** 所有已提供条件均须匹配；时间范围包含起止边界。 */
    public static boolean matches(GlobalLockVo lock, JSONObject filter) {
        if (filter == null || filter.isEmpty()) return true;
        JSONObject metadata = lock.getHandlerParam();
        if (metadata == null) metadata = new JSONObject();
        String[] parts = lock.getKey() == null ? new String[0] : lock.getKey().split("/", -1);
        String app = filter.getString("appSystemId"), module = filter.getString("appModuleId");
        if (app != null && (parts.length < 1 || !app.equals(parts[0]))) return false;
        if (module != null && (parts.length < 2 || !module.equals(parts[1]))) return false;
        if (filter.getLong("jobId") != null && !Objects.equals(filter.getLong("jobId"), metadata.getLong("jobId"))) return false;
        if (filter.getString("lockMode") != null && !Objects.equals(filter.getString("lockMode"), metadata.getString("lockMode"))) return false;
        boolean hasError = lock.getNotifyError() != null || lock.getUnlockError() != null;
        if (filter.getBoolean("hasError") != null && filter.getBoolean("hasError") != hasError) return false;
        Long from = filter.getLong("startTime"), to = filter.getLong("endTime");
        return (from == null || lock.getFcd() != null && lock.getFcd().getTime() >= from)
                && (to == null || lock.getFcd() != null && lock.getFcd().getTime() <= to);
    }
}
