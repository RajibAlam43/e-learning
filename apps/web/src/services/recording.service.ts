import { apiClient } from '@/lib/api-client'

export interface RecordingInfo {
  id: string; status: 'UPLOADING' | 'QUEUED' | 'PROCESSING' | 'READY' | 'FAILED';
  method: 'BROWSER' | 'EXTENSION'; createdAt: string; durationSeconds: number | null; failureMessage: string | null
}
export interface RecordingClass {
  enabled: boolean; title: string; meetingUrl: string; classStatus: string; recordings: RecordingInfo[]
}
export interface UploadSession {
  id: string; uploadToken: string; expiresAt: string; mimeType: string; partSizeBytes: number; maxBytes: number
}
export interface GoogleConnection {
  connected: boolean; email: string | null; sharedOrganizerEnabled: boolean; calendarSyncEnabled: boolean
}
export const recordingService = {
  list: async (classId: string) => (await apiClient.get<RecordingClass>(`/instructor/live-classes/${classId}/recordings`)).data,
  start: async (classId: string, mimeType: string) => (await apiClient.post<UploadSession>(`/instructor/live-classes/${classId}/recordings`, { method: 'BROWSER', mimeType })).data,
  resume: async (id: string) => (await apiClient.post<UploadSession>(`/instructor/recordings/${id}/resume`)).data,
  startClass: async (classId: string) => (await apiClient.post(`/live-classes/${classId}/start`)).data,
  connection: async () => (await apiClient.get<GoogleConnection>('/instructor/google-meet/connection')).data,
  authorize: async () => (await apiClient.post<{ authorizationUrl: string }>('/instructor/google-meet/connection/authorize')).data,
  disconnect: async () => { await apiClient.delete('/instructor/google-meet/connection') },
}

// Scoped upload capabilities deliberately bypass the LMS bearer-token/cookie client.
export async function uploadControl<T>(id: string, token: string, suffix: string): Promise<T> {
  const base = apiClient.defaults.baseURL?.replace(/\/$/, '') ?? ''
  const response = await fetch(`${base}/recording-uploads/${id}/${suffix}`, {
    method: 'POST', credentials: 'omit', referrerPolicy: 'no-referrer',
    headers: { 'X-Recording-Token': token }, signal: AbortSignal.timeout(30000),
  })
  if (!response.ok) {
    throw new Error(`Upload authorization failed (${response.status}). Sign in and retry uploading.`)
  }
  return response.json() as Promise<T>
}
