'use client'

import { useState, useEffect } from 'react'
import { useParams } from 'next/navigation'
import Image from 'next/image'
import Link from 'next/link'
import { useCourse } from '@/hooks/useCourses'
import { useCourseCheckout } from '@/hooks/useCourseCheckout'
import { useAuthStore } from '@/store/auth.store'
import { ErrorState } from '@/components/shared/ErrorState'
import { ChevronLeft, ChevronDown } from 'lucide-react'
import { useTranslations } from 'next-intl'
import { LegalAgreement } from '@/components/auth/LegalAgreement'

const COUNTRIES = [
  'Afghanistan','Albania','Algeria','Andorra','Angola','Antigua and Barbuda','Argentina','Armenia','Australia','Austria','Azerbaijan','Bahamas','Bahrain','Bangladesh','Barbados','Belarus','Belgium','Belize','Benin','Bhutan','Bolivia','Bosnia and Herzegovina','Botswana','Brazil','Brunei','Bulgaria','Burkina Faso','Burundi','Cabo Verde','Cambodia','Cameroon','Canada','Central African Republic','Chad','Chile','China','Colombia','Comoros','Congo','Costa Rica','Croatia','Cuba','Cyprus','Czech Republic','Denmark','Djibouti','Dominica','Dominican Republic','Ecuador','Egypt','El Salvador','Equatorial Guinea','Eritrea','Estonia','Eswatini','Ethiopia','Fiji','Finland','France','Gabon','Gambia','Georgia','Germany','Ghana','Greece','Grenada','Guatemala','Guinea','Guinea-Bissau','Guyana','Haiti','Honduras','Hungary','Iceland','India','Indonesia','Iran','Iraq','Ireland','Israel','Italy','Jamaica','Japan','Jordan','Kazakhstan','Kenya','Kiribati','Kosovo','Kuwait','Kyrgyzstan','Laos','Latvia','Lebanon','Lesotho','Liberia','Libya','Liechtenstein','Lithuania','Luxembourg','Madagascar','Malawi','Malaysia','Maldives','Mali','Malta','Marshall Islands','Mauritania','Mauritius','Mexico','Micronesia','Moldova','Monaco','Mongolia','Montenegro','Morocco','Mozambique','Myanmar','Namibia','Nauru','Nepal','Netherlands','New Zealand','Nicaragua','Niger','Nigeria','North Korea','North Macedonia','Norway','Oman','Pakistan','Palau','Palestine','Panama','Papua New Guinea','Paraguay','Peru','Philippines','Poland','Portugal','Qatar','Romania','Russia','Rwanda','Saint Kitts and Nevis','Saint Lucia','Saint Vincent and the Grenadines','Samoa','San Marino','Sao Tome and Principe','Saudi Arabia','Senegal','Serbia','Seychelles','Sierra Leone','Singapore','Slovakia','Slovenia','Solomon Islands','Somalia','South Africa','South Korea','South Sudan','Spain','Sri Lanka','Sudan','Suriname','Sweden','Switzerland','Syria','Taiwan','Tajikistan','Tanzania','Thailand','Timor-Leste','Togo','Tonga','Trinidad and Tobago','Tunisia','Turkey','Turkmenistan','Tuvalu','Uganda','Ukraine','United Arab Emirates','United Kingdom','United States','Uruguay','Uzbekistan','Vanuatu','Vatican City','Venezuela','Vietnam','Yemen','Zambia','Zimbabwe',
]

