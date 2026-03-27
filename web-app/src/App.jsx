import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import CourseList from './pages/CourseList';
import CourseDetail from './pages/CourseDetail';
import VideoPlayer from './pages/VideoPlayer';
import './App.css';

/**
 * 应用路由配置
 * AuthProvider 必须在 Router 内部，因为认证相关操作依赖路由跳转。
 * 课程浏览页面（列表/详情）对外开放，视频播放和仪表板需要登录保护。
 */
function App() {
  return (
    <Router>
      <AuthProvider>
        <div className="app">
          <Routes>
            <Route path="/" element={<Navigate to="/courses" />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/courses" element={<CourseList />} />
            <Route path="/courses/:id" element={<CourseDetail />} />
            {/* 视频播放需要登录态，未认证用户由 PrivateRoute 重定向到登录页 */}
            <Route
              path="/courses/:courseId/lessons/:lessonId"
              element={
                <PrivateRoute>
                  <VideoPlayer />
                </PrivateRoute>
              }
            />
            <Route
              path="/dashboard"
              element={
                <PrivateRoute>
                  <Dashboard />
                </PrivateRoute>
              }
            />
          </Routes>
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;
