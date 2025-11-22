import { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { API_BASE_URL, getApiUrl, API_ENDPOINTS } from '../utils/api';
import * as XLSX from 'xlsx';
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
  InputAdornment,
  Avatar
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
  Search,
  InfoOutlined,
  CheckCircleOutline,
  Email,
  GetApp
} from '@mui/icons-material';
import './Dashboard.css';
import { useTheme } from '../contexts/ThemeContext';
import EventCalendar from '../components/EventCalendar';
import CertificateEditor from '../components/CertificateEditor';
import EmailStatusTracker from '../components/EmailStatusTracker';
import { getDatabase, ref, onValue, query, orderByChild, limitToLast, startAt, endAt, set, get } from 'firebase/database';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { format, isSameDay } from 'date-fns';
import AttendanceAnalytics from '../components/AttendanceAnalytics';
import EventAnalytics from '../components/EventAnalytics';
import { getAuth } from 'firebase/auth';

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
  const [attendanceTab, setAttendanceTab] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [showModal, setShowModal] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [eventError, setEventError] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [activeFilter, setActiveFilter] = useState('');
  const [totalEvents, setTotalEvents] = useState(0);
  const [showCertificateEditor, setShowCertificateEditor] = useState(false);
  const [certificateEvent, setCertificateEvent] = useState(null);
  const [certificateTemplate, setCertificateTemplate] = useState(null);
  const [attendanceLogs, setAttendanceLogs] = useState([]);
  const [loadingAttendance, setLoadingAttendance] = useState(false);
  
  // Faculty attendance states
  const [facultyLogs, setFacultyLogs] = useState([]);
  const [loadingFacultyLogs, setLoadingFacultyLogs] = useState(false);
  const [facultySearchDate, setFacultySearchDate] = useState(new Date().toISOString().split('T')[0]);
  const [facultyTimeFilter, setFacultyTimeFilter] = useState('today');
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [zoomImage, setZoomImage] = useState(null);

  // Add these new states after the existing useState declarations
  const [attendanceStats, setAttendanceStats] = useState({
    present: 0,
    late: 0,
    absent: 0
  });
  const [lateThreshold, setLateThreshold] = useState('09:00');
  const [showLateThresholdModal, setShowLateThresholdModal] = useState(false);
  const [timeWindow, setTimeWindow] = useState({
    start: '13:30',
    end: '17:00'
  });
  const [showTimeWindowModal, setShowTimeWindowModal] = useState(false);
  const [timeWindowError, setTimeWindowError] = useState(null);
  const [showLateFacultyModal, setShowLateFacultyModal] = useState(false);
  const [lateFacultyList, setLateFacultyList] = useState([]);
  const [showNoTimeInModal, setShowNoTimeInModal] = useState(false);
  const [noTimeInList, setNoTimeInList] = useState([]);
  const [filterType, setFilterType] = useState('single'); // 'single' or 'range'

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

  const [attendanceStartDate, setAttendanceStartDate] = useState('');
  const [attendanceEndDate, setAttendanceEndDate] = useState('');

  // Add state for error handling
  const [thresholdError, setThresholdError] = useState(null);

  // Add after the useState declarations
  const [exportLoading, setExportLoading] = useState(false);

  useEffect(() => {
    fetchDepartments();
    fetchEvents();
  }, []);
  
  const fetchDepartments = async () => {
    try {
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_DEPARTMENTS));
      setDepartments(response.data);
    } catch (error) {
      console.error('Error fetching departments:', error);
    }
  };
  
  // Fetch events from the backend API with date range filtering
  const fetchEvents = async (startDate = '', endDate = '') => {
    try {
      setLoading(true);
      setEventError(null); // Clear any previous event-specific errors
      
      // Use the paginated endpoint but with a large size to get all events
      // Use cache buster to prevent caching issues
      let url = getApiUrl(API_ENDPOINTS.GET_EVENTS_PAGINATED);
      const params = { 
        page: 0,
        size: 100, // Get up to 100 events at once
        _cache: new Date().getTime() // Cache buster
      };
      
      if (startDate || endDate) {
        url = getApiUrl(API_ENDPOINTS.GET_EVENTS_BY_DATE_RANGE);
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;
      }
      
      const response = await axios.get(url, { params });

      // If using paginated endpoint, response structure is different
      const responseData = response.data.content || response.data;
      const totalCount = response.data.totalElements || responseData.length;
      setTotalEvents(totalCount);
      
      // Calculate correct event statuses based on current time
      const currentDate = new Date();
      const processedEvents = responseData.map(event => {
        // Create a copy to avoid mutating the original
        const processedEvent = { ...event };
        
        // Parse event date
        const eventDate = new Date(event.date);
        
        // Calculate event end time
        const [hours, minutes, seconds] = (event.duration || '0:00:00').split(':').map(Number);
        const eventEndTime = new Date(eventDate);
        eventEndTime.setHours(eventEndTime.getHours() + hours);
        eventEndTime.setMinutes(eventEndTime.getMinutes() + minutes);
        eventEndTime.setSeconds(eventEndTime.getSeconds() + seconds);
        
        // Check if the event is actually ongoing right now
        const isNowBetweenStartAndEnd = currentDate >= eventDate && 
                                       currentDate <= eventEndTime && 
                                       event.status !== 'Cancelled' && 
                                       event.status !== 'Ended';
        
        // If the event should be ongoing (current time is between start and end time),
        // but it's not marked as such, correct the status
        if (isNowBetweenStartAndEnd && event.status !== 'Ongoing') {
          processedEvent.status = 'Ongoing';
          
          // Update the backend about this status correction using the dedicated endpoint
          axios.put(getApiUrl(API_ENDPOINTS.UPDATE_EVENT_STATUS(event.eventId)), {
            status: 'Ongoing'
          }).catch(error => {
            console.error('Failed to update event status:', error);
          });
        }
        
        // If the event should be ended (current time is after end time),
        // but it's not marked as such, correct the status
        if (currentDate > eventEndTime && 
           (event.status === 'Ongoing' || event.status === 'Upcoming')) {
          processedEvent.status = 'Ended';
          
          // Update the backend about this status correction using the dedicated endpoint
          axios.put(getApiUrl(API_ENDPOINTS.UPDATE_EVENT_STATUS(event.eventId)), {
            status: 'Ended'
          }).catch(error => {
            console.error('Failed to update event status:', error);
          });
        }
        
        // Format the date for display
        let formattedDate = 'Unknown Date';
        try {
          const date = new Date(event.date);
          formattedDate = date.toLocaleString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            timeZone: 'Asia/Manila' // Use Philippines timezone
          });
        } catch (error) {
          console.error('Error formatting date:', error);
        }
        
        return {
          id: event.eventId || `#${Math.floor(Math.random() * 90000000) + 10000000}`,
          name: event.eventName || 'Unnamed Event',
          duration: event.duration || '0 mins',
          date: formattedDate,
          status: processedEvent.status || 'Unknown',
          createdBy: event.createdBy || 'Unknown',
          departmentId: event.departmentId || null, // Added departmentId here
          venue: event.venue || 'N/A',
          _rawDate: event.date, // Keep the raw date for sorting
          _endTime: eventEndTime // Store end time for later use
        };
      });

      setEvents(processedEvents);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch events:', error);
      setEventError('Failed to load events. Please try again later.');
      setLoading(false);
    }
  };

  // Handle date filter changes
  const handleDateFilterChange = async () => {
    try {
      // Validate date range
      if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
        setEventError('Start date cannot be after end date');
        return;
      }
      
      await fetchEvents(startDate, endDate);
      setCurrentPage(1); // Reset to first page after filtering
    } catch (error) {
      console.error('Error applying date filter:', error);
      setEventError('Failed to apply date filter. Please try again.');
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
      
      const response = await axios.post(getApiUrl(API_ENDPOINTS.CREATE_EVENT), eventData);
      
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
      await axios.post(getApiUrl(API_ENDPOINTS.CREATE_EVENT_CERTIFICATE(certificateEvent.eventId)), certificateData);
      setShowCertificateEditor(false);
    } catch (error) {
      console.error('Error saving certificate template:', error);
    }
  };

  // Fetch attendance logs
  const fetchAttendanceLogs = async () => {
    try {
      setLoadingAttendance(true);
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_ATTENDANCE_LOGS));
      setAttendanceLogs(response.data || []);
      setLoadingAttendance(false);
    } catch (error) {
      console.error('Error fetching attendance logs:', error);
      setLoadingAttendance(false);
    }
  };

  const handleAttendanceTabChange = (event, newValue) => {
    setAttendanceTab(newValue);
  };

  // Add useEffect back for date changes
  useEffect(() => {
    if (mainTab === 0) {
      fetchFacultyLogs();
    }
  }, [selectedDate, mainTab]);

  const fetchFacultyLogs = () => {
    setLoadingFacultyLogs(true);
    setError(null); // Clear any previous errors
    const db = getDatabase();
    const logsRef = ref(db, 'timeLogs');
    
    onValue(logsRef, (snapshot) => {
      const dailyLogs = [];
      
      // First level: user IDs
      snapshot.forEach((userSnapshot) => {
        const userId = userSnapshot.key;
        const userEntries = [];
        
        // Second level: individual log entries
        userSnapshot.forEach((logSnapshot) => {
          const log = logSnapshot.val();
          const logDate = new Date(log.timestamp);
          
          // Only process logs from the selected date
          if (isSameDay(logDate, selectedDate)) {
            userEntries.push({
              id: logSnapshot.key,
              userId,
              ...log,
              time: format(logDate, 'hh:mm:ss a'),
              attendanceBadge: log.attendanceBadge || 'Unknown'
            });
          }
        });
        
        // If we have entries for this user on this day
        if (userEntries.length > 0) {
          // Sort entries by timestamp
          userEntries.sort((a, b) => a.timestamp - b.timestamp);
          
          // Group TimeIn and TimeOut entries
          const timeInOuts = [];
          let currentTimeIn = null;
          
          userEntries.forEach(entry => {
            if (entry.type === 'TimeIn') {
              currentTimeIn = entry;
            } else if (entry.type === 'TimeOut' && currentTimeIn) {
              timeInOuts.push({
                timeIn: currentTimeIn,
                timeOut: entry
              });
              currentTimeIn = null;
            }
          });
          
          // If there's a TimeIn without TimeOut, add it too
          if (currentTimeIn) {
            timeInOuts.push({
              timeIn: currentTimeIn,
              timeOut: null
            });
          }
          
          // Add each TimeIn-TimeOut pair as a separate row
          timeInOuts.forEach((pair, index) => {
            dailyLogs.push({
              id: `${pair.timeIn.id}-${index}`,
              userId: userId,
              firstName: pair.timeIn.firstName,
              email: pair.timeIn.email,
              imageUrl: pair.timeIn.imageUrl,
              timeIn: pair.timeIn,
              timeOut: pair.timeOut,
              entryNumber: index + 1,
              attendanceBadge: pair.timeIn.attendanceBadge
            });
          });
        }
      });
      
      setFacultyLogs(dailyLogs);
      setLoadingFacultyLogs(false);
    }, (error) => {
      console.error('Error fetching faculty logs:', error);
      setFacultyLogs([]);
      setLoadingFacultyLogs(false);
      setError('Failed to load faculty logs. Please check your permissions or try again later.');
    });
  };

  // Add a helper function to calculate duration
  const calculateDuration = (timeIn, timeOut) => {
    if (!timeIn || !timeOut) return null;
    const duration = (timeOut.timestamp - timeIn.timestamp) / 1000; // Convert to seconds
    const hours = Math.floor(duration / 3600);
    const minutes = Math.floor((duration % 3600) / 60);
    return `${hours}h ${minutes}m`;
  };

  const formatTimeDisplay = (timeString) => {
    if (!timeString) return '--:--';
    try {
      const [hour, minute] = timeString.split(':').map(Number);
      if (Number.isNaN(hour) || Number.isNaN(minute)) return timeString;
      const date = new Date();
      date.setHours(hour);
      date.setMinutes(minute);
      date.setSeconds(0);
      date.setMilliseconds(0);
      return format(date, 'h:mm a');
    } catch (error) {
      console.error('Error formatting time string:', error);
      return timeString;
    }
  };

  const handleMainTabChange = (event, newValue) => {
    setMainTab(newValue);
    
    // Reset the inner tab when changing main tab
    setActiveTab(0);
    
    // Load attendance data if switching to attendance tab (now index 0)
    if (newValue === 0) {
      fetchFacultyLogs();
    }
  };

  // Add the Image Zoom Modal component
  const ImageZoomModal = ({ imageUrl, onClose }) => (
    <Modal
      open={Boolean(imageUrl)}
      onClose={onClose}
      aria-labelledby="zoom-image"
    >
      <Box sx={{
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        maxWidth: '90vw',
        maxHeight: '90vh',
        bgcolor: 'background.paper',
        boxShadow: 24,
        p: 1,
        borderRadius: 2,
        outline: 'none'
      }}>
        <IconButton
          onClick={onClose}
          sx={{
            position: 'absolute',
            right: 8,
            top: 8,
            bgcolor: 'rgba(0, 0, 0, 0.5)',
            color: 'white',
            '&:hover': {
              bgcolor: 'rgba(0, 0, 0, 0.7)'
            }
          }}
        >
          <Close />
        </IconButton>
        <img
          src={imageUrl}
          alt="Zoomed verification"
          style={{
            maxWidth: '100%',
            maxHeight: '85vh',
            objectFit: 'contain'
          }}
        />
      </Box>
    </Modal>
  );

  // Update the calculateAttendanceStats function
  const calculateAttendanceStats = async () => {
    try {
      const db = getDatabase();
      const logsRef = ref(db, 'timeLogs');
      
      // Initialize counters
      let onTimeCount = 0;
      let lateCount = 0;
      let absentCount = 0;
      const processedUsers = new Set(); // To track unique users

      // Get all faculty (excluding admins)
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_ALL_USERS));
      const facultyList = response.data.filter(user => user.role !== 'ADMIN');
      const totalFaculty = facultyList.length;

      onValue(logsRef, (snapshot) => {
        snapshot.forEach((userSnapshot) => {
          const userId = userSnapshot.key;
          let latestTimestamp = 0;
          let latestBadge = null;

          // Find the latest entry for this user on the selected date
          userSnapshot.forEach((logSnapshot) => {
            const log = logSnapshot.val();
            const logDate = new Date(log.timestamp);
            
            if (isSameDay(logDate, selectedDate) && log.timestamp > latestTimestamp) {
              latestTimestamp = log.timestamp;
              latestBadge = log.attendanceBadge;
            }
          });

          // Only count each user once based on their latest badge for the day
          if (latestBadge && !processedUsers.has(userId)) {
            processedUsers.add(userId);
            
            switch (latestBadge) {
              case 'On Time':
                onTimeCount++;
                break;
              case 'Late':
                lateCount++;
                break;
              case 'Absent':
                absentCount++;
                break;
            }
          }
        });

        // Calculate absent count for users not found in logs
        const totalProcessed = processedUsers.size;
        const remainingAbsent = totalFaculty - totalProcessed;
        absentCount += remainingAbsent;

        setAttendanceStats({
          present: onTimeCount,
          late: lateCount,
          absent: absentCount
        });

        // Update lists for modals
        updateLateFacultyList(snapshot);
        updateNoTimeInList(facultyList, processedUsers);
      });
    } catch (error) {
      console.error('Error calculating attendance stats:', error);
    }
  };

  // Add new helper function to update late faculty list
  const updateLateFacultyList = (snapshot) => {
    const lateFaculty = [];
    
    snapshot.forEach((userSnapshot) => {
      let latestEntry = null;
      let latestTimestamp = 0;

      userSnapshot.forEach((logSnapshot) => {
        const log = logSnapshot.val();
        const logDate = new Date(log.timestamp);
        
        if (isSameDay(logDate, selectedDate) && 
            log.timestamp > latestTimestamp && 
            log.attendanceBadge === 'Late') {
          latestTimestamp = log.timestamp;
          latestEntry = log;
        }
      });

      if (latestEntry) {
        lateFaculty.push({
          name: latestEntry.firstName,
          timeIn: new Date(latestEntry.timestamp).toLocaleTimeString(),
          imageUrl: latestEntry.imageUrl,
          email: latestEntry.email
        });
      }
    });

    setLateFacultyList(lateFaculty);
  };

  // Add new helper function to update no time-in list
  const updateNoTimeInList = (facultyList, processedUsers) => {
    const absentFaculty = facultyList.filter(faculty => !processedUsers.has(faculty.userId));
    setNoTimeInList(absentFaculty);
  };

  // Update useEffect to include attendance stats calculation
  useEffect(() => {
    if (mainTab === 0) {
      fetchFacultyLogs();
      calculateAttendanceStats();
    }
  }, [selectedDate, mainTab, lateThreshold]);

  // Update loadLateThreshold function
  const loadLateThreshold = async () => {
    try {
      const db = getDatabase();
      const thresholdRef = ref(db, 'settings/lateThreshold');
      
      // First try to get the existing value
      const snapshot = await get(thresholdRef);
      if (snapshot.exists()) {
        setLateThreshold(snapshot.val());
      }

      // Set up real-time listener
      onValue(thresholdRef, (snapshot) => {
        if (snapshot.exists()) {
          setLateThreshold(snapshot.val());
        }
      }, (error) => {
        console.error('Error loading late threshold:', error);
        setThresholdError('Failed to load late threshold. Please check your permissions.');
      });
    } catch (error) {
      console.error('Error loading late threshold:', error);
      setThresholdError('Failed to load late threshold. Please check your permissions.');
    }
  };

  const loadTimeWindow = async () => {
    try {
      const db = getDatabase();
      const timeWindowRef = ref(db, 'settings/timeWindow');

      const snapshot = await get(timeWindowRef);
      if (snapshot.exists()) {
        const data = snapshot.val();
        setTimeWindow({
          start: data.start || '13:30',
          end: data.end || '17:00'
        });
        setTimeWindowError(null);
      }

      onValue(timeWindowRef, (snapshot) => {
        if (snapshot.exists()) {
          const data = snapshot.val();
          setTimeWindow({
            start: data.start || '13:30',
            end: data.end || '17:00'
          });
          setTimeWindowError(null);
        }
      }, (error) => {
        console.error('Error loading time window:', error);
        setTimeWindowError('Failed to load time-in window. Please check your permissions.');
      });
    } catch (error) {
      console.error('Error loading time window:', error);
      setTimeWindowError('Failed to load time-in window. Please check your permissions.');
    }
  };

  // Update saveLateThreshold function
  const saveLateThreshold = async (newThreshold) => {
    try {
      const auth = getAuth();
      const user = auth.currentUser;
      
      if (!user) {
        setThresholdError('You must be logged in to change the late threshold.');
        return false;
      }

      const db = getDatabase();
      const thresholdRef = ref(db, 'settings/lateThreshold');
      await set(thresholdRef, newThreshold);
      setThresholdError(null);
      return true;
    } catch (error) {
      console.error('Error saving late threshold:', error);
      if (error.message.includes('permission_denied')) {
        setThresholdError('You do not have permission to change the late threshold. Only administrators can modify this setting.');
      } else {
        setThresholdError('Failed to save late threshold. Please try again.');
      }
      return false;
    }
  };

  const saveTimeWindow = async (start, end) => {
    try {
      const auth = getAuth();
      const user = auth.currentUser;

      if (!user) {
        setTimeWindowError('You must be logged in to change the time-in window.');
        return false;
      }

      const db = getDatabase();
      const timeWindowRef = ref(db, 'settings/timeWindow');
      await set(timeWindowRef, { start, end });
      setTimeWindow({
        start,
        end
      });
      setTimeWindowError(null);
      return true;
    } catch (error) {
      console.error('Error saving time window:', error);
      if (error.message?.includes('permission_denied')) {
        setTimeWindowError('You do not have permission to change the time-in window. Only administrators can modify this setting.');
      } else {
        setTimeWindowError('Failed to save time-in window. Please try again.');
      }
      return false;
    }
  };

  // Load late threshold when component mounts
  useEffect(() => {
    loadLateThreshold();
    loadTimeWindow();
  }, []);

  // Update the LateThresholdModal component
  const LateThresholdModal = () => {
    const [tempThreshold, setTempThreshold] = useState(lateThreshold);
    const [saving, setSaving] = useState(false);

    const handleSave = async () => {
      setSaving(true);
      setThresholdError(null);
      const success = await saveLateThreshold(tempThreshold);
      setSaving(false);
      if (success) {
        setShowLateThresholdModal(false);
      }
    };

    return (
      <Modal
        open={showLateThresholdModal}
        onClose={() => {
          setShowLateThresholdModal(false);
          setThresholdError(null);
        }}
        aria-labelledby="late-threshold-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 400,
          bgcolor: darkMode ? 'var(--card-bg)' : 'background.paper',
          boxShadow: 24,
          borderRadius: 2,
          p: 3,
          border: darkMode ? '1px solid var(--border-color)' : 'none'
        }}>
          <Typography variant="h6" sx={{ mb: 2, color: darkMode ? 'var(--text-primary)' : 'inherit' }}>Set Late Threshold</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Faculty members who time in after this time will be marked as late.
          </Typography>
          <TextField
            type="time"
            value={tempThreshold}
            onChange={(e) => setTempThreshold(e.target.value)}
            fullWidth
            sx={{ mb: thresholdError ? 1 : 3 }}
          />
          {thresholdError && (
            <Typography 
              color="error" 
              variant="body2" 
              sx={{ mb: 2 }}
            >
              {thresholdError}
            </Typography>
          )}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            <Button 
              onClick={() => {
                setShowLateThresholdModal(false);
                setThresholdError(null);
              }}
              sx={{ color: darkMode ? 'var(--text-secondary)' : 'inherit' }}
            >
              Cancel
            </Button>
            <Button 
              variant="contained" 
              onClick={handleSave}
              disabled={saving}
              sx={{
                bgcolor: darkMode ? 'var(--accent-color)' : undefined,
                '&:hover': {
                  bgcolor: darkMode ? 'var(--accent-hover)' : undefined
                }
              }}
            >
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </Box>
        </Box>
      </Modal>
    );
  };

  const TimeWindowModal = () => {
    const [tempStart, setTempStart] = useState(timeWindow.start);
    const [tempEnd, setTempEnd] = useState(timeWindow.end);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
      if (showTimeWindowModal) {
        setTempStart(timeWindow.start);
        setTempEnd(timeWindow.end);
      }
    }, [showTimeWindowModal, timeWindow]);

    const handleSave = async () => {
      setTimeWindowError(null);

      if (!tempStart || !tempEnd) {
        setTimeWindowError('Both start and end times are required.');
        return;
      }

      if (tempStart >= tempEnd) {
        setTimeWindowError('Start time must be earlier than end time.');
        return;
      }

      setSaving(true);
      const success = await saveTimeWindow(tempStart, tempEnd);
      setSaving(false);
      if (success) {
        setShowTimeWindowModal(false);
      }
    };

    return (
      <Modal
        open={showTimeWindowModal}
        onClose={() => {
          setShowTimeWindowModal(false);
          setTimeWindowError(null);
        }}
        aria-labelledby="time-window-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 420,
          bgcolor: darkMode ? 'var(--card-bg)' : 'background.paper',
          boxShadow: 24,
          borderRadius: 2,
          p: 3,
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
          border: darkMode ? '1px solid var(--border-color)' : 'none'
        }}>
          <Typography variant="h6" sx={{ color: darkMode ? 'var(--text-primary)' : 'inherit' }}>
            Set Allowed Time-In Window
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Faculty members may only time-in between these times. This setting applies across all devices.
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <TextField
              type="time"
              label="Start Time"
              value={tempStart}
              onChange={(e) => setTempStart(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ flex: 1, minWidth: 150 }}
            />
            <TextField
              type="time"
              label="End Time"
              value={tempEnd}
              onChange={(e) => setTempEnd(e.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ flex: 1, minWidth: 150 }}
            />
          </Box>
          {timeWindowError && (
            <Typography color="error" variant="body2">
              {timeWindowError}
            </Typography>
          )}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            <Button
              onClick={() => {
                setShowTimeWindowModal(false);
                setTimeWindowError(null);
              }}
              sx={{ color: darkMode ? 'var(--text-secondary)' : 'inherit' }}
            >
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={saving}
              sx={{
                bgcolor: darkMode ? 'var(--accent-color)' : undefined,
                '&:hover': {
                  bgcolor: darkMode ? 'var(--accent-hover)' : undefined
                }
              }}
            >
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </Box>
        </Box>
      </Modal>
    );
  };

  // Add this function after calculateAttendanceStats
  const getLateAttendanceDetails = () => {
    try {
      const lateList = facultyLogs
        .filter(log => {
          if (log.timeIn) {
            const timeInDate = new Date(log.timeIn.timestamp);
            const [thresholdHour, thresholdMinute] = lateThreshold.split(':').map(Number);
            const thresholdDate = new Date(selectedDate);
            thresholdDate.setHours(thresholdHour, thresholdMinute, 0);
            
            return isSameDay(timeInDate, selectedDate) && timeInDate > thresholdDate;
          }
          return false;
        })
        .map(log => ({
          name: log.firstName,
          timeIn: new Date(log.timeIn.timestamp).toLocaleTimeString(),
          imageUrl: log.timeIn.imageUrl,
          email: log.email
        }));

      setLateFacultyList(lateList);
      setShowLateFacultyModal(true);
    } catch (error) {
      console.error('Error getting late attendance details:', error);
    }
  };

  // Add this function after getLateAttendanceDetails
  const getNoTimeInDetails = () => {
    setShowNoTimeInModal(true);
  };

  // Add the Late Faculty Modal component before the return statement
  const LateFacultyModal = () => (
    <Modal
      open={showLateFacultyModal}
      onClose={() => setShowLateFacultyModal(false)}
      aria-labelledby="late-faculty-modal"
    >
      <Box sx={{
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        width: 600,
        bgcolor: 'background.paper',
        boxShadow: 24,
        borderRadius: 2,
        p: 0,
        maxHeight: '80vh',
        display: 'flex',
        flexDirection: 'column'
      }}>
        <Box sx={{ 
          p: 2, 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          borderBottom: '1px solid #E2E8F0',
          bgcolor: '#F8FAFC'
        }}>
          <Box>
            <Typography variant="h6" fontWeight="600" color="#1E293B">
              Late Faculty Members
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {format(selectedDate, 'MMMM d, yyyy')} - After {lateThreshold}
            </Typography>
          </Box>
          <IconButton onClick={() => setShowLateFacultyModal(false)}>
            <Close />
          </IconButton>
        </Box>

        <Box sx={{ overflow: 'auto', flex: 1, p: 2 }}>
          {lateFacultyList.length === 0 ? (
            <Box sx={{ 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center', 
              justifyContent: 'center',
              p: 4
            }}>
              <AccessTime sx={{ fontSize: 48, color: '#CBD5E1', mb: 2 }} />
              <Typography variant="body1" fontWeight="500">
                No late faculty members
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Everyone arrived before {lateThreshold}
              </Typography>
            </Box>
          ) : (
            <List sx={{ width: '100%' }}>
              {lateFacultyList.map((faculty, index) => (
                <ListItem
                  key={index}
                  sx={{
                    py: 2,
                    borderBottom: index < lateFacultyList.length - 1 ? '1px solid #E2E8F0' : 'none'
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                    {faculty.imageUrl ? (
                      <Avatar
                        src={faculty.imageUrl}
                        alt={faculty.name}
                        sx={{ width: 40, height: 40, mr: 2 }}
                      />
                    ) : (
                      <Avatar sx={{ width: 40, height: 40, mr: 2, bgcolor: '#0288d1' }}>
                        {faculty.name.charAt(0)}
                      </Avatar>
                    )}
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="subtitle2" fontWeight="600">
                        {faculty.name}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {faculty.email}
                      </Typography>
                    </Box>
                    <Chip
                      label={faculty.timeIn}
                      color="warning"
                      variant="outlined"
                      size="small"
                      icon={<AccessTime sx={{ fontSize: 16 }} />}
                    />
                  </Box>
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Box>
    </Modal>
  );

  // Add the No Time-in Faculty Modal component before the return statement
  const NoTimeInModal = () => (
    <Modal
      open={showNoTimeInModal}
      onClose={() => setShowNoTimeInModal(false)}
      aria-labelledby="no-time-in-modal"
    >
      <Box sx={{
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        width: 600,
        bgcolor: 'background.paper',
        boxShadow: 24,
        borderRadius: 2,
        p: 0,
        maxHeight: '80vh',
        display: 'flex',
        flexDirection: 'column'
      }}>
        <Box sx={{ 
          p: 2, 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          borderBottom: '1px solid #E2E8F0',
          bgcolor: '#F8FAFC'
        }}>
          <Box>
            <Typography variant="h6" fontWeight="600" color="#1E293B">
              Faculty Without Time-in
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {format(selectedDate, 'MMMM d, yyyy')}
            </Typography>
          </Box>
          <IconButton onClick={() => setShowNoTimeInModal(false)}>
            <Close />
          </IconButton>
        </Box>

        <Box sx={{ overflow: 'auto', flex: 1, p: 2 }}>
          {noTimeInList.length === 0 ? (
            <Box sx={{ 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center', 
              justifyContent: 'center',
              p: 4
            }}>
              <CheckCircleOutline sx={{ fontSize: 48, color: '#CBD5E1', mb: 2 }} />
              <Typography variant="body1" fontWeight="500">
                Everyone has timed in today
              </Typography>
              <Typography variant="body2" color="text.secondary">
                No missing time-ins for {format(selectedDate, 'MMMM d, yyyy')}
              </Typography>
            </Box>
          ) : (
            <List sx={{ width: '100%' }}>
              {noTimeInList.map((faculty, index) => (
                <ListItem
                  key={faculty.userId}
                  sx={{
                    py: 2,
                    borderBottom: index < noTimeInList.length - 1 ? '1px solid #E2E8F0' : 'none'
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                    {faculty.profilePictureUrl ? (
                      <Avatar
                        src={faculty.profilePictureUrl}
                        alt={`${faculty.firstName} ${faculty.lastName}`}
                        sx={{ width: 40, height: 40, mr: 2 }}
                      />
                    ) : (
                      <Avatar sx={{ width: 40, height: 40, mr: 2, bgcolor: '#EF4444' }}>
                        {faculty.firstName?.charAt(0)}
                      </Avatar>
                    )}
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="subtitle2" fontWeight="600">
                        {`${faculty.firstName} ${faculty.lastName}`}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {faculty.email}
                      </Typography>
                    </Box>
                    <Chip
                      label="No Time-in"
                      color="error"
                      variant="outlined"
                      size="small"
                    />
                  </Box>
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Box>
    </Modal>
  );

  // Helper to aggregate attendance by hour
  const getAttendanceAnalyticsData = (logs) => {
    const hourMap = {};
    logs.forEach(log => {
      if (log.timeIn) {
        const date = new Date(log.timeIn.timestamp);
        const hour = date.getHours();
        const label = `${hour.toString().padStart(2, '0')}:00`;
        if (!hourMap[label]) hourMap[label] = { time: label, timeInCount: 0, timeOutCount: 0 };
        hourMap[label].timeInCount++;
      }
      if (log.timeOut) {
        const date = new Date(log.timeOut.timestamp);
        const hour = date.getHours();
        const label = `${hour.toString().padStart(2, '0')}:00`;
        if (!hourMap[label]) hourMap[label] = { time: label, timeInCount: 0, timeOutCount: 0 };
        hourMap[label].timeOutCount++;
      }
    });
    // Sort by hour
    return Object.values(hourMap).sort((a, b) => a.time.localeCompare(b.time));
  };

  // Helper to aggregate events by status
  const getEventAnalyticsData = (events) => {
    const statusMap = {};
    events.forEach(event => {
      const status = event.status || 'Unknown';
      if (!statusMap[status]) statusMap[status] = 0;
      statusMap[status]++;
    });
    return Object.entries(statusMap).map(([name, value]) => ({ name, value }));
  };

  // Add this function after fetchFacultyLogs
  const handleAttendanceDateFilterChange = () => {
    try {
      // Validate date range
      if (attendanceStartDate && attendanceEndDate && 
          new Date(attendanceStartDate) > new Date(attendanceEndDate)) {
        setError('Start date cannot be after end date');
        return;
      }
      
      setLoadingFacultyLogs(true);
      const db = getDatabase();
      const logsRef = ref(db, 'timeLogs');
      
      onValue(logsRef, (snapshot) => {
        const dailyLogs = [];
        
        snapshot.forEach((userSnapshot) => {
          const userId = userSnapshot.key;
          const userEntries = [];
          
          userSnapshot.forEach((logSnapshot) => {
            const log = logSnapshot.val();
            const logDate = new Date(log.timestamp);
            
            // Check if the log date falls within the selected range
            const isWithinRange = (!attendanceStartDate || logDate >= new Date(attendanceStartDate)) &&
                                (!attendanceEndDate || logDate <= new Date(new Date(attendanceEndDate).setHours(23, 59, 59)));
            
            if (isWithinRange) {
              userEntries.push({
                id: logSnapshot.key,
                userId,
                ...log,
                time: format(logDate, 'hh:mm:ss a'),
                attendanceBadge: log.attendanceBadge || 'Unknown'
              });
            }
          });
          
          if (userEntries.length > 0) {
            userEntries.sort((a, b) => a.timestamp - b.timestamp);
            
            const timeInOuts = [];
            let currentTimeIn = null;
            
            userEntries.forEach(entry => {
              if (entry.type === 'TimeIn') {
                currentTimeIn = entry;
              } else if (entry.type === 'TimeOut' && currentTimeIn) {
                timeInOuts.push({
                  timeIn: currentTimeIn,
                  timeOut: entry
                });
                currentTimeIn = null;
              }
            });
            
            if (currentTimeIn) {
              timeInOuts.push({
                timeIn: currentTimeIn,
                timeOut: null
              });
            }
            
            timeInOuts.forEach((pair, index) => {
              dailyLogs.push({
                id: `${pair.timeIn.id}-${index}`,
                userId: userId,
                firstName: pair.timeIn.firstName,
                email: pair.timeIn.email,
                imageUrl: pair.timeIn.imageUrl,
                timeIn: pair.timeIn,
                timeOut: pair.timeOut,
                entryNumber: index + 1,
                attendanceBadge: pair.timeIn.attendanceBadge
              });
            });
          }
        });
        
        setFacultyLogs(dailyLogs);
        setLoadingFacultyLogs(false);
      }, (error) => {
        console.error('Error fetching faculty logs:', error);
        setFacultyLogs([]);
        setLoadingFacultyLogs(false);
        setError('Failed to load faculty logs. Please check your permissions or try again later.');
      });
    } catch (error) {
      console.error('Error applying date filter:', error);
      setError('Failed to apply date filter. Please try again.');
      setLoadingFacultyLogs(false);
    }
  };

  // Add this function after handleAttendanceDateFilterChange
  const handleClearAttendanceDateFilter = () => {
    setAttendanceStartDate('');
    setAttendanceEndDate('');
    fetchFacultyLogs();
  };

  // Add this function after handleClearAttendanceDateFilter
  const toggleFilterType = (type) => {
    setFilterType(type);
    // Clear all date filters when switching
    setSelectedDate(new Date());
    setAttendanceStartDate('');
    setAttendanceEndDate('');
    fetchFacultyLogs();
  };

  // Add the export function before the return statement
  const handleExportAttendance = async () => {
    try {
      setExportLoading(true);
      
      // Get the filename based on the filter type
      let filename;
      if (filterType === 'single') {
        filename = `Faculty_Attendance_${format(selectedDate, 'MMM_d_yyyy')}.xlsx`;
      } else {
        filename = `Faculty_Attendance_${format(new Date(attendanceStartDate), 'MMM_d_yyyy')}_to_${format(new Date(attendanceEndDate), 'MMM_d_yyyy')}.xlsx`;
      }

      // Prepare the export data
      const exportData = facultyLogs.map(log => ({
        'Faculty Name': log.firstName,
        'Email': log.email,
        'Date': format(new Date(log.timeIn.timestamp), 'MMMM d, yyyy'),
        'Time In': log.timeIn ? format(new Date(log.timeIn.timestamp), 'hh:mm:ss a') : 'N/A',
        'Time Out': log.timeOut ? format(new Date(log.timeOut.timestamp), 'hh:mm:ss a') : 'Active Session',
        'Duration': calculateDuration(log.timeIn, log.timeOut) || 'In Progress',
        'Status': log.attendanceBadge || 'Unknown'
      }));

      // Add summary information
      const summaryData = [
        {
          'Faculty Name': 'ATTENDANCE SUMMARY',
          'Email': '',
          'Date': filterType === 'single' ? format(selectedDate, 'MMMM d, yyyy') : `${format(new Date(attendanceStartDate), 'MMMM d, yyyy')} to ${format(new Date(attendanceEndDate), 'MMMM d, yyyy')}`,
          'Time In': '',
          'Time Out': '',
          'Duration': '',
          'Status': ''
        },
        {
          'Faculty Name': 'On Time:',
          'Email': attendanceStats.present.toString(),
          'Date': '',
          'Time In': '',
          'Time Out': '',
          'Duration': '',
          'Status': ''
        },
        {
          'Faculty Name': 'Late:',
          'Email': attendanceStats.late.toString(),
          'Date': '',
          'Time In': '',
          'Time Out': '',
          'Duration': '',
          'Status': ''
        },
        {
          'Faculty Name': 'No Time-in:',
          'Email': attendanceStats.absent.toString(),
          'Date': '',
          'Time In': '',
          'Time Out': '',
          'Duration': '',
          'Status': ''
        },
        {}, // Empty row for spacing
        ...exportData
      ];

      // Create workbook and add the data
      const worksheet = XLSX.utils.json_to_sheet(summaryData);
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Attendance Records');

      // Auto-size columns
      const max_width = summaryData.reduce((w, r) => Math.max(w, r['Faculty Name'] ? r['Faculty Name'].length : 0), 10);
      const col_width = Math.min(max_width, 50);
      worksheet['!cols'] = [
        { wch: col_width }, // Faculty Name
        { wch: 30 }, // Email
        { wch: 20 }, // Date
        { wch: 15 }, // Time In
        { wch: 15 }, // Time Out
        { wch: 15 }, // Duration
        { wch: 15 }  // Status
      ];

      // Style the summary section
      const summaryRange = XLSX.utils.decode_range('A1:G4');
      for (let R = summaryRange.s.r; R <= summaryRange.e.r; ++R) {
        for (let C = summaryRange.s.c; C <= summaryRange.e.c; ++C) {
          const cell_address = { c: C, r: R };
          const cell_ref = XLSX.utils.encode_cell(cell_address);
          if (!worksheet[cell_ref]) worksheet[cell_ref] = { t: 's', v: '' };
          worksheet[cell_ref].s = {
            font: { bold: true },
            fill: { fgColor: { rgb: "EFEFEF" } }
          };
        }
      }

      // Save the file
      XLSX.writeFile(workbook, filename);
    } catch (error) {
      console.error('Error exporting attendance:', error);
      setError('Failed to export attendance data. Please try again.');
    } finally {
      setExportLoading(false);
    }
  };

  return (
    <Box 
      className={`dashboard-container ${darkMode ? 'dark-mode' : ''}`}
      sx={{ width: '100%', display: 'flex', flexDirection: 'column' }}
    >
      {/* Dashboard Content */}
      <Box 
        className="dashboard-main"
        sx={{ width: '100%', maxWidth: '100%', margin: '0 auto', flex: 1 }}
      >
        
        {/* Main Tabs */}
        <Box sx={{ mb: 4 }}>
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
              label="Daily Attendance Record" 
              icon={<Group />} 
              iconPosition="start"
            />
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
              label="Email Status" 
              icon={<Email />} 
              iconPosition="start"
            />
          </Tabs>
        </Box>

        {/* Attendance Logs Tab (now Day to Day Attendance Record) */}
        {mainTab === 0 && (
          <>
            <AttendanceAnalytics
              data={getAttendanceAnalyticsData(facultyLogs)}
              title="Faculty Time-In/Out Distribution"
            />
            {/* Existing Attendance UI */}
            <Box sx={{ 
              bgcolor: darkMode ? 'var(--card-bg)' : 'white', 
              borderRadius: '8px', 
              boxShadow: '0 4px 20px rgba(0,0,0,0.05)', 
              overflow: 'hidden',
              border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)',
              p: 3
            }}>
              {/* Replace the existing DatePicker in the attendance section with this new filter UI */}
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: { xs: 'wrap', md: 'nowrap' }, gap: 2 }}>
                <Box>
                  <Typography variant="h6" fontWeight="600" color="#1E293B">
                    Faculty Day-to-Day Attendance
                  </Typography>
                  {filterType === 'range' && attendanceStartDate && attendanceEndDate && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Showing records from {format(new Date(attendanceStartDate), 'MMMM d, yyyy')} to {format(new Date(attendanceEndDate), 'MMMM d, yyyy')}
                    </Typography>
                  )}
                </Box>
                
                <Box sx={{ 
                  display: 'flex', 
                  gap: 2,
                  flexWrap: { xs: 'wrap', md: 'nowrap' },
                  alignItems: 'center'
                }}>
                  <Button
                    variant="outlined"
                    color="primary"
                    disabled={exportLoading || facultyLogs.length === 0}
                    onClick={handleExportAttendance}
                    startIcon={exportLoading ? <CircularProgress size={20} /> : <GetApp />}
                    sx={{ 
                      textTransform: 'none',
                      borderRadius: '8px',
                      borderColor: darkMode ? 'var(--border-color)' : 'rgba(0,0,0,0.15)',
                      color: darkMode ? 'var(--text-primary)' : 'inherit',
                      '&:hover': {
                        borderColor: darkMode ? 'var(--text-tertiary)' : 'rgba(0,0,0,0.25)',
                        backgroundColor: darkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)'
                      }
                    }}
                  >
                    {exportLoading ? 'Exporting...' : 'Export to Excel'}
                  </Button>

                  <Box sx={{ 
                    display: 'flex', 
                    gap: 1, 
                    bgcolor: darkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)', 
                    borderRadius: '8px',
                    p: 0.5
                  }}>
                    <Button
                      size="small"
                      variant={filterType === 'single' ? 'contained' : 'text'}
                      onClick={() => toggleFilterType('single')}
                      sx={{ 
                        textTransform: 'none',
                        minWidth: 'auto',
                        px: 2,
                        backgroundColor: filterType === 'single' ? (darkMode ? 'var(--accent-color)' : 'royalblue') : 'transparent',
                        color: filterType === 'single' ? 'white' : 'text.secondary',
                        '&:hover': {
                          backgroundColor: filterType === 'single' 
                            ? (darkMode ? 'var(--accent-hover)' : 'rgb(52, 84, 180)')
                            : (darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)')
                        }
                      }}
                    >
                      Single Date
                    </Button>
                    <Button
                      size="small"
                      variant={filterType === 'range' ? 'contained' : 'text'}
                      onClick={() => toggleFilterType('range')}
                      sx={{ 
                        textTransform: 'none',
                        minWidth: 'auto',
                        px: 2,
                        backgroundColor: filterType === 'range' ? (darkMode ? 'var(--accent-color)' : 'royalblue') : 'transparent',
                        color: filterType === 'range' ? 'white' : 'text.secondary',
                        '&:hover': {
                          backgroundColor: filterType === 'range' 
                            ? (darkMode ? 'var(--accent-hover)' : 'rgb(52, 84, 180)')
                            : (darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)')
                        }
                      }}
                    >
                      Date Range
                    </Button>
                  </Box>

                  {filterType === 'single' ? (
                    <LocalizationProvider dateAdapter={AdapterDateFns}>
                      <DatePicker
                        label="Select Date"
                        value={selectedDate}
                        onChange={(newValue) => {
                          if (newValue && !isNaN(newValue.getTime())) {
                            setSelectedDate(newValue);
                          }
                        }}
                        sx={{ width: 200 }}
                        disabled={filterType !== 'single'}
                      />
                    </LocalizationProvider>
                  ) : (
                    <Box sx={{ 
                      display: 'flex', 
                      gap: 2,
                      flexWrap: { xs: 'wrap', md: 'nowrap' },
                      alignItems: 'center'
                    }}>
                      <TextField
                        type="date"
                        size="small"
                        label="Date From"
                        value={attendanceStartDate}
                        onChange={(e) => setAttendanceStartDate(e.target.value)}
                        InputLabelProps={{ shrink: true }}
                        disabled={filterType !== 'range'}
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
                        value={attendanceEndDate}
                        onChange={(e) => setAttendanceEndDate(e.target.value)}
                        InputLabelProps={{ shrink: true }}
                        disabled={filterType !== 'range'}
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
                        onClick={handleAttendanceDateFilterChange}
                        disabled={filterType !== 'range' || !attendanceStartDate || !attendanceEndDate}
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
                        onClick={handleClearAttendanceDateFilter}
                        disabled={filterType !== 'range' || (!attendanceStartDate && !attendanceEndDate)}
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
                  )}
                </Box>
              </Box>

              {/* Today's Attendance Summary */}
              <Box sx={{ mb: 4 }}>
                <Box sx={{
                  display: 'flex',
                  flexDirection: { xs: 'column', md: 'row' },
                  justifyContent: 'space-between',
                  alignItems: { xs: 'flex-start', md: 'center' },
                  gap: 2,
                  mb: 2
                }}>
                  <Typography variant="h6" fontWeight="600" color="#1E293B">
                    Today's Attendance Summary
                  </Typography>
                </Box>
                <Grid container spacing={2} sx={{ mb: 3 }}>
                  <Grid item xs={12} md={6}>
                    <Paper
                      elevation={0}
                      sx={{
                        p: 2,
                        borderRadius: 2,
                        border: darkMode ? '1px solid rgba(99, 102, 241, 0.3)' : '1px solid #E2E8F0',
                        bgcolor: darkMode ? 'rgba(79, 70, 229, 0.12)' : 'rgba(79, 70, 229, 0.08)',
                        display: 'flex',
                        flexDirection: { xs: 'column', sm: 'row' },
                        gap: 2,
                        alignItems: { xs: 'flex-start', sm: 'center' },
                        justifyContent: 'space-between'
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar
                          sx={{
                            width: 40,
                            height: 40,
                            bgcolor: darkMode ? 'var(--accent-color)' : '#4F46E5'
                          }}
                        >
                          <AccessTime sx={{ fontSize: 20 }} />
                        </Avatar>
                        <Box>
                          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                            Late Threshold
                          </Typography>
                          <Typography variant="h6" fontWeight="700" color={darkMode ? 'var(--text-primary)' : '#312E81'}>
                            After {formatTimeDisplay(lateThreshold)}
                          </Typography>
                        </Box>
                      </Box>
                      <Button
                        variant="outlined"
                        size="small"
                        startIcon={<AccessTime />}
                        onClick={() => setShowLateThresholdModal(true)}
                        sx={{
                          textTransform: 'none',
                          borderRadius: '8px',
                          borderColor: darkMode ? 'var(--border-color)' : '#C7D2FE',
                          color: darkMode ? 'var(--text-primary)' : '#4F46E5',
                          '&:hover': {
                            borderColor: darkMode ? 'var(--text-tertiary)' : '#4338CA',
                            backgroundColor: darkMode ? 'rgba(255,255,255,0.08)' : 'rgba(79, 70, 229, 0.08)'
                          }
                        }}
                      >
                        Adjust Threshold
                      </Button>
                    </Paper>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Paper
                      elevation={0}
                      sx={{
                        p: 2,
                        borderRadius: 2,
                        border: darkMode ? '1px solid rgba(14, 165, 233, 0.3)' : '1px solid #BAE6FD',
                        bgcolor: darkMode ? 'rgba(14, 165, 233, 0.12)' : 'rgba(14, 165, 233, 0.1)',
                        display: 'flex',
                        flexDirection: { xs: 'column', sm: 'row' },
                        gap: 2,
                        alignItems: { xs: 'flex-start', sm: 'center' },
                        justifyContent: 'space-between'
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar
                          sx={{
                            width: 40,
                            height: 40,
                            bgcolor: darkMode ? 'rgba(14, 165, 233, 0.6)' : '#0EA5E9'
                          }}
                        >
                          <CalendarToday sx={{ fontSize: 20 }} />
                        </Avatar>
                        <Box>
                          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                            Allowed Time-In Window
                          </Typography>
                          <Typography variant="h6" fontWeight="700" color={darkMode ? 'var(--text-primary)' : '#0F172A'}>
                            {`${formatTimeDisplay(timeWindow.start)} - ${formatTimeDisplay(timeWindow.end)}`}
                          </Typography>
                          {timeWindowError && (
                            <Typography variant="body2" color="error" sx={{ mt: 0.5 }}>
                              {timeWindowError}
                            </Typography>
                          )}
                        </Box>
                      </Box>
                      <Button
                        variant="contained"
                        size="small"
                        startIcon={<CalendarToday sx={{ fontSize: 18 }} />}
                        onClick={() => {
                          setTimeWindowError(null);
                          setShowTimeWindowModal(true);
                        }}
                        sx={{
                          textTransform: 'none',
                          borderRadius: '8px',
                          bgcolor: darkMode ? 'var(--accent-color)' : '#0284C7',
                          boxShadow: 'none',
                          '&:hover': {
                            bgcolor: darkMode ? 'var(--accent-hover)' : '#0369A1',
                            boxShadow: darkMode ? '0 4px 12px rgba(14, 165, 233, 0.25)' : '0 4px 12px rgba(2, 132, 199, 0.3)'
                          }
                        }}
                      >
                        Edit Window
                      </Button>
                    </Paper>
                        
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Paper
                      elevation={0}
                      sx={{
                        p: 2,
                        borderRadius: 2,
                        border: darkMode ? '1px solid rgba(252, 211, 77, 0.25)' : '1px solid #FDE68A',
                        bgcolor: darkMode ? 'rgba(253, 224, 71, 0.12)' : 'rgba(253, 224, 71, 0.12)',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Avatar
                          sx={{
                            width: 40,
                            height: 40,
                            bgcolor: darkMode ? 'rgba(250, 204, 21, 0.4)' : '#F59E0B'
                          }}
                        >
                        
                        </Avatar>
                        <Box>
                          <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 0.5 }}>
                            Faculty Break Window
                          </Typography>
                          <Typography variant="h6" fontWeight="700" color={darkMode ? 'var(--text-primary)' : '#92400E'}>
                          12:00 PM - 1:00 PM
                          </Typography>
                         
                        </Box>
                      </Box>                           
                    </Paper>
                  </Grid>

                </Grid>
                <Box sx={{ 
                  display: 'flex', 
                  gap: 2,
                  flexDirection: { xs: 'column', md: 'row' },
                  mb: 4
                }}>
                  {/* Present Card */}
                  <Paper
                    elevation={0}
                    sx={{
                      flex: 1,
                      p: 2,
                      borderRadius: 2,
                      bgcolor: '#F0FDF4',
                      border: '1px solid #BBF7D0'
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                      <Typography variant="subtitle2" color="#15803D">On Time</Typography>
                      <Tooltip title="Faculty who timed in before the late threshold" arrow>
                        <InfoOutlined sx={{ color: '#15803D', fontSize: 16 }} />
                      </Tooltip>
                    </Box>
                    <Typography variant="h4" color="#166534" fontWeight="bold">
                      {attendanceStats.present}
                    </Typography>
                  </Paper>

                  {/* Late Card */}
                  <Paper
                    elevation={0}
                    sx={{
                      flex: 1,
                      p: 2,
                      borderRadius: 2,
                      bgcolor: '#FEF3C7',
                      border: '1px solid #FDE68A',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      '&:hover': {
                        transform: 'translateY(-2px)',
                        boxShadow: '0 4px 12px rgba(234, 179, 8, 0.2)'
                      }
                    }}
                    onClick={getLateAttendanceDetails}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                      <Typography variant="subtitle2" color="#B45309">Late</Typography>
                      <Tooltip title={`Click to view late faculty members`} arrow>
                        <InfoOutlined sx={{ color: '#B45309', fontSize: 16 }} />
                      </Tooltip>
                    </Box>
                    <Typography variant="h4" color="#92400E" fontWeight="bold">
                      {attendanceStats.late}
                    </Typography>
                  </Paper>

                  {/* No Time-in Card */}
                  <Paper
                    elevation={0}
                    sx={{
                      flex: 1,
                      p: 2,
                      borderRadius: 2,
                      bgcolor: '#FEE2E2',
                      border: '1px solid #FECACA',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      '&:hover': {
                        transform: 'translateY(-2px)',
                        boxShadow: '0 4px 12px rgba(239, 68, 68, 0.2)'
                      }
                    }}
                    onClick={getNoTimeInDetails}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                      <Typography variant="subtitle2" color="#B91C1C">No Time-in</Typography>
                      <Tooltip title="Click to view faculty without time-in" arrow>
                        <InfoOutlined sx={{ color: '#B91C1C', fontSize: 16 }} />
                      </Tooltip>
                    </Box>
                    <Typography variant="h4" color="#991B1B" fontWeight="bold">
                      {attendanceStats.absent}
                    </Typography>
                  </Paper>
                </Box>
              </Box>

              {/* Time-in/out Table */}
              {loadingFacultyLogs ? (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Photo</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell>Email</TableCell>
                        <TableCell>Date</TableCell>
                        <TableCell>Time In</TableCell>
                        <TableCell>Time Out</TableCell>
                        <TableCell>Duration</TableCell>
                      {/*  <TableCell>Venue</TableCell>
                        <TableCell>Status</TableCell>*/}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      <TableRowsSkeleton />
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : error && error.includes("faculty logs") ? (
                <Box sx={{ p: 3, textAlign: 'center', color: 'error.main' }}>
                  <Typography>{error}</Typography>
                </Box>
              ) : facultyLogs.length === 0 ? (
                <Box sx={{ p: 3, textAlign: 'center', color: 'text.secondary' }}>
                  <Typography>No attendance records found for this date</Typography>
                </Box>
              ) : (
                <TableContainer>
                  <Table>
                    <TableHead sx={{ bgcolor: darkMode ? 'var(--table-header-bg)' : 'rgba(0,0,0,0.02)' }}>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Photo</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Email</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Date</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Time In</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Time Out</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Duration</TableCell>

                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {[...facultyLogs]
                        .sort((a, b) => {
                          const dateA = a.timeIn ? new Date(a.timeIn.timestamp) : new Date(0);
                          const dateB = b.timeIn ? new Date(b.timeIn.timestamp) : new Date(0);
                          return dateA - dateB; // Ascending order (oldest to newest)
                        })
                        .map((entry) => {
                          const duration = calculateDuration(entry.timeIn, entry.timeOut);
                          const timeInDate = entry.timeIn ? new Date(entry.timeIn.timestamp) : null;
                          const formattedDate = timeInDate ? format(timeInDate, 'MMMM d, yyyy') : 'N/A';
                        
                        return (
                          <TableRow key={entry.id} sx={{ '&:hover': { bgcolor: darkMode ? 'var(--accent-light)' : 'action.hover' } }}>
                            <TableCell>
                              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                                {/* Time In Photo */}
                                {entry.timeIn?.imageUrl ? (
                                  <Box
                                    component="img"
                                    src={entry.timeIn.imageUrl}
                                    alt={`${entry.firstName}'s time-in photo`}
                                    sx={{
                                      width: 40,
                                      height: 40,
                                      borderRadius: '50%',
                                      objectFit: 'cover',
                                      cursor: 'pointer',
                                      border: '2px solid #4caf50',
                                      '&:hover': {
                                        opacity: 0.8,
                                        transform: 'scale(1.1)',
                                        transition: 'all 0.2s ease-in-out'
                                      }
                                    }}
                                    onClick={() => setZoomImage(entry.timeIn.imageUrl)}
                                  />
                                ) : (
                                  <Box
                                    sx={{
                                      width: 40,
                                      height: 40,
                                      borderRadius: '50%',
                                      bgcolor: 'grey.300',
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'center',
                                      border: '2px solid #4caf50'
                                    }}
                                  >
                                    <Typography variant="body2" color="text.secondary">
                                      IN
                                    </Typography>
                                  </Box>
                                )}
                                
                                {/* Time Out Photo */}
                                {entry.timeOut?.imageUrl ? (
                                  <Box
                                    component="img"
                                    src={entry.timeOut.imageUrl}
                                    alt={`${entry.firstName}'s time-out photo`}
                                    sx={{
                                      width: 40,
                                      height: 40,
                                      borderRadius: '50%',
                                      objectFit: 'cover',
                                      cursor: 'pointer',
                                      border: '2px solid #f44336',
                                      '&:hover': {
                                        opacity: 0.8,
                                        transform: 'scale(1.1)',
                                        transition: 'all 0.2s ease-in-out'
                                      }
                                    }}
                                    onClick={() => setZoomImage(entry.timeOut.imageUrl)}
                                  />
                                ) : (
                                  entry.timeOut && (
                                    <Box
                                      sx={{
                                        width: 40,
                                        height: 40,
                                        borderRadius: '50%',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        border: '2px solid #f44336'
                                      }}
                                    >
                                      <Typography variant="body2" color="text.secondary">
                                        OUT
                                      </Typography>
                                    </Box>
                                  )
                                )}
                              </Box>
                            </TableCell>
                            <TableCell>{entry.firstName}</TableCell>
                            <TableCell>{entry.email}</TableCell>
                            <TableCell>
                              <Chip 
                                label={formattedDate}
                                size="small"
                                color="default"
                                variant="outlined"
                                icon={<CalendarToday sx={{ fontSize: 16 }} />}
                              />
                            </TableCell>
                            <TableCell>
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Tooltip title="Time In" arrow>
                                  <Chip 
                                    label={entry.timeIn.time}
                                    color="success"
                                    size="small"
                                    variant="outlined"
                                    icon={<AccessTime sx={{ fontSize: 16 }} />}
                                  />
                                </Tooltip>
                              </Box>
                            </TableCell>
                            <TableCell>
                              <Tooltip title={entry.timeOut ? "Time Out" : "Not yet timed out"} arrow>
                                <Chip 
                                  label={entry.timeOut ? entry.timeOut.time : 'Active Session'}
                                  color={entry.timeOut ? "error" : "warning"}
                                  size="small"
                                  variant="outlined"
                                  icon={<AccessTime sx={{ fontSize: 16 }} />}
                                />
                              </Tooltip>
                            </TableCell>
                            <TableCell>
                              {duration ? (
                                <Tooltip title="Total Duration" arrow>
                                  <Chip 
                                    label={duration}
                                    color="primary"
                                    size="small"
                                    variant="outlined"
                                    icon={<AccessTime sx={{ fontSize: 16 }} />}
                                  />
                                </Tooltip>
                              ) : (
                                <Typography variant="body2" color="text.secondary">
                                  In Progress
                                </Typography>
                              )}
                            </TableCell>
                           {/*<TableCell>{entry.venue || 'N/A'}</TableCell>
                            <TableCell>
                              <Chip 
                                label={entry.attendanceBadge} 
                                size="small" 
                                className={`status-chip ${getStatusClass(entry.attendanceBadge)}`}
                                variant="outlined"
                              />
                            </TableCell>*/}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
              
              {/* Image Zoom Modal */}
              <ImageZoomModal 
                imageUrl={zoomImage}
                onClose={() => setZoomImage(null)}
              />
            </Box>
          </>
        )}
        
        {/* Event Summary */}
        {mainTab === 1 && (
          <>
            <EventAnalytics
              data={getEventAnalyticsData(events)}
              title="Event Status Distribution"
            />
            {/* Existing Event Summary UI */}
            <Box sx={{ 
              padding:'10px',
              bgcolor: darkMode ? 'var(--card-bg)' : 'white', 
              borderRadius: '8px', 
              boxShadow: '0 4px 20px rgba(0,0,0,0.05)', 
              overflow: 'hidden',
              border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)'
            }}>
              {/* ...rest of event summary UI... */}
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
                            {events.filter(event => event.status === 'Ongoing').length}
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
                borderRadius: '8px', 
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
                          <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Venue</TableCell>
                          <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Status</TableCell>
                          <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Actions</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <TableRowsSkeleton />
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : eventError ? (
                  <Box sx={{ p: 3, textAlign: 'center', color: 'error.main' }}>
                    <Typography>{eventError}</Typography>
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
                          <TableCell sx={{ fontWeight: 600, color: 'text.secondary', fontSize: '0.875rem' }}>Venue</TableCell>
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
                            <TableCell>{event.venue || 'N/A'}</TableCell>
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
            </Box>
          </>
        )}

        {/* Calendar Tab */}
        {mainTab === 2 && (
          <Box sx={{ 
            bgcolor: darkMode ? 'var(--card-bg)' : 'white', 
            borderRadius: '8px', 
            boxShadow: '0 4px 20px rgba(0,0,0,0.05)', 
            overflow: 'hidden',
            border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)',
            color: darkMode ? 'white' : 'white',
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

        {/* Email Status Tab */}
        {mainTab === 3 && (
          <Box sx={{ 
            bgcolor: darkMode ? 'var(--card-bg)' : 'white', 
            borderRadius: '8px', 
            boxShadow: '0 4px 20px rgba(0,0,0,0.05)', 
            overflow: 'hidden',
            border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)',
            p: 3
          }}>
            <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 3 }}>Certificate Email Status</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Monitor the delivery status of certificate emails sent via Firebase Extensions
            </Typography>
            <EmailStatusTracker />
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
                 <Typography variant="body2" color="text.secondary">Venue</Typography>
                  <Typography variant="body1" fontWeight={500}>{selectedEvent.venue || 'N/A'}</Typography>
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

      {/* Add the Time Window & Late Threshold Modals */}
      <TimeWindowModal />
      <LateThresholdModal />

      {/* Add the Late Faculty Modal */}
      <LateFacultyModal />

      {/* Add the No Time-in Faculty Modal */}
      <NoTimeInModal />
    </Box>
  );
}