package com.gii.api.model.request.payment;

import com.gii.common.enums.OrderItemType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCheckoutOrderItemRequest(
    @NotNull OrderItemType itemType, UUID courseId, UUID collectionId) {}
