'use client'
import { useState } from 'react'
import type { MemberInput } from '@/services/faculty.service'
type Details = NonNullable<MemberInput['profileDetails']>

export function profileDetailsFrom(value?: Partial<Details>): Details {
  return { credentialsText: value?.credentialsText ?? '', credentialsTextEn: value?.credentialsTextEn ?? '', institution: value?.institution ?? '', institutionEn: value?.institutionEn ?? '', expertiseArea: value?.expertiseArea ?? '', expertiseAreaEn: value?.expertiseAreaEn ?? '', specialties: value?.specialties ?? [], specialtiesEn: value?.specialtiesEn ?? [], yearsExperience: value?.yearsExperience }
}
const input = 'w-full min-w-0 rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1c3873]'
const split = (value: string) => value.split(',').map(s => s.trim()).filter(Boolean)
export function FacultyProfileDetailsFields({ value, onChange }: { value: Details; onChange: (value: Details) => void }) {
  const [specialties, setSpecialties] = useState(value.specialties?.join(', ') ?? '')
  const [specialtiesEn, setSpecialtiesEn] = useState(value.specialtiesEn?.join(', ') ?? '')
  const text = (label: string, key: Exclude<keyof Details, 'specialties' | 'specialtiesEn' | 'yearsExperience'>, max = 255) => <label className="grid min-w-0 gap-1.5 text-sm font-medium text-slate-700">{label}<input className={input} maxLength={max} value={value[key] ?? ''} onChange={e => onChange({ ...value, [key]: e.target.value })} /></label>
  return <fieldset className="grid min-w-0 gap-4 border-t border-slate-100 pt-4"><legend className="px-1 text-sm font-semibold text-[#1c3873]">Profile details (optional)</legend>
    <div className="grid gap-4 sm:grid-cols-2">{text('Qualifications', 'credentialsText', 2000)}<label className="grid min-w-0 gap-1.5 text-sm font-medium text-slate-700">Years of experience<input type="number" min={0} max={100} className={input} value={value.yearsExperience ?? ''} onChange={e => onChange({ ...value, yearsExperience: e.target.value === '' ? undefined : Number(e.target.value) })} /></label>{text('Institution', 'institution')}{text('Expertise', 'expertiseArea')}</div>
    <label className="grid min-w-0 gap-1.5 text-sm font-medium text-slate-700">Specialties (comma separated)<input className={input} value={specialties} onChange={e => { setSpecialties(e.target.value); onChange({ ...value, specialties: split(e.target.value) }) }} /></label>
    <details><summary className="cursor-pointer rounded text-sm font-medium text-[#1c3873] focus-visible:outline-2">English translations (optional)</summary><div className="mt-4 grid gap-4 sm:grid-cols-2">{text('Qualifications (English)', 'credentialsTextEn', 2000)}{text('Institution (English)', 'institutionEn')}{text('Expertise (English)', 'expertiseAreaEn')}<label className="grid min-w-0 gap-1.5 text-sm font-medium text-slate-700">Specialties (English, comma separated)<input className={input} value={specialtiesEn} onChange={e => { setSpecialtiesEn(e.target.value); onChange({ ...value, specialtiesEn: split(e.target.value) }) }} /></label></div></details>
  </fieldset>
}
