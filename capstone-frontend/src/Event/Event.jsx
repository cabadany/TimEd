import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { parse, format } from 'date-fns';
import { useTheme } from '../contexts/ThemeContext';
import { QrCode2, Download, BrandingWatermark } from '@mui/icons-material';
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
  Divider,
  TablePagination,
  Skeleton,
  Checkbox,
  Tooltip
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
  Close,
  Logout,
  Delete,
  Add,
  Error,
  Edit,
  CheckCircle,
  Cancel,
  MoreVert,
  DeleteSweep
} from '@mui/icons-material';
import './Event.css';
import NotificationSystem from '../components/NotificationSystem';
import CertificateEditor from '../components/CertificateEditor';
import { API_BASE_URL, getApiUrl, API_ENDPOINTS } from '../utils/api';
import { formatDatePH, createLocalDateISO } from '../utils/dateUtils';

// Default certificate template
const defaultCertificate = {
  title: 'CERTIFICATE',
  subtitle: 'OF ACHIEVEMENT', 
  recipientText: 'THIS CERTIFICATE IS PROUDLY PRESENTED TO',
  recipientName: '{FirstName, LastName}',
  description: 'For outstanding participation in the event and demonstrating exceptional dedication throughout the program.',
  signatories: [
    { name: 'John Doe', title: 'REPRESENTATIVE' },
    { name: 'Jane Smith', title: 'REPRESENTATIVE' }
  ],
  eventName: '{Event Name}',
  eventDate: '{Event Date}',
  certificateNumber: '{Certificate Number}',
  backgroundColor: '#ffffff',
  headerColor: '#000000',
  textColor: '#000000',
  fontFamily: 'Times New Roman'
};

