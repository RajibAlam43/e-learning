'use client'
import { useTranslations } from 'next-intl'
import { useFaculty } from '@/services/faculty.service'
import { PageHeader } from '@/components/shared/PageHeader'
import { FacultyPersonCard } from './FacultyPersonCard'
export function FacultyDirectory() {
  const t = useTranslations('faculty'), query = useFaculty()
  return <main className="min-h-screen bg-white pb-16"><PageHeader title={t('title')} description={t('description')} />
    <div className="container mx-auto px-5 pt-8">
      {query.isLoading && <p role="status" className="py-12 text-[#5e5f5c]">{t('loading')}</p>}
      {query.isError && <div role="alert" className="py-8"><p>{t('error')}</p><button onClick={() => query.refetch()} className="mt-3 rounded-lg border px-4 py-2 text-[#1c3873]">{t('retry')}</button></div>}
      {!query.isLoading && !query.isError && !query.data?.length && <p className="py-12 text-[#5e5f5c]">{t('empty')}</p>}
      {!!query.data?.length && <nav aria-label={t('groups')} className="mb-10 flex flex-wrap gap-x-6 gap-y-3 border-b border-slate-200 pb-5">{query.data.map(g => <a key={g.id} href={`#group-${g.id}`} className="rounded text-sm font-semibold text-[#1c3873] underline-offset-4 hover:underline focus-visible:outline-2">{g.name}</a>)}</nav>}
      {query.data?.map(g => <section key={g.id} id={`group-${g.id}`} aria-labelledby={`heading-${g.id}`} className="mb-12 scroll-mt-28"><h2 id={`heading-${g.id}`} className="mb-5 font-heading text-2xl font-semibold text-[#1c3873]">{g.name}</h2><div className="grid gap-5 bg-[#f5f9ff] p-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">{g.members?.map(p => <FacultyPersonCard key={p.id} person={p} />)}</div></section>)}
    </div>
  </main>
}
