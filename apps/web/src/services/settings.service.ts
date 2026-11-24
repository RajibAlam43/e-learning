import { apiClient } from '@/lib/api-client'
import type { components } from '@/types/api'

type PublicAppSetting = components['schemas']['PublicAppSettingResponse']

export const settingsService = {
  getPublicSettings: async (): Promise<PublicAppSetting[]> =>
    (await apiClient.get<PublicAppSetting[]>('/public/settings')).data,
}
