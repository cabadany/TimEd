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
import NotificationSystem from './components/NotificationSystem';

// Layout component without fixed notification positioning
const AppLayout = ({ children }) => {
  return (
    <Box sx={{ position: 'relative' }}>
      {children}
    </Box>
  );
};

function App() {
  return (
    <Router>
      <Routes>
        {/* Public routes */}
        <Route path="/" element={<LoginPage />} />
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected routes (Only Admin can access) */}
        <Route 
          path="/department" 
          element={
            <ProtectedRoute>
              <AppLayout>
                <Department />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route 
          path="/attendance/:eventId" 
          element={
            <ProtectedRoute>
              <AppLayout>
                <Attendance />
              </AppLayout>
            </ProtectedRoute>
          } 
        />

        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <AppLayout>
                <Dashboard />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route 
          path="/accounts" 
          element={
            <ProtectedRoute>
              <AppLayout>
                <Accounts />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route 
          path="/event" 
          element={
            <ProtectedRoute>
              <AppLayout>
                <Event />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route 
          path="/settings" 
          element={
            <ProtectedRoute>
              <AppLayout>
                <Setting />
              </AppLayout>
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;
