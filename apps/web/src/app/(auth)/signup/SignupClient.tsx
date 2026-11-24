'use client'

import { SignupProvider, useSignupContext } from '@/components/auth/SignupContext'
import { AuthShell } from '@/components/auth/AuthShell'
import { CreateAccountStep } from '@/components/auth/CreateAccountStep'
import { SetupPasswordStep } from '@/components/auth/SetupPasswordStep'
import { VerifyAccountStep } from '@/components/auth/VerifyAccountStep'

function SignupWizard() {
  const { step } = useSignupContext()

  return (
    <AuthShell showFooter={step !== 'verify'}>
      {step === 'create' && <CreateAccountStep />}
      {step === 'password' && <SetupPasswordStep />}
      {step === 'verify' && <VerifyAccountStep />}
    </AuthShell>
  )
}

export default function SignupClient() {
  return (
    <SignupProvider>
      <SignupWizard />
    </SignupProvider>
  )
}
