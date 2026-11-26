'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { useLocale } from 'next-intl'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { recordingService } from '@/services/recording.service'
import { BrowserClassRecorder as Recorder, message } from '@/lib/recording/browser-recorder'
import type { RecorderState } from '@/lib/recording/browser-recorder'
import { downloadLocal, listLocal, removeLocal } from '@/lib/recording/local-recordings'
import type { LocalRecording } from '@/lib/recording/local-recordings'

const action = 'min-h-11 rounded-md px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-50'
const initial: RecorderState = { stage: 'idle', error: '', uploadError: '', bytes: 0, uploaded: 0, seconds: 0, sampleUrl: '' }
const amount = (bytes: number) => `${(bytes / 1048576).toFixed(1)} MB`
const duration = (seconds: number) => `${Math.floor(seconds / 3600).toString().padStart(2, '0')}:${Math.floor(seconds % 3600 / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}`

export function BrowserClassRecorder({ classId }: { classId: string }) {
  const bn = useLocale() === 'bn'
  const ownerId = useAuthStore(state => state.user?.id)
  const details = useQuery({ queryKey: ['recording-class', classId, ownerId], queryFn: () => recordingService.list(classId), refetchInterval: 10000 })
  const video = useRef<HTMLVideoElement>(null)
  const engine = useRef<Recorder | null>(null)
  const recoveryRecorders = useRef(new Map<string, Recorder>())
  const [recoveries, setRecoveries] = useState<LocalRecording[]>([])
  const [state, setState] = useState(initial)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [sampling, setSampling] = useState(false)
  const [locals, setLocals] = useState<LocalRecording[]>([])
  const [levels, setLevels] = useState({ tab: 0, mic: 0 })
  const refreshLocal = useCallback(async () => {
    if (ownerId) setLocals(await listLocal(ownerId, classId))
  }, [ownerId, classId])
  useEffect(() => {
    if (!ownerId) return
    const recorder = new Recorder(ownerId, () => useAuthStore.getState().user?.id === ownerId,
      setState, stream => { if (video.current) video.current.srcObject = stream })
    engine.current = recorder
    void listLocal(ownerId, classId).then(setLocals).catch(failure => setError(message(failure)))
    const unload = (event: BeforeUnloadEvent) => {
      if (engine.current?.hasUnsavedWork() || [...recoveryRecorders.current.values()].some(recorder => recorder.recoveryCopy())) {
        event.preventDefault(); event.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', unload)
    return () => { engine.current?.dispose(); engine.current = null; window.removeEventListener('beforeunload', unload) }
  }, [ownerId, classId])
  useEffect(() => {
    if (state.stage !== 'ready' && state.stage !== 'recording') return
    const timer = setInterval(() => {
      const level = (node?: AnalyserNode) => {
        if (!node) return 0
        const data = new Uint8Array(node.fftSize); node.getByteTimeDomainData(data)
        return Math.min(100, Math.sqrt(data.reduce((sum, value) => sum + ((value - 128) / 128) ** 2, 0) / data.length) * 300)
      }
      setLevels({ tab: level(engine.current?.tabMeter), mic: level(engine.current?.micMeter) })
    }, 150)
    return () => clearInterval(timer)
  }, [state.stage])
  const run = async (operation: () => Promise<unknown>) => {
    setError(''); setBusy(true)
    try { await operation(); await refreshLocal(); void details.refetch() }
    catch (failure) { setError(message(failure)) }
    finally { setBusy(false) }
  }
  const resetRecorder = () => {
    const recovery = engine.current?.recoveryCopy()
    if (recovery) {
      recoveryRecorders.current.set(recovery.localId, engine.current!)
      setRecoveries(previous => [...previous.filter(local => local.localId !== recovery.localId), recovery])
    }
    engine.current?.dispose()
    engine.current = new Recorder(ownerId!, () => useAuthStore.getState().user?.id === ownerId,
      setState, stream => { if (video.current) video.current.srcObject = stream })
    setState(initial)
  }
  const memoryCopies = [...recoveries, ...(state.recovery ? [state.recovery] : [])]
  const recoveryIds = new Set(memoryCopies.map(local => local.localId))
  const localCopies = [...new Map([...locals, ...memoryCopies].filter(local => local.ownerId === ownerId && local.classId === classId).map(local => [local.localId, local])).values()]
  const downloadCopy = async (local: LocalRecording) => {
    const recorder = recoveryRecorders.current.get(local.localId)
      ?? (engine.current?.recoveryCopy()?.localId === local.localId ? engine.current : undefined)
    if (recorder) await recorder.download()
    else await downloadLocal(local)
  }
  const failure = error || state.error || state.uploadError || (details.error ? message(details.error) : '')
  const stageLabel = ({ idle: bn ? 'শুরু করার জন্য প্রস্তুত' : 'Not recording', preparing: bn ? 'ট্যাব ও মাইক্রোফোন নির্বাচন করুন' : 'Selecting capture sources', ready: bn ? 'অডিও পরীক্ষা করুন' : 'Check your audio', recording: bn ? 'রেকর্ড হচ্ছে' : 'Recording', stopped: bn ? 'রেকর্ডিং বন্ধ হয়েছে' : 'Recording stopped', uploading: bn ? 'আপলোড হচ্ছে' : 'Uploading', queued: bn ? 'আপলোড সম্পূর্ণ — যাচাই হচ্ছে' : 'Uploaded — validating' })[state.stage]
  return <div className='mx-auto flex max-w-5xl flex-col gap-6'>
    <Link href='/instructor/live-classes' className='w-fit text-sm text-[#1c3873] underline underline-offset-4'>{bn ? 'লাইভ ক্লাসে ফিরে যান' : 'Back to live classes'}</Link>
    <header><h1 className='text-2xl font-heading'>{bn ? 'ক্লাস রেকর্ড করুন' : 'Record class'}</h1><p className='mt-1 text-slate-600'>{details.data?.title ?? (bn ? 'ক্লাস লোড হচ্ছে…' : 'Loading class…')}</p></header>
    <section className='rounded-xl border border-slate-200 bg-white p-5 sm:p-6' aria-label={bn ? 'ব্রাউজার রেকর্ডার' : 'Browser recorder'}>
      <div className='mb-5 flex flex-wrap items-center justify-between gap-3'>
        <p aria-live='polite' className={`font-medium ${state.stage === 'recording' ? 'text-red-700' : 'text-[#1c3873]'}`}>{stageLabel}</p>
        <output className='font-mono text-xl tabular-nums text-[#1c3873]' aria-label={bn ? 'রেকর্ডিং সময়' : 'Recording time'}>{duration(state.seconds)}</output>
      </div>
      <video ref={video} autoPlay playsInline muted className='aspect-video max-h-[40vh] w-full rounded-md bg-slate-950 object-contain' aria-label={bn ? 'নির্বাচিত ট্যাবের প্রিভিউ' : 'Selected tab preview'} />
      {(state.stage === 'ready' || state.stage === 'recording') && <div className='mt-4 grid gap-3 sm:grid-cols-2'>
        <label className='flex items-center gap-3 text-sm'>{bn ? 'Meet ট্যাবের অডিও' : 'Meet tab audio'}<meter min={0} max={100} value={levels.tab} className='h-5 flex-1 accent-blue-700' /></label>
        <label className='flex items-center gap-3 text-sm'>{bn ? 'আপনার মাইক্রোফোন' : 'Your microphone'}<meter min={0} max={100} value={levels.mic} className='h-5 flex-1 accent-blue-700' /></label>
      </div>}
      <p className='mt-4 max-w-prose text-sm leading-relaxed text-slate-600'>{bn ? 'ডেস্কটপ Chrome-এ Meet ট্যাবটি খুলুন। ট্যাব বাছাইয়ের সময় “Share tab audio” চালু রাখুন এবং মাইক্রোফোনের অনুমতি দিন। রেকর্ডিং শেষ ও আপলোড হওয়া পর্যন্ত এই পৃষ্ঠা খোলা রাখুন।' : 'Open Meet in desktop Chrome. Select its tab with “Share tab audio” enabled, then allow your microphone. Keep this page open until recording and uploading finish.'}</p>
      {details.data && !details.data.enabled && <p role='status' className='mt-3 text-sm text-amber-800'>{bn ? 'রেকর্ডিং এখনো চালু হয়নি। প্রশাসককে স্টোরেজ সেটআপ করতে বলুন।' : 'Recording is not enabled yet. Ask an administrator to configure recording storage.'}</p>}
      <div className='mt-5 flex flex-wrap gap-3'>
        {details.data?.meetingUrl && <a href={details.data.meetingUrl} target='_blank' rel='noopener noreferrer' className={`${action} border border-slate-300 hover:bg-slate-50`}>{bn ? 'Meet খুলুন' : 'Open Meet'}</a>}
        {details.data?.classStatus === 'SCHEDULED' && <button type='button' disabled={busy} onClick={() => void run(() => recordingService.startClass(classId))} className={`${action} border border-slate-300 hover:bg-slate-50`}>{bn ? 'ক্লাস লাইভ করুন' : 'Mark class live'}</button>}
        {['stopped', 'queued'].includes(state.stage) && <button type='button' disabled={busy} onClick={resetRecorder} className={`${action} border border-slate-300 hover:bg-slate-50`}>{bn ? 'আরেকটি রেকর্ডিং করুন' : 'Record another'}</button>}
        {state.stage === 'idle' && <button type='button' disabled={busy || !details.data?.enabled} onClick={() => void run(() => engine.current!.prepare())} className={`${action} bg-[#1c3873] text-white hover:bg-[#0f1f3f]`}>{busy ? (bn ? 'নির্বাচন হচ্ছে…' : 'Selecting…') : (bn ? 'Meet ট্যাব নির্বাচন করুন' : 'Select Meet tab')}</button>}
        {state.stage === 'ready' && <>
          <button type='button' disabled={busy || sampling} onClick={() => { setSampling(true); void run(() => engine.current!.sample()).finally(() => setSampling(false)) }} className={`${action} border border-slate-300 hover:bg-slate-50`}>{sampling ? (bn ? '৫ সেকেন্ডের নমুনা রেকর্ড হচ্ছে…' : 'Recording 5-second sample…') : (bn ? 'অডিও নমুনা পরীক্ষা করুন' : 'Test audio sample')}</button>
          <button type='button' disabled={busy || sampling} onClick={() => void run(() => engine.current!.start(classId, details.data?.title ?? 'Class'))} className={`${action} bg-red-700 text-white hover:bg-red-800`}>{bn ? 'রেকর্ডিং শুরু করুন' : 'Start recording'}</button>
          <button type='button' disabled={busy || sampling} onClick={() => void run(() => engine.current!.stop())} className={`${action} border border-slate-300 hover:bg-slate-50`}>{bn ? 'নির্বাচন বাতিল করুন' : 'Cancel selection'}</button>
        </>}
        {state.stage === 'recording' && <button type='button' disabled={busy} onClick={() => void run(() => engine.current!.stop())} className={`${action} bg-red-700 text-white hover:bg-red-800`}>{bn ? 'বন্ধ করে আপলোড করুন' : 'Stop and finish upload'}</button>}
        {(state.stage === 'stopped' || Boolean(state.uploadError)) && <button type='button' disabled={busy} onClick={() => void run(() => engine.current!.retry())} className={`${action} bg-[#1c3873] text-white hover:bg-[#0f1f3f]`}>{bn ? 'আপলোড আবার চেষ্টা করুন' : 'Retry upload'}</button>}
        {state.bytes > 0 && state.stage !== 'recording' && <button type='button' disabled={busy} onClick={() => void run(() => engine.current!.download())} className={`${action} border border-slate-300 hover:bg-slate-50`}>{bn ? 'স্থানীয় কপি ডাউনলোড করুন' : 'Download local copy'}</button>}
      </div>
      {state.bytes > 0 && <div className='mt-4 space-y-1'><label htmlFor='recording-upload' className='text-sm text-slate-600'>{bn ? 'আপলোড হয়েছে' : 'Uploaded'} {amount(state.uploaded)} / {amount(state.bytes)}</label><progress id='recording-upload' max={Math.max(1, state.bytes)} value={state.uploaded} className='h-2 w-full accent-blue-700' /></div>}
      {failure && <p role='alert' className='mt-4 max-w-prose text-sm leading-relaxed text-red-700'>{failure}</p>}
      {state.sampleUrl && <div className='mt-6 border-t border-slate-200 pt-4'><h2 className='mb-2 text-base font-medium'>{bn ? 'অডিও নমুনা: নিজের ও অন্যদের কণ্ঠ শুনুন' : 'Audio sample: check your voice and the class audio'}</h2><video src={state.sampleUrl} controls playsInline className='max-h-64 w-full rounded-md bg-slate-950' /></div>}
    </section>
    <section className='space-y-3'><h2 className='text-xl'>{bn ? 'এই ডিভাইসে সংরক্ষিত রেকর্ডিং' : 'Recordings saved on this device'}</h2>
      <p className='max-w-prose text-sm text-slate-600'>{bn ? 'সংযোগ বিচ্ছিন্ন হলে এখানে ফিরে আপলোড চালিয়ে যেতে পারবেন। বাধাগ্রস্ত রেকর্ডিংয়ের শেষ কয়েক সেকেন্ড অনুপস্থিত থাকতে পারে।' : 'Return here to resume an interrupted upload. An interrupted recording may be missing its last few seconds.'}</p>
      {localCopies.length === 0 ? <p className='text-sm text-slate-600'>{bn ? 'এখনো কোনো স্থানীয় রেকর্ডিং নেই।' : 'No local recordings yet.'}</p> : <ul className='divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white'>
        {localCopies.map(local => <li key={local.localId} data-recording-id={local.recordingId} className='flex flex-wrap items-center justify-between gap-3 p-4'><div className='space-y-1'><p className='text-sm font-medium text-[#1c3873]'>{new Date(local.startedAt).toLocaleString()} · {amount(local.bytes)}</p><p className='text-sm text-slate-600'>{recoveryIds.has(local.localId) ? (bn ? 'এই কপির কিছু অংশ শুধু এই পৃষ্ঠায় আছে। পৃষ্ঠা বন্ধ করার আগে ডাউনলোড করুন।' : 'Part of this copy is held only in this page. Download before closing it.') : local.phase === 'queued' ? (bn ? 'আপলোড হয়েছে; নিচে যাচাইয়ের অবস্থা দেখুন।' : 'Uploaded; check validation status below.') : (bn ? 'স্থানীয় কপি রাখা আছে।' : 'Local copy retained.')}</p></div>
          <div className='flex flex-wrap gap-2'>
            {local.phase !== 'queued' && !recoveryIds.has(local.localId) && ['idle', 'queued'].includes(state.stage) && <button type='button' disabled={busy || !local.bytes} className={`${action} border border-slate-300 hover:bg-slate-50`} onClick={() => void run(async () => { resetRecorder(); await engine.current!.recover(local); await engine.current!.retry() })}>{bn ? 'আপলোড চালিয়ে যান' : 'Resume upload'}</button>}
            <button type='button' className={`${action} border border-slate-300 hover:bg-slate-50`} disabled={busy || local.phase === 'recording' && state.stage === 'recording'} onClick={() => void run(() => downloadCopy(local))}>{bn ? 'ডাউনলোড' : 'Download'}</button>
            {local.phase === 'queued' && <button type='button' className={`${action} text-red-700 hover:bg-red-50`} disabled={busy} onClick={() => { if (window.confirm(bn ? 'এই ডিভাইসের কপি মুছবেন? সার্ভারের রেকর্ডিং থাকবে।' : 'Delete this device’s copy? The server recording will remain.')) void run(() => removeLocal(local)) }}>{bn ? 'স্থানীয় কপি মুছুন' : 'Remove local copy'}</button>}
          </div></li>)}
      </ul>}
    </section>
    <section className='space-y-3'><h2 className='text-xl'>{bn ? 'সার্ভারে রেকর্ডিং' : 'Server recordings'}</h2>
      {details.data?.recordings.length ? <ul className='divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white'>{details.data.recordings.map(recording => <li key={recording.id} className='p-4'><p className='text-sm font-medium'>{new Date(recording.createdAt).toLocaleString()} · {recording.status === 'READY' ? (bn ? 'যাচাই সম্পূর্ণ — প্রকাশিত' : 'Validated — published') : recording.status}</p>{recording.failureMessage && <p className='mt-1 text-sm text-red-700'>{recording.failureMessage}</p>}</li>)}</ul> : <p className='text-sm text-slate-600'>{bn ? 'কোনো রেকর্ডিং জমা হয়নি।' : 'No recordings submitted yet.'}</p>}
    </section>
  </div>
}
