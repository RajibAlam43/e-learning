'use client'

import { useTranslations } from 'next-intl'

import { useState } from 'react'
import Link from 'next/link'
import { useCertificates } from '@/hooks/useStudentDashboard'
import { studentService } from '@/services/student.service'
import { toast } from 'sonner'
import { EmptyState } from '@/components/shared/EmptyState'
import { ErrorState } from '@/components/shared/ErrorState'
import {
  Award,
  Download,
  ExternalLink,
  Search,
  Calendar,
  BookOpen,
  Loader2,
} from 'lucide-react'

interface Certificate {
  id: string
  certificateCode: string
  courseName: string
  courseSlug: string
  recipientName: string
  issuedAt: string
  pdfUrl: string
  downloadEndpoint: string
  verificationUrl: string
  isRevoked: boolean
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function CertificateCard({ cert }: { cert: Certificate }) {
  const ui = useTranslations('privateUi')

  const [downloading, setDownloading] = useState(false)

  const handleDownload = async () => {
    setDownloading(true)
    try {
      const result = await studentService.getCertificateDownloadUrl(cert.id)
      if (cert.isRevoked) throw new Error('This certificate has been revoked.')
      const url = result.downloadUrl ?? cert.pdfUrl
      if (!url) throw new Error('Download is not available yet.')
      window.open(url, '_blank', 'noopener,noreferrer')
    } catch (error) {
      toast.error((error as { message?: string }).message ?? ui('unableToDownloadCertificate'))
    } finally { setDownloading(false) }
  }

  return (
    <div className='bg-white rounded-2xl shadow-[0px_2px_6px_0px_rgba(16,24,40,0.06)] overflow-hidden transition-all duration-300 hover:-translate-y-0.5 hover:shadow-[0px_8px_20px_0px_rgba(20,26,184,0.12)]'>
      {/* Certificate preview */}
      <div className='relative w-full h-40 bg-gradient-to-br from-[#1c3873] to-[#3b82f6] flex flex-col items-center justify-center gap-2 p-4'>
        <div className='w-14 h-14 rounded-full bg-white/20 flex items-center justify-center'>
          <Award className='w-8 h-8 text-white' />
        </div>
        <p className='text-white/80 text-xs font-medium tracking-wider uppercase'>
          {ui('certificateOfCompletion')}</p>
        <p className='text-white text-sm font-medium font-switzer text-center line-clamp-1'>
          {cert.courseName}
        </p>
        {cert.isRevoked && <span className='rounded bg-red-500/90 px-2 py-0.5 text-[10px] font-semibold uppercase text-white'>{ui('revoked')}</span>}
      </div>

      {/* Content */}
      <div className='p-4 flex flex-col gap-3'>
        <h3 className='text-base font-medium font-switzer leading-6 text-[#1c3873] line-clamp-1'>
          {cert.courseName}
        </h3>

        <div className='flex flex-col gap-1.5'>
          <div className='flex items-center gap-2 text-sm text-[#5e5f5c]'>
            <Calendar className='w-4 h-4 text-[#3b82f6] shrink-0' />
            {ui('issued')}{' '}{formatDate(cert.issuedAt)}
          </div>
          <div className='flex items-center gap-2 text-sm text-[#5e5f5c]'>
            <BookOpen className='w-4 h-4 text-[#3b82f6] shrink-0' />
            {ui('code')}{' '}<span className='font-mono text-xs text-[#1c3873]'>{cert.certificateCode}</span>
          </div>
        </div>

        <p className='text-xs text-[#a3a3a3]'>
          {ui('awardedTo')}{' '}<span className='text-[#1c3873] font-medium'>{cert.recipientName}</span>
        </p>

        {/* Actions */}
        <div className='flex gap-2 pt-1'>
          <button
            onClick={handleDownload}
            disabled={downloading || cert.isRevoked}
            className='flex-1 h-9 flex items-center justify-center gap-1.5 rounded-md bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] text-white text-xs font-medium font-switzer disabled:opacity-50 hover:opacity-90 transition-opacity cursor-pointer'
          >
            {downloading ? <Loader2 className='w-3.5 h-3.5 animate-spin' /> : <Download className='w-3.5 h-3.5' />}
            {ui('downloadPdf')}</button>
          <Link
            href={cert.verificationUrl || '#'}
            target='_blank'
            className='flex-1 h-9 flex items-center justify-center gap-1.5 rounded-md bg-white outline outline-1 outline-blue-500/30 text-blue-500 text-xs font-medium font-switzer shadow-[0px_2px_6px_0px_rgba(0,0,0,0.05)] hover:bg-blue-50 transition-colors'
          >
            <ExternalLink className='w-3.5 h-3.5' />
            {ui('verify')}</Link>
        </div>
      </div>
    </div>
  )
}

export default function CertificatesPage() {
  const ui = useTranslations('privateUi')

  const { data: apiCerts, isLoading, isError, refetch } = useCertificates()

  const certificates: Certificate[] = apiCerts && apiCerts.length > 0
    ? apiCerts.map((c) => ({
        id: c.certificateId ?? '',
        certificateCode: c.certificateCode ?? '',
        courseName: c.targetName ?? '',
        courseSlug: c.targetSlug ?? '',
        recipientName: c.recipientName ?? '',
        issuedAt: c.issuedAt ?? '',
        pdfUrl: c.pdfUrl ?? '',
        downloadEndpoint: c.downloadEndpoint ?? '',
        verificationUrl: c.verificationUrl ?? '',
        isRevoked: c.isRevoked ?? false,
      }))
    : []

  const [search, setSearch] = useState('')

  const filtered = certificates.filter((c) =>
    c.courseName.toLowerCase().includes(search.toLowerCase()) ||
    c.certificateCode.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className='flex flex-col gap-6'>
      {/* Header */}
      <div className='flex flex-col gap-1'>
        <h1 className='text-2xl font-heading font-medium text-[#1c3873]'>{ui('certificates')}</h1>
        <p className='text-sm text-[#5e5f5c]'>
          {certificates.length} {ui('certificate')}{certificates.length !== 1 ? ui('s2') : ''} {ui('earned')}</p>
      </div>

      {/* Search */}
      {certificates.length > 0 && (
        <div className='relative'>
          <Search className='absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#97a3bf]' />
          <input
            type='text'
            placeholder={ui('searchCertificates')}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className='w-full h-10 pl-10 pr-4 rounded-lg border border-gray-200 bg-white text-sm text-[#1b1c1a] placeholder:text-[#97a3bf] focus:outline-none focus:ring-2 focus:ring-[#3b82f6] focus:border-transparent transition-all'
          />
        </div>
      )}

      {/* Error */}
      {isError && !isLoading && (
        <ErrorState message={ui('failedToLoadCertificatesPleaseTryAgain')} onRetry={() => refetch()} />
      )}

      {/* Loading */}
      {isLoading && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {[1, 2, 3].map((i) => (
            <div key={i} className='bg-white rounded-2xl overflow-hidden animate-pulse'>
              <div className='h-40 bg-gray-200' />
              <div className='p-4 space-y-3'>
                <div className='h-5 w-3/4 bg-gray-100 rounded' />
                <div className='h-4 w-1/2 bg-gray-100 rounded' />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Grid */}
      {!isLoading && filtered.length > 0 && (
        <div className='grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4'>
          {filtered.map((cert) => (
            <CertificateCard key={cert.id} cert={cert} />
          ))}
        </div>
      )}

      {/* No results */}
      {!isLoading && search && filtered.length === 0 && certificates.length > 0 && (
        <EmptyState
          title={ui('noCertificatesFound')}
          description={`No certificates matching "${search}"`}
          icon={<Search className='h-8 w-8 text-[#97a3bf]' />}
        />
      )}

      {/* Empty */}
      {!isLoading && certificates.length === 0 && (
        <EmptyState
          title={ui('noCertificatesYet')}
          description={ui('completeCoursesToEarnCertificatesTheyWillAppearHere')}
          icon={<Award className='h-8 w-8 text-[#97a3bf]' />}
          action={
            <Link
              href='/student/courses'
              className='h-10 px-5 flex items-center justify-center gap-2 rounded-md bg-[linear-gradient(20deg,rgba(79,103,216,1)_0%,rgba(58,79,194,1)_60%,rgba(46,63,168,1)_100%)] text-white text-sm font-medium font-switzer'
            >
              {ui('browseCourses')}</Link>
          }
        />
      )}
    </div>
  )
}
