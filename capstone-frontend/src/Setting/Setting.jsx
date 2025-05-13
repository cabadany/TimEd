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
  Paper,
  IconButton,
  Tooltip,
  CircularProgress,
  Alert,
  Snackbar
} from '@mui/material';
import { ChevronRight, Lock, LockOpen } from '@mui/icons-material';
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
  const [loading, setLoading] = useState(true);
  const [departmentData, setDepartmentData] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  
  // Form fields
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [schoolId, setSchoolId] = useState('');
  const [role, setRole] = useState('');
  const [department, setDepartment] = useState({});
  
  // Field lock states
  const [firstNameLocked, setFirstNameLocked] = useState(true);
  const [lastNameLocked, setLastNameLocked] = useState(true);
  const [schoolIdLocked, setSchoolIdLocked] = useState(true);
  
  // Use the user context
  const { profilePictureUrl, updateProfilePicture } = useUser();
  const [userId, setUserId] = useState('');
  
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
    
    // Fetch user data
    const fetchUserData = async () => {
      try {
        const storedUserId = localStorage.getItem("userId");
        console.log("User ID from localStorage:", storedUserId);
        
        if (storedUserId) {
          setUserId(storedUserId); // Set the userId state immediately
          
          const response = await axios.get(`http://localhost:8080/api/user/getUser/${storedUserId}`);
          console.log("Response Status:", response.status);
          console.log("Response Data:", response.data); // Log the fetched user data
          
          if (response.status === 200) {
            const userData = response.data;
            setFirstName(userData.firstName || '');
            setLastName(userData.lastName || '');
            setEmail(userData.email || '');
            setSchoolId(userData.schoolId || '');
            setRole(userData.role || '');
            
            // Log if departmentId exists
            if (userData.departmentId) {
              console.log("User has departmentId:", userData.departmentId);
            }
            
            // If departmentId exists, fetch department data from specific endpoint
            if (userData.departmentId) {
              try {
                console.log("Fetching department with ID:", userData.departmentId);
                const deptResponse = await axios.get(`http://localhost:8080/api/departments/${userData.departmentId}`);
                console.log("Department Response Status:", deptResponse.status);
                console.log("Department Response Data:", deptResponse.data);
                
                if (deptResponse.status === 200) {
                  // Ensure the departmentId is kept when setting the department object
                  const deptData = {
                    ...deptResponse.data,
                    departmentId: userData.departmentId // Ensure departmentId is preserved
                  };
                  setDepartment(deptData);
                  console.log("Department data set with ID preserved:", deptData);
                }
              } catch (deptError) {
                console.error('Error fetching department by ID:', deptError);
                // Fallback to fetching all departments if specific endpoint fails
                try {
                  const allDeptsResponse = await axios.get(`http://localhost:8080/api/departments`);
                  if (allDeptsResponse.status === 200) {
                    const departments = allDeptsResponse.data;
                    const userDept = departments.find(dept => dept.departmentId === userData.departmentId);
                    if (userDept) {
                      setDepartment(userDept);
                      console.log("Department found from all departments:", userDept);
                    } else {
                      console.log("No department found with ID:", userData.departmentId);
                      // If no department found in the list, create a minimal department object with just the ID
                      setDepartment({ departmentId: userData.departmentId, name: 'Unknown Department' });
                    }
                  }
                } catch (allDeptsError) {
                  console.error('Error fetching all departments:', allDeptsError);
                  // Even if both requests fail, still preserve the departmentId
                  setDepartment({ departmentId: userData.departmentId, name: 'Department Data Unavailable' });
                }
              }
            } else if (userData.department) {
              // If department is already included in user data
              // Ensure it has the departmentId property
              const deptWithId = {
                ...userData.department,
                departmentId: userData.department.id || userData.department.departmentId
              };
              setDepartment(deptWithId);
              console.log("Department from user data with ID:", deptWithId);
            } else {
              console.log("No department or departmentId found in user data.");
            }
          }
        } else {
          console.log("No user ID found in localStorage.");
          setErrorMessage('User ID not found. Please log in again.');
        }
      } catch (error) {
        console.error('Error fetching user data:', error);
        setErrorMessage('Failed to load user data: ' + (error.response?.data?.message || error.message));
      } finally {
        setLoading(false);
      }
    };
    
    fetchUserData();
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
  
  // Toggle lock for fields
  const toggleLock = (field) => {
    switch (field) {
      case 'firstName':
        setFirstNameLocked(!firstNameLocked);
        break;
      case 'lastName':
        setLastNameLocked(!lastNameLocked);
        break;
      case 'schoolId':
        setSchoolIdLocked(!schoolIdLocked);
        break;
      default:
        break;
    }
  };
  
  // Save profile changes
  // Save profile changes
