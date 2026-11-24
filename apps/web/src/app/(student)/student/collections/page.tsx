'use client'

import { useTranslations } from 'next-intl'
import Image from 'next/image'
import Link from 'next/link'
import { Library } from 'lucide-react'
import { useStudentCollections } from '@/hooks/useStudentDashboard'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'

export default function StudentCollectionsPage(){
  const ui = useTranslations('privateUi')
const q=useStudentCollections();if(q.isLoading)return <div className='grid gap-4 md:grid-cols-2'>{[1,2].map(i=><div key={i} className='h-56 animate-pulse rounded-2xl bg-white'/>)}</div>;if(q.isError)return <ErrorState message={ui('unableToLoadYourPrograms')} onRetry={()=>q.refetch()}/>;const items=q.data??[];return <div className='flex flex-col gap-6'><header><h1 className='font-heading text-2xl text-[#1c3873]'>{ui('myPrograms')}</h1><p className='text-sm text-slate-600'>{ui('trackProgressAcrossYourLearningPaths')}</p></header>{items.length===0?<EmptyState icon={<Library className='h-8 w-8'/>} title={ui('noProgramsYet')} description={ui('programsYouEnrollInWillAppearHere')}/ >:<div className='grid gap-4 md:grid-cols-2 xl:grid-cols-3'>{items.map(item=><Link key={item.collectionId} href={`/student/collections/${item.collectionId}`} className='overflow-hidden rounded-2xl bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md'><div className='relative h-36 bg-slate-100'>{item.thumbnailUrl&&<Image src={item.thumbnailUrl} alt={item.collectionName??ui('program')} fill sizes='(max-width:768px) 100vw, 33vw' className='object-cover'/>}</div><div className='p-4'><p className='text-xs font-semibold text-[#8c7355]'>{item.collectionType}</p><h2 className='font-heading text-lg text-[#1c3873]'>{item.collectionName}</h2><div className='mt-3 h-2 overflow-hidden rounded-full bg-slate-100'><div className='h-full bg-blue-600' style={{width:`${Math.min(100,Math.max(0,item.progressPercentage??0))}%`}}/></div><p className='mt-1 text-xs text-slate-500'>{Math.round(item.progressPercentage??0)}{ui('complete3')}{' '}{item.courseCount??0} {ui('courses2')}</p></div></Link>)}</div>}</div>}
