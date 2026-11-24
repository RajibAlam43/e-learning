'use client'

import Image from 'next/image'
import Link from 'next/link'
import { usePublicCollections } from '@/hooks/useCourses'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'

export default function CollectionsClient() {
  const query = usePublicCollections({ page: 0, size: 24 })
  const items = query.data?.content ?? []
  if (query.isLoading) return <main className='mx-auto max-w-6xl px-4 py-20'><div className='grid gap-6 sm:grid-cols-2 lg:grid-cols-3'>{[1,2,3].map(i=><div key={i} className='h-80 animate-pulse rounded-2xl bg-slate-100' />)}</div></main>
  if (query.isError) return <main className='mx-auto max-w-6xl px-4 py-20'><ErrorState message='Unable to load programs.' onRetry={()=>query.refetch()} /></main>
  return <main className='mx-auto max-w-6xl px-4 py-12 sm:py-20'><header className='mb-10'><h1 className='font-heading text-4xl text-[#1c3873]'>Programs & learning tracks</h1><p className='mt-2 text-slate-600'>Build a complete learning path with curated courses.</p></header>{items.length===0 ? <EmptyState title='No programs available' description='Check back soon for new learning tracks.' /> : <div className='grid gap-6 sm:grid-cols-2 lg:grid-cols-3'>{items.map(item=><article key={item.id} className='overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-100'><div className='relative aspect-[16/9] bg-slate-100'>{item.thumbnailUrl&&<Image src={item.thumbnailUrl} alt={item.title??'Program'} fill sizes='(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw' className='object-cover' />}</div><div className='p-5'><p className='text-xs font-semibold uppercase tracking-wide text-[#8c7355]'>{item.collectionType}</p><h2 className='mt-1 font-heading text-xl text-[#1c3873]'>{item.title}</h2><p className='mt-2 text-sm text-slate-600'>{item.courseCount??0} courses · {item.priceBdt ? `${item.priceBdt.toLocaleString()} BDT` : 'Free'}</p><Link href={`/collections/${item.slug}`} className='mt-4 inline-flex text-sm font-semibold text-blue-600'>View program →</Link></div></article>)}</div>}</main>
}
