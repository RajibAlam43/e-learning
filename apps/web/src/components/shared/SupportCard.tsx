import React from 'react'

interface SupportCardProps {
  title?: string
  description?: string
  buttonText?: string
  buttonHref?: string
  isLoading?: boolean
}

const SupportCard = ({
  title = 'Need guidance?',
  description = 'Reach out to us. We are here to help you',
  buttonText = 'Send us a message',
  buttonHref = '/contact',
  isLoading,
}: SupportCardProps) => {
  if (isLoading) {
    return (
      <div className='w-full p-8 rounded-2xl bg-gray-100 animate-pulse flex flex-col items-center gap-4'>
        <div className='size-9 bg-gray-200 rounded-full' />
        <div className='h-4 w-28 bg-gray-200 rounded' />
        <div className='h-3 w-52 bg-gray-200 rounded' />
        <div className='h-11 w-40 bg-gray-200 rounded-md' />
      </div>
    )
  }

  return (
    <div className='w-full p-8 rounded-2xl outline outline-2 outline-offset-[-2px] outline-blue-100 flex flex-col items-center gap-3'>
      {/* WhatsApp icon */}
      <svg width='36' height='36' viewBox='0 0 36 36' fill='none' xmlns='http://www.w3.org/2000/svg'>
        <g clipPath='url(#wa-clip)'>
          <path
            d='M0.768226 17.7846C0.767383 20.8093 1.56388 23.7627 3.07841 26.3658L0.623383 35.2601L9.79663 32.8735C12.3338 34.2439 15.1766 34.9621 18.0654 34.9623H18.073C27.6095 34.9623 35.3724 27.2623 35.3765 17.7981C35.3783 13.212 33.58 8.89954 30.3127 5.65508C27.046 2.41089 22.7014 0.623309 18.0723 0.621216C8.53467 0.621216 0.772304 8.32075 0.768367 17.7846Z'
            fill='url(#wa-grad1)'
          />
          <path
            d='M0.150469 17.779C0.149484 20.9125 0.974531 23.9715 2.54306 26.6679L0 35.881L9.50217 33.4088C12.1203 34.8253 15.0681 35.572 18.0676 35.5732H18.0754C27.954 35.5732 35.9958 27.5962 36 17.7932C36.0017 13.0423 34.1387 8.57484 30.7547 5.21414C27.3703 1.85386 22.8703 0.00195349 18.0754 0C8.19506 0 0.154406 7.97581 0.150469 17.779ZM5.80936 26.2035L5.45456 25.6447C3.96309 23.2916 3.17587 20.5723 3.177 17.7801C3.18009 9.63195 9.86316 3.00279 18.081 3.00279C22.0607 3.00447 25.8007 4.54381 28.6138 7.33674C31.4267 10.13 32.9746 13.843 32.9736 17.7921C32.97 25.9402 26.2867 32.5702 18.0754 32.5702H18.0695C15.3958 32.5688 12.7735 31.8564 10.4867 30.51L9.94247 30.1898L4.30369 31.6567L5.80936 26.2035Z'
            fill='url(#wa-grad2)'
          />
          <path
            d='M13.5953 10.3462C13.2598 9.60629 12.9067 9.59136 12.5876 9.57838C12.3263 9.56722 12.0277 9.56805 11.7293 9.56805C11.4306 9.56805 10.9453 9.67954 10.5351 10.124C10.1244 10.5688 8.96738 11.6438 8.96738 13.8301C8.96738 16.0165 10.5723 18.1296 10.7961 18.4264C11.0201 18.7227 13.8945 23.353 18.4468 25.1343C22.2301 26.6146 23.0001 26.3202 23.8212 26.246C24.6424 26.172 26.4711 25.1713 26.8442 24.1335C27.2175 23.096 27.2175 22.2066 27.1056 22.0207C26.9937 21.8355 26.695 21.7243 26.2471 21.5022C25.7992 21.2801 23.5972 20.2048 23.1867 20.0565C22.776 19.9083 22.4775 19.8343 22.1788 20.2793C21.8801 20.7236 21.0225 21.7243 20.761 22.0207C20.4999 22.3178 20.2385 22.3548 19.7907 22.1325C19.3425 21.9095 17.9002 21.4408 16.1889 19.927C14.8575 18.749 13.9586 17.2944 13.6973 16.8494C13.436 16.4051 13.6693 16.1643 13.8939 15.9428C14.0951 15.7437 14.3419 15.4239 14.5661 15.1645C14.7894 14.905 14.8639 14.7198 15.0133 14.4234C15.1628 14.1268 15.0879 13.8673 14.9761 13.645C14.8639 13.4227 13.9936 11.2249 13.5953 10.3462Z'
            fill='white'
          />
        </g>
        <defs>
          <linearGradient id='wa-grad1' x1='18' y1='35.2601' x2='18' y2='0.621216' gradientUnits='userSpaceOnUse'>
            <stop stopColor='#1FAF38' />
            <stop offset='1' stopColor='#60D669' />
          </linearGradient>
          <linearGradient id='wa-grad2' x1='18' y1='35.881' x2='18' y2='0' gradientUnits='userSpaceOnUse'>
            <stop stopColor='#F9F9F9' />
            <stop offset='1' stopColor='white' />
          </linearGradient>
          <clipPath id='wa-clip'>
            <rect width='36' height='36' fill='white' />
          </clipPath>
        </defs>
      </svg>

      {/* Text */}
      <div className='flex flex-col items-center gap-1'>
        <h3 className='text-base font-medium font-switzer leading-4 text-gray-900'>{title}</h3>
        <p className='px-4 text-center text-xs font-normal font-switzer leading-4 text-[#5e5f5c]'>{description}</p>
      </div>

      {/* Button */}
      <a
        href={buttonHref}
        className='h-11 px-5 py-2.5 bg-white rounded-md shadow-[0px_3px_8px_0px_rgba(0,0,0,0.08),0px_1px_2px_0px_rgba(0,0,0,0.05)] [box-shadow:inset_0px_1px_0px_0px_rgba(255,255,255,0.80)] outline outline-1 outline-blue-500/30 inline-flex justify-center items-center text-blue-700 text-sm font-medium font-switzer leading-5 transition-colors duration-200 hover:bg-blue-50'
      >
        {buttonText}
      </a>
    </div>
  )
}

export default SupportCard
