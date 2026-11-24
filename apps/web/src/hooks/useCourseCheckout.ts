'use client'

import { useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useTranslations } from 'next-intl'
import { toast } from 'sonner'
import { useAuthStore } from '@/store/auth.store'
import { useCreateOrder, useInitiatePayment } from '@/hooks/usePayment'
import { paymentService } from '@/services/payment.service'

export function useCourseCheckout({ slug, courseId, fullName, identifier, channel, agreedToTerms }: {
  slug: string; courseId?: string; fullName: string; identifier: string
  channel: 'email' | 'phone'; agreedToTerms: boolean
}) {
  const router = useRouter()
  const t = useTranslations('checkout')
  const { user, isSessionReady } = useAuthStore()
  const createOrder = useCreateOrder()
  const initiatePayment = useInitiatePayment()
  const [processing, setProcessing] = useState(false)
  const inFlight = useRef(false)
  const existingOrder = useRef<{ courseId: string; userId: string; orderId: string } | null>(null)

  async function handleCheckout(provider: 'BKASH' | 'SSLCOMMERZ') {
    if (inFlight.current || !isSessionReady) return
    if (!user) {
      toast.info(t('loginRequired'))
      router.push('/login?redirect=' + encodeURIComponent(`/courses/${slug}/checkout`))
      return
    }
    if (!fullName.trim()) return void toast.error(t('enterName'))
    if (!identifier.trim()) return void toast.error(t(channel === 'email' ? 'enterEmail' : 'enterPhone'))
    if (!agreedToTerms) return void toast.error(t('acceptTerms'))
    if (!courseId) return void toast.error(t('courseNotFound'))
    inFlight.current = true
    setProcessing(true)
    let navigating = false
    try {
      let orderId = existingOrder.current?.courseId === courseId && existingOrder.current.userId === user.id
        ? existingOrder.current.orderId : undefined
      if (!orderId) {
        const order = await createOrder.mutateAsync({ items: [{ itemType: 'COURSE', courseId }] })
        orderId = order.orderId
        if (!orderId) throw new Error('The order could not be created.')
        existingOrder.current = { courseId, userId: user.id, orderId }
        // The server determines whether this order requires a gateway payment.
        if (order.status === 'PAID' || order.totalAmount === 0) {
          router.push('/payment/result?orderId=' + encodeURIComponent(orderId))
          navigating = true
          return
        }
      } else {
        const order = await paymentService.getOrderStatus(orderId)
        if (order.status === 'PAID' || order.totalAmount === 0) {
          router.push('/payment/result?orderId=' + encodeURIComponent(orderId))
          navigating = true
          return
        }
      }
      const initiation = await initiatePayment.mutateAsync({ orderId, provider })
      const target = initiation.redirectUrl || initiation.paymentUrl
      if (!target) throw new Error('The payment provider did not return a payment URL.')
      const url = new URL(target, window.location.origin)
      if (url.protocol !== 'https:' && !(url.protocol === 'http:' && url.origin === window.location.origin)) throw new Error('Invalid payment URL returned.')
      window.location.assign(url.toString())
      navigating = true
    } catch (error) {
      toast.error((error as { message?: string }).message || t('checkoutFailed'))
    } finally {
      if (!navigating) {
        inFlight.current = false
        setProcessing(false)
      }
    }
  }

  return { handleCheckout, processing: processing || !isSessionReady }
}
