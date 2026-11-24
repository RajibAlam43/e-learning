'use client'

import { useState } from 'react'
import { format } from 'date-fns'
import { LifeBuoy, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { DataTable, type Column } from '@/components/admin/shared/DataTable'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import { useAdminSupportTickets, useUpdateSupportTicket } from '@/hooks/useAdmin'
import type { AdminSupportTicket } from '@/types'
import { Button } from '@/components/ui/button'

export default function SupportPage() {
  const query=useAdminSupportTickets(); const update=useUpdateSupportTicket(); const [selected,setSelected]=useState<AdminSupportTicket|null>(null)
  const columns:Column<AdminSupportTicket>[]=[{key:'subject',header:'Subject',render:t=><span className='font-medium text-[#1c3873]'>{t.subject}</span>},{key:'from',header:'From',hideOnMobile:true,render:t=><div><div>{t.name??'Anonymous'}</div><div className='text-xs text-gray-500'>{t.email??t.phone??'-'}</div></div>},{key:'status',header:'Status',render:t=><StatusBadge status={t.status??'OPEN'}/>},{key:'created',header:'Created',hideOnMobile:true,render:t=>t.createdAt?format(new Date(t.createdAt),'MMM d, yyyy'):'-'}]
  return <div className='mx-auto flex w-full max-w-[1152px] flex-col gap-6 pb-10'><AdminPageHeader title='Support Tickets' description='Review and close customer support requests.' action={<Button variant='outline' size='icon' aria-label='Refresh tickets' onClick={()=>query.refetch()} disabled={query.isFetching}><RefreshCw className={query.isFetching?'animate-spin':''}/></Button>}/>{query.isError?<div role='alert' className='rounded-xl border border-red-200 bg-white p-8 text-center'>Unable to load support tickets. <button className='text-blue-700 underline' onClick={()=>query.refetch()}>Retry</button></div>:<DataTable columns={columns} data={query.data??[]} isLoading={query.isLoading} keyExtractor={t=>t.ticketId??''} onRowClick={setSelected} emptyIcon={<LifeBuoy className='h-10 w-10'/>} emptyTitle='No tickets' emptyDescription='New support tickets will appear here.'/>}{selected&&<div role='dialog' aria-modal='true' className='fixed inset-0 z-50 grid place-items-center bg-black/40 p-4'><div className='max-h-[85vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-xl'><div className='flex items-start justify-between'><div><h2 className='font-heading text-xl text-[#1c3873]'>{selected.subject}</h2><p className='text-sm text-gray-500'>{selected.name??'Anonymous'} · {selected.email??selected.phone??'-'}</p></div><button type='button' onClick={()=>setSelected(null)} aria-label='Close'>×</button></div><p className='my-5 whitespace-pre-wrap rounded-lg bg-slate-50 p-4 text-sm'>{selected.message}</p><div className='flex items-center justify-between border-t pt-4'><StatusBadge status={selected.status??'OPEN'}/><Button disabled={update.isPending} onClick={()=>update.mutate({id:selected.ticketId!,data:{status:selected.status==='OPEN'?'CLOSED':'OPEN'}},{onSuccess:(ticket)=>{setSelected(ticket);toast.success('Ticket status updated')},onError:e=>toast.error(e.message)})}>{selected.status==='OPEN'?'Close ticket':'Reopen ticket'}</Button></div></div></div>}</div>
}
