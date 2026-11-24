import type { Metadata } from 'next'
import LoginClient from './LoginClient'

export const metadata: Metadata = {
  title: 'Log In',
  description: 'Sign in to your Global Islamic Institute account',
}

export default function LoginPage() {
  return <LoginClient />
}
