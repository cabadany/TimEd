    import { useState, useEffect } from 'react';
    import { useNavigate, useLocation } from 'react-router-dom';
    import {
    Box, Typography, Button, IconButton, InputBase, Paper, TextField, Menu, MenuItem, ListItemIcon, ListItemText, Avatar,
    Badge, Modal, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Snackbar, Alert, CircularProgress, Select, FormControl
    } from '@mui/material';
    import {
    Search, AccountTree, Settings, Notifications, FilterList, Home, Event, People, CalendarToday,
    Group, Add, Close, Logout, Edit, Delete, VisibilityOutlined
    } from '@mui/icons-material';
    import axios from 'axios';

    // Base API URL
    const API_BASE_URL = 'http://localhost:8080/api';

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
        role: "USER"
    });
    
    // View professor modal state
    const [showViewModal, setShowViewModal] = useState(false);
    const [viewingProfessor, setViewingProfessor] = useState(null);
    
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
        console.log(response.data);
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
        return department ? department.abbreviation : 'N/A';
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
        const response = await axios.post(`${API_BASE_URL}/auth/register`, newProfessor, {
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
        departmentId: professor.departmentId || "",
        schoolId: professor.schoolId,
        role: professor.role || "USER"
        });
        setShowEditModal(true);
    };
    
    const handleViewClick = (professor) => {
        setViewingProfessor(professor);
        setShowViewModal(true);
    };
    
    const handleUpdateProfessor = async () => {
        try {
        await axios.put(`${API_BASE_URL}/user/updateUser/${editingProfessor.userId}`, editingProfessor, {
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
        const departmentName = getDepartmentName(professor.departmentId).toLowerCase();
        const departmentAbbr = getDepartmentAbbreviation(professor.departmentId).toLowerCase();
        
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
            return matchesSearch && professor.departmentId;
        case 'ID':
            return matchesSearch && professor.schoolId;
        case 'Name':
            return matchesSearch && (professor.firstName || professor.lastName);
        default:
            return matchesSearch;
        }
    });

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
                color: location.pathname === '/' ? '#0288d1' : '#64748B',
                fontWeight: location.pathname === '/' ? 600 : 500,
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
                Account Management
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
                    fontWeight: 500
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
                <IconButton>
                <Badge badgeContent="" color="error" variant="dot">
                    <Notifications sx={{ color: '#64748B', fontSize: 20 }} />
                </Badge>
                </IconButton>
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

            {/* Account Content */}
            <Box sx={{ 
            p: 3, 
            flex: 1, 
            overflow: 'auto', 
            bgcolor: '#FFFFFF' 
            }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h6" fontWeight="600" color="#1E293B">
                Faculty Accounts
                </Typography>
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

            <Paper 
                elevation={0} 
                sx={{ 
                borderRadius: '8px', 
                border: '1px solid #E2E8F0',
                overflow: 'hidden'
                }}
            >
                <TableContainer>
                <Table sx={{ minWidth: 650 }}>
                    <TableHead sx={{ bgcolor: '#F8FAFC' }}>
                    <TableRow>
                        <TableCell sx={{ fontWeight: 600, color: '#64748B', py: 2 }}>ID</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#64748B', py: 2 }}>Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#64748B', py: 2 }}>Email</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#64748B', py: 2 }}>Department</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#64748B', py: 2 }}>Actions</TableCell>
                    </TableRow>
                    </TableHead>
                    <TableBody>
                    {isLoading ? (
                        <TableRow>
                        <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                            <CircularProgress size={24} sx={{ color: '#0288d1' }} />
                        </TableCell>
                        </TableRow>
                    ) : error ? (
                        <TableRow>
                        <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#EF4444' }}>
                            {error}
                        </TableCell>
                        </TableRow>
                    ) : filteredProfessors.length === 0 ? (
                        <TableRow>
                        <TableCell colSpan={5} align="center" sx={{ py: 3, color: '#64748B' }}>
                            No Faculty found
                        </TableCell>
                        </TableRow>
                    ) : (
                        filteredProfessors.map((professor) => (
                        <TableRow key={professor.schoolId} sx={{ '&:hover': { bgcolor: '#F8FAFC' } }}>
                            <TableCell sx={{ py: 2 }}>{professor.schoolId}</TableCell>
                            <TableCell sx={{ py: 2 }}>{`${professor.firstName} ${professor.lastName}`}</TableCell>
                            <TableCell sx={{ py: 2 }}>{professor.email}</TableCell>
                            <TableCell sx={{ py: 2 }}>
                            {getDepartmentName(professor.department.departmentId)} 
                            {professor.departmentId && (
                                <Typography variant="caption" color="text.secondary" display="block">
                                ({getDepartmentAbbreviation(professor.department.departmentId)})
                                </Typography>
                            )}
                            </TableCell>
                            <TableCell sx={{ py: 2 }}>
                            <Box sx={{ display: 'flex', gap: 1 }}>
                                <IconButton 
                                size="small" 
                                sx={{ color: '#0288d1' }}
                                onClick={() => handleViewClick(professor)}
                                >
                                <VisibilityOutlined fontSize="small" />
                                </IconButton>
                                <IconButton 
                                size="small" 
                                sx={{ color: '#10B981' }}
                                onClick={() => handleEditClick(professor)}
                                >
                                <Edit fontSize="small" />
                                </IconButton>
                                <IconButton 
                                size="small" 
                                sx={{ color: '#EF4444' }}
                                onClick={() => handleDeleteProfessor(professor.userId)}
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
            </Box>
        </Box>

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
                    placeholder="School ID"
                    value={editingProfessor.schoolId}
                    disabled
                    sx={{
                        '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        '& fieldset': {
                            borderColor: '#E2E8F0',
                        },
                        '&.Mui-disabled': {
                            bgcolor: '#F8FAFC',
                        }
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
            width: 500,
            bgcolor: 'background.paper',
            boxShadow: 24,
            borderRadius: 1,
            p: 0,
            overflow: 'hidden'
            }}>
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #E2E8F0' }}>
                <Typography variant="h6" fontWeight="600">
                Faculty Details
                </Typography>
                <IconButton onClick={() => setShowViewModal(false)}>
                <Close />
                </IconButton>
            </Box>
            {viewingProfessor && (
                <Box sx={{ p: 3 }}>
                <Box sx={{ display: 'grid', gridTemplateColumns: '1fr', gap: 2 }}>
                    <Box>
                    <Typography variant="body2" fontWeight="500" color="#64748B">
                        School ID
                    </Typography>
                    <Typography variant="body1" fontWeight="500" color="#1E293B">
                        {viewingProfessor.schoolId}
                    </Typography>
                    </Box>
                    <Box>
                    <Typography variant="body2" fontWeight="500" color="#64748B">
                        Full Name
                    </Typography>
                    <Typography variant="body1" fontWeight="500" color="#1E293B">
                        {`${viewingProfessor.firstName} ${viewingProfessor.lastName}`}
                    </Typography>
                    </Box>
                    <Box>
                    <Typography variant="body2" fontWeight="500" color="#64748B">
                        Email
                    </Typography>
                    <Typography variant="body1" color="#1E293B">
                        {viewingProfessor.email}
                    </Typography>
                    </Box>
                    <Box>
                    <Typography variant="body2" fontWeight="500" color="#64748B">
                        Department
                    </Typography>
                    <Typography variant="body1" color="#1E293B">
                        {getDepartmentName(viewingProfessor.department.departmentId)}
                        {viewingProfessor.departmentId && (
                        <Typography variant="caption" color="text.secondary" display="inline" sx={{ ml: 1 }}>
                            ({getDepartmentAbbreviation(viewingProfessor.department.departmentId)})
                        </Typography>
                        )}
                    </Typography>
                    </Box>
                    <Box>
                    <Typography variant="body2" fontWeight="500" color="#64748B">
                        Role
                    </Typography>
                    <Typography variant="body1" color="#1E293B">
                        {viewingProfessor.role}
                    </Typography>
                    </Box>
                    <Box>
                    <Typography variant="body2" fontWeight="500" color="#64748B">
                        Last Updated
                    </Typography>
                    <Typography variant="body1" color="#1E293B">
                        {viewingProfessor.updatedAt ? new Date(viewingProfessor.updatedAt).toLocaleString() : 'N/A'}
                    </Typography>
                    </Box>
                </Box>
                </Box>
            )}
            <Box sx={{ p: 2, bgcolor: '#F8FAFC', display: 'flex', justifyContent: 'flex-end', gap: 2, borderTop: '1px solid #E2E8F0' }}>
                <Button 
                variant="outlined" 
                onClick={() => setShowViewModal(false)}
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
                Close
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
        </Modal>

        {/* Snackbar notifications */}
        <Snackbar
            open={snackbar.open}
            autoHideDuration={5000}
            onClose={handleCloseSnackbar}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
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