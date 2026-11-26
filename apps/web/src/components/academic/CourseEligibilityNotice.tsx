'use client'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'
export function CourseEligibilityNotice({ courseId, enrolled }: { courseId: string; enrolled: boolean }) {
 const user = useAuthStore(s => s.user)
 const query = useQuery({ queryKey: ['student', 'course-eligibility', courseId], enabled: !!user?.roles?.includes('STUDENT') && !enrolled, queryFn: async () => (await apiClient.get<components['schemas']['Requirement'][]>(`/student/courses/${courseId}/eligibility`)).data })
 if (!query.data?.length) return null
 return <div className="mx-auto my-5 max-w-7xl px-5 text-sm"><p className="font-semibold text-[#1c3873]">Complete these required courses or request a credit waiver before enrolling:</p><ul className="mt-2 space-y-2">{query.data.map(c => <li key={c.courseTemplateId}>{c.title} · {c.credits} credits <Link href={`/student/waivers?courseTemplateId=${c.courseTemplateId}`} className="ml-2 text-[#1c3873] underline underline-offset-4">Request waiver</Link></li>)}</ul></div>
}
