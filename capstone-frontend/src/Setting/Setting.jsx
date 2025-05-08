import { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
  Switch,
  Tabs,
  Tab,
  FormControlLabel,
  TextField,
  Paper
} from '@mui/material';
import { ChevronRight } from '@mui/icons-material';
import './Setting.css';
import ProfilePicture from '../components/ProfilePicture';
import axios from 'axios';
import { useUser } from '../contexts/UserContext';

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

export default function SettingPage() {
  // State variables
  const [tabValue, setTabValue] = useState(0);
  const [darkMode, setDarkMode] = useState(false);
  const [notificationEnabled, setNotificationEnabled] = useState(true);
  
  // Form fields
  const [name, setName] = useState('Charlene Reed');
  const [username, setUsername] = useState('Charlene Reed');
  const [email, setEmail] = useState('charlenereed@gmail.com');
  const [password, setPassword] = useState('••••••••••');
  const [dateOfBirth, setDateOfBirth] = useState('25 January 1990');
  const [presentAddress, setPresentAddress] = useState('San Jose, California, USA');
  const [permanentAddress, setPermanentAddress] = useState('San Jose, California, USA');
  const [city, setCity] = useState('San Jose');
  const [postalCode, setPostalCode] = useState('45962');
  const [country, setCountry] = useState('USA');
  
  // Use the user context instead of local state
  const { profilePictureUrl, updateProfilePicture } = useUser();
  const [userId] = useState(localStorage.getItem('userId'));
  
  useEffect(() => {
    // Check if dark mode is stored in localStorage
    const storedDarkMode = localStorage.getItem('darkMode') === 'true';
    setDarkMode(storedDarkMode);
    
    // Apply dark mode to body if enabled
    if (storedDarkMode) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
  }, []);
  
  // Update handleProfilePictureChange to use the context
  const handleProfilePictureChange = (newUrl) => {
    updateProfilePicture(newUrl);
    
    // Update avatar in header as well
    const headerAvatar = document.querySelector('.MuiAvatar-root');
    if (headerAvatar) {
      headerAvatar.style.backgroundImage = `url(${newUrl})`;
      headerAvatar.style.backgroundSize = 'cover';
      headerAvatar.innerHTML = '';
    }
  };
  
  // Handle dark mode toggle
  const handleDarkModeToggle = () => {
    const newDarkMode = !darkMode;
    setDarkMode(newDarkMode);
    localStorage.setItem('darkMode', newDarkMode);
    
    if (newDarkMode) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
  };
  
  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };
  
  // Handle notification toggle
  const handleNotificationToggle = () => {
    setNotificationEnabled(!notificationEnabled);
  };
  
  // Save profile changes
  const handleSaveProfile = async () => {
    // Get user ID from local storage
    const userId = localStorage.getItem('userId');
    if (!userId) {
      console.error('User ID not found');
      return;
    }
    
    try {
      // Prepare user data
      const userData = {
        firstName: name.split(' ')[0],
        lastName: name.split(' ').slice(1).join(' '),
        username: username,
        email: email,
        // Include profile picture URL if available
        ...(profilePictureUrl && { profilePictureUrl }),
        dateOfBirth: dateOfBirth,
        presentAddress: presentAddress,
        permanentAddress: permanentAddress,
        city: city,
        postalCode: postalCode,
        country: country
      };
      
      // Update user profile in the database
      const response = await axios.put(`http://localhost:8080/api/user/update/${userId}`, userData);
      
      if (response.status === 200) {
        // Show success message or notification
        alert('Profile updated successfully');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      alert('Failed to update profile');
    }
  };

  return (
    <Box sx={{ width: '100%' }} className={darkMode ? 'dark-mode' : ''}>
      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="settings tabs">
          <Tab label="Edit Profile" sx={{ 
            textTransform: 'none', 
            fontWeight: 600,
            color: tabValue === 0 ? 'primary.main' : 'text.secondary',
            '&.Mui-selected': { color: 'primary.main' }
          }} />
          <Tab label="Preferences" sx={{ 
            textTransform: 'none', 
            fontWeight: 600,
            color: tabValue === 1 ? 'primary.main' : 'text.secondary',
            '&.Mui-selected': { color: 'primary.main' }
          }} />
          <Tab label="Security" sx={{ 
            textTransform: 'none', 
            fontWeight: 600,
            color: tabValue === 2 ? 'primary.main' : 'text.secondary',
            '&.Mui-selected': { color: 'primary.main' }
          }} />
        </Tabs>
      </Box>
      
      {/* Tab Content */}
      <TabPanel value={tabValue} index={0}>
        <Box sx={{ display: 'flex', flexDirection: 'row', gap: 4 }}>
          {/* Profile Picture - Left Section */}
          <Box sx={{ width: 'auto' }}>
            <ProfilePicture 
              userId={userId}
              src={profilePictureUrl}
              size={120}
              onPictureChange={handleProfilePictureChange}
            />
          </Box>
          
          {/* Form Fields - Right Section */}
          <Box sx={{ flex: 1, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 3 }}>
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Your Name
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={name}
                onChange={(e) => setName(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                User Name
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Email
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Password
              </Typography>
              <TextField
                fullWidth
                type="password"
                variant="outlined"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Date of Birth
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
                InputProps={{
                  endAdornment: <ChevronRight sx={{ color: 'text.secondary', fontSize: 20 }} />
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Present Address
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={presentAddress}
                onChange={(e) => setPresentAddress(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Permanent Address
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={permanentAddress}
                onChange={(e) => setPermanentAddress(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                City
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Postal Code
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={postalCode}
                onChange={(e) => setPostalCode(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
            
            <Box>
              <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                Country
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={country}
                onChange={(e) => setCountry(e.target.value)}
                size="small"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: '4px',
                    fontSize: '14px',
                  },
                }}
              />
            </Box>
          </Box>
        </Box>
        
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
          <Button
            variant="contained"
            color="primary"
            onClick={handleSaveProfile}
            sx={{
              textTransform: 'none',
              fontWeight: 500,
              px: 4,
              bgcolor: '#304FFF',
              '&:hover': { bgcolor: '#2840CC' }
            }}
          >
            Save
          </Button>
        </Box>
      </TabPanel>
        
      <TabPanel value={tabValue} index={1}>
        <Box sx={{ maxWidth: 600 }}>
          <Typography variant="h6" fontWeight="600" color="text.primary" mb={3}>
            Application Preferences
          </Typography>
          
          <Box sx={{ mb: 4 }}>
            <Typography variant="body1" fontWeight="500" color="text.primary" mb={2}>
              Appearance
            </Typography>
            <Paper sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
              <FormControlLabel
                control={
                  <Switch 
                    checked={darkMode} 
                    onChange={handleDarkModeToggle}
                    color="primary"
                  />
                }
                label="Dark Mode"
                sx={{ '& .MuiTypography-root': { color: 'text.primary', fontWeight: 500 } }}
              />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1, ml: 4 }}>
                Turn on dark mode to reduce eye strain and save battery power.
              </Typography>
            </Paper>
          </Box>
          
          <Box sx={{ mb: 4 }}>
            <Typography variant="body1" fontWeight="500" color="text.primary" mb={2}>
              Notifications
            </Typography>
            <Paper sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
              <FormControlLabel
                control={
                  <Switch 
                    checked={notificationEnabled} 
                    onChange={handleNotificationToggle}
                    color="primary"
                  />
                }
                label="Enable Notifications"
                sx={{ '& .MuiTypography-root': { color: 'text.primary', fontWeight: 500 } }}
              />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1, ml: 4 }}>
                Receive notifications about event updates, new messages, and system alerts.
              </Typography>
            </Paper>
          </Box>
        </Box>
      </TabPanel>
        
      <TabPanel value={tabValue} index={2}>
        <Box sx={{ maxWidth: 600 }}>
          <Typography variant="h6" fontWeight="600" color="text.primary" mb={3}>
            Security Settings
          </Typography>
          
          <Box sx={{ mb: 4 }}>
            <Typography variant="body1" fontWeight="500" color="text.primary" mb={2}>
              Change Password
            </Typography>
            <Paper sx={{ p: 3, bgcolor: 'background.paper', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
              <Box sx={{ mb: 3 }}>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  Current Password
                </Typography>
                <TextField
                  fullWidth
                  type="password"
                  variant="outlined"
                  size="small"
                  placeholder="Enter your current password"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                    },
                  }}
                />
              </Box>
              
              <Box sx={{ mb: 3 }}>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  New Password
                </Typography>
                <TextField
                  fullWidth
                  type="password"
                  variant="outlined"
                  size="small"
                  placeholder="Enter new password"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                    },
                  }}
                />
              </Box>
              
              <Box sx={{ mb: 3 }}>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  Confirm New Password
                </Typography>
                <TextField
                  fullWidth
                  type="password"
                  variant="outlined"
                  size="small"
                  placeholder="Confirm new password"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                    },
                  }}
                />
              </Box>
              
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
                <Button
                  variant="contained"
                  color="primary"
                  sx={{
                    textTransform: 'none',
                    fontWeight: 500,
                  }}
                >
                  Update Password
                </Button>
              </Box>
            </Paper>
          </Box>
          
          <Box sx={{ mb: 4 }}>
            <Typography variant="body1" fontWeight="500" color="text.primary" mb={2}>
              Two-Factor Authentication
            </Typography>
            <Paper sx={{ p: 2, bgcolor: 'background.paper', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}>
              <FormControlLabel
                control={
                  <Switch 
                    color="primary"
                  />
                }
                label="Enable Two-Factor Authentication"
                sx={{ '& .MuiTypography-root': { color: 'text.primary', fontWeight: 500 } }}
              />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1, ml: 4 }}>
                Add an extra layer of security to your account by requiring a verification code in addition to your password.
              </Typography>
            </Paper>
          </Box>
        </Box>
      </TabPanel>
    </Box>
  );
}