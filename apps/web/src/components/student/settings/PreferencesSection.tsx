'use client'

import { useTranslations } from 'next-intl'

import { useProfile, useUpdateProfile } from '@/hooks/useStudentDashboard'
import { Globe, Languages, ChevronRight } from 'lucide-react'

export default function PreferencesSection() {
  const ui = useTranslations('privateUi')

  const { data: profile } = useProfile()
  const updateProfile = useUpdateProfile()

  const locale = (profile as Record<string, unknown>)?.locale as string ?? 'en'
  const timezone = (profile as Record<string, unknown>)?.timezone as string ?? 'Asia/Dhaka'

  const handleUpdate = (field: string, value: string) => {
    updateProfile.mutate({ [field]: value })
  }

  return (
    <section className='bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden'>
      <div className='p-4 md:p-6 border-b border-gray-100'>
        <div className='flex items-center gap-3'>
          <div className='w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center'>
            <Globe className='w-5 h-5 text-[#3b82f6]' />
          </div>
          <div>
            <h2 className='text-base font-medium font-switzer text-[#1c3873]'>{ui('preferences')}</h2>
            <p className='text-sm text-[#5e5f5c]'>{ui('languageAndDisplaySettings')}</p>
          </div>
        </div>
      </div>
      <div className='p-4 md:p-6 flex flex-col gap-5'>
        <div className='flex flex-col gap-1'>
          <label className='text-sm font-semibold text-[#1b1c1a]'>{ui('language')}</label>
          <div className='relative'>
            <Languages className='absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#667085]' />
            <select
              value={locale}
              onChange={(e) => handleUpdate('locale', e.target.value)}
              className='w-full h-11 pl-10 pr-4 rounded-lg border border-[#e5e7eb] bg-white text-sm text-[#5e5f5c] focus:outline-none focus:border-[#1c3873] transition-colors appearance-none cursor-pointer'
            >
              <option value='en'>{ui('english')}</option>
              <option value='bn'>{ui('bangla')}</option>
              <option value='ar'>{ui('arabic')}</option>
            </select>
            <ChevronRight className='absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#667085] rotate-90 pointer-events-none' />
          </div>
        </div>

        <div className='flex flex-col gap-1'>
          <label className='text-sm font-semibold text-[#1b1c1a]'>{ui('timezone')}</label>
          <div className='relative'>
            <Globe className='absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#667085]' />
            <select
              value={timezone}
              onChange={(e) => handleUpdate('timezone', e.target.value)}
              className='w-full h-11 pl-10 pr-4 rounded-lg border border-[#e5e7eb] bg-white text-sm text-[#5e5f5c] focus:outline-none focus:border-[#1c3873] transition-colors appearance-none cursor-pointer'
            >
              <option value='Asia/Dhaka'>{ui('asiaDhakaGmt6')}</option>
              <option value='Asia/Kolkata'>{ui('asiaKolkataGmt530')}</option>
              <option value='Asia/Riyadh'>{ui('asiaRiyadhGmt3')}</option>
              <option value='Europe/London'>{ui('europeLondonGmt0')}</option>
              <option value='America/New_York'>{ui('americaNewYorkGmt5')}</option>
            </select>
            <ChevronRight className='absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#667085] rotate-90 pointer-events-none' />
          </div>
        </div>
      </div>
    </section>
  )
}
