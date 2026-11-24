'use client'

import { Suspense, useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import Link from 'next/link'
import { CheckCircle2, AlertCircle, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/store/auth.store'
import { usePaymentStatus } from '@/hooks/usePayment'
import { paymentView } from '@/lib/payment-status'

function PaymentResultContent() {
  const searchParams = useSearchParams()
  const orderId = searchParams.get('orderId')
  return <OrderResult key={orderId} orderId={orderId} />
}

function OrderResult({ orderId }: { orderId: string | null }) {
  const queryClient = useQueryClient()
  const { user, isSessionReady, sessionError } = useAuthStore()
  const [retry, setRetry] = useState(0)
  const query = usePaymentStatus(orderId, user?.id, isSessionReady, retry)
  const order = query.data
  const view = paymentView(order)

  useEffect(() => {
    if (view === 'complete') {
      void queryClient.invalidateQueries({ queryKey: ['courses'] })
      void queryClient.invalidateQueries({ queryKey: ['student'] })
    }
  }, [view, queryClient])

  if (!orderId) return <ResultShell><Notice title='Order ID missing' message='Open this page from your order to check its payment status.' /><Link href='/courses' className='mt-6 text-blue-700 underline'>Browse courses</Link></ResultShell>

  if (!isSessionReady) {
    return <ResultShell>{sessionError ? <><Notice title='Unable to verify your session' message='Check your connection and try again.' /><Button className='mt-6' onClick={() => window.dispatchEvent(new Event('auth-session-retry'))}>Retry</Button></> : <Loading />}</ResultShell>
  }

  if (!user) {
    const redirect = '/payment/result?orderId=' + encodeURIComponent(orderId)
    return <ResultShell><Notice title='Sign in to check your payment' message='Use the account that placed this order.' /><Link className='mt-6 text-blue-700 underline' href={'/login?redirect=' + encodeURIComponent(redirect)}>Sign in</Link></ResultShell>
  }

  if (query.isPending) return <ResultShell><Loading /></ResultShell>

  if (query.isError) return <ResultShell><Notice title='Unable to verify payment' message='Your payment status could not be confirmed. Try checking again before starting another payment.' /><Button variant='outline' className='mt-6' onClick={() => setRetry(value => value + 1)}>Check again</Button><Link href='/contact' className='mt-4 text-blue-700 underline'>Contact support</Link></ResultShell>

  const messages = {
    complete: ['Payment Successful!', 'You are now enrolled. You can start learning immediately.'],
    enrolling: ['Payment received', 'Your payment is confirmed. We are waiting for your course access to be activated.'],
    pending: ['Payment pending', 'We are waiting for confirmation. Check again before starting another payment.'],
    failed: ['Payment failed', 'The payment provider reported that this payment failed. Contact support if you see a charge.'],
    cancelled: ['Payment cancelled', 'The payment provider reported that this payment was cancelled.'],
    refunded: ['Payment refunded', 'This order is marked as refunded. Contact support for refund details.'],
    unknown: ['Payment status unavailable', 'We could not confirm a recognized payment status. Please contact support.'],
  } as const
  const [title, message] = messages[view]

  return <ResultShell>
    {view === 'complete' ? <CheckCircle2 className='h-16 w-16 text-green-600' /> : <AlertCircle className='h-16 w-16 text-amber-600' />}
    <h1 className='mt-4 text-center font-heading text-2xl font-medium text-[#1c3873]'>{title}</h1>
    <p role='status' className='mt-2 max-w-sm text-center text-sm text-[#5e5f5c]'>{message}</p>
    <dl className='mt-6 grid w-full gap-3 rounded-2xl border border-gray-100 bg-white p-5 text-sm'>
      <div><dt className='text-slate-500'>Order ID</dt><dd className='break-all font-mono'>{orderId}</dd></div>
      {order?.totalAmount != null && <div><dt className='text-slate-500'>Order total</dt><dd>{order.currency ?? 'BDT'} {order.totalAmount.toLocaleString()}</dd></div>}
      {order?.provider && <div><dt className='text-slate-500'>Payment method</dt><dd>{order.provider}</dd></div>}
    </dl>
    <div className='mt-6 flex flex-wrap justify-center gap-3'>
      {view === 'complete' ? <Link href='/student/courses'><Button className='btn-blue-gradient text-white'>Go to My Courses</Button></Link> : <Button variant='outline' disabled={query.isFetching} onClick={() => setRetry(value => value + 1)}>{query.isFetching ? 'Checking...' : 'Check again'}</Button>}
      <Link href='/courses'><Button variant='outline'>Browse courses</Button></Link>
    </div>
  </ResultShell>
}

function Notice({ title, message }: { title: string; message: string }) {
  return <><AlertCircle className='h-12 w-12 text-amber-600' /><h1 className='mt-4 text-center font-heading text-2xl text-[#1c3873]'>{title}</h1><p role='status' className='mt-2 text-center text-sm text-slate-600'>{message}</p></>
}

function Loading() {
  return <><Loader2 className='h-12 w-12 animate-spin text-blue-600' /><p role='status' className='mt-4'>Verifying your payment...</p></>
}

function ResultShell({ children }: { children: React.ReactNode }) {
  return <main className='flex min-h-screen items-center justify-center bg-[#f5f9ff] px-4 py-20'><div className='flex w-full max-w-lg flex-col items-center'>{children}</div></main>
}

export default function PaymentResultPage() {
  return <Suspense fallback={<ResultShell><Loading /></ResultShell>}><PaymentResultContent /></Suspense>
}
