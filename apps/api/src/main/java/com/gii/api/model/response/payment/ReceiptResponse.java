package com.gii.api.model.response.payment;

import com.gii.common.enums.OrderProvider;
import com.gii.common.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record ReceiptResponse(
        // Receipt metadata
        UUID orderId,
        String receiptNumber,  // Generated receipt ID (e.g., INV-20260429-001)
        
        // Order details
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        String currency,
        
        // Payment provider
        OrderProvider paymentProvider,
        String providerTransactionId,
        
        // Customer info
        String customerName,
        String customerEmail,
        String customerPhone,
        
        // Items purchased
        List<ReceiptItemResponse> items,
        
        // Breakdown
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal finalAmount,
        
        // Timing
        Instant orderDate,
        Instant paidDate,
        
        // Document links
        String pdfReceiptUrl,  // Downloadable PDF receipt
        String receiptPageUrl,  // View receipt online
        
        // Support
        String supportEmail,
        String supportPhone,
        
        // Message
        String footerMessage  // Company message, return policy, etc.
) {}