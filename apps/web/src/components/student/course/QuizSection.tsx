'use client'

import { useTranslations } from 'next-intl'

import { useState, useId } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Breadcrumb } from '@/components/shared/Breadcrumb'

export interface QuizQuestion {
  id: string
  question: string
  options: { id: string; text: string }[]
  correctOptionId: string
}

interface QuizSectionProps {
  questions: QuizQuestion[]
  quizTitle?: string
  courseName: string
  moduleName: string
  categoryName: string
  onComplete: (answers: Record<string, string>) => void
  isSubmitting?: boolean
  answers: Record<string, string>
  onAnswersChange: (answers: Record<string, string>) => void
  locked?: boolean
}

export default function QuizSection({ questions, quizTitle, courseName, moduleName, categoryName, onComplete, isSubmitting = false, answers, onAnswersChange, locked = false }: QuizSectionProps) {
  const ui = useTranslations('privateUi')

  const [currentIndex, setCurrentIndex] = useState(0)
  const [selectionError, setSelectionError] = useState(false)
  const radioName = useId()

  const current = questions[currentIndex]
  const total = questions.length
  const completedCount = Object.keys(answers).length
  const progressPct = (completedCount / total) * 100
  const selectedAnswer = current ? (answers[current.id] ?? '') : ''

  const handleSelect = (optionId: string) => {
    if (!current || locked || isSubmitting) return
    setSelectionError(false)
    onAnswersChange({ ...answers, [current.id]: optionId })
  }

  const goNext = () => {
    if (!selectedAnswer) {
      setSelectionError(true)
      return
    }
    if (currentIndex < total - 1) {
      setCurrentIndex((i) => i + 1)
    } else {
      onComplete(answers)
    }
  }

  const goPrev = () => {
    if (currentIndex > 0) setCurrentIndex((i) => i - 1)
  }

  if (!current) return null

  return (
    <div className='flex flex-col gap-6'>
      {/* Breadcrumb */}
      <Breadcrumb items={[
        { label: ui('myCourses'), href: '/student/courses' },
        { label: courseName },
        { label: moduleName },
      ]} />

      {/* Title */}
      <h1 className='text-xl md:text-[28px] font-heading font-medium text-[#1c3873] leading-7 md:leading-9'>
        {quizTitle || `${moduleName} : Knowledge Check`}
      </h1>

      {/* Main card */}
      <section className='flex flex-col gap-6 md:gap-8 p-4 md:p-8 bg-white rounded-2xl border border-solid border-blue-100'>
        {/* Progress */}
        <div className='flex flex-col gap-3 self-stretch'>
          <div className='flex items-end justify-between gap-2'>
            <p className='text-sm md:text-base leading-[18px]'>
              <span className='text-[#5e5f5c]'>{ui('question2')}{' '}</span>
              <span className='text-[#1c3873]'>{currentIndex + 1}</span>
              <span className='text-[#5e5f5c]'> {ui('of')}{' '}{total}</span>
            </p>
            <p className='text-sm md:text-base text-[#5e5f5c] leading-[18px] opacity-60 whitespace-nowrap'>
              {Math.round(progressPct)}{ui('completed')}</p>
          </div>
          <div
            className='h-2 bg-blue-100 rounded-full overflow-hidden'
            role='progressbar'
            aria-valuenow={progressPct}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <div className='h-full bg-[#3b82f6] transition-all' style={{ width: `${progressPct}%` }} />
          </div>
        </div>

        {/* Category badge + Question */}
        <div className='flex flex-col gap-4'>
          <div className='bg-[#f4f9ff] rounded-md px-2 py-1 inline-flex'>
            <span className='text-base text-[#49608f] leading-[18px]'>{categoryName}</span>
          </div>
          <h2 className='text-lg md:text-2xl font-heading font-medium text-[#1b1c1a] leading-7 md:leading-8'>
            {current.question}
          </h2>
        </div>

        {/* Divider */}
        <div className='h-px bg-blue-100' />

        {/* Answer options */}
        <form className='w-full'>
          <fieldset disabled={locked || isSubmitting} className='grid gap-4'>
            <legend className='sr-only'>{ui('selectOneAnswer')}</legend>
            {current.options.map((option) => (
              <label
                key={option.id}
                className='flex items-center min-h-[60px] md:min-h-[74px] p-4 md:p-6 bg-[#f4f9ff] rounded-xl border border-solid border-blue-100 cursor-pointer hover:border-blue-200 transition-colors'
              >
                <input
                  checked={selectedAnswer === option.id}
                  className='sr-only'
                  name={radioName}
                  onChange={() => handleSelect(option.id)}
                  type='radio'
                  value={option.id}
                />
                <div className='w-10 h-6 flex items-start shrink-0 pr-4'>
                  <div
                    className={`w-6 h-6 rounded-full border-2 border-solid flex items-center justify-center ${
                      selectedAnswer === option.id ? 'border-[#3b82f6]' : 'border-blue-300'
                    }`}
                  >
                    {selectedAnswer === option.id && (
                      <div className='w-3 h-3 rounded-full bg-[#3b82f6]' />
                    )}
                  </div>
                </div>
                <p className='text-sm md:text-base text-[#5e5f5c] leading-snug md:leading-[18px]'>{option.text}</p>
              </label>
            ))}
          </fieldset>
          {selectionError && (
            <p role='alert' className='mt-3 text-sm text-red-600'>{ui('selectAnAnswerBeforeContinuing')}</p>
          )}
        </form>
      </section>

      {/* Footer navigation */}
      <div className='flex items-center justify-between md:justify-end gap-3 md:gap-4 py-4 md:py-6 border-t border-[#bec9c233]'>
        <button
          type='button'
          onClick={goPrev}
          disabled={currentIndex === 0 || isSubmitting}
          className='inline-flex h-10 md:h-11 items-center justify-center gap-1.5 md:gap-2 px-3 md:px-5 py-2.5 bg-white rounded-md border border-solid border-[#3b82f64c] cursor-pointer disabled:opacity-40'
        >
          <ChevronLeft className='w-5 h-5 md:w-6 md:h-6 text-[#3b82f6]' />
          <span className='text-xs md:text-sm font-medium text-[#3b82f6]'>{ui('previous')}</span>
        </button>
        <button
          type='button'
          onClick={goNext}
          disabled={isSubmitting || locked}
          className='inline-flex h-10 md:h-11 items-center justify-center gap-1.5 md:gap-2 px-3 md:px-5 py-2.5 rounded-md shadow-[inset_0px_1px_0px_#ffffff1f,inset_0px_-1px_0px_#0000002e] bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] cursor-pointer disabled:cursor-wait disabled:opacity-60'
        >
          <span className='text-xs md:text-sm font-medium text-white'>
            {isSubmitting ? ui('submitting2') : currentIndex === total - 1 ? ui('submit') : ui('nextQuestion')}
          </span>
          {currentIndex < total - 1 && <ChevronRight className='w-5 h-5 md:w-6 md:h-6 text-white' />}
        </button>
      </div>
    </div>
  )
}
