'use client'

interface LiveClassesEmptyProps {
  title?: string
  heading?: string
  description?: string
}

export default function LiveClassesEmpty({
  title = 'No live classes scheduled yet',
  heading = 'Upcoming Live Classes',
  description = 'Once you enroll, your upcoming live sessions will appear here.',
}: LiveClassesEmptyProps) {
  return (
    <div className='flex-1 flex flex-col gap-4'>
      <h2 className='text-lg font-semibold font-switzer leading-6 text-[#1c3873]'>
        {heading}
      </h2>
      <div className='w-full h-auto py-8 px-6 relative bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden'>
        {/* Icon */}
        <div className='flex items-center justify-center mx-auto'>
          <svg width='56' height='56' viewBox='0 0 56 56' fill='none' xmlns='http://www.w3.org/2000/svg'>
            <g clipPath='url(#live-empty-clip)'>
              <path d='M28 56C12.536 56 0 43.464 0 28C0 12.536 12.536 0 28 0C43.464 0 56 12.536 56 28C56 43.464 43.464 56 28 56Z' fill='#FFF7ED' />
              <path d='M14 16H42C44.2091 16 46 17.7909 46 20V38C46 40.2091 44.2091 42 42 42H14C11.7909 42 10 40.2091 10 38V20C10 17.7909 11.7909 16 14 16Z' fill='#FED7AA' />
              <path d='M33 28L25 23V33L33 28Z' fill='#F97316' />
            </g>
            <defs>
              <clipPath id='live-empty-clip'>
                <rect width='56' height='56' fill='white' transform='matrix(-1 0 0 1 56 0)' />
              </clipPath>
            </defs>
          </svg>
        </div>

        {/* Text */}
        <div className='flex flex-col items-center gap-1.5 text-center mt-5'>
          <h3 className='text-xs font-medium leading-4 text-blue-900'>
            {title}
          </h3>
          <p className='text-xs text-zinc-600 leading-4'>
            {description}
          </p>
        </div>
      </div>
    </div>
  )
}
