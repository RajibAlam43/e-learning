/**
 * i18n field helpers.
 *
 * The backend stores dynamic content with paired fields:
 *   `title`        — Bangla (primary, default)
 *   `titleEn`      — English
 *   `description`  — Bangla
 *   `descriptionEn`— English
 *   ... etc
 *
 * Both fields are returned on every response. The frontend picks which to
 * display based on the user's locale. These helpers do that picking safely,
 * always falling back to the other language if the preferred one is empty.
 */

import { getApiLocale } from '@/lib/api-client'

/** A record that *might* have localized variants of a field. */
export interface LocalizedFields {
  [banglaField: string]: unknown
}

/**
 * Pick the right variant of a field for the current locale.
 *
 * @example
 *   const title = localized(course, 'title')           // → course.title or course.titleEn
 *   const desc  = localized(course, 'description')     // → course.description or course.descriptionEn
 *
 * For Bangla users: prefers the unsuffixed field, falls back to *En.
 * For English users: prefers *En, falls back to the unsuffixed field.
 */
export function localized<T extends LocalizedFields>(
  record: T | null | undefined,
  field: string,
): string {
  if (!record) return ''
  const enField = `${field}En`
  const bangla = record[field]
  const english = record[enField]

  if (getApiLocale() === 'en') {
    return (typeof english === 'string' && english) || (typeof bangla === 'string' && bangla) || ''
  }
  return (typeof bangla === 'string' && bangla) || (typeof english === 'string' && english) || ''
}

/**
 * Localize a list of strings (e.g. `highlights`, `courseOutcomes`) — picks the
 * right variant element-by-element, falling back to the other language.
 */
export function localizedList<T extends LocalizedFields>(
  record: T | null | undefined,
  field: string,
): string[] {
  if (!record) return []
  const enField = `${field}En`
  const bangla = record[field]
  const english = record[enField]
  const banglaArr = Array.isArray(bangla) ? bangla : []
  const enArr = Array.isArray(english) ? english : []

  if (getApiLocale() === 'en') {
    return enArr.length > 0 ? enArr.map(String) : banglaArr.map(String)
  }
  return banglaArr.length > 0 ? banglaArr.map(String) : enArr.map(String)
}
