// Video URLs from Google's public test video CDN
const VIDEO_URLS = [
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
  'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
];

// Cycle through video URLs
function getVideoUrl(index) {
  return VIDEO_URLS[index % VIDEO_URLS.length];
}

export const USERS = [
  { username: 'admin_user', email: 'admin@edu-test.com', password: 'Admin123456', firstName: 'Admin', lastName: 'User', role: 'ADMIN' },
  { username: 'instructor_wang', email: 'wang@edu-test.com', password: 'Teach123456', firstName: 'Wei', lastName: 'Wang', role: 'INSTRUCTOR' },
  { username: 'instructor_li', email: 'li@edu-test.com', password: 'Teach123456', firstName: 'Ming', lastName: 'Li', role: 'INSTRUCTOR' },
  { username: 'student_zhang', email: 'zhang@edu-test.com', password: 'Study123456', firstName: 'Xiao', lastName: 'Zhang', role: 'STUDENT' },
  { username: 'student_chen', email: 'chen@edu-test.com', password: 'Study123456', firstName: 'Lei', lastName: 'Chen', role: 'STUDENT' },
];

export const CATEGORIES = [
  { name: 'Programming', sortOrder: 1 },
  { name: 'Web Development', sortOrder: 2 },
  { name: 'Design', sortOrder: 3 },
  { name: 'Business', sortOrder: 4 },
  { name: 'Data Science', sortOrder: 5 },
  { name: 'Languages', sortOrder: 6 },
];

