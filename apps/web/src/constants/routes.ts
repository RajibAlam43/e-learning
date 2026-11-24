export const ROUTES = {
  // Public
  home: '/',
  courses: '/courses',
  programs: '/programs',
  courseDetail: (id: string) => `/courses/${id}`,
  instructors: '/instructors',
  instructorDetail: (id: string) => `/instructors/${id}`,
  blog: '/blog',
  contact: '/contact',

  // Auth
  login: '/login',
  signup: '/signup',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  verifyAccount: '/verify-account',

  // Student
  student: {
    dashboard: '/student/dashboard',
    myCourses: '/student/courses',
    learn: (courseId: string) => `/student/courses/${encodeURIComponent(courseId)}`,
    certificates: '/student/certificates',
    settings: '/student/settings',
  },

  // Instructor
  instructor: {
    dashboard: '/instructor/dashboard',
    courses: '/instructor/courses',
    liveClasses: '/instructor/live-classes',
    announcements: '/instructor/announcements',
    settings: '/instructor/settings',
  },

  // Admin
  admin: {
    dashboard: '/admin/dashboard',
    students: '/admin/students',
    teachers: '/admin/teachers',
    courses: '/admin/courses',
    courseDetail: (id: string) => `/admin/courses/${id}`,
    courseCreate: '/admin/courses/create',
    programs: '/admin/programs',
    liveClasses: '/admin/live-classes',
    payments: '/admin/payments',
    analytics: '/admin/analytics',
    announcements: '/admin/announcements',
    support: '/admin/support',
    settings: '/admin/settings',
  },
} as const
