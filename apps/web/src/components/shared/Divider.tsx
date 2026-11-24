import React from 'react'

interface DividerProps {
  className?: string
}

const Divider = ({ className = '' }: DividerProps) => {
  return (
    <div
      className={`w-full h-px ${className}`}
      style={{
        background: 'linear-gradient(to right, transparent, #e5e7eb 15%, #e5e7eb 85%, transparent)',
      }}
      role='separator'
    />
  )
}

export default Divider
