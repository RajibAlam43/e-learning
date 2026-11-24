export const QUERY_KEYS = {
  // Auth
  user: () => ['user'] as const,
  userStats: () => ['user', 'stats'] as const,

  // Courses
  courses: (params?: object) => ['courses', params] as const,
  course: (id: string) => ['courses', id] as const,
  courseReviews: (id: string) => ['courses', id, 'reviews'] as const,
  publicCourses: (params?: object) => ['courses', 'public', params] as const,

  // Categories
  categories: () => ['categories'] as const,
  featuredCourses: (limit: number) => ['courses', 'featured', limit] as const,
  platformStats: () => ['public', 'stats'] as const,
  publicCourseReviews: (slug: string) => ['courses', slug, 'public-reviews'] as const,

  // Enrollments
  enrollments: (params?: object) => ['enrollments', params] as const,
  enrollment: (id: string) => ['enrollments', id] as const,

  // Instructors
  instructors: (params?: object) => ['instructors', params] as const,
  instructor: (id: string) => ['instructors', id] as const,

  // Students (admin)
  students: (params?: object) => ['admin', 'students', params] as const,

  // Lessons
  lessons: (courseId: string) => ['courses', courseId, 'lessons'] as const,

  // Progress
  progress: (courseId: string) => ['progress', courseId] as const,

  // Student Dashboard
  studentDashboard: () => ['student', 'dashboard'] as const,
  myCourses: () => ['student', 'courses'] as const,
  liveClasses: () => ['student', 'live-classes'] as const,
  studentAnnouncements: (page = 0, size = 10) => ['student', 'announcements', page, size] as const,
  learningStreak: () => ['student', 'progress', 'streak'] as const,
  completedLessons: () => ['student', 'progress', 'completed-lessons'] as const,

  // Instructor Dashboard
  instructorDashboard: () => ['instructor', 'dashboard'] as const,

  // Learning (quizzes, progress, resources)
  quiz: (quizId: string) => ['learn', 'quizzes', quizId] as const,
  courseQuiz: (courseId: string, quizId: string) => ['learn', 'courses', courseId, 'quizzes', quizId] as const,
  quizAttempts: (courseId: string, quizId: string) => ['learn', 'courses', courseId, 'quizzes', quizId, 'attempts'] as const,
  quizAttempt: (attemptId: string) => ['learn', 'quiz-attempts', attemptId] as const,
  courseProgress: (courseId: string) => ['learn', 'courses', courseId, 'progress'] as const,
  lessonResources: (lessonId: string) => ['learn', 'lessons', lessonId, 'resources'] as const,

  // Student Course Learning
  courseHome: (courseId: string) => ['student', 'courses', courseId] as const,
  lessonContent: (lessonId: string) => ['student', 'lessons', lessonId] as const,
  lessonPlayback: (lessonId: string) => ['student', 'lessons', lessonId, 'playback'] as const,
  courseLessonContent: (courseId: string, lessonId: string) => ['learn', 'courses', courseId, 'lessons', lessonId] as const,
  courseLessonPlayback: (courseId: string, lessonId: string) => ['learn', 'courses', courseId, 'lessons', lessonId, 'playback'] as const,
  courseLessonResources: (courseId: string, lessonId: string) => ['learn', 'courses', courseId, 'lessons', lessonId, 'resources'] as const,

  // Profile & Settings
  profile: () => ['user', 'profile'] as const,
  certificates: () => ['student', 'certificates'] as const,
  certificateDownload: (certificateId: string) => ['student', 'certificates', certificateId, 'download'] as const,
  certificate: (certificateId: string) => ['student', 'certificates', certificateId] as const,
  studentOrders: () => ['student', 'orders'] as const,
  studentOrder: (id: string) => ['student', 'orders', id] as const,
  studentCollections: () => ['student', 'collections'] as const,
  studentCollection: (id: string) => ['student', 'collections', id] as const,
  certificateVerification: (code: string) => ['certificates', 'verify', code] as const,

  // Admin
  adminCourses: () => ['admin', 'courses'] as const,
  adminCourse: (id: string) => ['admin', 'courses', id] as const,
  adminInstructors: () => ['admin', 'instructors'] as const,
  adminInstructor: (id: string) => ['admin', 'instructors', id] as const,
  adminOrders: () => ['admin', 'orders'] as const,
  adminOrder: (id: string) => ['admin', 'orders', id] as const,
  adminLiveClasses: () => ['admin', 'live-classes'] as const,
  adminCollections: () => ['admin', 'collections'] as const,
  adminCollection: (id: string) => ['admin', 'collections', id] as const,
  adminCategories: () => ['admin', 'categories'] as const,
  adminReviews: (pending?: boolean) => ['admin', 'reviews', pending] as const,
  adminSupportTickets: () => ['admin', 'support', 'tickets'] as const,
  adminSettings: () => ['admin', 'settings'] as const,
} as const
