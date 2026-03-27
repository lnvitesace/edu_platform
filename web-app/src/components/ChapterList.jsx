import { useState } from 'react';
import './ChapterList.css';

/**
 * 章节目录组件，用于课程详情页和视频播放页的侧栏
 * 支持章节折叠/展开，默认全部展开以便用户快速浏览课程结构。
 * currentLessonId 用于在视频播放页高亮当前课时。
 * isEnrolled 控制非免费课时的锁定显示。
 */
const ChapterList = ({ chapters, currentLessonId, onLessonClick, courseId, isEnrolled: _isEnrolled = false }) => {
  const [expandedChapters, setExpandedChapters] = useState(
    chapters?.map(() => true) || []
  );

  const toggleChapter = (index) => {
    setExpandedChapters((prev) => {
      const newState = [...prev];
      newState[index] = !newState[index];
      return newState;
    });
  };

  const formatDuration = (seconds) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (!chapters || chapters.length === 0) {
    return (
      <div className="chapter-list-empty">
        <p>No chapters available</p>
      </div>
    );
  }

  return (
    <div className="chapter-list">
      {chapters.map((chapter, chapterIndex) => (
        <div key={chapter.id} className="chapter-item">
          <div
            className="chapter-header"
            onClick={() => toggleChapter(chapterIndex)}
          >
            <span className="chapter-toggle">
              {expandedChapters[chapterIndex] ? '▼' : '▶'}
            </span>
            <span className="chapter-title">
              Chapter {chapterIndex + 1}: {chapter.title}
            </span>
            <span className="chapter-lesson-count">
              {chapter.lessons?.length || 0} lessons
            </span>
          </div>
          {expandedChapters[chapterIndex] && chapter.lessons && (
            <div className="lesson-list">
              {chapter.lessons.map((lesson, lessonIndex) => {
                const isLocked = !lesson.isFree;
                return (
                  <div
                    key={lesson.id}
                    className={`lesson-item ${
                      currentLessonId === lesson.id ? 'active' : ''
                    } ${isLocked ? 'locked' : ''}`}
                    onClick={() => onLessonClick && onLessonClick(lesson, courseId)}
                  >
                    <span className="lesson-index">
                      {chapterIndex + 1}.{lessonIndex + 1}
                    </span>
                    <span className="lesson-title">{lesson.title}</span>
                    <div className="lesson-meta">
                      {isLocked && <span className="lesson-lock-icon">🔒</span>}
                      {lesson.isFree && (
                        <span className="lesson-free-badge">Free</span>
                      )}
                      {lesson.duration && (
                        <span className="lesson-duration">
                          {formatDuration(lesson.duration)}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default ChapterList;