export default function CheckoutPage() {
  const params = useParams<{ slug: string }>()
  const { data: course, isLoading, isError, error, refetch } = useCourse(params.slug)
  const user = useAuthStore((s) => s.user)

  const [checkoutMethod, setCheckoutMethod] = useState<'email' | 'phone'>('email')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [agreedToTerms, setAgreedToTerms] = useState(false)
  const [country, setCountry] = useState('')
  const t = useTranslations('checkout')

  // Pre-fill from auth store
  useEffect(() => {
    // Form fields are intentionally synchronized when the authenticated profile arrives.
    if (user) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFullName(user.fullName || '')
      setEmail(user.email || user.phone || '')
    }
  }, [user])

  const { handleCheckout, processing } = useCourseCheckout({ slug: params.slug, courseId: course?.id, fullName, identifier: email, channel: checkoutMethod, agreedToTerms })

  if (!course && isLoading) {
    return <CheckoutSkeleton />
  }

  if (!course && isError) {
    return (
      <main className="bg-white">
        <div className="container mx-auto px-5 py-20">
          <ErrorState message={error?.message ?? t('courseNotFound')} onRetry={() => refetch()} />
        </div>
      </main>
    )
  }

  if (!course) return null

  const isFree = (course.priceBdt ?? -1) === 0
  const priceDisplay = isFree
    ? t('free')
    : course.priceBdt != null
      ? `${course.priceBdt.toLocaleString()} BDT`
      : 'TBA'

  return (
    <main className="bg-white">
      <div className="flex flex-col min-[936px]:flex-row">
        {/* ── Left Panel — Checkout Form ── */}
        <div className="flex-1 bg-white flex flex-col items-start py-12 px-[128px] max-[750px]:px-16 max-[675px]:py-[31px] max-[450px]:px-5 gap-6 min-w-0 min-[936px]:min-w-[468px]">
          {/* Back */}
          <Link
            href={`/courses/${params.slug}`}
            className="group flex items-center gap-2 no-underline text-[#80674a] hover:text-[#6b5740] transition-all duration-200"
          >
            <ChevronLeft className="h-6 w-6 transition-transform duration-200 group-hover:-translate-x-1" />
            <span className="text-[16px] leading-[24px] font-medium font-sans">{t('back')}</span>
          </Link>

          {/* Title */}
          <h2 className="m-0 self-stretch text-[32px] tracking-[-1px] leading-8 font-medium font-heading text-[#49608f] max-[450px]:text-[19px] max-[450px]:leading-[19px] max-[750px]:text-[26px] max-[750px]:leading-[26px]">
            {t('title')}
          </h2>

          {/* Tabs */}
          <div className="self-stretch rounded-lg bg-[#e8ebf1] flex items-center justify-center p-1 gap-3 max-[675px]:flex-wrap">
            <button
              type="button"
              onClick={() => setCheckoutMethod('email')}
              className={`flex-1 rounded-[4px] flex items-center justify-center p-2 min-w-[144px] transition-colors duration-200 cursor-pointer text-[14px] leading-[150%] font-sans ${
                checkoutMethod === 'email'
                  ? 'bg-[#1c3873] text-white hover:bg-[#425e99]'
                  : 'bg-transparent text-[#49608f]'
              }`}
            >
              {t('emailTab')}
            </button>
            <button
              type="button"
              onClick={() => setCheckoutMethod('phone')}
              className={`flex-1 rounded-[4px] flex items-center justify-center p-2 min-w-[144px] transition-colors duration-200 cursor-pointer text-[14px] leading-[150%] font-sans ${
                checkoutMethod === 'phone'
                  ? 'bg-[#1c3873] text-white hover:bg-[#425e99]'
                  : 'bg-transparent text-[#49608f]'
              }`}
            >
              {t('phoneTab')}
            </button>
          </div>

          {/* Form Fields */}
          <section className="self-stretch flex flex-col items-start gap-6 text-[16px] text-[#1b1c1a] font-sans">
            <div className="self-stretch flex flex-col items-start gap-4">
              {/* Full Name */}
              <div className="self-stretch flex flex-col items-start gap-2">
                <label className="leading-6 font-medium">
                  <span>{t('fullName')} </span>
                  <span className="text-[#ef4444]">*</span>
                </label>
                <div className="self-stretch h-[52px] rounded-[4px] border border-[#e5e5e5] overflow-hidden flex items-start pt-[15px] px-[17px] pb-3">
                  <input
                    type="text" aria-label={t('fullName')}
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Md. Abdur Rahman"
                    className="w-full border-none outline-none text-[14px] bg-transparent leading-[150%] text-[#5e5f5c] text-left p-0 font-sans"
                  />
                </div>
              </div>

              {/* Email / Phone */}
              {checkoutMethod === 'email' ? (
                <div className="self-stretch flex flex-col items-start gap-2">
                  <div className="leading-6 font-medium">
                    <span>{t('email')} </span>
                    <span className="text-[#ef4444]">*</span>
                  </div>
                  <div className="self-stretch h-[52px] rounded-[4px] border border-[#e5e5e5] overflow-hidden flex items-start pt-[15px] px-[17px] pb-3">
                    <input
                      type="email" aria-label={t('email')}
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="rahman1234@gmail.com"
                      className="w-full border-none outline-none text-[14px] bg-transparent leading-[150%] text-[#5e5f5c] text-left p-0 font-sans"
                    />
                  </div>
                </div>
              ) : (
                <div className="self-stretch flex flex-col items-start gap-2">
                  <div className="leading-6 font-medium">
                    <span>{t('phone')} </span>
                    <span className="text-[#ef4444]">*</span>
                  </div>
                  <div className="self-stretch h-[52px] rounded-[4px] border border-[#e5e5e5] overflow-hidden flex items-start pt-[15px] px-[17px] pb-3">
                    <input
                      type="tel" aria-label={t('phone')}
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="+880 1XXX-XXXXXX"
                      className="w-full border-none outline-none text-[14px] bg-transparent leading-[150%] text-[#5e5f5c] text-left p-0 font-sans"
                    />
                  </div>
                </div>
              )}

              {/* Country */}
              <div className="self-stretch flex flex-col items-start gap-2">
                <div className="leading-6">
                  <span className="font-medium">{t('country')} </span>
                  <span className="text-[#6b7280]">{t('optional')}</span>
                </div>
                <div className="relative self-stretch">
                  <select
                    aria-label={t('country')}
                    value={country}
                    onChange={(e) => setCountry(e.target.value)}
                    className="self-stretch h-[52px] w-full rounded-[4px] border border-[#e5e5e5] appearance-none bg-white pl-[17px] pr-10 text-[14px] leading-[150%] text-[#5e5f5c] font-sans cursor-pointer focus:outline-none focus:border-[#1c3873] transition-colors duration-200"
                  >
                    <option value="">{t('selectCountry')}</option>
                    {COUNTRIES.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-[17px] top-1/2 -translate-y-1/2 h-4 w-4 text-[#5e5f5c]" />
                </div>
              </div>
            </div>

            {/* Terms */}
            <LegalAgreement checked={agreedToTerms} onChange={setAgreedToTerms} />
          </section>
        </div>

        {/* ── Right Panel — Cart Review ── */}
        <div className="flex-1 bg-[#f5f5f5] flex flex-col items-center py-12 px-[128px] max-[750px]:px-16 max-[675px]:py-[31px] max-[450px]:px-5 gap-6 min-w-0 min-[936px]:min-w-[468px]">
          <h2 className="m-0 self-stretch text-[32px] tracking-[-1px] leading-8 font-medium font-heading text-[#49608f] max-[450px]:text-[19px] max-[450px]:leading-[19px] max-[750px]:text-[26px] max-[750px]:leading-[26px]">
            {t('reviewCart')}
          </h2>

          {/* Course Card */}
          <section className="self-stretch shadow-[0px_6px_15px_-2px_rgba(16,24,40,0.08)] rounded-lg bg-white flex flex-col items-start p-6 gap-4 text-[14px] text-[#5e5f5c] font-sans max-[450px]:p-5">
            {/* Thumbnail + Info Row */}
            <div className="self-stretch rounded-[4px] border-2 border-[#f5f5f5] flex flex-col">
              <div className="self-stretch h-[143px] rounded-lg bg-white overflow-hidden flex items-start max-[450px]:h-auto max-[450px]:flex-wrap">
                {/* Thumbnail */}
                <div className="self-stretch w-48 overflow-hidden shrink-0 flex flex-col items-start justify-center relative max-[450px]:flex-1">
                  {course.thumbnailUrl ? (
                    <Image
                      src={course.thumbnailUrl}
                      alt={course.title ?? 'Course'}
                      fill
                      className="object-cover z-0"
                      sizes="192px"
                    />
                  ) : (
                    <div className="h-[300px] w-full bg-[#e8ebf1]" />
                  )}
                  {course.level && (
                    <span className="absolute top-4 left-4 rounded-[4px] bg-[#b45309] flex items-center justify-center py-1 px-2 z-[1] text-[12px] leading-5 font-medium text-[#fffbeb] font-sans">
                      {course.level}
                    </span>
                  )}
                </div>

                {/* Info */}
                <div className="self-stretch flex-1 flex flex-col items-start justify-between p-4 gap-5 min-w-[146px]">
                  <div className="self-stretch flex flex-col items-start gap-1">
                    <span className="self-stretch leading-6">x1</span>
                    <h3 className="m-0 self-stretch text-[20px] leading-5 font-normal text-[#1c3873] font-heading max-[450px]:text-[16px] max-[450px]:leading-4">
                      {course.title}
                    </h3>
                  </div>

                  {/* Description bullets (hidden by default in Figma) */}
                  {course.shortDescription && (
                    <div className="flex flex-col items-start gap-1 text-[14px] text-[#49608f]">
                      <span className="flex items-center gap-2">
                        <Image src="/images/check-mark.png" alt="" width={18} height={18} className="h-[18px] w-[18px] shrink-0" />
                        <span className="leading-[150%]">{course.shortDescription}</span>
                      </span>
                    </div>
                  )}

                  <div className="self-stretch flex items-center justify-between text-[18px] text-[#917400]">
                    <span>
                      <span className="leading-6 font-semibold">{isFree ? t('free') : `${course.priceBdt?.toLocaleString() ?? 'TBA'} `}</span>
                      {!isFree && <span className="leading-7">BDT</span>}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Features List */}
            <ul className="self-stretch flex flex-col items-start gap-2 list-none p-0 m-0">
              <li className="self-stretch flex items-center gap-2">
                <Image src="/images/check-mark.png" alt="" width={16} height={16} className="h-4 w-4 shrink-0" />
                <span className="flex-1 text-sm leading-[150%] text-[#5e5f5c] font-sans">{t('oneTimePayment')}</span>
              </li>
              <li className="self-stretch flex items-center gap-2">
                <Image src="/images/check-mark.png" alt="" width={16} height={16} className="h-4 w-4 shrink-0" />
                <span className="flex-1 text-sm leading-[150%] text-[#5e5f5c] font-sans">{t('instantAccess')}</span>
              </li>
            </ul>

            {/* Discount + Pricing */}
            <div className="self-stretch flex flex-col items-end text-[#6b7280] max-[450px]:gap-5">
              <div className="self-stretch flex flex-col items-end gap-6">
                {/* Price Breakdown */}
                <div className="self-stretch flex flex-col items-start gap-3 text-right">
                  <div className="self-stretch flex items-center justify-between gap-5 max-[450px]:flex-wrap">
                    <span className="leading-[150%]">{t('subtotal')}</span>
                    <span className="leading-5 font-semibold text-[#49608f]">{priceDisplay}</span>
                  </div>
                  <div className="self-stretch flex items-center justify-between gap-5 max-[450px]:flex-wrap">
                    <span className="leading-[150%]">{t('discount')}</span>
                    <span className="leading-5 font-semibold text-[#49608f]">{isFree ? '-' : '-0 BDT'}</span>
                  </div>
                  <div className="self-stretch flex items-center justify-between gap-5 text-[18px] text-[#1c3873] max-[450px]:flex-wrap">
                    <span className="leading-6 font-semibold">{t('total')}</span>
                    <span className="leading-6 font-semibold">{priceDisplay}</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* Payment Buttons */}
          <div className="self-stretch flex flex-col items-center justify-center gap-2 text-[#5e5f5c]">
            {/* Pay with SSL Commerz */}
            <button
              type="button"
              onClick={() => handleCheckout('SSLCOMMERZ')}
              disabled={processing}
              className="self-stretch h-11 py-2.5 px-5 rounded-[6px] flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 hover:shadow-lg hover:scale-[1.01] text-[#1d4ed8] bg-white font-sans disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:scale-100"
              style={{
                boxShadow: '0px 1px 0px rgba(255,255,255,0.8) inset, 0px 1px 2px rgba(0,0,0,0.05), 0px 3px 8px rgba(0,0,0,0.08)',
              }}
            >
              <Image src="/images/sslcommerz.png" alt="SSL Commerz" width={96} height={24} className="h-6 w-auto" />
              <span className="text-base leading-6 font-semibold">
                {processing ? t('processing') : t('payWithSSL')}
              </span>
            </button>
            <button
              type="button"
              onClick={() => handleCheckout('BKASH')}
              disabled={processing}
              className="self-stretch h-11 py-2.5 px-5 rounded-[6px] flex items-center justify-center gap-2 cursor-pointer transition-all duration-200 hover:shadow-lg hover:scale-[1.01] text-[#e2136e] bg-white font-sans disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:scale-100"
              style={{ boxShadow: '0px 1px 0px rgba(255,255,255,0.8) inset, 0px 1px 2px rgba(0,0,0,0.05), 0px 3px 8px rgba(0,0,0,0.08)' }}
            >
              <Image src="/images/bkash.png" alt="bKash" width={96} height={24} className="h-6 w-auto" />
              <span className="text-base leading-6 font-semibold">{processing ? t('processing') : t('payWithBkash')}</span>
            </button>
          </div>

          {/* Secure Badge */}
          <div className="self-stretch flex items-center justify-center gap-2">
            <Image src="/images/icon-lock.png" alt="" width={16} height={16} className="h-4 w-4" />
            <span className="text-sm leading-5 font-medium text-[#1b1c1a] font-sans">{t('secureCheckout')}</span>
          </div>
        </div>
      </div>
    </main>
  )
}

