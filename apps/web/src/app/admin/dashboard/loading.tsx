import { Skeleton } from '@/components/ui/skeleton'

/**
 * Route-level loading state for /admin/dashboard.
 * Shown by Next.js during SSR/streaming and on slow navigations.
 */
export default function DashboardLoading() {
  return (
    <div className='flex flex-col gap-8 w-full max-w-[1152px] mx-auto pb-10 px-4 sm:px-6'>
      {/* Greeting skeleton */}
      <div className='rounded-2xl [background:linear-gradient(100.17deg,#684f17,#bf9843)] p-4 md:p-8 h-32'>
        <Skeleton className='h-7 w-48 bg-white/20 rounded-lg' />
        <Skeleton className='h-4 w-72 mt-2 bg-white/20 rounded-lg' />
      </div>

      {/* Stats skeleton (5 cards) */}
      <div className='grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3'>
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className='bg-white rounded-2xl shadow-sm p-5 flex flex-col gap-4'>
            <Skeleton className='h-4 w-24' />
            <Skeleton className='h-8 w-20' />
          </div>
        ))}
      </div>

      {/* Analytics skeleton */}
      <div className='bg-white rounded-2xl shadow-sm p-5'>
        <Skeleton className='h-5 w-40 mb-4' />
        <div className='grid grid-cols-2 gap-3 mb-4'>
          <Skeleton className='h-16 rounded-xl' />
          <Skeleton className='h-16 rounded-xl' />
        </div>
        <Skeleton className='h-64 rounded-xl' />
      </div>

      {/* Grid skeleton */}
      <div className='flex flex-col lg:flex-row gap-4'>
        <div className='flex-1 bg-white rounded-2xl shadow-sm p-5'>
          <Skeleton className='h-5 w-32 mb-4' />
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className='h-14 w-full mb-2' />
          ))}
        </div>
        <div className='w-full lg:w-[340px] bg-white rounded-2xl shadow-sm p-5'>
          <Skeleton className='h-5 w-32 mb-4' />
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className='h-12 w-full mb-2' />
          ))}
        </div>
      </div>
    </div>
  )
}
