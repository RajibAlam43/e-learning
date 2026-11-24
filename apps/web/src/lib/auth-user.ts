import type { UserRole } from '@/types'

export interface AuthUser {
  id: string
  fullName: string
  email?: string
  phone?: string
  roles: UserRole[]
  avatar?: string
}

const allowedRoles = new Set<string>(['STUDENT', 'INSTRUCTOR', 'TEACHER', 'ADMIN', 'SUPER_ADMIN'])

/** Cached profiles are hints; malformed data must never drive role-dependent UI. */
export function normalizeAuthUser(value: unknown): AuthUser | null {
  if (!value || typeof value !== 'object') return null
  const data = value as Record<string, unknown>
  const id = data.id ?? data.userId
  if (typeof id !== 'string' || !id || !Array.isArray(data.roles)) return null
  if (!data.roles.every(role => typeof role === 'string')) return null
  const roles = data.roles.filter((role): role is UserRole => typeof role === 'string' && allowedRoles.has(role))
  if (!roles.length) return null
  return {
    id,
    fullName: typeof data.fullName === 'string' ? data.fullName : '',
    email: typeof data.email === 'string' ? data.email : undefined,
    phone: typeof data.phone === 'string' ? data.phone : undefined,
    avatar: typeof data.avatar === 'string' ? data.avatar : typeof data.avatarUrl === 'string' ? data.avatarUrl : undefined,
    roles,
  }
}
