'use client'
import { useParams } from 'next/navigation'
import { AcademicProgressView } from '@/components/academic/AcademicProgressView'
export default function StudentAcademicsPage() {
  const { id } = useParams<{ id: string }>()
  return <AcademicProgressView key={id} userId={id} />
}
