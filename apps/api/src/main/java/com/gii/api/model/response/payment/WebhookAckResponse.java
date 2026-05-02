package com.gii.api.model.response.payment;

import lombok.Builder;

@Builder
public record WebhookAckResponse(
    Boolean acknowledged,
    String message, // e.g., "Webhook received and queued for processing"
    String webhookId, // For provider reference
    Long processingDelayMs // Expected delay before processing
) {}
