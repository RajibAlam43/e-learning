'use client'

import { useMemo, useState } from 'react'

/**
 * useClientPagination — paginates an in-memory array client-side.
 *
 * Use when the data source is small enough to fetch in full (e.g., admin
 * dashboard widgets showing recent items). For large datasets, switch to
 * server-side pagination (page/size query params on the hook).
 *
 * @example
 *   const { items, page, totalPages, setPage } = useClientPagination(courses, 5)
 */
export function useClientPagination<T>(items: T[], pageSize: number) {
  const [page, setPage] = useState(0)

  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(items.length / pageSize)),
    [items.length, pageSize],
  )

  // Clamp page if items shrink (e.g., after a delete).
  const safePage = Math.min(page, totalPages - 1)

  const paginatedItems = useMemo(
    () => items.slice(safePage * pageSize, safePage * pageSize + pageSize),
    [items, safePage, pageSize],
  )

  function next() {
    setPage((p) => Math.min(totalPages - 1, p + 1))
  }

  function prev() {
    setPage((p) => Math.max(0, p - 1))
  }

  function reset() {
    setPage(0)
  }

  return {
    items: paginatedItems,
    page: safePage,
    totalPages,
    hasNext: safePage < totalPages - 1,
    hasPrev: safePage > 0,
    next,
    prev,
    setPage,
    reset,
    totalItems: items.length,
  }
}
