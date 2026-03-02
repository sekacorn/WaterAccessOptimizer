package com.water.data.dto;

import lombok.Data;

/**
 * Request DTO for creating a new risk assessment.
 */
@Data
public class CreateAssessmentRequest {
    private String name;           // Optional, defaults to "Risk Assessment"
    private String description;    // Optional
    private Boolean isPublic;      // Optional, defaults to false

    public String getName() {
        return name != null && !name.trim().isEmpty() ? name : "Risk Assessment";
    }

    public Boolean getIsPublic() {
        return isPublic != null ? isPublic : false;
    }
}
