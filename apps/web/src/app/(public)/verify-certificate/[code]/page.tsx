import type { Metadata } from 'next'
import { CheckCircle2, XCircle } from 'lucide-react'
import { collectionService } from '@/services/collection.service'
import type { CertificateVerification } from '@/types'

export const metadata: Metadata = {
  title: 'Certificate Verification Result',
  robots: { index: false, follow: false },
}

export default async function CertificateResultPage({ params }: { params: Promise<{ code: string }> }) {
  const { code } = await params
  let result: CertificateVerification | null = null
  try {
    result = await collectionService.verifyCertificate(code)
  } catch {
    result = null
  }

  if (!result) {
    return <CertificatePanel valid={false} title="Certificate not found" message="Check the code and try again." code={code} />
  }

  const valid = result.isValid !== false && !result.isRevoked
  return <CertificatePanel valid={valid} title={valid ? 'Certificate verified' : 'Certificate revoked'} code={code} recipient={result.recipientName} achievement={result.targetName} />
}

function CertificatePanel({ valid, title, message, code, recipient, achievement }: { valid: boolean; title: string; message?: string; code: string; recipient?: string; achievement?: string }) {
  const Icon = valid ? CheckCircle2 : XCircle
  return <main className="mx-auto max-w-xl px-4 py-20"><div className="rounded-2xl bg-white p-8 text-center shadow-sm"><Icon className={`mx-auto h-14 w-14 ${valid ? 'text-green-600' : 'text-red-600'}`} /><h1 className="mt-4 font-heading text-3xl text-[#1c3873]">{title}</h1>{message && <p className="mt-2 text-slate-600">{message}</p>}<dl className="mt-6 grid gap-3 text-left text-sm">{recipient && <div><dt className="text-slate-500">Recipient</dt><dd className="font-medium">{recipient}</dd></div>}{achievement && <div><dt className="text-slate-500">Achievement</dt><dd className="font-medium">{achievement}</dd></div>}<div><dt className="text-slate-500">Code</dt><dd className="font-mono">{code}</dd></div></dl></div></main>
}
