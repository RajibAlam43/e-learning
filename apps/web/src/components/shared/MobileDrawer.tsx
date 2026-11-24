'use client'

import { SidebarContent, type SidebarProps } from './DashboardSidebar'
import { useRef } from 'react'
import { useTranslations } from 'next-intl'
import { useDialogFocus } from '@/hooks/useDialogFocus'

interface MobileDrawerProps extends Omit<SidebarProps, 'onNavigate'> {
  isOpen: boolean
  onClose: () => void
}

const MobileDrawer = ({ isOpen, onClose, ...sidebarProps }: MobileDrawerProps) => {
  const container = useRef<HTMLDivElement>(null)
  const t = useTranslations('privateNav')
  useDialogFocus(isOpen, container, onClose)
  return (
    <>
      {/* Overlay */}
      <div
        className={`fixed inset-0 z-40 bg-black/40 transition-opacity duration-300 lg:hidden ${
          isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
        onClick={onClose}
      />

      {/* Drawer */}
      <div
        ref={container}
        role="dialog"
        aria-label={t('navigation')}
        aria-modal={isOpen}
        aria-hidden={!isOpen}
        inert={!isOpen}
        tabIndex={-1}
        className={`fixed top-0 left-0 z-50 h-screen w-60 max-w-[80vw] px-6 py-6 flex flex-col overflow-hidden bg-white border-r border-blue-100 transition-transform duration-300 ease-[cubic-bezier(0.4,0,0.2,1)] lg:hidden ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <SidebarContent {...sidebarProps} onNavigate={onClose} />
      </div>
    </>
  )
}

export default MobileDrawer
