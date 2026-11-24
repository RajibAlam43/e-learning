import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'
import { collectPages } from '@/lib/pagination'
import { NETWORK } from '@/config/network.config'

type CourseSummary = components['schemas']['CourseSummaryResponse']
type CourseDetails = components['schemas']['CourseDetailsResponse']
type PageResponse = components['schemas']['PageResponseCourseSummaryResponse']
type Category = components['schemas']['CategoryResponse']
type CourseReview = components['schemas']['CourseReviewResponse']
type CourseReviewPage = components['schemas']['PageResponseCourseReviewResponse']
type PublicPlatformStats = components['schemas']['PublicPlatformStatsResponse']

export interface PublicCourseCatalog {
  firstPage: PageResponse
  allCourses: CourseSummary[]
}

export interface CourseFilters {
  categorySlugs?: string[]
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  language?: 'BN' | 'EN'
  search?: string
  duration?: 'Under 2 weeks' | '2-4 weeks' | '4-8 weeks'
  isFree?: boolean
  isPaid?: boolean
  priceMin?: number
  priceMax?: number
  page?: number
  size?: number
  sort?: string[]
}

function durationInWeeks(startsAt: string | undefined, endsAt: string | undefined): number | null {
  if (!startsAt || !endsAt) return null
  const start = new Date(startsAt).getTime()
  const end = new Date(endsAt).getTime()
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) return null
  return (end - start) / (7 * 24 * 60 * 60 * 1000)
}

async function requestCoursePage(filters: CourseFilters | undefined, page: number, size: number, signal?: AbortSignal, locale?: string): Promise<PageResponse> {
  return (await apiClient.get<PageResponse>('/public/courses', {
    signal,
    ...(locale ? { headers: { 'Accept-Language': locale } } : {}),
    paramsSerializer: { indexes: null },
    params: { categorySlugs: filters?.categorySlugs, level: filters?.level, language: filters?.language, page, size, sort: filters?.sort },
  })).data
}

export const courseService = {
  getCategories: async (): Promise<Category[]> =>
    (await apiClient.get<Category[]>('/public/categories')).data,

  getFeaturedCourses: async (limit = 6): Promise<CourseSummary[]> =>
    (await apiClient.get<CourseSummary[]>('/public/courses/featured', { params: { limit } })).data,

  getPlatformStats: async (): Promise<PublicPlatformStats> =>
    (await apiClient.get<PublicPlatformStats>('/public/stats')).data,

  getCourseReviews: async (slug: string): Promise<CourseReview[]> =>
    (await apiClient.get<CourseReview[]>(`/public/courses/${encodeURIComponent(slug)}/reviews`)).data,

  getPublicReviews: async (): Promise<CourseReview[]> => {
    const response = await apiClient.get<CourseReviewPage>('/public/reviews')
    return response.data.content ?? []
  },

  getPublicCourseCatalog: async (filters?: CourseFilters, signal?: AbortSignal, locale?: string): Promise<PublicCourseCatalog> => {
    const deadline = AbortSignal.timeout(NETWORK.catalogTimeoutMs)
    const catalogSignal = signal ? AbortSignal.any([signal, deadline]) : deadline
    const requestPage = (page: number) => requestCoursePage(filters, page, 20, catalogSignal, locale)
    const firstPage = await requestPage(0)
    return { firstPage, allCourses: await collectPages(firstPage, requestPage, catalogSignal) }
  },

  getPublicCourses: async (filters?: CourseFilters, signal?: AbortSignal, getCatalog?: () => Promise<PublicCourseCatalog>, locale?: string): Promise<PageResponse> => {

    const requestedPage = filters?.page ?? 0
    const requestedSize = filters?.size ?? 12
    const searchTerm = filters?.search?.trim().toLocaleLowerCase()
    const hasLocalFilters = Boolean(
      searchTerm || filters?.duration || filters?.isFree || filters?.isPaid ||
      (filters?.priceMin ?? 0) > 0 || (filters?.priceMax ?? 10000) < 10000,
    )

    if (!hasLocalFilters) return requestCoursePage(filters, requestedPage, requestedSize, signal, locale)

    const { firstPage, allCourses } = await (getCatalog ? getCatalog() : courseService.getPublicCourseCatalog(filters, signal, locale))
    signal?.throwIfAborted()
    const matchingCourses = allCourses
      .filter((course) => {
        if (searchTerm && ![
          course.title,
          course.slug,
          course.shortDescription,
          ...(course.categoryNames ?? []),
          ...(course.instructorNames ?? []),
        ].filter(Boolean).join(' ').toLocaleLowerCase().includes(searchTerm)) return false

        const price = course.priceBdt ?? 0
        if (filters?.isFree && !filters.isPaid && price > 0) return false
        if (filters?.isPaid && !filters.isFree && price <= 0) return false
        const maxPrice = filters?.priceMax === 10000 ? Infinity : filters?.priceMax ?? Infinity
        if (price < (filters?.priceMin ?? 0) || price > maxPrice) return false

        if (filters?.duration) {
          const weeks = durationInWeeks(course.startsAt, course.endsAt)
          if (weeks === null) return false
          if (filters.duration === 'Under 2 weeks' && weeks >= 2) return false
          if (filters.duration === '2-4 weeks' && (weeks < 2 || weeks > 4)) return false
          if (filters.duration === '4-8 weeks' && (weeks <= 4 || weeks > 8)) return false
        }
        return true
      })
    const offset = requestedPage * requestedSize

    return {
      ...firstPage,
      content: matchingCourses.slice(offset, offset + requestedSize),
      page: requestedPage,
      size: requestedSize,
      totalElements: matchingCourses.length,
      totalPages: Math.max(Math.ceil(matchingCourses.length / requestedSize), 1),
    }
  },

  getCourseBySlug: async (slug: string, signal?: AbortSignal, locale?: string): Promise<CourseDetails> => {
    const res = await apiClient.get<CourseDetails>(`/public/courses/${encodeURIComponent(slug)}`, { signal, ...(locale ? { headers: { 'Accept-Language': locale } } : {}) })
    return res.data
  },
}
