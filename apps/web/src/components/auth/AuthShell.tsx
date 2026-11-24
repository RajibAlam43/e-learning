'use client'

import { ROUTES } from '@/constants/routes'
import { useTranslations } from 'next-intl'
import Image from 'next/image'
import Link from 'next/link'
import type { ReactNode } from 'react'

interface AuthShellProps {
  children: ReactNode
  showFooter?: boolean
}

export function AuthShell({ children, showFooter = true }: AuthShellProps) {
  const t = useTranslations('auth.signup')
  return (
    <div className="flex w-full items-center justify-center overflow-auto bg-[#e8ebf1] p-4 leading-[normal] tracking-[normal] md:p-5 max-lg:min-h-screen">
      <main className="flex w-full max-w-[1400px] items-stretch gap-5 max-lg:flex-col max-lg:items-center max-lg:gap-0">
        {/* Left panel - form card */}
        <div className="flex w-full max-w-[562px] flex-col items-start justify-center gap-6 rounded-3xl bg-white px-6 py-8 shadow-[0px_6px_15px_-2px_rgba(16,24,40,0.08)] sm:px-10 md:px-16 md:py-10 max-lg:max-w-full max-lg:min-h-screen max-lg:px-6 max-lg:py-8">
          {/* Logo */}
          <Link href="/" className="shrink-0 transition-opacity duration-200 hover:opacity-80">
            <Image
              src="/images/logo.svg"
              alt="Global Islamic Institute"
              width={95}
              height={58}
              className="h-auto"
              quality={100}
              unoptimized
            />
          </Link>

          {/* Step content */}
          {children}

          {/* Footer */}
          {showFooter && (
            <div className="self-stretch text-center text-sm">
              <span className="font-sans leading-[150%] text-[#71717a]">
                Already have an account?{' '}
              </span>
              <Link
                href={ROUTES.login}
                className="font-sans text-[16px] font-semibold leading-5 text-[#b91c1c] hover:underline"
              >
                {t('loginLink')}
              </Link>

            </div>
          )}
        </div>

        {/* Right panel - image fills remaining space, hidden on mobile */}
        <Image
          src="/images/rightImage.png"
          alt=""
          width={838}
          height={984}
          className="hidden w-full h-auto rounded-3xl object-cover lg:block"
          loading="lazy"
        />
      </main>
    </div>
  )
}
