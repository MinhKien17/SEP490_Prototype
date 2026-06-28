import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { LanguageProvider } from './context/LanguageContext';
import ProtectedRoute from './components/ProtectedRoute';

import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Profile from './pages/Profile.jsx';
import AdminDashboard from './pages/Admin/AdminDashboard.jsx';
// INSTRUCTOR SUB-SYSTEM IMPORTS (Đã map chuẩn xác theo thực tế API Collection)
import CollectionList from './pages/Instructor/CollectionList.jsx';
import CreateCollection from './pages/Instructor/CreateCollection.jsx';
import ReviewRequests from './pages/Instructor/ReviewRequests.jsx';
import InstructorDashboard from './pages/Instructor/Dashboard.jsx';

// STUDENT SUB-SYSTEM IMPORTS
import StudentProjects from './pages/Student/Projects.jsx';
import Workspace from './pages/Student/Workspace.jsx';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <LanguageProvider>
          <Routes>
            {/* Public Entry Nodes */}
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Shared Authenticated Node */}
            <Route path="/instructor/profile" element={
  
                <Profile /> 
           
            } />

            {/* Instructor / Admin Telemetry Control Hubs */}
            <Route path="/instructor/dashboard" element={
             <InstructorDashboard />
            } />
            <Route path="/instructor/requests" element={
              <ReviewRequests />
            } />
            
            {/* Đồng bộ cấu trúc route riêng biệt cho phân hệ Evidence Collection */}
            <Route path="/instructor/collections" element={
              <CollectionList />
            } />
            <Route path="/instructor/collections/create" element={
              <CreateCollection />
            } />

            {/* Student Sandbox Execution Workspace */}
            <Route path="/student/projects" element={
              <ProtectedRoute allowedRoles={['STUDENT']}><StudentProjects /></ProtectedRoute>
            } />
            <Route path="/student/projects/:projectId" element={
              <ProtectedRoute allowedRoles={['STUDENT']}><Workspace /></ProtectedRoute>
            } />
            <Route path="/admin/dashboard" element={
            <AdminDashboard />
          } />
          </Routes>
        </LanguageProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;