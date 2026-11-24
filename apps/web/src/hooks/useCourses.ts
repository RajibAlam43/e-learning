import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useLocale } from 'next-intl'
import { courseService, type CourseFilters } from '@/services/course.service'
import { QUERY_KEYS } from '@/constants/queryKeys'
import { collectionService } from '@/services/collection.service'
import { NETWORK } from '@/config/network.config'
import type { CourseDetails } from '@/types'

export function useCourses(filters?: CourseFilters) {
  const locale = useLocale()
  const queryClient = useQueryClient()
  // The contract supports category, level and language; other filters run on
  // one shared, bounded catalog instead of downloading it for every UI page.
  const serverFilters = {
    categorySlugs: filters?.categorySlugs ? [...filters.categorySlugs].sort() : undefined,
    level: filters?.level,
    language: filters?.language,
    sort: filters?.sort,
  }
  return useQuery({
    queryKey: [...QUERY_KEYS.publicCourses(filters), locale],
    queryFn: ({ signal }) => courseService.getPublicCourses(filters, signal, () => queryClient.fetchQuery({
      queryKey: ['courses', 'public', 'catalog', serverFilters, locale],
      queryFn: ({ signal: catalogSignal }) => courseService.getPublicCourseCatalog(serverFilters, catalogSignal, locale),
      staleTime: NETWORK.catalogStaleTimeMs,
      gcTime: NETWORK.catalogStaleTimeMs,
      retry: false,
    }), locale),
  })
}

export function usePublicCategories() {
  return useQuery({ queryKey: QUERY_KEYS.categories(), queryFn: courseService.getCategories, staleTime: 30 * 60 * 1000 })
}

export function useFeaturedCourses(limit = 6) {
  return useQuery({ queryKey: QUERY_KEYS.featuredCourses(limit), queryFn: () => courseService.getFeaturedCourses(limit) })
}

export function usePlatformStats() {
  return useQuery({
    queryKey: QUERY_KEYS.platformStats(),
    queryFn: courseService.getPlatformStats,
    staleTime: 10 * 60 * 1000,
  })
}

export function usePublicCourseReviews(slug: string) {
  return useQuery({ queryKey: QUERY_KEYS.publicCourseReviews(slug), queryFn: () => courseService.getCourseReviews(slug), enabled: !!slug })
}

export function usePublicReviews() {
  return useQuery({
    queryKey: ['public', 'reviews'],
    queryFn: courseService.getPublicReviews,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}

export function usePublicCollections(params?: { page?: number; size?: number; search?: string }) {
  return useQuery({ queryKey: ['public', 'collections', params], queryFn: () => collectionService.getPublicCollections(params) })
}
export function usePublicCollection(slug: string) {
  return useQuery({ queryKey: ['public', 'collection', slug], queryFn: () => collectionService.getPublicCollection(slug), enabled: !!slug })
}

export function useCourse(slug: string, initialData?: CourseDetails) {
  const locale = useLocale()
  const enabled = Boolean(slug && slug !== '__skip__')
  return useQuery({
    queryKey: [...QUERY_KEYS.course(slug), locale],
    queryFn: ({ signal }) => courseService.getCourseBySlug(slug, signal, locale),
    initialData,
    enabled,
  })
}
