package com.interviewme.billing.dto;

import java.util.Map;

public record FeatureCostResponse(
    Map<String, Integer> costs,
    Map<String, Integer> freeQuotas
) {}
