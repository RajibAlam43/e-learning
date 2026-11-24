'use client'

import { useTranslations } from 'next-intl'

interface TeacherScheduledClassesEmptyProps {
  userName?: string
}

const TeacherScheduledClassesEmpty = ({ userName = 'Teacher' }: TeacherScheduledClassesEmptyProps) => {
  const ui = useTranslations('privateUi')


  return (
    <div className="group w-full rounded-2xl p-8 flex flex-col items-start gap-8 shadow-[0px_4px_8px_0px_rgba(20,26,184,0.10),0px_15px_15px_0px_rgba(20,26,184,0.09),0px_34px_20px_0px_rgba(20,26,184,0.05),0px_60px_24px_0px_rgba(20,26,184,0.01)] bg-gradient-to-br from-indigo-800 to-blue-500">
      {/* Header */}
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-medium leading-8 text-white">
          {ui('assalamuAlaikum')}{' '}{userName}
        </h1>
        <p className="text-sm font-normal leading-5 text-white/80 font-switzer">
          {ui('youHave0ClassesScheduledForToday')}</p>
      </div>

    </div>
  )
}

export default TeacherScheduledClassesEmpty
