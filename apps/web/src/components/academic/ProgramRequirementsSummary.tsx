import type { ProgramSnapshot } from '@/services/program-curriculum.service'
export function ProgramRequirementsSummary({ requirements }: { requirements?: ProgramSnapshot | null }) {
 if (!requirements?.requiredCredits) return null
 return <div className="my-5 text-sm text-slate-700"><p><strong>{requirements.requiredCredits} credits</strong> required · up to {requirements.electiveCreditLimit} elective credits count</p><p className="mt-1">Expected duration: {requirements.expectedDurationMonths} months. Program access: {requirements.accessDurationMonths} months from payment confirmation. Register for individual courses separately.</p></div>
}
