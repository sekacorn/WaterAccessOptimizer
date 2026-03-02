package com.water.llm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    private String response;
    private String mbtiType;
    private String tone;
    private Long processingTimeMs;
}
