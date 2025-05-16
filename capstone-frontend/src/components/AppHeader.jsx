import { useState } from 'react';
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
  CircularProgress
} from '@mui/material';
import {
  Notifications,
  Logout
} from '@mui/icons-material';
import { useUser } from '../contexts/UserContext';
import NotificationSystem from './NotificationSystem';

const AppHeader = ({ title }) => {
  const navigate = useNavigate();
  const { profilePictureUrl, loading } = useUser();
  
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
    
    // Redirect to login page after logout
    navigate('/login');
    
    handleAvatarClose();
  };
  
  return (
    <Box sx={{ 
      py: 1.5, 
      px: 3,
      bgcolor: 'background.paper', 
      borderBottom: '1px solid',
      borderColor: 'divider',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center'
    }}>
      <Typography variant="h5" fontWeight="600" color="text.primary">
        {title}
      </Typography>
      
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <NotificationSystem />
        
        <Box sx={{ position: 'relative' }}>
          {loading ? (
            <CircularProgress 
              size={36} 
              thickness={4}
              sx={{ 
                color: 'primary.main',
              }}
            />
          ) : (
            <Avatar 
              onClick={handleAvatarClick}
              src={profilePictureUrl}
              sx={{ 
                width: 36, 
                height: 36,
                bgcolor: 'primary.light',
                color: 'primary.contrastText',
                cursor: 'pointer'
              }}
            >
              {!profilePictureUrl && 'U'}
            </Avatar>
          )}
        </Box>
        
        <Menu
          anchorEl={avatarAnchorEl}
          open={avatarMenuOpen}
          onClose={handleAvatarClose}
          PaperProps={{
            elevation: 3,
            sx: { 
              width: 180,
              mt: 1,
              '& .MuiMenuItem-root': {
                fontSize: 14,
                py: 1
              }
            }
          }}
        >
          <MenuItem onClick={() => {
            navigate('/settings');
            handleAvatarClose();
          }}>
            <ListItemIcon>
              {loading ? (
                <CircularProgress size={24} thickness={4} />
              ) : (
                <Avatar 
                  src={profilePictureUrl}
                  sx={{ width: 24, height: 24, mr: 1 }}
                >
                  {!profilePictureUrl && 'U'}
                </Avatar>
              )}
            </ListItemIcon>
            <ListItemText>Profile</ListItemText>
          </MenuItem>
          <MenuItem onClick={handleLogout}>
            <ListItemText>Logout</ListItemText>
          </MenuItem>
        </Menu>
      </Box>
    </Box>
  );
};

export default AppHeader; 