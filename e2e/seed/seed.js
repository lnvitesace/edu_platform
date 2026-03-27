import { USERS, CATEGORIES, COURSES } from './data.js';
import { registerUser, getCategories, createCategory, getCourses, createCourse, createChapter, createLesson, publishCourse } from './api-client.js';

export default async function globalSetup() {
  console.log('\n🌱 Seeding edu_platform data...\n');

  // Step 1: Register/login all users
  const tokens = {};
  for (const user of USERS) {
    try {
      const result = await registerUser(user);
      tokens[user.username] = result.accessToken;
      console.log(`  ✓ User: ${user.username} (${user.role})`);
    } catch (err) {
      console.error(`  ✗ Failed to create user ${user.username}:`, err.response?.data?.message || err.message);
      throw err;
    }
  }

  // Step 2: Create categories (with admin token)
  const adminToken = tokens['admin_user'];
  let existingCategories = [];
  try {
    const catResponse = await getCategories(adminToken);
    existingCategories = catResponse.content || catResponse || [];
  } catch { existingCategories = []; }

  const categoryMap = {};
  // Map existing categories
  if (Array.isArray(existingCategories)) {
    for (const cat of existingCategories) {
      categoryMap[cat.name] = cat.id;
    }
  }

  for (const cat of CATEGORIES) {
    if (categoryMap[cat.name]) {
      console.log(`  ⊘ Category exists: ${cat.name}`);
      continue;
    }
    try {
      const created = await createCategory(adminToken, cat);
      categoryMap[cat.name] = created.id;
      console.log(`  ✓ Category: ${cat.name} (id=${created.id})`);
    } catch (err) {
      console.error(`  ✗ Failed to create category ${cat.name}:`, err.response?.data?.message || err.message);
    }
  }

  // Step 3: Check existing courses to avoid duplicates
  let existingCourses = [];
  try {
    const courseResponse = await getCourses(adminToken);
    existingCourses = courseResponse.content || [];
  } catch { existingCourses = []; }
  const existingTitles = new Set(existingCourses.map(c => c.title));

  // Step 4: Create courses with chapters and lessons
  for (const courseDef of COURSES) {
    if (existingTitles.has(courseDef.title)) {
      console.log(`  ⊘ Course exists: ${courseDef.title}`);
      continue;
    }

    const instructorToken = tokens[courseDef.instructor];
    const categoryId = categoryMap[courseDef.categoryName];

    try {
      // Create course
      const course = await createCourse(instructorToken, {
        title: courseDef.title,
        description: courseDef.description,
        price: courseDef.price,
        coverImage: courseDef.coverImage,
        categoryId,
      });
      console.log(`  ✓ Course: ${courseDef.title} (id=${course.id})`);

      // Create chapters and lessons
      for (const chapterDef of courseDef.chapters) {
        const chapter = await createChapter(instructorToken, course.id, {
          title: chapterDef.title,
          sortOrder: chapterDef.sortOrder,
        });

        for (const lessonDef of chapterDef.lessons) {
          await createLesson(instructorToken, chapter.id, {
            title: lessonDef.title,
            videoUrl: lessonDef.videoUrl,
            duration: lessonDef.duration,
            isFree: lessonDef.isFree,
            sortOrder: lessonDef.sortOrder,
          });
        }
        console.log(`    ✓ Chapter: ${chapterDef.title} (${chapterDef.lessons.length} lessons)`);
      }

      // Publish course
      await publishCourse(instructorToken, course.id);
      console.log(`    ✓ Published`);
    } catch (err) {
      console.error(`  ✗ Failed to create course ${courseDef.title}:`, err.response?.data?.message || err.message);
    }
  }

  console.log('\n✅ Seed complete!\n');
}

// Allow running directly: node seed/seed.js
const isDirectRun = process.argv[1]?.endsWith('seed.js') || process.argv[1]?.endsWith('seed/seed.js');
if (isDirectRun) {
  globalSetup().catch(err => {
    console.error('Seed failed:', err);
    process.exit(1);
  });
}
