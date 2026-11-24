'use client'

import { Megaphone } from 'lucide-react'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { EmptyState } from '@/components/shared/EmptyState'
import { Button } from '@/components/ui/button'

export default function AnnouncementsPage() {
  return (
    <div className="mx-auto flex w-full max-w-[1152px] flex-col gap-6 pb-10">
      <AdminPageHeader title="Announcements" action={<Button disabled>Create Announcement</Button>} />
      <EmptyState
        icon={<Megaphone className="h-10 w-10" />}
        title="Platform announcements are unavailable"
        description="Platform-wide announcements are not available yet. Course announcements remain available to instructors."
      />
    </div>
  )
}
