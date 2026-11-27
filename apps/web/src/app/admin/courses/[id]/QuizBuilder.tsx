'use client'

import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import type { CreateQuizRequest } from '@/types'

type Question = CreateQuizRequest['questions'][number]
type Choice = Question['choices'][number]

const MIN_CHOICES = 2
const MAX_CHOICES = 10

function blankChoices(): Choice[] {
  return [
    { choiceText: '', choiceTextEn: '', isCorrect: true },
    { choiceText: '', choiceTextEn: '', isCorrect: false },
  ]
}

export function createQuizQuestion(position = 1): Question {
  return {
    position,
    questionText: '',
    questionTextEn: '',
    questionType: 'MCQ',
    points: 1,
    choices: blankChoices(),
  }
}

export function QuizBuilder({
  value,
  onChange,
  saving,
  onSubmit,
  positionAvailable,
  submitLabel = 'Create quiz',
}: {
  value: CreateQuizRequest
  onChange: (value: CreateQuizRequest) => void
  saving: boolean
  onSubmit: (event: React.FormEvent) => void
  positionAvailable: boolean
  submitLabel?: string
}) {
  const updateQuestion = (index: number, patch: Partial<Question>) => {
    onChange({
      ...value,
      questions: value.questions.map((question, questionIndex) =>
        questionIndex === index ? { ...question, ...patch } : question,
      ),
    })
  }

  const addQuestion = () => {
    onChange({
      ...value,
      questions: [...value.questions, createQuizQuestion(value.questions.length + 1)],
    })
  }

  const removeQuestion = (index: number) => {
    onChange({
      ...value,
      questions: value.questions
        .filter((_, questionIndex) => questionIndex !== index)
        .map((question, questionIndex) => ({ ...question, position: questionIndex + 1 })),
    })
  }

  const setQuestionType = (index: number, question: Question, questionType: string) => {
    updateQuestion(index, {
      questionType,
      choices:
        questionType === 'TRUE_FALSE'
          ? [
              { choiceText: 'সত্য', choiceTextEn: 'True', isCorrect: true },
              { choiceText: 'মিথ্যা', choiceTextEn: 'False', isCorrect: false },
            ]
          : question.questionType === 'TRUE_FALSE'
            ? blankChoices()
            : question.choices,
    })
  }

  const updateChoice = (questionIndex: number, question: Question, choiceIndex: number, patch: Partial<Choice>) => {
    updateQuestion(questionIndex, {
      choices: question.choices.map((choice, index) =>
        index === choiceIndex ? { ...choice, ...patch } : choice,
      ),
    })
  }

  const selectCorrectChoice = (questionIndex: number, question: Question, choiceIndex: number) => {
    updateQuestion(questionIndex, {
      choices: question.choices.map((choice, index) => ({
        ...choice,
        isCorrect: index === choiceIndex,
      })),
    })
  }

  const addChoice = (questionIndex: number, question: Question) => {
    if (question.questionType === 'TRUE_FALSE' || question.choices.length >= MAX_CHOICES) return
    updateQuestion(questionIndex, {
      choices: [...question.choices, { choiceText: '', choiceTextEn: '', isCorrect: false }],
    })
  }

  const removeChoice = (questionIndex: number, question: Question, choiceIndex: number) => {
    if (question.questionType === 'TRUE_FALSE' || question.choices.length <= MIN_CHOICES) return
    const choices = question.choices.filter((_, index) => index !== choiceIndex)
    if (!choices.some(choice => choice.isCorrect) && choices[0]) choices[0] = { ...choices[0], isCorrect: true }
    updateQuestion(questionIndex, { choices })
  }

  return (
    <form onSubmit={onSubmit} className="grid gap-5">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <FormField label="Quiz title (Bangla)" required value={value.title} onChange={title => onChange({ ...value, title })} className="lg:col-span-2" />
        <FormField label="Quiz title (English)" value={value.titleEn ?? ''} onChange={titleEn => onChange({ ...value, titleEn })} />
        <FormField label="Position" type="number" min={1} required value={String(value.position)} onChange={position => onChange({ ...value, position: numberValue(position, 1) })} error={!positionAvailable ? 'This position is already used in the section.' : undefined} />
        <FormField label="Passing score (%)" type="number" min={1} max={100} required value={String(value.passingScorePct)} onChange={passingScorePct => onChange({ ...value, passingScorePct: Math.min(100, numberValue(passingScorePct, 1)) })} />
        <FormField label="Maximum attempts" type="number" min={1} required value={String(value.maxAttempts)} onChange={maxAttempts => onChange({ ...value, maxAttempts: numberValue(maxAttempts, 1) })} />
        <FormField label="Time limit (seconds)" type="number" min={1} value={String(value.timeLimitSec ?? '')} onChange={timeLimitSec => onChange({ ...value, timeLimitSec: timeLimitSec ? numberValue(timeLimitSec, 1) : undefined })} />
      </div>

      <div className="flex items-center justify-between gap-3">
        <h3 className="font-semibold text-[#1c3873]">Questions ({value.questions.length})</h3>
        <button type="button" onClick={addQuestion} className="inline-flex h-10 items-center gap-2 rounded-lg border border-slate-200 px-3 text-sm font-semibold text-[#1c3873] hover:bg-slate-50">
          <Plus className="h-4 w-4" />Add question
        </button>
      </div>

      {value.questions.map((question, questionIndex) => (
        <section key={questionIndex} className="grid gap-4 rounded-lg border border-slate-200 p-4">
          <header className="flex items-center justify-between gap-3">
            <strong className="text-sm text-[#1c3873]">Question {questionIndex + 1}</strong>
            {value.questions.length > 1 && (
              <button type="button" onClick={() => removeQuestion(questionIndex)} className="rounded-md p-2 text-red-600 hover:bg-red-50" aria-label={`Remove question ${questionIndex + 1}`} title="Remove question">
                <Trash2 className="h-4 w-4" />
              </button>
            )}
          </header>

          <QuizTextArea label="Question (Bangla)" required value={question.questionText} onChange={questionText => updateQuestion(questionIndex, { questionText })} />
          <QuizTextArea label="Question (English)" value={question.questionTextEn ?? ''} onChange={questionTextEn => updateQuestion(questionIndex, { questionTextEn })} />

          <div className="grid gap-4 sm:grid-cols-2">
            <SelectField label="Question type" value={question.questionType} onChange={questionType => setQuestionType(questionIndex, question, questionType)} options={[['MCQ', 'Multiple choice'], ['TRUE_FALSE', 'True / False']]} />
            <FormField label="Points" type="number" min={1} required value={String(question.points)} onChange={points => updateQuestion(questionIndex, { points: numberValue(points, 1) })} />
          </div>

          <div className="grid gap-3">
            <div className="flex items-center justify-between gap-3">
              <h4 className="text-sm font-semibold text-slate-700">Answer options ({question.choices.length})</h4>
              {question.questionType !== 'TRUE_FALSE' && (
                <button type="button" disabled={question.choices.length >= MAX_CHOICES} onClick={() => addChoice(questionIndex, question)} className="inline-flex h-9 items-center gap-2 rounded-lg border border-slate-200 px-3 text-xs font-semibold text-[#1c3873] hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">
                  <Plus className="h-3.5 w-3.5" />Add option
                </button>
              )}
            </div>

            {question.choices.map((choice, choiceIndex) => (
              <div key={choiceIndex} className="grid grid-cols-[24px_minmax(0,1fr)_36px] items-start gap-2 sm:grid-cols-[24px_minmax(0,1fr)_minmax(0,1fr)_36px]">
                <input type="radio" name={`question-${questionIndex}-correct`} checked={choice.isCorrect === true} onChange={() => selectCorrectChoice(questionIndex, question, choiceIndex)} className="mt-3.5 h-4 w-4 accent-[#1c3873]" aria-label={`Mark option ${choiceIndex + 1} as correct`} />
                <ChoiceInput required readOnly={question.questionType === 'TRUE_FALSE'} value={choice.choiceText} onChange={choiceText => updateChoice(questionIndex, question, choiceIndex, { choiceText })} placeholder={`Option ${choiceIndex + 1} (Bangla)`} />
                <ChoiceInput readOnly={question.questionType === 'TRUE_FALSE'} value={choice.choiceTextEn ?? ''} onChange={choiceTextEn => updateChoice(questionIndex, question, choiceIndex, { choiceTextEn })} placeholder={`Option ${choiceIndex + 1} (English)`} className="col-start-2 sm:col-start-auto" />
                <button type="button" disabled={question.questionType === 'TRUE_FALSE' || question.choices.length <= MIN_CHOICES} onClick={() => removeChoice(questionIndex, question, choiceIndex)} className="row-start-1 rounded-md p-2 text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-30 sm:col-start-4" aria-label={`Remove option ${choiceIndex + 1}`} title="Remove option">
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </section>
      ))}

      <button disabled={saving || !positionAvailable} className="inline-flex h-11 items-center justify-center rounded-lg bg-[#1c3873] px-5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50">
        {saving ? 'Saving...' : submitLabel}
      </button>
    </form>
  )
}

function FormField({ label, value, onChange, type = 'text', min, max, required, error, className = '' }: { label: string; value: string; onChange: (value: string) => void; type?: string; min?: number; max?: number; required?: boolean; error?: string; className?: string }) {
  const [validationError, setValidationError] = useState('')
  const visibleError = error || validationError
  return <label className={`grid content-start gap-1 text-sm font-medium text-slate-700 ${className}`}><span>{label}{required && <span className="ml-0.5 text-red-600" aria-hidden="true">*</span>}</span><input required={required} type={type} min={min} max={max} value={value} aria-invalid={Boolean(visibleError)} onInvalid={event => { event.preventDefault(); setValidationError(event.currentTarget.validationMessage) }} onChange={event => { onChange(event.target.value); if (validationError) setValidationError(event.currentTarget.validity.valid ? '' : event.currentTarget.validationMessage) }} className={`h-10 min-w-0 rounded-lg border px-3 outline-none transition-colors focus:ring-2 ${visibleError ? 'border-red-500 focus:border-red-500 focus:ring-red-100' : 'border-slate-200 focus:border-[#1c3873] focus:ring-[#1c3873]/10'}`} />{visibleError && <span role="alert" className="text-xs font-normal text-red-600">{visibleError}</span>}</label>
}

function QuizTextArea({ label, value, onChange, required }: { label: string; value: string; onChange: (value: string) => void; required?: boolean }) {
  const [validationError, setValidationError] = useState('')
  return <label className="grid gap-1 text-sm font-medium text-slate-700"><span>{label}{required && <span className="ml-0.5 text-red-600" aria-hidden="true">*</span>}</span><textarea required={required} rows={3} value={value} aria-invalid={Boolean(validationError)} onInvalid={event => { event.preventDefault(); setValidationError(event.currentTarget.validationMessage) }} onChange={event => { onChange(event.target.value); if (validationError) setValidationError(event.currentTarget.validity.valid ? '' : event.currentTarget.validationMessage) }} className={`min-h-24 resize-y rounded-lg border px-3 py-2 outline-none transition-colors focus:ring-2 ${validationError ? 'border-red-500 focus:border-red-500 focus:ring-red-100' : 'border-slate-200 focus:border-[#1c3873] focus:ring-[#1c3873]/10'}`} />{validationError && <span role="alert" className="text-xs font-normal text-red-600">{validationError}</span>}</label>
}

function SelectField({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: [string, string][] }) {
  return <label className="grid content-start gap-1 text-sm font-medium text-slate-700"><span>{label}</span><select value={value} onChange={event => onChange(event.target.value)} className="h-10 min-w-0 rounded-lg border border-slate-200 px-3 outline-none focus:border-[#1c3873] focus:ring-2 focus:ring-[#1c3873]/10">{options.map(([key, text]) => <option key={key} value={key}>{text}</option>)}</select></label>
}

function ChoiceInput({ value, onChange, placeholder, required, readOnly, className = '' }: { value: string; onChange: (value: string) => void; placeholder: string; required?: boolean; readOnly?: boolean; className?: string }) {
  const [validationError, setValidationError] = useState('')
  return <div className={`grid min-w-0 gap-1 ${className}`}><input required={required} readOnly={readOnly} value={value} placeholder={placeholder} aria-label={placeholder} aria-invalid={Boolean(validationError)} onInvalid={event => { event.preventDefault(); setValidationError(event.currentTarget.validationMessage) }} onChange={event => { onChange(event.target.value); if (validationError) setValidationError(event.currentTarget.validity.valid ? '' : event.currentTarget.validationMessage) }} className={`h-10 min-w-0 rounded-lg border px-3 text-sm outline-none transition-colors focus:ring-2 read-only:bg-slate-50 ${validationError ? 'border-red-500 focus:border-red-500 focus:ring-red-100' : 'border-slate-200 focus:border-[#1c3873] focus:ring-[#1c3873]/10'}`} />{validationError && <span role="alert" className="text-xs text-red-600">{validationError}</span>}</div>
}

function numberValue(value: string, fallback: number) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}
