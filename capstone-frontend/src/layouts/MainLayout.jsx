import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Button,
  Tooltip,
  Collapse,
  Divider,
  Badge
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
  Email,
  Person
} from '@mui/icons-material';
import AppHeader from '../components/AppHeader';
import { useTheme } from '../contexts/ThemeContext';
import { useState, useEffect } from 'react';
import '../styles/sidebar.css';
import axios from 'axios';

const MainLayout = ({ children, title }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { darkMode } = useTheme();
  const [collapsed, setCollapsed] = useState(false);
  const [hoveredItem, setHoveredItem] = useState(null);
  
  // Add state for badge counts
  const [departmentCount, setDepartmentCount] = useState(0);
  const [accountCount, setAccountCount] = useState(0);
  const [excuseLetterCount, setExcuseLetterCount] = useState(0);
  
  // Fetch counts on component mount
  useEffect(() => {
    fetchDepartmentCount();
    fetchAccountCount();
    fetchExcuseLetterCount();
  }, []);
  
  // Fetch department count
  const fetchDepartmentCount = async () => {
    try {
      const response = await axios.get('https://timed-utd9.onrender.com/api/departments');
      if (response.data && Array.isArray(response.data)) {
        setDepartmentCount(response.data.length);
      }
    } catch (error) {
      console.error('Error fetching departments count:', error);
    }
  };
  
  // Fetch account count
  const fetchAccountCount = async () => {
    try {
      const response = await axios.get('https://timed-utd9.onrender.com/api/user/getAll', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (response.data && Array.isArray(response.data)) {
        setAccountCount(response.data.length);
      }
    } catch (error) {
      console.error('Error fetching accounts count:', error);
    }
  };
  
  // Fetch excuse letter count
  const fetchExcuseLetterCount = async () => {
    try {
      const response = await axios.get('https://timed-utd9.onrender.com/api/excuse-letters/getAll', {
        params: { page: 0, size: 1000 } // Get a large count to ensure we get all
      });
      if (response.data && response.data.content && Array.isArray(response.data.content)) {
        setExcuseLetterCount(response.data.content.length);
      } else if (response.data && response.data.totalElements) {
        setExcuseLetterCount(response.data.totalElements);
      }
    } catch (error) {
      console.error('Error fetching excuse letters count:', error);
    }
  };

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
    navigate('/profile');
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
    // Main menu group
    { 
      icon: <Home />, 
      text: 'DASHBOARD', 
      onClick: handleNavigateToDashboard, 
      path: '/dashboard',
      tooltip: 'Dashboard',
      badge: null,
      group: 'main'
    },
    { 
      icon: <Event />, 
      text: 'EVENT', 
      onClick: handleNavigateToEvent, 
      path: '/event',
      tooltip: 'Events',
      badge: null,
      group: 'main'
    },
    { 
      icon: <People />, 
      text: 'ACCOUNTS', 
      onClick: handleNavigateToAccounts, 
      path: '/accounts',
      tooltip: 'Accounts',
      badge: accountCount > 0 ? accountCount : null,
      group: 'main'
    },
    // Resources group
    { 
      icon: <WorkspacePremium />, 
      text: 'CERTIFICATES', 
      onClick: handleNavigateToCertificate, 
      path: '/certificates',
      tooltip: 'Certificates',
      badge: null,
      group: 'resources'
    },
    { 
      icon: <AccountTree />, 
      text: 'DEPARTMENTS', 
      onClick: handleNavigateToDepartment, 
      path: '/department',
      tooltip: 'Departments',
      badge: departmentCount > 0 ? departmentCount : null,
      group: 'resources'
    },
    {
      icon: <Email />,
      text: 'EXCUSE LETTERS',
      onClick: handleNavigateToExcuseLetters,
      path: '/excuse-letters',
      tooltip: 'Excuse Letters',
      badge: excuseLetterCount > 0 ? excuseLetterCount : null,
      group: 'resources'
    },
    // Profile group
    { 
      icon: <Person />, 
      text: 'PROFILE', 
      onClick: handleNavigateToSettings, 
      path: '/profile',
      tooltip: 'Profile',
      badge: null,
      group: 'settings'
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
          transition: 'padding 0.3s ease',
          position: 'relative'
        }}>
          <img 
            src="/timeed.png" 
            alt="TimeED Logo" 
            style={{ 
              height: collapsed ? 40 : 100,
              transition: 'height 0.3s ease'
            }} 
            className="logo-image"
          />
          
          {/* Sidebar toggle button */}
          <Box 
            className="sidebar-toggle"
            onClick={() => setCollapsed(!collapsed)}
            sx={{
              position: 'fixed',
              left: collapsed ? 70 : 240,
              top: 70,
              transform: 'translateX(-50%)',
              zIndex: 1300,
              width: 30, 
              height: 30,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              borderRadius: '50%',
              backgroundColor: 'primary.main',
              color: '#fff',
              boxShadow: '0 3px 5px rgba(0, 0, 0, 0.2)',
              transition: 'all 0.3s ease, left 0.3s ease',
              border: '2px solid white',
              '&:hover': {
                backgroundColor: 'primary.dark',
                transform: 'translateX(-50%) scale(1.1)',
                boxShadow: '0 4px 8px rgba(0, 0, 0, 0.3)'
              }
            }}
          >
            {collapsed ? 
              <ChevronRight sx={{ fontSize: 20 }} /> : 
              <ChevronLeft sx={{ fontSize: 20 }} />
            }
          </Box>
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
              PopperProps={{
                sx: {
                  // Fix for tooltip initial position
                  "& .MuiTooltip-tooltip": {
                    position: "relative",
                    // This ensures tooltip is properly positioned right away
                    animation: "none !important"
                  }
                }
              }}
            >
              <Button 
                startIcon={!collapsed && item.icon}
                onClick={item.onClick}
                onMouseEnter={() => setHoveredItem(index)}
                onMouseLeave={() => setHoveredItem(null)}
                className={`sidebar-menu-item ${isActive(item.path) ? 'sidebar-item-active' : ''} ${hoveredItem === index ? 'sidebar-item-hovered' : ''} group-${item.group}`}
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
                  transition: 'none',
                  position: 'relative',
                  overflow: 'hidden',
                  minWidth: 'unset',
                  '&:hover': {
                    backgroundColor: 'transparent', // Let CSS handle the hover effect
                    transform: 'none'
                  }
                }}
              >
                {collapsed ? (
                  // Only show icon when collapsed
                  <Box sx={{ display: 'flex', justifyContent: 'center', position: 'relative' }}>
                    {item.icon}
                    <Badge 
                      badgeContent={item.badge} 
                      color="error"
                      sx={{
                        position: 'absolute',
                        top: -8,
                        right: -8,
                        visibility: item.badge ? 'visible' : 'hidden',
                        '& .MuiBadge-badge': {
                          fontSize: '0.5rem',
                          height: 14,
                          minWidth: 14,
                          padding: '0 2px',
                          transform: 'scale(1) translate(25%, -25%)'
                        }
                      }}
                    />
                  </Box>
                ) : (
                  // Show text when expanded
                  <Box sx={{ display: 'flex', width: '100%', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>{item.text}</span>
                    <Badge 
                      badgeContent={item.badge} 
                      color="error"
                      sx={{
                        visibility: item.badge ? 'visible' : 'hidden',
                        '& .MuiBadge-badge': {
                          fontSize: '0.6rem',
                          height: 16,
                          minWidth: 16
                        }
                      }}
                    />
                  </Box>
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