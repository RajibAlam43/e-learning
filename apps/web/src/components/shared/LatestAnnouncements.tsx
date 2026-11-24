'use client'

import { useTranslations } from 'next-intl'

import React from 'react'

interface Announcement {
  title: string
  description: string
  time: string
}

interface LatestAnnouncementsProps {
  announcements: Announcement[]
}

const AnnouncementItem = ({ title, description, time }: Announcement) => {
  return (
    <div className='w-full flex gap-3'>
      {/* Dot indicator */}
      <div className='mt-1.5 flex-shrink-0'>
        <svg width='6' height='12' viewBox='0 0 6 12' fill='none' xmlns='http://www.w3.org/2000/svg'>
          <rect y='6' width='6' height='6' rx='3' fill='#3B82F6' />
        </svg>
      </div>

      {/* Content */}
      <div className='flex-1 flex flex-col gap-1'>
        <h4 className='text-xs font-medium font-switzer leading-4 text-[#1c3873]'>
          {title}
        </h4>
        <p className='text-[10px] font-normal font-inter leading-4 text-[#5e5f5c]'>
          {description}
        </p>
        <span className='text-[10px] font-semibold font-inter leading-4 text-[#5e5f5c]'>
          {time}
        </span>
      </div>
    </div>
  )
}

const LatestAnnouncements = ({ announcements }: LatestAnnouncementsProps) => {
  const ui = useTranslations('privateUi')

  return (
    <div className='flex-1 flex flex-col gap-4'>
      <h2 className='text-lg font-semibold font-switzer leading-6 text-[#1c3873]'>
        {ui('latestAnnouncements')}</h2>
      <div className='w-full p-6 bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] flex flex-col gap-6'>
        {announcements.length === 0 && <p className='text-sm text-[#5e5f5c]'>{ui('noAnnouncementsYet2')}</p>}
        {announcements.map((item) => (
          <AnnouncementItem key={item.title} {...item} />
        ))}
      </div>
    </div>
  )
}

export default LatestAnnouncements
