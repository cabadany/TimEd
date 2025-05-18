import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Button,
  Tooltip,
  Collapse,
  Divider
} from '@mui/material';
import {
  Home,
  Event,
  WorkspacePremium,
  People,
  AccountTree,
  Settings,
  ChevronLeft,
  ChevronRight,
  Email
} from '@mui/icons-material';
import AppHeader from '../components/AppHeader';
import { useTheme } from '../contexts/ThemeContext';
import { useState, useEffect } from 'react';
import '../styles/sidebar.css';

const MainLayout = ({ children, title }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { darkMode } = useTheme();
  const [collapsed, setCollapsed] = useState(false);
  const [hoveredItem, setHoveredItem] = useState(null);

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
  
  const handleNavigateToExcuseLetters = () => {
    navigate('/excuse-letters');
  };

  // Function to check if a route is active
  const isActive = (path) => {
    return location.pathname === path;
  };

  // Menu items data for easy mapping
  const menuItems = [
    { 
      icon: <Home />, 
      text: 'DASHBOARD', 
      onClick: handleNavigateToDashboard, 
      path: '/dashboard',
      tooltip: 'Dashboard'
    },
    { 
      icon: <Event />, 
      text: 'EVENT', 
      onClick: handleNavigateToEvent, 
      path: '/event',
      tooltip: 'Events'
    },
    { 
      icon: <People />, 
      text: 'ACCOUNTS', 
      onClick: handleNavigateToAccounts, 
      path: '/accounts',
      tooltip: 'Accounts'
    },
    { 
      icon: <WorkspacePremium />, 
      text: 'CERTIFICATES', 
      onClick: handleNavigateToCertificate, 
      path: '/certificates',
      tooltip: 'Certificates'
    },
    { 
      icon: <AccountTree />, 
      text: 'DEPARTMENTS', 
      onClick: handleNavigateToDepartment, 
      path: '/department',
      tooltip: 'Departments'
    },
    {
      icon: <Email />,
      text: 'EXCUSE LETTERS',
      onClick: handleNavigateToExcuseLetters,
      path: '/excuse-letters',
      tooltip: 'Excuse Letters'
    },
    { 
      icon: <Settings />, 
      text: 'SETTING', 
      onClick: handleNavigateToSettings, 
      path: '/settings',
      tooltip: 'Settings'
    }
  ];

  return (
    <Box sx={{ 
      display: 'flex', 
      height: '100vh', 
      width: '100%', 
      overflow: 'hidden',
      bgcolor: darkMode ? 'background.default' : 'background.paper'
    }} className={darkMode ? 'dark-mode' : ''}>
      {/* Sidebar */}
      <Box 
        className={`sidebar-container ${collapsed ? 'sidebar-collapsed' : ''}`}
        sx={{ 
          width: collapsed ? 70 : 240, 
          bgcolor: darkMode ? '#1e1e1e' : 'background.paper', 
          borderRight: '1px solid',
          borderColor: darkMode ? '#333333' : 'divider',
          display: 'flex',
          flexDirection: 'column',
          flexShrink: 0,
          transition: 'width 0.3s ease',
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        {/* Logo Section */}
        <Box sx={{ 
          p: collapsed ? 1 : 3, 
          borderBottom: '1px solid', 
          borderColor: darkMode ? '#333333' : 'divider', 
          display: 'flex', 
          justifyContent: 'center',
          alignItems: 'center',
          transition: 'padding 0.3s ease'
        }}>
          <img 
            src="/timed 1.png" 
            alt="TimeED Logo" 
            style={{ 
              height: collapsed ? 40 : 80,
              transition: 'height 0.3s ease'
            }} 
          />
        </Box>

        {/* Sidebar toggle button */}
        <Box 
          className="sidebar-toggle"
          onClick={() => setCollapsed(!collapsed)}
          sx={{
            position: 'absolute',
            top: '100px',
            right: 0,
            zIndex: 10,
            width: 22,
            height: 22,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            borderRadius: '50% 0 0 50%',
            backgroundColor: darkMode ? '#333' : '#f0f0f0',
            border: '1px solid',
            borderColor: darkMode ? '#444' : '#ddd',
            borderRight: 'none',
            color: darkMode ? '#aaa' : '#666',
            transition: 'transform 0.3s ease',
            '&:hover': {
              backgroundColor: darkMode ? '#444' : '#e0e0e0',
              color: darkMode ? '#fff' : '#333'
            }
          }}
        >
          {collapsed ? <ChevronRight sx={{ fontSize: 18 }} /> : <ChevronLeft sx={{ fontSize: 18 }} />}
        </Box>

        {/* Menu Items */}
        <Box 
          className="sidebar-menu"
          sx={{ 
            p: collapsed ? 1 : 2, 
            display: 'flex', 
            flexDirection: 'column', 
            gap: collapsed ? 3 : 1,
            transition: 'padding 0.3s ease',
            flex: 1,
            overflowY: 'auto',
            '&::-webkit-scrollbar': {
              width: '4px',
            },
            '&::-webkit-scrollbar-track': {
              background: 'transparent',
            },
            '&::-webkit-scrollbar-thumb': {
              background: darkMode ? '#555' : '#ccc',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: darkMode ? '#777' : '#aaa',
            }
          }}
        >
          {menuItems.map((item, index) => (
            <Tooltip 
              key={item.path} 
              title={collapsed ? item.tooltip : ""}
              placement="right"
              arrow
            >
              <Button 
                startIcon={!collapsed && item.icon}
                onClick={item.onClick}
                onMouseEnter={() => setHoveredItem(index)}
                onMouseLeave={() => setHoveredItem(null)}
                className={`sidebar-menu-item ${isActive(item.path) ? 'sidebar-item-active' : ''} ${hoveredItem === index ? 'sidebar-item-hovered' : ''}`}
                sx={{ 
                  justifyContent: collapsed ? 'center' : 'flex-start', 
                  color: isActive(item.path) 
                    ? 'primary.main' 
                    : darkMode ? '#f5f5f5' : 'text.secondary',
                  fontWeight: isActive(item.path) ? 600 : 500,
                  py: 1.5,
                  px: collapsed ? 1 : 2,
                  textAlign: 'left',
                  borderRadius: '8px',
                  transition: 'all 0.2s ease',
                  position: 'relative',
                  overflow: 'hidden',
                  minWidth: 'unset',
                  '&:hover': {
                    backgroundColor: darkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.04)',
                    transform: 'translateY(-1px)'
                  },
                  '&::after': isActive(item.path) ? {
                    content: '""',
                    position: 'absolute',
                    left: 0,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    width: '4px',
                    height: '60%',
                    backgroundColor: 'primary.main',
                    borderRadius: '0 4px 4px 0'
                  } : {}
                }}
              >
                {collapsed ? (
                  // Only show icon when collapsed
                  <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                    {item.icon}
                  </Box>
                ) : (
                  // Show text when expanded
                  item.text
                )}
              </Button>
            </Tooltip>
          ))}
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