'use client'

import { useTranslations } from 'next-intl'
import { useParams } from 'next/navigation'
import { ProgramGraduationPanel } from '@/components/academic/ProgramGraduationPanel'
import { StudentProgramCurriculum } from '@/components/academic/StudentProgramCurriculum'
import { useStudentCollection } from '@/hooks/useStudentDashboard'
import { ErrorState } from '@/components/shared/ErrorState'

export default function StudentCollectionPage(){
  const ui = useTranslations('privateUi')
const {collectionId}=useParams<{collectionId:string}>();const q=useStudentCollection(collectionId);if(q.isLoading)return <div className='h-72 animate-pulse rounded-2xl bg-white'/>;if(q.isError||!q.data)return <ErrorState message={ui('unableToLoadThisProgram')} onRetry={()=>q.refetch()}/>;const item=q.data;return <div className='flex flex-col gap-6'><header><p className='text-xs font-semibold uppercase text-[#8c7355]'>{item.collectionType}</p><h1 className='font-heading text-3xl text-[#1c3873]'>{item.collectionName}</h1><p className='text-sm text-slate-600'>{Math.round(item.progressPercentage??0)}{ui('complete2')}</p></header><StudentProgramCurriculum id={collectionId} /><ProgramGraduationPanel id={collectionId} /></div>}