/* ── Skeleton ── */
function CheckoutSkeleton() {
  return (
    <main className="bg-white">
      <div className="flex flex-col min-[936px]:flex-row">
        {/* Left */}
        <div className="flex-1 bg-white flex flex-col items-start py-12 px-[128px] max-[750px]:px-16 max-[675px]:py-[31px] max-[450px]:px-5 gap-6 min-w-0 min-[936px]:min-w-[468px]">
          <div className="skeleton-shimmer h-6 w-[80px] rounded" />
          <div className="skeleton-shimmer h-8 w-[160px] rounded" />
          <div className="skeleton-shimmer self-stretch h-11 rounded-lg" />
          <div className="self-stretch flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <div className="skeleton-shimmer h-4 w-[100px] rounded" />
              <div className="skeleton-shimmer h-[52px] w-full rounded" />
            </div>
            <div className="flex flex-col gap-2">
              <div className="skeleton-shimmer h-4 w-[130px] rounded" />
              <div className="skeleton-shimmer h-[52px] w-full rounded" />
            </div>
            <div className="flex flex-col gap-2">
              <div className="skeleton-shimmer h-4 w-[120px] rounded" />
              <div className="skeleton-shimmer h-[52px] w-full rounded" />
            </div>
          </div>
          <div className="skeleton-shimmer h-6 w-[300px] rounded" />
        </div>
        {/* Right */}
        <div className="flex-1 bg-[#f5f5f5] flex flex-col items-start py-12 px-[128px] max-[750px]:px-16 max-[675px]:py-[31px] max-[450px]:px-5 gap-6 min-w-0 min-[936px]:min-w-[468px]">
          <div className="skeleton-shimmer self-stretch h-8 w-[220px] rounded" />
          <div className="self-stretch rounded-lg bg-white p-6 flex flex-col gap-4">
            <div className="skeleton-shimmer h-[143px] w-full rounded-lg" />
            <div className="flex flex-col gap-2">
              <div className="skeleton-shimmer h-4 w-[160px] rounded" />
              <div className="skeleton-shimmer h-4 w-[220px] rounded" />
            </div>
            <div className="skeleton-shimmer h-10 w-full rounded" />
            <div className="flex flex-col gap-3 items-end">
              <div className="skeleton-shimmer h-4 w-full rounded" />
              <div className="skeleton-shimmer h-4 w-full rounded" />
              <div className="skeleton-shimmer h-5 w-[120px] rounded" />
            </div>
          </div>
          <div className="skeleton-shimmer self-stretch h-11 rounded-md" />
          <div className="self-stretch flex items-center gap-2">
            <div className="h-px flex-1 bg-[#e5e5e5]" />
            <span className="text-[14px] text-[#6b7280]">Or</span>
            <div className="h-px flex-1 bg-[#e5e5e5]" />
          </div>
          <div className="skeleton-shimmer self-stretch h-11 rounded-md" />
          <div className="skeleton-shimmer self-stretch h-6 w-[260px] rounded mx-auto" />
        </div>
      </div>
    </main>
  )
}
