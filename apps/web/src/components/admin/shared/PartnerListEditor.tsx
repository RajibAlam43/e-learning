'use client'
import { partnerDrafts, type HomepagePartner } from '@/config/homepage-settings'
const input = 'w-full min-w-0 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1c3873]'
export function PartnerListEditor({ value, onChange }: { value: unknown; onChange: (value: HomepagePartner[]) => void }) {
  const partners = partnerDrafts(value)
  function change(index: number, key: keyof HomepagePartner, value: string) { onChange(partners.map((p, i) => i === index ? { ...p, [key]: value } : p)) }
  return <div className="grid min-w-0 gap-4"><p className="text-sm text-slate-600">The Partners section stays hidden when this list is empty. Add approved partner names and public logo URLs in the order they should appear.</p>
    {partners.map((p, index) => <fieldset key={index} className="grid min-w-0 gap-3 rounded-lg border border-slate-200 p-4"><legend className="px-1 text-sm font-semibold text-[#1c3873]">Partner {index + 1}</legend>
      <label className="grid gap-1 text-sm">Partner name<input required className={input} maxLength={200} value={p.name} onChange={e => change(index, 'name', e.target.value)} /></label>
      <label className="grid gap-1 text-sm">Logo URL<input required className={input} value={p.logoUrl} placeholder="https://assets.example.com/logo.png" onChange={e => change(index, 'logoUrl', e.target.value)} /></label>
      <label className="grid gap-1 text-sm">Website URL (optional)<input className={input} value={p.websiteUrl} placeholder="https://example.com" onChange={e => change(index, 'websiteUrl', e.target.value)} /></label>
      <div className="flex flex-wrap gap-3"><button type="button" disabled={index === 0} onClick={() => { const next = [...partners]; [next[index - 1], next[index]] = [next[index]!, next[index - 1]!]; onChange(next) }} className="rounded text-sm text-[#1c3873] underline disabled:opacity-30" aria-label={`Move partner ${index + 1} up`}>Move up</button><button type="button" disabled={index === partners.length - 1} onClick={() => { const next = [...partners]; [next[index], next[index + 1]] = [next[index + 1]!, next[index]!]; onChange(next) }} className="rounded text-sm text-[#1c3873] underline disabled:opacity-30" aria-label={`Move partner ${index + 1} down`}>Move down</button><button type="button" className="rounded text-sm text-red-700 underline" onClick={() => onChange(partners.filter((_, i) => i !== index))} aria-label={`Remove partner ${index + 1}`}>Remove</button></div>
    </fieldset>)}
    <button type="button" disabled={partners.length >= 50} onClick={() => onChange([...partners, { name: '', logoUrl: '', websiteUrl: '' }])} className="justify-self-start rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-[#1c3873] disabled:opacity-50">Add partner</button>
  </div>
}
