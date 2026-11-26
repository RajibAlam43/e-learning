/** Matches the backend AdminUserSummaryResponse and PageResponse contracts. */
export interface AdminUserSummary {
  userId: string
  fullName: string
  email: string | null
  phone: string | null
  phoneCountryCode: string | null
  roles: string[]
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED'
  createdAt: string
}

export interface AdminUsersPage {
  content: AdminUserSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface AdminUsersParams {
  search: string
  page: number
  size: number
}
