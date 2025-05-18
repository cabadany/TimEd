import { useState, useEffect, useRef } from 'react';
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
  Modal,
  Grid,
  Card,
  CardContent,
  TextField,
  Chip,
  Divider,
  List,
  ListItem,
  Tooltip,
  Skeleton,
  InputAdornment
} from '@mui/material';
import {
  Visibility,
  ChevronLeft,
  ChevronRight,
  Close,
  CalendarToday,
  AccessTime,
  Group,
  ViewList,
  Event,
  EventNote,
  Search
} from '@mui/icons-material';
import './dashboard.css';
import { useTheme } from '../contexts/ThemeContext';
import EventCalendar from '../components/EventCalendar';
import CertificateEditor from '../components/CertificateEditor';

// Skeleton loading components
const DashboardSkeleton = () => (
  <>
    <Box sx={{ mb: 4 }}>
      <Skeleton variant="text" width={150} height={30} sx={{ mb: 2 }} />
      <Grid container spacing={3}>
        {[1, 2, 3, 4].map((_, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card sx={{ borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
              <CardContent sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <Skeleton variant="circular" width={20} height={20} sx={{ mr: 1 }} />
                  <Skeleton variant="text" width={120} />
                </Box>
                <Skeleton variant="rectangular" width={60} height={40} />
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  </>
);

// Event table skeleton
const TableRowsSkeleton = () => (
  <>
    {[...Array(5)].map((_, index) => (
      <TableRow key={index}>
        <TableCell><Skeleton variant="text" width="70%" /></TableCell>
        <TableCell><Skeleton variant="text" width="80%" /></TableCell>
        <TableCell><Skeleton variant="text" width="60%" /></TableCell>
        <TableCell><Skeleton variant="text" width="80%" /></TableCell>
        <TableCell><Skeleton variant="text" width="40%" /></TableCell>
        <TableCell><Skeleton variant="rectangular" width={80} height={24} sx={{ borderRadius: 1 }} /></TableCell>
        <TableCell>
          <Skeleton variant="circular" width={30} height={30} />
        </TableCell>
      </TableRow>
    ))}
  </>
);

export default function Dashboard() {
  const { darkMode } = useTheme();
  
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [activeTab, setActiveTab] = useState(0);
  const [mainTab, setMainTab] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [showModal, setShowModal] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [activeFilter, setActiveFilter] = useState('');
  const [totalEvents, setTotalEvents] = useState(0);
  const [showCertificateEditor, setShowCertificateEditor] = useState(false);
  const [certificateEvent, setCertificateEvent] = useState(null);
  const [certificateTemplate, setCertificateTemplate] = useState(null);
  const [attendanceLogs, setAttendanceLogs] = useState([]);
  const [loadingAttendance, setLoadingAttendance] = useState(false);

  // Default certificate template
  const defaultCertificate = {
    title: 'CERTIFICATE',
    subtitle: 'OF ACHIEVEMENT',
    recipientText: 'THIS CERTIFICATE IS PROUDLY PRESENTED TO',
    recipientName: '{Recipient Name}',
    description: 'For outstanding participation in the event and demonstrating exceptional dedication throughout the program.',
    signatories: [
      { name: 'John Doe', title: 'REPRESENTATIVE' },
      { name: 'Jane Smith', title: 'REPRESENTATIVE' }
    ],
    eventName: '{Event Name}',
    eventDate: '{Event Date}',
    certificateNumber: '{Certificate Number}',
    backgroundColor: '#ffffff',
    borderColor: '#0047AB',
    headerColor: '#0047AB',
    textColor: '#000000',
    fontFamily: 'Times New Roman'
  };

  useEffect(() => {
    fetchDepartments();
    fetchEvents();
  }, []);
  
  const fetchDepartments = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/departments');
      setDepartments(response.data);
    } catch (error) {
      console.error('Error fetching departments:', error);
    }
  };
  
  // Fetch events from the backend API with date range filtering
  const fetchEvents = async (startDate = '', endDate = '') => {
    try {
      setLoading(true);
      
      // Use the paginated endpoint but with a large size to get all events
      // Use cache buster to prevent caching issues
      let url = 'http://localhost:8080/api/events/getPaginated';
      const params = { 
        page: 0,
        size: 100, // Get up to 100 events at once
        _cache: new Date().getTime() // Cache buster
      };
      
      if (startDate || endDate) {
        url = 'http://localhost:8080/api/events/getByDateRange';
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
      }
      
      const response = await axios.get(url, { params });

      // If using paginated endpoint, response structure is different
      const responseData = response.data.content || response.data;
      const totalCount = response.data.totalElements || responseData.length;
      setTotalEvents(totalCount);

      const formattedEvents = responseData.map(event => {
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

  // Handle date filter changes
  const handleDateFilterChange = async () => {
    try {
      // Validate date range
      if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
        setError('Start date cannot be after end date');
        return;
      }
      
      await fetchEvents(startDate, endDate);
      setCurrentPage(1); // Reset to first page after filtering
    } catch (error) {
      console.error('Error applying date filter:', error);
    }
  };

  // Clear date filters
  const handleClearDateFilter = () => {
    setStartDate('');
    setEndDate('');
    fetchEvents();
  };

  // Open event detail modal
  const handleViewEvent = (event) => {
    setSelectedEvent(event);
    setShowModal(true);
  };

  // Handle tab changes
  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
    setCurrentPage(1);
  };

  // Get appropriate status class for badges
  const getStatusClass = (status = 'Unknown') => {
    const normalizedStatus = status.toLowerCase();
    
    if (normalizedStatus.includes('upcoming')) return 'status-pending';
    if (normalizedStatus.includes('ongoing')) return 'status-active';
    if (normalizedStatus.includes('completed')) return 'status-completed';
    if (normalizedStatus.includes('cancelled')) return 'status-canceled';
    if (normalizedStatus.includes('postponed')) return 'status-inactive';
    
    return 'status-inactive';
  };

  // Function to get filtered events based on active tab
  const getFilteredEvents = () => {
    // Apply any additional filtering based on active tab
    let filteredEvents = [...events];
    
    // Filter based on tab selection
    if (activeTab === 1) {
      // Upcoming events
      filteredEvents = filteredEvents.filter(event => 
        event.status.toLowerCase().includes('upcoming') || 
        event.status.toLowerCase().includes('scheduled')
      );
    } else if (activeTab === 2) {
      // Ongoing events
      filteredEvents = filteredEvents.filter(event => 
        event.status.toLowerCase().includes('ongoing')
      );
    } else if (activeTab === 3) {
      // Past events
      filteredEvents = filteredEvents.filter(event => 
        event.status.toLowerCase().includes('completed') || 
        event.status.toLowerCase().includes('ended') ||
        event.status.toLowerCase().includes('cancelled')
      );
    }
    
    // Apply department filter if active
    if (activeFilter) {
      filteredEvents = filteredEvents.filter(event => {
        const departmentName = getDepartmentName(event.departmentId);
        return departmentName.toLowerCase().includes(activeFilter.toLowerCase());
      });
    }
    
    return filteredEvents;
  };

  // Helper function to get department name by ID
  const getDepartmentName = (id) => {
    if (!id) return 'Unknown Department';
    const department = departments.find(dept => dept.departmentId === id);
    return department ? department.name : 'Unknown Department';
  };

  // Get current page of events
  const eventsPerPage = 10;
  const filteredEvents = getFilteredEvents();
  const totalPages = Math.ceil(filteredEvents.length / eventsPerPage);
  
  // Get current page of events
  const indexOfLastEvent = currentPage * eventsPerPage;
  const indexOfFirstEvent = indexOfLastEvent - eventsPerPage;
  const currentEvents = filteredEvents.slice(indexOfFirstEvent, indexOfLastEvent);

  // Pagination handlers
  const handlePreviousPage = () => {
    setCurrentPage(prev => Math.max(prev - 1, 1));
  };

  const handleNextPage = () => {
    setCurrentPage(prev => Math.min(prev + 1, totalPages));
  };

  // Handle event click in calendar
  const handleCalendarEventClick = (event) => {
    setSelectedEvent(event);
    setShowModal(true);
  };
  
  // Handle adding a new event from the calendar
  const handleAddEvent = async (newEvent) => {
    try {
      setLoading(true);
      
      const eventData = {
        eventName: newEvent.eventName,
        departmentId: newEvent.departmentId,
        date: newEvent.date,
        duration: newEvent.duration,
        location: newEvent.location || '',
        description: newEvent.description || '',
        status: 'Scheduled',
        createdBy: 'Dashboard'
      };
      
      const response = await axios.post('http://localhost:8080/api/events/createEvent', eventData);
      
      if (response.data && response.data.eventId) {
        // Add the new event to the events state
        fetchEvents();
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Error creating event:', error);
      setLoading(false);
    }
  };
  
  // Open certificate editor
  const handleOpenCertificateEditor = (event) => {
    // Create a template using the event details
    const template = {
      ...defaultCertificate,
      eventName: event.eventName || '{Event Name}',
      eventDate: event.date ? new Date(event.date).toLocaleDateString() : '{Event Date}'
    };
    
    setCertificateEvent(event);
    setCertificateTemplate(template);
    setShowCertificateEditor(true);
  };
  
  // Close certificate editor
  const handleCloseCertificateEditor = () => {
    setShowCertificateEditor(false);
    setCertificateEvent(null);
  };
  
  // Save certificate template
  const handleSaveCertificate = async (certificateData) => {
    try {
      if (!certificateEvent || !certificateEvent.eventId) {
        console.error('No event ID available for saving certificate template');
        return;
      }
      
      // Save certificate template to the event
      await axios.post(`http://localhost:8080/api/events/${certificateEvent.eventId}/certificate`, certificateData);
      setShowCertificateEditor(false);
    } catch (error) {
      console.error('Error saving certificate template:', error);
    }
  };

  // Fetch attendance logs
  const fetchAttendanceLogs = async () => {
    try {
      setLoadingAttendance(true);
      const response = await axios.get('http://localhost:8080/api/attendance/logs');
      setAttendanceLogs(response.data || []);
      setLoadingAttendance(false);
    } catch (error) {
      console.error('Error fetching attendance logs:', error);
      setLoadingAttendance(false);
    }
  };

  const handleMainTabChange = (event, newValue) => {
    setMainTab(newValue);
    
    // Reset the inner tab when changing main tab
    setActiveTab(0);
    
    // Load attendance data if switching to attendance tab
    if (newValue === 2) {
      fetchAttendanceLogs();
    }
  };

  return (
    <Box className={`dashboard-container ${darkMode ? 'dark-mode' : ''}`}>
      {/* Dashboard Content */}
      <Box className="dashboard-main">
        
        {/* Main Tabs */}
        <Box sx={{ mb: 3 }}>
          <Tabs 
            value={mainTab} 
            onChange={handleMainTabChange}
            sx={{ 
              borderBottom: 1, 
              borderColor: 'divider',
              '& .MuiTab-root': { 
                minWidth: { xs: 100, md: 120 }, 
                textTransform: 'none',
                fontSize: '16px',
                fontWeight: 500
              },
              '& .Mui-selected': {
                color: darkMode ? 'var(--accent-color)' : 'royalblue',
                fontWeight: 600
              },
              '& .MuiTabs-indicator': {
                backgroundColor: darkMode ? 'var(--accent-color)' : 'royalblue'
              }
            }}
          >
            <Tab 
              label="Event Summary" 
              icon={<EventNote />} 
              iconPosition="start"
            />
            <Tab 
              label="Calendar" 
              icon={<CalendarToday />} 
              iconPosition="start"
            />
            <Tab 
              label="Attendance Logs" 
              icon={<Group />} 
              iconPosition="start"
            />
          </Tabs>
        </Box>

        {/* Event Summary */}
        {mainTab === 0 && (
          <>
            {/* Event Summary Cards */}
            <Box sx={{ mb: 3 }}>
              <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 2 }}>Event Summary</Typography>
              {loading ? (
                <Grid container spacing={2}>
                  {[1, 2, 3, 4].map((_, index) => (
                    <Grid item xs={6} sm={3} md={3} key={index}>
                      <Card sx={{ 
                        borderRadius: '10px', 
                        boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
                        bgcolor: darkMode ? 'var(--card-bg)' : 'white'
                      }}>
                        <CardContent sx={{ p: 1.5 }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                            <Skeleton variant="circular" width={16} height={16} sx={{ mr: 1 }} />
                            <Skeleton variant="text" width={100} />
                          </Box>
                          <Skeleton variant="rectangular" width={50} height={30} />
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              ) : (
                <Grid container spacing={2}>
                  <Grid item xs={6} sm={3} md={3}>
                    <Card className="stat-card" sx={{ 
                      borderRadius: '10px', 
                      boxShadow: '0 2px 10px rgba(0,0,0,0.05)', 
                      background: darkMode ? 'var(--card-bg)' : 'linear-gradient(135deg, #f6f9fc 0%, #ffffff 100%)',
                      transition: 'all 0.2s ease',
                      height: '100%',
                      '&:hover': {
                        transform: 'translateY(-3px)',
                        boxShadow: darkMode ? '0 4px 15px rgba(0,0,0,0.15)' : '0 4px 15px rgba(0,0,0,0.08)'
                      }
                    }}>
                      <CardContent sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
                        <Box className="stat-icon" sx={{ 
                          background: darkMode ? 'var(--accent-light)' : 'rgba(65, 105, 225, 0.1)', 
                          color: darkMode ? 'var(--accent-color)' : 'royalblue',
                          borderRadius: '8px',
                          width: '36px',
                          height: '36px',
                          mb: 1,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}>
                          <CalendarToday fontSize="small" />
                        </Box>
                        <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500, mb: 0.5 }}>Total Events</Typography>
                        <Typography variant="h4" sx={{ fontWeight: 700, color: darkMode ? 'var(--accent-color)' : 'royalblue', mb: 0, mt: 'auto' }}>{totalEvents}</Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={6} sm={3} md={3}>
                    <Card className="stat-card" sx={{ 
                      borderRadius: '10px', 
                      boxShadow: '0 2px 10px rgba(0,0,0,0.05)', 
                      background: darkMode ? 'var(--card-bg)' : 'linear-gradient(135deg, #f9f8ff 0%, #ffffff 100%)',
                      transition: 'all 0.2s ease',
                      height: '100%',
                      '&:hover': {
                        transform: 'translateY(-3px)',
                        boxShadow: darkMode ? '0 4px 15px rgba(0,0,0,0.15)' : '0 4px 15px rgba(0,0,0,0.08)'
                      }
                    }}>
                      <CardContent sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
                        <Box className="stat-icon" sx={{ 
                          background: darkMode ? 'rgba(255, 152, 0, 0.15)' : 'rgba(255, 152, 0, 0.1)', 
                          color: 'orange',
                          borderRadius: '8px',
                          width: '36px',
                          height: '36px',
                          mb: 1,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}>
                          <AccessTime fontSize="small" />
                        </Box>
                        <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500, mb: 0.5 }}>Upcoming Events</Typography>
                        <Typography variant="h4" sx={{ fontWeight: 700, color: 'orange', mb: 0, mt: 'auto' }}>
                          {events.filter(event => event.status.toLowerCase().includes('upcoming')).length}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={6} sm={3} md={3}>
                    <Card className="stat-card" sx={{ 
                      borderRadius: '10px', 
                      boxShadow: '0 2px 10px rgba(0,0,0,0.05)', 
                      background: darkMode ? 'var(--card-bg)' : 'linear-gradient(135deg, #f8feff 0%, #ffffff 100%)',
                      transition: 'all 0.2s ease',
                      height: '100%',
                      '&:hover': {
                        transform: 'translateY(-3px)',
                        boxShadow: darkMode ? '0 4px 15px rgba(0,0,0,0.15)' : '0 4px 15px rgba(0,0,0,0.08)'
                      }
                    }}>
                      <CardContent sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
                        <Box className="stat-icon" sx={{ 
                          background: darkMode ? 'rgba(0, 150, 136, 0.15)' : 'rgba(0, 150, 136, 0.1)', 
                          color: 'teal',
                          borderRadius: '8px',
                          width: '36px',
                          height: '36px',
                          mb: 1,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}>
                          <Group fontSize="small" />
                        </Box>
                        <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500, mb: 0.5 }}>Ongoing Events</Typography>
                        <Typography variant="h4" sx={{ fontWeight: 700, color: 'teal', mb: 0, mt: 'auto' }}>
                          {events.filter(event => event.status.toLowerCase().includes('ongoing')).length}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                  <Grid item xs={6} sm={3} md={3}>
                    <Card className="stat-card" sx={{ 
                      borderRadius: '10px', 
                      boxShadow: '0 2px 10px rgba(0,0,0,0.05)', 
                      background: darkMode ? 'var(--card-bg)' : 'linear-gradient(135deg, #f8f8fc 0%, #ffffff 100%)',
                      transition: 'all 0.2s ease',
                      height: '100%',
                      '&:hover': {
                        transform: 'translateY(-3px)',
                        boxShadow: darkMode ? '0 4px 15px rgba(0,0,0,0.15)' : '0 4px 15px rgba(0,0,0,0.08)'
                      }
                    }}>
                      <CardContent sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
                        <Box className="stat-icon" sx={{ 
                          background: darkMode ? 'rgba(76, 175, 80, 0.15)' : 'rgba(76, 175, 80, 0.1)', 
                          color: 'green',
                          borderRadius: '8px',
                          width: '36px',
                          height: '36px',
                          mb: 1,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}>
                          <CalendarToday fontSize="small" />
                        </Box>
                        <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500, mb: 0.5 }}>Completed Events</Typography>
                        <Typography variant="h4" sx={{ fontWeight: 700, color: 'green', mb: 0, mt: 'auto' }}>
                          {events.filter(event => 
                            event.status.toLowerCase().includes('completed') || 
                            event.status.toLowerCase().includes('ended')
                          ).length}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                </Grid>
              )}
            </Box>

            {/* Filter and Table Section */}
            <Box sx={{ 
              bgcolor: darkMode ? 'var(--card-bg)' : 'white', 
              borderRadius: '16px', 
              boxShadow: '0 4px 20px rgba(0,0,0,0.05)', 
              overflow: 'hidden',
              border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)'
            }}>
              {/* Tab and Filter Section */}
              <Box sx={{ 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center', 
                p: { xs: 2, md: 3 }, 
                borderBottom: '1px solid', 
                borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.06)',
                flexDirection: { xs: 'column', md: 'row' },
                gap: { xs: 2, md: 0 }
              }}>
                <Tabs 
                  value={activeTab} 
                  onChange={handleTabChange}
                  sx={{ 
                    '& .MuiTab-root': { 
                      minWidth: { xs: 80, md: 100 }, 
                      textTransform: 'none',
                      fontSize: '14px',
                      fontWeight: 500
                    },
                    '& .Mui-selected': {
                      color: darkMode ? 'var(--accent-color)' : 'royalblue',
                      fontWeight: 600
                    },
                    '& .MuiTabs-indicator': {
                      backgroundColor: darkMode ? 'var(--accent-color)' : 'royalblue'
                    }
                  }}
                >
                  <Tab label="All Events" />
                  <Tab label="Upcoming" />
                  <Tab label="Ongoing" />
                  <Tab label="Past Events" />
                </Tabs>
                
                <Box sx={{ 
                  display: 'flex', 
                  gap: 2,
                  flexWrap: { xs: 'wrap', md: 'nowrap' },
                  width: { xs: '100%', md: 'auto' },
                  justifyContent: { xs: 'center', md: 'flex-end' }
                }}>
                  <TextField
                    type="date"
                    size="small"
                    label="Date From"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                    sx={{ 
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '8px',
                        '&:hover fieldset': {
                          borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.15)',
                        },
                      }
                    }}
                  />
                  <TextField
                    type="date"
                    size="small"
                    label="To"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                    sx={{ 
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '8px',
                        '&:hover fieldset': {
                          borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.15)',
                        },
                      }
                    }}
                  />
                  <Button 
                    variant="contained" 
                    color="primary"
                    size="small"
                    onClick={handleDateFilterChange}
                    sx={{ 
                      textTransform: 'none', 
                      borderRadius: '8px',
                      backgroundColor: darkMode ? 'var(--accent-color)' : 'royalblue',
                      boxShadow: 'none',
                      '&:hover': {
                        backgroundColor: darkMode ? 'var(--accent-hover)' : 'rgb(52, 84, 180)',
                        boxShadow: darkMode ? '0 4px 12px rgba(107, 110, 247, 0.25)' : '0 4px 12px rgba(65, 105, 225, 0.25)',
                      }
                    }}
                  >
                    Apply Filter
                  </Button>
                  <Button 
                    variant="outlined" 
                    size="small"
                    onClick={handleClearDateFilter}
                    sx={{ 
                      textTransform: 'none',
                      borderRadius: '8px',
                      borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.15)',
                      color: 'text.secondary',
                      '&:hover': {
                        borderColor: darkMode ? 'var(--text-tertiary)' : 'rgba(0,0,0,0.25)',
                        backgroundColor: darkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)'
                      }
                    }}
                  >
                    Clear
                  </Button>
                </Box>
              </Box>
              
              {/* Events Table */}
              {loading ? (
                <TableContainer>
                  <Table sx={{ minWidth: 650 }}>
                    <TableHead sx={{ bgcolor: darkMode ? 'var(--table-header-bg)' : 'rgba(0,0,0,0.02)' }}>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Event ID</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Event Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Department</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Date</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Duration</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Status</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      <TableRowsSkeleton />
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : error ? (
                <Box sx={{ p: 3, textAlign: 'center', color: 'error.main' }}>
                  <Typography>{error}</Typography>
                </Box>
              ) : currentEvents.length === 0 ? (
                <Box sx={{ p: 3, textAlign: 'center', color: 'text.secondary' }}>
                  <Typography>No events found</Typography>
                </Box>
              ) : (
                <TableContainer>
                  <Table sx={{ minWidth: 650 }}>
                    <TableHead sx={{ bgcolor: darkMode ? 'var(--table-header-bg)' : 'rgba(0,0,0,0.02)' }}>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Event ID</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Event Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Department</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Date</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Duration</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Status</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {currentEvents.map((event) => (
                        <TableRow key={event.id} sx={{ '&:hover': { bgcolor: darkMode ? 'var(--accent-light)' : 'action.hover' } }}>
                          <TableCell component="th" scope="row" sx={{ fontWeight: 500, color: darkMode ? 'var(--accent-color)' : 'primary.main' }}>
                            {event.id}
                          </TableCell>
                          <TableCell>{event.name}</TableCell>
                          <TableCell>{getDepartmentName(event.departmentId)}</TableCell>
                          <TableCell>{event.date}</TableCell>
                          <TableCell>{event.duration}</TableCell>
                          <TableCell>
                            <Chip 
                              label={event.status} 
                              size="small" 
                              className={`status-chip ${getStatusClass(event.status)}`}
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell>
                            <Tooltip title="View Details">
                              <IconButton 
                                size="small" 
                                color="primary"
                                onClick={() => handleViewEvent(event)}
                              >
                                <Visibility fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              
              {/* Pagination Controls */}
              {activeTab !== 4 && (
                <Box sx={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'center', 
                  p: { xs: 2, md: 3 }, 
                  borderTop: '1px solid', 
                  borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.06)',
                  bgcolor: darkMode ? 'var(--background-tertiary)' : 'rgba(0,0,0,0.01)'
                }}>
                  <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500 }}>
                    Showing {currentEvents.length > 0 ? indexOfFirstEvent + 1 : 0} to {Math.min(indexOfLastEvent, filteredEvents.length)} of {filteredEvents.length} events
                  </Typography>
                  
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Typography variant="body2" color="text.secondary" sx={{ mr: 2, fontWeight: 500 }}>
                      Page {currentPage} of {totalPages || 1}
                    </Typography>
                    <Button
                      variant="outlined"
                      size="small"
                      startIcon={<ChevronLeft />}
                      onClick={handlePreviousPage}
                      disabled={currentPage === 1 || totalPages === 0}
                      sx={{ 
                        minWidth: { xs: 40, md: 100 }, 
                        textTransform: 'none', 
                        mr: 1,
                        borderRadius: '8px',
                        borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.15)',
                        color: 'text.secondary',
                        px: { xs: 1, md: 2 },
                        '&:hover': {
                          borderColor: darkMode ? 'var(--text-tertiary)' : 'rgba(0,0,0,0.25)',
                          backgroundColor: darkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)'
                        },
                        '&.Mui-disabled': {
                          borderColor: darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'
                        }
                      }}
                    >
                      <Box sx={{ display: { xs: 'none', md: 'flex' }, alignItems: 'center' }}>
                        Previous
                      </Box>
                    </Button>
                    <Button
                      variant="outlined"
                      size="small"
                      endIcon={<ChevronRight />}
                      onClick={handleNextPage}
                      disabled={currentPage === totalPages || totalPages === 0}
                      sx={{ 
                        minWidth: { xs: 40, md: 100 }, 
                        textTransform: 'none',
                        borderRadius: '8px',
                        borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.15)',
                        color: 'text.secondary',
                        px: { xs: 1, md: 2 },
                        '&:hover': {
                          borderColor: darkMode ? 'var(--text-tertiary)' : 'rgba(0,0,0,0.25)',
                          backgroundColor: darkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)'
                        },
                        '&.Mui-disabled': {
                          borderColor: darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'
                        }
                      }}
                    >
                      <Box sx={{ display: { xs: 'none', md: 'flex' }, alignItems: 'center' }}>
                        Next
                      </Box>
                    </Button>
                  </Box>
                </Box>
              )}
            </Box>
          </>
        )}

        {/* Calendar Tab */}
        {mainTab === 1 && (
          <Box sx={{ 
            bgcolor: darkMode ? 'var(--card-bg)' : 'white', 
            borderRadius: '16px', 
            boxShadow: '0 4px 20px rgba(0,0,0,0.05)', 
            overflow: 'hidden',
            border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)',
            p: 3
          }}>
            <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 3 }}>Event Calendar</Typography>
            
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress />
              </Box>
            ) : (
              <EventCalendar
                events={getFilteredEvents()}
                departments={departments}
                onEventClick={handleCalendarEventClick}
                onAddEvent={handleAddEvent}
                onOpenCertificateEditor={handleOpenCertificateEditor}
              />
            )}
          </Box>
        )}

        {/* Attendance Logs Tab */}
        {mainTab === 2 && (
          <Box sx={{ 
            bgcolor: darkMode ? 'var(--card-bg)' : 'white', 
            borderRadius: '16px', 
            boxShadow: '0 4px 20px rgba(0,0,0,0.05)', 
            overflow: 'hidden',
            border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)',
            p: 3
          }}>
            <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 3 }}>Day-to-Day Attendance Logs</Typography>
            
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
              <TextField
                size="small"
                variant="outlined"
                placeholder="Search attendance logs..."
                sx={{ width: '300px' }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Search />
                    </InputAdornment>
                  ),
                }}
              />
              
              <Box sx={{ display: 'flex', gap: 2 }}>
                <TextField
                  type="date"
                  size="small"
                  label="From"
                  InputLabelProps={{ shrink: true }}
                  sx={{ 
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                    },
                  }}
                />
                <TextField
                  type="date"
                  size="small"
                  label="To"
                  InputLabelProps={{ shrink: true }}
                  sx={{ 
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '8px',
                    },
                  }}
                />
                <Button 
                  variant="contained" 
                  color="primary"
                  size="small"
                  sx={{ 
                    textTransform: 'none', 
                    borderRadius: '8px',
                    backgroundColor: darkMode ? 'var(--accent-color)' : 'royalblue',
                    boxShadow: 'none',
                  }}
                >
                  Filter
                </Button>
              </Box>
            </Box>
            
            {loadingAttendance ? (
              <TableContainer>
                <Table>
                  <TableHead sx={{ bgcolor: darkMode ? 'var(--table-header-bg)' : 'rgba(0,0,0,0.02)' }}>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>User ID</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Name</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Event</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Department</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Time In</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Time Out</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Duration</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    <TableRowsSkeleton />
                  </TableBody>
                </Table>
              </TableContainer>
            ) : attendanceLogs.length === 0 ? (
              <Box sx={{ p: 3, textAlign: 'center', color: 'text.secondary' }}>
                <Typography>No attendance logs found</Typography>
              </Box>
            ) : (
              <TableContainer>
                <Table>
                  <TableHead sx={{ bgcolor: darkMode ? 'var(--table-header-bg)' : 'rgba(0,0,0,0.02)' }}>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>User ID</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Name</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Event</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Department</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Time In</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Time Out</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Duration</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {attendanceLogs.map((log) => (
                      <TableRow key={log.id} sx={{ '&:hover': { bgcolor: darkMode ? 'var(--accent-light)' : 'action.hover' } }}>
                        <TableCell>{log.userId}</TableCell>
                        <TableCell>{`${log.firstName} ${log.lastName}`}</TableCell>
                        <TableCell>{log.eventName}</TableCell>
                        <TableCell>{log.departmentName}</TableCell>
                        <TableCell>{log.timeIn}</TableCell>
                        <TableCell>{log.timeOut || 'N/A'}</TableCell>
                        <TableCell>{log.duration || 'N/A'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        )}
      </Box>
      
      {/* Event Detail Modal */}
      <Modal
        open={showModal}
        onClose={() => setShowModal(false)}
        aria-labelledby="event-detail-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: { xs: '90%', sm: 600 },
          bgcolor: darkMode ? 'var(--card-bg)' : 'background.paper',
          boxShadow: 24,
          borderRadius: 2,
          p: 3,
          maxHeight: '90vh',
          overflow: 'auto',
          border: darkMode ? '1px solid var(--border-color)' : 'none'
        }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight={600}>Event Details</Typography>
            <IconButton onClick={() => setShowModal(false)} size="small">
              <Close />
            </IconButton>
          </Box>
          
          <Divider sx={{ mb: 2, borderColor: darkMode ? 'var(--border-color)' : 'inherit' }} />
          
          {selectedEvent && (
            <List sx={{ p: 0 }}>
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Event ID</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.id}</Typography>
                </Box>
              </ListItem>
              <Divider sx={{ borderColor: darkMode ? 'var(--border-color)' : 'inherit' }} />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Event Name</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.name}</Typography>
                </Box>
              </ListItem>
              <Divider sx={{ borderColor: darkMode ? 'var(--border-color)' : 'inherit' }} />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Department</Typography>
                  <Typography variant="body1" fontWeight={500}>{getDepartmentName(selectedEvent.departmentId)}</Typography>
                </Box>
              </ListItem>
              <Divider sx={{ borderColor: darkMode ? 'var(--border-color)' : 'inherit' }} />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Date and Time</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.date}</Typography>
                </Box>
              </ListItem>
              <Divider sx={{ borderColor: darkMode ? 'var(--border-color)' : 'inherit' }} />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Duration</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.duration}</Typography>
                </Box>
              </ListItem>
              <Divider sx={{ borderColor: darkMode ? 'var(--border-color)' : 'inherit' }} />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Status</Typography>
                  <Chip 
                    label={selectedEvent.status} 
                    size="small" 
                    className={`status-chip ${getStatusClass(selectedEvent.status)}`}
                    sx={{ mt: 0.5 }}
                  />
                </Box>
              </ListItem>
            </List>
          )}
          
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 3 }}>
            <Button 
              variant="outlined" 
              onClick={() => setShowModal(false)}
              sx={{ 
                textTransform: 'none', 
                mr: 1,
                borderColor: darkMode ? 'var(--border-color)' : 'inherit'
              }}
            >
              Close
            </Button>
            <Button 
              variant="contained" 
              color="primary" 
              onClick={() => {
                setShowModal(false);
                // Navigate to attendance page with event ID
                window.location.href = `/attendance/${selectedEvent.id}`;
              }}
              sx={{ 
                textTransform: 'none',
                backgroundColor: darkMode ? 'var(--accent-color)' : undefined,
                '&:hover': {
                  backgroundColor: darkMode ? 'var(--accent-hover)' : undefined
                }
              }}
            >
              View Attendance
            </Button>
          </Box>
        </Box>
      </Modal>
      
      {/* Certificate Editor */}
      {showCertificateEditor && (
        <CertificateEditor
          open={showCertificateEditor}
          onClose={handleCloseCertificateEditor}
          initialData={certificateTemplate}
          onSave={handleSaveCertificate}
          eventData={certificateEvent}
        />
      )}
    </Box>
  );
}