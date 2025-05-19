import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  IconButton,
  Avatar,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Badge,
  CircularProgress,
  Divider,
  Fade,
  Paper,
  Tooltip
} from '@mui/material';
import { Notifications, Logout, Person, Settings, ExitToApp } from '@mui/icons-material';
import { useUser } from '../contexts/UserContext';
import { useTheme } from '../contexts/ThemeContext';
import NotificationSystem from './NotificationSystem';
import '../styles/appHeader.css';

const AppHeader = ({ title }) => {
  const navigate = useNavigate();
  const { profilePictureUrl, loading, user } = useUser();
  const { darkMode } = useTheme();
  const [avatarLoading, setAvatarLoading] = useState(true);
  
  // Set up avatar loading state
  useEffect(() => {
    if (profilePictureUrl) {
      const img = new Image();
      img.onload = () => {
        setAvatarLoading(false);
      };
      img.onerror = () => {
        // If error loading the image, stop showing loading state
        setAvatarLoading(false);
      };
      img.src = profilePictureUrl;
    } else {
      // If there's no profile picture URL, don't show loading state
      setAvatarLoading(false);
    }
  }, [profilePictureUrl]);
  
  // Avatar dropdown menu state
  const [avatarAnchorEl, setAvatarAnchorEl] = useState(null);
  const avatarMenuOpen = Boolean(avatarAnchorEl);
  
  // Handle avatar click
  const handleAvatarClick = (event) => {
    setAvatarAnchorEl(event.currentTarget);
  };
  
  // Handle avatar menu close
  const handleAvatarClose = () => {
    setAvatarAnchorEl(null);
  };
  
  // Handle logout
  const handleLogout = () => {
    // Remove authentication token and user role from localStorage or sessionStorage
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    
    // Dispatch auth-change event to clear UserContext
    window.dispatchEvent(new CustomEvent('auth-change', { detail: { userId: null } }));
    
    // Redirect to login page after logout
    navigate('/login');
    
    handleAvatarClose();
  };
  
  // Get user initials for avatar fallback
  const getUserInitials = () => {
    if (user && user.firstName && user.lastName) {
      return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`;
    }
    return 'U';
  };

  // Get user full name or username
  const getUserDisplayName = () => {
    if (user && user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    } else if (user && user.email) {
      return user.email.split('@')[0];
    }
    return 'User';
  };
  
  return (
    <Box 
      className="app-header" 
      sx={{ 
        py: 1.5, 
        px: 3,
        bgcolor: darkMode ? '#1e1e1e' : 'background.paper', 
        borderBottom: '1px solid',
        borderColor: darkMode ? '#333333' : 'divider',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        boxShadow: darkMode ? '0 2px 10px rgba(0,0,0,0.2)' : '0 2px 10px rgba(0,0,0,0.05)'
      }}
    >
      <Typography 
        variant="h5" 
        fontWeight="600" 
        color={darkMode ? '#f5f5f5' : 'text.primary'}
        className="app-header-title"
        sx={{ 
          position: 'relative',
          '&::after': {
            content: '""',
            position: 'absolute',
            bottom: -2,
            left: 0,
            width: '30%',
            height: 2,
            bgcolor: 'primary.main',
            transition: 'width 0.3s ease',
            opacity: 0.7,
            borderRadius: 1
          },
          '&:hover::after': {
            width: '100%'
          }
        }}
      >
        {title}
      </Typography>
      
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <NotificationSystem />
        
        <Tooltip title="Your profile" arrow>
          <Box sx={{ position: 'relative' }}>
            {loading || avatarLoading ? (
              <CircularProgress 
                size={38} 
                thickness={4}
                sx={{ 
                  color: 'primary.main',
                }}
              />
            ) : (
              <Avatar 
                onClick={handleAvatarClick}
                src={profilePictureUrl}
                className="header-avatar"
                sx={{ 
                  width: 38, 
                  height: 38,
                  bgcolor: 'primary.main',
                  color: 'white',
                  cursor: 'pointer',
                  border: '2px solid',
                  borderColor: 'primary.light',
                  transition: 'all 0.2s ease-in-out',
                  '&:hover': {
                    boxShadow: '0 0 0 2px rgba(25, 118, 210, 0.3)',
                    transform: 'scale(1.05)'
                  }
                }}
              >
                {!profilePictureUrl && getUserInitials()}
              </Avatar>
            )}
          </Box>
        </Tooltip>
        
        <Menu
          anchorEl={avatarAnchorEl}
          open={avatarMenuOpen}
          onClose={handleAvatarClose}
          TransitionComponent={Fade}
          TransitionProps={{ timeout: 200 }}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          className="user-menu"
          PaperProps={{
            elevation: 5,
            component: Paper,
            sx: { 
              width: 240,
              mt: 1.5,
              overflow: 'visible',
              bgcolor: darkMode ? '#1e1e1e' : 'background.paper',
              color: darkMode ? '#f5f5f5' : 'text.primary',
              borderRadius: '12px',
              boxShadow: darkMode 
                ? '0 8px 24px rgba(0,0,0,0.4)' 
                : '0 8px 24px rgba(0,0,0,0.1)',
              '&::before': {
                content: '""',
                display: 'block',
                position: 'absolute',
                top: -6,
                right: 14,
                width: 12,
                height: 12,
                bgcolor: darkMode ? '#1e1e1e' : 'background.paper',
                transform: 'rotate(45deg)',
                zIndex: 0,
                boxShadow: '-3px -3px 5px rgba(0,0,0,0.05)',
                className: 'menu-arrow'
              }
            }
          }}
        >
          <Box 
            sx={{ px: 2, py: 1.5, borderBottom: '1px solid', borderColor: darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' }}
            className="user-menu-header"
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.5 }}>
              <Avatar 
                src={profilePictureUrl}
                sx={{ 
                  width: 42, 
                  height: 42,
                  bgcolor: 'primary.main',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                }}
              >
                {!profilePictureUrl && getUserInitials()}
              </Avatar>
              <Box>
                <Typography variant="subtitle1" fontWeight="600" sx={{ lineHeight: 1.3 }}>
                  {getUserDisplayName()}
                </Typography>
                <Typography 
                  variant="body2" 
                  color="text.secondary" 
                  className="user-email"
                  sx={{ opacity: 0.8, fontSize: '0.8rem' }}
                >
                  {user && user.email}
                </Typography>
              </Box>
            </Box>
          </Box>
          
          <Box sx={{ py: 1 }}>
            <MenuItem 
              onClick={() => {
                navigate('/profile');
                handleAvatarClose();
              }}
              className="user-menu-item"
              sx={{
                mx: 1,
                borderRadius: '8px',
                my: 0.5,
                transition: 'all 0.2s',
                '&:hover': {
                  bgcolor: darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.04)',
                  transform: 'translateX(4px)'
                }
              }}
            >
              <ListItemIcon sx={{ minWidth: '38px' }}>
                <Person sx={{ 
                  color: darkMode ? 'primary.light' : 'primary.main',
                  fontSize: '1.3rem'
                }} />
              </ListItemIcon>
              <ListItemText 
                primary="Profile" 
                primaryTypographyProps={{
                  fontWeight: 500
                }}
              />
            </MenuItem>
            
            <MenuItem 
              onClick={handleLogout}
              className="user-menu-item logout-button"
              sx={{
                mx: 1,
                borderRadius: '8px',
                my: 0.5,
                transition: 'all 0.2s',
                '&:hover': {
                  bgcolor: darkMode ? 'rgba(244,67,54,0.15)' : 'rgba(244,67,54,0.08)',
                  transform: 'translateX(4px)'
                }
              }}
            >
              <ListItemIcon sx={{ minWidth: '38px' }}>
                <ExitToApp sx={{ 
                  color: 'error.main',
                  fontSize: '1.3rem'
                }} />
              </ListItemIcon>
              <ListItemText 
                primary="Logout" 
                primaryTypographyProps={{
                  fontWeight: 500,
                  color: 'error.main'
                }}
              />
            </MenuItem>
          </Box>
        </Menu>
      </Box>
    </Box>
  );
};

export default AppHeader; 