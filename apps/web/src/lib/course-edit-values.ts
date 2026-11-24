import type { AdminCourseDetail } from '@/types'
import { getYouTubeWatchUrl } from '@/lib/video-embed'
import { toDateTimeInput } from '@/lib/date-time'

export function courseEditValues(course: AdminCourseDetail) {
  return {
    title: course.title ?? '',
    titleEn: course.titleEn ?? '',
    slug: course.slug ?? '',
    thumbnailObjectKey: course.thumbnailObjectKey ?? '',
    thumbnailUrl: course.thumbnailUrl ?? '',
    videoUrl: course.video?.provider === 'YOUTUBE' ? getYouTubeWatchUrl(course.video.sourceId) : '',
    categoryIds: course.categories?.map((category) => category.id).filter((id): id is string => Boolean(id)) ?? [],
    shortDescription: course.shortDescription ?? '',
    shortDescriptionEn: course.shortDescriptionEn ?? '',
    description: course.description ?? '',
    descriptionEn: course.descriptionEn ?? '',
    priceBdt: course.priceBdt ?? 0,
    isFree: course.isFree ?? false,
    level: course.level ?? 'BEGINNER',
    language: course.language ?? 'BN',
    studyMode: course.studyMode ?? 'COHORT_BASED',
    estimatedDurationMinutes: course.estimatedDurationMinutes ?? 0,
    timezone: course.timezone ?? 'Asia/Dhaka',
    enrollmentStartsAt: toDateTimeInput(course.enrollmentStartsAt, course.timezone || 'Asia/Dhaka'),
    enrollmentEndsAt: toDateTimeInput(course.enrollmentEndsAt, course.timezone || 'Asia/Dhaka'),
    startsAt: toDateTimeInput(course.startsAt, course.timezone || 'Asia/Dhaka'),
    endsAt: toDateTimeInput(course.endsAt, course.timezone || 'Asia/Dhaka'),
    capacity: course.capacity ?? 0,
    accessDurationDays: course.accessDurationDays ?? 0,
    courseOutcomes: course.courseOutcomes?.join('\n') ?? '',
    courseOutcomesEn: course.courseOutcomesEn?.join('\n') ?? '',
    requirements: course.requirements?.join('\n') ?? '',
    requirementsEn: course.requirementsEn?.join('\n') ?? '',
    highlights: course.highlights?.join('\n') ?? '',
    highlightsEn: course.highlightsEn?.join('\n') ?? '',
    targetAudience: course.targetAudience ?? '',
    targetAudienceEn: course.targetAudienceEn ?? '',
    prerequisites: course.prerequisites ?? '',
    prerequisitesEn: course.prerequisitesEn ?? '',
  }
}
