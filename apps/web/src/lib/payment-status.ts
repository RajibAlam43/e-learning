import type { PaymentStatusResponse } from '@/types'

export type PaymentView = 'pending' | 'enrolling' | 'complete' | 'failed' | 'cancelled' | 'refunded' | 'unknown'

export function paymentView(order: PaymentStatusResponse | undefined): PaymentView {
  switch (order?.status) {
    case 'PAID': return order.coursesEnrolled === true ? 'complete' : 'enrolling'
    case 'PENDING': return 'pending'
    case 'FAILED': return 'failed'
    case 'CANCELLED': return 'cancelled'
    case 'REFUNDED': return 'refunded'
    default: return 'unknown'
  }
}

export function shouldPollPayment(order: PaymentStatusResponse | undefined): boolean {
  return !order || ['pending', 'enrolling'].includes(paymentView(order))
}
