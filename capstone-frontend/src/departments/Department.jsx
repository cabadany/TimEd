import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import { API_BASE_URL, getApiUrl, API_ENDPOINTS } from '../utils/api';
import {
  Box,
  Typography,
  Button,
  IconButton,
  CircularProgress,
  InputBase,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  Tab,
  Badge,
  Avatar,
  Modal,
  Grid,
  Card,
  CardContent,
  TextField,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Chip,
  FormControl,
  InputLabel,
  Select,
  OutlinedInput,
  Tooltip,
  Skeleton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
  Alert
} from '@mui/material';
import {
  Search,
  AccountTree,
  Settings,
  Notifications,
  FilterList,
  Visibility,
  Home,
  Event,
  People,
  ChevronLeft,
  ChevronRight,
  Close,
  Add,
  Edit,
  Delete,
  Logout,
  School,
  MoreVert
} from '@mui/icons-material';
import NotificationSystem from '../components/NotificationSystem';
import './Department.css';
import { useTheme } from '../contexts/ThemeContext';

export default function DepartmentManagement() {
  const navigate = useNavigate();
  const location = useLocation();
  const { darkMode } = useTheme();
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState('view'); // 'view', 'add', 'edit'
  const [selectedDepartment, setSelectedDepartment] = useState(null);
  const [avatarAnchorEl, setAvatarAnchorEl] = useState(null);
  const avatarMenuOpen = Boolean(avatarAnchorEl);
  const [filterAnchorEl, setFilterAnchorEl] = useState(null);
  const filterMenuOpen = Boolean(filterAnchorEl);
  const [activeFilter, setActiveFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Form states
  const [formData, setFormData] = useState({
    name: '',
    abbreviation: '',
    numberOfFaculty: 0,
    offeredPrograms: []
  });
  const [newProgram, setNewProgram] = useState('');

  // Add these state variables at the top with other state declarations
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [departmentToDelete, setDepartmentToDelete] = useState(null);

  // Add snackbar state if not already present
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });

  // Fetch departments from the backend API
  useEffect(() => {
    const fetchDepartments = async () => {
      try {
        setLoading(true);
        const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_DEPARTMENTS));
        setDepartments(response.data);
        setLoading(false);
      } catch (error) {
        console.error('Failed to fetch departments:', error);
        setError('Failed to load departments. Please try again later.');
        setLoading(false);
      }
    };

    fetchDepartments();
  }, []);

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: name === 'numberOfFaculty' ? parseInt(value) || 0 : value
    });
  };

  // Handle adding a program to the form data
  const handleAddProgram = () => {
    if (newProgram.trim() !== '' && !formData.offeredPrograms.includes(newProgram.trim())) {
      setFormData({
        ...formData,
        offeredPrograms: [...formData.offeredPrograms, newProgram.trim()]
      });
      setNewProgram('');
    }
  };

  // Handle removing a program from the form data
  const handleRemoveProgram = (program) => {
    setFormData({
      ...formData,
      offeredPrograms: formData.offeredPrograms.filter(p => p !== program)
    });
  };

  // Handle form submission for adding a new department
  const handleSubmitAddDepartment = async () => {
    try {
      // Validate form data
      if (!formData.name || !formData.abbreviation) {
        console.error('Name and abbreviation are required');
        return;
      }
      
      const response = await axios.post(getApiUrl(API_ENDPOINTS.CREATE_DEPARTMENT), formData);
      
      // Add the new department to the state
      setDepartments([...departments, response.data]);
      
      // Close the modal and reset form
      handleCloseModal();
      
      console.log('Department added successfully:', response.data);
    } catch (error) {
      console.error('Error adding department:', error);
    }
  };

  // Handle form submission for editing a department
  const handleSubmitEditDepartment = async () => {
    try {
      if (!selectedDepartment || !formData.name || !formData.abbreviation) {
        console.error('Selected department and form fields are required');
        return;
      }
      
      const response = await axios.put(getApiUrl(API_ENDPOINTS.UPDATE_DEPARTMENT(selectedDepartment.departmentId)), formData);
      
      // Update the department in the state
      setDepartments(departments.map(dept => 
        dept.departmentId === selectedDepartment.departmentId ? response.data : dept
      ));
      
      // Close the modal and reset form
      handleCloseModal();
      
      console.log('Department updated successfully:', response.data);
    } catch (error) {
      console.error('Error updating department:', error);
    }
  };

  // Handle opening the modal for viewing a department
  const handleViewDepartment = (department) => {
    setSelectedDepartment(null); // Reset to trigger skeleton loading
    setModalMode('view');
    setShowModal(true);
    
    // Simulate loading state for better demonstration of skeleton loading
    const modalLoading = setTimeout(() => {
      setSelectedDepartment(department);
    }, 1000); // Simulate network delay of 1 second
    
    // Cleanup timeout on component unmount
    return () => clearTimeout(modalLoading);
  };

  // Handle opening the modal for adding a new department
  const handleAddDepartment = () => {
    setSelectedDepartment(null);
    setFormData({
      name: '',
      abbreviation: '',
      numberOfFaculty: 0,
      offeredPrograms: []
    });
    setModalMode('add');
    setShowModal(true);
  };

  // Handle opening the modal for editing a department
  const handleEditDepartment = (department) => {
    setSelectedDepartment(null); // Reset to trigger skeleton loading
    setFormData({
      name: '',
      abbreviation: '',
      numberOfFaculty: 0,
      offeredPrograms: []
    });
    setModalMode('edit');
    setShowModal(true);
    
    // Simulate loading state for better demonstration of skeleton loading
    const modalLoading = setTimeout(() => {
      setSelectedDepartment(department);
      setFormData({
        name: department.name,
        abbreviation: department.abbreviation,
        numberOfFaculty: department.numberOfFaculty,
        offeredPrograms: [...department.offeredPrograms]
      });
    }, 1000); // Simulate network delay of 1 second
    
    // Cleanup timeout on component unmount
    return () => clearTimeout(modalLoading);
  };

  // Handle closing the modal
  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedDepartment(null);
    setFormData({
      name: '',
      abbreviation: '',
      numberOfFaculty: 0,
      offeredPrograms: []
    });
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
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    setCurrentPage(1); // Reset to first page when searching
  };
  
  // Get filtered departments based on search query
  const getFilteredDepartments = () => {
    if (!searchQuery) return departments;
    
    return departments.filter(dept => 
      dept.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      dept.abbreviation.toLowerCase().includes(searchQuery.toLowerCase())
    );
  };
  
  // Pagination
  const itemsPerPage = 5;
  const filteredDepartments = getFilteredDepartments();
  const pageCount = Math.ceil(filteredDepartments.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const currentDepartments = filteredDepartments.slice(startIndex, startIndex + itemsPerPage);
  
  // Pagination handlers
  const handlePreviousPage = () => {
    setCurrentPage(prev => Math.max(prev - 1, 1));
  };
  
  const handleNextPage = () => {
    setCurrentPage(prev => Math.min(prev + 1, pageCount));
  };

  // Skeleton loading component for departments table
  const DepartmentTableSkeleton = () => (
    <>
      {[...Array(5)].map((_, index) => (
        <TableRow key={`skeleton-${index}`}>
          <TableCell><Skeleton variant="text" width="70%" /></TableCell>
          <TableCell><Skeleton variant="text" width="40%" /></TableCell>
          <TableCell><Skeleton variant="text" width="30%" /></TableCell>
          <TableCell>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
              <Skeleton variant="rectangular" width={60} height={24} sx={{ borderRadius: 1 }} />
              <Skeleton variant="rectangular" width={70} height={24} sx={{ borderRadius: 1 }} />
              <Skeleton variant="rectangular" width={65} height={24} sx={{ borderRadius: 1 }} />
            </Box>
          </TableCell>
          <TableCell>
            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
              <Skeleton variant="circular" width={30} height={30} />
              <Skeleton variant="circular" width={30} height={30} />
              <Skeleton variant="circular" width={30} height={30} />
            </Box>
          </TableCell>
        </TableRow>
      ))}
    </>
  );
  
  // Skeleton loading component for the header section
  const DepartmentHeaderSkeleton = () => (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
      <Skeleton variant="text" width={150} height={32} />
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Skeleton variant="rectangular" width={300} height={40} sx={{ borderRadius: 1 }} />
        <Skeleton variant="rectangular" width={100} height={40} sx={{ borderRadius: 1 }} />
        <Skeleton variant="rectangular" width={150} height={40} sx={{ borderRadius: 1 }} />
      </Box>
    </Box>
  );
  
  // Skeleton loading component for the department modal
  const DepartmentModalSkeleton = () => (
    <Box>
      <Grid container spacing={3}>
        <Grid item xs={12} sm={6}>
          <Typography variant="caption" color="#64748B">
            <Skeleton variant="text" width={100} />
          </Typography>
          <Skeleton variant="text" width="80%" height={24} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="caption" color="#64748B">
            <Skeleton variant="text" width={80} />
          </Typography>
          <Skeleton variant="text" width="60%" height={24} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <Typography variant="caption" color="#64748B">
            <Skeleton variant="text" width={120} />
          </Typography>
          <Skeleton variant="text" width="40%" height={24} />
        </Grid>
        <Grid item xs={12}>
          <Typography variant="caption" color="#64748B">
            <Skeleton variant="text" width={130} />
          </Typography>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
            <Skeleton variant="rectangular" width={80} height={24} sx={{ borderRadius: 1 }} />
            <Skeleton variant="rectangular" width={100} height={24} sx={{ borderRadius: 1 }} />
            <Skeleton variant="rectangular" width={90} height={24} sx={{ borderRadius: 1 }} />
            <Skeleton variant="rectangular" width={70} height={24} sx={{ borderRadius: 1 }} />
          </Box>
        </Grid>
      </Grid>
    </Box>
  );

  // Add this function to handle department deletion
  const handleDeleteDepartment = async () => {
    try {
      await axios.delete(getApiUrl(API_ENDPOINTS.DELETE_DEPARTMENT(departmentToDelete.departmentId)));
      
      // Update the departments list
      setDepartments(departments.filter(dept => dept.departmentId !== departmentToDelete.departmentId));
      
      // Close the confirmation dialog
      setDeleteConfirmOpen(false);
      setDepartmentToDelete(null);
      
      // Show success message
      setSnackbar({
        open: true,
        message: 'Department deleted successfully',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error deleting department:', error);
      setSnackbar({
        open: true,
        message: 'Failed to delete department',
        severity: 'error'
      });
    }
  };

  // Add this function to open delete confirmation
  const handleOpenDeleteConfirm = (department) => {
    setDepartmentToDelete(department);
    setDeleteConfirmOpen(true);
  };

  // Add this function to close delete confirmation
  const handleCloseDeleteConfirm = () => {
    setDeleteConfirmOpen(false);
    setDepartmentToDelete(null);
  };

  return (
    <Box className="department-container">
      {/* Department Content */}
      <Box className="department-main">
        {/* Filter and Search Bar */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6" fontWeight="600" color="#1E293B">
            Departments / Grade Level
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
                placeholder="Search departments..."
                value={searchQuery}
                onChange={handleSearchChange}
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
                borderRadius: '4px',
                fontSize: '0.875rem',
                py: 0.5,
                px: 2
              }}
            >
              {activeFilter || 'Filter'}
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
              <MenuItem onClick={() => handleFilterSelect('Name')}>
                <ListItemIcon>
                  <School fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Name</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => handleFilterSelect('Abbreviation')}>
                <ListItemIcon>
                  <AccountTree fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Abbreviation</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => handleFilterSelect('Number of Faculty')}>
                <ListItemIcon>
                  <People fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Faculty Count</ListItemText>
              </MenuItem>
            </Menu>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleAddDepartment}
              sx={{
                bgcolor: '#0288d1',
                '&:hover': {
                  bgcolor: '#0277bd'
                },
                textTransform: 'none',
                borderRadius: '4px',
                fontWeight: 500
              }}
            >
              Add Department / Grade Level
            </Button>
          </Box>
        </Box>
        
        {/* Departments Table */}
        <Box sx={{ mb: 3 }}>
          {/* Departments Table */}
          <TableContainer
            component={Paper}
            sx={{
              mb: 3,
              boxShadow: 'none',
              border: '1px solid #E2E8F0',
              borderRadius: '4px',
              overflow: 'hidden',
              maxHeight: 'calc(100vh - 250px)'
            }}
          >
            <Table stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Department / Grade Level</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Abbreviation</TableCell>
              {/*    <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Number of Faculty</TableCell>*/}
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Programs Offered</TableCell>
                  <TableCell sx={{ width: 120, bgcolor: '#F8FAFC', textAlign: 'center' }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <DepartmentTableSkeleton />
                ) : error ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ color: '#EF4444' }}>{error}</TableCell>
                  </TableRow>
                ) : currentDepartments.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">No departments found</TableCell>
                  </TableRow>
                ) : (
                  currentDepartments.map((department, index) => (
                    <TableRow key={department.departmentId} sx={{ '&:hover': { bgcolor: '#F8FAFC' } }}>
                      <TableCell sx={{ fontWeight: 500 }}>{department.name}</TableCell>
                      <TableCell>{department.abbreviation}</TableCell>
                     {/* <TableCell>{department.numberOfFaculty}</TableCell>*/}
                      <TableCell>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {department.offeredPrograms && department.offeredPrograms.length > 0 ? (
                            department.offeredPrograms.map((program, i) => (
                              <Chip 
                                key={i} 
                                label={program} 
                                size="small" 
                                sx={{ 
                                  fontSize: '0.75rem', 
                                  bgcolor: '#E0F2FE',
                                  color: '#0369A1'
                                }} 
                              />
                            ))
                          ) : (
                            <Typography variant="body2" color="#64748B">
                              No programs
                            </Typography>
                          )}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                          <Tooltip title="View Details">
                            <IconButton 
                              size="small" 
                              onClick={() => handleViewDepartment(department)}
                              sx={{ color: '#64748B' }}
                            >
                              <Visibility fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Edit Department">
                            <IconButton 
                              size="small" 
                              onClick={() => handleEditDepartment(department)}
                              sx={{ color: '#0288d1' }}
                            >
                              <Edit fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete Department">
                            <IconButton 
                              size="small" 
                              onClick={() => handleOpenDeleteConfirm(department)}
                              sx={{ color: '#EF4444' }}
                            >
                              <Delete fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          
          {/* Pagination Controls */}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', mb: 2 }}>
            <Typography variant="body2" color={darkMode ? '#aaaaaa' : '#64748B'} sx={{ mr: 2 }}>
              Page {currentPage} of {pageCount || 1}
            </Typography>
            <Button
              variant="outlined"
              size="small"
              startIcon={<ChevronLeft />}
              onClick={handlePreviousPage}
              disabled={currentPage === 1 || pageCount === 0}
              sx={{ 
                minWidth: 100, 
                textTransform: 'none', 
                mr: 1,
                borderColor: darkMode ? '#555555' : '#E2E8F0',
                color: darkMode ? (currentPage === 1 || pageCount === 0 ? '#666666' : '#90caf9') : '#64748B',
                '&:hover': {
                  borderColor: darkMode ? '#90caf9' : '#CBD5E1',
                  bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'transparent',
                },
                '&.Mui-disabled': {
                  borderColor: darkMode ? '#333333' : '#E2E8F0',
                  color: darkMode ? '#666666' : 'rgba(0, 0, 0, 0.26)',
                }
              }}
            >
              Previous
            </Button>
            <Button
              variant="outlined"
              size="small"
              endIcon={<ChevronRight />}
              onClick={handleNextPage}
              disabled={currentPage === pageCount || pageCount === 0}
              sx={{ 
                minWidth: 100, 
                textTransform: 'none',
                borderColor: darkMode ? '#555555' : '#E2E8F0',
                color: darkMode ? (currentPage === pageCount || pageCount === 0 ? '#666666' : '#90caf9') : '#64748B',
                '&:hover': {
                  borderColor: darkMode ? '#90caf9' : '#CBD5E1',
                  bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'transparent',
                },
                '&.Mui-disabled': {
                  borderColor: darkMode ? '#333333' : '#E2E8F0',
                  color: darkMode ? '#666666' : 'rgba(0, 0, 0, 0.26)',
                }
              }}
            >
              Next
            </Button>
          </Box>
        </Box>
      </Box>
      
      {/* Department Modal (View/Add/Edit) */}
      <Modal
        open={showModal}
        onClose={handleCloseModal}
        aria-labelledby="department-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 600,
          bgcolor: darkMode ? '#1e1e1e' : 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          overflow: 'hidden',
          border: darkMode ? '1px solid #333333' : 'none'
        }}>
          <Box sx={{ 
            p: 3, 
            display: 'flex', 
            justifyContent: 'space-between', 
            borderBottom: '1px solid',
            borderColor: darkMode ? '#333333' : '#E2E8F0',
            bgcolor: darkMode ? '#2d2d2d' : '#F8FAFC'
          }}>
            <Typography variant="h6" fontWeight="600" color={darkMode ? '#f5f5f5' : 'inherit'}>
              {modalMode === 'view' ? 'Department Details' : 
               modalMode === 'add' ? 'Add New Department' : 'Edit Department'}
            </Typography>
            <IconButton onClick={handleCloseModal} sx={{ color: darkMode ? '#aaaaaa' : 'inherit' }}>
              <Close />
            </IconButton>
          </Box>
          
          <Box sx={{ p: 3, bgcolor: darkMode ? '#1e1e1e' : 'background.paper' }}>
            {loading && modalMode === 'view' ? (
              <DepartmentModalSkeleton />
            ) : modalMode === 'view' ? (
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" color={darkMode ? '#aaaaaa' : '#64748B'}>Department Name</Typography>
                  <Typography variant="body1" fontWeight="500" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B' }}>
                    {selectedDepartment?.name}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" color={darkMode ? '#aaaaaa' : '#64748B'}>Abbreviation</Typography>
                  <Typography variant="body1" fontWeight="500" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B' }}>
                    {selectedDepartment?.abbreviation}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" color={darkMode ? '#aaaaaa' : '#64748B'}>Number of Faculty</Typography>
                  <Typography variant="body1" fontWeight="500" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B' }}>
                    {selectedDepartment?.numberOfFaculty}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color={darkMode ? '#aaaaaa' : '#64748B'}>Programs Offered</Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                    {selectedDepartment?.offeredPrograms && selectedDepartment.offeredPrograms.length > 0 ? (
                      selectedDepartment.offeredPrograms.map((program, i) => (
                        <Chip 
                          key={i} 
                          label={program} 
                          size="small" 
                          sx={{ 
                            fontSize: '0.75rem', 
                            bgcolor: darkMode ? '#1e293b' : '#E0F2FE',
                            color: darkMode ? '#90caf9' : '#0369A1',
                            border: darkMode ? '1px solid #333333' : 'none'
                          }} 
                        />
                      ))
                    ) : (
                      <Typography variant="body2" color={darkMode ? '#aaaaaa' : '#64748B'}>
                        No programs offered
                      </Typography>
                    )}
                  </Box>
                </Grid>
              </Grid>
            ) : (
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="name"
                    label="Department / Grade Level"
                    value={formData.name}
                    onChange={handleInputChange}
                    fullWidth
                    required
                    size="small"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        '& fieldset': {
                          borderColor: darkMode ? '#333333' : '#E2E8F0',
                        },
                        '&:hover fieldset': {
                          borderColor: darkMode ? '#555555' : '#CBD5E1',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: darkMode ? '#90caf9' : '#0288d1',
                        },
                        '& input': {
                          color: darkMode ? '#f5f5f5' : 'inherit',
                        },
                      },
                      '& .MuiInputLabel-root': {
                        color: darkMode ? '#aaaaaa' : 'inherit',
                      },
                      '& .MuiInputLabel-root.Mui-focused': {
                        color: darkMode ? '#90caf9' : '#0288d1',
                      },
                    }}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="abbreviation"
                    label="Abbreviation"
                    value={formData.abbreviation}
                    onChange={handleInputChange}
                    fullWidth
                    required
                    size="small"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        '& fieldset': {
                          borderColor: darkMode ? '#333333' : '#E2E8F0',
                        },
                        '&:hover fieldset': {
                          borderColor: darkMode ? '#555555' : '#CBD5E1',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: darkMode ? '#90caf9' : '#0288d1',
                        },
                        '& input': {
                          color: darkMode ? '#f5f5f5' : 'inherit',
                        },
                      },
                      '& .MuiInputLabel-root': {
                        color: darkMode ? '#aaaaaa' : 'inherit',
                      },
                      '& .MuiInputLabel-root.Mui-focused': {
                        color: darkMode ? '#90caf9' : '#0288d1',
                      },
                    }}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    disabled={true}
                    name="numberOfFaculty"
                    label="Number of Faculty"
                    type="number"
                    value={formData.numberOfFaculty}
                    onChange={handleInputChange}
                    fullWidth
                    required
                    size="small"
                    InputProps={{ inputProps: { min: 0 } }}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        '& fieldset': {
                          borderColor: darkMode ? '#333333' : '#E2E8F0',
                        },
                        '&:hover fieldset': {
                          borderColor: darkMode ? '#555555' : '#CBD5E1',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: darkMode ? '#90caf9' : '#0288d1',
                        },
                        '& input': {
                          color: darkMode ? '#f5f5f5' : 'inherit',
                        },
                      },
                      '& .MuiInputLabel-root': {
                        color: darkMode ? '#aaaaaa' : 'inherit',
                      },
                      '& .MuiInputLabel-root.Mui-focused': {
                        color: darkMode ? '#90caf9' : '#0288d1',
                      },
                    }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color={darkMode ? '#aaaaaa' : '#64748B'} sx={{ mb: 1, display: 'block' }}>
                    Programs Offered
                  </Typography>
                  <Box sx={{ display: 'flex', mb: 1 }}>
                    <TextField
                      value={newProgram}
                      onChange={(e) => setNewProgram(e.target.value)}
                      placeholder="Add a program"
                      size="small"
                      fullWidth
                      sx={{
                        '& .MuiOutlinedInput-root': {
                          '& fieldset': {
                            borderColor: darkMode ? '#333333' : '#E2E8F0',
                          },
                          '&:hover fieldset': {
                            borderColor: darkMode ? '#555555' : '#CBD5E1',
                          },
                          '&.Mui-focused fieldset': {
                            borderColor: darkMode ? '#90caf9' : '#0288d1',
                          },
                          '& input': {
                            color: darkMode ? '#f5f5f5' : 'inherit',
                          },
                          '& input::placeholder': {
                            color: darkMode ? '#aaaaaa' : '#64748B',
                            opacity: 1,
                          },
                        },
                      }}
                    />
                    <Button
                      variant="contained"
                      onClick={handleAddProgram}
                      disabled={newProgram.trim() === ''}
                      sx={{
                        ml: 1,
                        bgcolor: darkMode ? '#90caf9' : '#0288d1',
                        color: darkMode ? '#1e1e1e' : '#ffffff',
                        '&:hover': {
                          bgcolor: darkMode ? '#42a5f5' : '#0277bd',
                        },
                        textTransform: 'none'
                      }}
                    >
                      Add
                    </Button>
                  </Box>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                    {formData.offeredPrograms.length > 0 ? (
                      formData.offeredPrograms.map((program, i) => (
                        <Chip 
                          key={i} 
                          label={program} 
                          onDelete={() => handleRemoveProgram(program)}
                          size="small" 
                          sx={{ 
                            fontSize: '0.75rem', 
                            bgcolor: darkMode ? '#1e293b' : '#E0F2FE',
                            color: darkMode ? '#90caf9' : '#0369A1',
                            border: darkMode ? '1px solid #333333' : 'none',
                            '& .MuiChip-deleteIcon': {
                              color: darkMode ? '#90caf9' : '#0369A1',
                              '&:hover': {
                                color: darkMode ? '#42a5f5' : '#0277bd',
                              },
                            },
                          }} 
                        />
                      ))
                    ) : (
                      <Typography variant="body2" color={darkMode ? '#aaaaaa' : '#64748B'}>
                        No programs added
                      </Typography>
                    )}
                  </Box>
                </Grid>
              </Grid>
            )}
          </Box>
          
          <Box sx={{ 
            p: 2, 
            bgcolor: darkMode ? '#2d2d2d' : '#F8FAFC', 
            display: 'flex', 
            justifyContent: 'flex-end', 
            gap: 1, 
            borderTop: '1px solid',
            borderColor: darkMode ? '#333333' : '#E2E8F0'
          }}>
            {modalMode === 'view' ? (
              <>
                <Button
                  variant="outlined"
                  onClick={() => handleEditDepartment(selectedDepartment)}
                  sx={{
                    borderColor: darkMode ? '#555555' : '#CBD5E1',
                    color: darkMode ? '#90caf9' : '#64748B',
                    '&:hover': {
                      borderColor: darkMode ? '#90caf9' : '#94A3B8',
                      bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : '#F8FAFC',
                    },
                    textTransform: 'none',
                    fontWeight: 500
                  }}
                >
                  Edit
                </Button>
                <Button
                  variant="contained"
                  onClick={handleCloseModal}
                  sx={{
                    bgcolor: darkMode ? '#90caf9' : '#0288d1',
                    color: darkMode ? '#1e1e1e' : '#ffffff',
                    '&:hover': {
                      bgcolor: darkMode ? '#42a5f5' : '#0277bd',
                    },
                    textTransform: 'none',
                    fontWeight: 500
                  }}
                >
                  Close
                </Button>
              </>
            ) : (
              <>
                <Button
                  variant="outlined"
                  onClick={handleCloseModal}
                  sx={{
                    borderColor: darkMode ? '#555555' : '#CBD5E1',
                    color: darkMode ? '#90caf9' : '#64748B',
                    '&:hover': {
                      borderColor: darkMode ? '#90caf9' : '#94A3B8',
                      bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : '#F8FAFC',
                    },
                    textTransform: 'none',
                    fontWeight: 500
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="contained"
                  onClick={modalMode === 'add' ? handleSubmitAddDepartment : handleSubmitEditDepartment}
                  sx={{
                    bgcolor: darkMode ? '#90caf9' : '#0288d1',
                    color: darkMode ? '#1e1e1e' : '#ffffff',
                    '&:hover': {
                      bgcolor: darkMode ? '#42a5f5' : '#0277bd',
                    },
                    textTransform: 'none',
                    fontWeight: 500
                  }}
                >
                  {modalMode === 'add' ? 'Add Department' : 'Save Changes'}
                </Button>
              </>
            )}
          </Box>
        </Box>
      </Modal>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteConfirmOpen}
        onClose={handleCloseDeleteConfirm}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
        PaperProps={{
          sx: {
            width: '400px',
            borderRadius: '8px',
            p: 1
          }
        }}
      >
        <DialogTitle id="alert-dialog-title" sx={{ pb: 1 }}>
          {"Confirm Department Deletion"}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1" sx={{ color: '#1E293B' }}>
            Are you sure you want to delete the department "{departmentToDelete?.name}"? This action cannot be undone.
          </Typography>
          <Typography variant="body2" sx={{ color: '#EF4444', mt: 2 }}>
            Note: All faculty members in this department will be unassigned.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 2, pt: 1 }}>
          <Button
            onClick={handleCloseDeleteConfirm}
            variant="outlined"
            sx={{
              color: '#64748B',
              borderColor: '#CBD5E1',
              '&:hover': {
                borderColor: '#94A3B8',
                bgcolor: '#F8FAFC'
              }
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleDeleteDepartment}
            variant="contained"
            sx={{
              bgcolor: '#EF4444',
              color: 'white',
              '&:hover': {
                bgcolor: '#DC2626'
              }
            }}
            autoFocus
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert 
          onClose={() => setSnackbar({ ...snackbar, open: false })} 
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}