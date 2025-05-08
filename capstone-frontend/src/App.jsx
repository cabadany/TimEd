import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './Dashboard/Dashboard';
import Event from './Event/Event';
import LoginPage from './Auth/LoginPage';
import Setting from './Setting/Setting';
import './App.css';
import ProtectedRoute from './ProtectedRoute';  // Import the ProtectedRoute
import Accounts from './Account/Account';
import Department from './departments/Department';
import Attendance from './attendance/Attendance';
import { Box } from '@mui/material';
import { UserProvider } from './contexts/UserContext';
import MainLayout from './layouts/MainLayout';

// Simple layout without sidebar or header, for login page
const SimpleLayout = ({ children }) => {
  return (
    <Box sx={{ position: 'relative', height: '100vh', width: '100vw' }}>
      {children}
    </Box>
  );
};

function App() {
  return (
    <UserProvider>
      <Router>
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<SimpleLayout><LoginPage /></SimpleLayout>} />
          <Route path="/login" element={<SimpleLayout><LoginPage /></SimpleLayout>} />
          
          {/* Protected routes with MainLayout */}
          <Route 
            path="/department" 
            element={
              <ProtectedRoute>
                <MainLayout title="Departments">
                  <Department />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route 
            path="/attendance/:eventId" 
            element={
              <ProtectedRoute>
                <MainLayout title="Attendance">
                  <Attendance />
                </MainLayout>
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <MainLayout title="Dashboard">
                  <Dashboard />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route 
            path="/accounts" 
            element={
              <ProtectedRoute>
                <MainLayout title="Accounts">
                  <Accounts />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route 
            path="/event" 
            element={
              <ProtectedRoute>
                <MainLayout title="Events">
                  <Event />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route 
            path="/settings" 
            element={
              <ProtectedRoute>
                <MainLayout title="Settings">
                  <Setting />
                </MainLayout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </Router>
    </UserProvider>
  );
}

export default App;
