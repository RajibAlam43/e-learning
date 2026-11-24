interface SectionHeaderProps {
  title: string
  description?: string
  align?: 'left' | 'center'
}

export function SectionHeader({
  title,
  description,
  align = 'center',
}: SectionHeaderProps) {
  const alignClass = align === 'center' ? 'text-center mx-auto' : 'text-left'

  return (
    <div className={`max-w-2xl ${alignClass}`}>
      <h2 className="font-heading text-4xl tracking-tight text-[var(--color-text-headline-primary)] md:text-5xl">
        {title}
      </h2>
      {description && (
        <p className="mt-4 text-base leading-relaxed text-[var(--color-text-body-secondary)]">
          {description}
        </p>
      )}
    </div>
  )
}
