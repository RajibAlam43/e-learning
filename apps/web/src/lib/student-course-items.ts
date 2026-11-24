import type { components } from '@/types/api'

export type StudentSectionHome = components['schemas']['StudentSectionHomeResponse']
export type StudentLessonHome = components['schemas']['StudentLessonHomeResponse']
export type StudentQuizHome = components['schemas']['StudentQuizHomeResponse']
export type StudentLiveClassHome = components['schemas']['StudentLiveClassHomeResponse']

export type StudentCourseItem =
  | {
      itemType: 'LESSON'
      itemId: string
      position: number
      lesson: StudentLessonHome
    }
  | {
      itemType: 'QUIZ'
      itemId: string
      position: number
      quiz: StudentQuizHome
    }
  | {
      itemType: 'LIVE_CLASS'
      itemId: string
      position: number
      liveClass: StudentLiveClassHome
    }

type ApiQuizShape = StudentQuizHome & {
  id?: string
  title?: string
}

type ApiLiveClassShape = StudentLiveClassHome & {
  id?: string
  expectedDurationMinutes?: number
  durationMinutes?: number
  isMandatory?: boolean
}

function normalizeQuiz(quiz: StudentQuizHome | ApiQuizShape, fallbackId?: string): (StudentQuizHome & { quizId: string }) | null {
  const raw = quiz as ApiQuizShape
  const quizId = raw.quizId ?? raw.id ?? fallbackId
  if (!quizId) return null

  return {
    ...raw,
    quizId,
    quizTitle: raw.quizTitle ?? raw.title,
  }
}

function normalizeLiveClass(
  liveClass: StudentLiveClassHome | ApiLiveClassShape,
  fallbackId?: string,
): (StudentLiveClassHome & { liveClassId: string }) | null {
  const raw = liveClass as ApiLiveClassShape
  const liveClassId = raw.liveClassId ?? raw.id ?? fallbackId
  if (!liveClassId) return null

  return {
    ...raw,
    liveClassId,
  }
}

function lessonToCourseItem(lesson: StudentLessonHome, position: number): StudentCourseItem | null {
  if (!lesson.lessonId) return null

  if (lesson.lessonType === 'QUIZ') {
    return {
      itemType: 'QUIZ',
      itemId: lesson.lessonId,
      position,
      quiz: {
        quizId: lesson.lessonId,
        quizTitle: lesson.lessonTitle,
        position,
        isAccessible: lesson.isAccessible,
        completed: lesson.completed,
      },
    }
  }

  return {
    itemType: 'LESSON',
    itemId: lesson.lessonId,
    position,
    lesson,
  }
}

/**
 * The student course endpoint supports the ordered `items` representation,
 * while older deployments may still return separate lessons and quizzes.
 * Keep the UI compatible with both shapes without changing their order.
 */
export function getStudentSectionItems(section: StudentSectionHome): StudentCourseItem[] {
  const orderedItems = (section.items ?? [])
    .map((item, index): StudentCourseItem | null => {
      const position = item.position ?? index + 1

      if (item.itemType === 'QUIZ' || item.quiz) {
        const quiz = item.quiz ? normalizeQuiz(item.quiz, item.itemId) : normalizeQuiz({} as StudentQuizHome, item.itemId)
        if (!quiz) return null
        return {
          itemType: 'QUIZ',
          itemId: quiz.quizId,
          position,
          quiz,
        }
      }

      if (item.itemType === 'LIVE_CLASS' || item.liveClass) {
        const liveClass = item.liveClass
          ? normalizeLiveClass(item.liveClass, item.itemId)
          : normalizeLiveClass({} as StudentLiveClassHome, item.itemId)
        if (!liveClass) return null
        return {
          itemType: 'LIVE_CLASS',
          itemId: liveClass.liveClassId,
          position,
          liveClass,
        }
      }

      if ((item.itemType === 'LESSON' || item.lesson) && item.lesson) {
        return lessonToCourseItem(item.lesson, position)
      }

      return null
    })
    .filter((item): item is StudentCourseItem => item !== null)

  const fallbackItems: StudentCourseItem[] = [
    ...(section.lessons ?? [])
      .map((lesson, index) => lessonToCourseItem(lesson, lesson.position ?? index + 1))
      .filter((item): item is StudentCourseItem => item !== null),
    ...(section.quizzes ?? [])
      .map((quiz, index): Extract<StudentCourseItem, { itemType: 'QUIZ' }> | null => {
        const normalized = normalizeQuiz(quiz, undefined)
        if (!normalized?.quizId) return null
        return {
          itemType: 'QUIZ' as const,
          itemId: normalized.quizId,
          position: normalized.position ?? index + 1,
          quiz: normalized,
        }
      })
      .filter((item): item is Extract<StudentCourseItem, { itemType: 'QUIZ' }> => item !== null),
  ]

  const canonicalIds = new Set(orderedItems.map((item) => item.itemId))
  const mergedItems = [
    ...orderedItems,
    ...fallbackItems.filter((item) => !canonicalIds.has(item.itemId)),
  ]

  return mergedItems.sort((a, b) => a.position - b.position)
}

export function findStudentCourseItem(
  sections: StudentSectionHome[],
  itemId: string | null | undefined,
): { section: StudentSectionHome; item: StudentCourseItem } | null {
  if (!itemId) return null

  for (const section of sections) {
    const item = getStudentSectionItems(section).find((candidate) => candidate.itemId === itemId)
    if (item) return { section, item }
  }

  return null
}
