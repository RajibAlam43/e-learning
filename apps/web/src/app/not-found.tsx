import Link from 'next/link'
import Image from 'next/image'

export default function NotFound() {
  return (
    <div className='flex min-h-screen flex-col items-center justify-center gap-6 px-4 bg-[#f5f9ff]'>
      <div className='flex flex-col items-center gap-4'>
        {/* Logo */}
        <Image
          src='/images/logo.svg'
          width={160}
          height={48}
          alt='GII Logo'
          className='h-12 w-auto object-contain mb-4'
        />

        {/* 404 number */}
        <h1 className='text-8xl md:text-9xl font-heading font-bold text-[#1c3873] leading-none'>
          404
        </h1>

        {/* Illustration text */}
        <div className='flex flex-col items-center gap-2'>
          <h2 className='text-xl md:text-2xl font-heading font-medium text-[#1c3873]'>
            Page not found
          </h2>
          <p className='text-sm md:text-base text-[#5e5f5c] text-center max-w-md'>
            The page you&apos;re looking for doesn&apos;t exist or has been moved.
            Let&apos;s get you back on track.
          </p>
        </div>
      </div>

      <div className='flex flex-col sm:flex-row items-center gap-3 mt-2'>
        <Link
          href='/'
          className='h-11 px-6 flex items-center justify-center gap-2 rounded-md bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] text-white text-sm font-medium font-switzer shadow-[inset_0px_1px_0px_#ffffff1f,inset_0px_-1px_0px_#0000002e] hover:opacity-90 transition-opacity'
        >
          Go to Homepage
        </Link>
        <Link
          href='/student/dashboard'
          className='h-11 px-6 flex items-center justify-center gap-2 rounded-md bg-white text-[#3b82f6] text-sm font-medium font-switzer outline outline-1 outline-blue-500/30 shadow-[0px_3px_8px_0px_rgba(0,0,0,0.08)] hover:bg-blue-50 transition-colors'
        >
          Go to Dashboard
        </Link>
      </div>
    </div>
  )
}
