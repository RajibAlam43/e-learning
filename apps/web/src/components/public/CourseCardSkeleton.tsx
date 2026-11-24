export function CourseCardSkeleton() {
  return (
    <article className="flex flex-1 flex-col items-start bg-white overflow-hidden rounded-lg animate-pulse">
      {/* Thumbnail */}
      <div className="h-[200px] w-full bg-[#e8ebf1] sm:h-[220px] md:h-[274px]" />

      {/* Body */}
      <div className="flex w-full flex-col items-start gap-3 p-4 sm:p-5 md:gap-4 md:p-6">
        {/* Rating */}
        <div className="h-5 w-20 rounded bg-[#e8ebf1]" />

        {/* Title */}
        <div className="flex w-full flex-col gap-2">
          <div className="h-6 w-3/4 rounded bg-[#e8ebf1]" />
        </div>

        {/* Metadata */}
        <div className="flex w-full gap-4">
          <div className="h-5 w-16 rounded bg-[#e8ebf1]" />
          <div className="h-5 w-20 rounded bg-[#e8ebf1]" />
          <div className="h-5 w-16 rounded bg-[#e8ebf1]" />
        </div>

        {/* Price + CTA */}
        <div className="flex w-full items-center justify-between gap-3">
          <div className="h-6 w-20 rounded bg-[#e8ebf1]" />
          <div className="h-10 w-28 rounded-[6px] bg-[#e8ebf1] sm:h-[46px] sm:w-32" />
        </div>
      </div>
    </article>
  )
}
