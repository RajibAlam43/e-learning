'use client'

import { type ReactNode } from 'react'

/**
 * SectionCard — the standard admin widget chrome.
 *
 * White rounded-2xl card with shadow, optional header (title + optional action
 * slot), and a body. Used across the admin dashboard and other admin pages
 * for visual consistency.
 *
 * @example
 *   <SectionCard title="Recent Courses" action={<Link href="...">See all</Link>}>
 *     <ul>...</ul>
 *   </SectionCard>
 */
interface SectionCardProps {
  title?: string
  description?: string
  /** Right-aligned slot in the header — usually a link, button, or counter. */
  action?: ReactNode
  /** Hide the header entirely (title + action). Default: false. */
  hideHeader?: boolean
  /** Optional aria-label for the section (defaults to title). */
  ariaLabel?: string
  /** Override padding — default `p-5`. */
  bodyClassName?: string
  children: ReactNode
}

export function SectionCard({
  title,
  description,
  action,
  hideHeader = false,
  ariaLabel,
  bodyClassName = 'p-5',
  children,
}: SectionCardProps) {
  return (
    <section
      aria-label={ariaLabel ?? title}
      className={`bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] flex flex-col gap-4 ${bodyClassName}`}
    >
      {!hideHeader && (title || action) && (
        <header className='flex items-center justify-between gap-3'>
          <div className='flex flex-col gap-1 min-w-0'>
            {title && (
              <h3 className='text-base font-heading font-medium text-[#1c3873] m-0'>{title}</h3>
            )}
            {description && (
              <p className='text-[11px] text-[#71717a] m-0'>{description}</p>
            )}
          </div>
          {action && <div className='shrink-0'>{action}</div>}
        </header>
      )}
      {children}
    </section>
  )
}
