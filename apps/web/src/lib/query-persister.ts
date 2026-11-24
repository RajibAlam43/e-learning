import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister'
import { get, set, del } from 'idb-keyval'

export function getQueryPersister() {
  if (typeof window === 'undefined') return undefined

  return createAsyncStoragePersister({
    storage: {
      getItem: async (key) => {
        return await get(key)
      },
      setItem: async (key, value) => {
        await set(key, value)
      },
      removeItem: async (key) => {
        await del(key)
      },
    },
    key: 'lms-offline-cache',
  })
}
