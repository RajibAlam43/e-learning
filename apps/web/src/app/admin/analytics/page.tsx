'use client'

import { useMemo } from 'react'
import { format } from 'date-fns'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts'
import StatCard from '@/components/shared/StatCard'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { DataTable, type Column } from '@/components/admin/shared/DataTable'
import { StatusBadge } from '@/components/admin/shared/StatusBadge'
import {
  useAdminCourses,
  useAdminInstructors,
  useAdminOrders,
  useAdminLiveClasses,
} from '@/hooks/useAdmin'
import type { AdminOrderSummary } from '@/types'
import { orderTotals, revenueByMonth as monthlyRevenue } from '@/lib/order-reporting'

const PIE_COLORS: Record<string, string> = {
  PUBLISHED: '#22c55e',
  DRAFT: '#f59e0b',
  ARCHIVED: '#9ca3af',
}

const orderColumns: Column<AdminOrderSummary>[] = [
  {
    key: 'customer',
    header: 'Customer',
    render: (item) => (
      <span className="font-medium text-[#1c3873]">
        {item.customerName ?? '-'}
      </span>
    ),
  },
  {
    key: 'amount',
    header: 'Amount',
    render: (item) => (
      <span className="text-[#5e5f5c]">
        {item.totalAmount?.toLocaleString() ?? 0} BDT
      </span>
    ),
  },
  {
    key: 'status',
    header: 'Status',
    render: (item) => <StatusBadge status={item.status ?? 'PENDING'} />,
  },
  {
    key: 'date',
    header: 'Date',
    hideOnMobile: true,
    render: (item) => (
      <span className="text-[#5e5f5c]">
        {item.createdAt
          ? format(new Date(item.createdAt), 'MMM d, yyyy')
          : '-'}
      </span>
    ),
  },
]

