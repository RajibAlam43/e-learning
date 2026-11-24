import type { Metadata } from 'next'
import ResetPasswordClient from './ResetPasswordClient'

export const metadata: Metadata = {
  title: 'Reset Password',
  description: 'Create a new password for your account',
}

export default function ResetPasswordPage() {
  return <ResetPasswordClient />
}
