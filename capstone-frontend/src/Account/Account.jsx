// Add imports for the new event attendance feature
import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Typography, Button, IconButton, InputBase, Paper, TextField, Menu, MenuItem, ListItemIcon, ListItemText, 
  Avatar, Badge, Modal, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Snackbar, Alert, 
  CircularProgress, Select, FormControl, Chip, Divider, List, ListItem
} from '@mui/material';
import {
  Search, AccountTree, Settings, Notifications, FilterList, Home, Event, People, CalendarToday,
  Group, Add, Close, Logout, Edit, Delete, VisibilityOutlined, EventAvailable, CheckCircleOutline,Email
} from '@mui/icons-material';
import axios from 'axios';
import NotificationSystem from '../components/NotificationSystem';

// Base API URL
const API_BASE_URL = 'http://localhost:8080/api';

// Date formatting helper function
const formatDate = (dateObj) => {
  if (!dateObj) return 'N/A';
  
  // Handle Firestore timestamp format
  if (dateObj.seconds) {
    const date = new Date(dateObj.seconds * 1000);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
  
  // Handle ISO string date
  try {
    const date = new Date(dateObj);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch (e) {
    return 'Invalid date';
  }
};

// Time formatting helper function
const formatTime = (timeString) => {
  if (!timeString) return 'N/A';
  
  try {
    const date = new Date(timeString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch (e) {
    return 'Invalid time';
  }
};

export default function AccountPage() {
  const navigate = useNavigate();
  const location = useLocation();

  // Professors state
  const [professors, setProfessors] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Departments state
  const [departments, setDepartments] = useState([]);
  const [departmentsLoading, setDepartmentsLoading] = useState(true);

  // Search state
  const [searchTerm, setSearchTerm] = useState("");

  // Add professor modal state
  const [showAddModal, setShowAddModal] = useState(false);
  const [newProfessor, setNewProfessor] = useState({
    firstName: "",
    lastName: "",
    email: "",
    departmentId: "",
    schoolId: "",
    password: "",
    role: "USER"
  });

  // Edit professor modal state
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingProfessor, setEditingProfessor] = useState({
    userId:"",
    firstName: "",
    lastName: "",
    email: "",
    departmentId: "",
    schoolId: "",
    role: "USER",
    password:"",
  });

  // View professor modal state
  const [showViewModal, setShowViewModal] = useState(false);
  const [viewingProfessor, setViewingProfessor] = useState(null);

  // Attended events modal state
  const [showAttendedEventsModal, setShowAttendedEventsModal] = useState(false);
  const [attendedEvents, setAttendedEvents] = useState([]);
  const [loadingEvents, setLoadingEvents] = useState(false);

  // Filter menu state
  const [filterAnchorEl, setFilterAnchorEl] = useState(null);
  const filterMenuOpen = Boolean(filterAnchorEl);
  const [activeFilter, setActiveFilter] = useState('');

  // Avatar dropdown menu state
  const [avatarAnchorEl, setAvatarAnchorEl] = useState(null);
  const avatarMenuOpen = Boolean(avatarAnchorEl);

  // Snackbar notification state
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'info'
  });

  // Fetch professors and departments on component mount
  useEffect(() => {
    fetchProfessors();
    fetchDepartments();
  }, []);

  // Fetch professors from API
  const fetchProfessors = async () => {
    setIsLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/user/getAll`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      setProfessors(response.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching professors:', err);
      setError('Failed to load professors. Please try again later.');
      showSnackbar('Failed to load professors', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // Fetch departments from API
  const fetchDepartments = async () => {
    setDepartmentsLoading(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/departments`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      setDepartments(response.data);
    } catch (err) {
      console.error('Error fetching departments:', err);
      showSnackbar('Failed to load departments', 'error');
    } finally {
      setDepartmentsLoading(false);
    }
  };

  // Fetch user's attended events
  const fetchUserAttendedEvents = async (userId) => {
    setLoadingEvents(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/attendance/user/${userId}/attended-events`);
      setAttendedEvents(response.data);
    } catch (err) {
      console.error('Error fetching attended events:', err);
      if (err.response?.status === 404) {
        setAttendedEvents([]);
      } else {
        showSnackbar('Failed to load attended events', 'error');
      }
    } finally {
      setLoadingEvents(false);
    }
  };

  // View attended events handler
  const handleViewAttendedEvents = (professor) => {
    setViewingProfessor(professor);
    fetchUserAttendedEvents(professor.userId);
    setShowAttendedEventsModal(true);
  };

  // Helper function to get department name by ID
  const getDepartmentName = (departmentId) => {
    if (!departmentId) return 'N/A';
    const department = departments.find(dept => dept.departmentId === departmentId);
    return department ? department.name : 'Unknown Department';
  };

  // Helper function to get department abbreviation by ID
  const getDepartmentAbbreviation = (departmentId) => {
    if (!departmentId) return 'N/A';
    const department = departments.find(dept => dept.departmentId === departmentId);
    return department ? department.abbreviation : 'Unknown';
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

  // Search handler
  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
  };

  // Avatar menu handlers
  const handleAvatarClick = (event) => {
    setAvatarAnchorEl(event.currentTarget);
  };

  const handleAvatarClose = () => {
    setAvatarAnchorEl(null);
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

  // Snackbar notification handler
  const showSnackbar = (message, severity = 'info') => {
    setSnackbar({
      open: true,
      message,
      severity
    });
  };

  const handleCloseSnackbar = () => {
    setSnackbar({
      ...snackbar,
      open: false
    });
  };

  // Professor management handlers
  const handleAddClick = () => {
    setShowAddModal(true);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setNewProfessor({ ...newProfessor, [name]: value });
  };

  const handleEditInputChange = (e) => {
    const { name, value } = e.target;
    setEditingProfessor({ ...editingProfessor, [name]: value });
  };

  const handleAddProfessor = async () => {
    try {
      // Create the proper nested structure for department
      const professorData = {
        ...newProfessor,
        department: {
          departmentId: newProfessor.departmentId
        }
      };
      
      const response = await axios.post(`${API_BASE_URL}/auth/register`, professorData, {
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      showSnackbar('Faculty added successfully', 'success');
      setNewProfessor({
        firstName: "",
        lastName: "",
        email: "",
        departmentId: "",
        schoolId: "",
        password: "",
        role: "USER"
      });
      setShowAddModal(false);
      fetchProfessors(); // Refresh the list
    } catch (err) {
      console.error('Error adding professor:', err);
      showSnackbar(`Failed to add professor: ${err.response?.data?.message || err.message}`, 'error');
    }
  };

  const handleEditClick = (professor) => {
    setEditingProfessor({
      userId: professor.userId,
      firstName: professor.firstName,
      lastName: professor.lastName,
      email: professor.email,
      departmentId: professor.department && professor.department.departmentId ? professor.department.departmentId : "",
      schoolId: professor.schoolId,
      role: professor.role || "USER",
      password: professor.password || "",
    });
    setShowEditModal(true);
  };

  const handleViewClick = (professor) => {
    setViewingProfessor(professor);
    setShowViewModal(true);
  };

  const handleUpdateProfessor = async () => {
    try {
      // Create a payload that matches the expected structure
      // Remove password from the payload if it's empty or null
      const { password, ...professorWithoutPassword } = editingProfessor;
      
      const updatePayload = {
        ...professorWithoutPassword,
        department: {
          departmentId: professorWithoutPassword.departmentId
        }
      };
      
      await axios.put(`${API_BASE_URL}/user/updateUser/${editingProfessor.userId}`, updatePayload, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      showSnackbar('Faculty updated successfully', 'success');
      setShowEditModal(false);
      fetchProfessors(); // Refresh the list
    } catch (err) {
      console.error('Error updating professor:', err);
      showSnackbar(`Failed to update professor: ${err.response?.data?.message || err.message}`, 'error');
    }
  };

  // Navigation handlers
  const handleNavigateToEvent = () => {
    navigate('/event');
  };
  const handleNavigateToDepartment = () => {
    navigate('/department');
  };
  const handleNavigateToDashboard = () => {
    navigate('/dashboard');
  };

  const handleNavigateToAccounts = () => {
    navigate('/accounts');
  };

  const handleNavigateToSettings = () => {
    navigate('/settings');
  };
  
  const handleDeleteProfessor = async (professorId) => {
    if (window.confirm('Are you sure you want to delete this professor?')) {
      try {
        await axios.delete(`${API_BASE_URL}/user/deleteUser/${professorId}`, {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        });
        showSnackbar('Faculty deleted successfully', 'success');
        fetchProfessors(); // Refresh the list
      } catch (err) {
        console.error('Error deleting professor:', err);
        showSnackbar(`Failed to delete professor: ${err.response?.data?.message || err.message}`, 'error');
      }
    }
  };

  // Filter professors based on search term and active filter
  const filteredProfessors = professors.filter((professor) => {
    const fullName = `${professor.firstName || ''} ${professor.lastName || ''}`.toLowerCase();
    const departmentId = professor.department?.departmentId;
    const departmentName = departmentId ? getDepartmentName(departmentId).toLowerCase() : '';
    const departmentAbbr = departmentId ? getDepartmentAbbreviation(departmentId).toLowerCase() : '';
    
    const matchesSearch = 
      fullName.includes(searchTerm.toLowerCase()) ||
      professor.schoolId?.toString().includes(searchTerm) ||
      departmentName.includes(searchTerm.toLowerCase()) ||
      departmentAbbr.includes(searchTerm.toLowerCase()) ||
      professor.email?.toLowerCase().includes(searchTerm.toLowerCase());
    
    if (!activeFilter) return matchesSearch;
    
    // Apply additional filtering based on activeFilter if needed
    switch(activeFilter) {
      case 'Department':
        return matchesSearch && professor.department?.departmentId;
      case 'ID':
        return matchesSearch && professor.schoolId;
      case 'Name':
        return matchesSearch && (professor.firstName || professor.lastName);
      default:
        return matchesSearch;
    }
  });

  return (
    <Box>
      <Box sx={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          mb: 3 
      }}>
        <Typography variant="h6" fontWeight="600" color="#1E293B">
          Faculty Accounts
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Paper
            elevation={0}
            sx={{ 
              p: '2px 4px', 
              display: 'flex', 
              alignItems: 'center', 
              width: 300, 
              bgcolor: '#F8FAFC',
              border: '1px solid #E2E8F0',
              borderRadius: '4px'
            }}
          >
            <IconButton sx={{ p: '10px' }} aria-label="search">
              <Search sx={{ color: '#64748B' }} />
            </IconButton>
            <InputBase
              sx={{ ml: 1, flex: 1, fontSize: 14 }}
              placeholder="Search Faculty..."
              value={searchTerm}
              onChange={handleSearch}
            />
          </Paper>
          <Button 
            variant="outlined" 
            startIcon={<FilterList />}
            size="small"
            onClick={handleFilterClick}
            sx={{
              borderColor: activeFilter ? '#0288d1' : '#E2E8F0',
              color: activeFilter ? '#0288d1' : '#64748B',
              textTransform: 'none',
              fontWeight: 500,
              mr: 0.6,
              borderRadius: '8px',
              fontSize: '0.875rem',
              py: 0.5,
              px: 2
            }}
          >
            {activeFilter || 'FILTER'}
          </Button>
          <Menu
            anchorEl={filterAnchorEl}
            open={filterMenuOpen}
            onClose={handleFilterClose}
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
            <MenuItem onClick={() => handleFilterSelect('Department')}>
              <ListItemIcon>
                <Group fontSize="small" sx={{ color: '#64748B' }} />
              </ListItemIcon>
              <ListItemText>Department</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleFilterSelect('ID')}>
              <ListItemIcon>
                <CalendarToday fontSize="small" sx={{ color: '#64748B' }} />
              </ListItemIcon>
              <ListItemText>ID</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleFilterSelect('Name')}>
              <ListItemIcon>
                <People fontSize="small" sx={{ color: '#64748B' }} />
              </ListItemIcon>
              <ListItemText>Name</ListItemText>
            </MenuItem>
          </Menu>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={handleAddClick}
            sx={{
              bgcolor: '#0288d1',
              '&:hover': {
                bgcolor: '#0277bd',
              },
              textTransform: 'none',
              borderRadius: '4px',
              fontWeight: 500
            }}
          >
            Add Faculty
          </Button>
        </Box>
      </Box>

      {/* Faculty Table */}
      <Paper 
        elevation={0} 
        sx={{ 
          mb: 3,
          border: '1px solid #E2E8F0',
          borderRadius: '8px',
          overflow: 'hidden'
        }}
      >
        <TableContainer sx={{ maxHeight: 'calc(100vh - 240px)' }}>
          <Table stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>School ID</TableCell>
                <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Name</TableCell>
                <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Email</TableCell>
                <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Department</TableCell>
                <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC', width: 180 }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                    <CircularProgress size={30} />
                  </TableCell>
                </TableRow>
              ) : error ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'error.main' }}>
                    {error}
                  </TableCell>
                </TableRow>
              ) : filteredProfessors.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                    No faculty members found
                  </TableCell>
                </TableRow>
              ) : (
                filteredProfessors.map((professor) => (
                  <TableRow 
                    key={professor.userId}
                    sx={{ '&:hover': { bgcolor: '#F1F5F9' } }}
                  >
                    <TableCell>{professor.schoolId}</TableCell>
                    <TableCell>{`${professor.firstName} ${professor.lastName}`}</TableCell>
                    <TableCell>{professor.email}</TableCell>
                    <TableCell>
                      {(() => {
                        try {
                          if (professor.role === 'ADMIN') {
                            return (
                              <>
                                ADMIN
                                <Typography variant="caption" color="#64748B" display="block">
                                  AD
                                </Typography>
                              </>
                            );
                          } else if (professor.department && professor.department.departmentId) {
                            return (
                              <>
                                {getDepartmentName(professor.department.departmentId)}
                                <Typography variant="caption" color="#64748B" display="block">
                                  {getDepartmentAbbreviation(professor.department.departmentId)}
                                </Typography>
                              </>
                            );
                          } else {
                            return 'Not assigned';
                          }
                        } catch (error) {
                          console.error("Error rendering department for", professor.firstName, professor.lastName, error);
                          return 'Not assigned';
                        }
                      })()}
                    </TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 1 }}>
                        <IconButton 
                          size="small" 
                          onClick={() => handleViewClick(professor)}
                          sx={{ color: '#64748B' }}
                          title="View Details"
                        >
                          <VisibilityOutlined fontSize="small" />
                        </IconButton>
                        <IconButton 
                          size="small" 
                          onClick={() => handleEditClick(professor)}
                          sx={{ color: '#0288d1' }}
                          title="Edit"
                        >
                          <Edit fontSize="small" />
                        </IconButton>
                        <IconButton 
                          size="small" 
                          onClick={() => handleViewAttendedEvents(professor)}
                          sx={{ color: '#10B981' }}
                          title="View Attended Events"
                        >
                          <EventAvailable fontSize="small" />
                        </IconButton>
                        <IconButton 
                          size="small" 
                          onClick={() => handleDeleteProfessor(professor.userId)}
                          sx={{ color: '#EF4444' }}
                          title="Delete"
                        >
                          <Delete fontSize="small" />
                        </IconButton>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Add Professor Modal */}
      <Modal
        open={showAddModal}
        onClose={() => setShowAddModal(false)}
        aria-labelledby="add-professor-modal-title"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 500,
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          p: 0,
          overflow: 'hidden'
        }}>
          <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #E2E8F0' }}>
            <Typography variant="h6" fontWeight="600">
              Add New Faculty
            </Typography>
            <IconButton onClick={() => setShowAddModal(false)}>
              <Close />
            </IconButton>
          </Box>
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr', gap: 2 }}>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  School ID
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  name="schoolId"
                  placeholder="Enter school ID (e.g., 22-2220-759)"
                  value={newProfessor.schoolId}
                  onChange={handleInputChange}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                    },
                  }}
                />
              </Box>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  First Name
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  name="firstName"
                  placeholder="Enter first name"
                  value={newProfessor.firstName}
                  onChange={handleInputChange}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                    },
                  }}
                />
              </Box>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  Last Name
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  name="lastName"
                  placeholder="Enter last name"
                  value={newProfessor.lastName}
                  onChange={handleInputChange}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                    },
                  }}
                />
              </Box>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  Email
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  name="email"
                  type="email"
                  placeholder="Enter email address"
                  value={newProfessor.email}
                  onChange={handleInputChange}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                    },
                  }}
                />
              </Box>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  Department
                </Typography>
                <FormControl fullWidth>
                  <Select
                    value={newProfessor.departmentId}
                    name="departmentId"
                    onChange={handleInputChange}
                    displayEmpty
                    sx={{
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& .MuiOutlinedInput-notchedOutline': {
                        borderColor: '#E2E8F0',
                      },
                    }}
                  >
                    <MenuItem value="" disabled>
                      <em>Select Department</em>
                    </MenuItem>
                    {departments.map((dept) => (
                      <MenuItem key={dept.departmentId} value={dept.departmentId}>
                        {dept.name} ({dept.abbreviation})
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Box>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  Password
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  name="password"
                  type="password"
                  placeholder="Enter password"
                  value={newProfessor.password}
                  onChange={handleInputChange}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                    },
                  }}
                />
              </Box>
            </Box>
          </Box>
          <Box sx={{ p: 2, bgcolor: '#F8FAFC', display: 'flex', justifyContent: 'flex-end', gap: 2, borderTop: '1px solid #E2E8F0' }}>
            <Button 
              variant="outlined" 
              onClick={() => setShowAddModal(false)}
              sx={{
                borderColor: '#E2E8F0',
                color: '#64748B',
                '&:hover': {
                  borderColor: '#CBD5E1',
                  bgcolor: 'transparent',
                },
                textTransform: 'none',
                fontWeight: 500
              }}
            >
              Cancel
            </Button>
            <Button 
              variant="contained" 
              onClick={handleAddProfessor}
              sx={{
                bgcolor: '#0288d1',
                '&:hover': {
                  bgcolor: '#0277bd',
                },
                textTransform: 'none',
                fontWeight: 500
              }}
            >
              Add Faculty
            </Button>
          </Box>
        </Box>
      </Modal>
      
      {/* Edit Professor Modal */}
      <Modal
        open={showEditModal}
        onClose={() => setShowEditModal(false)}
        aria-labelledby="edit-professor-modal-title"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 500,
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          p: 0,
          overflow: 'hidden'
        }}>
          <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #E2E8F0' }}>
            <Typography variant="h6" fontWeight="600">
              Edit Faculty
            </Typography>
            <IconButton onClick={() => setShowEditModal(false)}>
              <Close />
            </IconButton>
          </Box>
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr', gap: 2 }}>
            <Box>
  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
    School ID
  </Typography>
  <TextField
    fullWidth
    variant="outlined"
    name="schoolId"
    placeholder="Enter school ID (e.g., 22-2220-759)"
    value={editingProfessor.schoolId}
    onChange={handleEditInputChange}
    sx={{
      '& .MuiOutlinedInput-root': {
        borderRadius: '4px',
        fontSize: '14px',
        '& fieldset': {
          borderColor: '#E2E8F0',
        },
      },
    }}
  />
</Box>
<Box>
  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
    First Name
  </Typography>
  <TextField
    fullWidth
    variant="outlined"
    name="firstName"
    placeholder="Enter first name"
    value={editingProfessor.firstName}
    onChange={handleEditInputChange}
    sx={{
      '& .MuiOutlinedInput-root': {
        borderRadius: '4px',
        fontSize: '14px',
        '& fieldset': {
          borderColor: '#E2E8F0',
        },
      },
    }}
  />
</Box>
<Box>
  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
    Last Name
  </Typography>
  <TextField
    fullWidth
    variant="outlined"
    name="lastName"
    placeholder="Enter last name"
    value={editingProfessor.lastName}
    onChange={handleEditInputChange}
    sx={{
      '& .MuiOutlinedInput-root': {
        borderRadius: '4px',
        fontSize: '14px',
        '& fieldset': {
          borderColor: '#E2E8F0',
        },
      },
    }}
  />
</Box>
<Box>
  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
    Email
  </Typography>
  <TextField
    fullWidth
    variant="outlined"
    name="email"
    type="email"
    placeholder="Enter email address"
    value={editingProfessor.email}
    onChange={handleEditInputChange}
    sx={{
      '& .MuiOutlinedInput-root': {
        borderRadius: '4px',
        fontSize: '14px',
        '& fieldset': {
          borderColor: '#E2E8F0',
        },
      },
    }}
  />
</Box>
<Box>
  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
    Department
  </Typography>
  <FormControl fullWidth>
    <Select
      value={editingProfessor.departmentId}
      name="departmentId"
      onChange={handleEditInputChange}
      displayEmpty
      sx={{
        borderRadius: '4px',
        fontSize: '14px',
        '& .MuiOutlinedInput-notchedOutline': {
          borderColor: '#E2E8F0',
        },
      }}
    >
      <MenuItem value="" disabled>
        <em>Select Department</em>
      </MenuItem>
      {departments.map((dept) => (
        <MenuItem key={dept.departmentId} value={dept.departmentId}>
          {dept.name} ({dept.abbreviation})
        </MenuItem>
      ))}
    </Select>
  </FormControl>
</Box>
<Box>
  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
    Role
  </Typography>
  <FormControl fullWidth>
    <Select
      value={editingProfessor.role}
      name="role"
      onChange={handleEditInputChange}
      sx={{
        borderRadius: '4px',
        fontSize: '14px',
        '& .MuiOutlinedInput-notchedOutline': {
          borderColor: '#E2E8F0',
        },
      }}
    >
      <MenuItem value="USER">User</MenuItem>
      <MenuItem value="ADMIN">Admin</MenuItem>
    </Select>
  </FormControl>
</Box>
</Box>
</Box>
<Box sx={{ p: 2, bgcolor: '#F8FAFC', display: 'flex', justifyContent: 'flex-end', gap: 2, borderTop: '1px solid #E2E8F0' }}>
<Button 
  variant="outlined" 
  onClick={() => setShowEditModal(false)}
  sx={{
    borderColor: '#E2E8F0',
    color: '#64748B',
    '&:hover': {
      borderColor: '#CBD5E1',
      bgcolor: 'transparent',
    },
    textTransform: 'none',
    fontWeight: 500
  }}
>
  Cancel
</Button>
<Button 
  variant="contained" 
  onClick={handleUpdateProfessor}
  sx={{
    bgcolor: '#0288d1',
    '&:hover': {
      bgcolor: '#0277bd',
    },
    textTransform: 'none',
    fontWeight: 500
  }}
>
  Update Faculty
</Button>
</Box>
</Box>
</Modal>

{/* View Professor Modal */}
<Modal
open={showViewModal}
onClose={() => setShowViewModal(false)}
aria-labelledby="view-professor-modal-title"
>
<Box sx={{
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 480,
  bgcolor: 'background.paper',
  boxShadow: 24,
  borderRadius: 1,
  p: 0,
  overflow: 'hidden'
}}>
  <Box sx={{ 
    p: 2, 
    display: 'flex', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    borderBottom: '1px solid #E2E8F0',
    bgcolor: '#F8FAFC'
  }}>
    <Typography variant="h6" fontWeight="600">
      Faculty Details
    </Typography>
    <IconButton onClick={() => setShowViewModal(false)}>
      <Close />
    </IconButton>
  </Box>
  {viewingProfessor && (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
        <Avatar 
          sx={{ 
            width: 64, 
            height: 64,
            bgcolor: '#0288d1',
            fontSize: '1.5rem'
          }}
        >
          {viewingProfessor.firstName?.[0]}{viewingProfessor.lastName?.[0]}
        </Avatar>
        <Box>
          <Typography variant="h6" fontWeight="600">
            {`${viewingProfessor.firstName} ${viewingProfessor.lastName}`}
          </Typography>
          <Chip 
            label={viewingProfessor.role === 'ADMIN' ? 'Administrator' : 'Faculty'} 
            size="small"
            sx={{ 
              bgcolor: viewingProfessor.role === 'ADMIN' ? '#EFF6FF' : '#ECFDF5',
              color: viewingProfessor.role === 'ADMIN' ? '#3B82F6' : '#10B981',
              fontWeight: 500,
              fontSize: '0.75rem'
            }}
          />
        </Box>
      </Box>
      
      <Divider sx={{ my: 2 }} />
      
      <Box sx={{ mb: 3 }}>
        <List disablePadding>
          <ListItem disablePadding sx={{ mb: 2 }}>
            <ListItemIcon sx={{ minWidth: 36 }}>
              <Badge fontSize="small" sx={{ color: '#64748B' }} />
            </ListItemIcon>
            <ListItemText 
              primary={
                <Typography variant="body2" fontWeight="600" color="#1E293B">
                  School ID
                </Typography>
              }
              secondary={viewingProfessor.schoolId || 'Not specified'}
              secondaryTypographyProps={{ color: '#64748B' }}
            />
          </ListItem>
          
          <ListItem disablePadding sx={{ mb: 2 }}>
            <ListItemIcon sx={{ minWidth: 36 }}>
              <Group fontSize="small" sx={{ color: '#64748B' }} />
            </ListItemIcon>
            <ListItemText 
              primary={
                <Typography variant="body2" fontWeight="600" color="#1E293B">
                  Department
                </Typography>
              }
              secondary={
                viewingProfessor.department?.departmentId 
                  ? `${getDepartmentName(viewingProfessor.department.departmentId)} (${getDepartmentAbbreviation(viewingProfessor.department.departmentId)})`
                  : viewingProfessor.role === 'ADMIN' ? 'Administrator' : 'Not assigned'
              }
              secondaryTypographyProps={{ color: '#64748B' }}
            />
          </ListItem>
          
          <ListItem disablePadding sx={{ mb: 2 }}>
            <ListItemIcon sx={{ minWidth: 36 }}>
              <Email fontSize="small" sx={{ color: '#64748B' }} />
            </ListItemIcon>
            <ListItemText 
              primary={
                <Typography variant="body2" fontWeight="600" color="#1E293B">
                  Email
                </Typography>
              }
              secondary={viewingProfessor.email || 'Not specified'}
              secondaryTypographyProps={{ color: '#64748B' }}
            />
          </ListItem>
        </List>
      </Box>
      
      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Button
          variant="outlined"
          startIcon={<EventAvailable />}
          onClick={() => {
            setShowViewModal(false);
            handleViewAttendedEvents(viewingProfessor);
          }}
          sx={{
            borderColor: '#10B981',
            color: '#10B981',
            '&:hover': {
              borderColor: '#059669',
              bgcolor: 'transparent',
            },
            textTransform: 'none',
            fontWeight: 500
          }}
        >
          View Attended Events
        </Button>
        
        <Button
          variant="contained"
          startIcon={<Edit />}
          onClick={() => {
            setShowViewModal(false);
            handleEditClick(viewingProfessor);
          }}
          sx={{
            bgcolor: '#0288d1',
            '&:hover': {
              bgcolor: '#0277bd',
            },
            textTransform: 'none',
            fontWeight: 500
          }}
        >
          Edit Faculty
        </Button>
      </Box>
    </Box>
  )}
</Box>
</Modal>

{/* Attended Events Modal */}
<Modal
open={showAttendedEventsModal}
onClose={() => setShowAttendedEventsModal(false)}
aria-labelledby="attended-events-modal-title"
>
<Box sx={{
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 700,
  bgcolor: 'background.paper',
  boxShadow: 24,
  borderRadius: 1,
  p: 0,
  overflow: 'hidden',
  maxHeight: '80vh',
  display: 'flex',
  flexDirection: 'column'
}}>
  <Box sx={{ 
    p: 2, 
    display: 'flex', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    borderBottom: '1px solid #E2E8F0',
    bgcolor: '#F8FAFC'
  }}>
    <Typography variant="h6" fontWeight="600" color="black">
      {viewingProfessor ? `Events Attended by ${viewingProfessor.firstName} ${viewingProfessor.lastName}` : 'Attended Events'}
    </Typography>
    <IconButton onClick={() => setShowAttendedEventsModal(false)}>
      <Close />
    </IconButton>
  </Box>
  
  <Box sx={{ flexGrow: 1, overflow: 'auto', p: 0 }}>
    {loadingEvents ? (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200 }}>
        <CircularProgress size={30} />
      </Box>
    ) : attendedEvents.length === 0 ? (
      <Box sx={{ p: 3, textAlign: 'center', color: '#64748B' }}>
        <EventAvailable sx={{ fontSize: 48, color: '#CBD5E1', mb: 2 }} />
        <Typography variant="body1" fontWeight="500">
          No events attended yet
        </Typography>
        <Typography variant="body2" color="#94A3B8" sx={{ mt: 1 }}>
          This faculty member hasn't attended any events
        </Typography>
      </Box>
    ) : (
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Event Name</TableCell>
              <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Date</TableCell>
              <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Time</TableCell>
              <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Attendance Status</TableCell>
              <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Entry Type</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {attendedEvents.map((event) => (
              <TableRow key={event.eventId} sx={{ '&:hover': { bgcolor: '#F1F5F9' } }}>
                <TableCell>{event.eventName}</TableCell>
                <TableCell>{formatDate(event.eventDate)}</TableCell>
                <TableCell>{`${formatTime(event.timeIn)} - ${formatTime(event.timeOut)}`}</TableCell>
                <TableCell>
                  <Chip 
                    icon={<CheckCircleOutline fontSize="small" />}
                    label="Present" 
                    size="small"
                    sx={{ 
                      bgcolor: '#ECFDF5',
                      color: '#10B981',
                      fontWeight: 500,
                      fontSize: '0.75rem'
                    }}
                  />
                </TableCell>
                <TableCell sx={{ fontWeight: 200, backgroundColor: '#F8FAFC' }}>
  {event.manualEntry ?'Manual Entry'  : 'QR'}
</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    )}
  </Box>
</Box>
</Modal>

{/* Notification Snackbar */}
<Snackbar
  open={snackbar.open}
  autoHideDuration={6000}
  onClose={handleCloseSnackbar}
  anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
>
  <Alert 
    onClose={handleCloseSnackbar} 
    severity={snackbar.severity} 
    sx={{ width: '100%' }}
  >
    {snackbar.message}
  </Alert>
</Snackbar>

</Box>
);
}