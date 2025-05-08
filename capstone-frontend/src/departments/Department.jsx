import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
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
  Tooltip
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

export default function DepartmentManagement() {
  const navigate = useNavigate();
  const location = useLocation();
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

  // Fetch departments from the backend API
  useEffect(() => {
    const fetchDepartments = async () => {
      try {
        setLoading(true);
        const response = await axios.get('http://localhost:8080/api/departments');
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

  // Handle adding a new program to the list
  const handleAddProgram = () => {
    if (newProgram.trim() !== '') {
      setFormData({
        ...formData,
        offeredPrograms: [...formData.offeredPrograms, newProgram.trim()]
      });
      setNewProgram('');
    }
  };

  // Handle removing a program from the list
  const handleRemoveProgram = (indexToRemove) => {
    setFormData({
      ...formData,
      offeredPrograms: formData.offeredPrograms.filter((_, index) => index !== indexToRemove)
    });
  };

  // Handle form submission
  const handleSubmit = async () => {
    try {
      if (modalMode === 'add') {
        await axios.post('http://localhost:8080/api/departments', formData);
      } else if (modalMode === 'edit') {
        await axios.put(`http://localhost:8080/api/departments/${selectedDepartment.departmentId}`, formData);
      }
      
      // Refresh the departments list
      const response = await axios.get('http://localhost:8080/api/departments');
      setDepartments(response.data);
      
      // Close the modal
      handleCloseModal();
    } catch (error) {
      console.error('Failed to save department:', error);
      setError('Failed to save department. Please try again.');
    }
  };

  // Handle department deletion
  const handleDeleteDepartment = async (departmentId) => {
    try {
      await axios.delete(`http://localhost:8080/api/departments/${departmentId}`);
      
      // Refresh the departments list
      const response = await axios.get('http://localhost:8080/api/departments');
      setDepartments(response.data);
      
      // Close the modal if open
      if (showModal) {
        handleCloseModal();
      }
    } catch (error) {
      console.error('Failed to delete department:', error);
      setError('Failed to delete department. Please try again.');
    }
  };

  // Handle opening the modal for viewing a department
  const handleViewDepartment = (department) => {
    setSelectedDepartment(department);
    setModalMode('view');
    setShowModal(true);
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
    setSelectedDepartment(department);
    setFormData({
      name: department.name,
      abbreviation: department.abbreviation,
      numberOfFaculty: department.numberOfFaculty,
      offeredPrograms: [...department.offeredPrograms]
    });
    setModalMode('edit');
    setShowModal(true);
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

  // Navigation handlers
  const handleNavigateToDashboard = () => {
    navigate('/dashboard');
  };

  const handleNavigateToEvent = () => {
    navigate('/event');
  };

  const handleNavigateToAccounts = () => {
    navigate('/accounts');
  };

  const handleNavigateToDepartment = () => {
    navigate('/department');
  };

  const handleNavigateToSettings = () => {
    navigate('/settings');
  };

  // Avatar menu handlers
  const handleAvatarClick = (event) => {
    setAvatarAnchorEl(event.currentTarget);
  };

  const handleAvatarClose = () => {
    setAvatarAnchorEl(null);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
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
    // Logic to apply the filter would go here
  };

  // Handle search input change
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };

  // Filter departments based on search query
  const filteredDepartments = departments.filter(dept => 
    dept.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    dept.abbreviation.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Pagination logic
  const departmentsPerPage = 5;
  const totalPages = Math.ceil(filteredDepartments.length / departmentsPerPage);
  const indexOfLastDept = currentPage * departmentsPerPage;
  const indexOfFirstDept = indexOfLastDept - departmentsPerPage;
  const currentDepartments = filteredDepartments.slice(indexOfFirstDept, indexOfLastDept);

  return (
    <Box sx={{ display: 'flex', height: '100vh', width: '100%', overflow: 'hidden' }}>
      {/* Sidebar */}
      <Box sx={{
        width: 240,
        bgcolor: 'white',
        borderRight: '1px solid #EAECF0',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0
      }}>
        <Box sx={{ p: 3, borderBottom: '1px solid #EAECF0', display: 'flex', justifyContent: 'center' }}>
          <img src="/timed 1.png" alt="TimeED Logo" style={{ height: 80 }} />
        </Box>
        <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
          <Button
            startIcon={<Home />}
            onClick={handleNavigateToDashboard}
            sx={{
              justifyContent: 'flex-start',
              color: location.pathname === '/dashboard' ? '#0288d1' : '#64748B',
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
              color: location.pathname === '/event' ? '#0288d1' : '#64748B',
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
              color: location.pathname === '/accounts' ? '#0288d1' : '#64748B',
              fontWeight: location.pathname === '/accounts' ? 600 : 500,
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
              color: location.pathname === '/settings' ? '#0288d1' : '#64748B',
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
          bgcolor: 'white',
          borderBottom: '1px solid #EAECF0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <Typography variant="h5" fontWeight="600" color="#1E293B">
            Departments
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
              <MenuItem onClick={() => handleFilterSelect('Faculty Count')}>
                <ListItemIcon>
                  <People fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Faculty Count</ListItemText>
              </MenuItem>
            </Menu>
            <NotificationSystem />
            <Avatar
              onClick={handleAvatarClick}
              sx={{
                width: 36,
                height: 36,
                bgcolor: '#CBD5E1',
                color: 'white',
                cursor: 'pointer'
              }}
            >
              P
            </Avatar>
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
              <MenuItem onClick={handleLogout}>
                <ListItemIcon>
                  <Logout fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Logout</ListItemText>
              </MenuItem>
            </Menu>
          </Box>
        </Box>

        {/* Department Content */}
        <Box sx={{
          p: 3,
          flex: 1,
          overflow: 'auto',
          bgcolor: '#FFFFFF'
        }}>
          {/* Actions Bar */}
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleAddDepartment}
              sx={{
                backgroundColor: '#0288d1',
                '&:hover': {
                  backgroundColor: '#0277bd'
                },
                textTransform: 'none',
                fontWeight: 500
              }}
            >
              Add Department
            </Button>
          </Box>

          {/* Department Count */}
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
            <Typography variant="body1" fontWeight="medium" sx={{ color: '#475569' }}>
              Total Departments: <Box component="span" sx={{ color: '#0288d1', fontWeight: 'bold' }}>{filteredDepartments.length}</Box>
            </Typography>
          </Box>

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
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Department Name</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Abbreviation</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Number of Faculty</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Programs Offered</TableCell>
                  <TableCell sx={{ width: 120, bgcolor: '#F8FAFC', textAlign: 'center' }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                      <CircularProgress size={24} sx={{ color: '#0288d1' }} />
                    </TableCell>
                  </TableRow>
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
                    <TableRow key={index} sx={{ '&:hover': { bgcolor: '#F8FAFC' } }}>
                      <TableCell sx={{ color: '#1E293B' }}>{department.name}</TableCell>
                      <TableCell sx={{ color: '#64748B' }}>{department.abbreviation}</TableCell>
                      <TableCell sx={{ color: '#64748B' }}>{department.numberOfFaculty}</TableCell>
                      <TableCell sx={{ color: '#64748B' }}>
                        {department.offeredPrograms.length > 0 ? (
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                            {department.offeredPrograms.map((program, idx) => (
                              <Chip 
                                key={idx} 
                                label={program} 
                                size="small" 
                                sx={{ 
                                  bgcolor: '#E2E8F0', 
                                  color: '#475569',
                                  fontSize: '0.75rem',
                                  height: 24
                                }} 
                              />
                            ))}
                          </Box>
                        ) : (
                          "No programs offered"
                        )}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
                          <Tooltip title="View">
                            <IconButton
                              onClick={() => handleViewDepartment(department)}
                              size="small"
                              sx={{ color: '#0288d1' }}
                            >
                              <Visibility fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Edit">
                            <IconButton
                              onClick={() => handleEditDepartment(department)}
                              size="small"
                              sx={{ color: '#64748B' }}
                            >
                              <Edit fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete">
                            <IconButton
                              onClick={() => handleDeleteDepartment(department.departmentId)}
                              size="small"
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

          {/* Pagination */}
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 1 }}>
            <Button
              startIcon={<ChevronLeft />}
              onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
              disabled={currentPage === 1}
              sx={{
                color: currentPage === 1 ? '#CBD5E1' : '#64748B',
                textTransform: 'none'
              }}
            >
              PREVIOUS
            </Button>
            {[...Array(Math.min(totalPages, 4))].map((_, index) => (
              <Button
                key={index}
                variant={currentPage === index + 1 ? "contained" : "text"}
                sx={{
                  minWidth: '36px',
                  width: '36px',
                  height: '36px',
                  borderRadius: '4px',
                  backgroundColor: currentPage === index + 1 ? '#0288d1' : 'transparent',
                  color: currentPage === index + 1 ? 'white' : '#64748B'
                }}
                onClick={() => setCurrentPage(index + 1)}
              >
                {index + 1}
              </Button>
            ))}
            <Button
              endIcon={<ChevronRight />}
              onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
              disabled={currentPage === totalPages || totalPages === 0}
              sx={{
                color: currentPage === totalPages || totalPages === 0 ? '#CBD5E1' : '#64748B',
                textTransform: 'none'
              }}
            >
              NEXT
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
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          overflow: 'hidden'
        }}>
          <Box sx={{ p: 3, display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #E2E8F0' }}>
            <Typography variant="h6" fontWeight="600">
              {modalMode === 'view' ? 'Department Details' : 
               modalMode === 'add' ? 'Add New Department' : 'Edit Department'}
            </Typography>
            <IconButton onClick={handleCloseModal}>
              <Close />
            </IconButton>
          </Box>
          
          <Box sx={{ p: 3 }}>
            {modalMode === 'view' ? (
              // View Mode
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" color="#64748B">Department Name</Typography>
                  <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>
                    {selectedDepartment?.name}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" color="#64748B">Abbreviation</Typography>
                  <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>
                    {selectedDepartment?.abbreviation}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" color="#64748B">Number of Faculty</Typography>
                  <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>
                    {selectedDepartment?.numberOfFaculty}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="caption" color="#64748B">Programs Offered</Typography>
                  <Box sx={{ mt: 1, display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {selectedDepartment?.offeredPrograms.length > 0 ? (
                      selectedDepartment.offeredPrograms.map((program, idx) => (
                        <Chip 
                          key={idx} 
                          label={program} 
                          size="small" 
                          sx={{ 
                            bgcolor: '#E2E8F0', 
                            color: '#475569',
                            fontSize: '0.75rem',
                            height: 24
                          }} 
                        />
                      ))
                    ) : (
                      <Typography variant="body2" sx={{ color: '#64748B' }}>No programs offered</Typography>
                    )}
                  </Box>
                </Grid>
              </Grid>
            ) : (
              // Add/Edit Mode
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="name"
                    label="Department Name"
                    value={formData.name}
                    onChange={handleInputChange}
                    fullWidth
                    required
                    size="small"
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
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="numberOfFaculty"
                    label="Number of Faculty"
                    type="number"
                    value={formData.numberOfFaculty}
                    onChange={handleInputChange}
                    fullWidth
                    required
                    size="small"
                    InputProps={{ inputProps: { min: 0 } }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" sx={{ mb: 1, color: '#475569' }}>
                    Programs Offered
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 2 }}>
                    {formData.offeredPrograms.map((program, idx) => (
                      <Chip 
                        key={idx} 
                        label={program} 
                        onDelete={() => handleRemoveProgram(idx)}
                        size="small" 
                        sx={{ 
                          bgcolor: '#E2E8F0', 
                          color: '#475569',
                          fontSize: '0.75rem',
                          height: 24
                        }} 
                      />
                    ))}
                    {formData.offeredPrograms.length === 0 && (
                      <Typography variant="body2" sx={{ color: '#64748B', fontStyle: 'italic' }}>
                        No programs added yet
                      </Typography>
                    )}
                  </Box>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <TextField
                      value={newProgram}
                      onChange={(e) => setNewProgram(e.target.value)}
                      placeholder="Add a program"
                      size="small"
                      sx={{ flex: 1 }}
                    />
                    <Button
                      onClick={handleAddProgram}
                      variant="outlined"
                      size="small"
                      sx={{
                        color: '#0288d1',
                        borderColor: '#0288d1',
                        textTransform: 'none'
                      }}
                    >
                      Add
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            )}
          </Box>
          
          <Box sx={{ p: 2, bgcolor: '#F8FAFC', display: 'flex', justifyContent: 'flex-end', gap: 1, borderTop: '1px solid #E2E8F0' }}>
            {modalMode === 'view' ? (
              // View Mode Actions
              <>
                <Button
                  variant="outlined"
                  onClick={() => handleEditDepartment(selectedDepartment)}
                  sx={{
                    color: '#64748B',
                    borderColor: '#CBD5E1',
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
                    backgroundColor: '#0288d1',
                    '&:hover': {
                      backgroundColor: '#0277bd'
                    },
                    textTransform: 'none',
                    fontWeight: 500
                  }}
                >
                  Close
                </Button>
              </>
            ) : (
              // Add/Edit Mode Actions
              <>
                <Button
                 variant="outlined"
                 onClick={handleCloseModal}
                 sx={{
                   color: '#64748B',
                   borderColor: '#CBD5E1',
                   textTransform: 'none',
                   fontWeight: 500
                 }}
               >

Cancel
                </Button>
                <Button
                  variant="contained"
                  onClick={handleSubmit}
                  sx={{
                    backgroundColor: '#0288d1',
                    '&:hover': {
                      backgroundColor: '#0277bd'
                    },
                    textTransform: 'none',
                    fontWeight: 500
                  }}
                >
                  {modalMode === 'add' ? 'Create Department' : 'Save Changes'}
                </Button>
              </>
            )}
          </Box>
        </Box>
      </Modal>

      {/* Error Snackbar */}
      <Modal
        open={Boolean(error)}
        onClose={() => setError(null)}
        aria-labelledby="error-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '10%',
          left: '50%',
          transform: 'translateX(-50%)',
          width: 400,
          bgcolor: '#FEECEB',
          borderRadius: 1,
          boxShadow: 3,
          p: 3,
          border: '1px solid #EF4444'
        }}>
          <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
            <Box sx={{ p: 1, borderRadius: '50%', bgcolor: '#FEE2E2' }}>
              <Close sx={{ color: '#EF4444', fontSize: 22 }} />
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography variant="subtitle1" fontWeight="600" sx={{ color: '#B91C1C', mb: 1 }}>
                Error
              </Typography>
              <Typography variant="body2" sx={{ color: '#7F1D1D', mb: 2 }}>
                {error}
              </Typography>
              <Button
                size="small"
                onClick={() => setError(null)}
                sx={{
                  bgcolor: 'white',
                  color: '#EF4444',
                  border: '1px solid #FECACA',
                  '&:hover': {
                    bgcolor: '#FEF2F2',
                    border: '1px solid #FECACA',
                  },
                  textTransform: 'none',
                  fontWeight: 500
                }}
              >
                Dismiss
              </Button>
            </Box>
          </Box>
        </Box>
      </Modal>

      {/* Success Notification */}
      <Modal
        open={false} // This can be controlled by a state variable in your actual implementation
        onClose={() => {}} // Add appropriate handler
        aria-labelledby="success-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '10%',
          left: '50%',
          transform: 'translateX(-50%)',
          width: 400,
          bgcolor: '#ECFDF5',
          borderRadius: 1,
          boxShadow: 3,
          p: 3,
          border: '1px solid #10B981'
        }}>
          <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2 }}>
            <Box sx={{ p: 1, borderRadius: '50%', bgcolor: '#D1FAE5' }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#10B981" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
            </Box>
            <Box sx={{ flex: 1 }}>
              <Typography variant="subtitle1" fontWeight="600" sx={{ color: '#047857', mb: 1 }}>
                Success
              </Typography>
              <Typography variant="body2" sx={{ color: '#065F46', mb: 2 }}>
                Operation completed successfully!
              </Typography>
              <Button
                size="small"
                onClick={() => {}} // Add appropriate handler
                sx={{
                  bgcolor: 'white',
                  color: '#10B981',
                  border: '1px solid #A7F3D0',
                  '&:hover': {
                    bgcolor: '#F0FDFA',
                    border: '1px solid #A7F3D0',
                  },
                  textTransform: 'none',
                  fontWeight: 500
                }}
              >
                Dismiss
              </Button>
            </Box>
          </Box>
        </Box>
      </Modal>
    </Box>
  );
}