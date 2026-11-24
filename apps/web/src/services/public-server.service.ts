import { cache } from 'react'
import { createServerApiClient } from '@/lib/server-api-client'
import type { CollectionDetails, CourseDetails } from '@/types'
import type { components } from '@/types/api'

type InstructorDetails = components['schemas']['InstructorDetailsResponse']

// React cache deduplicates metadata/page requests within the incoming render,
// while locale stays part of the key and data is never shared between users.
export const getServerCourse = cache(async (slug: string, locale: string): Promise<CourseDetails> =>
  (await createServerApiClient(undefined, locale).get<CourseDetails>(`/public/courses/${encodeURIComponent(slug)}`)).data,
)

export const getServerInstructor = cache(async (slug: string, locale: string): Promise<InstructorDetails> =>
  (await createServerApiClient(undefined, locale).get<InstructorDetails>(`/public/instructors/${encodeURIComponent(slug)}`)).data,
)

export const getServerCollection = cache(async (slug: string, locale: string): Promise<CollectionDetails> =>
  (await createServerApiClient(undefined, locale).get<CollectionDetails>(`/public/collections/${encodeURIComponent(slug)}`)).data,
)

export function isNotFoundError(error: unknown): boolean {
  return (error as { response?: { status?: number } })?.response?.status === 404
}
