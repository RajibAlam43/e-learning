'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useTranslations } from 'next-intl'
import type { components } from '@/types/api'

interface ExpertInstructorProps {
  instructors?: components['schemas']['InstructorSummaryResponse'][]
}

export function ExpertInstructor({ instructors }: ExpertInstructorProps) {
  const t = useTranslations('courses.detail')
  const safeInstructors = instructors ?? []
  if (safeInstructors.length === 0) return null

  return (
    <section
      aria-labelledby="expert-instructor-heading"
      className="w-full pt-20 pb-20 max-[450px]:pt-12 max-[450px]:pb-12"
    >
      <div className="container mx-auto flex flex-col items-start gap-8 px-5">
        <h2
          id="expert-instructor-heading"
          className="mt-[-1px] flex items-center self-stretch text-[36px] font-normal leading-[44px] tracking-[0px] text-[#1c3873] font-heading max-[450px]:text-[28px] max-[450px]:leading-[34px]"
        >
          {t('instructorTitle.title')}
        </h2>

        {safeInstructors.map((instructor) => {
          const title = [
            instructor.credentials,
            instructor.yearsExperience
              ? `${instructor.yearsExperience}${t('instructorTitle.yearsExperience')}`
              : null,
          ]
            .filter(Boolean)
            .join(' \u2022 ')

          return (
            <article
              key={instructor.id}
              className="flex h-[495px] w-full items-end gap-12 rounded-lg bg-white p-8 max-[750px]:flex-col max-[750px]:h-auto max-[450px]:p-5 transition-all duration-300 hover:shadow-lg opacity-0 animate-fade-in-up"
            >
              {/* Image */}
              <div className="flex flex-1 items-center justify-center overflow-hidden rounded-[4px] self-stretch group">
                <div className="relative h-[631px] w-[787px] -ml-[125px] -mt-[100px] -mb-[100px] max-[750px]:h-[400px] max-[750px]:w-full max-[750px]:ml-0 max-[750px]:mt-0 max-[750px]:mb-0 max-[450px]:h-[280px] max-[450px]:w-full">
                  {instructor.avatarUrl ? (
                    <Image
                      src={instructor.avatarUrl}
                      alt={instructor.fullName ?? 'Instructor'}
                      fill
                      className="rounded-lg object-cover object-center shadow-[0px_4px_6px_-4px_#0000001a,0px_10px_15px_-3px_#0000001a] transition-transform duration-500 group-hover:scale-105"
                    />
                  ) : (
                    <div className="h-full w-full rounded-lg bg-[#e8ebf1]" />
                  )}
                </div>
              </div>

              {/* Info */}
              <div className="flex flex-1 flex-col items-start justify-end gap-6">
                <header className="flex w-full flex-col gap-1">
                  {instructor.slug ? (
                    <Link
                      href={`/instructors/${instructor.slug}`}
                      className="group flex w-fit items-center gap-1 no-underline"
                    >
                      <h3 className="mt-[-1px] w-fit whitespace-nowrap text-2xl font-normal leading-8 tracking-[0px] text-[#1b1c1a] font-heading max-[450px]:text-xl max-[450px]:leading-7 group-hover:underline">
                        {instructor.fullName}
                      </h3>
                    </Link>
                  ) : (
                    <h3 className="mt-[-1px] w-fit whitespace-nowrap text-2xl font-normal leading-8 tracking-[0px] text-[#1b1c1a] font-heading max-[450px]:text-xl max-[450px]:leading-7">
                      {instructor.fullName}
                    </h3>
                  )}
                  {title && (
                    <p className="flex items-center self-stretch text-sm font-normal leading-[150%] tracking-[0px] text-[#af8d11] font-sans">
                      {title}
                    </p>
                  )}
                </header>
                {instructor.shortBio && (
                  <p className="flex items-center self-stretch text-lg font-normal leading-7 tracking-[0px] text-[#5e5f5c] font-sans max-[450px]:text-base max-[450px]:leading-6">
                    {instructor.shortBio}
                  </p>
                )}
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