// Use React.memo for event cards to prevent unnecessary re-renders
const EventCard = React.memo(({ event, getDepartmentName, formatDate, openQrModal, updateEventStatus, setEventToEdit, setEditedStatus, setEditDurationHours, setEditDurationMinutes, setEditDurationSeconds, setEditDuration, setEditDialogOpen, openCertificateEditor }) => {
  // Calculate remaining time
  const startDate = new Date(event.date);
  const [hours, minutes, seconds] = event.duration.split(':').map(Number);
  const endTime = new Date(startDate);
  endTime.setHours(endTime.getHours() + hours);
  endTime.setMinutes(endTime.getMinutes() + minutes);
  endTime.setSeconds(endTime.getSeconds() + seconds);
  
  const now = new Date();
  const remainingMs = endTime - now;
  const remainingHours = Math.floor(remainingMs / (1000 * 60 * 60));
  const remainingMinutes = Math.floor((remainingMs % (1000 * 60 * 60)) / (1000 * 60));
  
  // Format for display
  const remainingTimeText = remainingMs > 0 
    ? `${remainingHours}h ${remainingMinutes}m remaining` 
    : "Time expired";

  return (
    <Grid size={{ sm: 12, md: 6, lg: 4 }} key={event.eventId}>
      <Card 
        elevation={0} 
        sx={{ 
          border: '1px solid #E2E8F0', 
          borderRadius: '8px',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          position: 'relative',
          overflow: 'visible'
        }}
      >
        {/* Time remaining badge */}
        <Box sx={{
          position: 'absolute',
          top: -10,
          right: 16,
          bgcolor: remainingMs > 0 ? '#E0F2FE' : '#FEE2E2',
          color: remainingMs > 0 ? '#0369A1' : '#B91C1C',
          borderRadius: '12px',
          px: 2,
          py: 0.5,
          fontSize: '0.75rem',
          fontWeight: 600,
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          {remainingTimeText}
        </Box>
        
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
  
  <Typography variant="body2" color="#64748B" gutterBottom>
    <strong>Duration:</strong> {event.duration}
  </Typography>

  <Typography variant="body2" color="#64748B" gutterBottom>
    <strong>Location:</strong> {event.venue || 'N/A'}
  </Typography>

  {event.description && (
    <Typography variant="body2" color="#64748B" sx={{ mt: 1 }}>
      <strong>Description:</strong> {event.description}
    </Typography>
  )}
</CardContent>
        
        <Divider />
        
        <CardActions sx={{ justifyContent: 'space-between', p: 2, flexWrap: 'wrap', gap: 1 }}>
          <Button
            size="small"
            startIcon={<QrCode2 />}
            onClick={() => openQrModal(event.eventId)}
            sx={{ 
              color: '#0288d1',
              bgcolor: '#E0F2FE',
              '&:hover': {
                bgcolor: '#BAE6FD'
              }
            }}
          >
            QR Code
          </Button>
          
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              size="small"
              startIcon={<AccessTime />}
              onClick={() => {
                // Set up for extension
                setEventToEdit(event);
                setEditedStatus(event.status);
                
                // Parse duration and add 30 minutes by default for extension
                const [hours, minutes, seconds] = event.duration.split(':');
                const newHours = hours;
                const newMinutes = String(parseInt(minutes) + 30).padStart(2, '0');
                
                setEditDurationHours(newHours);
                setEditDurationMinutes(newMinutes);
                setEditDurationSeconds(seconds);
                setEditDuration(`${newHours}:${newMinutes}:${seconds}`);
                
                setEditDialogOpen(true);
              }}
              sx={{ 
                color: '#059669', 
                bgcolor: '#DCFCE7',
                '&:hover': {
                  bgcolor: '#BBF7D0'
                }
              }}
            >
              Extend
            </Button>
            
            <Button
              size="small"
              color="error"
              startIcon={<Cancel />}
              onClick={() => updateEventStatus(event.eventId, 'Ended')}
              sx={{
                bgcolor: '#FEF2F2',
                '&:hover': {
                  bgcolor: '#FEE2E2'
                }
              }}
            >
              End
            </Button>
          </Box>
        </CardActions>
      </Card>
    </Grid>
  );
});

// Event card loading placeholder
const EventCardSkeleton = () => (
  <Grid size={{ sm: 12, md: 6, lg: 4 }}>
    <Card 
      elevation={0} 
      sx={{ 
        border: '1px solid #E2E8F0', 
        borderRadius: '8px',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        p: 3
      }}
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Skeleton variant="text" width="70%" height={32} />
        <Skeleton variant="rectangular" width={80} height={24} sx={{ borderRadius: 1 }} />
      </Box>
      
      <Skeleton variant="text" width="90%" sx={{ mb: 1 }} />
      <Skeleton variant="text" width="80%" sx={{ mb: 1 }} />
      <Skeleton variant="text" width="85%" sx={{ mb: 1 }} />
      <Skeleton variant="text" width="75%" sx={{ mb: 2 }} />
      
      <Box sx={{ mt: 'auto', pt: 2, borderTop: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between' }}>
        <Skeleton variant="rectangular" width={100} height={36} sx={{ borderRadius: 1 }} />
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Skeleton variant="rectangular" width={80} height={36} sx={{ borderRadius: 1 }} />
          <Skeleton variant="rectangular" width={80} height={36} sx={{ borderRadius: 1 }} />
        </Box>
      </Box>
    </Card>
  </Grid>
);

export default function EventPage() {
  const { darkMode, toggleDarkMode } = useTheme();
  const navigate = useNavigate();

  
  // State for form fields
  const [eventName, setEventName] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [date, setDate] = useState('');
  const [duration, setDuration] = useState('0:00:00');
  const [durationHours, setDurationHours] = useState('0');
  const [durationMinutes, setDurationMinutes] = useState('00');
  const [durationSeconds, setDurationSeconds] = useState('00');
  const [description, setDescription] = useState(''); // Add description state
  const [venue, setVenue] = useState(''); // Add location state
  
  // State for events data
  const [events, setEvents] = useState([]);
  const [ongoingEvents, setOngoingEvents] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(false);
  
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
  // Add separate state for edit dialog duration
  const [editDurationHours, setEditDurationHours] = useState('0');
  const [editDurationMinutes, setEditDurationMinutes] = useState('00');
  const [editDurationSeconds, setEditDurationSeconds] = useState('00');
  const [editDuration, setEditDuration] = useState('0:00:00');
  // Add state for additional editable fields in edit dialog
  const [editEventName, setEditEventName] = useState('');
  const [editDate, setEditDate] = useState('');
  const [editVenue, setEditVenue] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [qrModalOpen, setQrModalOpen] = useState(false);
  const [currentQrEventId, setCurrentQrEventId] = useState(null);
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

  // Add state for certificate template
  const [showCertificateEditor, setShowCertificateEditor] = useState(false);
  const [currentCertificateData, setCurrentCertificateData] = useState(null);
  const [eventForCertificate, setEventForCertificate] = useState(null);
  
  // Add pagination state
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalEvents, setTotalEvents] = useState(0);
  
  // Add new state variables for bulk delete functionality
  const [bulkDeleteMode, setBulkDeleteMode] = useState(false);
  const [selectedEvents, setSelectedEvents] = useState([]);
  const [bulkDeleteDialogOpen, setBulkDeleteDialogOpen] = useState(false);
  
  const openQrModal = (eventId) => {
    setCurrentQrEventId(eventId);
    setQrModalOpen(true);
  };
  
  const closeQrModal = () => {
    setQrModalOpen(false);
    setCurrentQrEventId(null);
  };
  
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
    // Skip processing if events array is empty
    if (!events.length) return;
    
    // Create a flag to track if we need to update state
    let needsUpdate = false;
    const currentDate = new Date();
    
    // Create copies for modifications to avoid direct state mutation
    const eventsCopy = [...events];
    const updatedEvents = eventsCopy.map(event => {
      // Make a copy to avoid direct mutation
      const updatedEvent = {...event};
      let statusChanged = false;
      
      // Parse the event date
      const eventDate = new Date(updatedEvent.date);
      
      // Calculate event end time
      const [hours, minutes, seconds] = updatedEvent.duration.split(':').map(Number);
      const eventEndTime = new Date(eventDate);
      eventEndTime.setHours(eventEndTime.getHours() + hours);
      eventEndTime.setMinutes(eventEndTime.getMinutes() + minutes);
      eventEndTime.setSeconds(eventEndTime.getSeconds() + seconds);
      
      // Determine if status should change
      if (currentDate > eventEndTime && (updatedEvent.status === 'Ongoing' || updatedEvent.status === 'Upcoming')) {
        // Should be Ended
        if (updatedEvent.status !== 'Ended') {
          updatedEvent.status = 'Ended';
          statusChanged = true;
        }
      } else if (currentDate >= eventDate && currentDate <= eventEndTime && 
          updatedEvent.status === 'Upcoming' && updatedEvent.status !== 'Cancelled') {
        // Should be Ongoing
        if (updatedEvent.status !== 'Ongoing') {
          updatedEvent.status = 'Ongoing';
          statusChanged = true;
        }
      }
      
      // If status changed, we need to make an API call and update state
      if (statusChanged) {
        needsUpdate = true;
        // Make API call with minimal data using dedicated status endpoint
        axios.put(getApiUrl(API_ENDPOINTS.UPDATE_EVENT_STATUS(updatedEvent.eventId)), {
          status: updatedEvent.status
        })
          .catch(error => console.error('Error updating event status:', error));
      }
      
      return updatedEvent;
    });
    
    // Only update state if needed to avoid infinite loops
    if (needsUpdate) {
      setEvents(updatedEvents);
      
      // Update ongoingEvents separately
      const ongoing = updatedEvents.filter(event => 
        event.status === 'Ongoing'
      );
      
        setOngoingEvents(ongoing);
      }
    
    // Using a stable dependency that won't change on each render
    // We'll use a JSON string of event IDs and their statuses
  }, [JSON.stringify(events.map(e => ({ id: e.eventId, status: e.status })))]);

  // API calls - optimize fetchEvents to include loading state
  const fetchEvents = async (pageNum = page, pageSize = rowsPerPage) => {
    // Don't set loading on page changes to avoid flickering
    const isInitialLoad = pageNum === 0 && events.length === 0;
    if (isInitialLoad) {
      setLoading(true);
    }
    
    try {
      // Add cache buster to avoid caching issues
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_EVENTS_PAGINATED), {
        params: {
          page: pageNum,
          size: pageSize,
          _cache: new Date().getTime() // Cache buster
        }
      });
      
      // Get current date for status validation
      const currentDate = new Date();
      
      // Process events and validate their status before setting state
      const processedEvents = response.data.content.map(event => {
        // Parse dates and calculate end time for each event
        const eventDate = new Date(event.date);
        const [hours, minutes, seconds] = event.duration.split(':').map(Number);
        const eventEndTime = new Date(eventDate);
        eventEndTime.setHours(eventEndTime.getHours() + hours);
        eventEndTime.setMinutes(eventEndTime.getMinutes() + minutes);
        eventEndTime.setSeconds(eventEndTime.getSeconds() + seconds);
        
        // Validate and correct status if needed
        let correctedStatus = event.status;
        
        // If event is in the past, it should be Ended
        if (currentDate > eventEndTime && (event.status === 'Ongoing' || event.status === 'Upcoming')) {
          correctedStatus = 'Ended';
        }
        // If event is happening now, it should be Ongoing
        else if (currentDate >= eventDate && currentDate <= eventEndTime && 
                 event.status === 'Upcoming' && event.status !== 'Cancelled') {
          correctedStatus = 'Ongoing';
        }
        
        // Return event with corrected status
        return {
          ...event,
          status: correctedStatus,
          _parsedDate: eventDate,
          _endTime: eventEndTime
        };
      });
      
      setTotalEvents(response.data.totalElements);
      setEvents(processedEvents);
      
      // Set ongoing events
      const ongoing = processedEvents.filter(event => event.status === 'Ongoing');
      setOngoingEvents(ongoing);
      
    } catch (error) {
      console.error('Error fetching events:', error);
      showSnackbar('Failed to load events', 'error');
    } finally {
      if (isInitialLoad) {
        setLoading(false);
      }
    }
  };

  const fetchDepartments = async () => {
    try {
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_DEPARTMENTS));
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
    
    try {
      // Create a Date object from the input
      const dateObj = new Date(date);
      
      // Use utility function to preserve local time
      const formattedDate = createLocalDateISO(date);
      
      if (!formattedDate) {
        showSnackbar('Invalid date format', 'error');
        return;
      }
      
      const eventData = {
        eventName,
        departmentId,
        date: formattedDate,
        duration,
        status: 'Upcoming',
        description, // Add description to event data
        venue // Add location to event data
      };
      
      setLoading(true);
      const response = await axios.post(getApiUrl(API_ENDPOINTS.CREATE_EVENT), eventData);
      
      // Get the new event ID from the response
      const newEventId = response.data;
      console.log('Created event with ID:', newEventId);
      
      // If we have a certificate template, save it for the new event
      if (currentCertificateData) {
        try {
          console.log('Saving certificate template for new event:', newEventId);
          
          // Create a copy of the certificate with the new event ID and name
          const certificatePayload = {
            ...currentCertificateData,
            eventId: newEventId,
            eventName: eventName
          };
          
          // Save the certificate template
          const certificateResponse = await axios.post(getApiUrl(API_ENDPOINTS.CREATE_CERTIFICATE), certificatePayload);
          console.log('Certificate saved successfully:', certificateResponse.data);
          
          // Link the certificate to the event
          if (certificateResponse.data && certificateResponse.data.id) {
            try {
              await axios.post(getApiUrl(API_ENDPOINTS.LINK_CERTIFICATE_TO_EVENT), {
                certificateId: certificateResponse.data.id,
                eventId: newEventId
              });
              console.log('Certificate linked to event successfully');
            } catch (linkError) {
              console.error('Error linking certificate to event:', linkError);
            }
          }
        } catch (certError) {
          console.error('Error saving certificate template for new event:', certError);
          // Don't show error to user - the event was created successfully
        }
      }
      
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
      await axios.delete(getApiUrl(API_ENDPOINTS.DELETE_EVENT(eventId)));
      
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

  // Modify updateEventStatus function to avoid triggering the effect unnecessarily
  const updateEventStatus = async (eventId, newStatus) => {
    try {
      const eventToUpdate = events.find(e => e.eventId === eventId);
      if (!eventToUpdate) return;
      
      // Only update if status actually changed
      if (eventToUpdate.status === newStatus) return;
      
      // Create minimal update object - don't include full event to avoid date issues
      const statusUpdate = {
        eventId: eventId,
        status: newStatus
      };
      
      // Update local state immediately for responsiveness
      setEvents(prev => prev.map(event => 
        event.eventId === eventId ? { ...event, status: newStatus } : event
      ));
      
      // Also update ongoingEvents state if needed
      if (newStatus === 'Ongoing') {
        setOngoingEvents(prev => {
          // Check if already in the array to avoid duplicates
          if (!prev.some(e => e.eventId === eventId)) {
            return [...prev, { ...eventToUpdate, status: newStatus }];
          }
          return prev;
        });
      } else {
        setOngoingEvents(prev => prev.filter(event => event.eventId !== eventId));
      }
      
      // Make API call to persist changes - use the dedicated status endpoint
      await axios.put(getApiUrl(API_ENDPOINTS.UPDATE_EVENT_STATUS(eventId)), {
        status: newStatus
      });
      
      showSnackbar(`Event status updated to ${newStatus}`, 'success');
    } catch (error) {
      console.error('Error updating event status:', error);
      showSnackbar('Failed to update event status', 'error');
      
      // Revert local state in case of error
      fetchEvents();
    }
  };

  const saveEditedEvent = async () => {
    if (!eventToEdit) return;
    
    try {
      // Format duration from the separate edit components
      const formattedDuration = `${editDurationHours}:${editDurationMinutes}:${editDurationSeconds}`;
      
      // Convert the date string to ISO format for the backend
      let formattedDate = eventToEdit.date;
      if (editDate) {
        const dateObj = new Date(editDate);
        formattedDate = dateObj.toISOString();
      }
      
      // Send the full event object as required by backend
      const updatePayload = {
        ...eventToEdit,
        eventName: editEventName,
        status: editedStatus,
        date: formattedDate,
        duration: formattedDuration,
        venue: editVenue,
        description: editDescription
      };
      
      await axios.put(getApiUrl(API_ENDPOINTS.UPDATE_EVENT(eventToEdit.eventId)), updatePayload);
      
      // Update local state with all changed fields
      setEvents(events.map(event => 
        event.eventId === eventToEdit.eventId ? { 
          ...event, 
          eventName: editEventName,
          status: editedStatus, 
          date: formattedDate,
          duration: formattedDuration,
          venue: editVenue,
          description: editDescription
        } : event
      ));
      
      showSnackbar('Event updated successfully', 'success');
      closeEditDialog();
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
    setDuration('');
    setDescription(''); // Reset description
    setVenue(''); // Reset location
    setCurrentCertificateData(null);
    setEventForCertificate(null);
    setShowCertificateEditor(false);
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
    setEditEventName(event.eventName || '');
    setEditVenue(event.venue || '');
    setEditDescription(event.description || '');
    
    // Parse the date for the edit dialog - convert to datetime-local format
    if (event.date) {
      // Handle both ISO string and formatted date string
      const eventDate = new Date(event.date);
      if (!isNaN(eventDate.getTime())) {
        // Format as YYYY-MM-DDTHH:mm for datetime-local input
        const year = eventDate.getFullYear();
        const month = String(eventDate.getMonth() + 1).padStart(2, '0');
        const day = String(eventDate.getDate()).padStart(2, '0');
        const hours = String(eventDate.getHours()).padStart(2, '0');
        const minutes = String(eventDate.getMinutes()).padStart(2, '0');
        setEditDate(`${year}-${month}-${day}T${hours}:${minutes}`);
      } else {
        setEditDate('');
      }
    } else {
      setEditDate('');
    }
    
    // Parse the duration into hours, minutes, seconds for the edit dialog
    if (event.duration) {
      const [hours, minutes, seconds] = event.duration.split(':');
      setEditDurationHours(hours || '0');
      setEditDurationMinutes(minutes || '00');
      setEditDurationSeconds(seconds || '00');
      setEditDuration(event.duration);
    }
    
    setEditDialogOpen(true);
  };
  
  const closeEditDialog = () => {
    setEditDialogOpen(false);
    setEventToEdit(null);
    // Reset edit dialog state
    setEditDurationHours('0');
    setEditDurationMinutes('00');
    setEditDurationSeconds('00');
    setEditDuration('0:00:00');
    setEditEventName('');
    setEditDate('');
    setEditVenue('');
    setEditDescription('');
  };

  // File upload handlers
  // const handleUploadClick = () => {
  //   setShowUploadModal(true);
  // };

  // const handleFileChange = (event) => {
  //   setUploadedFile(event.target.files[0]);
  // };

  // const handleUploadSubmit = () => {
  //   // Handle the file upload logic here
  //   console.log('File uploaded:', uploadedFile);
  //   setShowUploadModal(false);
  // };

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
  const getDepartmentName = useCallback((id) => {
    const dept = departments.find(d => d.departmentId === id);
    return dept ? dept.name : 'Unknown Department';
  }, [departments]);
  
  // Format date for display - now using utility function
  const formatDate = useCallback((dateString) => {
    return formatDatePH(dateString);
  }, []);

  // Format date for backend - reverting to simpler approach that worked before
  const formatDateForBackend = (dateString) => {
    try {
      console.log("Original date input:", dateString);
      
      // Parse the date-time from input field (format: YYYY-MM-DDTHH:mm)
      const [datePart, timePart] = dateString.split('T');
      
      if (!datePart || !timePart) {
        console.error("Invalid date format:", dateString);
        return null;
      }
      
      // Extract date parts
      const [year, month, day] = datePart.split('-');
      
      // Extract time parts
      const [hours, minutes] = timePart.split(':');
      
      // Construct a Date object with explicit parts
      // Important: month is 0-indexed in JavaScript Date constructor
      const dateObj = new Date(year, month - 1, day, hours, minutes, 0, 0);
      
      console.log("Constructed Date object:", dateObj);
      console.log("Constructed Date string:", dateObj.toString());
      
      return dateObj;
    } catch (e) {
      console.error("Error formatting date for backend:", e);
      return null;
    }
  };

  // New function to handle opening certificate editor for a new template
  const openCertificateEditor = (event = null) => {
    console.log('Opening certificate editor for event:', event);
    
    // If an event is provided, this is for an existing event
    if (event && event.eventId) {
      // Make sure event object has all needed properties
      const fullEvent = {
        ...event,
        eventId: event.eventId, // Ensure eventId is correctly set
        eventName: event.eventName || 'Unknown Event'
      };
      
      console.log('Setting eventForCertificate:', fullEvent);
      setEventForCertificate(fullEvent);
      
      // Show loading indicator while fetching
      setLoading(true);
      
      // Try to fetch existing certificate for this event
      fetchCertificateForEvent(event.eventId)
        .then((certificateData) => {
          // Certificate data should now be set in state
          console.log('Certificate data loaded for event:', event.eventName, certificateData);
          setShowCertificateEditor(true);
        })
        .catch(error => {
          console.error('Error fetching certificate for event:', error);
          // If fetch fails, create a default template with event data
          const defaultTemplate = createDefaultTemplate(event.eventId);
          setCurrentCertificateData(defaultTemplate);
          setShowCertificateEditor(true);
        })
        .finally(() => {
          setLoading(false);
        });
    } 
    // If no event provided, but we're in the Add Event form with a certificate already
    else if (currentCertificateData) {
      // Just open the editor with the current data
      console.log('Editing current certificate template in Add Event form');
      
      // Update certificate data with current event name from form
      if (eventName) {
        setCurrentCertificateData(prev => ({
          ...prev,
          eventName: eventName
        }));
      }
      
      setShowCertificateEditor(true);
    } 
    // Creating a brand new template
    else {
      console.log('Creating new certificate template');
      
      // Initialize with current event name from form
      const defaultTemplate = createDefaultTemplate();
      if (eventName) {
        defaultTemplate.eventName = eventName;
      }
      
      setCurrentCertificateData(defaultTemplate);
      setShowCertificateEditor(true);
    }
  };

  // Close certificate editor
  const closeCertificateEditor = () => {
    setShowCertificateEditor(false);
    setEventForCertificate(null);
    setCurrentCertificateData(null);
  };

  // Fetch certificate template for a specific event
  const fetchCertificateForEvent = async (eventId) => {
    try {
      setLoading(true);
      console.log(`Fetching certificate for event ID: ${eventId}`);
      
      try {
        // Try to fetch an existing certificate
        const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_CERTIFICATE_BY_EVENT(eventId)));
        console.log('Certificate fetch response:', response);
        
        if (response.data && Object.keys(response.data).length > 0) {
          console.log('Found certificate for event:', response.data);
          setCurrentCertificateData(response.data);
          return response.data;
        } else {
          // No certificate in response data, creating new template
          console.log('No certificate data in response, creating new template');
          const defaultTemplate = createDefaultTemplate(eventId);
          setCurrentCertificateData(defaultTemplate);
          return defaultTemplate;
        }
      } catch (fetchError) {
        // Handle 404 (no certificate found) as a normal case, not an error
        console.log('No existing certificate found for this event. Creating new template.');
        
        // Create a default template
        const defaultTemplate = createDefaultTemplate(eventId);
        setCurrentCertificateData(defaultTemplate);
        return defaultTemplate;
      }
    } finally {
      setLoading(false);
    }
  };

  // Helper function to create a default certificate template
  const createDefaultTemplate = (eventId) => {
    const defaultTemplate = { ...defaultCertificate };
    
    // Add event details if available from eventForCertificate
    if (eventForCertificate?.eventName) {
      defaultTemplate.eventName = eventForCertificate.eventName;
    } 
    // Or use the event name from the form if available
    else if (eventName) {
      defaultTemplate.eventName = eventName;
    }
    
    // Add event ID if provided
    if (eventId) {
      defaultTemplate.eventId = eventId;
    }
    
    console.log('Created default template with eventName:', defaultTemplate.eventName);
    
    return defaultTemplate;
  };

  // Save certificate template
  const saveCertificateTemplate = async (certificateData) => {
    try {
      setLoading(true);
      
      // Create the payload with event information if available
      const payload = {
        ...certificateData,
        // Don't include eventId if we're creating a new event - it will be assigned later
        eventId: eventForCertificate?.eventId || null,
        eventName: eventForCertificate?.eventName || eventName || certificateData.eventName
      };
      
      // Log what we're saving for debugging
      console.log('Saving certificate template:', payload);
      console.log('Event for certificate:', eventForCertificate);
      
      let response;
      
      try {
        if (certificateData.id) {
          // Update existing certificate
          console.log('Updating existing certificate with ID:', certificateData.id);
          response = await axios.put(getApiUrl(API_ENDPOINTS.UPDATE_CERTIFICATE(certificateData.id)), payload);
        } else {
          // Create new certificate
          console.log('Creating new certificate template with eventId:', payload.eventId);
          response = await axios.post(getApiUrl(API_ENDPOINTS.CREATE_CERTIFICATE), payload);
        }
        
        console.log('Certificate save response:', response);
        
        // Get the complete data from response or fallback to input data
        const savedCertificateData = response.data || certificateData;
        
        // Set the current certificate data to show in preview
        setCurrentCertificateData(savedCertificateData);
        
        // Show success message
        showSnackbar('Certificate template saved successfully', 'success');
        
        // Link certificate to event if both certificate and event exist
        if (savedCertificateData.id && savedCertificateData.eventId) {
          try {
            console.log('Linking certificate to event', savedCertificateData.id, savedCertificateData.eventId);
            await axios.post(getApiUrl(API_ENDPOINTS.LINK_CERTIFICATE_TO_EVENT), {
              certificateId: savedCertificateData.id,
              eventId: savedCertificateData.eventId
            });
            console.log('Certificate linked to event successfully');
          } catch (linkError) {
            console.error('Error linking certificate to event:', linkError);
            // Continue execution - don't block on linking error
          }
        }
        
        // Double check certificate was saved - only if we have an event ID
        if (savedCertificateData.eventId) {
          try {
            console.log('Verifying certificate was saved correctly for eventId:', savedCertificateData.eventId);
            const verifyResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_CERTIFICATE_BY_EVENT(savedCertificateData.eventId)));
            console.log('Certificate verification response:', verifyResponse.data);
          } catch (verifyError) {
            console.error('Certificate verification failed:', verifyError);
            // Continue execution - verification is just for debugging
          }
        }
        
        // Close the editor 
        setShowCertificateEditor(false);
        
        // Return to trigger the preview update in UI
        return savedCertificateData;
      } catch (apiError) {
        console.error('API error saving certificate:', apiError);
        if (apiError.response) {
          console.error('Error response data:', apiError.response.data);
          console.error('Error response status:', apiError.response.status);
        }
        
        // If we couldn't save to the server but have data, still update the local state
        // This ensures the UI still works even if the backend is having issues
        if (certificateData) {
          setCurrentCertificateData(certificateData);
          showSnackbar('Failed to save to server, but template is available for this session', 'warning');
          return certificateData;
        }
        
        throw apiError; // Re-throw to be caught by outer try/catch
      }
      
    } catch (error) {
      console.error('Error saving certificate:', error);
      showSnackbar('Failed to save certificate template', 'error');
      return null;
    } finally {
      setLoading(false);
    }
  };

  // Apply certificate template to current event form without saving to database
  const applyTemplateToEvent = async (certificateData) => {
    try {
      console.log('Applying certificate template to current event form:', certificateData);
      console.log('Current eventForCertificate:', eventForCertificate);
      
      // Update eventName in the certificate if current form has a value
      let templateToApply = { ...certificateData };
      if (eventName) {
        templateToApply.eventName = eventName;
      }
      
      // Set the current certificate data to show in preview
      setCurrentCertificateData(templateToApply);
      
      // Only save to backend if we're editing an existing event
      if (eventForCertificate?.eventId) {
        console.log('Apply: Saving certificate for existing event with ID:', eventForCertificate.eventId);
        
        try {
          // Make sure the certificate has the event ID
          const payload = {
            ...templateToApply,
            eventId: eventForCertificate.eventId,
            // Ensure the event name is always updated correctly
            eventName: eventName || eventForCertificate.eventName || templateToApply.eventName
          };
          
          console.log('Apply: Final certificate payload:', payload);
          
          // First, check if a certificate already exists for this event
          let certificateExists = false;
          let existingCertificateId = null;
          
          try {
            const checkResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_CERTIFICATE_BY_EVENT(eventForCertificate.eventId)));
            
            // Certificate exists if we get data back with an ID
            if (checkResponse.data && checkResponse.data.id) {
              certificateExists = true;
              existingCertificateId = checkResponse.data.id;
              console.log('Apply: Found existing certificate:', existingCertificateId);
            }
          } catch (checkError) {
            // 404 means no certificate exists yet - this is expected
            if (checkError.response && checkError.response.status === 404) {
              console.log('Apply: No existing certificate found (404)');
            } else {
              console.error('Apply: Error checking for certificate:', checkError);
            }
          }
          
          let response;
          
          if (certificateExists && existingCertificateId) {
            // Update existing certificate
            console.log('Apply: Updating existing certificate:', existingCertificateId);
            response = await axios.put(getApiUrl(API_ENDPOINTS.UPDATE_CERTIFICATE(existingCertificateId)), {
              ...payload,
              id: existingCertificateId
            });
            console.log('Apply: Certificate updated successfully:', response.data);
          } else {
            // Create new certificate
            console.log('Apply: Creating new certificate for event:', eventForCertificate.eventId);
            response = await axios.post(getApiUrl(API_ENDPOINTS.CREATE_CERTIFICATE), payload);
            console.log('Apply: Certificate created successfully:', response.data);
          }
          
          // Try to link certificate to event if we got a valid response
          if (response && response.data && response.data.id) {
            try {
              await axios.post(getApiUrl(API_ENDPOINTS.LINK_CERTIFICATE_TO_EVENT), {
                certificateId: response.data.id,
                eventId: eventForCertificate.eventId
              });
              console.log('Apply: Certificate linked to event successfully');
            } catch (linkError) {
              console.error('Apply: Error linking certificate to event:', linkError);
            }
          }
          
        } catch (saveError) {
          console.error('Apply: Error saving certificate to server:', saveError);
          // Continue with local template even if we can't save to server
        }
      } else {
        console.log('Apply: No eventForCertificate with ID - not saving to database yet');
      }
      
      // Show success message
      showSnackbar('Certificate template applied to event', 'success');
      
      // Close the editor
      setShowCertificateEditor(false);
      
    } catch (error) {
      console.error('Error applying certificate template:', error);
      showSnackbar('Failed to apply certificate template', 'error');
    }
  };

  // Add handlers for pagination
  const handleChangePage = useCallback((event, newPage) => {
    setPage(newPage);
    fetchEvents(newPage);
  }, [page]);
  
  const handleChangeRowsPerPage = useCallback((event) => {
    const newRowsPerPage = parseInt(event.target.value, 10);
    setRowsPerPage(newRowsPerPage);
    setPage(0); // Reset to first page
    fetchEvents(0, newRowsPerPage);
  }, [rowsPerPage]);

  // Optimize data loading by returning to last position when navigating away and back
  useEffect(() => {
    const savedPage = sessionStorage.getItem('eventsPage');
    const savedRowsPerPage = sessionStorage.getItem('eventsRowsPerPage');
    
    if (savedPage) {
      setPage(parseInt(savedPage, 10));
    }
    
    if (savedRowsPerPage) {
      setRowsPerPage(parseInt(savedRowsPerPage, 10));
    }
    
    // Load data with saved pagination settings or defaults
    fetchEvents(
      savedPage ? parseInt(savedPage, 10) : 0,
      savedRowsPerPage ? parseInt(savedRowsPerPage, 10) : 10
    );
    fetchDepartments();
    
    // Save pagination settings when unmounting
    return () => {
      sessionStorage.setItem('eventsPage', page.toString());
      sessionStorage.setItem('eventsRowsPerPage', rowsPerPage.toString());
    };
  }, []);

  // Handle bulk delete button click
  const toggleBulkDeleteMode = () => {
    // Reset selections when toggling the mode
    setSelectedEvents([]);
    setBulkDeleteMode(!bulkDeleteMode);
  };
  
  // Handle selection of individual event
  const handleSelectEvent = (eventId) => {
    setSelectedEvents(prev => {
      if (prev.includes(eventId)) {
        return prev.filter(id => id !== eventId);
      } else {
        return [...prev, eventId];
      }
    });
  };
  
  // Handle select all events
  const handleSelectAllEvents = (event) => {
    if (event.target.checked) {
      // Select all events on the current page
      setSelectedEvents(events.map(event => event.eventId));
    } else {
      // Deselect all
      setSelectedEvents([]);
    }
  };
  
  // Open bulk delete confirmation dialog
  const openBulkDeleteDialog = () => {
    if (selectedEvents.length > 0) {
      setBulkDeleteDialogOpen(true);
    } else {
      showSnackbar('Please select at least one event to delete', 'warning');
    }
  };
  
  // Close bulk delete confirmation dialog
  const closeBulkDeleteDialog = () => {
    setBulkDeleteDialogOpen(false);
  };
  
  // Execute bulk delete operation
  const confirmBulkDelete = async () => {
    setLoading(true);
    try {
      // Delete each selected event
      const deletePromises = selectedEvents.map(eventId => 
        axios.delete(getApiUrl(API_ENDPOINTS.DELETE_EVENT(eventId)))
      );
      
      await Promise.all(deletePromises);
      
      // Remove deleted events from state
      setEvents(events.filter(event => !selectedEvents.includes(event.eventId)));
      setOngoingEvents(ongoingEvents.filter(event => !selectedEvents.includes(event.eventId)));
      
      showSnackbar(`Successfully deleted ${selectedEvents.length} events`, 'success');
      
      // Clear selections and exit bulk delete mode
      setSelectedEvents([]);
      setBulkDeleteMode(false);
    } catch (error) {
      console.error('Error deleting events:', error);
      showSnackbar('Failed to delete some events', 'error');
    } finally {
      setLoading(false);
      setBulkDeleteDialogOpen(false);
    }
  };

  return (
    <Box className="event-container">
      {/* Event Content */}
      <Box className="event-main">
        {/* Ongoing Events Section */}
        <Box sx={{ mb: 4 }}>
  <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
    Ongoing Events
  </Typography>
  <Paper 
    elevation={0} 
    sx={{ 
      p: 3, 
      borderRadius: '8px', 
      border: '1px solid #E2E8F0' 
    }}
  >
    {loading ? (
      <Grid container spacing={3}>
        {[1, 2, 3].map(index => (
          <EventCardSkeleton key={index} />
        ))}
      </Grid>
    ) : ongoingEvents.length === 0 ? (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 2 }}>
        <CalendarToday sx={{ fontSize: 40, color: '#94A3B8', mb: 1 }} />
        <Typography variant="body1" color="#1E293B" fontWeight={500}>
          No ongoing events at the moment
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
                
                <Typography variant="body2" color="#64748B" gutterBottom>
                  <strong>Duration:</strong> {event.duration}
                </Typography>
                <Typography variant="body2" color="#64748B" gutterBottom>
    <strong>Venue:</strong> {event.venue || 'N/A'}
  </Typography>

                {event.description && (
                  <Typography variant="body2" color="#64748B" sx={{ mt: 1 }}>
                    <strong>Description:</strong> {event.description}
                  </Typography>
                )}
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
</Box>

        <Typography variant="h6" fontWeight="600" color="#1E293B">
          Add New Event
        </Typography>

        <Paper 
          elevation={0} 
          sx={{ 
            p: 4, 
            borderRadius: '8px', 
            border: '1px solid #E2E8F0',
            mb: 5  // Add margin bottom to create space between this section and "All Events"
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
                      '& fieldset': {color: darkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.38)',
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
                onChange={(e) => {
                  console.log("Date input changed:", e.target.value);
                  setDate(e.target.value);
                }}
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
              <Box sx={{ display: 'flex', gap: 1 }}>
                {/* Hours input */}
                <TextField
                  variant="outlined"
                  label="Hours"
                  value={durationHours}
                  type="number"
                  inputProps={{ min: 0 }}
                  onChange={(e) => {
                    const val = e.target.value;
                    if (val === '' || /^\d+$/.test(val)) {
                      setDurationHours(val);
                      setDuration(`${val}:${durationMinutes}:${durationSeconds}`);
                    }
                  }}
                  sx={{
                    width: '33%',
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
                {/* Minutes input */}
                <TextField
                  variant="outlined"
                  label="Minutes"
                  value={durationMinutes}
                  type="number"
                  inputProps={{ min: 0, max: 59 }}
                  onChange={(e) => {
                    const val = e.target.value;
                    if (val === '' || /^\d+$/.test(val)) {
                      const formattedVal = val === '' ? '00' : val.padStart(2, '0');
                      setDurationMinutes(formattedVal);
                      setDuration(`${durationHours}:${formattedVal}:${durationSeconds}`);
                    }
                  }}
                  sx={{
                    width: '33%',
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
                {/* Seconds input */}
                <TextField
                  variant="outlined"
                  label="Seconds"
                  value={durationSeconds}
                  type="number"
                  inputProps={{ min: 0, max: 59 }}
                  onChange={(e) => {
                    const val = e.target.value;
                    if (val === '' || /^\d+$/.test(val)) {
                      const formattedVal = val === '' ? '00' : val.padStart(2, '0');
                      setDurationSeconds(formattedVal);
                      setDuration(`${durationHours}:${durationMinutes}:${formattedVal}`);
                    }
                  }}
                  sx={{
                    width: '33%',
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
              {/* Hidden field to hold the combined duration value */}
              <input type="hidden" value={duration} />
            </Box>
            
            <Box sx={{ gridColumn: "span 2" }}>
              <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                Description
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                multiline
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Enter event description..."
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
            
            <Box sx={{ gridColumn: "span 2" }}>
              <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                Venue
              </Typography>
              <TextField
                fullWidth
                variant="outlined"
                value={venue}
                onChange={(e) => setVenue(e.target.value)}
                placeholder="Enter event Venue..."
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
            
            <Box sx={{ gridColumn: "span 2" }}>
              <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                Certificate Template
              </Typography>
              <Box sx={{ 
                border: '1px dashed #CBD5E1', 
                borderRadius: '4px', 
                p: 2, 
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center', 
                alignItems: 'center',
                minHeight: '120px'
              }}>
                {currentCertificateData ? (
                  <>
                    <Box sx={{ 
                      display: 'flex', 
                      alignItems: 'flex-start',
                      mb: 2,
                      p: 2,
                      border: '1px solid #E2E8F0',
                      borderRadius: '4px',
                      width: '100%',
                      bgcolor: '#FFFFFF',
                      position: 'relative',
                      overflow: 'hidden'
                    }}>
                      <Box sx={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        height: '4px',
                        bgcolor: '#0288d1'
                      }} />
                      
                      <Box 
                        sx={{ 
                          width: '80px', 
                          height: '60px', 
                          border: `2px solid ${currentCertificateData.borderColor || '#0047AB'}`,
                          borderRadius: '4px',
                          mr: 2,
                          bgcolor: currentCertificateData.backgroundColor || '#FFFFFF',
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          justifyContent: 'center',
                          padding: '4px',
                          position: 'relative',
                          overflow: 'hidden'
                        }}
                      >
                        {/* Miniature curved header background */}
                        <Box 
                          sx={{ 
                            position: 'absolute', 
                            top: -20, 
                            left: -20, 
                            width: 60, 
                            height: 40, 
                            backgroundColor: currentCertificateData.headerColor || '#0047AB',
                            borderRadius: '50%',
                            transform: 'rotate(-45deg)',
                            zIndex: 0
                          }} 
                        />
                        
                        <Typography variant="caption" sx={{ 
                          fontSize: '8px', 
                          fontWeight: 'bold', 
                          color: currentCertificateData.textColor || '#000000',
                          zIndex: 1,
                          textAlign: 'center'
                        }}>
                          {currentCertificateData.title || 'CERTIFICATE'}
                        </Typography>
                        <Typography variant="caption" sx={{ 
                          fontSize: '6px', 
                          color: currentCertificateData.textColor || '#000000',
                          zIndex: 1,
                          textAlign: 'center'
                        }}>
                          {currentCertificateData.subtitle || 'OF ACHIEVEMENT'}
                        </Typography>
                      </Box>
                      
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="subtitle2" fontWeight="600">
                          {currentCertificateData.title || 'Certificate Template'}
                        </Typography>
                        <Typography variant="caption" sx={{ display: 'block', mb: 1 }} color="#64748B">
                          Template selected for this event
                        </Typography>
                        
                        <Chip 
                          size="small" 
                          label={currentCertificateData.eventName !== '{Event Name}' ? 
                            currentCertificateData.eventName : eventName || 'Current Event'} 
                          sx={{ 
                            bgcolor: '#E0F2FE', 
                            color: '#0288d1',
                            fontSize: '0.75rem',
                            height: '22px'
                          }} 
                        />
                      </Box>
                    </Box>
                    
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<Edit />}
                        onClick={() => openCertificateEditor()}
                        sx={{
                          borderColor: '#CBD5E1',
                          color: '#64748B',
                          '&:hover': {
                            borderColor: '#94A3B8',
                            bgcolor: 'rgba(148, 163, 184, 0.04)',
                          },
                        }}
                      >
                        Edit Template
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        startIcon={<Delete />}
                        onClick={() => {
                          setCurrentCertificateData(null);
                          showSnackbar('Certificate template removed', 'info');
                        }}
                        sx={{
                          borderColor: '#FCA5A5',
                          color: '#B91C1C',
                          '&:hover': {
                            borderColor: '#EF4444',
                            bgcolor: 'rgba(239, 68, 68, 0.04)',
                          },
                        }}
                      >
                        Remove
                      </Button>
                    </Box>
                  </>
                ) : (
                  <Button
                    variant="outlined"
                    startIcon={<BrandingWatermark />}
                    onClick={() => openCertificateEditor()}
                    sx={{
                      borderColor: '#0288d1',
                      color: '#0288d1',
                      '&:hover': {
                        borderColor: '#0277bd',
                        bgcolor: 'rgba(2, 136, 209, 0.04)',
                      },
                    }}
                  >
                    Create Certificate Template
                  </Button>
                )}
              </Box>
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
          <Box>
            <Button
              variant="outlined"
              startIcon={bulkDeleteMode ? <Close /> : <DeleteSweep />}
              onClick={toggleBulkDeleteMode}
              sx={{
                borderColor: bulkDeleteMode ? '#EF4444' : '#CBD5E1',
                color: bulkDeleteMode ? '#EF4444' : '#64748B',
                fontWeight: 500,
                textTransform: 'none',
                '&:hover': {
                  borderColor: bulkDeleteMode ? '#DC2626' : '#94A3B8',
                  bgcolor: bulkDeleteMode ? 'rgba(239, 68, 68, 0.04)' : 'rgba(148, 163, 184, 0.04)',
                },
              }}
            >
              {bulkDeleteMode ? 'Cancel' : 'Bulk Delete'}
            </Button>
          </Box>
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
                  {bulkDeleteMode && (
                    <TableCell padding="checkbox">
                      <Checkbox
                        indeterminate={selectedEvents.length > 0 && selectedEvents.length < events.length}
                        checked={events.length > 0 && selectedEvents.length === events.length}
                        onChange={handleSelectAllEvents}
                        sx={{
                          '&.Mui-checked': {
                            color: '#0288d1',
                          },
                          '&.MuiCheckbox-indeterminate': {
                            color: '#0288d1',
                          },
                        }}
                      />
                    </TableCell>
                  )}
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Event Name</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Department</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Description</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Date</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Duration</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Venue</TableCell>
                  <TableCell sx={{ fontWeight: 600, color: '#1E293B', py: 1.5 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading ? (
                  <EventTableSkeleton />
                ) : events.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={bulkDeleteMode ? 8 : 7} align="center" sx={{ py: 3 }}>
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
                        borderBottom: '1px solid #E2E8F0',
                        ...(selectedEvents.includes(event.eventId) ? { bgcolor: '#EFF6FF' } : {})
                      }}
                    >
                      {bulkDeleteMode && (
                        <TableCell padding="checkbox">
                          <Checkbox
                            checked={selectedEvents.includes(event.eventId)}
                            onChange={() => handleSelectEvent(event.eventId)}
                            sx={{
                              '&.Mui-checked': {
                                color: '#0288d1',
                              },
                            }}
                          />
                        </TableCell>
                      )}
                      <TableCell sx={{ py: 2 }}>{event.eventName}</TableCell>
                      <TableCell>{getDepartmentName(event.departmentId)}</TableCell>
                      <TableCell>
                        <Tooltip title={event.description || 'No description provided'}>
                          <Typography
                            sx={{
                              maxWidth: 200,
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap'
                            }}
                          >
                            {event.description || 'No description'}
                          </Typography>
                        </Tooltip>
                      </TableCell>
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
                      <TableCell>{event.venue || 'N/A'}</TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <IconButton 
                            size="small" 
                            onClick={() => openQrModal(event.eventId)}
                            sx={{ color: '#64748B' }}
                            title="Show QR Code"
                          >
                            <QrCode2 fontSize="small" />
                          </IconButton>
                          <IconButton 
                            size="small" 
                            onClick={() => openCertificateEditor(event)}
                            sx={{ color: '#64748B' }}
                          >
                            <BrandingWatermark />
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
          
          {/* Bulk Delete Button - shown when in bulk delete mode and events are selected */}
          {bulkDeleteMode && (
            <Box sx={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center', 
              p: 2, 
              borderTop: '1px solid #E2E8F0'
            }}>
              <Typography variant="body2" color="#64748B">
                {selectedEvents.length > 0 
                  ? `${selectedEvents.length} event${selectedEvents.length > 1 ? 's' : ''} selected` 
                  : 'Select events to delete'}
              </Typography>
              <Button
                variant="contained"
                color="error"
                startIcon={<Delete />}
                disabled={selectedEvents.length === 0}
                onClick={openBulkDeleteDialog}
                sx={{ 
                  textTransform: 'none',
                  fontWeight: 500
                }}
              >
                Delete Selected
              </Button>
            </Box>
          )}
          
          {/* Standard TablePagination component */}
          {!bulkDeleteMode && (
            <TablePagination
              component="div"
              count={totalEvents}
              page={page}
              onPageChange={handleChangePage}
              rowsPerPage={rowsPerPage}
              onRowsPerPageChange={handleChangeRowsPerPage}
              rowsPerPageOptions={[5, 10, 25, 50]}
              sx={{
                borderTop: '1px solid #E2E8F0',
                '.MuiTablePagination-selectLabel, .MuiTablePagination-displayedRows': {
                  fontSize: '0.875rem',
                  color: '#64748B'
                }
              }}
            />
          )}
        </Paper>
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
        maxWidth="md"
        fullWidth
        PaperProps={{
          sx: { borderRadius: '8px' }
        }}
      >
        <DialogTitle sx={{ borderBottom: '1px solid #E2E8F0', py: 2 }}>
          Edit Event
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography variant="body2" color="#64748B" sx={{ mb: 3 }}>
            Update the details for this event
          </Typography>
          
          {/* Event Name */}
          <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
            Event Name *
          </Typography>
          <TextField
            fullWidth
            variant="outlined"
            value={editEventName}
            onChange={(e) => setEditEventName(e.target.value)}
            placeholder="Enter event name"
            sx={{
              mb: 3,
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

          {/* Status */}
          <FormControl fullWidth sx={{ mb: 3 }}>
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

          {/* Date and Time */}
          <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
            Date and Time *
          </Typography>
          <TextField
            fullWidth
            type="datetime-local"
            variant="outlined"
            value={editDate}
            onChange={(e) => setEditDate(e.target.value)}
            sx={{
              mb: 3,
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
          
          {/* Duration */}
          <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
            Duration *
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, mb: 3 }}>
            {/* Hours input */}
            <TextField
              variant="outlined"
              label="Hours"
              value={editDurationHours}
              type="number"
              inputProps={{ min: 0 }}
              onChange={(e) => {
                const val = e.target.value;
                if (val === '' || /^\d+$/.test(val)) {
                  setEditDurationHours(val);
                  setEditDuration(`${val}:${editDurationMinutes}:${editDurationSeconds}`);
                }
              }}
              sx={{
                width: '33%',
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
            {/* Minutes input */}
            <TextField
              variant="outlined"
              label="Minutes"
              value={editDurationMinutes}
              type="number"
              inputProps={{ min: 0, max: 59 }}
              onChange={(e) => {
                const val = e.target.value;
                if (val === '' || /^\d+$/.test(val)) {
                  const formattedVal = val === '' ? '00' : val.padStart(2, '0');
                  setEditDurationMinutes(formattedVal);
                  setEditDuration(`${editDurationHours}:${formattedVal}:${editDurationSeconds}`);
                }
              }}
              sx={{
                width: '33%',
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
            {/* Seconds input */}
            <TextField
              variant="outlined"
              label="Seconds"
              value={editDurationSeconds}
              type="number"
              inputProps={{ min: 0, max: 59 }}
              onChange={(e) => {
                const val = e.target.value;
                if (val === '' || /^\d+$/.test(val)) {
                  const formattedVal = val === '' ? '00' : val.padStart(2, '0');
                  setEditDurationSeconds(formattedVal);
                  setEditDuration(`${editDurationHours}:${editDurationMinutes}:${formattedVal}`);
                }
              }}
              sx={{
                width: '33%',
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

          {/* Venue */}
          <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
            Venue
          </Typography>
          <TextField
            fullWidth
            variant="outlined"
            value={editVenue}
            onChange={(e) => setEditVenue(e.target.value)}
            placeholder="Enter venue"
            sx={{
              mb: 3,
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

          {/* Description */}
          <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
            Description
          </Typography>
          <TextField
            fullWidth
            variant="outlined"
            multiline
            rows={3}
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
            placeholder="Enter event description"
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
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid #E2E8F0' }}>
          <Button 
            onClick={closeEditDialog} 
            sx={{ color: '#64748B' }}
          >
            Cancel
          </Button>
          <Button 
            onClick={saveEditedEvent} 
            variant="contained"
            disabled={!editEventName.trim()}
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
      <Dialog
  open={qrModalOpen}
  onClose={closeQrModal}
  maxWidth="xs"
  PaperProps={{
    sx: { borderRadius: '12px' }
  }}
>
  <DialogTitle sx={{ 
    borderBottom: '1px solid #E2E8F0', 
    py: 2,
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  }}>
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <QrCode2 sx={{ color: '#0288d1' }} />
      <Typography variant="h6">Event Check-in QR</Typography>
    </Box>
    <IconButton onClick={closeQrModal} size="small">
      <Close fontSize="small" />
    </IconButton>
  </DialogTitle>
  
  <DialogContent sx={{ pt: 3, pb: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
    {currentQrEventId && (
      <>
        <Typography variant="subtitle1" color="#1E293B" sx={{ mb: 1, fontWeight: 500, textAlign: 'center' }}>
          {events.find(e => e.eventId === currentQrEventId)?.eventName || 'Event'}
        </Typography>
        
        <Typography variant="body2" color="#64748B" sx={{ mb: 3, textAlign: 'center' }}>
          Faculty members can scan this QR code to check in to this event
        </Typography>
        
        <Box sx={{ 
          width: '240px', 
          height: '240px', 
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          mb: 2,
          p: 2,
          border: '1px solid #E2E8F0',
          borderRadius: '8px',
          position: 'relative'
        }}>
          <img 
            src={getApiUrl(API_ENDPOINTS.EVENT_QR(currentQrEventId))} 
            alt="Event QR Code" 
            style={{ maxWidth: '100%', maxHeight: '100%' }}
          />
          {/* Pulsing animation effect */}
          <Box sx={{
            position: 'absolute',
            width: '100%',
            height: '100%',
            border: '2px solid #0288d1',
            borderRadius: '8px',
            animation: 'pulse 2s infinite',
            '@keyframes pulse': {
              '0%': {
                transform: 'scale(1)',
                opacity: 1
              },
              '50%': {
                transform: 'scale(1.05)',
                opacity: 0.6
              },
              '100%': {
                transform: 'scale(1)',
                opacity: 1
              }
            }
          }} />
        </Box>
        
        <Box sx={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center',
          gap: 1,
          mb: 1
        }}>
          <Chip 
            icon={<Event fontSize="small" />}
            label={formatDate(events.find(e => e.eventId === currentQrEventId)?.date || new Date().toISOString())}
            size="small"
            sx={{ 
              bgcolor: '#F1F5F9',
              color: '#475569',
              fontSize: '0.75rem'
            }}
          />
        </Box>
        
        <Typography variant="caption" color="#94A3B8" sx={{ mt: 1 }}>
          Event ID: {currentQrEventId}
        </Typography>
      </>
    )}
  </DialogContent>
  
  <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid #E2E8F0', justifyContent: 'space-between' }}>
    <Button 
      variant="outlined"
      onClick={() => {
        // Create an anchor element
        const link = document.createElement('a');
        // Set the href to the QR code URL
        link.href = getApiUrl(API_ENDPOINTS.EVENT_QR(currentQrEventId));
        // Set the download attribute with filename
        link.download = `event-${currentQrEventId}-qr.png`;
        // Append to document, click, and remove
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }}
      startIcon={<Download />}
      sx={{
        borderColor: '#CBD5E1',
        color: '#64748B',
        '&:hover': {
          borderColor: '#94A3B8',
          bgcolor: 'rgba(148, 163, 184, 0.04)',
        }
      }}
    >
      Download
    </Button>
    
    <Button
      variant="contained"
      onClick={closeQrModal}
      endIcon={<Close />}
      sx={{
        bgcolor: '#0288d1',
        '&:hover': {
          bgcolor: '#0277bd',
        }
      }}
    >
      Close
    </Button>
  </DialogActions>
</Dialog>
      {/* Upload CSV Modal */}
      {/* <Modal
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
      </Modal> */}

      {/* Certificate Editor Modal */}
      <Modal
        open={showCertificateEditor}
        onClose={closeCertificateEditor}
        aria-labelledby="certificate-editor-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: '90%',
          maxWidth: 1200,
          height: '90vh',
          bgcolor: 'background.paper',
          boxShadow: 24,
          p: 0,
          borderRadius: 2,
          overflow: 'auto'
        }}>
          <CertificateEditor 
            initialData={currentCertificateData} 
            onSave={saveCertificateTemplate}
            onApply={!eventForCertificate ? applyTemplateToEvent : undefined}
            onClose={closeCertificateEditor}
          />
        </Box>
      </Modal>
      
      {/* Bulk Delete Confirmation Dialog */}
      <Dialog
        open={bulkDeleteDialogOpen}
        onClose={closeBulkDeleteDialog}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          sx: { borderRadius: '8px' }
        }}
      >
        <DialogTitle sx={{ pb: 1 }}>
          Delete Multiple Events
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete {selectedEvents.length} selected event{selectedEvents.length > 1 ? 's' : ''}? This action cannot be undone and will also delete any certificates associated with these events.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button 
            onClick={closeBulkDeleteDialog}
            sx={{ color: '#64748B' }}
          >
            Cancel
          </Button>
          <Button 
            onClick={confirmBulkDelete} 
            variant="contained" 
            color="error"
            startIcon={<Delete />}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

// Event table loading placeholder
const EventTableSkeleton = () => (
  <>
    {[...Array(5)].map((_, index) => (
      <TableRow key={index}>
        <TableCell><Skeleton variant="text" width="70%" /></TableCell>
        <TableCell><Skeleton variant="text" width="60%" /></TableCell>
        <TableCell><Skeleton variant="text" width="80%" /></TableCell>
        <TableCell><Skeleton variant="text" width="40%" /></TableCell>
        <TableCell><Skeleton variant="rectangular" width={80} height={24} sx={{ borderRadius: 1 }} /></TableCell>
        <TableCell>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Skeleton variant="circular" width={30} height={30} />
            <Skeleton variant="circular" width={30} height={30} />
            <Skeleton variant="circular" width={30} height={30} />
          </Box>
        </TableCell>
      </TableRow>
    ))}
  </>
);