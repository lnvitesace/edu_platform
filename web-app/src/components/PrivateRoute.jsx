import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * 路由守卫组件
 * 需要先等待 AuthContext 的 loading 完成，否则在 localStorage 会话恢复之前
 * 会误判为未认证而跳转到登录页。
 */
const PrivateRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  return isAuthenticated ? children : <Navigate to="/login" />;
};

export default PrivateRoute;
