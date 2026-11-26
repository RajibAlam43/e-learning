import { StudentWaivers } from '@/components/academic/StudentWaivers'
export default async function WaiversPage({ searchParams }: { searchParams: Promise<{ courseTemplateId?: string }> }) {
 const { courseTemplateId } = await searchParams
 return <StudentWaivers initialTemplateId={courseTemplateId} />
}
