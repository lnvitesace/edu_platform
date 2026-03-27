import { useNavigate } from 'react-router-dom';
import './CourseCard.css';

/** 课程卡片组件，整张卡片可点击跳转到课程详情 */
const CourseCard = ({ course }) => {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/courses/${course.id}`);
  };

  const formatPrice = (price) => {
    if (!price || price === 0) return 'Free';
    return `$${parseFloat(price).toFixed(2)}`;
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      PUBLISHED: { text: 'Published', className: 'status-published' },
      DRAFT: { text: 'Draft', className: 'status-draft' },
      ARCHIVED: { text: 'Archived', className: 'status-archived' },
    };
    return statusMap[status] || { text: status, className: '' };
  };

  const statusBadge = getStatusBadge(course.status);

  return (
    <div className="course-card" onClick={handleClick}>
      <div className="course-card-image">
        {course.coverImage ? (
          <img src={course.coverImage} alt={course.title} />
        ) : (
          <div className="course-card-placeholder">
            <span>{course.title?.charAt(0) || 'C'}</span>
          </div>
        )}
        {course.status && (
          <span className={`course-card-status ${statusBadge.className}`}>
            {statusBadge.text}
          </span>
        )}
      </div>
      <div className="course-card-content">
        <h3 className="course-card-title">{course.title}</h3>
        {course.categoryName && (
          <p className="course-card-category">{course.categoryName}</p>
        )}
        <p className="course-card-description">
          {course.description?.substring(0, 100)}
          {course.description?.length > 100 ? '...' : ''}
        </p>
        <div className="course-card-footer">
          <span className="course-card-price">{formatPrice(course.price)}</span>
          <span className="course-card-lessons">
            {course.chapters?.reduce((acc, ch) => acc + (ch.lessons?.length || 0), 0) || 0} lessons
          </span>
        </div>
      </div>
    </div>
  );
};

export default CourseCard;
