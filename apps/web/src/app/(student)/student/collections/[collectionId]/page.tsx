'use client'

import { useTranslations } from 'next-intl'
import { useParams } from 'next/navigation'
import { Award } from 'lucide-react'
import { StudentProgramCurriculum } from '@/components/academic/StudentProgramCurriculum'
import { useStudentCollection,useIssueCollectionCertificate } from '@/hooks/useStudentDashboard'
import { ErrorState } from '@/components/shared/ErrorState'
import { toast } from 'sonner'

export default function StudentCollectionPage(){
  const ui = useTranslations('privateUi')
const {collectionId}=useParams<{collectionId:string}>();const q=useStudentCollection(collectionId);const issue=useIssueCollectionCertificate();if(q.isLoading)return <div className='h-72 animate-pulse rounded-2xl bg-white'/>;if(q.isError||!q.data)return <ErrorState message={ui('unableToLoadThisProgram')} onRetry={()=>q.refetch()}/>;const item=q.data;return <div className='flex flex-col gap-6'><header><p className='text-xs font-semibold uppercase text-[#8c7355]'>{item.collectionType}</p><h1 className='font-heading text-3xl text-[#1c3873]'>{item.collectionName}</h1><p className='text-sm text-slate-600'>{Math.round(item.progressPercentage??0)}{ui('complete2')}</p></header><StudentProgramCurriculum id={collectionId} /><button type='button' disabled={issue.isPending} onClick={()=>issue.mutate(collectionId,{onSuccess:(r)=>{const url=r.downloadUrl??r.pdfUrl;if(url)window.open(url,'_blank','noopener,noreferrer');else toast.success(r.message??ui('certificateIsReady'))},onError:(e)=>toast.error(e.message)})} className='inline-flex w-fit items-center gap-2 rounded-lg bg-[#1c3873] px-5 py-3 text-sm font-semibold text-white disabled:opacity-50'><Award className='h-4 w-4'/>{issue.isPending?ui('checkingEligibility'):ui('getCertificate')}</button></div>}
