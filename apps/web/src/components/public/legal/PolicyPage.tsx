import Link from 'next/link'
import policies from '@/content/legal.json'

const policyPaths: Record<string, string> = { privacy: '/privacy-policy', terms: '/terms', cookies: '/cookie-policy' }

function PolicyText({ text }: { text: string }) {
  return text.split(/(https:\/\/[^\s]+|rajibalam@globalislamicinstitute\.com)/g).map((part, index) => {
    const url = part.startsWith('https://') ? part.replace(/\.$/, '') : undefined
    const email = part === 'rajibalam@globalislamicinstitute.com'
    return url || email
      ? <span key={index}><a href={url ?? `mailto:${part}`} className='break-words text-[#1c3873] underline underline-offset-4 hover:text-blue-700 focus-visible:outline-2 focus-visible:outline-offset-4'>{url ?? part}</a>{url && part.endsWith('.') ? '.' : ''}</span>
      : part
  })
}

export function PolicyPage({ kind }: { kind: keyof typeof policies }) {
  const policy = policies[kind]
  return <main className='bg-white px-5 py-12 sm:px-8 sm:py-16' lang='en'>
    <div className='mx-auto max-w-[72ch]'>
      <header className='max-w-[72ch]'>
        <h1 className='font-heading text-3xl font-medium text-[#1c3873] sm:text-4xl'>{policy.title}</h1>
        <p className='mt-3 text-sm text-slate-600'>Last updated {policy.updated}</p>
        <p className='mt-6 text-base leading-7 text-slate-700'><PolicyText text={policy.intro} /></p>
      </header>
      <div className='mt-10'>
        <article className='min-w-0 space-y-9'>{policy.sections.map(section =>
          <section key={section.id} id={section.id} className='scroll-mt-28'>
            <h2 className='mb-3 font-heading text-2xl font-medium text-[#1c3873]'>{section.title}</h2>
            <div className='space-y-4 text-base leading-7 text-slate-700'>{section.paragraphs.map(text => <p key={text}><PolicyText text={text} /></p>)}</div>
          </section>
        )}</article>
      </div>
      <div className='mt-12 flex flex-wrap gap-6 border-t border-slate-200 pt-6 text-sm text-[#1c3873]'>
        {Object.entries(policies).filter(([key]) => key !== kind).map(([key, item]) =>
          <Link key={key} href={policyPaths[key] ?? '/privacy-policy'} className='underline underline-offset-4'>{item.title}</Link>
        )}
        <Link href='/contact' className='underline underline-offset-4'>Contact the institute</Link>
      </div>
    </div>
  </main>
}
