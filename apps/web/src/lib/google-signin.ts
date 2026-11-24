import { getSessionSignal } from '@/lib/api-client'

declare global {
  interface Window {
    google?: { accounts: { id: {
      initialize: (options: { client_id: string; callback: (response: { credential: string }) => void }) => void
      prompt: () => void
    } } }
  }
}

let scriptPromise: Promise<void> | null = null

function loadIdentityScript(): Promise<void> {
  if (window.google) return Promise.resolve()
  if (scriptPromise) return scriptPromise
  scriptPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    const timer = setTimeout(() => finish(new Error('GOOGLE_UNAVAILABLE')), 10_000)
    function finish(error?: Error) {
      clearTimeout(timer)
      script.onload = null
      script.onerror = null
      if (error) { script.remove(); reject(error) } else resolve()
    }
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.defer = true
    script.dataset.googleIdentity = 'true'
    script.onload = () => finish(window.google ? undefined : new Error('GOOGLE_UNAVAILABLE'))
    script.onerror = () => finish(new Error('GOOGLE_UNAVAILABLE'))
    document.head.appendChild(script)
  }).catch(error => { scriptPromise = null; throw error })
  return scriptPromise
}

export async function startGoogleSignIn(onCredential: (credential: string) => void, componentSignal?: AbortSignal): Promise<void> {
  const signal = componentSignal ? AbortSignal.any([getSessionSignal(), componentSignal]) : getSessionSignal()
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
  if (!clientId || clientId === 'your-google-client-id') throw new Error('GOOGLE_NOT_CONFIGURED')
  await loadIdentityScript()
  if (signal.aborted) return
  window.google?.accounts.id.initialize({ client_id: clientId, callback: response => { if (!signal.aborted) onCredential(response.credential) } })
  window.google?.accounts.id.prompt()
}
