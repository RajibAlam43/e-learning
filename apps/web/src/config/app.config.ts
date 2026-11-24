export const APP_CONFIG = {
  name: 'Global Islamic Institute',
  shortName: 'GII',
  description: 'Online Islamic education platform',
  url: process.env.NEXT_PUBLIC_APP_URL ?? 'https://globalislamicinstitute.com',
  api: {
    baseUrl: process.env.NEXT_PUBLIC_API_URL ?? 'https://stage-api.globalislamicinstitute.com',
    serverUrl: process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? 'https://stage-api.globalislamicinstitute.com',
  },
  locale: {
    default: 'bn',
    supported: ['en', 'bn'] as const,
  },
} as const
