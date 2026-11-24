'use client'

import { useTranslations } from 'next-intl'

import Link from 'next/link'
import { Clock, Target, RotateCcw } from 'lucide-react'
import { Breadcrumb } from '@/components/shared/Breadcrumb'
import type { QuizAttemptResult } from '@/types'

interface QuizResultSectionProps {
  serverResult: QuizAttemptResult
  courseName: string
  moduleName: string
  onRetake: () => void
  canRetry?: boolean
}

interface ReviewItem {
  questionText: string
  userChoiceText: string | null
  correctChoiceText: string | null
  isCorrect: boolean
  explanation?: string
}

interface ResultSummary {
  correctCount: number
  totalCount: number
  scorePct: number
  durationSeconds?: number
  passed?: boolean
  review: ReviewItem[]
}

function formatDuration(totalSeconds?: number): string {
  if (!totalSeconds || totalSeconds <= 0) return '—'
  const m = Math.floor(totalSeconds / 60)
  const s = Math.round(totalSeconds % 60)
  return `${m}m ${s.toString().padStart(2, '0')}s`
}

function buildFromServer(server: QuizAttemptResult): ResultSummary {
  const qResults = server.questionResults ?? []
  const review: ReviewItem[] = qResults
    .slice()
    .sort((a, b) => (a.position ?? 0) - (b.position ?? 0))
    .map((qr) => ({
      questionText: qr.questionText ?? '',
      userChoiceText: qr.userChoiceText ?? null,
      correctChoiceText: qr.correctChoiceText ?? null,
      isCorrect: !!qr.userAnswerCorrect,
      explanation: qr.explanation,
    }))
  return {
    correctCount: review.filter((r) => r.isCorrect).length,
    totalCount: review.length,
    scorePct: server.scorePct ?? 0,
    durationSeconds: server.durationSeconds,
    passed: server.passed,
    review,
  }
}

