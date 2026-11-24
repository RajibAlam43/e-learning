import StatCard from './StatCard'
import React from 'react'

interface StatCardsProps {
  stats: {
    icon: string
    iconBg: string
    iconAlt: string
    value: string | number
    label: string
  }[]
  isLoading?: boolean
}

const StatCards = ({ stats, isLoading }: StatCardsProps) => {
  if (isLoading) {
    return (
      <div className='grid grid-cols-2 xl:grid-cols-3 gap-3'>
        {[1, 2, 3].map((i) => (
          <div key={i} className='h-24 rounded-2xl bg-gray-100 animate-pulse' />
        ))}
      </div>
    )
  }

  return (
    <div className='grid grid-cols-2 md:grid-cols-2 xl:grid-cols-3 gap-3'>
      {stats.map((stat, i) => (
        <div key={stat.label} className='animate-fade-in-up' style={{ animationDelay: `${i * 100}ms` }}>
          <StatCard {...stat} />
        </div>
      ))}
    </div>
  )
}

export default StatCards
