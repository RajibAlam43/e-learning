'use client'

import { useTranslations } from 'next-intl'

import { Receipt, CheckCircle2, XCircle, Clock, AlertCircle } from 'lucide-react'
import { useStudentOrders } from '@/hooks/useStudentDashboard'
import { Skeleton } from '@/components/ui/skeleton'
import type { components } from '@/types/api'
import Link from 'next/link'

type StudentOrderSummary = components['schemas']['StudentOrderSummaryResponse']

function StatusBadge({ status }: { status: string }) {
  const ui = useTranslations('privateUi')

  switch (status) {
    case 'PAID':
      return (
        <span className='inline-flex items-center gap-1 px-2 py-1 rounded-md bg-green-50 text-green-700 text-xs font-medium'>
          <CheckCircle2 className='w-3 h-3' /> {ui('paid2')}</span>
      )
    case 'FAILED':
      return (
        <span className='inline-flex items-center gap-1 px-2 py-1 rounded-md bg-red-50 text-red-700 text-xs font-medium'>
          <XCircle className='w-3 h-3' /> {ui('failed')}</span>
      )
    case 'PENDING':
      return (
        <span className='inline-flex items-center gap-1 px-2 py-1 rounded-md bg-orange-50 text-orange-700 text-xs font-medium'>
          <Clock className='w-3 h-3' /> {ui('pending')}</span>
      )
    default:
      return <span className='text-xs text-gray-500'>{status}</span>
  }
}

export default function TransactionsSection() {
  const ui = useTranslations('privateUi')

  const { data: orders, isLoading, isError, refetch } = useStudentOrders()

  return (
    <div className='flex flex-col gap-6'>
      <section className='bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden'>
        <div className='p-4 md:p-6 border-b border-gray-100 flex items-center justify-between'>
          <div className='flex items-center gap-3'>
            <div className='w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center'>
              <Receipt className='w-5 h-5 text-[#3b82f6]' />
            </div>
            <div>
              <h2 className='text-base font-medium font-switzer text-[#1c3873]'>{ui('orderHistory')}</h2>
              <p className='text-sm text-[#5e5f5c]'>{ui('viewYourPastTransactions')}</p>
            </div>
          </div>
        </div>

        {/* Loading */}
        {isLoading && (
          <div className='p-6'>
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className='h-12 mb-2 rounded-lg' />
            ))}
          </div>
        )}

        {/* Error */}
        {isError && !isLoading && (
          <div className='p-8 flex flex-col items-center gap-3'>
            <AlertCircle className='w-8 h-8 text-red-400' />
            <p className='text-sm text-[#5e5f5c]'>{ui('failedToLoadTransactions')}</p>
            <button onClick={() => refetch()} className='text-sm text-[#3b82f6] font-medium hover:underline'>
              {ui('tryAgain')}</button>
          </div>
        )}

        {/* Empty */}
        {!isLoading && !isError && (!orders || orders.length === 0) && (
          <div className='p-8 text-center flex flex-col items-center justify-center gap-3'>
            <div className='w-12 h-12 rounded-full bg-gray-50 flex items-center justify-center'>
              <Receipt className='w-6 h-6 text-gray-400' />
            </div>
            <p className='text-[#5e5f5c]'>{ui('noTransactionsFound')}</p>
            <p className='text-xs text-[#a3a3a3]'>{ui('yourCoursePurchasesWillAppearHere')}</p>
          </div>
        )}

        {/* Orders table */}
        {!isLoading && orders && orders.length > 0 && (
          <div className='overflow-x-auto'>
            <table className='w-full text-left text-sm whitespace-nowrap'>
              <thead className='bg-gray-50/50 text-[#667085] font-medium'>
                <tr>
                  <th className='px-6 py-4'>{ui('orderId')}</th>
                  <th className='px-6 py-4'>{ui('date')}</th>
                  <th className='px-6 py-4'>{ui('amount')}</th>
                  <th className='px-6 py-4'>{ui('status')}</th>
                  <th className='px-6 py-4'>{ui('items')}</th>
                </tr>
              </thead>
              <tbody className='divide-y divide-gray-100'>
                {orders.map((tx) => (
                  <OrderRow key={tx.orderId} tx={tx} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

function OrderRow({ tx }: { tx: StudentOrderSummary }) {
  const ui = useTranslations('privateUi')

  const courseNames = (tx.items ?? []).map((i) => i.courseName).filter(Boolean)

  return (
    <tr className='hover:bg-gray-50/50 transition-colors'>
      <td className='px-6 py-4 font-medium text-[#1c3873]'>
        {(tx.orderId ?? '').slice(0, 8)}…
      </td>
      <td className='px-6 py-4 text-[#5e5f5c]'>
        {tx.paidAt
          ? new Date(tx.paidAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
          : tx.createdAt
            ? new Date(tx.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
            : '—'}
      </td>
      <td className='px-6 py-4 text-[#1c3873] font-medium'>
        {tx.totalAmount === 0
          ? ui('free')
          : `${tx.currency ?? 'BDT'} ${(tx.totalAmount ?? 0).toLocaleString()}`}
      </td>
      <td className='px-6 py-4'>
        <StatusBadge status={tx.status ?? 'PENDING'} />
      </td>
      <td className='px-6 py-4 text-[#5e5f5c] max-w-[260px]'>
        <span className='block truncate'>{courseNames.length > 0 ? courseNames.join(', ') : `${tx.courseCount ?? 0} item(s)`}</span>
        {tx.status === 'PAID' && tx.orderId && (
          <Link
            href={`/student/orders/${tx.orderId}/receipt`}
            className='mt-1 inline-block text-xs font-medium text-blue-600 hover:underline'
          >
            {ui('viewReceipt')}</Link>
        )}
      </td>
    </tr>
  )
}
