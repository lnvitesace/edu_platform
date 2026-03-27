import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import CourseCard from '../components/CourseCard';
import LoadingSpinner from '../components/LoadingSpinner';
import Navbar from '../components/Navbar';
import { courseService } from '../services/api';
import './CourseList.css';

/**
 * 课程列表页
 * 筛选状态（关键词、分类、分页）通过 URL searchParams 管理，
 * 使得筛选结果可以通过 URL 分享和浏览器后退恢复。
 */
const CourseList = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [courses, setCourses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pagination, setPagination] = useState({
    pageNumber: 0,
    pageSize: 12,
    totalPages: 0,
    totalElements: 0,
  });

  const [searchKeyword, setSearchKeyword] = useState(searchParams.get('keyword') || '');
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') || '');

  useEffect(() => {
    loadCategories();
  }, []);

  useEffect(() => {
    loadCourses();
  }, [searchParams]);

  const loadCategories = async () => {
    try {
      const data = await courseService.getCategories();
      setCategories(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load categories:', err);
    }
  };

  const loadCourses = async () => {
    setLoading(true);
    setError('');

    try {
      const page = parseInt(searchParams.get('page') || '0');
      const keyword = searchParams.get('keyword') || '';
      const categoryId = searchParams.get('category') || '';

      // 根据筛选条件选择不同的 API 端点，优先级：关键词搜索 > 分类筛选 > 默认列表
      let data;
      if (keyword) {
        data = await courseService.searchCourses(keyword, page, 12, categoryId || null);
      } else if (categoryId) {
        data = await courseService.getCoursesByCategory(categoryId, page, 12);
      } else {
        data = await courseService.getCourses(page, 12);
      }

      setCourses(data.content || []);
      setPagination({
        pageNumber: data.pageNumber || 0,
        pageSize: data.pageSize || 12,
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0,
      });
    } catch (err) {
      setError('Failed to load courses. Please try again.');
      console.error('Failed to load courses:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (searchKeyword) params.set('keyword', searchKeyword);
    if (selectedCategory) params.set('category', selectedCategory);
    params.set('page', '0');
    setSearchParams(params);
  };

  const handleCategoryChange = (categoryId) => {
    setSelectedCategory(categoryId);
    const params = new URLSearchParams();
    if (searchKeyword) params.set('keyword', searchKeyword);
    if (categoryId) params.set('category', categoryId);
    params.set('page', '0');
    setSearchParams(params);
  };

  const handlePageChange = (newPage) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', newPage.toString());
    setSearchParams(params);
  };

  const clearFilters = () => {
    setSearchKeyword('');
    setSelectedCategory('');
    setSearchParams({});
  };

  return (
    <div className="course-list-page">
      <Navbar />
      <div className="course-list-container">
        <div className="course-list-header">
          <h1>Explore Courses</h1>
          <p>Discover the best courses to advance your skills</p>
        </div>

        <div className="course-list-filters">
          <form onSubmit={handleSearch} className="search-form">
            <input
              type="text"
              placeholder="Search courses..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="search-input"
            />
            <button type="submit" className="btn-primary search-btn">
              Search
            </button>
          </form>

          <div className="filter-group">
            <select
              value={selectedCategory}
              onChange={(e) => handleCategoryChange(e.target.value)}
              className="category-select"
            >
              <option value="">All Categories</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>

            {(searchKeyword || selectedCategory) && (
              <button onClick={clearFilters} className="clear-filters-btn">
                Clear Filters
              </button>
            )}
          </div>
        </div>

        {loading ? (
          <LoadingSpinner size="large" text="Loading courses..." />
        ) : error ? (
          <div className="error-container">
            <p className="error-message">{error}</p>
            <button onClick={loadCourses} className="btn-primary">
              Try Again
            </button>
          </div>
        ) : courses.length === 0 ? (
          <div className="empty-container">
            <p>No courses found</p>
            {(searchKeyword || selectedCategory) && (
              <button onClick={clearFilters} className="btn-secondary">
                Clear Filters
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="course-grid">
              {courses.map((course) => (
                <CourseCard key={course.id} course={course} />
              ))}
            </div>

            {pagination.totalPages > 1 && (
              <div className="pagination">
                <button
                  onClick={() => handlePageChange(pagination.pageNumber - 1)}
                  disabled={pagination.pageNumber === 0}
                  className="pagination-btn"
                >
                  Previous
                </button>
                <span className="pagination-info">
                  Page {pagination.pageNumber + 1} of {pagination.totalPages}
                </span>
                <button
                  onClick={() => handlePageChange(pagination.pageNumber + 1)}
                  disabled={pagination.pageNumber >= pagination.totalPages - 1}
                  className="pagination-btn"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default CourseList;
