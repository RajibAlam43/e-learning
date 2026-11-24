'use client'

import { useTranslations } from 'next-intl'

import Link from 'next/link'
import { useParams } from 'next/navigation'
import { Download, Printer, ReceiptText } from 'lucide-react'
import { useOrderReceipt } from '@/hooks/useStudentDashboard'
import { ErrorState } from '@/components/shared/ErrorState'

const formatMoney = (value?: number, currency = 'BDT') =>
  `${currency} ${(value ?? 0).toLocaleString('en-BD', { minimumFractionDigits: 2 })}`

const formatDate = (value?: string) =>
  value
    ? new Date(value).toLocaleString('en-BD', { dateStyle: 'medium', timeStyle: 'short' })
    : '—'

export default function StudentReceiptPage() {
  const ui = useTranslations('privateUi')

  const { orderId } = useParams<{ orderId: string }>()
  const receipt = useOrderReceipt(orderId)

  if (receipt.isLoading) {
    return <div className='mx-auto h-[520px] max-w-3xl animate-pulse rounded-2xl bg-white' />
  }

  if (receipt.isError || !receipt.data) {
    return (
      <div className='mx-auto max-w-3xl rounded-2xl bg-white p-6'>
        <ErrorState
          title={ui('receiptUnavailable')}
          message={ui('weCouldNotLoadThisReceiptConfirmThatTheOrderIsPaidAndBelongsToYo')}
          onRetry={() => receipt.refetch()}
        />
        <div className='text-center'>
          <Link href='/student/settings' className='text-sm font-semibold text-blue-600 hover:underline'>
            {ui('returnToTransactions')}</Link>
        </div>
      </div>
    )
  }

  const data = receipt.data

  return (
    <main className='mx-auto max-w-3xl print:max-w-none'>
      <div className='mb-4 flex flex-wrap items-center justify-between gap-3 print:hidden'>
        <Link href='/student/settings' className='text-sm font-semibold text-blue-600 hover:underline'>
          {ui('backToTransactions')}</Link>
        <div className='flex gap-2'>
          <button
            type='button'
            onClick={() => window.print()}
            className='inline-flex items-center gap-2 rounded-lg border bg-white px-4 py-2 text-sm font-semibold'
          >
            <Printer className='h-4 w-4' /> {ui('print')}</button>
          {data.pdfReceiptUrl && (
            <a
              href={data.pdfReceiptUrl}
              target='_blank'
              rel='noopener noreferrer'
              className='inline-flex items-center gap-2 rounded-lg bg-[#1c3873] px-4 py-2 text-sm font-semibold text-white'
            >
              <Download className='h-4 w-4' /> {ui('downloadPdf')}</a>
          )}
        </div>
      </div>

      <article className='rounded-2xl bg-white p-6 shadow-sm sm:p-10 print:rounded-none print:shadow-none'>
        <header className='flex flex-col gap-4 border-b pb-6 sm:flex-row sm:items-start sm:justify-between'>
          <div>
            <div className='flex items-center gap-2 text-[#1c3873]'>
              <ReceiptText className='h-7 w-7' />
              <h1 className='font-heading text-3xl'>{ui('paymentReceipt')}</h1>
            </div>
            <p className='mt-2 text-sm text-slate-500'>{ui('receipt')}{data.receiptNumber ?? orderId.slice(0, 8)}</p>
          </div>
          <span className='w-fit rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700'>
            {data.orderStatus ?? ui('paid')}
          </span>
        </header>

        <section className='grid gap-5 border-b py-6 text-sm sm:grid-cols-2'>
          <div>
            <p className='text-slate-500'>{ui('billedTo')}</p>
            <p className='mt-1 font-semibold text-slate-900'>{data.customerName ?? ui('student')}</p>
            <p>{data.customerEmail}</p>
            <p>{data.customerPhone}</p>
          </div>
          <div className='sm:text-right'>
            <p className='text-slate-500'>{ui('paymentDetails')}</p>
            <p className='mt-1'>{ui('orderDate')}{' '}{formatDate(data.orderDate)}</p>
            <p>{ui('paidDate')}{' '}{formatDate(data.paidDate)}</p>
            <p>{data.paymentProvider ?? '—'} · {data.providerTransactionId ?? '—'}</p>
          </div>
        </section>

        <section className='py-6'>
          <h2 className='font-heading text-xl text-[#1c3873]'>{ui('items')}</h2>
          <div className='mt-3 overflow-x-auto'>
            <table className='w-full min-w-[520px] text-left text-sm'>
              <thead className='border-b text-slate-500'>
                <tr><th className='py-3'>{ui('item')}</th><th className='py-3 text-right'>{ui('price')}</th><th className='py-3 text-right'>{ui('discount')}</th><th className='py-3 text-right'>{ui('total')}</th></tr>
              </thead>
              <tbody className='divide-y'>
                {(data.items ?? []).map((item, index) => (
                  <tr key={item.itemId ?? item.courseId ?? index}>
                    <td className='py-4 font-medium'>{item.itemName ?? item.courseName ?? ui('course')}</td>
                    <td className='py-4 text-right'>{formatMoney(item.unitPrice, data.currency)}</td>
                    <td className='py-4 text-right'>{formatMoney(item.discountAmount, data.currency)}</td>
                    <td className='py-4 text-right font-semibold'>{formatMoney(item.lineTotal, data.currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className='ml-auto grid max-w-sm gap-2 border-t pt-5 text-sm'>
          <div className='flex justify-between'><span>{ui('subtotal')}</span><span>{formatMoney(data.subtotal, data.currency)}</span></div>
          <div className='flex justify-between'><span>{ui('discount')}</span><span>-{formatMoney(data.totalDiscount, data.currency)}</span></div>
          <div className='flex justify-between text-lg font-bold text-[#1c3873]'><span>{ui('totalPaid')}</span><span>{formatMoney(data.finalAmount ?? data.totalAmount, data.currency)}</span></div>
        </section>

        {data.footerMessage && <p className='mt-8 border-t pt-5 text-center text-xs text-slate-500'>{data.footerMessage}</p>}
      </article>
    </main>
  )
}
