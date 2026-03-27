import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ChapterList from '../components/ChapterList';
import LoadingSpinner from '../components/LoadingSpinner';
import Navbar from '../components/Navbar';
import { courseService, progressService } from '../services/api';
import './VideoPlayer.css';

/** 视频播放页：左侧播放器 + 右侧章节目录，支持课时间顺序导航 */
const VideoPlayer = () => {
  const { courseId, lessonId } = useParams();
  const navigate = useNavigate();
  const videoRef = useRef(null);
  const lastReportedRef = useRef(0);
  const [course, setCourse] = useState(null);
  const [lesson, setLesson] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [progressData, setProgressData] = useState(null);

  useEffect(() => {
    loadData();
  }, [courseId, lessonId]);

  // 节流上报：每 15 秒向后端同步一次观看进度
  const handleTimeUpdate = () => {
    const video = videoRef.current;
    if (!video) return;
    const now = Date.now();
    if (now - lastReportedRef.current < 15000) return;
    lastReportedRef.current = now;
    const seconds = Math.floor(video.currentTime);
    progressService.updateProgress(parseInt(lessonId), seconds)
      .then((data) => setProgressData(data))
      .catch(() => {});
  };

  // 视频播放结束时立即上报最终进度
  const handleEnded = () => {
    const video = videoRef.current;
    if (!video) return;
    const seconds = Math.floor(video.currentTime);
    progressService.updateProgress(parseInt(lessonId), seconds)
      .then((data) => setProgressData(data))
      .catch(() => {});
  };

  // 课程数据在同一课程内切换课时时复用，避免重复请求；仅课时数据每次重新加载。
  // 首次加载显示全屏 loading，课时切换时静默加载以保持侧栏可见。
  const loadData = async () => {
    const isInitialLoad = !course;
    if (isInitialLoad) setLoading(true);
    setError('');
    setProgressData(null);
    try {
      const [courseData, lessonData] = await Promise.all([
        course && course.id === parseInt(courseId)
          ? Promise.resolve(course)
          : courseService.getCourseById(courseId),
        courseService.getLessonById(lessonId),
      ]);
      setCourse(courseData);
      setLesson(lessonData);
    } catch (err) {
      if (err.response?.status === 403) {
        setError('You must enroll in this course to access this lesson.');
      } else {
        setError('Failed to load lesson. Please try again.');
      }
      console.error('Failed to load lesson:', err);
    } finally {
      if (isInitialLoad) setLoading(false);
    }
  };

  useEffect(() => {
    if (!lessonId || !lesson) {
      return;
    }

    let cancelled = false;

    const loadProgress = async () => {
      try {
        const progress = await progressService.getLessonProgress(lessonId);
        if (cancelled) {
          return;
        }
        setProgressData(progress);
        if (progress.watchedSeconds > 0 && videoRef.current) {
          videoRef.current.currentTime = progress.watchedSeconds;
        }
      } catch {
        // 进度加载失败不阻塞播放
      }
    };

    loadProgress();

    return () => {
      cancelled = true;
    };
  }, [lessonId, lesson]);

  const handleLessonClick = (newLesson) => {
    if (!newLesson.isFree) return;
    navigate(`/courses/${courseId}/lessons/${newLesson.id}`);
  };

  // 将嵌套的章节-课时结构扁平化为有序列表，用于上/下一课时导航
  const getAllLessons = () => {
    if (!course?.chapters) return [];
    const lessons = [];
    course.chapters.forEach((chapter) => {
      chapter.lessons?.forEach((lesson) => {
        lessons.push({ ...lesson, chapterId: chapter.id });
      });
    });
    return lessons;
  };

  const getCurrentLessonIndex = () => {
    const lessons = getAllLessons();
    return lessons.findIndex((l) => l.id === parseInt(lessonId));
  };

  const handlePrevious = () => {
    const lessons = getAllLessons();
    const currentIndex = getCurrentLessonIndex();
    if (currentIndex > 0) {
      const prev = lessons[currentIndex - 1];
      if (!prev.isFree) return;
      navigate(`/courses/${courseId}/lessons/${prev.id}`);
    }
  };

  const handleNext = () => {
    const lessons = getAllLessons();
    const currentIndex = getCurrentLessonIndex();
    if (currentIndex < lessons.length - 1) {
      const next = lessons[currentIndex + 1];
      if (!next.isFree) return;
      navigate(`/courses/${courseId}/lessons/${next.id}`);
    }
  };

  const formatDuration = (seconds) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (loading) {
    return (
      <div className="video-player-page">
        <Navbar />
        <LoadingSpinner size="large" text="Loading lesson..." />
      </div>
    );
  }

  if (error || !lesson) {
    return (
      <div className="video-player-page">
        <Navbar />
        <div className="error-container">
          <p className="error-message">{error || 'Lesson not found'}</p>
          <button onClick={() => navigate(`/courses/${courseId}`)} className="btn-primary">
            Back to Course
          </button>
        </div>
      </div>
    );
  }

  const lessons = getAllLessons();
  const currentIndex = getCurrentLessonIndex();
  const hasPrevious = currentIndex > 0;
  const hasNext = currentIndex < lessons.length - 1;

  return (
    <div className="video-player-page">
      <Navbar />
      <div className="video-player-container">
        <div className="video-section">
          <div className="video-wrapper">
            {lesson.videoUrl ? (
              <video
                key={lessonId}
                ref={videoRef}
                src={lesson.videoUrl}
                controls
                autoPlay
                onTimeUpdate={handleTimeUpdate}
                onEnded={handleEnded}
                className="video-element"
              >
                Your browser does not support the video tag.
              </video>
            ) : (
              <div className="video-placeholder">
                <p>Video not available</p>
              </div>
            )}
          </div>
          <div className="lesson-info">
            <h1 className="lesson-title">{lesson.title}</h1>
            {lesson.duration && (
              <span className="lesson-duration">
                Duration: {formatDuration(lesson.duration)}
              </span>
            )}
            {progressData && (
              <span className={`progress-badge ${progressData.completed ? 'completed' : ''}`}>
                {progressData.completed
                  ? 'Completed'
                  : `${Math.min(100, Math.round((progressData.watchedSeconds / (progressData.totalDuration || 1)) * 100))}% watched`}
              </span>
            )}
          </div>
          <div className="lesson-navigation">
            <button
              onClick={handlePrevious}
              disabled={!hasPrevious}
              className="nav-btn prev-btn"
            >
              Previous Lesson
            </button>
            <button
              onClick={() => navigate(`/courses/${courseId}`)}
              className="nav-btn course-btn"
            >
              Back to Course
            </button>
            <button
              onClick={handleNext}
              disabled={!hasNext}
              className="nav-btn next-btn"
            >
              Next Lesson
            </button>
          </div>
        </div>
        <div className="sidebar-section">
          <div className="sidebar-header">
            <h3>{course?.title || 'Course Content'}</h3>
          </div>
          <ChapterList
            chapters={course?.chapters || []}
            currentLessonId={parseInt(lessonId)}
            onLessonClick={handleLessonClick}
            courseId={courseId}
          />
        </div>
      </div>
    </div>
  );
};

export default VideoPlayer;
