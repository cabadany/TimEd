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
  Snackbar,
  Card,
  Avatar,
  Grid,
  Divider,
  Zoom,
  Fade,
  Grow,
  Modal,
  Backdrop,
  Drawer
} from '@mui/material';
import { ChevronRight, Lock, LockOpen, School, PersonOutline, Work, LinkedIn, GitHub, Email, Language, Close } from '@mui/icons-material';
import './Setting.css';
import ProfilePicture from '../components/ProfilePicture';
import axios from 'axios';
import { useUser } from '../contexts/UserContext';
import { useTheme } from '../contexts/ThemeContext';

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
  const [notificationEnabled, setNotificationEnabled] = useState(true);
  const [loading, setLoading] = useState(true);
  const [departmentData, setDepartmentData] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const { darkMode, toggleDarkMode } = useTheme();
  
  // Team member modal state
  const [selectedMember, setSelectedMember] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  
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

  // Handle team member click
  const handleMemberClick = (member) => {
    setSelectedMember(member);
    setIsModalOpen(true);
  };

  // Handle modal close
  const handleModalClose = () => {
    setIsModalOpen(false);
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
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="profile tabs">
          <Tab label="Edit Profile" sx={{ 
            textTransform: 'none', 
            fontWeight: 600,
            color: tabValue === 0 ? 'primary.main' : 'text.secondary',
            '&.Mui-selected': { color: 'primary.main' }
          }} />
          <Tab label="About Us" sx={{ 
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
                <Typography variant="body2" fontWeight="500" color={darkMode ? 'rgba(255, 255, 255, 0.7)' : 'text.secondary'} mb={1}>
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
                        '& input': {
                          color: darkMode ? 'rgba(255, 255, 255, 0.9)' : 'inherit',
                        },
                        '& fieldset': {
                          borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                        },
                        '&.Mui-disabled': {
                          '& input': {
                            WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                            color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                          },
                          '& fieldset': {
                            borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                          },
                          backgroundColor: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)',
                        },
                      },
                      '& .MuiInputBase-input.Mui-disabled': {
                        WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      },
                    }}
                  />
                  <Tooltip title={firstNameLocked ? "Unlock to edit" : "Lock field"}>
                    <IconButton 
                      onClick={() => toggleLock('firstName')}
                      size="small"
                      sx={{ 
                        ml: 1,
                        color: darkMode ? 'rgba(255, 255, 255, 0.7)' : 'inherit'
                      }}
                    >
                      {firstNameLocked ? <Lock fontSize="small" /> : <LockOpen fontSize="small" />}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color={darkMode ? 'rgba(255, 255, 255, 0.7)' : 'text.secondary'} mb={1}>
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
                        '& input': {
                          color: darkMode ? 'rgba(255, 255, 255, 0.9)' : 'inherit',
                        },
                        '& fieldset': {
                          borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                        },
                        '&.Mui-disabled': {
                          '& input': {
                            WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                            color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                          },
                          '& fieldset': {
                            borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                          },
                          backgroundColor: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)',
                        },
                      },
                      '& .MuiInputBase-input.Mui-disabled': {
                        WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      },
                    }}
                  />
                  <Tooltip title={lastNameLocked ? "Unlock to edit" : "Lock field"}>
                    <IconButton 
                      onClick={() => toggleLock('lastName')}
                      size="small"
                      sx={{ 
                        ml: 1,
                        color: darkMode ? 'rgba(255, 255, 255, 0.7)' : 'inherit'
                      }}
                    >
                      {lastNameLocked ? <Lock fontSize="small" /> : <LockOpen fontSize="small" />}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color={darkMode ? 'rgba(255, 255, 255, 0.7)' : 'text.secondary'} mb={1}>
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
                      bgcolor: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'action.disabledBackground',
                      '& input': {
                        WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      },
                      '& fieldset': {
                        borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                      },
                    },
                    '& .MuiInputBase-input.Mui-disabled': {
                      WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                    },
                  }}
                />
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color={darkMode ? 'rgba(255, 255, 255, 0.7)' : 'text.secondary'} mb={1}>
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
                        '& input': {
                          color: darkMode ? 'rgba(255, 255, 255, 0.9)' : 'inherit',
                        },
                        '& fieldset': {
                          borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                        },
                        '&.Mui-disabled': {
                          '& input': {
                            WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                            color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                          },
                          '& fieldset': {
                            borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                          },
                          backgroundColor: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)',
                        },
                      },
                      '& .MuiInputBase-input.Mui-disabled': {
                        WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      },
                    }}
                  />
                  <Tooltip title={schoolIdLocked ? "Unlock to edit" : "Lock field"}>
                    <IconButton 
                      onClick={() => toggleLock('schoolId')}
                      size="small"
                      sx={{ 
                        ml: 1,
                        color: darkMode ? 'rgba(255, 255, 255, 0.7)' : 'inherit'
                      }}
                    >
                      {schoolIdLocked ? <Lock fontSize="small" /> : <LockOpen fontSize="small" />}
                    </IconButton>
                  </Tooltip>
                </Box>
              </Box>
        
              
              <Box>
                <Typography variant="body2" fontWeight="500" color={darkMode ? 'rgba(255, 255, 255, 0.7)' : 'text.secondary'} mb={1}>
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
                      bgcolor: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'action.disabledBackground',
                      '& input': {
                        WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      },
                      '& fieldset': {
                        borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                      },
                    },
                    '& .MuiInputBase-input.Mui-disabled': {
                      WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                    },
                  }}
                />
              </Box>
              
              <Box>
                <Typography variant="body2" fontWeight="500" color={darkMode ? 'rgba(255, 255, 255, 0.7)' : 'text.secondary'} mb={1}>
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
                      bgcolor: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'action.disabledBackground',
                      '& input': {
                        WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      },
                      '& fieldset': {
                        borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                      },
                    },
                    '& .MuiInputBase-input.Mui-disabled': {
                      WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                      color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                    },
                  }}
                />
              </Box>
              
              {department && department.offeredPrograms && department.offeredPrograms.length > 0 && (
                <Box>
                  <Typography variant="body2" fontWeight="500" color={darkMode ? 'rgba(255, 255, 255, 0.7)' : 'text.secondary'} mb={1}>
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
                        bgcolor: darkMode ? 'rgba(255, 255, 255, 0.05)' : 'action.disabledBackground',
                        '& input': {
                          WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                          color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        },
                        '& textarea': {
                          WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                          color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        },
                        '& fieldset': {
                          borderColor: darkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                        },
                      },
                      '& .MuiInputBase-input.Mui-disabled': {
                        WebkitTextFillColor: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
                        color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
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
        <Box sx={{ maxWidth: '100%' }} className="about-us-section">
          <Fade in={tabValue === 1} timeout={800}>
            <Typography variant="h4" fontWeight="700" color="primary" mb={4} className="section-title about-title">
              About TimeED
          </Typography>
          </Fade>
          
          <Zoom in={tabValue === 1} style={{ transitionDelay: tabValue === 1 ? '300ms' : '0ms' }}>
            <Paper 
              sx={{ 
                p: 4, 
                mb: 5, 
                borderRadius: 3, 
                boxShadow: '0 8px 32px rgba(0,0,0,0.08)',
                background: 'linear-gradient(145deg, #ffffff 0%, #f9fafc 100%)',
                position: 'relative',
                overflow: 'hidden',
                '&::before': {
                  content: '""',
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: '5px',
                  background: 'linear-gradient(90deg, #304FFF 0%, #8C9EFF 100%)',
                }
              }}
              className="mission-paper"
            >
              <Typography variant="h5" fontWeight="600" color="text.primary" mb={3} className="pulse-animation">
                Our Mission
            </Typography>
              <Typography variant="body1" color="text.secondary" paragraph className="mission-statement" sx={{ fontSize: '1.1rem', lineHeight: 1.8 }}>
                TimeED is a comprehensive time management and educational platform designed to help educational institutions 
                streamline their operations. Our system provides tools for event management, attendance tracking, 
                certificate generation, and department organization to enhance productivity and efficiency in educational settings.
              </Typography>
              <Typography variant="body1" color="text.secondary" paragraph sx={{ fontSize: '1.05rem' }}>
                We are committed to creating intuitive, user-friendly solutions that address the unique challenges faced by schools, 
                colleges, and universities in managing their day-to-day activities.
              </Typography>
            </Paper>
          </Zoom>
          
          <Fade in={tabValue === 1} timeout={1000} style={{ transitionDelay: tabValue === 1 ? '600ms' : '0ms' }}>
            <Typography variant="h4" fontWeight="700" color="primary" mb={4} className="section-title team-title">
              Development Team
            </Typography>
          </Fade>
          
          <Box 
            sx={{ 
              mb: 5, 
              position: 'relative',
              background: '#0d0d0d',
              borderRadius: '16px',
              overflow: 'hidden',
              p: 0
            }}
            className="team-showcase"
          >
            <Box 
              sx={{ 
                position: 'absolute', 
                top: 0, 
                left: 0, 
                right: 0, 
                bottom: 0, 
                background: 'url(/wavy-pattern.svg), linear-gradient(to right, rgba(30,30,30,0.7), rgba(30,30,30,0.7))',
                opacity: 0.1,
                zIndex: 0
              }} 
              className="background-pattern"
            />
            
            <Box 
              sx={{ 
                display: 'flex',
                flexDirection: { xs: 'column', md: 'row' },
                alignItems: 'stretch',
                position: 'relative',
                zIndex: 1
              }}
            >
              {[
                 {
                  name: 'Cabana, Danisse',
                  firstName: 'Danisse',
                  lastName: 'Cabana',
                  role: 'Mobile Backend Developer',
                  image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F494828260_646748648390700_2831467056740054150_n.jpg?alt=media&token=d613d9b2-88ff-4c59-9c3a-2d5f1825f3da',
                  description: 'Backend developer focusing on mobile app integration, database connectivity, and secure API development for seamless mobile experiences.',
                  skills: ['Firebase', 'Spring Boot', 'RESTful APIs', 'Mobile Integration', 'Authentication', 'Database Management']
                },
                {
                  name: 'Tumungha, Alexa',
                  firstName: 'Alexa',
                  lastName: 'Tumungha',
                  role: 'Project Manager',
                  image: 'https://randomuser.me/api/portraits/women/68.jpg',
                  description: 'Organized and goal-driven manager with expertise in managing project timelines, coordinating teams, and ensuring successful project delivery.',
                  skills: ['Project Management', 'Scrum', 'Agile Methodologies', 'Team Leadership', 'Jira', 'Communication']
                },
                {
                  name: 'Navaroo, Mikhail James',
                  firstName: 'Mikhail James',
                  lastName: 'Navaroo',
                  role: 'Mobile Frontend Developer',
                  image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F495270598_1925613518207533_3638793039034373708_n.png?alt=media&token=d7aaf893-9208-4f51-b771-c9838034ffdc',
                  description: 'Frontend developer focused on building intuitive, responsive mobile applications with attention to detail and performance.',
                  skills: ['React Native', 'JavaScript', 'UI Components', 'Mobile Design Patterns', 'Expo', 'CSS-in-JS']
                },
                {
                  name: 'Largo, John Wayne',
                  firstName: 'John Wayne',
                  lastName: 'Largo',
                  role: 'Web Backend Developer',
                  image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F434168392_2189176254770061_5356900714852223221_n.jpg?alt=media&token=9410fa9a-fd06-40e7-bbb3-5e53aab0262d',
                  description: 'Backend specialist for web systems, focused on building scalable infrastructure and secure APIs with modern tech stack.',
                  skills: ['Spring Boot', 'Firebase', 'PostgreSQL', 'JWT Authentication', 'Docker', 'API Security']
                },
                {
                  name: 'Gemongala, Clark',
                  firstName: 'Clark',
                  lastName: 'Gemongala',
                  role: 'Web Frontend Developer',
                  image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F126269967.jfif?alt=media&token=0079cc0e-5fcc-41c6-b131-d2832f359eb1',
                  description: 'Frontend developer with strong attention to responsive design, user experience, and clean code implementation for web platforms.',
                  skills: ['React.js', 'HTML5', 'CSS3', 'Tailwind CSS', 'JavaScript', 'Version Control']
                }
              ].map((member, index) => (
                <Box 
                  key={index}
                  onClick={() => handleMemberClick(member)}
                  sx={{
                    flex: 1,
                    position: 'relative',
                    height: { xs: '60vw', sm: '50vw', md: '400px' },
                    maxHeight: { xs: '350px', sm: '400px', md: '500px' },
                    overflow: 'hidden',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    filter: 'grayscale(100%)',
                    '&:hover': {
                      filter: 'grayscale(0%)',
                      flex: { md: 1.2 },
                    },
                    '&:hover .member-info': {
                      opacity: 1,
                      transform: 'translateY(0)'
                    },
                    '&:before': {
                      content: '""',
                      position: 'absolute',
                      bottom: 0,
                      left: 0,
                      right: 0,
                      height: '70%',
                      background: 'linear-gradient(to top, rgba(0,0,0,0.8), rgba(0,0,0,0))',
                      zIndex: 1
                    },
                    '&:after': {
                      content: '""',
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      right: 0,
                      bottom: 0,
                      borderRight: '1px solid rgba(255,255,255,0.1)',
                      zIndex: 2,
                      display: { xs: 'none', md: 'block' }
                    },
                    '&:last-child:after': {
                      display: 'none'
                    }
                  }}
                  className="team-member-banner"
                >
                  <Box
                    component="img"
                    src={member.image}
                    alt={member.name}
                    sx={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover',
                      display: 'block'
                    }}
                  />
                  <Box
                    className="member-info"
                    sx={{
                      position: 'absolute',
                      bottom: 0,
                      left: 0,
                      width: '100%',
                      padding: '20px',
                      opacity: 0.8,
                      transform: 'translateY(10px)',
                      transition: 'all 0.3s ease',
                      zIndex: 2
                    }}
                  >
                    <Typography variant="h6" sx={{ color: 'white', fontWeight: 700, textShadow: '0 2px 4px rgba(0,0,0,0.5)' }}>
                      {member.firstName}
              </Typography>
                    <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.8)', textShadow: '0 2px 4px rgba(0,0,0,0.5)' }}>
                      {member.lastName}
                    </Typography>
                  </Box>
                  <Box
                    sx={{
                      position: 'absolute',
                      top: 0,
                      right: 0,
                      bottom: 0,
                      width: '30%',
                      background: 'linear-gradient(to left, rgba(196, 30, 58, 0.8), rgba(196, 30, 58, 0))',
                      zIndex: 0,
                      opacity: 0.5
                    }}
                    className="red-overlay"
                  />
                </Box>
              ))}
            </Box>
          </Box>
          
          <Fade in={tabValue === 1} timeout={1000} style={{ transitionDelay: tabValue === 1 ? '900ms' : '0ms' }}>
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                p: 4,
                mt: 3,
                mb: 4,
                borderRadius: 3,
                background: 'rgba(48, 79, 255, 0.03)',
                textAlign: 'center'
              }}
              className="contact-section"
            >
              <Typography variant="h5" fontWeight="600" color="primary.main" mb={2}>
                Want to connect with our team?
            </Typography>
              <Typography variant="body1" color="text.secondary" mb={3} sx={{ maxWidth: '800px' }}>
                We're always looking to improve TimeED. If you have suggestions, questions, or would like to learn more about our platform, please reach out!
              </Typography>
              <Button 
                variant="contained" 
                    color="primary"
                startIcon={<Email />}
                sx={{ 
                  borderRadius: '50px',
                  px: 4,
                  py: 1.2,
                  boxShadow: '0 6px 20px rgba(48, 79, 255, 0.3)',
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    transform: 'translateY(-3px)',
                    boxShadow: '0 8px 25px rgba(48, 79, 255, 0.4)',
                  }
                }}
              >
                Contact Us
              </Button>
          </Box>
          </Fade>
        </Box>
      </TabPanel>
        
      <TabPanel value={tabValue} index={2}>
        <Box sx={{ maxWidth: 600 }}>
          <Typography variant="h6" fontWeight="600" color="text.primary" mb={3}>
            Security
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
      
      {/* Member Detail Modal */}
      <Drawer
        anchor="right"
        open={isModalOpen}
        onClose={handleModalClose}
        transitionDuration={450}
        className="member-detail-drawer"
        PaperProps={{
          sx: {
            width: { xs: '100%', sm: '450px' },
            background: darkMode ? '#1a1a1a' : 'white',
            boxShadow: '0 0 30px rgba(0,0,0,0.2)',
          },
        }}
      >
        {selectedMember && (
          <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <Box
              sx={{
                position: 'relative',
                height: '220px',
                background: 'linear-gradient(145deg, rgba(48, 79, 255, 0.9) 0%, rgba(48, 79, 255, 0.7) 100%)',
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'flex-end',
                alignItems: 'center',
                p: 3
              }}
              className="drawer-header"
            >
              <IconButton
                onClick={handleModalClose}
                sx={{
                  position: 'absolute',
                  top: 16,
                  right: 16,
                  color: 'white',
                  bgcolor: 'rgba(0,0,0,0.2)',
                  '&:hover': {
                    bgcolor: 'rgba(0,0,0,0.3)',
                  }
                }}
              >
                <Close />
              </IconButton>
              
              <Avatar
                src={selectedMember.image}
                alt={selectedMember.name}
                sx={{
                  width: 120,
                  height: 120,
                  border: '4px solid white',
                  boxShadow: '0 4px 15px rgba(0,0,0,0.2)',
                  mb: 2
                }}
              />
              
              <Typography variant="h5" sx={{ color: 'white', fontWeight: 700 }}>
                {selectedMember.firstName} {selectedMember.lastName}
              </Typography>
            </Box>
            
            <Box sx={{ p: 3, flex: 1, overflow: 'auto' }}>
              <Typography variant="h6" color="primary" fontWeight={600} sx={{ mb: 3 }}>
                Team Member Details
              </Typography>
              
              <Paper sx={{ p: 2, mb: 3, borderRadius: 2, boxShadow: '0 2px 12px rgba(0,0,0,0.05)' }}>
                <Typography variant="body1" color="text.secondary" paragraph>
                  {selectedMember.description}
                </Typography>
              </Paper>
              
              <Box sx={{ mb: 3 }}>
                
                
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Work fontSize="small" sx={{ color: 'primary.main', mr: 1.5 }} />
                  <Typography variant="body1" color="text.secondary" fontWeight="500">
                    Role: {selectedMember.role}
                  </Typography>
                </Box>
              </Box>
              
              <Divider sx={{ my: 3 }} />
              
              <Typography variant="h6" color="primary" fontWeight={600} sx={{ mb: 2 }}>
                Connect
              </Typography>
              
              <Box sx={{ display: 'flex', gap: 2 }}>
                <Button 
                  variant="outlined" 
                  color="primary" 
                  startIcon={<GitHub />}
                  sx={{ flex: 1, borderRadius: '8px' }}
                >
                  GitHub
                </Button>
                <Button 
                  variant="outlined" 
                  color="primary" 
                  startIcon={<LinkedIn />}
                  sx={{ flex: 1, borderRadius: '8px' }}
                >
                  LinkedIn
                </Button>
                <Button 
                  variant="outlined" 
                  color="primary" 
                  startIcon={<Email />}
                  sx={{ flex: 1, borderRadius: '8px' }}
                >
                  Email
                </Button>
              </Box>
            </Box>
          </Box>
        )}
      </Drawer>
    </Box>
  );
}