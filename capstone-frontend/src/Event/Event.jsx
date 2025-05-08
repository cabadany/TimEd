import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { parse, format } from 'date-fns';

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
  Alert,
  FormControlLabel,
  Switch,
  Grid,
  Card,
  CardContent,
  CardActions,
  Divider
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
  Error,
  Edit,
  CheckCircle,
  Cancel,
  MoreVert
} from '@mui/icons-material';
import './Event.css';
import NotificationSystem from '../components/NotificationSystem';

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
  const [ongoingEvents, setOngoingEvents] = useState([]);
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
  
  // State for edit event dialog
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [eventToEdit, setEventToEdit] = useState(null);
  const [editedStatus, setEditedStatus] = useState('');
  
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

  // Check event status based on current date
  
useEffect(() => {
  const currentDate = new Date();
  
  // Update event statuses based on current date
  if (events.length > 0) {
    // Identify ongoing events
    const ongoing = events.filter(event => {
      // Parse the event date correctly with timezone information
      // Using a more robust approach for timezone parsing
      let eventDate;
      if (event.date.includes('UTC+8') || event.date.includes('+09:00')) {
        // Convert the string to ISO 8601 format with proper timezone
        const dateStr = event.date.replace('UTC+8', '+03:00');
        eventDate = new Date(dateStr);
      } else {
        // If no timezone info, assume local timezone
        eventDate = new Date(event.date);
      }
      
      // Calculate event end time based on duration
      const [hours, minutes, seconds] = event.duration.split(':').map(Number);
      const eventEndTime = new Date(eventDate);
      eventEndTime.setHours(eventEndTime.getHours() + hours);
      eventEndTime.setMinutes(eventEndTime.getMinutes() + minutes);
      eventEndTime.setSeconds(eventEndTime.getSeconds() + seconds);
      
      // Event is ongoing if current time is between start and end time
      return eventDate <= currentDate && currentDate <= eventEndTime && event.status !== 'Cancelled';
    });
    
    setOngoingEvents(ongoing);
    
    // Check for events that should be marked as ended or ongoing
    events.forEach(event => {
      // Parse the event date with proper timezone handling
      let eventDate;
      if (event.date.includes('UTC+9') || event.date.includes('+09:00')) {
        const dateStr = event.date.replace('UTC+9', '+09:00');
        eventDate = new Date(dateStr);
      } else {
        eventDate = new Date(event.date);
      }
      
      // Calculate event end time
      const [hours, minutes, seconds] = event.duration.split(':').map(Number);
      const eventEndTime = new Date(eventDate);
      eventEndTime.setHours(eventEndTime.getHours() + hours);
      eventEndTime.setMinutes(eventEndTime.getMinutes() + minutes);
      eventEndTime.setSeconds(eventEndTime.getSeconds() + seconds);
      
      // If event has passed end time but still marked as ongoing or upcoming
      if (currentDate > eventEndTime && (event.status === 'Ongoing' || event.status === 'Upcoming')) {
        // Update event status to Ended
        updateEventStatus(event.eventId, 'Ended');
      }
      
      // If event has started but is still marked as upcoming
      if (currentDate >= eventDate && currentDate <= eventEndTime && event.status === 'Upcoming') {
        // Update event status to Ongoing
        updateEventStatus(event.eventId, 'Ongoing');
      }
    });
  }
}, [events]);

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
    
    // Ensure the date is correctly formatted with timezone information
    const formattedDate = new Date(date).toISOString();
    
    const eventData = {
      eventName,
      departmentId,
      date: formattedDate, // Use ISO format to preserve exact time
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
  
  // Function to parse the custom date string
  const parseDateString = (dateString) => {
    // Example input: "May 3, 2025 at 8:05:00 AM UTC+8"
    const regex = /(\w+ \d{1,2}, \d{4}) at (\d{1,2}:\d{2}:\d{2}) (AM|PM) UTC([+-]\d{1,2})/;
    const match = dateString.match(regex);
    
    if (!match) return null;
  
    const datePart = match[1]; // "May 3, 2025"
    const timePart = match[2]; // "8:05:00"
    const ampm = match[3]; // "AM"
    const timezoneOffset = match[4]; // "+8"
  
    // Combine parts into a single string
    const combinedDateString = `${datePart} ${timePart} ${ampm}`;
  
    // Parse the date string into a Date object
    const parsedDate = parse(combinedDateString, 'MMMM d, yyyy h:mm:ss a', new Date());
  
    // Adjust for timezone offset manually
    const offsetHours = parseInt(timezoneOffset, 10);
    const utcDate = new Date(parsedDate.getTime() - (offsetHours * 60 * 60 * 1000));
  
    return utcDate;
  };
  

  const deleteEvent = async (eventId) => {
    setLoading(true);
    try {
      await axios.delete(`http://localhost:8080/api/events/deleteEvent/${eventId}`);
      
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

  const updateEventStatus = async (eventId, newStatus) => {
    try {
      const eventToUpdate = events.find(e => e.eventId === eventId);
      if (!eventToUpdate) return;
      
      const updatedEvent = {
        ...eventToUpdate,
        status: newStatus
      };
      
      await axios.put(`http://localhost:8080/api/events/update/${eventId}`, updatedEvent);
      
      // Update local state
      setEvents(events.map(event => 
        event.eventId === eventId ? { ...event, status: newStatus } : event
      ));
      
      showSnackbar(`Event status updated to ${newStatus}`, 'success');
    } catch (error) {
      console.error('Error updating event status:', error);
      showSnackbar('Failed to update event status', 'error');
    }
  };

  const saveEditedEvent = async () => {
    if (!eventToEdit) return;
    
    try {
      const updatedEvent = {
        ...eventToEdit,
        status: editedStatus
      };
      
      await axios.put(`http://localhost:8080/api/events/update/${eventToEdit.eventId}`, updatedEvent);
      
      // Update local state
      setEvents(events.map(event => 
        event.eventId === eventToEdit.eventId ? { ...event, status: editedStatus } : event
      ));
      
      showSnackbar('Event updated successfully', 'success');
      setEditDialogOpen(false);
    } catch (error) {
      console.error('Error updating event:', error);
      showSnackbar('Failed to update event', 'error');
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

  // Edit event handlers
  const openEditDialog = (event) => {
    setEventToEdit(event);
    setEditedStatus(event.status);
    setEditDialogOpen(true);
  };
  
  const closeEditDialog = () => {
    setEditDialogOpen(false);
    setEventToEdit(null);
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
      // Check if the dateString includes time information (from ISO format)
      const hasTimeInfo = dateString.includes('T');
      
      // Create date from the string - preserving the exact time
      const date = new Date(dateString);
      
      // Use local timezone to ensure we respect the input time
      const options = {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      };
      
      // Format date with the proper locale options
      return date.toLocaleString('en-US', options);
    } catch (e) {
      console.error("Date parsing error:", e);
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
                fontWeight: 500,
                mr: 1.5,
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

        {/* Event Content - Wrapped in a scrollable container */}
        <Box sx={{ 
          flex: 1, 
          overflow: 'auto', /* This makes the content scrollable */
          bgcolor: '#FFFFFF'
        }}>
          <Box sx={{ 
            p: 3, 
            display: 'flex',
            flexDirection: 'column',
            gap: 3
          }}>
            {/* Ongoing Event Dashboard */}
            <Typography variant="h6" fontWeight="600" color="#1E293B">
              Ongoing Events Dashboard
            </Typography>
            
            <Paper 
              elevation={0} 
              sx={{ 
                p: 3, 
                borderRadius: '8px', 
                border: '1px solid #E2E8F0'
              }}
            >
              {ongoingEvents.length === 0 ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 2 }}>
                  <CalendarToday sx={{ fontSize: 40, color: '#94A3B8', mb: 1 }} />
                  <Typography variant="body1" color="#1E293B" fontWeight={500}>
                    No ongoing events
                  </Typography>
                  <Typography variant="body2" color="#64748B">
                    All scheduled events will appear here when they're in progress
                  </Typography>
                </Box>
              ) : (
                <Grid container spacing={3}>
                  {ongoingEvents.map(event => (
                    <Grid item xs={12} md={6} lg={4} key={event.eventId}>
                      <Card 
                        elevation={0} 
                        sx={{ 
                          border: '1px solid #E2E8F0', 
                          borderRadius: '8px',
                          height: '100%',
                          display: 'flex',
                          flexDirection: 'column'
                        }}
                      >
                        <CardContent sx={{ flex: 1 }}>
                          <Box sx={{ 
                            mb: 2, 
                            display: 'flex', 
                            justifyContent: 'space-between',
                            alignItems: 'flex-start'
                          }}>
                            <Typography variant="h6" component="h3" sx={{ fontWeight: 600 }}>
                              {event.eventName}
                            </Typography>
                            <Chip 
                              label="Ongoing" 
                              size="small"
                              sx={{ 
                                bgcolor: '#E0F2FE',
                                color: '#0369A1',
                                fontWeight: 500,
                                fontSize: '0.75rem'
                              }} 
                            />
                          </Box>
                          
                          <Typography variant="body2" color="#64748B" gutterBottom>
                            <strong>Department:</strong> {getDepartmentName(event.departmentId)}
                          </Typography>
                          
                          <Typography variant="body2" color="#64748B" gutterBottom>
                            <strong>Started:</strong> {formatDate(event.date)}
                          </Typography>
                          
                          <Typography variant="body2" color="#64748B">
                            <strong>Duration:</strong> {event.duration}
                          </Typography>
                        </CardContent>
                        
                        <Divider />
                        
                        <CardActions sx={{ justifyContent: 'space-between', p: 2 }}>
                          <Button
                            size="small"
                            startIcon={<Edit />}
                            onClick={() => openEditDialog(event)}
                            sx={{ color: '#0288d1' }}
                          >
                            Update Status
                          </Button>
                          
                          <Button
                            size="small"
                            color="error"
                            startIcon={<Cancel />}
                            onClick={() => updateEventStatus(event.eventId, 'Cancelled')}
                          >
                            Cancel Event
                          </Button>
                        </CardActions>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </Paper>

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
                    Date & Time *
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    type="datetime-local"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    // No min attribute to prevent any browser-based default time setting
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
                    Duration * (format: 0:00:00)
                  </Typography>
                  <TextField
                    fullWidth
                    variant="outlined"
                    placeholder="0:00:00"
                    value={duration}
                    onChange={(e) => {
                      // Validate and format as 0:00:00
                      const input = e.target.value;
                      const timePattern = /^(\d+):([0-5]?\d):([0-5]?\d)$/;
                      
                      // Either accept valid format or keep previous value
                      if (input === '' || timePattern.test(input)) {
                        setDuration(input);
                      }
                    }}
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
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 4 }}>
                <Button
                  variant="outlined"
                  onClick={resetForm}
                  sx={{
                    borderColor: '#CBD5E1',
                    color: '#64748B',
                    fontWeight: 500,
                    '&:hover': {
                      borderColor: '#94A3B8',
                      bgcolor: 'rgba(148, 163, 184, 0.04)',
                    },
                    px: 3
                  }}
                >
                  Reset
                </Button>

                <Button
                  variant="contained"
                  onClick={handleAddEvent}
                  disabled={loading}
                  startIcon={loading ? <CircularProgress size={20} /> : <Add />}
                  sx={{
                    bgcolor: '#0288d1',
                    fontWeight: 500,
                    '&:hover': {
                      bgcolor: '#0277bd',
                    },
                    px: 3
                  }}
                >
                  Add Event
                </Button>
              </Box>
            </Paper>

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="h6" fontWeight="600" color="#1E293B">
                All Events
              </Typography>
              <Button
                variant="outlined"
                startIcon={<Upload />}
                onClick={handleUploadClick}
                sx={{
                  borderColor: '#CBD5E1',
                  color: '#64748B',
                  fontWeight: 500,
                  textTransform: 'none',
                  '&:hover': {
                    borderColor: '#94A3B8',
                    bgcolor: 'rgba(148, 163, 184, 0.04)',
                  },
                }}
              >
                Import CSV
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
                  <TableHead>
                    <TableRow sx={{ bgcolor: '#F8FAFC' }}>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Event Name</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Department</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Date</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Duration</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {loading ? (
                      <TableRow>
                        <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                          <CircularProgress size={30} />
                        </TableCell>
                      </TableRow>
                    ) : events.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} align="center" sx={{ py: 3 }}>
                          <Typography variant="body1" color="#64748B">
                            No events found
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ) : (
                      events.map((event) => (
                        <TableRow 
                          key={event.eventId}
                          sx={{ 
                            '&:hover': { bgcolor: '#F8FAFC' },
                            borderBottom: '1px solid #E2E8F0'
                          }}
                        >
                          <TableCell sx={{ py: 2 }}>{event.eventName}</TableCell>
                          <TableCell>{getDepartmentName(event.departmentId)}</TableCell>
                          <TableCell>{formatDate(event.date)}</TableCell>
                          <TableCell>{event.duration}</TableCell>
                          <TableCell>
                            <Chip
                              label={event.status}
                              size="small"
                              sx={{
                                bgcolor: 
                                  event.status === 'Ongoing' ? '#E0F2FE' :
                                  event.status === 'Upcoming' ? '#DCFCE7' :
                                  event.status === 'Ended' ? '#F1F5F9' :
                                  event.status === 'Cancelled' ? '#FEE2E2' : '#F1F5F9',
                                color: 
                                  event.status === 'Ongoing' ? '#0369A1' :
                                  event.status === 'Upcoming' ? '#166534' :
                                  event.status === 'Ended' ? '#475569' :
                                  event.status === 'Cancelled' ? '#B91C1C' : '#475569',
                                fontWeight: 500,
                                fontSize: '0.75rem'
                              }}
                            />
                          </TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', gap: 1 }}>
                              <IconButton 
                                size="small" 
                                onClick={() => openEditDialog(event)}
                                sx={{ color: '#64748B' }}
                              >
                                <Edit fontSize="small" />
                              </IconButton>
                              <IconButton 
                                size="small" 
                                onClick={() => openDeleteDialog(event)} 
                                sx={{ color: '#64748B' }}
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
      </Box>

      {/* Department Selection Modal */}
      <Dialog
        open={showDepartmentModal}
        onClose={() => setShowDepartmentModal(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: { borderRadius: '8px' }
        }}
      >
        <DialogTitle sx={{ borderBottom: '1px solid #E2E8F0', py: 2 }}>
          Select Department
        </DialogTitle>
        <DialogContent sx={{ pt: 3, pb: 1 }}>
          <TextField
            autoFocus
            margin="dense"
            label="Search Departments"
            type="text"
            fullWidth
            variant="outlined"
            value={departmentFilter}
            onChange={(e) => setDepartmentFilter(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
            }}
            sx={{
              mb: 2,
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
          
          <Box sx={{ maxHeight: '300px', overflow: 'auto', mb: 2 }}>
            {filteredDepartments.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 3 }}>
                <Typography variant="body2" color="#64748B">
                  No departments found
                </Typography>
              </Box>
            ) : (
              filteredDepartments.map((dept) => (
                <Box
                  key={dept.departmentId}
                  sx={{
                    py: 1.5,
                    px: 2,
                    borderRadius: '4px',
                    cursor: 'pointer',
                    '&:hover': {
                      bgcolor: '#F1F5F9',
                    },
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    borderBottom: '1px solid #E2E8F0',
                  }}
                  onClick={() => handleDepartmentSelect(dept)}
                >
                  <Box>
                    <Typography variant="body1" color="#1E293B" fontWeight={500}>
                      {dept.name}
                    </Typography>
                    <Typography variant="body2" color="#64748B">
                      {dept.abbreviation}
                    </Typography>
                  </Box>
                  <Button
                    variant="text"
                    size="small"
                    sx={{ color: '#0288d1' }}
                  >
                    Select
                  </Button>
                </Box>
              ))
            )}
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid #E2E8F0' }}>
          <Button 
            onClick={() => setShowDepartmentModal(false)}
            sx={{ color: '#64748B' }}
          >
            Cancel
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={closeDeleteDialog}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          sx: { borderRadius: '8px' }
        }}
      >
        <DialogTitle sx={{ pb: 1 }}>
          Delete Event
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the event <strong>{eventToDelete?.eventName}</strong>? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button 
            onClick={closeDeleteDialog}
            sx={{ color: '#64748B' }}
          >
            Cancel
          </Button>
          <Button 
            onClick={confirmDelete} 
            variant="contained" 
            color="error"
            startIcon={<Delete />}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Event Dialog */}
      <Dialog
        open={editDialogOpen}
        onClose={closeEditDialog}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: { borderRadius: '8px' }
        }}
      >
        <DialogTitle sx={{ borderBottom: '1px solid #E2E8F0', py: 2 }}>
          Edit Event Status
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography variant="body2" color="#64748B" sx={{ mb: 3 }}>
            Update the status for event: <strong>{eventToEdit?.eventName}</strong>
          </Typography>
          
          <FormControl fullWidth>
            <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
              Status *
            </Typography>
            <Select
              value={editedStatus}
              onChange={(e) => setEditedStatus(e.target.value)}
              displayEmpty
              sx={{
                '& .MuiOutlinedInput-notchedOutline': {
                  borderColor: '#E2E8F0',
                },
                '&:hover .MuiOutlinedInput-notchedOutline': {
                  borderColor: '#CBD5E1',
                },
                '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                  borderColor: '#0288d1',
                },
              }}
            >
              <MenuItem value="Upcoming">Upcoming</MenuItem>
              <MenuItem value="Ongoing">Ongoing</MenuItem>
              <MenuItem value="Ended">Ended</MenuItem>
              <MenuItem value="Cancelled">Cancelled</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid #E2E8F0', mt: 2 }}>
          <Button 
            onClick={closeEditDialog} 
            sx={{ color: '#64748B' }}
          >
            Cancel
          </Button>
          <Button 
            onClick={saveEditedEvent} 
            variant="contained"
            sx={{
              bgcolor: '#0288d1',
              '&:hover': {
                bgcolor: '#0277bd',
              },
            }}
          >
            Save Changes
          </Button>
        </DialogActions>
      </Dialog>

      {/* Upload CSV Modal */}
      <Modal
        open={showUploadModal}
        onClose={() => setShowUploadModal(false)}
        aria-labelledby="upload-csv-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 400,
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: '8px',
          p: 4,
        }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" component="h2">
              Import Events from CSV
            </Typography>
            <IconButton onClick={() => setShowUploadModal(false)} size="small">
              <Close />
            </IconButton>
          </Box>
          
          <Typography variant="body2" color="#64748B" sx={{ mb: 3 }}>
            Upload a CSV file with event details to bulk import events. The CSV should include columns for name, department, date, and duration.
          </Typography>
          
          <Button
            variant="outlined"
            component="label"
            fullWidth
            startIcon={<Upload />}
            sx={{
              py: 1.5,
              borderStyle: 'dashed',
              borderColor: '#CBD5E1',
              color: '#64748B',
              '&:hover': {
                borderColor: '#94A3B8',
                bgcolor: 'rgba(148, 163, 184, 0.04)',
              },
            }}
          >
            {uploadedFile ? uploadedFile.name : 'Browse files...'}
            <input
              type="file"
              accept=".csv"
              hidden
              onChange={handleFileChange}
            />
          </Button>
          
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 3 }}>
            <Button
              onClick={() => setShowUploadModal(false)}
              sx={{ color: '#64748B' }}
            >
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={handleUploadSubmit}
              disabled={!uploadedFile}
              sx={{
                bgcolor: '#0288d1',
                '&:hover': {
                  bgcolor: '#0277bd',
                },
              }}
            >
              Upload
            </Button>
          </Box>
        </Box>
      </Modal>

      {/* Snackbar for notifications */}
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