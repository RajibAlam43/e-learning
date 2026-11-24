import { APP_CONFIG } from '@/config/app.config'

/** Escape HTML-significant characters before embedding JSON in a script. */
export function serializeJsonLd(value: unknown): string {
  return JSON.stringify(value).replace(/</g, '\\u003c').replace(/\u2028/g, '\\u2028').replace(/\u2029/g, '\\u2029')
}

export function courseJsonLd(course: Record<string, unknown>) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Course',
    name: course.courseName || course.title,
    description: course.shortDescription || course.description,
    provider: {
      '@type': 'Organization',
      name: APP_CONFIG.name,
      sameAs: APP_CONFIG.url,
    },
    ...(course.thumbnailUrl ? { image: course.thumbnailUrl as string } : {}),
    ...(course.instructorName
      ? { instructor: { '@type': 'Person', name: course.instructorName as string } }
      : {}),
  }
}

export function instructorJsonLd(instructor: Record<string, unknown>) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Person',
    name: instructor.fullName || instructor.name,
    description: instructor.bio || instructor.shortBio,
    ...(instructor.avatarUrl ? { image: instructor.avatarUrl as string } : {}),
    worksFor: {
      '@type': 'Organization',
      name: APP_CONFIG.name,
    },
  }
}
