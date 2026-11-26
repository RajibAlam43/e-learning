import { recordingService, uploadControl } from '@/services/recording.service'
import type { UploadSession } from '@/services/recording.service'
import { saveLocal, readPart, downloadLocal } from './local-recordings'
import type { LocalRecording } from './local-recordings'

export interface RecorderState {
  stage: 'idle' | 'preparing' | 'ready' | 'recording' | 'stopped' | 'uploading' | 'queued';
  error: string; uploadError: string; bytes: number; uploaded: number; sampleUrl: string; seconds: number
}
export function supportedRecordingMime(): string | null {
  if (typeof MediaRecorder === 'undefined') return null
  return ['video/mp4;codecs=avc1.42E01F,mp4a.40.2', 'video/mp4;codecs=avc1,mp4a.40.2']
    .find(mime => MediaRecorder.isTypeSupported(mime)) ?? null
}
export class BrowserClassRecorder {
  private state: RecorderState = { stage: 'idle', error: '', uploadError: '', bytes: 0, uploaded: 0, sampleUrl: '', seconds: 0 }
  private display?: MediaStream
  private microphone?: MediaStream
  private mixed?: MediaStream
  private context?: AudioContext
  private recorder?: MediaRecorder
  private metadata?: LocalRecording
  private session?: UploadSession
  private writes: Promise<void> = Promise.resolve()
  private pumping?: Promise<void>
  private finishRequested = false
  private disposed = false
  private storageFailed = false
  private emergency = new Map<number, Blob>()
  private nextChunk = 0
  private releaseLock?: () => void
  private interval?: ReturnType<typeof setInterval>
  private stopPromise?: Promise<void>
  private sampling = false
  public tabMeter?: AnalyserNode
  public micMeter?: AnalyserNode

  constructor(private ownerId: string, private stillOwner: () => boolean,
    private changed: (state: RecorderState) => void, private preview: (stream: MediaStream | null) => void) {}

