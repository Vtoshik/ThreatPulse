package com.threatpulse.analyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ThreatAnalysis {
    private String summary;
    private String severity;           // CRITICAL | HIGH | MEDIUM | LOW | INFO
    private String category;           // RCE | XSS | SQLI | DATA_BREACH | OTHER
    @JsonProperty("affected_technologies")
    private List<String> affectedTechnologies;
    @JsonProperty("recommended_action")
    private String recommendedAction;
}