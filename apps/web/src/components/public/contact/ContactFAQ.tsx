'use client'

import { useState } from 'react'
import { Plus, Minus } from 'lucide-react'
import { useTranslations } from 'next-intl'

interface FAQItem {
  qKey: string
  aKey: string
}

const FAQ_ITEMS: FAQItem[] = [
  { qKey: 'faqs.q1', aKey: 'faqs.a1' },
  { qKey: 'faqs.q2', aKey: 'faqs.a2' },
  { qKey: 'faqs.q3', aKey: 'faqs.a3' },
  { qKey: 'faqs.q4', aKey: 'faqs.a4' },
]

export function ContactFAQ() {
  const t = useTranslations('contact')
  const [openIndex, setOpenIndex] = useState<number | null>(1)

  const toggle = (index: number) => {
    setOpenIndex((prev) => (prev === index ? null : index))
  }

  return (
    <section className="w-full bg-[#f5f5f5] py-20 max-md:py-[52px]">
      <div className="container mx-auto flex flex-col items-center gap-10 px-5">
        <h2 className="text-center text-[24px] font-semibold leading-7 tracking-[-1px] text-[#1b1c1a] font-heading max-md:text-[24px] max-md:leading-[29px] md:text-[32px] md:leading-[38px] lg:text-[40px] lg:leading-[48px]">
          {t('faqTitle')}
        </h2>

        <div className="w-full max-w-[628px] overflow-hidden rounded-lg bg-white shadow-[0px_1px_3px_0px_rgba(0,0,0,0.05)] transition-shadow duration-300 hover:shadow-[0px_4px_12px_0px_rgba(0,0,0,0.08)]">
          {FAQ_ITEMS.map((item, index) => {
            const isOpen = openIndex === index
            return (
              <div key={index}>
                {/* Divider above (skip first) */}
                {index > 0 && (
                  <div className="h-px border-t border-solid border-[#f1f2f9]" />
                )}

                {/* Question row */}
                <button
                  type="button"
                  onClick={() => toggle(index)}
                  className="flex w-full items-center gap-5 py-4 px-5 text-left cursor-pointer border-none bg-transparent transition-colors duration-200 hover:bg-[#f9fafb]"
                >
                  {/* Number */}
                  <span className="shrink-0 text-lg font-semibold leading-[135%] text-[#6b7280] transition-colors duration-200 group-hover:text-[#1c3873]">
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  {/* Question text + icon */}
                  <div className="flex flex-1 items-center justify-between gap-5">
                    <span className="font-sans text-base font-semibold leading-6 text-[#1c3873]">
                      {t(item.qKey)}
                    </span>
                    <span className="flex items-center justify-center shrink-0">
                      {isOpen
                        ? (
                          <Minus className="h-3.5 w-3.5 text-[#1c3873] transition-transform duration-300 rotate-0" />
                        )
                        : (
                          <Plus className="h-[20px] w-[20px] text-[#71717a] transition-transform duration-300 rotate-0 hover:rotate-90" />
                        )}
                    </span>
                  </div>
                </button>

                {/* Answer (animated with CSS grid) */}
                <div
                  className="grid transition-all duration-300 ease-in-out"
                  style={{ gridTemplateRows: isOpen ? '1fr' : '0fr' }}
                >
                  <div className="min-h-0 overflow-hidden">
                    <div
                      className="px-5 pb-4 pt-3.5"
                      style={{
                        background: isOpen
                          ? 'linear-gradient(257.4deg, rgba(255,255,255,0), rgba(28,56,115,0.1))'
                          : undefined,
                      }}
                    >
                      <div className="pl-[30px]">
                        <p className="m-0 max-w-[530px] font-sans text-base leading-7 text-[#5e5f5c]">
                          {t(item.aKey)}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
