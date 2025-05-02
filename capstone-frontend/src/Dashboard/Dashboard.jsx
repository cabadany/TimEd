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
  Chip
} from '@mui/material';
import {
  Search,
  Settings,
  Notifications,
  AccountTree,
  FilterList,
  Visibility,
  Home,
  Event,
  People,
  ChevronLeft,
  ChevronRight,
  Close,
  CalendarToday,
  AccessTime,
  Group,
  Logout
} from '@mui/icons-material';
import './dashboard.css';

export default function Dashboard() {
  const navigate = useNavigate();
  const location = useLocation();
  
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [activeTab, setActiveTab] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [showModal, setShowModal] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [departments, setDepartments] = useState([]);
  // Filter menu state
  const [filterAnchorEl, setFilterAnchorEl] = useState(null);
  const filterMenuOpen = Boolean(filterAnchorEl);
  const [activeFilter, setActiveFilter] = useState('');
  
  // Avatar dropdown menu state
  const [avatarAnchorEl, setAvatarAnchorEl] = useState(null);
  const avatarMenuOpen = Boolean(avatarAnchorEl);
  useEffect(() => {
    fetchDepartments();
  }, []);
  const fetchDepartments = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/departments');
      setDepartments(response.data);
    } catch (error) {
      console.error('Error fetching departments:', error);
    }
  };
  // Fetch events from the backend API
  useEffect(() => {
    const fetchEvents = async () => {
      try {
        setLoading(true);
        const response = await axios.get('http://localhost:8080/api/events/getAll');
  
        const formattedEvents = response.data.map(event => {
          // Handle Firebase timestamp - improved parser that checks various timestamp formats
          let formattedDate = 'Invalid Date';
          
          if (event.date) {
            // Handle different potential timestamp formats
            if (typeof event.date === 'string') {
              // If date is already a string, try to parse it directly
              const dateObj = new Date(event.date);
              if (!isNaN(dateObj.getTime())) {
                formattedDate = dateObj.toLocaleString('en-PH', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                  hour: 'numeric',
                  minute: '2-digit',
                  hour12: true,
                  timeZone: 'Asia/Manila'
                });
              }
            } else if (typeof event.date === 'object') {
              // Check for Firestore timestamp object format
              if (event.date.seconds && event.date.nanoseconds) {
                // Classic Firestore timestamp
                const dateObj = new Date(event.date.seconds * 1000 + event.date.nanoseconds / 1000000);
                formattedDate = dateObj.toLocaleString('en-PH', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                  hour: 'numeric',
                  minute: '2-digit',
                  hour12: true,
                  timeZone: 'Asia/Manila'
                });
              } else if (event.date.toDate && typeof event.date.toDate === 'function') {
                // Firestore timestamp with toDate method
                const dateObj = event.date.toDate();
                formattedDate = dateObj.toLocaleString('en-PH', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                  hour: 'numeric',
                  minute: '2-digit',
                  hour12: true,
                  timeZone: 'Asia/Manila'
                });
              } else if (event.date._seconds && event.date._nanoseconds) {
                // Serialized Firestore timestamp
                const dateObj = new Date(event.date._seconds * 1000 + event.date._nanoseconds / 1000000);
                formattedDate = dateObj.toLocaleString('en-PH', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                  hour: 'numeric',
                  minute: '2-digit',
                  hour12: true,
                  timeZone: 'Asia/Manila'
                });
              }
            }
          }
  
          return {
            id: event.eventId || `#${Math.floor(Math.random() * 90000000) + 10000000}`,
            name: event.eventName || 'Unnamed Event',
            duration: event.duration || '0 mins',
            date: formattedDate,
            status: event.status || 'Unknown',
            createdBy: event.createdBy || 'Unknown',
            departmentId: event.departmentId || null // Added departmentId here
          };
        });
  
        setEvents(formattedEvents);
        setLoading(false);
      } catch (error) {
        console.error('Failed to fetch events:', error);
        setError('Failed to load events. Please try again later.');
        setLoading(false);
      }
    };
  
    fetchEvents();
    fetchDepartments();
  }, []);

  const handleViewEvent = (event) => {
    navigate(`/attendance/${event.id}`);
  };

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  // Navigation handlers
  const handleNavigateToEvent = () => {
    navigate('/event');
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
    // Additional logic to actually apply the filter would go here
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

  // Function to get status color
  const getStatusColor = (status = 'Unknown') => {
    switch(status) {
      case 'Starting':
        return '#10B981';
      case 'Ongoing':
        return '#0288d1';
      case 'Ended':
        return '#EF4444';
      default:
        return '#64748B';
    }
  };

  // Function to filter events based on the active tab
  const getFilteredEvents = () => {
    if (activeTab === 0) {
      // All events
      return events;
    } else if (activeTab === 1) {
      // Starting events
      return events.filter(event => event.status === 'Starting');
    } else if (activeTab === 2) {
      // Ongoing events
      return events.filter(event => event.status === 'Ongoing');
    } else if (activeTab === 3) {
      // Ended events
      return events.filter(event => event.status === 'Ended');
    }
    return events;
  };
  const getDepartmentName = (id) => {
    const dept = departments.find(d => d.departmentId === id);
    return dept ? dept.name : 'Unknown Department';
  };

  const filteredEvents = getFilteredEvents();

  // Pagination logic
  const eventsPerPage = 5;
  const totalPages = Math.ceil(filteredEvents.length / eventsPerPage);
  const indexOfLastEvent = currentPage * eventsPerPage;
  const indexOfFirstEvent = indexOfLastEvent - eventsPerPage;
  const currentEvents = filteredEvents.slice(indexOfFirstEvent, indexOfLastEvent);

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
            Dashboard
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

        {/* Dashboard Content */}
        <Box sx={{ 
          p: 3, 
          flex: 1, 
          overflow: 'auto', 
          bgcolor: '#FFFFFF' 
        }}>
          {/* Date Range */}
          <Box sx={{ mb: 4, display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body1" sx={{ color: '#475569' }}>
              Date from
            </Typography>
            <TextField
              variant="standard"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              sx={{ 
                width: 200,
                '& .MuiInputBase-root': {
                  fontSize: '0.875rem'
                },
                '& .MuiInput-underline:before': {
                  borderBottomColor: '#0288d1'
                },
                '& .MuiInput-underline:after': {
                  borderBottomColor: '#0288d1'
                }
              }}
              InputProps={{
                disableUnderline: false,
                sx: { paddingBottom: 0 }
              }}
            />
            <Typography variant="body1" sx={{ color: '#475569' }}>
              to
            </Typography>
            <TextField
              variant="standard"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              sx={{
                width: 200,
                '& .MuiInputBase-root': {
                  fontSize: '0.875rem'
                },
                '& .MuiInput-underline:before': {
                  borderBottomColor: '#0288d1'
                },
                '& .MuiInput-underline:after': {
                  borderBottomColor: '#0288d1'
                }
              }}
              InputProps={{
                disableUnderline: false,
                sx: { paddingBottom: 0 }
              }}
            />
          </Box>

          {/* Events Count */}
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
            <Typography variant="body1" fontWeight="medium" sx={{ color: '#475569' }}>
              Number of Events: <Box component="span" sx={{ color: '#0288d1', fontWeight: 'bold' }}>{filteredEvents.length}</Box>
            </Typography>
          </Box>

          {/* Tabs */}
          <Box sx={{ mb: 3, borderBottom: '1px solid #E2E8F0' }}>
            <Tabs 
              value={activeTab} 
              onChange={handleTabChange}
              sx={{
                '& .MuiTabs-indicator': {
                  backgroundColor: '#0288d1',
                  height: 3
                },
                '& .MuiTab-root': {
                  textTransform: 'uppercase',
                  fontWeight: 600,
                  color: '#64748B',
                  fontSize: 14,
                  '&.Mui-selected': {
                    color: '#0288d1'
                  }
                }
              }}
            >
              <Tab label="All Events" />
              <Tab label="Starting" />
              <Tab label="Ongoing" />
              <Tab label="Ended" />
            </Tabs>
          </Box>

          {/* Events Table with Added Status Column */}
          <TableContainer 
            component={Paper} 
            sx={{ 
              mb: 3, 
              boxShadow: 'none', 
              border: '1px solid #E2E8F0',
              borderRadius: '4px',
              overflow: 'hidden',
              maxHeight: 'calc(100vh - 300px)'
            }}
          >
            <Table stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Event Name</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Department</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Event ID</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Duration</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Date</TableCell>
                  <TableCell sx={{ width: 60, bgcolor: '#F8FAFC' }}></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                    <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                    <CircularProgress size={24} sx={{ color: '#0288d1' }} />
                  </TableCell>
                ) : error ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center" sx={{ color: '#EF4444' }}>{error}</TableCell>
                  </TableRow>
                ) : currentEvents.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">No events found</TableCell>
                  </TableRow>
                ) : (
                  currentEvents.map((event, index) => (
                    <TableRow key={index} sx={{ '&:hover': { bgcolor: '#F8FAFC' } }}>
                      <TableCell sx={{ color: '#1E293B' }}>{event.name}</TableCell>
                      <TableCell sx={{ color: '#1E293B' }}>{getDepartmentName(event.departmentId)}</TableCell>
                      <TableCell sx={{ color: '#64748B' }}>{event.id}</TableCell>
                      <TableCell sx={{ color: '#64748B' }}>{event.duration}</TableCell>
                      <TableCell>
                        <Chip 
                          label={event.status}
                          size="small"
                          sx={{
                            backgroundColor: `${getStatusColor(event.status)}10`, // 10% opacity
                            color: getStatusColor(event.status),
                            fontWeight: 500,
                            fontSize: '0.75rem',
                            height: 24,
                            borderRadius: '4px'
                          }}
                        />
                      </TableCell>
                      <TableCell sx={{ color: '#64748B' }}>{event.date}</TableCell>
                      <TableCell>
                        <IconButton 
                          onClick={() => handleViewEvent(event)}
                          sx={{ color: '#0288d1' }}
                        >
                          <Visibility />
                        </IconButton>
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

      {/* Event Details Modal */}
      <Modal
        open={showModal}
        onClose={() => setShowModal(false)}
        aria-labelledby="event-details-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 800,
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          overflow: 'hidden'
        }}>
          <Box sx={{ p: 3, display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #E2E8F0' }}>
            <Typography variant="h6" fontWeight="600">
              {selectedEvent?.name} Details
            </Typography>
            <IconButton onClick={() => setShowModal(false)}>
              <Close />
            </IconButton>
          </Box>
          <Box sx={{ p: 3 }}>
            <Grid container spacing={3} sx={{ mb: 3 }}>
              
              <Grid item xs={6}>
                <Typography variant="caption" color="#64748B">Event ID</Typography>
                <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>{selectedEvent?.id}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="caption" color="#64748B">Date</Typography>
                <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>{selectedEvent?.date}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="caption" color="#64748B">Duration</Typography>
                <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>{selectedEvent?.duration}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="caption" color="#64748B">Status</Typography>
                <Box sx={{ mt: 0.5 }}>
                  <Chip 
                    label={selectedEvent?.status}
                    size="small"
                    sx={{
                      backgroundColor: selectedEvent ? `${getStatusColor(selectedEvent.status)}10` : '#f1f5f9',
                      color: selectedEvent ? getStatusColor(selectedEvent.status) : '#64748B',
                      fontWeight: 500,
                      fontSize: '0.75rem',
                      height: 24,
                      borderRadius: '4px'
                    }}
                  />
                </Box>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="caption" color="#64748B">Organizer</Typography>
                <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>{selectedEvent?.createdBy || 'Unknown'}</Typography>
              </Grid>
            </Grid>
            
            {/* Sample Graph */}
            <Card sx={{ mb: 2, boxShadow: 'none', border: '1px solid #E2E8F0', borderRadius: '4px' }}>
              <CardContent>
                <Typography variant="body2" fontWeight="600" sx={{ mb: 2, color: '#1E293B' }}>
                  Attendance Statistics
                </Typography>
                <Box sx={{ height: 200, display: 'flex', alignItems: 'flex-end', gap: 1 }}>
                  <Box sx={{ flex: 1, bgcolor: '#0288d1', height: '40%', borderRadius: '4px 4px 0 0' }}></Box>
                  <Box sx={{ flex: 1, bgcolor: '#0288d1', height: '60%', borderRadius: '4px 4px 0 0' }}></Box>
                  <Box sx={{ flex: 1, bgcolor: '#0288d1', height: '70%', borderRadius: '4px 4px 0 0' }}></Box>
                  <Box sx={{ flex: 1, bgcolor: '#0288d1', height: '55%', borderRadius: '4px 4px 0 0' }}></Box>
                  <Box sx={{ flex: 1, bgcolor: '#0288d1', height: '45%', borderRadius: '4px 4px 0 0' }}></Box>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                  <Typography variant="caption" color="#64748B">Day 1</Typography>
                  <Typography variant="caption" color="#64748B">Day 2</Typography>
                  <Typography variant="caption" color="#64748B">Day 3</Typography>
                  <Typography variant="caption" color="#64748B">Day 4</Typography>
                  <Typography variant="caption" color="#64748B">Day 5</Typography>
                </Box>
              </CardContent>
            </Card>
          </Box>
          <Box sx={{ p: 2, bgcolor: '#F8FAFC', display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid #E2E8F0' }}>
            <Button 
              variant="contained" 
              onClick={() => setShowModal(false)}
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
          </Box>
        </Box>
      </Modal>
    </Box>
  );
}