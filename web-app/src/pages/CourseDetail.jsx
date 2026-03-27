import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ChapterList from '../components/ChapterList';
import LoadingSpinner from '../components/LoadingSpinner';
import Navbar from '../components/Navbar';
import { courseService, enrollmentService } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './CourseDetail.css';

/** 课程详情页：展示课程信息和章节目录，未登录用户点击课时会跳转登录 */
const CourseDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [course, setCourse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('description');
  const [isEnrolled, setIsEnrolled] = useState(false);
  const [enrolling, setEnrolling] = useState(false);
  const [checkingEnrollment, setCheckingEnrollment] = useState(false);

  useEffect(() => {
    loadCourse();
  }, [id]);

  useEffect(() => {
    if (isAuthenticated && id) {
      setCheckingEnrollment(true);
      enrollmentService.checkEnrollment(id)
        .then(setIsEnrolled)
        .catch(() => {})
        .finally(() => setCheckingEnrollment(false));
    } else {
      setIsEnrolled(false);
      setCheckingEnrollment(false);
    }
  }, [isAuthenticated, id]);

  const loadCourse = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await courseService.getCourseById(id);
      setCourse(data);
    } catch (err) {
      setError('Failed to load course. Please try again.');
      console.error('Failed to load course:', err);
    } finally {
      setLoading(false);
    }
  };

  // 课时点击的认证守卫：虽然路由层有 PrivateRoute 保护，但在此提前拦截
  // 可以避免用户先跳转再被重定向的体验割裂
  const handleLessonClick = (lesson, courseId) => {
    if (checkingEnrollment || enrolling) {
      return;
    }
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!lesson.isFree) {
      return;
    }
    if (!isEnrolled) {
      handleEnroll();
      return;
    }
    navigate(`/courses/${courseId}/lessons/${lesson.id}`);
  };

  const handleEnroll = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    setEnrolling(true);
    try {
      await enrollmentService.enroll(id);
      setIsEnrolled(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to enroll');
    } finally {
      setEnrolling(false);
    }
  };

  const formatPrice = (price) => {
    if (!price || price === 0) return 'Free';
    return `$${parseFloat(price).toFixed(2)}`;
  };

  const getTotalLessons = () => {
    if (!course?.chapters) return 0;
    return course.chapters.reduce((acc, ch) => acc + (ch.lessons?.length || 0), 0);
  };

  const getTotalDuration = () => {
    if (!course?.chapters) return 0;
    let total = 0;
    course.chapters.forEach((ch) => {
      ch.lessons?.forEach((lesson) => {
        total += lesson.duration || 0;
      });
    });
    const hours = Math.floor(total / 3600);
    const mins = Math.floor((total % 3600) / 60);
    if (hours > 0) return `${hours}h ${mins}m`;
    return `${mins} min`;
  };

  const getFirstLesson = () => {
    if (!course?.chapters?.[0]?.lessons?.[0]) return null;
    return course.chapters[0].lessons[0];
  };

  const handleStartLearning = () => {
    if (checkingEnrollment || enrolling) {
      return;
    }
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    if (!isEnrolled) {
      handleEnroll();
      return;
    }
    const firstLesson = getFirstLesson();
    if (firstLesson) {
      navigate(`/courses/${course.id}/lessons/${firstLesson.id}`);
    }
  };

  const getActionButtonText = () => {
    if (!isAuthenticated) return 'Login to Start';
    if (enrolling) return 'Enrolling...';
    if (isEnrolled) return 'Continue Learning';
    return 'Enroll Now';
  };

  if (loading) {
    return (
      <div className="course-detail-page">
        <Navbar />
        <LoadingSpinner size="large" text="Loading course..." />
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="course-detail-page">
        <Navbar />
        <div className="error-container">
          <p className="error-message">{error || 'Course not found'}</p>
          <button onClick={() => navigate('/courses')} className="btn-primary">
            Back to Courses
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="course-detail-page">
      <Navbar />
      <div className="course-detail-container">
        <div className="course-detail-header">
          <div className="course-cover">
            {course.coverImage ? (
              <img src={course.coverImage} alt={course.title} />
            ) : (
              <div className="course-cover-placeholder">
                <span>{course.title?.charAt(0) || 'C'}</span>
              </div>
            )}
          </div>
          <div className="course-info">
            {course.categoryName && (
              <span className="course-category">{course.categoryName}</span>
            )}
            <h1 className="course-title">{course.title}</h1>
            <div className="course-meta">
              <span className="course-lessons">{getTotalLessons()} lessons</span>
              <span className="course-duration">{getTotalDuration()}</span>
              <span className="course-chapters">{course.chapters?.length || 0} chapters</span>
            </div>
            <div className="course-price-section">
              <span className="course-price">{formatPrice(course.price)}</span>
              {isAuthenticated && checkingEnrollment ? (
                <button className="btn-primary" disabled>
                  Checking Enrollment...
                </button>
              ) : (
                <button
                  onClick={handleStartLearning}
                  className="btn-primary start-btn"
                  disabled={enrolling}
                >
                  {getActionButtonText()}
                </button>
              )}
            </div>
          </div>
        </div>

        <div className="course-detail-content">
          <div className="course-tabs">
            <button
              className={`tab-btn ${activeTab === 'description' ? 'active' : ''}`}
              onClick={() => setActiveTab('description')}
            >
              Description
            </button>
            <button
              className={`tab-btn ${activeTab === 'curriculum' ? 'active' : ''}`}
              onClick={() => setActiveTab('curriculum')}
            >
              Curriculum
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'description' && (
              <div className="description-content">
                <h2>About This Course</h2>
                <p>{course.description || 'No description available.'}</p>
              </div>
            )}

            {activeTab === 'curriculum' && (
              <div className="curriculum-content">
                <h2>Course Curriculum</h2>
                <ChapterList
                  chapters={course.chapters || []}
                  onLessonClick={handleLessonClick}
                  courseId={course.id}
                  isEnrolled={isEnrolled}
                />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CourseDetail;
