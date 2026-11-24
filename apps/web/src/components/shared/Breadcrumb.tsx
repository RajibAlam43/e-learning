import Link from 'next/link'
import { ChevronRight } from 'lucide-react'

interface BreadcrumbItem {
  label: string
  href?: string
}

interface BreadcrumbProps {
  items: BreadcrumbItem[]
}

export function Breadcrumb({ items }: BreadcrumbProps) {
  return (
    <nav className='text-sm overflow-x-auto whitespace-nowrap flex items-center gap-1'>
      {items.map((item, index) => {
        const isLast = index === items.length - 1

        return (
          <span key={index} className='flex items-center gap-1'>
            {index > 0 && (
              <ChevronRight size={14} className='text-[#a3a3a3] shrink-0' />
            )}
            {isLast || !item.href ? (
              <span
                className={
                  isLast
                    ? 'text-[#1c3873] font-medium'
                    : 'text-[#5e5f5c]'
                }
              >
                {item.label}
              </span>
            ) : (
              <Link
                href={item.href}
                className='text-[#5e5f5c] hover:text-[#1c3873] transition-colors'
              >
                {item.label}
              </Link>
            )}
          </span>
        )
      })}
    </nav>
  )
}
