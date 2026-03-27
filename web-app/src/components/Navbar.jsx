import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

/** 顶部导航栏，根据认证状态动态切换导航项和用户操作区 */
const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo">
          EduPlatform
        </Link>

        <div className="navbar-links">
          <Link to="/courses" className="navbar-link">
            Courses
          </Link>
          {isAuthenticated && (
            <Link to="/dashboard" className="navbar-link">
              My Learning
            </Link>
          )}
        </div>

        <div className="navbar-auth">
          {isAuthenticated ? (
            <div className="navbar-user">
              <span className="navbar-username">{user?.username}</span>
              <button onClick={handleLogout} className="navbar-logout-btn">
                Logout
              </button>
            </div>
          ) : (
            <div className="navbar-auth-links">
              <Link to="/login" className="navbar-link">
                Login
              </Link>
              <Link to="/register" className="btn-primary navbar-register-btn">
                Register
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
