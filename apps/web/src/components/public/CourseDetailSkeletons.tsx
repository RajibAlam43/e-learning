'use client'

export function HeroSkeleton() {
  return (
    <section
      className="pt-[48px] pb-16"
      style={{ backgroundImage: "url('/images/course-detail-page-hero.png')" }}
    >
      <div className="container mx-auto px-5 flex flex-col items-start gap-20">
        <section className="flex w-full flex-col items-start gap-12 container mx-auto">
          <div className="flex items-center gap-2">
            <div className="skeleton-shimmer h-6 w-6 rounded" />
            <div className="skeleton-shimmer h-4 w-20 rounded" />
          </div>

          <div className="flex w-full flex-col items-start gap-4">
            <div className="skeleton-shimmer h-6 w-24 rounded" />
            <div className="skeleton-shimmer h-[52px] w-[400px] rounded max-[750px]:w-[300px]" />
            <div className="skeleton-shimmer h-6 w-[489px] rounded max-[675px]:w-full" />
          </div>
        </section>

        <section className="flex w-full h-[220px] items-start gap-0 max-[675px]:flex-col">
          <div className="skeleton-shimmer h-[220px] w-[220px] rounded-l-xl max-[675px]:w-full max-[675px]:rounded-b-none" />
          <div className="flex flex-1 flex-col gap-5 bg-white p-8 min-w-[419px] max-[750px]:min-w-full max-[450px]:p-5 h-full">
            <div className="skeleton-shimmer h-8 w-[180px] rounded" />
            <div className="flex flex-col gap-3">
              <div className="skeleton-shimmer h-5 w-[240px] rounded" />
              <div className="skeleton-shimmer h-5 w-[200px] rounded" />
              <div className="skeleton-shimmer h-5 w-[220px] rounded" />
              <div className="skeleton-shimmer h-5 w-[180px] rounded" />
            </div>
          </div>
          <div className="flex w-[319px] flex-col items-center justify-center gap-6 bg-[#ffefcc] p-8 max-[675px]:w-full max-[675px]:p-6 max-[450px]:p-5">
            <div className="skeleton-shimmer h-5 w-[130px] rounded" />
            <div className="skeleton-shimmer h-10 w-[160px] rounded" />
            <div className="skeleton-shimmer h-11 w-full rounded-md" />
          </div>
        </section>
      </div>
    </section>
  )
}

export function OverviewSkeleton() {
  return (
    <section className="w-full pt-10">
      <div className="container mx-auto flex items-start gap-20 px-5 pb-10 max-[750px]:flex-col max-[750px]:gap-6">
        <div className="flex flex-1 flex-col items-start gap-6">
          <div className="skeleton-shimmer h-[44px] w-[130px] rounded" />
          <div className="flex flex-col gap-3 w-full">
            <div className="skeleton-shimmer h-4 w-full rounded" />
            <div className="skeleton-shimmer h-4 w-full rounded" />
            <div className="skeleton-shimmer h-4 w-[80%] rounded" />
          </div>
        </div>
        <div className="flex flex-1 flex-col items-start gap-6">
          <div className="skeleton-shimmer h-[44px] w-[220px] rounded" />
          <div className="flex flex-col gap-3 w-full pl-5">
            <div className="skeleton-shimmer h-4 w-full rounded" />
            <div className="skeleton-shimmer h-4 w-[90%] rounded" />
            <div className="skeleton-shimmer h-4 w-[85%] rounded" />
            <div className="skeleton-shimmer h-4 w-[70%] rounded" />
          </div>
        </div>
      </div>
    </section>
  )
}

