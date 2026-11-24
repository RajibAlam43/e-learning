'use client'

import Image from 'next/image'
import Link from 'next/link'

interface EmptyCoursesCardProps {
  title?: string
  description?: string
  actionLabel?: string
  actionHref?: string
}

export default function EmptyCoursesCard({
  title = "You haven't enrolled in any courses yet",
  description = "Start your Islamic learning journey — discover courses tailored for you.",
  actionLabel = 'Browse Courses',
  actionHref = '/courses',
}: EmptyCoursesCardProps) {
  return (
    <div className="w-full p-5 relative bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden gap-3 flex flex-col items-center">
      {/* Icon */}
      <div className="flex items-center justify-center w-20 h-20">
        <Image
          src="/images/Frame.svg"
          alt=""
          width={80}
          height={80}
          className="object-contain"
          priority={false}
        />
      </div>

      {/* Text */}
      <div className="flex flex-col items-center gap-2 text-center ">
        <h3 className="text-sm md:text-base font-medium leading-4 text-blue-900 font-switzer">
          {title}
        </h3>
        <p className="text-xs text-zinc-500 leading-4 font-switzer">
          {description}
        </p>
      </div>

      {/* Action */}
      <div className="flex items-center justify-center pt-4">
        <Link
          href={actionHref}
          className="inline-flex items-center justify-center w-48 h-11 rounded-md bg-gradient-to-r from-indigo-500 to-blue-800 text-sm font-medium text-white hover:opacity-90 transition-opacity duration-200"
        >
          {actionLabel}
        </Link>
      </div>
    </div>
  )
}
