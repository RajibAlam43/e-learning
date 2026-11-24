'use client'

import { useTranslations } from 'next-intl'

interface CourseOverviewRequirementsProps {
  description?: string
  requirements?: string[]
}

export function CourseOverviewRequirements({
  description,
  requirements,
}: CourseOverviewRequirementsProps) {
  const t = useTranslations('courses.detail')
  const safeRequirements = requirements ?? []
  const hasContent = description || safeRequirements.length > 0

  if (!hasContent) return null

  return (
    <section
      className="w-full pt-10 mt-10 md:mt-35"
      aria-labelledby="overview-heading"
    >
      <div className="container mx-auto flex items-start gap-20 px-5 pb-10 max-[750px]:flex-col max-[750px]:gap-6 opacity-0 animate-fade-in-up">
        {/* Overview */}
        {description && (
          <div className="flex flex-1 flex-col items-start gap-6">
            <h2
              id="overview-heading"
              className="mt-[-1px] text-[36px] font-normal leading-[44px] tracking-[0px] text-[#1c3873] font-heading whitespace-nowrap max-[450px]:text-[28px] max-[450px]:leading-[34px]"
            >
              {t('overview.title')}
            </h2>
            <p className="flex w-full items-center text-base font-normal leading-6 tracking-[0px] text-[#5e5f5c] font-sans">
              {description}
            </p>
          </div>
        )}

        {/* Course Requirements */}
        {safeRequirements.length > 0 && (
          <div className="flex flex-1 flex-col items-start gap-6">
            <h2 className="mt-[-1px] text-[36px] font-normal leading-[44px] tracking-[0px] text-[#1c3873] font-heading whitespace-nowrap max-[450px]:text-[28px] max-[450px]:leading-[34px]">
              {t('requirementsTitle.title')}
            </h2>
            <ul className="flex w-full flex-col items-start gap-2 self-stretch list-disc pl-5">
              {safeRequirements.map((req, i) => (
                <li
                  key={req}
                  className="self-stretch text-base font-normal leading-6 tracking-[0px] text-[#5e5f5c] font-sans pl-1 transition-colors duration-200 hover:text-[#1c3873]"
                  style={{ animationDelay: `${i * 60}ms` }}
                >
                  {req}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </section>
  )
}
