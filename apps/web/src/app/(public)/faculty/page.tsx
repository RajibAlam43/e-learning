import { generatePageMetadata } from '@/config/seo.config'
import { FacultyDirectory } from '@/components/public/faculty/FacultyDirectory'
export const metadata = generatePageMetadata('faculty')
export default function FacultyPage() { return <FacultyDirectory /> }
