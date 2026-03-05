package br.com.imovelconecte.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HubSpotWebhookPayload {

    private Long subscriptionId;
    private Long portalId;
    private Long occurredAt;
    private String subscriptionType;
    private Long attemptNumber;
    private Long objectId;
    private String propertyName;
    private String propertyValue;
    private String changeSource;

    @JsonProperty("subscriptionType")
    public void setSubscriptionType(String type) {
        this.subscriptionType = type;
    }

    @JsonProperty("eventType")
    public void setEventType(String type) {
        this.subscriptionType = type; // HubSpot v3 usa eventType
    }

    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("appId")
    private Long appId;

    @JsonProperty("objectType")
    private String objectType;

    @JsonProperty("propertyValues")
    private List<PropertyChange> propertyValues;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyChange {
        private String name;
        private String value;
    }
}
