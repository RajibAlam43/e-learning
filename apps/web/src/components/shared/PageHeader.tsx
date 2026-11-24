interface PageHeaderProps {
  title: string
  description?: string
  children?: React.ReactNode
}

export function PageHeader({ title, description, children }: PageHeaderProps) {
  return (
    <div className="container mx-auto px-5 pt-5 flex flex-col gap-2">
      <h1 className="font-heading text-3xl tracking-tight text-[var(--color-text-headline-primary)] md:text-4xl">
        {title}
      </h1>
      {description && (
        <p className="text-base text-[var(--color-text-body-secondary)]">
          {description}
        </p>
      )}
      {children}
    </div>
  )
}
