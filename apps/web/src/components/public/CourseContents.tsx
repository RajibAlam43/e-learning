'use client'

import { useTranslations } from 'next-intl'
import Image from 'next/image'

interface CourseContentsProps {
  recordedHoursCount?: number
  liveSessionCount?: number
}

const CONTENT_ITEMS = [
  { iconSrc: '/images/icon-play.png', key: 'hours' },
  { iconSrc: '/images/icon-user.png', key: 'live' },
] as const

export function CourseContents({
  recordedHoursCount,
  liveSessionCount,
}: CourseContentsProps) {
  const t = useTranslations('courses.detail')
  const hours = recordedHoursCount ?? 0
  const liveSessions = liveSessionCount ?? 0

  const hasContent = hours > 0 || liveSessions > 0
  if (!hasContent) return null

  const items = [
    { title: t('contentsTitle.hoursLectures', { hours }), description: t('contentsTitle.learnAtYourPace'), ...CONTENT_ITEMS[0], show: hours > 0 },
    { title: t('contentsTitle.liveSession', { liveSessions }), description: t('contentsTitle.interactiveSession'), ...CONTENT_ITEMS[1], show: liveSessions > 0 },
  ].filter((item) => item.show)

  return (
    <section className="flex w-full py-20 bg-[#fdfcf8] max-[450px]:py-12">
      <div className="container mx-auto px-5 flex-col items-start">
        <div className="flex w-full flex-col gap-6">
          <header className="flex w-full flex-col gap-2">
            <h2 className="mt-[-1px] text-[36px] font-normal leading-[44px] tracking-[0px] text-[#1c3873] font-heading max-[450px]:text-[28px] max-[450px]:leading-[34px]">
              {t('contentsTitle.title')}
            </h2>
          </header>

          <div
            className="flex w-full gap-4 max-[450px]:flex-col rounded-2xl overflow-hidden"
            style={{
              boxShadow:
                '0px 3px 8px #6e5f0d40, 0px 14px 14px #6e5f0d36, 0px 31px 19px #6e5f0d21, 0px 55px 22px #6e5f0d0a, 0px 86px 24px transparent',
            }}
            role="list"
            aria-label="Course content highlights"
          >
            {items.map((item, i) => (
              <article
                key={item.key}
                className="group flex flex-1 flex-col h-[212px] items-center gap-4 rounded-xl bg-[#735c00] px-6 py-12 shadow-[inset_0px_8px_24px_#d4af37] max-[450px]:h-auto max-[450px]:py-6 transition-all duration-300 hover:brightness-110 opacity-0 animate-fade-in-up"
                role="listitem"
                style={{ animationDelay: `${i * 100}ms` }}
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-[linear-gradient(180deg,rgba(255,255,255,1)_0%,rgba(255,255,255,0)_100%)] transition-transform duration-300 group-hover:scale-110">
                  <Image
                    src={item.iconSrc}
                    alt=""
                    aria-hidden="true"
                    width={25}
                    height={25}
                    className="h-[25px] w-[25px]"
                  />
                </div>
                <div className="flex flex-col items-center gap-1">
                  <h3 className="flex w-fit items-center justify-center whitespace-nowrap text-lg font-medium leading-7 text-white font-sans">
                    {item.title}
                  </h3>
                  <p className="flex w-fit items-center justify-center whitespace-nowrap text-xs font-normal leading-5 text-white/90 font-sans">
                    {item.description}
                  </p>
                </div>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