export default function AnalyticsPage() {
  const { data: courses } = useAdminCourses()
  const { data: instructors } = useAdminInstructors()
  const { data: orders } = useAdminOrders()
  const { data: liveClasses } = useAdminLiveClasses()

  const totalRevenue = useMemo(() => orderTotals(orders ?? []).totalRevenue, [orders])
  const revenueByMonth = useMemo(() => monthlyRevenue(orders ?? []), [orders])

  const courseStatusData = useMemo(() => {
    if (!courses) return []
    const map: Record<string, number> = {}
    courses.forEach((c) => {
      const s = c.status ?? 'DRAFT'
      map[s] = (map[s] ?? 0) + 1
    })
    return Object.entries(map).map(([name, value]) => ({ name, value }))
  }, [courses])

  const topCourses = useMemo(() => {
    if (!courses) return []
    return [...courses]
      .sort((a, b) => (b.totalEnrolled ?? 0) - (a.totalEnrolled ?? 0))
      .slice(0, 10)
  }, [courses])

  const recentOrders = useMemo(() => {
    if (!orders) return []
    return [...orders]
      .sort(
        (a, b) =>
          new Date(b.createdAt ?? 0).getTime() -
          new Date(a.createdAt ?? 0).getTime()
      )
      .slice(0, 10)
  }, [orders])

  return (
    <div className="flex flex-col gap-6 w-[1152px] max-w-full mx-auto pb-10">
      <AdminPageHeader
        title="Analytics"
        description="Platform performance overview"
      />
      {/* Stats row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon="/images/book-icon.png"
          iconBg="bg-blue-50"
          iconAlt="Courses"
          value={courses?.length ?? 0}
          label="Total Courses"
        />
        <StatCard
          icon="/images/dashboard-icon.png"
          iconBg="bg-purple-50"
          iconAlt="Teachers"
          value={instructors?.length ?? 0}
          label="Total Teachers"
        />
        <StatCard
          icon="/images/price.png"
          iconBg="bg-green-50"
          iconAlt="Revenue"
          value={`${totalRevenue.toLocaleString()} BDT`}
          label="Total Revenue"
        />
        <StatCard
          icon="/images/live-icon.png"
          iconBg="bg-orange-50"
          iconAlt="Live Classes"
          value={liveClasses?.length ?? 0}
          label="Total Live Classes"
        />
      </div>
      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RevenueChart data={revenueByMonth} />
        <CourseStatusChart data={courseStatusData} />
      </div>
      {/* Tables row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TopCoursesTable courses={topCourses} />
        <div>
          <h2 className="font-heading text-lg font-semibold text-[#1c3873] mb-3">
            Recent Orders
          </h2>
          <DataTable
            columns={orderColumns}
            data={recentOrders}
            keyExtractor={(item) => item.orderId ?? ''}
          />
        </div>
      </div>
    </div>
  )
}

// --- Sub-components below ---

function RevenueChart({ data }: { data: { month: string; revenue: number }[] }) {
  return (
    <div className="bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-5">
      <h2 className="font-heading text-lg font-semibold text-[#1c3873] mb-4">
        Monthly Revenue (BDT)
      </h2>
      <p className="mb-3 text-xs text-slate-500">Paid orders by payment month in Asia/Dhaka. Orders without a payment date are excluded.</p>
      {data.length === 0 ? (
        <p className="text-sm text-[#5e5f5c]">No revenue data available.</p>
      ) : (
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis dataKey="month" fontSize={12} tick={{ fill: '#5e5f5c' }} />
            <YAxis fontSize={12} tick={{ fill: '#5e5f5c' }} />
            <Tooltip />
            <Bar dataKey="revenue" fill="#3b82f6" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
function CourseStatusChart({
  data,
}: {
  data: { name: string; value: number }[]
}) {
  return (
    <div className="bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-5">
      <h2 className="font-heading text-lg font-semibold text-[#1c3873] mb-4">
        Course Status Distribution
      </h2>
      {data.length === 0 ? (
        <p className="text-sm text-[#5e5f5c]">No course data available.</p>
      ) : (
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              nameKey="name"
              cx="50%"
              cy="50%"
              outerRadius={100}
              label={({ name, value }) => `${name} (${value})`}
            >
              {data.map((entry) => (
                <Cell
                  key={entry.name}
                  fill={PIE_COLORS[entry.name] ?? '#9ca3af'}
                />
              ))}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}

interface TopCourse {
  courseId?: string
  title?: string
  totalEnrolled?: number
  status?: string
}

function TopCoursesTable({ courses }: { courses: TopCourse[] }) {
  return (
    <div className="bg-white rounded-2xl shadow-[0px_2px_6px_rgba(16,24,40,0.06)] p-5">
      <h2 className="font-heading text-lg font-semibold text-[#1c3873] mb-4">
        Top Courses by Enrollment
      </h2>
      {courses.length === 0 ? (
        <p className="text-sm text-[#5e5f5c]">No courses yet.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#f0f0f0] text-left">
                <th className="px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#71717a]">
                  #
                </th>
                <th className="px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#71717a]">
                  Title
                </th>
                <th className="px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#71717a]">
                  Enrolled
                </th>
                <th className="px-3 py-2.5 text-xs font-semibold uppercase tracking-wider text-[#71717a]">
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              {courses.map((course, idx) => (
                <tr
                  key={course.courseId ?? idx}
                  className="border-b border-[#f0f0f0] last:border-b-0"
                >
                  <td className="px-3 py-2.5 text-[#5e5f5c]">{idx + 1}</td>
                  <td className="px-3 py-2.5 font-medium text-[#1c3873]">
                    {course.title ?? '-'}
                  </td>
                  <td className="px-3 py-2.5 text-[#5e5f5c]">
                    {course.totalEnrolled ?? 0}
                  </td>
                  <td className="px-3 py-2.5">
                    <StatusBadge status={course.status ?? 'DRAFT'} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
