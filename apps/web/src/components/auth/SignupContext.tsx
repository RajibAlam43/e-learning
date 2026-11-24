'use client'

import { createContext, useContext, useState, type ReactNode } from 'react'

export type SignupChannel = 'EMAIL' | 'PHONE'

export interface SignupFormData {
  fullname: string
  channel: SignupChannel
  phone: string
  email: string
  password: string
  confirmPassword: string
  otp: string
  agreedToTerms: boolean
}

export type SignupStep = 'create' | 'password' | 'verify'

interface SignupContextValue {
  step: SignupStep
  formData: SignupFormData
  goToStep: (step: SignupStep) => void
  updateField: <K extends keyof SignupFormData>(key: K, value: SignupFormData[K]) => void
}

const initialFormData: SignupFormData = {
  fullname: '',
  channel: 'EMAIL',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  otp: '',
  agreedToTerms: false,
}

const SignupContext = createContext<SignupContextValue | null>(null)

export function SignupProvider({ children }: { children: ReactNode }) {
  const [step, setStep] = useState<SignupStep>('create')
  const [formData, setFormData] = useState<SignupFormData>(initialFormData)

  const updateField = <K extends keyof SignupFormData>(key: K, value: SignupFormData[K]) => {
    setFormData((prev) => ({ ...prev, [key]: value }))
  }

  const goToStep = (s: SignupStep) => setStep(s)

  return (
    <SignupContext.Provider value={{ step, formData, goToStep, updateField }}>
      {children}
    </SignupContext.Provider>
  )
}

export function useSignupContext() {
  const ctx = useContext(SignupContext)
  if (!ctx) throw new Error('useSignupContext must be used inside SignupProvider')
  return ctx
}
