import type { Metadata } from 'next'
import AdminShell from './AdminShell'

/**
 * Admin route group — server component so we can export `metadata`.
 * Client-side sidebar/header/auth logic lives in AdminShell.
 */
export const metadata: Metadata = {
  title: { default: 'Admin · GII', template: '%s · Admin · GII' },
  description: 'Admin dashboard for the GII LMS platform.',
  robots: { index: false, follow: false },
}

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <AdminShell>{children}</AdminShell>
}
