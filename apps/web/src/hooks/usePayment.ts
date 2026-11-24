'use client'

import { useMutation, useQuery } from '@tanstack/react-query'
import { paymentService } from '@/services/payment.service'
import type { CreateCheckoutOrderRequest, InitiatePaymentRequest } from '@/types'
import { shouldPollPayment } from '@/lib/payment-status'

export function usePaymentStatus(orderId: string | null, userId: string | undefined, ready: boolean, retry: number) {
  return useQuery({
    queryKey: ['payment', userId, orderId, retry],
    queryFn: () => paymentService.getOrderStatus(orderId!),
    enabled: Boolean(orderId && userId && ready),
    staleTime: 0,
    retry: 1,
    refetchOnMount: 'always',
    refetchOnWindowFocus: true,
    refetchInterval: query => query.state.status !== 'error' && query.state.dataUpdateCount < 30 && shouldPollPayment(query.state.data) ? 2000 : false,
  })
}

/** Create a checkout order (single or multiple items). */
export function useCreateOrder() {
  return useMutation({
    mutationFn: (data: CreateCheckoutOrderRequest) => paymentService.createOrder(data),
  })
}

/** Initiate payment for an order — returns the gateway redirect URL. */
export function useInitiatePayment() {
  return useMutation({
    mutationFn: ({ orderId, provider }: { orderId: string; provider: InitiatePaymentRequest['provider'] }) =>
      paymentService.initiatePayment(orderId, { provider }),
  })
}
