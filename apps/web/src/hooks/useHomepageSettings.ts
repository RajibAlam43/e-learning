import { usePublicSettings } from '@/hooks/useSettings'
import { HOMEPAGE_SETTING_KEY, homepageSettings } from '@/config/homepage-settings'
export function useHomepageSettings() {
  const query = usePublicSettings()
  return { ...query, settings: homepageSettings(query.data?.find(s => s.key === HOMEPAGE_SETTING_KEY)?.value) }
}
