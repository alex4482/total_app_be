package com.work.total_app.models.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TenantBulkDeleteResultDto {
    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<FailedDeletion> failures = new ArrayList<>();

    @Getter
    @Setter
    public static class FailedDeletion {
        private Long tenantId;
        private String tenantName;
        private String reason;

        public FailedDeletion(Long tenantId, String tenantName, String reason) {
            this.tenantId = tenantId;
            this.tenantName = tenantName;
            this.reason = reason;

        }
    }
}