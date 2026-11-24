'use client'

import { useTranslations } from 'next-intl'

import { ChevronRight } from 'lucide-react'
import Link from 'next/link'

interface ContinuedCoursesEmptyProps {
  userName?: string
}

const ContinuedCoursesEmpty = ({ userName = 'Student' }: ContinuedCoursesEmptyProps) => {
  const ui = useTranslations('privateUi')


  return (
    <div className="group w-full rounded-2xl p-8 flex flex-col items-start gap-8 shadow-[0px_4px_8px_0px_rgba(20,26,184,0.10),0px_15px_15px_0px_rgba(20,26,184,0.09),0px_34px_20px_0px_rgba(20,26,184,0.05),0px_60px_24px_0px_rgba(20,26,184,0.01)] bg-gradient-to-br from-indigo-800 to-blue-500">
      {/* Header */}
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-medium leading-8 text-white">
          {ui('assalamuAlaikum')}{' '}{userName}
        </h1>
        <p className="text-sm font-normal leading-5 text-white/80 font-switzer">
          {ui('startFromWhereYouLeftOff')}</p>
      </div>

      <Link href='/courses' className='py-2.5 px-5 flex items-center gap-2 bg-white text-[#1d4ed8] cursor-pointer hover:bg-white/90 rounded-md shadow-[0px_3px_8px_0px_rgba(0,0,0,0.08)] shadow-[0px_1px_2px_0px_rgba(0,0,0,0.05)] shadow-[inset_0px_1px_0px_0px_rgba(255,255,255,0.80)]'>
        <span>{ui('browseAndEnrollInCourses')}</span><ChevronRight />
      </Link>

        

    </div>
  )
}

export default ContinuedCoursesEmpty
