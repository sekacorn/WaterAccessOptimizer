package com.water.llm.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    private String query;
    private String mbtiType;
    private Map<String, Object> context;
}
