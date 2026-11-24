import type { Metadata } from 'next'
import VerifyAccountClient from './VerifyAccountClient'

export const metadata: Metadata = {
  title: 'Verify Account',
  description: 'Verify your Global Islamic Institute account',
}

export default function VerifyAccountPage() {
  return <VerifyAccountClient />
}