export function OutcomesSkeleton() {
  return (
    <section className="w-full">
      <div className="container mx-auto flex flex-col items-start gap-6 px-5 py-20">
        <div className="skeleton-shimmer h-[44px] w-[220px] rounded" />
        <div className="flex w-full gap-4 max-[450px]:flex-col">
          {[1, 2, 3].map((n) => (
            <div
              key={n}
              className="flex flex-1 flex-col gap-12 rounded-xl bg-white p-8 max-[450px]:gap-8 max-[450px]:p-5"
              style={{ boxShadow: '0px 2px 6px 0px rgba(16,24,40,0.06)' }}
            >
              <div className="skeleton-shimmer h-10 w-10 rounded-lg" />
              <div className="skeleton-shimmer h-5 w-[180px] rounded" />
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

export function ContentsSkeleton() {
  return (
    <section className="flex w-full py-20 bg-[#fdfcf8]">
      <div className="container mx-auto px-5">
        <div className="flex w-full flex-col gap-6">
          <div className="skeleton-shimmer h-[44px] w-[200px] rounded" />
          <div className="flex w-full gap-4 max-[450px]:flex-col">
            {[1, 2, 3].map((n) => (
              <div
                key={n}
                className="flex flex-1 flex-col h-[212px] items-center gap-4 rounded-xl bg-[#735c00] px-6 py-12 max-[450px]:h-auto max-[450px]:py-6"
                style={{ boxShadow: 'inset 0px 8px 24px #d4af37' }}
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/10">
                  <div className="skeleton-shimmer h-[25px] w-[25px] rounded" />
                </div>
                <div className="skeleton-shimmer h-5 w-[160px] rounded" />
                <div className="skeleton-shimmer h-4 w-[120px] rounded" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}

export function CurriculumSkeleton() {
  return (
    <section className="w-full py-20 bg-[#f5f5f5]">
      <div className="container mx-auto flex flex-col items-start px-5">
        <div className="flex w-full flex-col gap-6">
          <div className="skeleton-shimmer h-[44px] w-[240px] rounded" />
          <ol className="flex w-full list-none flex-col items-start gap-2 p-0 m-0">
            {[1, 2, 3, 4].map((n) => (
              <li
                key={n}
                className="flex w-full items-center gap-6 rounded-xl bg-white p-6"
              >
                <div className="skeleton-shimmer h-8 w-[48px] rounded" />
                <div className="flex flex-1 flex-col gap-1">
                  <div className="skeleton-shimmer h-5 w-[200px] rounded" />
                  <div className="skeleton-shimmer h-4 w-[300px] rounded max-[450px]:w-[160px]" />
                </div>
              </li>
            ))}
          </ol>
        </div>
      </div>
    </section>
  )
}

export function InstructorSkeleton() {
  return (
    <section className="w-full pt-20 pb-10">
      <div className="container mx-auto flex flex-col items-start gap-8 px-5">
        <div className="skeleton-shimmer h-[44px] w-[260px] rounded" />
        <article className="flex h-[495px] w-full items-end gap-12 rounded-lg bg-white p-8 max-[750px]:flex-col max-[750px]:h-auto max-[450px]:p-5">
          <div className="flex flex-1 items-center justify-center self-stretch">
            <div className="skeleton-shimmer h-[400px] w-full rounded-lg max-[750px]:h-[300px]" />
          </div>
          <div className="flex flex-1 flex-col items-start gap-6">
            <div className="flex flex-col gap-1">
              <div className="skeleton-shimmer h-8 w-[200px] rounded" />
              <div className="skeleton-shimmer h-4 w-[150px] rounded" />
            </div>
            <div className="flex flex-col gap-2 w-full">
              <div className="skeleton-shimmer h-4 w-full rounded" />
              <div className="skeleton-shimmer h-4 w-full rounded" />
              <div className="skeleton-shimmer h-4 w-[80%] rounded" />
            </div>
          </div>
        </article>
      </div>
    </section>
  )
}

export function RelatedCoursesSkeleton() {
  return (
    <section className="flex w-full flex-col items-center gap-8 bg-white px-5 py-20">
      <div className="flex w-full max-w-[1440px] items-center justify-between max-[450px]:flex-col max-[450px]:gap-4">
        <div className="skeleton-shimmer h-[48px] w-[280px] rounded" />
        <div className="skeleton-shimmer h-11 w-[140px] rounded-md" />
      </div>
      <div className="flex w-full max-w-[1440px] gap-4 overflow-hidden">
        {[1, 2, 3].map((n) => (
          <div
            key={n}
            className="flex w-96 shrink-0 flex-col bg-white"
          >
            <div className="skeleton-shimmer h-[274px] w-full rounded-md" />
            <div className="flex flex-col gap-4 p-6">
              <div className="skeleton-shimmer h-7 w-[260px] rounded" />
              <div className="skeleton-shimmer h-4 w-[180px] rounded" />
              <div className="flex items-center justify-between">
                <div className="skeleton-shimmer h-5 w-[80px] rounded" />
                <div className="skeleton-shimmer h-[46px] w-[130px] rounded-md" />
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
