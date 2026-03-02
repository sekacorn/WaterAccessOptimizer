package com.water.data.dto;

import com.water.data.service.StorageQuotaService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for storage quota information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaInfoResponse {

    private Double usedMb;
    private Double quotaMb;
    private Double availableMb;
    private Double usagePercentage;

    /**
     * Creates quota info response from service object.
     *
     * @param quotaInfo Storage quota info from service
     * @return QuotaInfoResponse
     */
    public static QuotaInfoResponse fromServiceInfo(StorageQuotaService.StorageQuotaInfo quotaInfo) {
        return new QuotaInfoResponse(
            quotaInfo.getUsedMb(),
            quotaInfo.getQuotaMb(),
            quotaInfo.getAvailableMb(),
            quotaInfo.getUsagePercentage()
        );
    }
}
