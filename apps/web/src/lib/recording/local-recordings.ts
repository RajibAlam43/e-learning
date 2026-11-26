import { createStore, get, getMany, setMany, delMany, keys } from 'idb-keyval'

const store = createStore('lms-class-recordings', 'recordings')
export interface LocalRecording {
  localId: string; ownerId: string; classId: string; title: string; mimeType: string;
  recordingId: string; startedAt: number; chunkCount: number; bytes: number;
  uploadedBytes: number; parts: number; cursorChunk: number; cursorOffset: number;
  phase: 'recording' | 'stopped' | 'queued'; partSize: number; maxBytes: number
}
export const metaKey = (id: string) => `meta:${id}`
export const chunkKey = (id: string, index: number) => `chunk:${id}:${index}`
export async function readLocal(id: string) { return get<LocalRecording>(metaKey(id), store) }
export async function saveLocal(meta: LocalRecording, blob?: Blob) {
  const entries: [IDBValidKey, unknown][] = [[metaKey(meta.localId), meta]]
  if (blob) entries.push([chunkKey(meta.localId, meta.chunkCount - 1), blob])
  await setMany(entries, store)
}
export async function listLocal(ownerId: string, classId: string) {
  const ids = (await keys(store)).filter(key => typeof key === 'string' && key.startsWith('meta:'))
  const metadata = await getMany<LocalRecording>(ids, store)
  return metadata.filter((entry): entry is LocalRecording => Boolean(entry && entry.ownerId === ownerId && entry.classId === classId))
}
export async function readPart(meta: LocalRecording, size: number) {
  const blobs: Blob[] = []
  let index = meta.cursorChunk, offset = meta.cursorOffset, remaining = size
  while (remaining > 0 && index < meta.chunkCount) {
    const blob = await get<Blob>(chunkKey(meta.localId, index), store)
    if (!blob) throw new Error('Some local recording data is missing. Keep the recording and contact support.')
    const count = Math.min(remaining, blob.size - offset)
    blobs.push(blob.slice(offset, offset + count))
    remaining -= count; offset += count
    if (offset === blob.size) { index++; offset = 0 }
  }
  if (remaining) throw new Error('The local recording is incomplete.')
  return { blob: new Blob(blobs), index, offset }
}
export async function downloadLocal(meta: LocalRecording, emergency: Map<number, Blob> = new Map()) {
  const chunks: Blob[] = []
  for (let i = 0; i < meta.chunkCount; i++) {
    const blob = emergency.get(i) ?? await get<Blob>(chunkKey(meta.localId, i), store)
    if (!blob) throw new Error('Some recording data is missing. Contact support before deleting this copy.')
    chunks.push(blob)
  }
  const url = URL.createObjectURL(new Blob(chunks, { type: meta.mimeType }))
  const anchor = document.createElement('a')
  anchor.href = url; anchor.download = `class-${meta.classId}-${meta.startedAt}.mp4`; anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 60000)
}
export async function removeLocal(meta: LocalRecording) {
  await delMany([metaKey(meta.localId), ...Array.from({ length: meta.chunkCount }, (_, i) => chunkKey(meta.localId, i))], store)
}