  private emit(patch: Partial<RecorderState>) {
    this.state = { ...this.state, ...patch }
    if (!this.disposed) this.changed(this.state)
  }
  private assertOwner() {
    if (this.disposed || !this.stillOwner()) throw new Error('Your LMS session changed. Sign in as the recording owner to continue.')
  }
  private async lock(id: string) {
    if (!navigator.locks) throw new Error('This browser cannot safely resume recording uploads. Use desktop Chrome.')
    let resolve!: () => void
    let reject!: (error: Error) => void
    const acquired = new Promise<void>((yes, no) => { resolve = yes; reject = no })
    void navigator.locks.request(`lms-recording:${id}`, { ifAvailable: true }, async lock => {
      if (!lock) { reject(new Error('This recording is open in another tab. Close that tab first.')); return }
      await new Promise<void>(release => { this.releaseLock = release; resolve() })
    }).catch(() => reject(new Error('Could not lock this recording. Close other recorder tabs and try again.')))
    await acquired
  }
  async prepare() {
    this.assertOwner()
    if (!window.isSecureContext || !navigator.mediaDevices?.getDisplayMedia) throw new Error("Recording needs desktop Chrome over HTTPS (or localhost).")
    const mime = supportedRecordingMime()
    if (!mime) throw new Error('This device cannot record H.264/AAC MP4. Try desktop Chrome on another supported device.')
    this.stopPromise = undefined; this.finishRequested = false
    this.emit({ stage: 'preparing', error: '', uploadError: '' })
    try {
      // The picker must run directly from the teacher's click, before other async work.
      const options = { video: true, audio: { suppressLocalAudioPlayback: false },
        selfBrowserSurface: 'exclude', systemAudio: 'exclude', surfaceSwitching: 'exclude' }
      this.display = await navigator.mediaDevices.getDisplayMedia(options as DisplayMediaStreamOptions)
      this.assertOwner()
      const track = this.display.getVideoTracks()[0]
      if (!track || !this.display.getAudioTracks().length || track.getSettings().displaySurface !== 'browser') {
        throw new Error('Select the Google Meet Chrome tab and turn on “Share tab audio”.')
      }
      await track.applyConstraints({ width: { max: 1280 }, height: { max: 720 }, frameRate: { max: 15 } })
      this.microphone = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }, video: false })
      this.assertOwner()
      if (track.readyState !== 'live') throw new Error('The shared tab closed. Select the Meet tab again.')
      this.context = new AudioContext()
      await this.context.resume()
      this.assertOwner()
      const destination = this.context.createMediaStreamDestination()
      const tab = this.context.createMediaStreamSource(this.display)
      const mic = this.context.createMediaStreamSource(this.microphone)
      this.tabMeter = this.context.createAnalyser(); this.micMeter = this.context.createAnalyser()
      for (const [source, analyser] of [[tab, this.tabMeter], [mic, this.micMeter]] as const) {
        const gain = this.context.createGain(); gain.gain.value = 0.7
        source.connect(analyser); analyser.connect(gain); gain.connect(destination)
      }
      this.mixed = new MediaStream([track, ...destination.stream.getAudioTracks()])
      // Fail before a class starts if the claimed codec cannot actually initialize.
      new MediaRecorder(this.mixed, { mimeType: mime })
      this.preview(this.mixed)
      track.addEventListener('ended', () => { void this.stop().catch(error => this.emit({ error: message(error) })) })
      this.microphone.getAudioTracks()[0]?.addEventListener('ended', () => {
        this.emit({ error: 'Microphone access ended. Recording stopped; reconnect the microphone before a new recording.' })
        void this.stop().catch(error => this.emit({ error: message(error) }))
      })
      this.emit({ stage: 'ready' })
    } catch (error) {
      this.releaseMedia(); this.emit({ stage: 'idle', error: message(error) }); throw error
    }
  }
  private makeRecorder() {
    if (!this.mixed) throw new Error('Select the Meet tab and microphone first.')
    return new MediaRecorder(this.mixed, { mimeType: supportedRecordingMime()!, videoBitsPerSecond: 1200000,
      audioBitsPerSecond: 64000, videoKeyFrameIntervalDuration: 2000 } as MediaRecorderOptions)
  }
  async sample() {
    if (this.sampling || this.state.stage !== 'ready') return
    this.sampling = true
    const recorder = this.makeRecorder(), blobs: Blob[] = []
    try {
      await new Promise<void>((resolve, reject) => {
        recorder.ondataavailable = event => { if (event.data.size) blobs.push(event.data) }
        recorder.onerror = () => reject(new Error('The sample could not be encoded. Try a different Chrome device.'))
        recorder.onstop = () => resolve()
        recorder.start(); setTimeout(() => { if (recorder.state !== 'inactive') recorder.stop() }, 5000)
      })
      if (this.disposed) return
      if (this.state.sampleUrl) URL.revokeObjectURL(this.state.sampleUrl)
      this.emit({ sampleUrl: URL.createObjectURL(new Blob(blobs, { type: recorder.mimeType })) })
    } finally { this.sampling = false }
  }
  async start(classId: string, title: string) {
    this.assertOwner()
    if (this.state.stage !== 'ready' || this.sampling) throw new Error('Finish the audio sample before recording.')
    const localId = crypto.randomUUID()
    await this.lock(localId)
    try {
      const estimate = await navigator.storage.estimate()
      if (estimate.quota && estimate.quota - (estimate.usage ?? 0) < 64 * 1024 * 1024) throw new Error('Not enough browser storage. Free space before recording.')
      await navigator.storage.persist?.().catch(() => false)
      this.recorder = this.makeRecorder()
      const mime = this.recorder.mimeType.replace(/["\s]/g, '')
      this.session = await recordingService.start(classId, mime)
      this.assertOwner()
      this.metadata = { localId, classId, title, ownerId: this.ownerId, mimeType: mime, recordingId: this.session.id,
        startedAt: Date.now(), chunkCount: 0, bytes: 0, uploadedBytes: 0, parts: 0, cursorChunk: 0, cursorOffset: 0,
        phase: 'recording', partSize: this.session.partSizeBytes, maxBytes: this.session.maxBytes }
      await saveLocal(this.metadata)
      this.recorder.ondataavailable = event => {
        if (!event.data.size) return
        const index = this.nextChunk++
        this.emergency.set(index, event.data)
        void this.write(async () => {
          const meta = { ...this.metadata!, chunkCount: index + 1, bytes: this.metadata!.bytes + event.data.size }
          this.metadata = meta
          try { await saveLocal(meta, event.data); this.emergency.delete(index) }
          catch { this.storageFailed = true; this.emit({ error: 'Browser storage failed. Keep this page open and download the recording.' }); void this.stop(); return }
          this.emit({ bytes: meta.bytes })
          if (meta.bytes > meta.maxBytes - 32 * 1024 * 1024) void this.stop()
          void this.pump()
        })
      }
      this.recorder.onerror = () => { this.emit({ error: 'The browser encoder stopped unexpectedly. Keep this page open and save the available recording.' }); void this.stop() }
      this.recorder.start(2000)
      this.emit({ stage: 'recording' })
      this.interval = setInterval(() => {
        const seconds = Math.floor((Date.now() - this.metadata!.startedAt) / 1000)
        this.emit({ seconds })
        if (!this.stillOwner() || seconds >= 28790) void this.stop()
      }, 1000)
    } catch (error) { this.releaseLock?.(); this.emit({ error: message(error) }); throw error }
  }
  private write(action: () => Promise<void>) {
    const next = this.writes.then(action)
    this.writes = next.catch(error => { this.emit({ error: message(error) }); this.storageFailed = true })
    return next
  }
  async stop() {
    if (this.stopPromise) return this.stopPromise
    this.stopPromise = this.finishStop()
    return this.stopPromise
  }
  private async finishStop() {
    if (this.interval) clearInterval(this.interval)
    if (this.recorder && this.recorder.state !== 'inactive') {
      await new Promise<void>(resolve => { this.recorder!.addEventListener('stop', () => resolve(), { once: true }); this.recorder!.stop() })
    }
    this.releaseMedia()
    if (!this.metadata) { this.emit({ stage: 'idle' }); return }
    await this.writes
    await this.write(async () => { this.metadata = { ...this.metadata!, phase: 'stopped' }; if (!this.storageFailed) await saveLocal(this.metadata) })
    this.finishRequested = true
    this.emit({ stage: 'stopped' })
    await this.pumping
    await this.pump()
  }
  async recover(meta: LocalRecording) {
    this.assertOwner()
    if (meta.ownerId !== this.ownerId) throw new Error('Sign in as the original recording owner.')
    await this.lock(meta.localId)
    this.metadata = { ...meta, phase: meta.phase === 'queued' ? 'queued' : 'stopped' }
    this.finishRequested = this.metadata.phase !== 'queued'
    this.emit({ stage: meta.phase === 'queued' ? 'queued' : 'stopped', bytes: meta.bytes, uploaded: meta.uploadedBytes })
  }
  async retry() {
    this.assertOwner()
    if (!this.metadata || this.metadata.phase === 'queued') return
    // UPLOADING sessions can renew the capability after a page reload. If finalization already
    // reached the server, complete with the existing capability is retried by the active engine.
    try { this.session = await recordingService.resume(this.metadata.recordingId) }
    catch (error) {
      const server = await recordingService.list(this.metadata.classId)
      const saved = server.recordings.find(entry => entry.id === this.metadata!.recordingId)
      if (saved && saved.status !== 'UPLOADING' && this.metadata.uploadedBytes === this.metadata.bytes && this.metadata.bytes > 0) {
        this.metadata = { ...this.metadata, phase: 'queued' }; await saveLocal(this.metadata)
        this.emit({ stage: 'queued', uploadError: '' }); this.releaseLock?.(); return
      }
      throw error
    }
    this.emit({ uploadError: '' })
    await this.pump()
  }
  private async pump() {
    if (this.pumping || !this.metadata || this.storageFailed || this.metadata.phase === 'queued') return this.pumping
    if (this.state.uploadError) return
    this.pumping = this.upload().catch(error => { this.emit({ uploadError: `${message(error)} Your local recording is kept.`, stage: this.recorder?.state === 'recording' ? 'recording' : 'stopped' }) })
      .finally(() => { this.pumping = undefined })
    return this.pumping
  }
  private async upload() {
    this.assertOwner()
    if (!this.session) this.session = await recordingService.resume(this.metadata!.recordingId)
    while (this.metadata && this.metadata.bytes - this.metadata.uploadedBytes >= this.metadata.partSize
      || this.metadata && this.finishRequested && this.metadata.bytes > this.metadata.uploadedBytes) {
      this.assertOwner()
      if (this.storageFailed) throw new Error("Browser storage failed. Download this recording before closing the page.")
      const snapshot = { ...this.metadata! }
      const size = Math.min(snapshot.partSize, snapshot.bytes - snapshot.uploadedBytes)
      const part = await readPart(snapshot, size)
      const signed = await uploadControl<{ url: string }>(snapshot.recordingId, this.session!.uploadToken, `parts/${snapshot.parts + 1}?bytes=${size}`)
      if (this.recorder?.state !== 'recording') this.emit({ stage: 'uploading' })
      const result = await fetch(signed.url, { method: 'PUT', credentials: 'omit', body: part.blob,
        referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(120000) })
      if (!result.ok) throw new Error(`S3 upload failed (${result.status}). Retry when your connection is stable.`)
      await this.write(async () => {
        this.metadata = { ...this.metadata!, parts: snapshot.parts + 1, uploadedBytes: snapshot.uploadedBytes + size,
          cursorChunk: part.index, cursorOffset: part.offset }
        await saveLocal(this.metadata)
        this.emit({ uploaded: this.metadata.uploadedBytes })
      })
    }
    if (!this.storageFailed && this.finishRequested && this.metadata?.bytes && this.metadata.bytes === this.metadata.uploadedBytes) {
      this.assertOwner()
      await uploadControl(this.metadata.recordingId, this.session!.uploadToken, `complete?parts=${this.metadata.parts}&bytes=${this.metadata.bytes}`)
      await this.write(async () => { this.metadata = { ...this.metadata!, phase: 'queued' }; await saveLocal(this.metadata) })
      this.emit({ stage: 'queued', uploadError: '' }); this.releaseLock?.()
    }
  }
  async download() { if (this.metadata) await downloadLocal(this.metadata, this.emergency) }
  hasUnsavedWork() { return Boolean(this.metadata && this.metadata.phase !== 'queued') || this.state.stage === 'recording' }
  private releaseMedia() {
    this.display?.getTracks().forEach(track => track.stop()); this.microphone?.getTracks().forEach(track => track.stop())
    this.mixed?.getTracks().forEach(track => track.stop()); void this.context?.close().catch(() => {})
    this.preview(null)
  }
  dispose() {
    this.disposed = true
    void this.stop().finally(() => this.releaseLock?.()).catch(() => {})
    if (this.state.sampleUrl) URL.revokeObjectURL(this.state.sampleUrl)
  }
}
export function message(error: unknown) { return error instanceof Error ? error.message : typeof error === 'object' && error && 'message' in error ? String(error.message) : 'Something went wrong. Try again.' }
