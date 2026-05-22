package com.tranquilai.subscription.dto.response

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class EntitlementResponse @JsonCreator constructor(
    @JsonProperty("allowed") val allowed: Boolean,
    @JsonProperty("remaining") val remaining: Int? = null,
    @JsonProperty("plan") val plan: String,
)

data class UsageResponse @JsonCreator constructor(
    @JsonProperty("allowed") val allowed: Boolean,
    @JsonProperty("used") val used: Int,
    @JsonProperty("limit") val limit: Int?,
    @JsonProperty("remaining") val remaining: Int?,
    @JsonProperty("plan") val plan: String,
)
