import Image from 'next/image'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import type { FacultyMember } from '@/services/faculty.service'

export function FacultyPersonCard({ person }: { person: FacultyMember }) {
  const t = useTranslations('faculty')
  return <article className="flex min-w-0 flex-1 flex-col overflow-hidden rounded-lg bg-white text-center transition-all duration-300 hover:scale-[1.02] hover:shadow-lg motion-reduce:transform-none motion-reduce:transition-none">
    <div className="relative grid h-[274px] shrink-0 place-items-center overflow-hidden bg-[#edf2fc] text-6xl font-semibold text-[#1c3873]">
      {person.photoUrl ? <Image src={person.photoUrl} alt={person.name ?? ''} fill sizes="(max-width: 639px) 100vw, (max-width: 1023px) 50vw, 33vw" className="object-cover object-top" /> : <span aria-hidden="true">{person.name?.trim().charAt(0) ?? '?'}</span>}
    </div>
    <div className="flex min-w-0 flex-1 flex-col gap-4 p-6">
      <div className="flex flex-col gap-1.5 text-center">
        <h3 className="break-words font-heading text-2xl font-medium leading-8 text-[var(--color-secondary-blue-500)]">{person.instructorId ? <Link href={`/instructors/${person.instructorId}`} aria-label={`${t('viewProfile')}: ${person.name}`} className="rounded underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4">{person.name}</Link> : person.name}</h3>
      </div>
      {person.bio && <p className="mx-auto max-w-prose whitespace-pre-line break-words text-sm leading-6 text-[#5e5f5c]">{person.bio}</p>}
    </div>
  </article>
}