export default function QuizResultSection({
  serverResult,
  courseName,
  moduleName,
  onRetake,
  canRetry = true,
}: QuizResultSectionProps) {
  const ui = useTranslations('privateUi')

  const summary: ResultSummary = buildFromServer(serverResult)

  const { correctCount, totalCount, scorePct, review } = summary
  const incorrectCount = totalCount - correctCount
  const accuracyPct = scorePct

  const performanceLevel = scorePct >= 80 ? ui('performanceExcellent') : scorePct >= 60 ? ui('performanceGood') : scorePct >= 40 ? ui('performancePractice') : ui('performanceImprove')
  const performanceColor = scorePct >= 80 ? 'text-green-800' : scorePct >= 60 ? 'text-blue-700' : scorePct >= 40 ? 'text-yellow-800' : 'text-red-800'

  return (
    <div className='flex flex-col gap-8 md:gap-10'>
      {/* Breadcrumb */}
      <Breadcrumb items={[
        { label: ui('myCourses'), href: '/student/courses' },
        { label: courseName },
        { label: moduleName },
      ]} />

      {/* 1. Completion Message */}
      <section className='flex flex-col gap-4 md:gap-6'>
        <div className='w-16 h-16 rounded-full bg-[#a7f3d0] flex items-center justify-center'>
          <svg className='w-8 h-8 text-green-800' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='2.5' strokeLinecap='round' strokeLinejoin='round'>
            <polyline points='20 6 9 17 4 12' />
          </svg>
        </div>
        <div className='flex flex-col gap-3'>
          <h2 className='text-2xl md:text-[32px] font-heading font-medium text-[#1c3873] leading-tight'>
            {ui('mubarakYouVeCompletedThe')}<br className='hidden md:inline' /> {ui('knowledgeCheck')}</h2>
          <p className='text-base text-[#5e5f5c] leading-6'>
            {ui('yourDedicationToUnderstandingTheseSacredConceptsIsAPathToEnlight')}</p>
        </div>
      </section>

      {/* 2. Performance Metrics */}
      <section className='flex flex-col md:flex-row items-stretch gap-2'>
        <article className='flex flex-col justify-between p-6 bg-white rounded-2xl border border-solid border-blue-100 shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)]'>
          <div className='text-[32px] md:text-[36px] font-heading text-[#004532] leading-[44px]'>
            {scorePct}%
          </div>
          <div className='flex flex-col gap-1'>
            <p className='text-lg md:text-xl font-heading text-[#121c2a] leading-5'>
              {correctCount} {ui('outOf')}{' '}{totalCount} {ui('correct')}</p>
            <div className={`text-sm font-medium ${performanceColor} leading-5`}>
              {performanceLevel}
            </div>
          </div>
        </article>

        <article className='flex-1 flex flex-col justify-between p-6 bg-white rounded-2xl border border-solid border-blue-100 shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)]'>
          <div className='pb-2'>
            <Clock className='w-5 h-5 text-[#5e5f5c]' />
          </div>
          <div className='flex flex-col'>
            <span className='text-xs font-medium text-[#5e5f5c] leading-4'>{ui('timeTaken')}</span>
            <span className='text-lg font-semibold text-[#121c2a] leading-6'>
              {formatDuration(summary.durationSeconds)}
            </span>
          </div>
        </article>

        <article className='flex-1 flex flex-col justify-between p-6 bg-white rounded-2xl border border-solid border-blue-100 shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)]'>
          <div className='pb-2'>
            <Target className='w-[22px] h-[21px] text-[#5e5f5c]' />
          </div>
          <div className='flex flex-col'>
            <span className='text-xs font-medium text-[#5e5f5c] leading-4'>{ui('accuracy')}</span>
            <span className='text-lg font-semibold text-[#121c2a] leading-6'>{accuracyPct}%</span>
          </div>
        </article>
      </section>

      {/* 3. Review & Insights */}
      <section className='flex flex-col gap-4'>
        <div className='flex items-center justify-between flex-wrap gap-3'>
          <h3 className='text-xl md:text-2xl font-heading font-medium text-[#1b1c1a]'>
            {ui('reviewInsights')}</h3>
          <div className='flex items-center gap-2'>
            <div className='inline-flex items-center gap-1.5 px-4 py-2 rounded-full bg-green-100'>
              <svg className='w-[13px] h-[13px] text-green-800' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='3'><polyline points='20 6 9 17 4 12' /></svg>
              <span className='text-xs font-medium text-green-800'>{correctCount} {ui('correct')}</span>
            </div>
            <div className='inline-flex items-center gap-1.5 px-4 py-2 rounded-full bg-red-100'>
              <svg className='w-[13px] h-[13px] text-red-800' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='3'><line x1='18' y1='6' x2='6' y2='18' /><line x1='6' y1='6' x2='18' y2='18' /></svg>
              <span className='text-xs font-medium text-red-800'>{incorrectCount} {ui('incorrect')}</span>
            </div>
          </div>
        </div>

        <div className='flex flex-col gap-3'>
          {review.map((r, index) => (
            <article
              key={index}
              className={`flex flex-col gap-3 p-5 bg-white rounded-2xl border-l-4 shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] ${
                r.isCorrect ? 'border-l-green-600' : 'border-l-red-600'
              }`}
            >
              <div className='flex items-center justify-between gap-4'>
                <div className='flex flex-col gap-1 min-w-0'>
                  <span className='text-xs text-[#5e5f5c] leading-4'>{ui('question')}{' '}{String(index + 1).padStart(2, '0')}</span>
                  <p className='text-base font-medium text-[#1b1c1a] leading-[18px]'>{r.questionText}</p>
                </div>
                <div className='shrink-0'>
                  {r.isCorrect ? (
                    <svg className='w-5 h-5 text-green-800' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='2.5'><path d='M22 11.08V12a10 10 0 11-5.93-9.14' /><polyline points='22 4 12 14.01 9 11.01' /></svg>
                  ) : (
                    <svg className='w-5 h-5 text-red-800' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='2.5'><circle cx='12' cy='12' r='10' /><line x1='15' y1='9' x2='9' y2='15' /><line x1='9' y1='9' x2='15' y2='15' /></svg>
                  )}
                </div>
              </div>
              <div className='flex items-start gap-2'>
                <span className={`text-sm font-medium whitespace-nowrap ${r.isCorrect ? 'text-green-800' : 'text-red-800'}`}>{ui('yourAnswer')}</span>
                <p className='text-sm text-[#5e5f5c] leading-4'>{r.userChoiceText ?? ui('notAnswered')}</p>
              </div>
              {!r.isCorrect && r.correctChoiceText && (
                <div className='flex items-start gap-2'>
                  <span className='text-sm font-medium text-green-800 whitespace-nowrap'>{ui('correctAnswer')}</span>
                  <p className='text-sm text-[#5e5f5c] leading-4'>{r.correctChoiceText}</p>
                </div>
              )}
              {r.explanation && (
                <div className='flex items-start gap-2 mt-1 pt-3 border-t border-blue-50'>
                  <span className='text-sm font-medium text-[#49608f] whitespace-nowrap'>{ui('explanation')}</span>
                  <p className='text-sm text-[#5e5f5c] leading-4'>{r.explanation}</p>
                </div>
              )}
            </article>
          ))}
        </div>
      </section>

      {/* 4. Navigation */}
      <div className='flex items-center justify-between md:justify-end gap-3 py-6 border-t border-[#bec9c233]'>
        <Link
          href='/student/courses'
          className='inline-flex h-10 md:h-11 items-center justify-center gap-2 px-4 md:px-5 py-2.5 bg-white rounded-md border border-solid border-[#3b82f64c] text-sm font-medium text-[#1d4ed8] hover:bg-blue-50 transition-colors'
        >
          {ui('backToCourses')}</Link>
        {canRetry && (
          <button
            type='button'
            onClick={onRetake}
            className='inline-flex h-10 md:h-11 items-center justify-center gap-2 px-4 md:px-5 py-2.5 bg-white rounded-md border border-solid border-[#3b82f64c] text-sm font-medium text-[#1d4ed8] cursor-pointer hover:bg-blue-50 transition-colors'
          >
            <RotateCcw className='w-4 h-4' />
            {ui('retakeQuiz')}</button>
        )}
      </div>
    </div>
  )
}
