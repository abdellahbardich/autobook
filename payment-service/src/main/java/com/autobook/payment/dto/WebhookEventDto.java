package com.autobook.payment.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventDto {

    @JsonProperty("meta")
    private Map<String, Object> meta;

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("event_name")
    private String eventName;
}