import { Skeleton } from '@/components/ui/skeleton'

interface SkeletonCardProps {
  className?: string
}

export function SkeletonCard({ className }: SkeletonCardProps) {
  return (
    <div className={`rounded-2xl overflow-hidden ${className ?? ''}`}>
      <Skeleton className='w-full aspect-video' />
      <div className='p-4 space-y-3'>
        <Skeleton className='h-4 w-3/4' />
        <Skeleton className='h-3 w-full' />
        <Skeleton className='h-3 w-1/2' />
      </div>
    </div>
  )
}
