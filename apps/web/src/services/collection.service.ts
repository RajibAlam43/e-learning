import { apiClient } from '@/lib/api-client'
import type { CertificateVerification, CollectionDetails, CollectionSummary } from '@/types'

export const collectionService = {
  getPublicCollections: async (params?: { page?: number; size?: number; search?: string }) =>
    (await apiClient.get<{ content?: CollectionSummary[]; totalPages?: number; totalElements?: number }>('/public/collections', { params })).data,
  getPublicCollection: async (slug: string) => (await apiClient.get<CollectionDetails>(`/public/collections/${slug}`)).data,
  verifyCertificate: async (code: string) => (await apiClient.get<CertificateVerification>(`/public/certificates/verify/${encodeURIComponent(code)}`)).data,
  createSupportTicket: async (data: import('@/types').CreateSupportTicketRequest) => (await apiClient.post('/public/support/tickets', data)).data,
}
