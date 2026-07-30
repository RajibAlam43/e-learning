package com.gii.api.model.request.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateCheckoutOrderRequest(
    @NotEmpty List<@Valid CreateCheckoutOrderItemRequest> items) {}
