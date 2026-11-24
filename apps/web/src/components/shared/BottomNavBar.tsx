'use client'

import { useTranslations } from 'next-intl'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { User } from 'lucide-react'

interface NavItem {
  label: string
  href: string
  icon: React.ComponentType<{ className?: string }>
}

interface BottomNavBarProps {
  items: NavItem[]
}

export default function BottomNavBar({ items }: BottomNavBarProps) {
  const ui = useTranslations('privateUi')

  const pathname = usePathname()

  const activeItem = items.find((item) => {
    if (item.href === `/${items[0]?.href.split('/')[1]}`) {
      return pathname === item.href
    }
    return pathname.startsWith(item.href)
  })

  return (
    <nav
      aria-label={ui('primary')}
      className='fixed bottom-4 left-1/2 -translate-x-1/2 z-50 inline-flex items-center bg-white rounded-full shadow-[0px_4px_24px_rgba(0,0,0,0.12)] lg:hidden'
    >
      {items.map((item) => {
        const Icon = item.icon
        const isActive = item === activeItem

        if (isActive) {
          return (
            <div key={item.href} className='flex items-center px-3 py-3'>
              <Link
                href={item.href}
                aria-current='page'
                className='inline-flex items-center gap-1.5 px-3 py-3 bg-[#3b82f6] rounded-full'
              >
                <Icon className='w-6 h-6 text-white' />
                <span className='text-xs font-medium text-white whitespace-nowrap'>
                  {item.label}
                </span>
              </Link>
            </div>
          )
        }

        return (
          <div key={item.href} className='flex items-center px-4 py-3'>
            <Link href={item.href} className='inline-flex items-center'>
              <Icon className='w-6 h-6 text-[#5e5f5c]' />
            </Link>
          </div>
        )
      })}

      {/* Profile avatar */}
      <div className='flex items-center px-3 py-3'>
        <Link href={items[0]?.href.replace(/\/[^/]*$/, '/profile') ?? '#'} className='inline-flex items-center rounded-full'>
          <div className='w-6 h-6 rounded-full border-2 border-solid border-[#3b82f6] bg-[#dbeafe] flex items-center justify-center'>
            <User className='w-3.5 h-3.5 text-[#3b82f6]' />
          </div>
        </Link>
      </div>
    </nav>
  )
}
