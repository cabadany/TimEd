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
      
      const response = await axios.post('http://localhost:8080/api/departments', formData);
      
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
      
      const response = await axios.put(`http://localhost:8080/api/departments/${selectedDepartment.departmentId}`, formData);
      
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
  const itemsPerPage = 10;
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

  return (
    <Box className="department-container">
      {/* Department Content */}
      <Box className="department-main">
        {/* Filter and Search Bar */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h6" fontWeight="600" color="#1E293B">
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
              Add Department
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
                    <TableRow key={department.departmentId} sx={{ '&:hover': { bgcolor: '#F8FAFC' } }}>
                      <TableCell sx={{ fontWeight: 500 }}>{department.name}</TableCell>
                      <TableCell>{department.abbreviation}</TableCell>
                      <TableCell>{department.numberOfFaculty}</TableCell>
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
            <Typography variant="body2" color="#64748B" sx={{ mr: 2 }}>
              Page {currentPage} of {pageCount || 1}
            </Typography>
            <Button
              variant="outlined"
              size="small"
              startIcon={<ChevronLeft />}
              onClick={handlePreviousPage}
              disabled={currentPage === 1 || pageCount === 0}
              sx={{ minWidth: 100, textTransform: 'none', mr: 1 }}
            >
              Previous
            </Button>
            <Button
              variant="outlined"
              size="small"
              endIcon={<ChevronRight />}
              onClick={handleNextPage}
              disabled={currentPage === pageCount || pageCount === 0}
              sx={{ minWidth: 100, textTransform: 'none' }}
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
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                    {selectedDepartment?.offeredPrograms && selectedDepartment.offeredPrograms.length > 0 ? (
                      selectedDepartment.offeredPrograms.map((program, i) => (
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
                        No programs offered
                      </Typography>
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
                  <Typography variant="caption" color="#64748B" sx={{ mb: 1, display: 'block' }}>
                    Programs Offered
                  </Typography>
                  <Box sx={{ display: 'flex', mb: 1 }}>
                    <TextField
                      value={newProgram}
                      onChange={(e) => setNewProgram(e.target.value)}
                      placeholder="Add a program"
                      size="small"
                      fullWidth
                    />
                    <Button
                      variant="contained"
                      onClick={handleAddProgram}
                      disabled={newProgram.trim() === ''}
                      sx={{
                        ml: 1,
                        bgcolor: '#0288d1',
                        '&:hover': {
                          bgcolor: '#0277bd'
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
                            bgcolor: '#E0F2FE',
                            color: '#0369A1'
                          }} 
                        />
                      ))
                    ) : (
                      <Typography variant="body2" color="#64748B">
                        No programs added
                      </Typography>
                    )}
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
                  onClick={modalMode === 'add' ? handleSubmitAddDepartment : handleSubmitEditDepartment}
                  sx={{
                    backgroundColor: '#0288d1',
                    '&:hover': {
                      backgroundColor: '#0277bd'
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
    </Box>
  );
}