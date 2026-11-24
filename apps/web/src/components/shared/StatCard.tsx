import Image from 'next/image'
import React from 'react'

interface StatCardProps {
  icon: string
  iconBg: string
  iconAlt: string
  value: string | number
  label: string
}

const StatCard = ({ icon, iconBg, iconAlt, value, label }: StatCardProps) => {
  return (
    <div className='p-4 bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] flex items-center gap-3 transition-all duration-300 ease-[cubic-bezier(0.4,0,0.2,1)] hover:-translate-y-1 hover:shadow-[0px_8px_16px_0px_rgba(20,26,184,0.12)] hover:cursor-default'>
      <div className={`p-3 rounded-lg flex items-center justify-center transition-transform duration-300 ease-[cubic-bezier(0.4,0,0.2,1)] ${iconBg} group-hover:scale-110`}>
        <Image src={icon} width={24} height={24} alt={iconAlt} className='transition-transform duration-300 ease-[cubic-bezier(0.4,0,0.2,1)]' />
      </div>
      <div className='flex flex-col gap-1'>
        <span className='text-2xl font-medium font-switzer leading-6 text-[#1c3873]'>
          {value}
        </span>
        <span className='text-xs font-normal font-switzer leading-4 text-[#5e5f5c]'>
          {label}
        </span>
      </div>
    </div>
  )
}

export default StatCard
