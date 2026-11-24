'use client'

import type { ReactNode, FormEvent } from 'react'

interface StepLayoutProps {
  title: string
  subtitle: string
  subtitleLineTwo?: string
  subtitleColor?: string
  onSubmit?: (e: FormEvent) => void
  actionsGap?: string
  children: ReactNode
  actions: ReactNode
}

export function StepLayout({
  title,
  subtitle,
  subtitleLineTwo = '',
  subtitleColor = 'text-[#71717a]',
  onSubmit,
  actionsGap = 'gap-2.5',
  children,
  actions,
}: StepLayoutProps) {
  const Wrapper = onSubmit ? 'form' : 'div'

  return (
    <Wrapper
      {...(onSubmit ? { onSubmit } : {})}
      className="flex w-full flex-col items-start gap-6"
    >
      <div className="self-stretch flex flex-col items-start gap-1">
        <h3 className="m-0 self-stretch font-heading text-2xl font-semibold leading-8 text-[#1c3873]">
          {title}
        </h3>
        <div className={`self-stretch text-sm leading-[150%] ${subtitleColor}`}>
          {subtitle}
          <br />
          {subtitleLineTwo}
        </div>
      </div>

      <div className="flex self-stretch flex-col items-start gap-4">
        {children}
      </div>

      <div className={`flex self-stretch flex-col items-start ${actionsGap}`}>
        {actions}
      </div>
    </Wrapper>
  )
}
