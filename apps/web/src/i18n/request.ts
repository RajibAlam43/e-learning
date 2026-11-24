import { getRequestConfig } from 'next-intl/server'
import { cookies } from 'next/headers'
import en from '../../messages/en.json'
import bn from '../../messages/bn.json'
import { resolveLocale } from '@/lib/navigation'

// Catalogs are imported statically rather than via `import(\`...${locale}.json\`)`.
// A template-literal import compiles to a dynamic-context module, which the dev
// server does not reliably invalidate — editing a catalog left the running
// process serving the previously-parsed messages and throwing MISSING_MESSAGE.
const MESSAGES = { en, bn } as const

export default getRequestConfig(async () => {
  const cookieStore = await cookies()
  // Validated against the catalogs we actually ship: an unrecognised `locale`
  // cookie previously resolved to a missing module and 500'd every route.
  const locale = resolveLocale(cookieStore.get('locale')?.value)

  return {
    locale,
    messages: MESSAGES[locale],
  }
})
