import { apiClient } from '@/lib/api-client'
import type {
  CreateCheckoutOrderRequest,
  CheckoutOrderResponse,
  InitiatePaymentRequest,
  PaymentInitiationResponse,
  PaymentStatusResponse,
} from '@/types'

/**
 * Payment service — handles checkout order creation and payment initiation.
 *
 * Flow:
 *   1. createOrder(items) → POST /checkout/orders → orderId
 *   2. initiatePayment(orderId, provider) → POST /payments/{orderId}/initiate → redirectUrl
 *   3. Browser redirects to redirectUrl (bKash / SSLCommerz gateway)
 *   4. After payment, provider calls backend callback → backend redirects (303) to frontend
 */
export const paymentService = {
  createOrder: async (data: CreateCheckoutOrderRequest): Promise<CheckoutOrderResponse> => {
    const res = await apiClient.post<CheckoutOrderResponse>('/checkout/orders', data)
    return res.data
  },

  initiatePayment: async (
    orderId: string,
    data: InitiatePaymentRequest,
  ): Promise<PaymentInitiationResponse> => {
    const res = await apiClient.post<PaymentInitiationResponse>(
      `/payments/${orderId}/initiate`,
      data,
    )
    return res.data
  },
  getOrderStatus: async (orderId: string): Promise<PaymentStatusResponse> => {
    const { data } = await apiClient.get<PaymentStatusResponse>(`/checkout/orders/${encodeURIComponent(orderId)}`)
    if (data.orderId && data.orderId !== orderId) throw new Error('The returned payment does not match this order.')
    return data
  },
}
