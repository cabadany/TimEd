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
  Tooltip
} from '@mui/material';
import {
  Visibility,
  ChevronLeft,
  ChevronRight,
  Close,
  CalendarToday,
  AccessTime,
  Group
} from '@mui/icons-material';
import './dashboard.css';

export default function Dashboard() {
  
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
  const [activeFilter, setActiveFilter] = useState('');

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
      
      // Construct URL with query parameters for date range filtering
      let url = 'http://localhost:8080/api/events/getAll';
      
      if (startDate || endDate) {
        url = 'http://localhost:8080/api/events/getByDateRange';
        if (startDate) url += `?startDate=${startDate}`;
        if (endDate) url += `${startDate ? '&' : '?'}endDate=${endDate}`;
      }
      
      const response = await axios.get(url);

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
    setCurrentPage(1); // Reset to first page when changing tabs
  };

  // Get appropriate status color for badges
  const getStatusColor = (status = 'Unknown') => {
    const normalizedStatus = status.toLowerCase();
    
    if (normalizedStatus.includes('upcoming')) return 'primary';
    if (normalizedStatus.includes('ongoing')) return 'success';
    if (normalizedStatus.includes('completed')) return 'info';
    if (normalizedStatus.includes('cancelled')) return 'error';
    if (normalizedStatus.includes('postponed')) return 'warning';
    
    return 'default';
  };

  // Filter events based on active tab
  const getFilteredEvents = () => {
    let filteredEvents = [...events];
    
    // Filter by status based on active tab
    if (activeTab === 1) {
      filteredEvents = filteredEvents.filter(event => event.status.toLowerCase().includes('upcoming'));
    } else if (activeTab === 2) {
      filteredEvents = filteredEvents.filter(event => event.status.toLowerCase().includes('ongoing'));
    } else if (activeTab === 3) {
      filteredEvents = filteredEvents.filter(event => 
        event.status.toLowerCase().includes('completed') || 
        event.status.toLowerCase().includes('cancelled') || 
        event.status.toLowerCase().includes('postponed')
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

  return (
    <Box className="dashboard-container">
      {/* Dashboard Content */}
      <Box className="dashboard-main">
        {/* Event Summary Cards */}
        <Box sx={{ mb: 4 }}>
          <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>Event Summary</Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                <CardContent sx={{ p: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <CalendarToday sx={{ color: '#304FFF', mr: 1, fontSize: 20 }} />
                    <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500 }}>Total Events</Typography>
                  </Box>
                  <Typography variant="h4" sx={{ fontWeight: 600 }}>{events.length}</Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                <CardContent sx={{ p: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <AccessTime sx={{ color: '#FF5630', mr: 1, fontSize: 20 }} />
                    <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500 }}>Upcoming Events</Typography>
                  </Box>
                  <Typography variant="h4" sx={{ fontWeight: 600 }}>
                    {events.filter(event => event.status.toLowerCase().includes('upcoming')).length}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                <CardContent sx={{ p: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <Group sx={{ color: '#00B8D9', mr: 1, fontSize: 20 }} />
                    <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500 }}>Ongoing Events</Typography>
                  </Box>
                  <Typography variant="h4" sx={{ fontWeight: 600 }}>
                    {events.filter(event => event.status.toLowerCase().includes('ongoing')).length}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
                <CardContent sx={{ p: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <CalendarToday sx={{ color: '#36B37E', mr: 1, fontSize: 20 }} />
                    <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500 }}>Completed Events</Typography>
                  </Box>
                  <Typography variant="h4" sx={{ fontWeight: 600 }}>
                    {events.filter(event => event.status.toLowerCase().includes('completed')).length}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Box>

        {/* Filter and Table Section */}
        <Box sx={{ bgcolor: 'background.paper', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)', overflow: 'hidden' }}>
          {/* Tab and Filter Section */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Tabs 
              value={activeTab} 
              onChange={handleTabChange}
              sx={{ 
                '& .MuiTab-root': { 
                  minWidth: 100, 
                  textTransform: 'none',
                  fontSize: '14px',
                  fontWeight: 500
                } 
              }}
            >
              <Tab label="All Events" />
              <Tab label="Upcoming" />
              <Tab label="Ongoing" />
              <Tab label="Past Events" />
            </Tabs>
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                type="date"
                size="small"
                label="Date From"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                type="date"
                size="small"
                label="To"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              <Button 
                variant="contained" 
                color="primary"
                size="small"
                onClick={handleDateFilterChange}
                sx={{ textTransform: 'none' }}
              >
                Apply Filter
              </Button>
              <Button 
                variant="outlined" 
                size="small"
                onClick={handleClearDateFilter}
                sx={{ textTransform: 'none' }}
              >
                Clear
              </Button>
            </Box>
          </Box>
          
          {/* Events Table */}
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
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
                <TableHead sx={{ bgcolor: 'action.hover' }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Event ID</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Event Name</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Department</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Date</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Duration</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {currentEvents.map((event) => (
                    <TableRow key={event.id} sx={{ '&:hover': { bgcolor: 'action.hover' } }}>
                      <TableCell component="th" scope="row" sx={{ fontWeight: 500, color: 'primary.main' }}>
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
                          color={getStatusColor(event.status)} 
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
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', p: 2, borderTop: '1px solid', borderColor: 'divider' }}>
            <Typography variant="body2" color="text.secondary" sx={{ mr: 2 }}>
              Page {currentPage} of {totalPages || 1}
            </Typography>
            <Button
              variant="outlined"
              size="small"
              startIcon={<ChevronLeft />}
              onClick={handlePreviousPage}
              disabled={currentPage === 1 || totalPages === 0}
              sx={{ minWidth: 100, textTransform: 'none', mr: 1 }}
            >
              Previous
            </Button>
            <Button
              variant="outlined"
              size="small"
              endIcon={<ChevronRight />}
              onClick={handleNextPage}
              disabled={currentPage === totalPages || totalPages === 0}
              sx={{ minWidth: 100, textTransform: 'none' }}
            >
              Next
            </Button>
          </Box>
        </Box>
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
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 2,
          p: 3,
          maxHeight: '90vh',
          overflow: 'auto'
        }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight={600}>Event Details</Typography>
            <IconButton onClick={() => setShowModal(false)} size="small">
              <Close />
            </IconButton>
          </Box>
          
          <Divider sx={{ mb: 2 }} />
          
          {selectedEvent && (
            <List sx={{ p: 0 }}>
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Event ID</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.id}</Typography>
                </Box>
              </ListItem>
              <Divider />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Event Name</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.name}</Typography>
                </Box>
              </ListItem>
              <Divider />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Department</Typography>
                  <Typography variant="body1" fontWeight={500}>{getDepartmentName(selectedEvent.departmentId)}</Typography>
                </Box>
              </ListItem>
              <Divider />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Date and Time</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.date}</Typography>
                </Box>
              </ListItem>
              <Divider />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Duration</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.duration}</Typography>
                </Box>
              </ListItem>
              <Divider />
              
              <ListItem sx={{ py: 1, px: 0 }}>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="body2" color="text.secondary">Status</Typography>
                  <Chip 
                    label={selectedEvent.status} 
                    size="small" 
                    color={getStatusColor(selectedEvent.status)} 
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
              sx={{ textTransform: 'none', mr: 1 }}
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
              sx={{ textTransform: 'none' }}
            >
              View Attendance
            </Button>
          </Box>
        </Box>
      </Modal>
    </Box>
  );
}