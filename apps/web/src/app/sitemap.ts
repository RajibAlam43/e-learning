import type { MetadataRoute } from 'next'
import axios from 'axios'
import { APP_CONFIG } from '@/config/app.config'
import { collectPages } from '@/lib/pagination'

export const revalidate = 3600

const API_URL = APP_CONFIG.api.serverUrl

interface PublicSitemapItem {
  id?: string
  slug?: string
  publishedAt?: string
}

function getContent(payload: unknown): PublicSitemapItem[] {
  if (Array.isArray(payload)) return payload as PublicSitemapItem[]
  if (!payload || typeof payload !== 'object') return []

  const data = payload as {
    content?: unknown
    data?: unknown
  }
  if (Array.isArray(data.content)) return data.content as PublicSitemapItem[]
  if (Array.isArray(data.data)) return data.data as PublicSitemapItem[]
  if (data.data && typeof data.data === 'object') {
    const nested = data.data as { content?: unknown }
    if (Array.isArray(nested.content)) return nested.content as PublicSitemapItem[]
  }
  return []
}

function publishedDate(value: string | undefined): Date | undefined {
  if (!value) return undefined
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? undefined : date
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = APP_CONFIG.url.replace(/\/+$/, '')
  const staticRoutes: MetadataRoute.Sitemap = [
    { url: baseUrl, changeFrequency: 'weekly', priority: 1 },
    { url: `${baseUrl}/courses`, changeFrequency: 'daily', priority: 0.9 },
    { url: `${baseUrl}/instructors`, changeFrequency: 'weekly', priority: 0.8 },
    { url: `${baseUrl}/programs`, changeFrequency: 'weekly', priority: 0.8 },
    { url: `${baseUrl}/collections`, changeFrequency: 'weekly', priority: 0.8 },
    { url: `${baseUrl}/verify-certificate`, changeFrequency: 'monthly', priority: 0.5 },
    { url: `${baseUrl}/contact`, changeFrequency: 'monthly', priority: 0.5 },
    { url: `${baseUrl}/privacy-policy`, changeFrequency: 'monthly', priority: 0.3 },
    { url: `${baseUrl}/terms`, changeFrequency: 'monthly', priority: 0.3 },
    { url: `${baseUrl}/cookie-policy`, changeFrequency: 'monthly', priority: 0.3 },
  ]

  const [coursesResult, instructorsResult, collectionsResult] = await Promise.allSettled([
    getPaginated('/public/courses'),
    axios.get(`${API_URL}/public/instructors`, { timeout: 15000 }),
    getPaginated('/public/collections'),
  ])

  const dynamicRoutes: MetadataRoute.Sitemap = []
  const addRoutes = (
    result: PromiseSettledResult<{ data: unknown }>,
    segment: 'courses' | 'instructors' | 'collections',
    changeFrequency: 'weekly' | 'monthly',
    priority: number,
  ) => {
    if (result.status !== 'fulfilled') return
    for (const item of getContent(result.value.data)) {
      const identifier = item.slug || item.id
      if (!identifier) continue
      dynamicRoutes.push({
        url: `${baseUrl}/${segment}/${encodeURIComponent(identifier)}`,
        lastModified: publishedDate(item.publishedAt),
        changeFrequency,
        priority,
      })
    }
  }

  addRoutes(coursesResult, 'courses', 'weekly', 0.8)
  addRoutes(instructorsResult, 'instructors', 'monthly', 0.6)
  addRoutes(collectionsResult, 'collections', 'weekly', 0.7)

  return [...staticRoutes, ...dynamicRoutes]
}

async function getPaginated(path: string) {
  const requestPage = async (page: number) => {
    const { data } = await axios.get(`${API_URL}${path}`, { params: { page, size: 100 }, timeout: 15000 })
    const payload: unknown = typeof data === 'string' ? JSON.parse(data) : data
    const totalPages = payload && typeof payload === 'object' && 'totalPages' in payload && typeof payload.totalPages === 'number' ? payload.totalPages : 1
    return { content: getContent(payload), totalPages }
  }
  return { data: await collectPages(await requestPage(0), requestPage) }
}
