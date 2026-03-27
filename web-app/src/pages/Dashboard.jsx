import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { userService, enrollmentService } from '../services/api';
import './Dashboard.css';

/** 用户仪表板：展示和编辑个人资料，需要登录态（由 PrivateRoute 保护） */
const Dashboard = () => {
  const { user, logout, updateUser } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [enrollments, setEnrollments] = useState([]);

  useEffect(() => {
    loadProfile();
    loadEnrollments();
  }, []);

  const loadEnrollments = async () => {
    try {
      const data = await enrollmentService.getMyEnrollments();
      setEnrollments(data);
    } catch (err) {
      console.error('Failed to load enrollments:', err);
    }
  };

  const loadProfile = async () => {
    try {
      const data = await userService.getProfile();
      setProfile(data);
      setFormData({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        phone: data.phone || '',
        bio: data.profile?.bio || '',
        country: data.profile?.country || '',
        city: data.profile?.city || '',
      });
    } catch {
      setError('Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const updatedUser = await userService.updateProfile(formData);
      setProfile(updatedUser);
      updateUser(updatedUser);
      setEditing(false);
    } catch {
      setError('Failed to update profile');
    }
  };

  // 用 window.location 而非 navigate 强制全页刷新，确保清除所有内存中的用户状态
  const handleLogout = async () => {
    await logout();
    window.location.href = '/login';
  };

  if (loading) {
    return <div className="dashboard-container">Loading...</div>;
  }

  return (
    <div className="dashboard-container">
      <button onClick={() => navigate('/courses')} className="btn-back">
        <span className="btn-back-arrow">&#8592;</span>
        <span className="btn-back-label">Browse Courses</span>
      </button>

      <div className="dashboard-header">
        <h1>Welcome, {user?.username}!</h1>
        <button onClick={handleLogout} className="btn-secondary">
          Logout
        </button>
      </div>

      <div className="profile-section">
        <h2>Profile Information</h2>
        {error && <div className="error-message">{error}</div>}

        {!editing ? (
          <div className="profile-view">
            <div className="profile-item">
              <strong>Username:</strong> {profile?.username}
            </div>
            <div className="profile-item">
              <strong>Email:</strong> {profile?.email}
            </div>
            <div className="profile-item">
              <strong>Name:</strong> {profile?.firstName} {profile?.lastName}
            </div>
            <div className="profile-item">
              <strong>Phone:</strong> {profile?.phone || 'Not provided'}
            </div>
            <div className="profile-item">
              <strong>Role:</strong> {profile?.role}
            </div>
            <div className="profile-item">
              <strong>Bio:</strong> {profile?.profile?.bio || 'Not provided'}
            </div>
            <div className="profile-item">
              <strong>Location:</strong>{' '}
              {profile?.profile?.city && profile?.profile?.country
                ? `${profile.profile.city}, ${profile.profile.country}`
                : 'Not provided'}
            </div>
            <button onClick={() => setEditing(true)} className="btn-primary">
              Edit Profile
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="profile-edit">
            <div className="form-group">
              <label>First Name</label>
              <input
                type="text"
                name="firstName"
                value={formData.firstName}
                onChange={handleChange}
              />
            </div>
            <div className="form-group">
              <label>Last Name</label>
              <input
                type="text"
                name="lastName"
                value={formData.lastName}
                onChange={handleChange}
              />
            </div>
            <div className="form-group">
              <label>Phone</label>
              <input
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
              />
            </div>
            <div className="form-group">
              <label>Bio</label>
              <textarea
                name="bio"
                value={formData.bio}
                onChange={handleChange}
                rows="4"
              />
            </div>
            <div className="form-group">
              <label>Country</label>
              <input
                type="text"
                name="country"
                value={formData.country}
                onChange={handleChange}
              />
            </div>
            <div className="form-group">
              <label>City</label>
              <input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleChange}
              />
            </div>
            <div className="form-actions">
              <button type="submit" className="btn-primary">
                Save Changes
              </button>
              <button
                type="button"
                onClick={() => setEditing(false)}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>

      <div className="enrollments-section">
        <h2>My Courses</h2>
        {enrollments.length === 0 ? (
          <p className="no-enrollments">
            You haven't enrolled in any courses yet.{' '}
            <button onClick={() => navigate('/courses')} className="link-btn">
              Browse courses
            </button>
          </p>
        ) : (
          <div className="enrollment-grid">
            {enrollments.map((enrollment) => (
              <div
                key={enrollment.id}
                className="enrollment-card"
                onClick={() => navigate(`/courses/${enrollment.courseId}`)}
              >
                {enrollment.courseCoverImage ? (
                  <img
                    src={enrollment.courseCoverImage}
                    alt={enrollment.courseTitle}
                    className="enrollment-cover"
                  />
                ) : (
                  <div className="enrollment-cover-placeholder">
                    {enrollment.courseTitle?.charAt(0) || 'C'}
                  </div>
                )}
                <div className="enrollment-info">
                  <h3>{enrollment.courseTitle}</h3>
                  <span className="enrollment-date">
                    Enrolled: {new Date(enrollment.enrolledAt).toLocaleDateString()}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;