export const COURSES = [
  {
    title: 'Java Programming Fundamentals',
    description: 'Master the basics of Java programming language. This comprehensive course covers variables, control flow, object-oriented programming, and more. Perfect for beginners who want to start their programming journey.',
    price: 29.99,
    instructor: 'instructor_wang',
    categoryName: 'Programming',
    coverImage: 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400',
    chapters: [
      {
        title: 'Getting Started with Java',
        sortOrder: 1,
        lessons: [
          { title: 'Introduction to Java', duration: 596, isFree: true, sortOrder: 1 },
          { title: 'Setting Up Your Environment', duration: 480, isFree: true, sortOrder: 2 },
          { title: 'Writing Your First Program', duration: 634, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Core Java Concepts',
        sortOrder: 2,
        lessons: [
          { title: 'Variables and Data Types', duration: 720, isFree: false, sortOrder: 1 },
          { title: 'Control Flow Statements', duration: 596, isFree: false, sortOrder: 2 },
          { title: 'Arrays and Collections', duration: 680, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Object-Oriented Programming',
        sortOrder: 3,
        lessons: [
          { title: 'Classes and Objects', duration: 634, isFree: false, sortOrder: 1 },
          { title: 'Inheritance and Polymorphism', duration: 480, isFree: false, sortOrder: 2 },
        ],
      },
    ],
  },
  {
    title: 'React Frontend Development',
    description: 'Learn to build modern web applications with React. From components and hooks to state management and routing, this course covers everything you need to become a proficient React developer.',
    price: 39.99,
    instructor: 'instructor_wang',
    categoryName: 'Web Development',
    coverImage: 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?w=400',
    chapters: [
      {
        title: 'React Basics',
        sortOrder: 1,
        lessons: [
          { title: 'What is React?', duration: 420, isFree: true, sortOrder: 1 },
          { title: 'JSX and Components', duration: 540, isFree: true, sortOrder: 2 },
          { title: 'Props and State', duration: 660, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Hooks and Effects',
        sortOrder: 2,
        lessons: [
          { title: 'useState and useEffect', duration: 720, isFree: false, sortOrder: 1 },
          { title: 'Custom Hooks', duration: 540, isFree: false, sortOrder: 2 },
          { title: 'Context API', duration: 480, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Advanced React',
        sortOrder: 3,
        lessons: [
          { title: 'React Router', duration: 600, isFree: false, sortOrder: 1 },
          { title: 'Performance Optimization', duration: 540, isFree: false, sortOrder: 2 },
        ],
      },
    ],
  },
  {
    title: 'Introduction to Web Design',
    description: 'A free course covering the fundamentals of web design. Learn HTML, CSS, and responsive design principles to create beautiful websites from scratch.',
    price: 0,
    instructor: 'instructor_li',
    categoryName: 'Design',
    coverImage: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400',
    chapters: [
      {
        title: 'HTML Fundamentals',
        sortOrder: 1,
        lessons: [
          { title: 'HTML Document Structure', duration: 360, isFree: true, sortOrder: 1 },
          { title: 'HTML Elements and Tags', duration: 420, isFree: true, sortOrder: 2 },
          { title: 'Forms and Input Elements', duration: 480, isFree: true, sortOrder: 3 },
        ],
      },
      {
        title: 'CSS Styling',
        sortOrder: 2,
        lessons: [
          { title: 'CSS Selectors and Properties', duration: 540, isFree: true, sortOrder: 1 },
          { title: 'Flexbox Layout', duration: 600, isFree: true, sortOrder: 2 },
          { title: 'Responsive Design', duration: 660, isFree: true, sortOrder: 3 },
        ],
      },
    ],
  },
  {
    title: 'Python for Data Science',
    description: 'Dive into the world of data science with Python. Learn NumPy, Pandas, and Matplotlib to analyze and visualize data effectively.',
    price: 49.99,
    instructor: 'instructor_wang',
    categoryName: 'Data Science',
    coverImage: 'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=400',
    chapters: [
      {
        title: 'Python Basics for Data Science',
        sortOrder: 1,
        lessons: [
          { title: 'Python Environment Setup', duration: 300, isFree: true, sortOrder: 1 },
          { title: 'NumPy Fundamentals', duration: 720, isFree: false, sortOrder: 2 },
          { title: 'Pandas DataFrames', duration: 840, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Data Visualization',
        sortOrder: 2,
        lessons: [
          { title: 'Matplotlib Basics', duration: 600, isFree: false, sortOrder: 1 },
          { title: 'Seaborn for Statistical Plots', duration: 540, isFree: false, sortOrder: 2 },
        ],
      },
    ],
  },
  {
    title: 'Digital Marketing Essentials',
    description: 'Learn the fundamentals of digital marketing including SEO, social media marketing, and content strategy to grow your online presence.',
    price: 19.99,
    instructor: 'instructor_li',
    categoryName: 'Business',
    coverImage: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=400',
    chapters: [
      {
        title: 'Marketing Foundations',
        sortOrder: 1,
        lessons: [
          { title: 'Introduction to Digital Marketing', duration: 480, isFree: true, sortOrder: 1 },
          { title: 'Understanding Your Audience', duration: 540, isFree: false, sortOrder: 2 },
        ],
      },
      {
        title: 'SEO and Content',
        sortOrder: 2,
        lessons: [
          { title: 'Search Engine Optimization Basics', duration: 660, isFree: false, sortOrder: 1 },
          { title: 'Content Marketing Strategy', duration: 600, isFree: false, sortOrder: 2 },
          { title: 'Social Media Marketing', duration: 540, isFree: false, sortOrder: 3 },
        ],
      },
    ],
  },
  {
    title: 'Spring Boot Microservices',
    description: 'Build scalable microservices with Spring Boot and Spring Cloud. Covers service discovery, API gateway, circuit breakers, and Docker deployment.',
    price: 59.99,
    instructor: 'instructor_wang',
    categoryName: 'Programming',
    coverImage: 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=400',
    chapters: [
      {
        title: 'Spring Boot Basics',
        sortOrder: 1,
        lessons: [
          { title: 'Spring Boot Overview', duration: 420, isFree: true, sortOrder: 1 },
          { title: 'Creating REST APIs', duration: 600, isFree: true, sortOrder: 2 },
          { title: 'Spring Data JPA', duration: 720, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Microservices Architecture',
        sortOrder: 2,
        lessons: [
          { title: 'Service Discovery with Nacos', duration: 540, isFree: false, sortOrder: 1 },
          { title: 'API Gateway Pattern', duration: 600, isFree: false, sortOrder: 2 },
          { title: 'Circuit Breakers', duration: 480, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Deployment',
        sortOrder: 3,
        lessons: [
          { title: 'Docker Containerization', duration: 660, isFree: false, sortOrder: 1 },
          { title: 'Docker Compose Orchestration', duration: 540, isFree: false, sortOrder: 2 },
        ],
      },
    ],
  },
  {
    title: 'UI/UX Design Principles',
    description: 'Master the principles of user interface and user experience design. Learn about layout, typography, color theory, and prototyping.',
    price: 34.99,
    instructor: 'instructor_li',
    categoryName: 'Design',
    coverImage: 'https://images.unsplash.com/photo-1561070791-2526d30994b5?w=400',
    chapters: [
      {
        title: 'Design Fundamentals',
        sortOrder: 1,
        lessons: [
          { title: 'Introduction to UI/UX', duration: 360, isFree: true, sortOrder: 1 },
          { title: 'Layout and Grid Systems', duration: 540, isFree: false, sortOrder: 2 },
          { title: 'Typography in Design', duration: 480, isFree: false, sortOrder: 3 },
        ],
      },
      {
        title: 'Color and Prototyping',
        sortOrder: 2,
        lessons: [
          { title: 'Color Theory for Digital', duration: 420, isFree: false, sortOrder: 1 },
          { title: 'Prototyping with Figma', duration: 720, isFree: false, sortOrder: 2 },
        ],
      },
    ],
  },
  {
    title: 'English for IT Professionals',
    description: 'Improve your English communication skills for the tech industry. Covers technical writing, presentations, and common IT terminology.',
    price: 0,
    instructor: 'instructor_li',
    categoryName: 'Languages',
    coverImage: 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=400',
    chapters: [
      {
        title: 'Technical English Basics',
        sortOrder: 1,
        lessons: [
          { title: 'IT Vocabulary and Terminology', duration: 360, isFree: true, sortOrder: 1 },
          { title: 'Reading Technical Documentation', duration: 420, isFree: true, sortOrder: 2 },
          { title: 'Writing Technical Emails', duration: 480, isFree: true, sortOrder: 3 },
        ],
      },
      {
        title: 'Professional Communication',
        sortOrder: 2,
        lessons: [
          { title: 'Giving Technical Presentations', duration: 540, isFree: true, sortOrder: 1 },
          { title: 'Code Review Communication', duration: 420, isFree: true, sortOrder: 2 },
        ],
      },
    ],
  },
];

// Assign video URLs to all lessons
let globalVideoIdx = 0;
for (const course of COURSES) {
  for (const chapter of course.chapters) {
    for (const lesson of chapter.lessons) {
      lesson.videoUrl = getVideoUrl(globalVideoIdx++);
    }
  }
}

// Credentials for test use
export const TEST_CREDENTIALS = {
  admin: { usernameOrEmail: 'admin_user', password: 'Admin123456' },
  instructor: { usernameOrEmail: 'instructor_wang', password: 'Teach123456' },
  student: { usernameOrEmail: 'student_zhang', password: 'Study123456' },
  student2: { usernameOrEmail: 'student_chen', password: 'Study123456' },
};
