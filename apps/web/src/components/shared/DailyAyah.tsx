'use client'

import { useTranslations } from 'next-intl'

import React from 'react'

interface DailyAyahProps {
  ayahText?: string
  translation?: string
  reference?: string
  isLoading?: boolean
}

const DailyAyah = ({
  ayahText = 'إِنَّ مَعَ الْعُسْرِ يُسْرًا',
  translation,
  reference,
  isLoading,
}: DailyAyahProps) => {
  const ui = useTranslations('privateUi')

  if (isLoading) {
    return (
      <div className='w-full p-6 bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] animate-pulse'>
        <div className='flex flex-col items-center gap-4'>
          <div className='h-3 w-20 bg-gray-100 rounded' />
          <div className='h-7 w-48 bg-gray-100 rounded' />
        </div>
      </div>
    )
  }

  return (
    <div className='group w-full p-6 relative bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] flex flex-col items-center gap-4 overflow-hidden cursor-default'>
      {/* Blur glow */}
      <div className='absolute w-28 h-20 -left-[30px] -top-[24px] bg-blue-500/20 rounded-full blur-[28px]' />

      {/* Label */}
      <div className='flex flex-col items-center gap-2'>
        <svg width='22' height='15' viewBox='0 0 22 15' fill='none' xmlns='http://www.w3.org/2000/svg'>
          <path
            d='M2.125 15L5 10C3.625 10 2.44792 9.51042 1.46875 8.53125C0.489583 7.55208 0 6.375 0 5C0 3.625 0.489583 2.44792 1.46875 1.46875C2.44792 0.489583 3.625 0 5 0C6.375 0 7.55208 0.489583 8.53125 1.46875C9.51042 2.44792 10 3.625 10 5C10 5.47917 9.94271 5.92188 9.82812 6.32812C9.71354 6.73438 9.54167 7.125 9.3125 7.5L5 15H2.125ZM13.375 15L16.25 10C14.875 10 13.6979 9.51042 12.7188 8.53125C11.7396 7.55208 11.25 6.375 11.25 5C11.25 3.625 11.7396 2.44792 12.7188 1.46875C13.6979 0.489583 14.875 0 16.25 0C17.625 0 18.8021 0.489583 19.7812 1.46875C20.7604 2.44792 21.25 3.625 21.25 5C21.25 5.47917 21.1927 5.92188 21.0781 6.32812C20.9635 6.73438 20.7917 7.125 20.5625 7.5L16.25 15H13.375Z'
            fill='#A3A3A3'
          />
        </svg>
        <span className='text-[10px] font-medium font-switzer leading-4 text-[#5e5f5c]'>{ui('dailyAyah')}</span>
      </div>

      {/* Arabic Ayah */}
      <p className='w-full text-center text-2xl font-medium leading-7 text-[#1c3873] font-arial'>
        {ayahText}
      </p>

      {/* Translation — hidden by default, slides in on hover with 80px gap */}
      <div className='w-full flex flex-col items-center gap-2 max-h-0 opacity-0 translate-y-[-10px] transition-all duration-700 ease-[cubic-bezier(0.25,0.1,0.25,1)] group-hover:max-h-40 group-hover:opacity-100 group-hover:translate-y-0 mt-0 group-hover:mt-8'>
        <p className='w-full text-center text-sm font-normal font-switzer leading-5 text-[#5e5f5c] italic'>
          {translation ?? ui('easeWithHardship')}
        </p>
        <span className='text-[10px] font-semibold font-inter leading-4 text-[#5e5f5c]'>
          {reference ?? ui('quranReference')}
        </span>
      </div>
    </div>
  )
}

export default DailyAyah
