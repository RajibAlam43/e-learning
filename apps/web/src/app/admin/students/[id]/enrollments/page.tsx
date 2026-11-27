'use client'
import { useParams } from 'next/navigation'
import { ManualEnrollmentManager } from '@/components/academic/ManualEnrollmentManager'

export default function StudentEnrollmentsPage() {
  const { id } = useParams<{ id: string }>()
  return <ManualEnrollmentManager key={id} userId={id} />
}
