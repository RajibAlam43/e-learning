import { useQuery } from '@tanstack/react-query'
import { instructorService } from '@/services/instructor.service'
import { QUERY_KEYS } from '@/constants/queryKeys'
import { useLocale } from 'next-intl'
import type { components } from '@/types/api'

export function useInstructors() {
  const locale = useLocale()
  return useQuery({
    queryKey: [...QUERY_KEYS.instructors(), locale],
    queryFn: ({ signal }) => instructorService.getPublicInstructors(signal, locale),
  })
}

export function useInstructor(slug: string, initialData?: components['schemas']['InstructorDetailsResponse']) {
  const locale = useLocale()
  return useQuery({
    queryKey: [...QUERY_KEYS.instructor(slug), locale],
    queryFn: ({ signal }) => instructorService.getInstructorBySlug(slug, signal, locale),
    initialData,
    enabled: !!slug,
  })
}
