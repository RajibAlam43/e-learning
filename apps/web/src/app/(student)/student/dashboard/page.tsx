'use client'

import dynamic from 'next/dynamic'

// Disable SSR for the Dashboard to prevent hydration mismatches with the offline IndexedDB cache
const DashboardComponent = dynamic(() => import('./DashboardComponent'), { 
  ssr: false 
})

export default function DashboardPage() {
  return <DashboardComponent />
}
