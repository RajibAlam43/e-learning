import type { Metadata } from 'next'
import SignupClient from './SignupClient'

export const metadata: Metadata = {
  title: 'Create Account',
  description: 'Join Global Islamic Institute and start your learning journey',
}

export default function SignupPage() {
  return <SignupClient />
}
