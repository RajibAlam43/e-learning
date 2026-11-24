import type { AdminOrderSummary } from '@/types'
import { toDateTimeInput } from '@/lib/date-time'

export interface RevenuePoint { date: string; revenue: number; paidOrders: number }
export interface OrderCustomer {
  name: string
  email: string
  orderCount: number
  totalSpent: number
  lastOrder: string
}

export function isPaidOrder(order: AdminOrderSummary): boolean {
  return order.status?.toUpperCase() === 'PAID'
}

export function customerEmail(order: AdminOrderSummary): string {
  return order.customerEmail?.trim().toLowerCase() ?? ''
}

export function deriveOrderCustomers(orders: AdminOrderSummary[]): OrderCustomer[] {
  const customers = new Map<string, OrderCustomer>()
  for (const order of orders) {
    const email = customerEmail(order)
    if (!email) continue
    const customer = customers.get(email) ?? {
      name: order.customerName || 'Unknown', email, orderCount: 0, totalSpent: 0, lastOrder: '',
    }
    customer.orderCount++
    if (isPaidOrder(order)) customer.totalSpent += order.totalAmount ?? 0
    if (order.createdAt && (!customer.lastOrder || Date.parse(order.createdAt) > Date.parse(customer.lastOrder))) {
      customer.lastOrder = order.createdAt
      customer.name = order.customerName || customer.name
    }
    customers.set(email, customer)
  }
  return [...customers.values()]
}

export function orderTotals(orders: AdminOrderSummary[]) {
  const paid = orders.filter(isPaidOrder)
  return {
    totalRevenue: paid.reduce((sum, order) => sum + (order.totalAmount ?? 0), 0),
    totalPaidOrders: paid.length,
    totalOrderCustomers: new Set(orders.map(customerEmail).filter(Boolean)).size,
  }
}

/** Reporting days use the platform timezone, independent of the viewer's computer. */
function paidDay(order: AdminOrderSummary): string {
  return isPaidOrder(order) ? toDateTimeInput(order.paidAt, 'Asia/Dhaka').slice(0, 10) : ''
}

export function revenueByDay(orders: AdminOrderSummary[], days: number, now = new Date()): RevenuePoint[] {
  const end = Date.parse(`${toDateTimeInput(now.toISOString(), 'Asia/Dhaka').slice(0, 10)}T00:00:00Z`)
  const buckets = new Map<string, RevenuePoint>()
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(end - i * 86_400_000).toISOString().slice(0, 10)
    buckets.set(date, { date, revenue: 0, paidOrders: 0 })
  }
  for (const order of orders) {
    const bucket = buckets.get(paidDay(order))
    if (bucket) { bucket.revenue += order.totalAmount ?? 0; bucket.paidOrders++ }
  }
  return [...buckets.values()]
}

export function revenueByMonth(orders: AdminOrderSummary[]): { month: string; revenue: number }[] {
  const buckets = new Map<string, number>()
  for (const order of orders) {
    const day = paidDay(order)
    if (!day) continue
    const month = day.slice(0, 7)
    buckets.set(month, (buckets.get(month) ?? 0) + (order.totalAmount ?? 0))
  }
  return [...buckets.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([month, revenue]) => ({ month, revenue }))
}
