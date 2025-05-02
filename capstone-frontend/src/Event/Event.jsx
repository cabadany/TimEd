  import { useState, useEffect } from 'react';
  import { useNavigate, useLocation } from 'react-router-dom';
  import axios from 'axios';
  import {
    Box,
    Typography,
    Button,
    IconButton,
    InputBase,
    Paper,
    TextField,
    Menu,
    MenuItem,
    ListItemIcon,
    ListItemText,
    Avatar,
    Badge,
    Modal,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Select,
    FormControl,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    InputAdornment,
    CircularProgress,
    Chip,
    Snackbar,
    Alert
  } from '@mui/material';
  import {
    Search,
    AccountTree,
    Settings,
    Notifications,
    FilterList,
    Home,
    Event,
    People,
    CalendarToday,
    AccessTime,
    Group,
    Upload,
    Close,
    Logout,
    Delete,
    Add,
    Error
  } from '@mui/icons-material';
  import './Event.css';

  export default function EventPage() {
    const navigate = useNavigate();
    const location = useLocation();
    
    // State for form fields
    const [eventName, setEventName] = useState('');
    const [departmentId, setDepartmentId] = useState('');
    const [date, setDate] = useState('');
    const [duration, setDuration] = useState('0:00:00');
    
    // State for events data
    const [events, setEvents] = useState([]);
    const [departments, setDepartments] = useState([]);
    const [loading, setLoading] = useState(false);
    
    // State for upload modal
    const [showUploadModal, setShowUploadModal] = useState(false);
    const [uploadedFile, setUploadedFile] = useState(null);
    
    // State for department selection modal
    const [showDepartmentModal, setShowDepartmentModal] = useState(false);
    const [departmentFilter, setDepartmentFilter] = useState('');
    const [filteredDepartments, setFilteredDepartments] = useState([]);
    
    // State for delete confirmation dialog
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [eventToDelete, setEventToDelete] = useState(null);
    
    // Snackbar notification state
    const [snackbar, setSnackbar] = useState({
      open: false,
      message: '',
      severity: 'success'
    });
    
    // Filter menu state
    const [filterAnchorEl, setFilterAnchorEl] = useState(null);
    const filterMenuOpen = Boolean(filterAnchorEl);
    const [activeFilter, setActiveFilter] = useState('');

    // Avatar dropdown menu state
    const [avatarAnchorEl, setAvatarAnchorEl] = useState(null);
    const avatarMenuOpen = Boolean(avatarAnchorEl);

    // Fetch events and departments on component mount
    useEffect(() => {
      fetchEvents();
      fetchDepartments();
    }, []);
    
    // Filter departments when filter text changes
    useEffect(() => {
      if (departments.length > 0) {
        const filtered = departments.filter(dept => 
          dept.name.toLowerCase().includes(departmentFilter.toLowerCase()) ||
          dept.abbreviation.toLowerCase().includes(departmentFilter.toLowerCase())
        );
        setFilteredDepartments(filtered);
      }
    }, [departmentFilter, departments]);

    // API calls
    const fetchEvents = async () => {
      setLoading(true);
      try {
        const response = await axios.get('http://localhost:8080/api/events/getAll');
        setEvents(response.data);
      } catch (error) {
        console.error('Error fetching events:', error);
        showSnackbar('Failed to load events', 'error');
      } finally {
        setLoading(false);
      }
    };

    const fetchDepartments = async () => {
      try {
        const response = await axios.get('http://localhost:8080/api/departments');
        setDepartments(response.data);
        setFilteredDepartments(response.data);
      } catch (error) {
        console.error('Error fetching departments:', error);
        showSnackbar('Failed to load departments', 'error');
      }
    };

    const createEvent = async () => {
      // Validate form inputs
      if (!eventName || !departmentId || !date || !duration) {
        showSnackbar('Please fill in all required fields', 'error');
        return;
      }
      
      // Format date for API based on supported backend formats (ISO format)
      // The backend expects one of these formats: 
      // "yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ss.SSS", "EEE, dd MMM yyyy HH:mm:ss zzz", "yyyy-MM-dd"
      
      // Using the ISO format which matches the first format the backend accepts
      const formattedDate = new Date(date).toISOString();
      
      const eventData = {
        eventName,
        departmentId,
        date: formattedDate,
        duration,
        status: 'Upcoming'
      };
      
      setLoading(true);
      try {
        const response = await axios.post('http://localhost:8080/api/events/createEvent', eventData);
        
        // Reset form fields
        resetForm();
        
        // Refresh events list
        fetchEvents();
        showSnackbar('Event created successfully', 'success');
      } catch (error) {
        console.error('Error creating event:', error);
        showSnackbar(error.response?.data?.message || 'Failed to create event', 'error');
      } finally {
        setLoading(false);
      }
    };

    const deleteEvent = async (eventId) => {
      setLoading(true);
      try {
        await axios.delete(`http://localhost:8080//api/events/${eventId}`);
        
        // Remove event from state
        setEvents(events.filter(event => event.eventId !== eventId));
        showSnackbar('Event deleted successfully', 'success');
      } catch (error) {
        console.error('Error deleting event:', error);
        showSnackbar('Failed to delete event', 'error');
      } finally {
        setLoading(false);
        setDeleteDialogOpen(false);
      }
    };

    // Form handling
    const resetForm = () => {
      setEventName('');
      setDepartmentId('');
      setDate('');
      setDuration('0:00:00');
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

    // Department modal handlers
    const openDepartmentModal = () => {
      setDepartmentFilter('');
      setShowDepartmentModal(true);
    };
    
    const handleDepartmentSelect = (dept) => {
      setDepartmentId(dept.departmentId);
      setShowDepartmentModal(false);
    };

    // Delete event handlers
    const openDeleteDialog = (event) => {
      setEventToDelete(event);
      setDeleteDialogOpen(true);
    };
    
    const closeDeleteDialog = () => {
      setDeleteDialogOpen(false);
      setEventToDelete(null);
    };
    
    const confirmDelete = () => {
      if (eventToDelete) {
        deleteEvent(eventToDelete.eventId);
      }
    };

    // File upload handlers
    const handleUploadClick = () => {
      setShowUploadModal(true);
    };

    const handleFileChange = (event) => {
      setUploadedFile(event.target.files[0]);
    };

    const handleUploadSubmit = () => {
      // Handle the file upload logic here
      console.log('File uploaded:', uploadedFile);
      setShowUploadModal(false);
    };

    // Form submit handler
    const handleAddEvent = () => {
      createEvent();
    };

    // Snackbar handlers
    const showSnackbar = (message, severity) => {
      setSnackbar({ open: true, message, severity });
    };
    
    const handleCloseSnackbar = () => {
      setSnackbar({ ...snackbar, open: false });
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
    
    // Helper to find department name from ID
    const getDepartmentName = (id) => {
      const dept = departments.find(d => d.departmentId === id);
      return dept ? dept.name : 'Unknown Department';
    };
    
    // Format date for display
    const formatDate = (dateString) => {
      try {
        const date = new Date(dateString.replace('UTC+8', '+08:00'));
        return date.toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        });
      } catch (e) {
        return dateString;
      }
    };

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
                color: '#64748B',
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
              Events
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
                  placeholder="Search for something"
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
                <MenuItem onClick={() => handleFilterSelect('Date')}>
                  <ListItemIcon>
                    <CalendarToday fontSize="small" sx={{ color: '#64748B' }} />
                  </ListItemIcon>
                  <ListItemText>Date</ListItemText>
                </MenuItem>
                <MenuItem onClick={() => handleFilterSelect('Organizer')}>
                  <ListItemIcon>
                    <Group fontSize="small" sx={{ color: '#64748B' }} />
                  </ListItemIcon>
                  <ListItemText>Organizer</ListItemText>
                </MenuItem>
                <MenuItem onClick={() => handleFilterSelect('Time')}>
                  <ListItemIcon>
                    <AccessTime fontSize="small" sx={{ color: '#64748B' }} />
                  </ListItemIcon>
                  <ListItemText>Time</ListItemText>
                </MenuItem>
                <MenuItem onClick={() => handleFilterSelect('Event')}>
                  <ListItemIcon>
                    <Event fontSize="small" sx={{ color: '#64748B' }} />
                  </ListItemIcon>
                  <ListItemText>Event</ListItemText>
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

          {/* Event Content */}
          <Box sx={{ 
            p: 3, 
            flex: 1, 
            overflow: 'auto', 
            bgcolor: '#FFFFFF',
            display: 'flex',
            flexDirection: 'column',
            gap: 3
          }}>
            <Typography variant="h6" fontWeight="600" color="#1E293B">
              Add New Event
            </Typography>

            <Paper 
              elevation={0} 
              sx={{ 
                p: 4, 
                borderRadius: '8px', 
                border: '1px solid #E2E8F0'
              }}
            >
              <Typography variant="body2" color="#64748B" sx={{ mb: 4 }}>
                With just one click, users can initiate the setup of meetings, parties, webinars, or 
                social gatherings. This button typically opens a form where essential details—such 
                as the event name, date, time, location, and description—can be entered.
              </Typography>

              <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 3 }}>
                <Box>
                  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                    Event Name *
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    value={eventName}
                    onChange={(e) => setEventName(e.target.value)}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        '& fieldset': {
                          borderColor: '#E2E8F0',
                        },
                        '&:hover fieldset': {
                          borderColor: '#CBD5E1',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: '#0288d1',
                        },
                      },
                    }}
                  />
                </Box>

                <Box>
                  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                    Department *
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <TextField
                      fullWidth
                      variant="outlined"
                      value={getDepartmentName(departmentId)}
                      disabled
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
                    <Button
                      variant="outlined"
                      onClick={openDepartmentModal}
                      sx={{
                        borderColor: '#0288d1',
                        color: '#0288d1',
                        '&:hover': {
                          borderColor: '#0277bd',
                          bgcolor: 'rgba(2, 136, 209, 0.04)',
                        },
                        minWidth: '120px'
                      }}
                    >
                      Select
                    </Button>
                  </Box>
                </Box>

                <Box>
                  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                    Date *
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    type="datetime-local"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        '& fieldset': {
                          borderColor: '#E2E8F0',
                        },
                        '&:hover fieldset': {
                          borderColor: '#CBD5E1',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: '#0288d1',
                        },
                      },
                    }}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />
                </Box>

                <Box>
                  <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                    Duration *
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    type="text"
                    value={duration}
                    onChange={(e) => setDuration(e.target.value)}
            
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '4px',
                        fontSize: '14px',
                        '& fieldset': {
                          borderColor: '#E2E8F0',
                        },
                        '&:hover fieldset': {
                          borderColor: '#CBD5E1',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: '#0288d1',
                        },
                      },
                    }}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />
                </Box>
              </Box>

              <Box sx={{ mt: 4 }}>
                <Button
                  variant="contained"
                  onClick={handleAddEvent}
                  disabled={loading}
                  startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <Add />}
                  sx={{
                    bgcolor: '#0288d1',
                    '&:hover': {
                      bgcolor: '#0277bd',
                    },
                    textTransform: 'none',
                    borderRadius: '4px',
                    fontWeight: 500,
                    px: 4
                  }}
                >
                  {loading ? 'Creating...' : 'Add Event'}
                </Button>
              </Box>
            </Paper>

            {/* Events Table */}
            <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mt: 2 }}>
              All Events
            </Typography>

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
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B' }}>Event Name</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B' }}>Department</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B' }}>Date</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B' }}>Duration</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B' }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B' }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {loading && events.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                          <CircularProgress size={30} />
                          <Typography variant="body2" color="#64748B" sx={{ mt: 1 }}>
                            Loading events...
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ) : events.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 3 }}>
                            <Error sx={{ fontSize: 40, color: '#94A3B8', mb: 1 }} />
                            <Typography variant="body1" color="#1E293B" fontWeight={500}>
                              No events found
                            </Typography>
                            <Typography variant="body2" color="#64748B">
                              Add a new event using the form above
                            </Typography>
                          </Box>
                        </TableCell>
                      </TableRow>
                    ) : (
                      events.map((event) => (
                        <TableRow key={event.eventId} hover>
                          <TableCell sx={{ color: '#1E293B' }}>{event.eventName}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>{getDepartmentName(event.departmentId)}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>{formatDate(event.date)}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>{event.duration}</TableCell>
                          <TableCell>
                            <Chip 
                              label={event.status} 
                              size="small"
                              sx={{ 
                                bgcolor: event.status === 'Upcoming' ? '#DCFCE7' : 
                                        event.status === 'Ongoing' ? '#E0F2FE' : '#FEF3C7',
                                color: event.status === 'Upcoming' ? '#166534' : 
                                      event.status === 'Ongoing' ? '#0369A1' : '#92400E',
                                fontWeight: 500,
                                fontSize: '0.75rem'
                              }} 
                            />
                          </TableCell>
                          <TableCell>
                            <IconButton 
                              size="small" 
                              onClick={() => openDeleteDialog(event)}
                              sx={{ color: '#EF4444' }}
                            >
                              <Delete fontSize="small" />
                            </IconButton>
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

        {/* Department Selection Modal */}
        <Modal
          open={showDepartmentModal}
          onClose={() => setShowDepartmentModal(false)}
          aria-labelledby="department-modal-title"
        >
          <Box sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: 600,
            maxWidth: '90vw',
            bgcolor: 'background.paper',
            boxShadow: 24,
            borderRadius: 1,
            p: 0,
            maxHeight: '80vh',
            display: 'flex',
            flexDirection: 'column'
          }}>
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #E2E8F0' }}>
              <Typography variant="h6" fontWeight="600">
                Select Department
              </Typography>
              <IconButton onClick={() => setShowDepartmentModal(false)}>
                <Close />
              </IconButton>
            </Box>
            
            <Box sx={{ p: 2, borderBottom: '1px solid #E2E8F0' }}>
              <Paper
                elevation={0}
                sx={{ 
                  p: '2px 4px', 
                  display: 'flex', 
                  alignItems: 'center', 
                  width: '100%',
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
                  placeholder="Search for departments by name or abbreviation"
                  value={departmentFilter}
                  onChange={(e) => setDepartmentFilter(e.target.value)}
                  autoFocus
                />
                {departmentFilter && (
                  <IconButton sx={{ p: '10px' }} aria-label="clear" onClick={() => setDepartmentFilter('')}>
                    <Close sx={{ color: '#64748B', fontSize: 18 }} />
                  </IconButton>
                )}
              </Paper>
            </Box>
            <Box sx={{ flex: 1, overflow: 'auto', p: 0 }}>
    {filteredDepartments.length === 0 ? (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}>
        <Error sx={{ fontSize: 40, color: '#94A3B8', mb: 1 }} />
        <Typography variant="body1" color="#1E293B" fontWeight={500}>
          No departments found
        </Typography>
        <Typography variant="body2" color="#64748B">
          Try a different search term
        </Typography>
      </Box>
    ) : (
      <Box>
        {filteredDepartments.map((dept) => (
          <Box
            key={dept.departmentId}
            sx={{
              p: 2,
              cursor: 'pointer',
              '&:hover': { bgcolor: '#F8FAFC' },
              borderBottom: '1px solid #E2E8F0'
            }}
            onClick={() => handleDepartmentSelect(dept)}
          >
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Box>
                <Typography variant="body1" fontWeight={500} color="#1E293B">
                  {dept.name}
                </Typography>
                <Typography variant="body2" color="#64748B">
                  {dept.abbreviation} • {dept.numberOfFaculty} Faculty
                </Typography>
              </Box>
              <Chip
                label={`${dept.offeredPrograms?.length || 0} Programs`}
                size="small"
                sx={{
                  bgcolor: '#E0F2FE',
                  color: '#0369A1',
                  fontWeight: 500,
                  fontSize: '0.75rem'
                }}
              />
            </Box>
          </Box>
        ))}
      </Box>
    )}
  </Box>

  {/* Upload Modal */}
  <Dialog
    open={showUploadModal}
    onClose={() => setShowUploadModal(false)}
    aria-labelledby="upload-dialog-title"
  >
    <DialogTitle id="upload-dialog-title">
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        Upload Events CSV
        <IconButton onClick={() => setShowUploadModal(false)}>
          <Close />
        </IconButton>
      </Box>
    </DialogTitle>
    <DialogContent>
      <DialogContentText sx={{ mb: 2 }}>
        Upload a CSV file containing event data. The file should include columns for event name,
        department ID, date, duration, and status.
      </DialogContentText>
      <Box
        sx={{
          border: '2px dashed #E2E8F0',
          borderRadius: 1,
          p: 3,
          textAlign: 'center'
        }}
      >
        {uploadedFile ? (
          <Box>
            <Typography variant="body1" color="#0288d1" fontWeight={500}>
              {uploadedFile.name}
            </Typography>
            <Typography variant="body2" color="#64748B">
              {(uploadedFile.size / 1024).toFixed(2)} KB
            </Typography>
            <Button
              onClick={() => setUploadedFile(null)}
              startIcon={<Delete />}
              sx={{ mt: 1, color: '#EF4444' }}
            >
              Remove
            </Button>
          </Box>
        ) : (
          <Box>
            <Upload sx={{ fontSize: 40, color: '#94A3B8', mb: 1 }} />
            <Typography variant="body1" color="#1E293B" fontWeight={500}>
              Drag & Drop or Click to Upload
            </Typography>
            <Typography variant="body2" color="#64748B" sx={{ mb: 2 }}>
              Supports CSV files up to 5MB
            </Typography>
            <Button
              component="label"
              variant="outlined"
              sx={{
                borderColor: '#0288d1',
                color: '#0288d1',
                '&:hover': {
                  borderColor: '#0277bd',
                  bgcolor: 'rgba(2, 136, 209, 0.04)',
                }
              }}
            >
              Select File
              <input
                type="file"
                hidden
                accept=".csv"
                onChange={handleFileChange}
              />
            </Button>
          </Box>
        )}
      </Box>
    </DialogContent>
    <DialogActions sx={{ p: 2, borderTop: '1px solid #E2E8F0' }}>
      <Button 
        onClick={() => setShowUploadModal(false)}
        sx={{ color: '#64748B' }}
      >
        Cancel
      </Button>
      <Button 
        onClick={handleUploadSubmit}
        variant="contained"
        disabled={!uploadedFile}
        sx={{
          bgcolor: '#0288d1',
          '&:hover': {
            bgcolor: '#0277bd',
          },
          textTransform: 'none',
          fontWeight: 500
        }}
      >
        Upload and Process
      </Button>
    </DialogActions>
  </Dialog>

  {/* Delete Confirmation Dialog */}
  <Dialog
    open={deleteDialogOpen}
    onClose={closeDeleteDialog}
    aria-labelledby="delete-dialog-title"
  >
    <DialogTitle id="delete-dialog-title">
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <Delete sx={{ color: '#EF4444' }} />
        Confirm Delete Event
      </Box>
    </DialogTitle>
    <DialogContent>
      <DialogContentText>
        Are you sure you want to delete the event <strong>{eventToDelete?.eventName}</strong>? This action cannot be undone.
      </DialogContentText>
    </DialogContent>
    <DialogActions sx={{ p: 2, borderTop: '1px solid #E2E8F0' }}>
      <Button 
        onClick={closeDeleteDialog}
        sx={{ color: '#64748B' }}
      >
        Cancel
      </Button>
      <Button 
        onClick={confirmDelete}
        variant="contained"
        sx={{
          bgcolor: '#EF4444',
          '&:hover': {
            bgcolor: '#DC2626',
          },
          textTransform: 'none',
          fontWeight: 500
        }}
      >
        Delete Event
      </Button>
    </DialogActions>
  </Dialog>

  {/* Snackbar Notifications */}
  <Snackbar
    open={snackbar.open}
    autoHideDuration={6000}
    onClose={handleCloseSnackbar}
    anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
  >
    <Alert 
      onClose={handleCloseSnackbar} 
      severity={snackbar.severity} 
      sx={{ width: '100%' }}
      variant="filled"
    >
      {snackbar.message}
    </Alert>
  </Snackbar>
  </Box>
  </Modal>

  {/* Main component closing tags */}
  </Box>

  );
  }