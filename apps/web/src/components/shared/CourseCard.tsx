import Image from 'next/image'
import Link from 'next/link'
import React from 'react'

interface CourseCardProps {
  title: string
  thumbnail: string
  href: string
  nextModule?: string
  progress?: number
  lessonsLeft?: number
  badge?: {
    text: string
    variant: 'popular' | 'short'
  }
}

const CourseCard = ({
  title,
  thumbnail,
  href,
  nextModule,
  progress = 0,
  lessonsLeft = 0,
  badge,
}: CourseCardProps) => {
  const barPercent = Math.min(Math.max(progress, 0), 100)

  return (
    <div className='flex-1 bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] inline-flex flex-col justify-start items-start overflow-hidden transition-all duration-300 ease-[cubic-bezier(0.4,0,0.2,1)] hover:-translate-y-1 hover:shadow-[0px_8px_20px_0px_rgba(20,26,184,0.12)]'>
      {/* Image */}
      <div className='relative w-full h-32 overflow-hidden'>
        <Image
          src={thumbnail}
          alt={title}
          fill
          className='object-cover'
          sizes='(max-width: 768px) 100vw, 33vw'
        />
        {badge && (
          <span
            className={`absolute top-3 left-3 px-2 py-1 rounded-sm text-xs font-medium font-switzer leading-5 ${
              badge.variant === 'popular'
                ? 'bg-amber-600 text-amber-50'
                : 'bg-blue-50 text-blue-500'
            }`}
          >
            {badge.text}
          </span>
        )}
      </div>

      {/* Content */}
      <div className='w-full p-4 flex flex-col gap-4'>
        <h3 className='text-base font-medium font-switzer leading-6 text-[#1c3873]'>
          {title}
        </h3>

        {nextModule && (
          <div className='w-full flex flex-col gap-2'>
            <span className='text-[10px] font-normal font-inter leading-4 text-[#5e5f5c]'>
              Next module
            </span>
            <div className='w-full flex flex-col gap-2'>
              <span className='text-sm font-medium font-switzer leading-4 text-[#5e5f5c]'>
                {nextModule}
              </span>
              {/* Progress bar */}
              <div className='w-full h-1 relative bg-blue-100 rounded-full overflow-hidden'>
                <div
                  className='absolute left-0 top-0 h-full bg-blue-500 rounded-full transition-all duration-500'
                  style={{ width: `${barPercent}%` }}
                />
              </div>
              <div className='w-full flex justify-between items-center'>
                <span className='text-xs font-normal font-switzer leading-4 text-[#1c3873]'>
                  {progress}%
                </span>
                <span className='text-xs font-normal font-switzer leading-4 text-[#1c3873]'>
                  {lessonsLeft} lesson{lessonsLeft !== 1 ? 's' : ''} left
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Resume button */}
        <Link
          href={href}
          className='w-full h-11 px-5 py-2.5 bg-white rounded-md shadow-[0px_3px_8px_0px_rgba(0,0,0,0.08),0px_1px_2px_0px_rgba(0,0,0,0.05)] [box-shadow:inset_0px_1px_0px_0px_rgba(255,255,255,0.80)] outline outline-1 outline-blue-500/30 inline-flex justify-center items-center gap-2 text-blue-500 text-base font-semibold font-switzer leading-6 transition-colors duration-200 hover:bg-blue-50'
        >
          Resume session
          <svg width='24' height='24' viewBox='0 0 24 24' fill='none' xmlns='http://www.w3.org/2000/svg'>
            <path d='M9 18L15 12L9 6' stroke='currentColor' strokeWidth='2' strokeLinecap='round' strokeLinejoin='round' />
          </svg>
        </Link>
      </div>
    </div>
  )
}

export default CourseCard
