'use client'
import { useState } from 'react'
import { downloadWaiverEvidence, type Waiver } from '@/services/prerequisite.service'
export function WaiverHistory({ items, admin, review }: { items: Waiver[]; admin?: boolean; review?: (item: Waiver) => void }) {
 const [error, setError] = useState('')
 const [downloading, setDownloading] = useState<string>()
 async function download(id: string) {
  setError(''); setDownloading(id)
  try { await downloadWaiverEvidence(id, !!admin) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Unable to download file.') } finally { setDownloading(undefined) }
 }
 return <>{error && <p role="alert" className="my-3 text-sm text-red-700">{error}</p>}
  <ul className="divide-y divide-slate-200">{items.map(item => <li key={item.id} className="py-5">
   <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold text-slate-900">{item.courseTitle}</h3><p className="mt-1 text-sm text-slate-600">{item.status} · {item.requestedCredits} credits requested{item.createdAt ? ` · ${new Date(item.createdAt).toLocaleDateString()}` : ''}</p></div>
   {review && item.status === 'PENDING' && <button onClick={() => review(item)} className="rounded-lg border px-4 py-2 text-sm text-[#1c3873]">Review request</button>}</div>
   {admin && <p className="mt-2 break-words text-sm font-medium">{item.studentName} · {item.studentContact}</p>}
   <p className="mt-3 max-w-prose whitespace-pre-wrap break-words text-sm text-slate-700">{item.reason}</p>
   {item.hasEvidence && <button disabled={downloading === item.id} onClick={() => download(item.id!)} className="mt-3 py-2 text-sm text-[#1c3873] underline underline-offset-4 disabled:opacity-60">{downloading === item.id ? 'Preparing download…' : 'Download supporting file'}</button>}
   {item.reviewReason && <div className="mt-4 text-sm"><p className="font-medium">{item.status === 'APPROVED' ? `Approved · ${item.awardedCredits ?? 0} course credits on record` : 'Request rejected'}{item.reviewerName ? ` · ${item.reviewerName}` : ''}</p><p className="mt-1 max-w-prose whitespace-pre-wrap break-words text-slate-600">{item.reviewReason}</p></div>}
  </li>)}</ul>
 </>
}
