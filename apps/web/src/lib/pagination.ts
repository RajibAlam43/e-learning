import { NETWORK } from '@/config/network.config'

export interface PageData<T> {
  content?: T[]
  totalPages?: number
}

/** Fetch remaining pages in small batches rather than a request per record set at once. */
export async function collectPages<T>(first: PageData<T>, requestPage: (page: number) => Promise<PageData<T>>, signal?: AbortSignal): Promise<T[]> {
  const items = [...(first.content ?? [])]
  const totalPages = first.totalPages ?? 1
  validatePageCount(totalPages)
  for (let page = 1; page < totalPages; page += 4) {
    signal?.throwIfAborted()
    const pages = await Promise.all(Array.from({ length: Math.min(4, totalPages - page) }, (_, index) => requestPage(page + index)))
    signal?.throwIfAborted()
    pages.forEach(result => validatePageCount(result.totalPages ?? totalPages))
    items.push(...pages.flatMap(result => result.content ?? []))
  }
  return items
}

function validatePageCount(totalPages: number): void {
  if (!Number.isSafeInteger(totalPages) || totalPages < 0 || totalPages > NETWORK.maxCollectedPages) {
    throw new Error(`The result is too large or has invalid pagination. Narrow your filters and try again (limit: ${NETWORK.maxCollectedPages} pages).`)
  }
}
