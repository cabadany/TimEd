import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './Dashboard/Dashboard';
import Event from './Event/Event';
import LoginPage from './Auth/LoginPage';
import Setting from './Setting/Setting';
import './App.css';
import './styles/darkMode.css';
import './styles/appHeader.css';
import ProtectedRoute from './ProtectedRoute';  //Import the ProtectedRoute
import Accounts from './Account/Account';
import Department from './departments/Department';
import Attendance from './attendance/Attendance';
import { Box } from '@mui/material';
import { UserProvider } from './contexts/UserContext';
import { ThemeProvider } from './contexts/ThemeContext';
import MainLayout from './layouts/MainLayout';
import Certificate from './certificate/certificate';
import QRJoin from './Event/QRJoin';
import ExcuseLetters from './ExcuseLetters/ExcuseLetters';
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
    <ThemeProvider>
      <UserProvider>
        <Router>
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<SimpleLayout><LoginPage /></SimpleLayout>} />
            <Route path="/login" element={<SimpleLayout><LoginPage /></SimpleLayout>} />
            <Route 
              path="/qr-join/:eventId" 
              element={
                <ProtectedRoute adminOnly={false}>
                 <SimpleLayout>
                    <QRJoin />
                    </SimpleLayout>
                </ProtectedRoute>
              } 
            />
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
              path="/qr-join/:eventId" 
              element={
                <ProtectedRoute>
                  <SimpleLayout>
                    <QRJoin />
                  </SimpleLayout>
                </ProtectedRoute>
              } 
            />
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
              path="/certificates" 
              element={
                <ProtectedRoute>
                  <MainLayout title="Certificates">
                    <Certificate />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route 
              path="/certificates/:eventId" 
              element={
                <ProtectedRoute>
                  <MainLayout title="Event Certificates">
                    <Certificate />
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
              path="/profile" 
              element={
                <ProtectedRoute>
                  <MainLayout title="Profile">
                    <Setting />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route 
              path="/excuse-letters" 
              element={
                <ProtectedRoute>
                  <MainLayout title="Excuse Letters">
                    <ExcuseLetters />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
          </Routes>
        </Router>
      </UserProvider>
    </ThemeProvider>
  );
}

export default App;
