import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './Dashboard/Dashboard';
import Event from './Event/Event';
import LoginPage from './Auth/LoginPage';
import Setting from './Setting/Setting';
import './App.css';
import ProtectedRoute from './ProtectedRoute';  // Import the ProtectedRoute

function App() {
  return (
    <Router>
      <Routes>
        {/* Public routes */}
        <Route path="/" element={<LoginPage />} />
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected routes (Only Admin can access) */}
        <Route 
          path="/dashboard" 
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route 
          path="/event" 
          element={
            <ProtectedRoute>
              <Event />
            </ProtectedRoute>
          }
        />
        <Route 
          path="/settings" 
          element={
            <ProtectedRoute>
              <Setting />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;
