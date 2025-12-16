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
  Person,
  PersonAdd
} from '@mui/icons-material';
import AppHeader from '../components/AppHeader';
import { useTheme } from '../contexts/ThemeContext';
import { useState, useEffect } from 'react';
import '../styles/sidebar.css';
import axios from 'axios';
import { API_BASE_URL, getApiUrl, API_ENDPOINTS } from '../utils/api';

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
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_DEPARTMENTS));
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
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_ALL_USERS), {
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
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_ALL_EXCUSE_LETTERS), {
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

  const handleNavigateToAccountRequests = () => {
    navigate('/account-requests');
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
    //  badge: accountCount > 0 ? accountCount : null,
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
      text: 'DEPTS/ GRADE', 
      onClick: handleNavigateToDepartment, 
      path: '/department',
      tooltip: 'Departma',
    //  badge: departmentCount > 0 ? departmentCount : null,
      group: 'resources'
    },
    {
      icon: <PersonAdd />,
      text: 'ACCOUNT REQUESTS',
      onClick: handleNavigateToAccountRequests,
      path: '/account-requests',
      tooltip: 'Account Requests',
      badge: null,
      group: 'resources'
    },
    {
      icon: <Email />,
      text: 'EXCUSE LETTERS',
      onClick: handleNavigateToExcuseLetters,
      path: '/excuse-letters',
      tooltip: 'Excuse Letters',
      //badge: excuseLetterCount > 0 ? excuseLetterCount : null,
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
          width: collapsed ? 78 : 260, 
          background: darkMode 
            ? 'linear-gradient(180deg, #1a1a2e 0%, #16213e 100%)' 
            : 'linear-gradient(180deg, #ffffff 0%, #f8fafc 100%)', 
          borderRight: 'none',
          boxShadow: darkMode 
            ? '4px 0 24px rgba(0, 0, 0, 0.3)' 
            : '4px 0 24px rgba(0, 0, 0, 0.06)',
          display: 'flex',
          flexDirection: 'column',
          flexShrink: 0,
          transition: 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          position: 'relative',
          overflow: 'visible',
          zIndex: 100
        }}
      >
        {/* Logo Section */}
        <Box sx={{ 
          p: collapsed ? 1.5 : 2.5, 
          borderBottom: '1px solid', 
          borderColor: darkMode ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)', 
          display: 'flex', 
          justifyContent: 'center',
          alignItems: 'center',
          transition: 'padding 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          position: 'relative',
          minHeight: collapsed ? 70 : 120,
          background: darkMode 
            ? 'rgba(255,255,255,0.02)' 
            : 'rgba(25, 118, 210, 0.02)'
        }}>
          <img 
            src="/timeed.png" 
            alt="TimeED Logo" 
            style={{ 
              height: collapsed ? 45 : 90,
              transition: 'height 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
              filter: darkMode ? 'brightness(1.1)' : 'none'
            }} 
            className="logo-image"
          />
          
        </Box>
        
        {/* Sidebar toggle button - positioned on the edge */}
        <Box 
          className="sidebar-toggle"
          onClick={() => setCollapsed(!collapsed)}
          sx={{
            position: 'absolute',
            right: 0,
            top: 55,
            transform: 'translateX(50%)',
            zIndex: 1301,
            width: 26, 
            height: 26,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #1976d2 0%, #1565c0 100%)',
            color: '#fff',
            boxShadow: '0 2px 8px rgba(25, 118, 210, 0.4)',
            transition: 'all 0.2s ease',
            border: '2px solid',
            borderColor: darkMode ? '#121212' : '#f8fafc',
            '&:hover': {
              background: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)',
              transform: 'translateX(50%) scale(1.1)',
              boxShadow: '0 4px 12px rgba(25, 118, 210, 0.5)'
            }
          }}
        >
          {collapsed ? 
            <ChevronRight sx={{ fontSize: 16 }} /> : 
            <ChevronLeft sx={{ fontSize: 16 }} />
          }
        </Box>

        {/* Menu Items */}
        <Box 
          className="sidebar-menu"
          sx={{ 
            p: collapsed ? 1.5 : 2, 
            pt: 3,
            display: 'flex', 
            flexDirection: 'column', 
            gap: collapsed ? 1.5 : 0.75,
            transition: 'padding 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            flex: 1,
            overflowY: 'auto',
            overflowX: 'hidden',
            '&::-webkit-scrollbar': {
              width: '4px',
            },
            '&::-webkit-scrollbar-track': {
              background: 'transparent',
            },
            '&::-webkit-scrollbar-thumb': {
              background: darkMode ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.15)',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: darkMode ? 'rgba(255,255,255,0.25)' : 'rgba(0,0,0,0.25)',
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
                  "& .MuiTooltip-tooltip": {
                    position: "relative",
                    animation: "none !important",
                    backgroundColor: darkMode ? '#2d3748' : '#1976d2',
                    fontSize: '12px',
                    fontWeight: 500,
                    padding: '8px 12px',
                    borderRadius: '8px'
                  },
                  "& .MuiTooltip-arrow": {
                    color: darkMode ? '#2d3748' : '#1976d2'
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
                    ? '#1976d2' 
                    : darkMode ? 'rgba(255,255,255,0.75)' : '#64748b',
                  fontWeight: isActive(item.path) ? 600 : 500,
                  fontSize: '0.85rem',
                  letterSpacing: '0.02em',
                  py: 1.4,
                  px: collapsed ? 1.5 : 2,
                  textAlign: 'left',
                  borderRadius: '12px',
                  transition: 'all 0.2s ease',
                  position: 'relative',
                  overflow: 'hidden',
                  minWidth: 'unset',
                  background: isActive(item.path) 
                    ? darkMode 
                      ? 'linear-gradient(135deg, rgba(25, 118, 210, 0.2) 0%, rgba(25, 118, 210, 0.1) 100%)'
                      : 'linear-gradient(135deg, rgba(25, 118, 210, 0.12) 0%, rgba(25, 118, 210, 0.06) 100%)'
                    : 'transparent',
                  border: isActive(item.path) 
                    ? '1px solid' 
                    : '1px solid transparent',
                  borderColor: isActive(item.path) 
                    ? darkMode ? 'rgba(25, 118, 210, 0.3)' : 'rgba(25, 118, 210, 0.2)' 
                    : 'transparent',
                  '& .MuiButton-startIcon': {
                    marginRight: collapsed ? 0 : '12px',
                    color: isActive(item.path) 
                      ? '#1976d2' 
                      : darkMode ? 'rgba(255,255,255,0.6)' : '#94a3b8',
                    transition: 'color 0.2s ease'
                  },
                  '&:hover': {
                    backgroundColor: darkMode 
                      ? 'rgba(255,255,255,0.06)' 
                      : 'rgba(25, 118, 210, 0.06)',
                    color: darkMode ? '#ffffff' : '#1976d2',
                    '& .MuiButton-startIcon': {
                      color: '#1976d2'
                    }
                  }
                }}
              >
                {collapsed ? (
                  <Box sx={{ 
                    display: 'flex', 
                    justifyContent: 'center', 
                    position: 'relative',
                    color: isActive(item.path) ? '#1976d2' : 'inherit'
                  }}>
                    {item.icon}
                    <Badge 
                      badgeContent={item.badge} 
                      color="error"
                      sx={{
                        position: 'absolute',
                        top: -10,
                        right: -10,
                        visibility: item.badge ? 'visible' : 'hidden',
                        '& .MuiBadge-badge': {
                          fontSize: '0.65rem',
                          height: 16,
                          minWidth: 16,
                          padding: '0 4px',
                          fontWeight: 600,
                          boxShadow: '0 2px 8px rgba(244, 67, 54, 0.4)'
                        }
                      }}
                    />
                  </Box>
                ) : (
                  <Box sx={{ display: 'flex', width: '100%', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ 
                      textTransform: 'none',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis'
                    }}>{item.text}</span>
                    <Badge 
                      badgeContent={item.badge} 
                      color="error"
                      sx={{
                        visibility: item.badge ? 'visible' : 'hidden',
                        '& .MuiBadge-badge': {
                          fontSize: '0.65rem',
                          height: 18,
                          minWidth: 18,
                          fontWeight: 600,
                          boxShadow: '0 2px 8px rgba(244, 67, 54, 0.4)'
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