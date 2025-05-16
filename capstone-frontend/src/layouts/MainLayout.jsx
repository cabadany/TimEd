import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Button
} from '@mui/material';
import {
  Home,
  Event,
  WorkspacePremium,
  People,
  AccountTree,
  Settings
} from '@mui/icons-material';
import AppHeader from '../components/AppHeader';
import { useTheme } from '../contexts/ThemeContext';

const MainLayout = ({ children, title }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { darkMode } = useTheme();

  // Navigation handlers
  const handleNavigateToEvent = () => {
    navigate('/event');
  };

  const handleNavigateToDashboard = () => {
    navigate('/dashboard');
  };
  const handleNavigateToCertificate = () => {
    navigate('/certificates');
  };
  const handleNavigateToSettings = () => {
    navigate('/settings');
  };
  
  const handleNavigateToAccounts = () => {
    navigate('/accounts');
  };
  
  const handleNavigateToDepartment = () => {
    navigate('/department');
  };

  return (
    <Box sx={{ 
      display: 'flex', 
      height: '100vh', 
      width: '100%', 
      overflow: 'hidden',
      bgcolor: darkMode ? 'background.default' : 'background.paper'
    }} className={darkMode ? 'dark-mode' : ''}>
      {/* Sidebar */}
      <Box sx={{ 
        width: 240, 
        bgcolor: darkMode ? '#1e1e1e' : 'background.paper', 
        borderRight: '1px solid',
        borderColor: darkMode ? '#333333' : 'divider',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0
      }} className="sidebar">
        <Box sx={{ p: 3, borderBottom: '1px solid', borderColor: darkMode ? '#333333' : 'divider', display: 'flex', justifyContent: 'center' }}>
            <img src="/timed 1.png" alt="TimeED Logo" style={{ height: 80 }} />
        </Box>
        <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
          <Button 
            startIcon={<Home />} 
            onClick={handleNavigateToDashboard}
            sx={{ 
              justifyContent: 'flex-start', 
              color: location.pathname === '/dashboard' ? 'primary.main' : darkMode ? '#f5f5f5' : 'text.secondary',
              fontWeight: location.pathname === '/dashboard' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            DASHBOARD
          </Button>
          <Button 
            startIcon={<Event />} 
            onClick={handleNavigateToEvent}
            sx={{ 
              justifyContent: 'flex-start', 
              color: location.pathname === '/event' ? 'primary.main' : darkMode ? '#f5f5f5' : 'text.secondary',
              fontWeight: location.pathname === '/event' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            EVENT
          </Button>
          <Button 
            startIcon={<People />} 
            onClick={handleNavigateToAccounts}
            sx={{ 
              justifyContent: 'flex-start', 
              color: location.pathname === '/accounts' ? 'primary.main' : darkMode ? '#f5f5f5' : 'text.secondary',
              fontWeight: location.pathname === '/accounts' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            ACCOUNTS
          </Button>
          <Button
            startIcon={<WorkspacePremium/>}
            onClick={handleNavigateToCertificate}
            sx={{
              justifyContent: 'flex-start',
              color: location.pathname === '/certificates' ? 'primary.main' : darkMode ? '#f5f5f5' : 'text.secondary',
              fontWeight: location.pathname === '/certificates' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left',
            }}
          >
            CERTIFICATES
          </Button>
          <Button
            startIcon={<AccountTree />}
            onClick={handleNavigateToDepartment}
            sx={{
              justifyContent: 'flex-start',
              color: location.pathname === '/department' ? 'primary.main' : darkMode ? '#f5f5f5' : 'text.secondary',
              fontWeight: location.pathname === '/department' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            DEPARTMENTS
          </Button>
          <Button 
            startIcon={<Settings />} 
            onClick={handleNavigateToSettings}
            sx={{ 
              justifyContent: 'flex-start', 
              color: location.pathname === '/settings' ? 'primary.main' : darkMode ? '#f5f5f5' : 'text.secondary',
              fontWeight: location.pathname === '/settings' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            SETTING
          </Button>
        </Box>
      </Box>

      {/* Main Content */}
      <Box sx={{ 
        flex: 1, 
        display: 'flex', 
        flexDirection: 'column', 
        height: '100vh',
        overflow: 'hidden',
        bgcolor: darkMode ? '#121212' : 'background.default'
      }}>
        {/* Header */}
        <AppHeader 
          title={title || 'TimEd System'}
        />
        
        {/* Content Area */}
        <Box sx={{ 
          p: 3, 
          flex: 1, 
          overflow: 'auto', 
          bgcolor: darkMode ? '#121212' : 'background.default'
        }}>
          {children}
        </Box>
      </Box>
    </Box>
  );
};

export default MainLayout; 