const handleSaveProfile = async () => {
  const userIdToUse = userId || localStorage.getItem("userId");
  
  if (!userIdToUse) {
      console.error('User  ID not found');
      setErrorMessage('User  ID not found. Please try logging in again.');
      return;
  }
  
  try {
      setLoading(true);
      
      // Prepare user data with proper structure
      const userData = {
        userId: userIdToUse,
          firstName,
          lastName,
          email,
          schoolId,
          role,
          department: {
              departmentId: department ? department.departmentId : null // Ensure department is structured correctly
          }
      };
      
      console.log("User  data being sent:", userData);
      
      // Update user profile in the database
      const response = await axios.put(`http://localhost:8080/api/user/updateUser/${userIdToUse}`, userData);
      
      if (response.status === 200) {
          setSuccessMessage('Profile updated successfully');
          // Reset lock states
          setFirstNameLocked(true);
          setLastNameLocked(true);
          setSchoolIdLocked(true);
      }
  } catch (error) {
      console.error('Error updating profile:', error);
      setErrorMessage('Failed to update profile: ' + (error.response?.data?.message || error.message));
  } finally {
      setLoading(false);
  }
};



  return (
    <Box sx={{ width: '100%' }} className={darkMode ? 'dark-mode' : ''}>
      {/* Notification Snackbars */}
      <Snackbar 
        open={!!successMessage} 
        autoHideDuration={6000} 
        onClose={() => setSuccessMessage('')}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert onClose={() => setSuccessMessage('')} severity="success" sx={{ width: '100%' }}>
          {successMessage}
        </Alert>
      </Snackbar>
      
      <Snackbar 
        open={!!errorMessage} 
        autoHideDuration={6000} 
        onClose={() => setErrorMessage('')}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert onClose={() => setErrorMessage('')} severity="error" sx={{ width: '100%' }}>
          {errorMessage}
        </Alert>
      </Snackbar>
      
      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }} className={darkMode ? 'dark-mode' : ''}>
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
        {/*  <Tab label="Security" sx={{ 
            textTransform: 'none', 
            fontWeight: 600,
            color: tabValue === 2 ? 'primary.main' : 'text.secondary',
            '&.Mui-selected': { color: 'primary.main' }
          }} />*/}
        </Tabs>
      </Box>
      
      {/* Tab Content */}
      <TabPanel value={tabValue} index={0}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '300px' }}>
            <CircularProgress />
          </Box>
        ) : (
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
                  First Name
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    disabled={firstNameLocked}
                    size="small"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                      },
                    }}
                  />
                  <Tooltip title={firstNameLocked ? "Unlock to edit" : "Lock field"}>
                    <IconButton 
                      onClick={() => toggleLock('firstName')}
                      size="small"
                      sx={{ ml: 1 }}
                    >
                      {firstNameLocked ? <Lock fontSize="small" /> : <LockOpen fontSize="small" />}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  Last Name
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    disabled={lastNameLocked}
                    size="small"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                      },
                    }}
                  />
                  <Tooltip title={lastNameLocked ? "Unlock to edit" : "Lock field"}>
                    <IconButton 
                      onClick={() => toggleLock('lastName')}
                      size="small"
                      sx={{ ml: 1 }}
                    >
                      {lastNameLocked ? <Lock fontSize="small" /> : <LockOpen fontSize="small" />}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  Email (Read-only)
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  value={email}
                  disabled={true}
                  size="small"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      bgcolor: 'action.disabledBackground',
                    },
                  }}
                />
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  School ID
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={schoolId}
                    onChange={(e) => setSchoolId(e.target.value)}
                    disabled={schoolIdLocked}
                    size="small"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                      },
                    }}
                  />
                  <Tooltip title={schoolIdLocked ? "Unlock to edit" : "Lock field"}>
                    <IconButton 
                      onClick={() => toggleLock('schoolId')}
                      size="small"
                      sx={{ ml: 1 }}
                    >
                      {schoolIdLocked ? <Lock fontSize="small" /> : <LockOpen fontSize="small" />}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>
        
              
              <Box>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  Role (Read-only)
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  value={role}
                  disabled={true}
                  size="small"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      bgcolor: 'action.disabledBackground',
                    },
                  }}
                />
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                  Department (Read-only)
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  value={department ? 
                    `${department.name || 'N/A'} ${department.abbreviation ? `(${department.abbreviation})` : ''}` : 
                    'Department information not available'}
                  disabled={true}
                  size="small"
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      bgcolor: 'action.disabledBackground',
                    },
                  }}
                />
              </Box>
              
              {department && department.offeredPrograms && department.offeredPrograms.length > 0 && (
                <Box>
                  <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                    Offered Programs (Read-only)
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={department.offeredPrograms.join(', ')}
                    disabled={true}
                    size="small"
                    multiline
                    maxRows={3}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        bgcolor: 'action.disabledBackground',
                      },
                    }}
                  />
                </Box>
              )}
              
              {/* Display additional department information if available */}
              {department && department.description && (
                <Box gridColumn="span 2">
                  <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                    Department Description (Read-only)
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={department.description}
                    disabled={true}
                    size="small"
                    multiline
                    maxRows={4}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        bgcolor: 'action.disabledBackground',
                      },
                    }}
                  />
                </Box>
              )}
              
              {department && department.location && (
                <Box>
                  <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                    Department Location (Read-only)
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={department.location}
                    disabled={true}
                    size="small"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        bgcolor: 'action.disabledBackground',
                      },
                    }}
                  />
                </Box>
              )}
              
              {department && department.website && (
                <Box>
                  <Typography variant="body2" fontWeight="500" color="text.secondary" mb={1}>
                    Department Website (Read-only)
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={department.website}
                    disabled={true}
                    size="small"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        bgcolor: 'action.disabledBackground',
                      },
                    }}
                  />
                </Box>
              )}
            </Box>
          </Box>
        )}
        
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
          <Button
            variant="contained"
            color="primary"
            onClick={handleSaveProfile}
            disabled={loading || (firstNameLocked && lastNameLocked && schoolIdLocked)}
            sx={{
              textTransform: 'none',
              fontWeight: 500,
              px: 4,
              bgcolor: '#304FFF',
              '&:hover': { bgcolor: '#2840CC' },
              '&.Mui-disabled': {
                bgcolor: 'rgba(48, 79, 255, 0.5)',
              }
            }}
          >
            {loading ? 'Saving...' : 'Save'}
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
        </Box>
      </TabPanel>
    </Box>
  );
}