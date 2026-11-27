'use client'

import { useQuery } from '@tanstack/react-query'
import { settingsService } from '@/services/settings.service'

export function usePublicSettings() {
  return useQuery({
    queryKey: ['public', 'settings'],
    queryFn: settingsService.getPublicSettings,
    staleTime: 5 * 60 * 1000,
    retry: 1,
    refetchOnMount: 'always',
    refetchOnWindowFocus: 'always',
  })
}
