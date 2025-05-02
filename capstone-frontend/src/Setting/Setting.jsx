import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  IconButton,
  InputBase,
  Paper,
  Avatar,
  Badge,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Switch,
  Tabs,
  Tab,
  FormControlLabel,
  TextField,
  Divider
} from '@mui/material';
import {
  Search,
  Notifications,
  AccountTree,
  FilterList,
  Home,
  Event,
  People,
  Settings,
  Edit,
  Logout,
  ChevronRight
} from '@mui/icons-material';
import './Setting.css';

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
  const navigate = useNavigate();
  const location = useLocation();
  
  // State variables
  const [tabValue, setTabValue] = useState(0);
  const [darkMode, setDarkMode] = useState(false);
  const [notificationEnabled, setNotificationEnabled] = useState(true);
  const [anchorEl, setAnchorEl] = useState(null);
  
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

  // Filter menu state
  const [filterAnchorEl, setFilterAnchorEl] = useState(null);
  const filterMenuOpen = Boolean(filterAnchorEl);
  const [activeFilter, setActiveFilter] = useState('');
  
  // Avatar menu
  const open = Boolean(anchorEl);
  
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
  
  // Navigation handlers
  const handleNavigateToEvent = () => {
    navigate('/event');
  };

  const handleNavigateToDashboard = () => {
    navigate('/dashboard');
  };
  
  const handleNavigateToSettings = () => {
    navigate('/settings');
  };
  const handleNavigateToAccounts = () => {
    navigate('/accounts');
  };
  const handleNavigateToDepartment = () => {
    navigate('/department');
  }
  // Avatar menu handlers
  const handleAvatarClick = (event) => {
    setAnchorEl(event.currentTarget);
  };
  
  const handleAvatarClose = () => {
    setAnchorEl(null);
  };
  
  
  const handleLogout = () => {
    // Remove authentication token and user role from localStorage or sessionStorage
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    console.log('Logging out...');
    
    // Redirect to login page after logout
    navigate('/login');
    
    handleAvatarClose();
  };
  
  // Filter menu handlers
  const handleFilterClick = (event) => {
    setFilterAnchorEl(event.currentTarget);
  };

  const handleFilterClose = () => {
    setFilterAnchorEl(null);
  };

  const handleFilterSelect = (filterType) => {
    setActiveFilter(filterType);
    handleFilterClose();
  };
  
  // Save profile changes
  const handleSaveProfile = () => {
    // Add save logic here
    console.log('Profile saved');
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh', width: '100%', overflow: 'hidden' }} className={darkMode ? 'dark-mode' : ''}>
      {/* Sidebar */}
      <Box sx={{ 
        width: 240, 
        bgcolor: 'background.paper', 
        borderRight: '1px solid',
        borderColor: 'divider',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0
      }} className="sidebar">
        <Box sx={{ p: 3, borderBottom: '1px solid', borderColor: 'divider', display: 'flex', justifyContent: 'center' }}>
            <img src="/timed 1.png" alt="TimeED Logo" style={{ height: 80 }} />
        </Box>
        <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
          <Button 
            startIcon={<Home />} 
            onClick={handleNavigateToDashboard}
            sx={{ 
              justifyContent: 'flex-start', 
              color: location.pathname === '/dashboard' ? 'primary.main' : 'text.secondary',
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
              color: location.pathname === '/event' ? 'primary.main' : 'text.secondary',
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
              color: 'text.secondary',
              fontWeight: 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            ACCOUNTS
          </Button>
          <Button
            startIcon={<AccountTree />}
            onClick={handleNavigateToDepartment}
            sx={{
              justifyContent: 'flex-start',
              color: location.pathname === '/department' ? '#0288d1' : '#64748B',
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
              color: location.pathname === '/settings' ? 'primary.main' : 'text.secondary',
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
        overflow: 'hidden'
      }}>
        {/* Top Bar */}
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
            Settings
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Paper
              elevation={0}
              sx={{ 
                p: '2px 4px', 
                display: 'flex', 
                alignItems: 'center', 
                width: 300, 
                bgcolor: 'action.hover',
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '4px'
              }}
            >
              <IconButton sx={{ p: '10px' }} aria-label="search">
                <Search sx={{ color: 'text.secondary' }} />
              </IconButton>
              <InputBase
                sx={{ ml: 1, flex: 1, fontSize: 14 }}
                placeholder="Search for something"
              />
            </Paper>
            <Button 
              variant="outlined" 
              startIcon={<FilterList />}
              size="small"
              onClick={handleFilterClick}
              sx={{
                borderColor: activeFilter ? 'primary.main' : 'divider',
                color: activeFilter ? 'primary.main' : 'text.secondary',
                textTransform: 'none',
                fontWeight: 500
              }}
            >
              {activeFilter || 'FILTER'}
            </Button>
            <IconButton>
              <Badge badgeContent="" color="error" variant="dot">
                <Notifications sx={{ color: 'text.secondary', fontSize: 20 }} />
              </Badge>
            </IconButton>
            <Avatar 
              onClick={handleAvatarClick}
              sx={{ 
                width: 36, 
                height: 36,
                bgcolor: 'action.selected',
                color: 'text.primary',
                cursor: 'pointer'
              }}
            >
              P
            </Avatar>
            <Menu
              anchorEl={anchorEl}
              open={open}
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
              <MenuItem onClick={handleLogout}>
                <ListItemIcon>
                  <Logout fontSize="small" sx={{ color: 'text.secondary' }} />
                </ListItemIcon>
                <ListItemText>Logout</ListItemText>
              </MenuItem>
            </Menu>
          </Box>
        </Box>

        {/* Settings Content */}
        <Box sx={{ 
          p: 3, 
          flex: 1, 
          overflow: 'auto', 
          bgcolor: 'background.default' 
        }}>
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
            <Box sx={{ position: 'relative' }}>
                <Avatar
                src="/profile-image.jpg"
                sx={{ width: 120, height: 120 }}
                />
                <input 
                type="file" 
                id="profile-image-upload" 
                className="hidden-file-input" 
                accept="image/*" 
                onChange={(e) => {
                    // Handle file upload logic here
                    console.log("File selected:", e.target.files[0]);
                    // You would typically upload the file to a server here
                }}
                />
                <IconButton 
                sx={{ 
                    position: 'absolute', 
                    bottom: 0, 
                    right: 0, 
                    bgcolor: 'primary.main', 
                    color: 'white',
                    '&:hover': { bgcolor: 'primary.dark' }
                }}
                onClick={() => document.getElementById('profile-image-upload').click()}
                >
                <Edit sx={{ fontSize: 18 }} />
                </IconButton>
            </Box>
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
      </Box>
    </Box>
  );
}