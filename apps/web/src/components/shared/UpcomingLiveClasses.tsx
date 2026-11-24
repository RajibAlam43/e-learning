'use client'

import { useTranslations } from 'next-intl'

import React from 'react'

interface LiveClass {
  title: string
  instructor: string
  day: string
  time: string
  isToday?: boolean
}

interface UpcomingLiveClassesProps {
  classes: LiveClass[]
  isLoading?: boolean
}

const LiveClassItem = ({ title, instructor, day, time, isToday }: LiveClass) => {
  return (
    <div
      className={`w-full p-4 bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] flex items-center gap-4 transition-all duration-200 hover:shadow-[0px_4px_12px_0px_rgba(20,26,184,0.10)] ${
        isToday ? '' : 'opacity-60'
      }`}
    >
      {/* Icon */}
      <div className={`size-10 rounded-lg flex items-center justify-center ${isToday ? 'bg-blue-50' : 'bg-gray-50'}`}>
        <svg width='17' height='14' viewBox='0 0 17 14' fill='none' xmlns='http://www.w3.org/2000/svg'>
          <path
            d='M5.83333 10H7.5V7.5H10V5.83333H7.5V3.33333H5.83333V5.83333H3.33333V7.5H5.83333V10ZM1.66667 13.3333C1.20833 13.3333 0.815972 13.1701 0.489583 12.8438C0.163194 12.5174 0 12.125 0 11.6667V1.66667C0 1.20833 0.163194 0.815972 0.489583 0.489583C0.815972 0.163194 1.20833 0 1.66667 0H11.6667C12.125 0 12.5174 0.163194 12.8438 0.489583C13.1701 0.815972 13.3333 1.20833 13.3333 1.66667V5.41667L16.6667 2.08333V11.25L13.3333 7.91667V11.6667C13.3333 12.125 13.1701 12.5174 12.8438 12.8438C12.5174 13.1701 12.125 13.3333 11.6667 13.3333H1.66667Z'
            fill={isToday ? '#3B82F6' : '#71717A'}
          />
        </svg>
      </div>

      {/* Info */}
      <div className='flex-1 flex flex-col gap-1'>
        <span className={`text-xs font-medium font-switzer leading-4 ${isToday ? 'text-[#1c3873]' : 'text-[#1c3873]'}`}>
          {title}
        </span>
        <span className='text-[10px] font-normal font-inter leading-4 text-[#5e5f5c]'>
          {instructor}
        </span>
      </div>

      {/* Time */}
      <div className='flex flex-col items-end gap-1'>
        <span className={`text-[10px] font-bold font-inter leading-4 uppercase ${isToday ? 'text-[#1c3873]' : 'text-[#1c3873]'}`}>
          {day}
        </span>
        <span className='text-[10px] font-normal font-inter leading-4 text-[#5e5f5c]'>
          {time}
        </span>
      </div>
    </div>
  )
}

const UpcomingLiveClasses = ({ classes, isLoading }: UpcomingLiveClassesProps) => {
  const ui = useTranslations('privateUi')

  if (isLoading) {
    return (
      <div className='flex-1 flex flex-col gap-4'>
        <div className='h-6 w-48 bg-gray-100 rounded animate-pulse' />
        <div className='flex flex-col gap-3'>
          {[1, 2].map((i) => (
            <div key={i} className='h-[72px] rounded-2xl bg-gray-100 animate-pulse' />
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className='flex-1 flex flex-col gap-4'>
      <h2 className='text-lg font-semibold font-switzer leading-6 text-[#1c3873]'>
        {ui('upcomingLiveClasses')}</h2>
      <div className='w-full flex flex-col gap-3'>
        {classes.map((cls) => (
          <LiveClassItem key={cls.title} {...cls} />
        ))}
      </div>
    </div>
  )
}

export default UpcomingLiveClasses
