import { BrowserClassRecorder } from '@/components/instructor/BrowserClassRecorder'

export default async function RecordClassPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params
  return <BrowserClassRecorder classId={id} />
}
