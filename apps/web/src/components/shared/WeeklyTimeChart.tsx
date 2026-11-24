'use client'

import React from 'react'
import { useTranslations } from 'next-intl'

interface WeeklyTimeChartProps {
  title?: string
  data?: { day: string; hours: number }[]
  isLoading?: boolean
}

const defaultData = [
  { day: 'SAT', hours: 0 },
  { day: 'SUN', hours: 0 },
  { day: 'MON', hours: 0 },
  { day: 'TUE', hours: 0 },
  { day: 'WED', hours: 0 },
  { day: 'THU', hours: 0 },
  { day: 'FRI', hours: 0 },
]

const WeeklyTimeChart = ({ title, data = defaultData, isLoading }: WeeklyTimeChartProps) => {
  const ui = useTranslations('privateUi')
  if (isLoading) {
    return (
      <div className='w-full p-6 bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] animate-pulse'>
        <div className='h-4 w-36 bg-gray-100 rounded mb-6' />
        <div className='h-48 w-full bg-gray-100 rounded' />
      </div>
    )
  }

  const maxHours = 4

  return (
    <div className='w-full p-6 bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] flex flex-col justify-start items-start gap-6'>
      <h3 className='text-base font-semibold font-switzer leading-4 text-[#5e5f5c]'>{title ?? ui('timeSpentThisWeek')}</h3>

      <div className='relative w-full h-48'>
        {/* Grid — Y-axis with dashed lines */}
        <div className='absolute w-64 h-44 left-0 top-0 flex flex-col justify-between'>
          {[4, 3, 2, 1, 0].map((h) => (
            <div key={h} className='flex items-center gap-2'>
              <span className='text-right text-[10px] font-inter leading-4 text-[#5e5f5c]'>
                {h === 0 ? '0' : ui('hourCount', { count: h })}
              </span>
              <div className='flex-1 h-px' style={{
                border: h === 0 ? 'none' : undefined,
                backgroundImage: h === 0
                  ? undefined
                  : 'repeating-linear-gradient(90deg, #F5F5F5 0 4px, transparent 4px 11px)',
              }} />
            </div>
          ))}
        </div>

        {/* Bars */}
        <div className='absolute w-60 h-48 left-[24px] top-[4px] flex justify-between items-end'>
          {data.map((item) => {
            const fillHeight = maxHours > 0 ? Math.round((item.hours / maxHours) * 176) : 0
            return (
              <div key={item.day} className='w-9 flex flex-col justify-end items-center gap-1'>
                <div className='w-2 flex flex-col justify-end items-center gap-1.5' style={{ height: '176px' }}>
                  <div className='w-full bg-blue-100 rounded-[2px]' style={{ height: `${176 - fillHeight}px` }} />
                  <div
                    className='w-full bg-blue-500 rounded-t-[2px]'
                    style={{ height: `${fillHeight}px` }}
                  />
                </div>
                <span className='text-[10px] font-normal font-inter leading-4 text-[#5e5f5c]'>{ui.has(`weekdays.${item.day}`) ? ui(`weekdays.${item.day}`) : item.day}</span>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}

export default WeeklyTimeChart
