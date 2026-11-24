import { Inbox } from 'lucide-react'

interface EmptyStateProps {
  title?: string
  description?: string
  icon?: React.ReactNode
  action?: React.ReactNode
  headingLevel?: 'h2' | 'h3'
}

export function EmptyState({
  title = 'No results found',
  description,
  icon,
  action,
  headingLevel = 'h2',
}: EmptyStateProps) {
  const Heading = headingLevel

  return (
    <div className="flex flex-col items-center justify-center gap-4 py-16 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[var(--color-neutral-100)]">
        {icon || <Inbox className="h-8 w-8 text-[var(--color-neutral-400)]" />}
      </div>
      <div className="flex flex-col gap-1">
        <Heading className="font-sans text-lg font-semibold text-[var(--color-text-headline-primary)]">
          {title}
        </Heading>
        {description && (
          <p className="text-sm text-[var(--color-text-body-secondary)]">
            {description}
          </p>
        )}
      </div>
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}
