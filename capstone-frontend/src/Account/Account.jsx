// Add imports for the new event attendance feature
import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Typography, Button, IconButton, InputBase, Paper, TextField, Menu, MenuItem, ListItemIcon, ListItemText, 
  Avatar, Badge, Modal, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Snackbar, Alert, 
  CircularProgress, Select, FormControl, Chip, Divider, List, ListItem, Skeleton,
  Tabs, Tab, Zoom, Card, Grid, Collapse, Accordion, AccordionSummary, AccordionDetails
} from '@mui/material';
import {
  Search, AccountTree, Settings, Notifications, FilterList, Home, Event, People, CalendarToday,
  Group, Add, Close, Logout, Edit, Delete, VisibilityOutlined, EventAvailable, CheckCircleOutline, Email,
  Person, AccessTime, RemoveFromQueue, DirectionsWalk, MoreVert, PhotoCamera, ArrowUpward, ArrowDownward,
  Refresh, PeopleAlt, DateRange, FileDownload, ExpandMore, History, ChevronLeft, ChevronRight
} from '@mui/icons-material';
import axios from 'axios';
import NotificationSystem from '../components/NotificationSystem';
import { storage, database } from '../firebase/firebase';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { ref as dbRef, get, query, orderByChild, limitToLast, onValue } from 'firebase/database';
import * as XLSX from 'xlsx';

// Base API URL
const API_BASE_URL = 'http://localhost:8080/api';

// TabPanel component for tab content
function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box>
          {children}
        </Box>
      )}
    </div>
  );
}

// Date formatting helper function
const formatDate = (dateObj) => {
  if (!dateObj) return 'N/A';
  
  // Handle Firestore timestamp format
  if (dateObj.seconds) {
    const date = new Date(dateObj.seconds * 1000);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }
  
  // Handle ISO string date
  try {
    const date = new Date(dateObj);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch (e) {
    return 'Invalid date';
  }
};

// Time formatting helper function
const formatTime = (timeString) => {
  if (!timeString) return 'N/A';
  
  try {
    const date = new Date(timeString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch (e) {
    return 'Invalid time';
  }
};

// Account table loading placeholder
const AccountTableSkeleton = () => (
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

export default function AccountPage() {
  const navigate = useNavigate();
  const location = useLocation();

  // Tab state
  const [tabValue, setTabValue] = useState(0);

  // Status counters state
  const [statusCounts, setStatusCounts] = useState({
    onDuty: 0,
    onBreak: 0,
    offDuty: 0,
    total: 0
  });

  // Faculty timeline data
  const [todayTimeline, setTodayTimeline] = useState([]);
  const [timelineLoading, setTimelineLoading] = useState(false);

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
    role: "USER",
    profilePictureUrl: ""
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
    role: "USER",
    password:"",
    profilePictureUrl: ""
  });

  // View professor modal state
  const [showViewModal, setShowViewModal] = useState(false);
  const [viewingProfessor, setViewingProfessor] = useState(null);

  // Attended events modal state
  const [showAttendedEventsModal, setShowAttendedEventsModal] = useState(false);
  const [attendedEvents, setAttendedEvents] = useState([]);
  const [loadingEvents, setLoadingEvents] = useState(false);

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

  // Add sorting and department filter states
  const [sortConfig, setSortConfig] = useState({
    key: null,
    direction: 'asc'
  });
  const [selectedDepartment, setSelectedDepartment] = useState('');

  // Add new state for profile picture zoom modal
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [selectedProfile, setSelectedProfile] = useState(null);

  // Add temporary profile picture states for previews
  const [newProfilePicture, setNewProfilePicture] = useState(null);
  const [editProfilePicture, setEditProfilePicture] = useState(null);

  // Loading states for real-time data
  const [statusLoading, setStatusLoading] = useState(true);

  // Attendance history state
  const [attendanceHistory, setAttendanceHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [expandedDays, setExpandedDays] = useState(new Set());

  // Add new state for selected date
  const [selectedDate, setSelectedDate] = useState(new Date());

  // Add function to handle date changes
  const handleDateChange = (direction) => {
    const newDate = new Date(selectedDate);
    if (direction === 'prev') {
      newDate.setDate(newDate.getDate() - 1);
    } else {
      newDate.setDate(newDate.getDate() + 1);
    }
    setSelectedDate(newDate);
    fetchTimelineForDate(newDate);
  };

  // Fetch professors and departments on component mount
  useEffect(() => {
    fetchProfessors();
    fetchDepartments();
  }, []);

  // Update status counts when professors data changes (to ensure count is always current)
  useEffect(() => {
    if (professors.length > 0 && tabValue === 1) {
      fetchStatusCounts();
      fetchTimelineForDate(selectedDate);
      fetchAttendanceHistory();
    }
  }, [professors.length, tabValue]);

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

  // Fetch user's attended events
  const fetchUserAttendedEvents = async (userId) => {
    setLoadingEvents(true);
    try {
      const response = await axios.get(`${API_BASE_URL}/attendance/user/${userId}/attended-events`);
      setAttendedEvents(response.data);
    } catch (err) {
      console.error('Error fetching attended events:', err);
      if (err.response?.status === 404) {
        setAttendedEvents([]);
      } else {
        showSnackbar('Failed to load attended events', 'error');
      }
    } finally {
      setLoadingEvents(false);
    }
  };

  // View attended events handler
  const handleViewAttendedEvents = (professor) => {
    setViewingProfessor(professor);
    fetchUserAttendedEvents(professor.userId);
    setShowAttendedEventsModal(true);
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
    return department ? department.abbreviation : 'Unknown';
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
      // First register the user to get their Firebase Auth UID
      const response = await axios.post(`${API_BASE_URL}/auth/register`, newProfessor, {
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (response.data && response.data.userId) {
        // Now upload the profile picture if one is selected
        let profilePictureUrl = '';
        if (newProfilePicture) {
          const storageRef = ref(storage, `profilePictures/${response.data.userId}/profile.${newProfilePicture.name.split('.').pop()}`);
          await uploadBytes(storageRef, newProfilePicture);
          profilePictureUrl = await getDownloadURL(storageRef);

          // Update the user's profile picture URL
          await axios.put(`${API_BASE_URL}/user/updateProfilePicture/${response.data.userId}`, 
            { profilePictureUrl },
            {
              headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
              }
            }
          );
        }
        
        showSnackbar('Faculty added successfully', 'success');
        setNewProfessor({
          firstName: "",
          lastName: "",
          email: "",
          departmentId: "",
          schoolId: "",
          password: "",
          role: "USER",
          profilePictureUrl: ""
        });
        setNewProfilePicture(null);
        setShowAddModal(false);
        fetchProfessors(); // Refresh the list
      }
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
      departmentId: professor.department && professor.department.departmentId ? professor.department.departmentId : "",
      schoolId: professor.schoolId,
      role: professor.role || "USER",
      password: professor.password || "",
      profilePictureUrl: professor.profilePictureUrl || ""
    });
    setShowEditModal(true);
  };

  const handleViewClick = (professor) => {
    setViewingProfessor(professor);
    setShowViewModal(true);
  };

  const handleUpdateProfessor = async () => {
    try {
      // First upload the new profile picture if one is selected
      let profilePictureUrl = editingProfessor.profilePictureUrl;
      if (editProfilePicture) {
        const storageRef = ref(storage, `profilePictures/${editingProfessor.userId}/profile.${editProfilePicture.name.split('.').pop()}`);
        await uploadBytes(storageRef, editProfilePicture);
        profilePictureUrl = await getDownloadURL(storageRef);
      }

      // Remove password from the payload if it's empty or null
      const { password, ...professorWithoutPassword } = editingProfessor;
      
      const updatePayload = {
        ...professorWithoutPassword,
        department: {
          departmentId: professorWithoutPassword.departmentId
        },
        profilePictureUrl
      };
      
      await axios.put(`${API_BASE_URL}/user/updateUser/${editingProfessor.userId}`, updatePayload, {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      showSnackbar('Faculty updated successfully', 'success');
      setEditProfilePicture(null);
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
    navigate('/profile');
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
    const departmentId = professor.department?.departmentId;
    const departmentName = departmentId ? getDepartmentName(departmentId).toLowerCase() : '';
    const departmentAbbr = departmentId ? getDepartmentAbbreviation(departmentId).toLowerCase() : '';
    
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
        return matchesSearch && professor.department?.departmentId;
      case 'ID':
        return matchesSearch && professor.schoolId;
      case 'Name':
        return matchesSearch && (professor.firstName || professor.lastName);
      default:
        return matchesSearch;
    }
  }).filter(professor => {
    // Apply department filter
    if (selectedDepartment) {
      return professor.department?.departmentId === selectedDepartment;
    }
    return true;
  });

  // Sort the filtered professors
  const sortedProfessors = [...filteredProfessors].sort((a, b) => {
    if (!sortConfig.key) return 0;

    let aValue, bValue;
    if (sortConfig.key === 'name') {
      aValue = `${a.firstName || ''} ${a.lastName || ''}`.toLowerCase();
      bValue = `${b.firstName || ''} ${b.lastName || ''}`.toLowerCase();
    } else if (sortConfig.key === 'email') {
      aValue = (a.email || '').toLowerCase();
      bValue = (b.email || '').toLowerCase();
    }

    if (aValue < bValue) return sortConfig.direction === 'asc' ? -1 : 1;
    if (aValue > bValue) return sortConfig.direction === 'asc' ? 1 : -1;
    return 0;
  });

  // Handle column sort
  const handleSort = (key) => {
    setSortConfig(prevConfig => {
      if (prevConfig.key === key) {
        // Toggle direction if same key
        return { key, direction: prevConfig.direction === 'asc' ? 'desc' : 'asc' };
      } else {
        // Reset other column and set new key to asc
        return { key, direction: 'asc' };
      }
    });
  };

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
    if (newValue === 1) {
      // When switching to Current Status tab, ensure professors data is loaded first
      if (professors.length === 0) {
        // If professors data is not loaded yet, fetch it first
        fetchProfessors().then(() => {
          fetchStatusCounts();
          fetchTimelineForDate(selectedDate);
          fetchAttendanceHistory();
        });
      } else {
        // If professors data is already loaded, proceed with status data
        fetchStatusCounts();
        fetchTimelineForDate(selectedDate);
        fetchAttendanceHistory();
      }
    }
  };

  // Fetch faculty status counts
  const fetchStatusCounts = async () => {
    setStatusLoading(true);
    try {
      // Use the same professors data that's already loaded
      const totalFaculty = professors.length;
      
      // Get status from Firebase Realtime Database
      const timeLogs = dbRef(database, 'timeLogs');
      const statusSnapshot = await get(timeLogs);
      
      let onDutyCount = 0;
      let onBreakCount = 0;
      let offDutyCount = 0;
      
      // Process data to count statuses
      if (statusSnapshot.exists()) {
        const latestStatusByUser = new Map();
        
        // For each user node in timeLogs
        statusSnapshot.forEach(userSnap => {
          const userId = userSnap.key;
          const userLogs = userSnap.val();
          
          // Skip if this is just the userId field
          if (typeof userLogs === 'string') return;
          
          // Get all entries for this user
          const entries = Object.entries(userLogs)
            .filter(([_, entry]) => typeof entry === 'object' && entry.timestamp)
            .map(([logId, entry]) => ({
              ...entry,
              logId,
              timestamp: typeof entry.timestamp === 'string' ? parseInt(entry.timestamp) : entry.timestamp
            }));
          
          // Sort entries by timestamp (newest first)
          entries.sort((a, b) => b.timestamp - a.timestamp);
          
          // Get the latest entry
          if (entries.length > 0) {
            const latestEntry = entries[0];
            latestStatusByUser.set(userId, latestEntry);
            
            console.log(`Latest entry for user ${userId}:`, {
              name: latestEntry.firstName,
              email: latestEntry.email,
              status: latestEntry.status,
              type: latestEntry.type,
              timestamp: new Date(latestEntry.timestamp).toLocaleString()
            });
          }
        });
        
        // Count the latest status for each user
        latestStatusByUser.forEach((latestEntry, userId) => {
          // Explicitly check status field first
          if (latestEntry.status === 'On Break') {
            onBreakCount++;
            console.log(`Counting as On Break:`, {
              userId,
              name: latestEntry.firstName,
              email: latestEntry.email,
              status: latestEntry.status,
              timestamp: new Date(latestEntry.timestamp).toLocaleString()
            });
          } else if (latestEntry.status === 'On Duty') {
            onDutyCount++;
            console.log(`Counting as On Duty:`, {
              userId,
              name: latestEntry.firstName,
              email: latestEntry.email,
              status: latestEntry.status,
              timestamp: new Date(latestEntry.timestamp).toLocaleString()
            });
          } else if (latestEntry.status === 'Off Duty' || latestEntry.type === 'TimeOut') {
            offDutyCount++;
            console.log(`Counting as Off Duty:`, {
              userId,
              name: latestEntry.firstName,
              email: latestEntry.email,
              status: latestEntry.status,
              type: latestEntry.type,
              timestamp: new Date(latestEntry.timestamp).toLocaleString()
            });
          } else if (latestEntry.type === 'TimeIn') {
            // If no explicit status but TimeIn, count as On Duty
            onDutyCount++;
            console.log(`Counting as On Duty (TimeIn):`, {
              userId,
              name: latestEntry.firstName,
              email: latestEntry.email,
              type: latestEntry.type,
              timestamp: new Date(latestEntry.timestamp).toLocaleString()
            });
          } else {
            // Default case
            offDutyCount++;
            console.log(`Counting as Off Duty (default):`, {
              userId,
              name: latestEntry.firstName,
              email: latestEntry.email,
              status: latestEntry.status,
              type: latestEntry.type,
              timestamp: new Date(latestEntry.timestamp).toLocaleString()
            });
          }
        });

        // Log final detailed counts
        console.log('Final Status Counts:', {
          onBreak: {
            count: onBreakCount,
            users: Array.from(latestStatusByUser.entries())
              .filter(([_, entry]) => entry.status === 'On Break')
              .map(([userId, entry]) => ({
                userId,
                name: entry.firstName,
                email: entry.email,
                timestamp: new Date(entry.timestamp).toLocaleString()
              }))
          },
          onDuty: {
            count: onDutyCount,
            users: Array.from(latestStatusByUser.entries())
              .filter(([_, entry]) => 
                entry.status === 'On Duty' || 
                (entry.type === 'TimeIn' && entry.status !== 'On Break' && entry.status !== 'Off Duty'))
              .map(([userId, entry]) => ({
                userId,
                name: entry.firstName,
                email: entry.email,
                timestamp: new Date(entry.timestamp).toLocaleString()
              }))
          },
          offDuty: {
            count: offDutyCount,
            users: Array.from(latestStatusByUser.entries())
              .filter(([_, entry]) => 
                entry.status === 'Off Duty' || 
                (entry.type === 'TimeOut' && entry.status !== 'On Break'))
              .map(([userId, entry]) => ({
                userId,
                name: entry.firstName,
                email: entry.email,
                timestamp: new Date(entry.timestamp).toLocaleString()
              }))
          }
        });
      }

      // Update state with counts
      setStatusCounts({
        onDuty: onDutyCount,
        onBreak: onBreakCount,
        offDuty: offDutyCount,
        total: totalFaculty
      });

    } catch (err) {
      console.error('Error fetching status counts:', err);
      showSnackbar('Failed to load status counts', 'error');
    } finally {
      setStatusLoading(false);
    }
  };

  // Export faculty attendance to Excel
  const handleExportToExcel = async () => {
    try {
      setTimelineLoading(true);
      
      // Get current date for filename
      const today = new Date();
      const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"];
      const month = monthNames[today.getMonth()];
      const day = today.getDate();
      const year = today.getFullYear();
      const filename = `Faculty Attendance for ${month} ${day}, ${year}.xlsx`;

      // Use the same professors data that's already loaded
      const exportFacultyUsers = professors;

      // Get timeline data from Firebase
      const timeLogs = dbRef(database, 'timeLogs');
      const timelineSnapshot = await get(timeLogs);
      
      const exportData = [];
      
      // Current date in milliseconds (start and end of day)
      const todayStart = new Date();
      todayStart.setHours(0, 0, 0, 0);
      const todayTimestamp = todayStart.getTime();
      
      const todayEnd = new Date();
      todayEnd.setHours(23, 59, 59, 999);
      const todayEndTimestamp = todayEnd.getTime();
      
      // Create a map to store all activities by user for today only
      const activitiesByUser = new Map();
      
      // Process Firebase data to extract today's activities
      if (timelineSnapshot.exists()) {
        timelineSnapshot.forEach(userSnap => {
          const userId = userSnap.key;
          const userData = userSnap.val();
          
          Object.entries(userData).forEach(([entryId, entry]) => {
            if (entry.timestamp && entry.userId) {
              const entryTimestamp = typeof entry.timestamp === 'string' 
                ? parseInt(entry.timestamp) 
                : entry.timestamp;
              
              // Only include entries from today
              if (entryTimestamp >= todayTimestamp && entryTimestamp <= todayEndTimestamp) {
                const activity = {
                  userId: entry.userId,
                  firstName: entry.firstName || 'Unknown',
                  email: entry.email || 'N/A',
                  time: new Date(entryTimestamp).toLocaleTimeString('en-US', {
                    hour: '2-digit', minute: '2-digit', second: '2-digit'
                  }),
                  date: new Date(entryTimestamp).toLocaleDateString('en-US'),
                  activity: entry.type === 'TimeIn' 
                    ? (entry.status === 'On Break' ? 'Break' : 'Time In')
                    : 'Time Out',
                  status: entry.status || 'N/A',
                  timestamp: entryTimestamp
                };
                
                if (!activitiesByUser.has(entry.userId)) {
                  activitiesByUser.set(entry.userId, []);
                }
                activitiesByUser.get(entry.userId).push(activity);
              }
            }
          });
        });
      }
      
      // Process each faculty member
      exportFacultyUsers.forEach(faculty => {
        const facultyActivities = activitiesByUser.get(faculty.userId) || [];
        
        // Sort activities by timestamp
        facultyActivities.sort((a, b) => a.timestamp - b.timestamp);
        
        if (facultyActivities.length > 0) {
          facultyActivities.forEach((activity, index) => {
            exportData.push({
              'Faculty Name': `${faculty.firstName} ${faculty.lastName}`,
              'Email': faculty.email,
              'Date': activity.date,
              'Time': activity.time,
              'Activity': activity.activity,
              'Status': activity.status,
              'Department': faculty.department?.name || 'N/A'
            });
          });
        } else {
          // Include faculty with no activity today
          exportData.push({
            'Faculty Name': `${faculty.firstName} ${faculty.lastName}`,
            'Email': faculty.email,
            'Date': today.toLocaleDateString('en-US'),
            'Time': 'No activity today',
            'Activity': 'No activity today',
            'Status': 'No activity today',
            'Department': faculty.department?.name || 'N/A'
          });
        }
      });

      // If no data found, add summary info
      if (exportData.length === 0) {
        exportData.push({
          'Faculty Name': 'No Data Available',
          'Email': 'N/A',
          'Date': today.toLocaleDateString('en-US'),
          'Time': 'N/A',
          'Activity': 'No activities recorded today',
          'Status': 'N/A',
          'Department': 'N/A'
        });
      }

      // Create Excel workbook
      const worksheet = XLSX.utils.json_to_sheet(exportData);
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Faculty Attendance');

      // Auto-size columns
      const colWidths = [];
      const header = Object.keys(exportData[0] || {});
      header.forEach((key, i) => {
        const maxLength = Math.max(
          key.length,
          ...exportData.map(row => (row[key] || '').toString().length)
        );
        colWidths[i] = { width: Math.min(maxLength + 2, 50) };
      });
      worksheet['!cols'] = colWidths;

      // Save file
      XLSX.writeFile(workbook, filename);
      
      showSnackbar(`Exported successfully: ${filename}`, 'success');
    } catch (err) {
      console.error('Error exporting to Excel:', err);
      showSnackbar('Failed to export data', 'error');
    } finally {
      setTimelineLoading(false);
    }
  };

  // Modify fetchTodayTimeline to accept a date parameter
  const fetchTimelineForDate = async (date) => {
    setTimelineLoading(true);
    try {
      // Get timeline data from Firebase
      const timeLogs = dbRef(database, 'timeLogs');
      const timelineSnapshot = await get(timeLogs);
      
      const timelineData = [];
      
      // Get start and end of the selected date
      const startOfDay = new Date(date);
      startOfDay.setHours(0, 0, 0, 0);
      const startTimestamp = startOfDay.getTime();
      
      const endOfDay = new Date(date);
      endOfDay.setHours(23, 59, 59, 999);
      const endTimestamp = endOfDay.getTime();
      
      if (timelineSnapshot.exists()) {
        timelineSnapshot.forEach(userSnap => {
          const userId = userSnap.key;
          const userLogs = userSnap.val();
          
          if (typeof userLogs === 'string') return;
          
          Object.entries(userLogs).forEach(([logId, entry]) => {
            if (!entry || !entry.timestamp) return;
            
            const entryTimestamp = typeof entry.timestamp === 'string' 
              ? parseInt(entry.timestamp) 
              : entry.timestamp;
            
            if (entryTimestamp >= startTimestamp && entryTimestamp <= endTimestamp) {
              let status = entry.status || '';
              let description = '';
              
              if (entry.type === 'TimeIn') {
                if (status === 'On Break') {
                  description = 'Started break';
                } else if (status === 'On Duty') {
                  description = 'Started shift';
                } else {
                  description = 'Timed in';
                }
              } else if (entry.type === 'TimeOut') {
                if (status === 'On Break') {
                  description = 'Started break';
                } else if (status === 'Off Duty') {
                  description = 'Ended shift';
                } else {
                  description = 'Timed out';
                }
              }
              
              const timelineEntry = {
                id: `${userId}_${logId}`,
                name: entry.firstName || 'Unknown User',
                status: status,
                description: description,
                time: new Date(entryTimestamp).toLocaleTimeString('en-US', {
                  hour: '2-digit', 
                  minute: '2-digit'
                }),
                timestamp: entryTimestamp,
                userId: entry.userId || userId,
                type: entry.type,
                email: entry.email
              };
              
              timelineData.push(timelineEntry);
            }
          });
        });
      }
      
      timelineData.sort((a, b) => a.timestamp - b.timestamp);
      setTodayTimeline(timelineData);
    } catch (err) {
      console.error('Error fetching timeline data:', err);
      showSnackbar('Failed to load timeline data', 'error');
    } finally {
      setTimelineLoading(false);
    }
  };

  // Fetch attendance history
  const fetchAttendanceHistory = async () => {
    setHistoryLoading(true);
    try {
      // Get timeline data from Firebase
      const timeLogs = dbRef(database, 'timeLogs');
      const timelineSnapshot = await get(timeLogs);
      
      const historyByDate = new Map();
      
      if (timelineSnapshot.exists()) {
        // Process timeline data from all users
        timelineSnapshot.forEach(userSnap => {
          const userId = userSnap.key;
          const userLogs = userSnap.val();
          
          // Skip if this is just the userId field
          if (typeof userLogs === 'string') return;
          
          // Process each time log entry for this user
          Object.entries(userLogs).forEach(([logId, entry]) => {
            if (!entry || !entry.timestamp) return;
            
            // Convert timestamp to number if it's a string
            const historyEntryTimestamp = typeof entry.timestamp === 'string' 
              ? parseInt(entry.timestamp) 
              : entry.timestamp;
            
            // Get date string for grouping (YYYY-MM-DD)
            const historyEntryDate = new Date(historyEntryTimestamp);
            const dateKey = historyEntryDate.toISOString().split('T')[0];
            const displayDate = historyEntryDate.toLocaleDateString('en-US', {
              weekday: 'long',
              year: 'numeric',
              month: 'long',
              day: 'numeric'
            });
            
            let status = entry.status || '';
            let description = '';
            
            if (entry.type === 'TimeIn') {
              description = status === 'On Duty' ? 'Started shift' : 'Timed in';
            } else if (entry.type === 'TimeOut') {
              if (status === 'On Break') {
                description = 'Started break';
              } else if (status === 'Off Duty') {
                description = 'Ended shift';
              } else {
                description = 'Timed out';
              }
            }
            
            const timelineEntry = {
              id: `${userId}_${logId}`,
              name: entry.firstName || 'Unknown User',
              status: status,
              description: description,
              time: new Date(historyEntryTimestamp).toLocaleTimeString('en-US', {
                hour: '2-digit',
                minute: '2-digit'
              }),
              timestamp: historyEntryTimestamp,
              userId: entry.userId || userId,
              type: entry.type,
              email: entry.email
            };
            
            if (!historyByDate.has(dateKey)) {
              historyByDate.set(dateKey, {
                dateKey,
                displayDate,
                entries: []
              });
            }
            
            historyByDate.get(dateKey).entries.push(timelineEntry);
          });
        });
      }
      
      // Convert map to array and sort by date (newest first)
      const historyArray = Array.from(historyByDate.values())
        .map(dayData => {
          // Sort entries within each day by timestamp (ascending)
          dayData.entries.sort((a, b) => a.timestamp - b.timestamp);
          return dayData;
        })
        .sort((a, b) => new Date(b.dateKey) - new Date(a.dateKey));
      
      setAttendanceHistory(historyArray);
    } catch (err) {
      console.error('Error fetching attendance history:', err);
      showSnackbar('Failed to load attendance history', 'error');
    } finally {
      setHistoryLoading(false);
    }
  };

  // Fetch faculty status reports
  const fetchStatusReports = async () => {
    setReportsLoading(true);
    try {
      // This would be replaced with actual API calls in a real implementation
      // Mock data for demonstration
      setStatusReports([
        { id: 1, facultyName: "John Wayne Largo", date: "Jan 15, 2025", status: "On Duty", duration: "8h 30m", notes: "Regular shift" },
        { id: 2, facultyName: "Alexa Tumungha", date: "Jan 14, 2025", status: "Break", duration: "1h 00m", notes: "Lunch break" },
        { id: 3, facultyName: "Mikhail Navarro", date: "Jan 14, 2025", status: "On Duty", duration: "4h 15m", notes: "Afternoon shift" },
        { id: 4, facultyName: "Danisse Cabana", date: "Jan 13, 2025", status: "Off Duty", duration: "0h 00m", notes: "Sick leave" }
      ]);
    } catch (err) {
      console.error('Error fetching status reports:', err);
      showSnackbar('Failed to load status reports', 'error');
    } finally {
      setReportsLoading(false);
    }
  };

  // Export status report function
  const handleExportReport = () => {
    // In a real implementation, this would generate and download a report
    showSnackbar('Report exported successfully', 'success');
  };

  // Add profile picture upload handler
  const handleProfilePictureUpload = async (userId, file) => {
    try {
      const storageRef = ref(storage, `profilePictures/${userId}/profile.${file.name.split('.').pop()}`);
      await uploadBytes(storageRef, file);
      const downloadURL = await getDownloadURL(storageRef);
      
      // Update the user's profile picture URL in the backend
      await axios.put(`${API_BASE_URL}/user/updateProfilePicture/${userId}`, {
        profilePictureUrl: downloadURL
      }, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      // Update the professors list with the new profile picture
      setProfessors(professors.map(prof => 
        prof.userId === userId ? { ...prof, profilePictureUrl: downloadURL } : prof
      ));

      showSnackbar('Profile picture updated successfully', 'success');
    } catch (error) {
      console.error('Error uploading profile picture:', error);
      showSnackbar('Failed to update profile picture', 'error');
    }
  };

  // Add profile picture click handler
  const handleProfileClick = (professor) => {
    setSelectedProfile(professor);
    setShowProfileModal(true);
  };

  // Add a function to refresh faculty status data
  const handleRefreshStatus = () => {
    setStatusLoading(true);
    fetchStatusCounts();
    fetchTimelineForDate(selectedDate);
    showSnackbar('Status data refreshed', 'success');
  };

  // Handle expanding/collapsing day entries
  const handleDayToggle = (dateKey) => {
    const newExpandedDays = new Set(expandedDays);
    if (newExpandedDays.has(dateKey)) {
      newExpandedDays.delete(dateKey);
    } else {
      newExpandedDays.add(dateKey);
    }
    setExpandedDays(newExpandedDays);
  };

  // Modify handleExportDayToExcel to use the selected date
  const handleExportDayToExcel = async (date) => {
    try {
      setTimelineLoading(true);
      const selectedDate = new Date(date);
      const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"];
      const month = monthNames[selectedDate.getMonth()];
      const day = selectedDate.getDate();
      const year = selectedDate.getFullYear();
      const filename = `Faculty Attendance for ${month} ${day}, ${year}.xlsx`;

      // Get timeline data from Firebase
      const timeLogs = dbRef(database, 'timeLogs');
      const timelineSnapshot = await get(timeLogs);
      
      const exportData = [];
      
      // Get start and end of the selected date
      const startOfDay = new Date(date);
      startOfDay.setHours(0, 0, 0, 0);
      const startTimestamp = startOfDay.getTime();
      
      const endOfDay = new Date(date);
      endOfDay.setHours(23, 59, 59, 999);
      const endTimestamp = endOfDay.getTime();
      
      if (timelineSnapshot.exists()) {
        timelineSnapshot.forEach(userSnap => {
          const userId = userSnap.key;
          const userLogs = userSnap.val();
          
          if (typeof userLogs === 'string') return;
          
          // Get all entries for this user on the selected date
          const entries = Object.entries(userLogs)
            .filter(([_, entry]) => {
              if (!entry || !entry.timestamp) return false;
              const entryTimestamp = typeof entry.timestamp === 'string' 
                ? parseInt(entry.timestamp) 
                : entry.timestamp;
              return entryTimestamp >= startTimestamp && entryTimestamp <= endTimestamp;
            })
            .map(([logId, entry]) => ({
              ...entry,
              timestamp: typeof entry.timestamp === 'string' ? parseInt(entry.timestamp) : entry.timestamp
            }));
          
          // Sort entries by timestamp
          entries.sort((a, b) => a.timestamp - b.timestamp);
          
          // Find the faculty details
          const faculty = professors.find(p => p.userId === userId);
          
          if (entries.length > 0) {
            entries.forEach(entry => {
              let status = entry.status || '';
              let activity = entry.type === 'TimeIn' 
                ? (status === 'On Break' ? 'Started Break' : 'Time In')
                : (status === 'Off Duty' ? 'Time Out' : status);
              
              exportData.push({
                'Faculty Name': faculty ? `${faculty.firstName} ${faculty.lastName}` : entry.firstName || 'Unknown',
                'Email': faculty ? faculty.email : entry.email || 'N/A',
                'Time': new Date(entry.timestamp).toLocaleTimeString('en-US', {
                  hour: '2-digit',
                  minute: '2-digit'
                }),
                'Activity': activity,
                'Status': status,
                'Department': faculty?.department?.name || 'N/A'
              });
            });
          }
        });
      }

      // Sort all entries by time
      exportData.sort((a, b) => {
        const timeA = new Date(`${month} ${day}, ${year} ${a.Time}`).getTime();
        const timeB = new Date(`${month} ${day}, ${year} ${b.Time}`).getTime();
        return timeA - timeB;
      });

      // If no data found, add a summary row
      if (exportData.length === 0) {
        exportData.push({
          'Faculty Name': 'No Data Available',
          'Email': 'N/A',
          'Time': 'N/A',
          'Activity': 'No activities recorded',
          'Status': 'N/A',
          'Department': 'N/A'
        });
      }

      // Create Excel workbook
      const worksheet = XLSX.utils.json_to_sheet(exportData);
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Attendance Log');

      // Auto-size columns
      const colWidths = [];
      const header = Object.keys(exportData[0] || {});
      header.forEach((key, i) => {
        const maxLength = Math.max(
          key.length,
          ...exportData.map(row => (row[key] || '').toString().length)
        );
        colWidths[i] = { width: Math.min(maxLength + 2, 50) };
      });
      worksheet['!cols'] = colWidths;

      // Save file
      XLSX.writeFile(workbook, filename);
      
      showSnackbar(`Exported successfully: ${filename}`, 'success');
    } catch (err) {
      console.error('Error exporting to Excel:', err);
      showSnackbar('Failed to export data', 'error');
    } finally {
      setTimelineLoading(false);
    }
  };

  return (
    <Box>
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        mb: 1 
      }}>
      </Box>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="account tabs">
          <Tab 
            icon={<Person sx={{ mr: 1 }} />}
            iconPosition="start"
            label="Faculty Accounts" 
            sx={{ 
              textTransform: 'none', 
              fontWeight: 600,
              fontSize: '1rem',
              color: tabValue === 0 ? 'primary.main' : 'text.secondary',
              '&.Mui-selected': { color: 'primary.main' }
            }} 
          />
          <Tab 
            icon={<AccessTime sx={{ mr: 1 }} />}
            iconPosition="start"
            label="Faculty Status" 
            sx={{ 
              textTransform: 'none', 
              fontWeight: 600,
              fontSize: '1rem',
              color: tabValue === 1 ? 'primary.main' : 'text.secondary',
              '&.Mui-selected': { color: 'primary.main' }
            }} 
          />
        </Tabs>
      </Box>

      {/* Faculty Accounts Tab */}
      <TabPanel value={tabValue} index={0}>
        <Box sx={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          mb: 3
        }}>
          <Typography variant="h5" fontWeight="600" color="#1E293B">
            Faculty Accounts
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
            <FormControl sx={{ minWidth: 200 }}>
              <Select
                value={selectedDepartment}
                onChange={(e) => setSelectedDepartment(e.target.value)}
                displayEmpty
                size="small"
                sx={{
                  bgcolor: '#F8FAFC',
                  '& .MuiOutlinedInput-notchedOutline': {
                    borderColor: '#E2E8F0',
                  },
                }}
              >
                <MenuItem value="">
                  <em>All Departments</em>
                </MenuItem>
                {departments.map((dept) => (
                  <MenuItem key={dept.departmentId} value={dept.departmentId}>
                    {dept.name} ({dept.abbreviation})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
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
        </Box>

        {/* Faculty Table */}
        <Paper 
          elevation={0} 
          sx={{ 
            mb: 3,
            border: '1px solid #E2E8F0',
            borderRadius: '8px',
            overflow: 'hidden'
          }}
        >
          <TableContainer sx={{ maxHeight: 'calc(100vh - 240px)' }}>
            <Table stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>School ID</TableCell>
                  <TableCell 
                    sx={{ 
                      fontWeight: 600, 
                      backgroundColor: '#F8FAFC',
                      cursor: 'pointer',
                      '&:hover': { bgcolor: '#F1F5F9' }
                    }}
                    onClick={() => handleSort('name')}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                      Name
                      {sortConfig.key === 'name' && (
                        sortConfig.direction === 'asc' ? 
                          <ArrowUpward sx={{ fontSize: 16 }} /> : 
                          <ArrowDownward sx={{ fontSize: 16 }} />
                      )}
                    </Box>
                  </TableCell>
                  <TableCell 
                    sx={{ 
                      fontWeight: 600, 
                      backgroundColor: '#F8FAFC',
                      cursor: 'pointer',
                      '&:hover': { bgcolor: '#F1F5F9' }
                    }}
                    onClick={() => handleSort('email')}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                      Email
                      {sortConfig.key === 'email' && (
                        sortConfig.direction === 'asc' ? 
                          <ArrowUpward sx={{ fontSize: 16 }} /> : 
                          <ArrowDownward sx={{ fontSize: 16 }} />
                      )}
                    </Box>
                  </TableCell>
                  <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Department</TableCell>
                  <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC', width: 180 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {isLoading ? (
                  <AccountTableSkeleton />
                ) : error ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'error.main' }}>
                      {error}
                    </TableCell>
                  </TableRow>
                ) : sortedProfessors.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                      No faculty members found
                    </TableCell>
                  </TableRow>
                ) : (
                  sortedProfessors.map((professor) => (
                    <TableRow 
                      key={professor.userId}
                      sx={{ '&:hover': { bgcolor: '#F1F5F9' } }}
                    >
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Avatar
                            src={professor.profilePictureUrl}
                            alt={`${professor.firstName} ${professor.lastName}`}
                            sx={{ 
                              width: 40, 
                              height: 40, 
                              cursor: 'pointer',
                              '&:hover': { opacity: 0.8 }
                            }}
                            onClick={() => handleProfileClick(professor)}
                          />
                          {professor.schoolId}
                        </Box>
                      </TableCell>
                      <TableCell>{`${professor.firstName} ${professor.lastName}`}</TableCell>
                      <TableCell>{professor.email}</TableCell>
                      <TableCell>
                        {(() => {
                          try {
                            if (professor.role === 'ADMIN') {
                              return (
                                <>
                                  ADMIN
                                  <Typography variant="caption" color="#64748B" display="block">
                                    AD
                                  </Typography>
                                </>
                              );
                            } else if (professor.department && professor.department.departmentId) {
                              return (
                                <>
                                  {getDepartmentName(professor.department.departmentId)}
                                  <Typography variant="caption" color="#64748B" display="block">
                                    {getDepartmentAbbreviation(professor.department.departmentId)}
                                  </Typography>
                                </>
                              );
                            } else {
                              return 'Not assigned';
                            }
                          } catch (error) {
                            console.error("Error rendering department for", professor.firstName, professor.lastName, error);
                            return 'Not assigned';
                          }
                        })()}
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <IconButton 
                            size="small" 
                            onClick={() => handleViewClick(professor)}
                            sx={{ color: '#64748B' }}
                            title="View Details"
                          >
                            <VisibilityOutlined fontSize="small" />
                          </IconButton>
                          <IconButton 
                            size="small" 
                            onClick={() => handleEditClick(professor)}
                            sx={{ color: '#0288d1' }}
                            title="Edit"
                          >
                            <Edit fontSize="small" />
                          </IconButton>
                          <IconButton 
                            size="small" 
                            onClick={() => handleDeleteProfessor(professor.userId)}
                            sx={{ color: '#EF4444' }}
                            title="Delete"
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
      </TabPanel>

      {/* Current Status Tab */}
      <TabPanel value={tabValue} index={1}>
        {/* Current Status Section */}
        <Box sx={{ mb: 4 }}>
          <Box sx={{ 
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 2
          }}>
            <Typography variant="h5" fontWeight="600" color="#1E293B">
              Faculty Status
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="body2" color="text.secondary" sx={{ display: 'flex', alignItems: 'center' }}>
                Last updated: {new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true })}
              </Typography>
              <IconButton 
                size="small" 
                onClick={handleRefreshStatus}
                sx={{ 
                  color: '#0288d1',
                  '&:hover': { color: '#01579b' }
                }}
                disabled={statusLoading}
              >
                <Refresh />
              </IconButton>
            </Box>
          </Box>

          {/* Total Faculty Card */}
          <Card
            elevation={0}
            sx={{
              p: 3,
              mb: 3,
              borderRadius: 2,
              bgcolor: '#fff',
              border: '1px solid #E2E8F0',
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <PeopleAlt sx={{ fontSize: 24, color: '#1E293B', mr: 1.5 }} />
                <Typography variant="h6" fontWeight="600" color="#1E293B">
                  Total Faculty Count
                </Typography>
              </Box>
              <Box sx={{
                display: 'flex',
                alignItems: 'center',
                px: 3,
                py: 1,
                borderRadius: 2,
                bgcolor: 'rgba(30,41,59,0.1)',
                minWidth: 60,
                justifyContent: 'center'
              }}>
                <Typography variant="h4" fontWeight="700" color="#1E293B">
                  {statusLoading ? <CircularProgress size={30} /> : statusCounts.total}
                </Typography>
              </Box>
            </Box>
          </Card>

          {/* Status Cards */}
          <Box sx={{ 
            display: 'flex', 
            gap: 2,
            mb: 4,
            flexDirection: { xs: 'column', sm: 'row' }
          }}>
            {/* On Duty Card */}
            <Paper
              elevation={0}
              sx={{
                flex: 1,
                p: 3,
                borderRadius: 2,
                bgcolor: '#1E293B',
                color: 'white',
                border: '1px solid rgba(255,255,255,0.1)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                position: 'relative',
                overflow: 'hidden',
                transition: 'transform 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)'
                }
              }}
            >
              <Box sx={{ 
                height: 10, 
                width: 10, 
                borderRadius: '50%', 
                bgcolor: '#4CAF50', 
                position: 'absolute',
                top: 15,
                left: 15,
                boxShadow: '0 0 10px #4CAF50'
              }} />
              <Box sx={{
                position: 'absolute',
                top: 0,
                right: 0,
                width: '150px',
                height: '150px',
                background: 'radial-gradient(circle at top right, rgba(76, 175, 80, 0.2), transparent 70%)',
                borderRadius: '0 0 0 100%'
              }} />
              <Typography variant="h6" sx={{ mb: 1, mt: 1, fontWeight: 600 }}>
                On Duty
              </Typography>
              <Typography variant="h2" fontWeight="700" sx={{ mb: 1 }}>
                {statusLoading ? <CircularProgress size={40} /> : statusCounts.onDuty}
              </Typography>
              <Box sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%',
                mt: 'auto',
                gap: 1
              }}>
                <AccessTime sx={{ fontSize: 18, opacity: 0.7 }} />
                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                  Active faculty members
                </Typography>
              </Box>
            </Paper>

            {/* On Break Card */}
            <Paper
              elevation={0}
              sx={{
                flex: 1,
                p: 3,
                borderRadius: 2,
                bgcolor: '#1E293B',
                color: 'white',
                border: '1px solid rgba(255,255,255,0.1)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                position: 'relative',
                overflow: 'hidden',
                transition: 'transform 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)'
                }
              }}
            >
              <Box sx={{ 
                height: 10, 
                width: 10, 
                borderRadius: '50%', 
                bgcolor: '#FF9800', 
                position: 'absolute',
                top: 15,
                left: 15,
                boxShadow: '0 0 10px #FF9800'
              }} />
              <Box sx={{
                position: 'absolute',
                top: 0,
                right: 0,
                width: '150px',
                height: '150px',
                background: 'radial-gradient(circle at top right, rgba(255, 152, 0, 0.2), transparent 70%)',
                borderRadius: '0 0 0 100%'
              }} />
              <Typography variant="h6" sx={{ mb: 1, mt: 1, fontWeight: 600 }}>
                On Break
              </Typography>
              <Typography variant="h2" fontWeight="700" sx={{ mb: 1 }}>
                {statusLoading ? <CircularProgress size={40} /> : statusCounts.onBreak}
              </Typography>
              <Box sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%',
                mt: 'auto',
                gap: 1
              }}>
                <RemoveFromQueue sx={{ fontSize: 18, opacity: 0.7 }} />
                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                  Faculty on break
                </Typography>
              </Box>
            </Paper>

            {/* Off Duty Card */}
            <Paper
              elevation={0}
              sx={{
                flex: 1,
                p: 3,
                borderRadius: 2,
                bgcolor: '#1E293B',
                color: 'white',
                border: '1px solid rgba(255,255,255,0.1)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                position: 'relative',
                overflow: 'hidden',
                transition: 'transform 0.2s',
                '&:hover': {
                  transform: 'translateY(-4px)'
                }
              }}
            >
              <Box sx={{ 
                height: 10, 
                width: 10, 
                borderRadius: '50%', 
                bgcolor: '#F44336', 
                position: 'absolute',
                top: 15,
                left: 15,
                boxShadow: '0 0 10px #F44336'
              }} />
              <Box sx={{
                position: 'absolute',
                top: 0,
                right: 0,
                width: '150px',
                height: '150px',
                background: 'radial-gradient(circle at top right, rgba(244, 67, 54, 0.2), transparent 70%)',
                borderRadius: '0 0 0 100%'
              }} />
              <Typography variant="h6" sx={{ mb: 1, mt: 1, fontWeight: 600 }}>
                Off Duty
              </Typography>
              <Typography variant="h2" fontWeight="700" sx={{ mb: 1 }}>
                {statusLoading ? <CircularProgress size={40} /> : statusCounts.offDuty}
              </Typography>
              <Box sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%',
                mt: 'auto',
                gap: 1
              }}>
                <DirectionsWalk sx={{ fontSize: 18, opacity: 0.7 }} />
                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                  Inactive faculty
                </Typography>
              </Box>
            </Paper>
          </Box>
        </Box>

        {/* Today's Timeline Section */}
        <Box sx={{ mb: 4 }}>
          <Box sx={{ 
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 2
          }}>
            <Typography variant="h5" fontWeight="600" color="#1E293B">
              Faculty Timeline
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
              <Box sx={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: 1,
                bgcolor: '#F8FAFC',
                border: '1px solid #E2E8F0',
                borderRadius: 1,
                px: 2,
                py: 0.5
              }}>
                <IconButton 
                  size="small" 
                  onClick={() => handleDateChange('prev')}
                  sx={{ color: '#64748B' }}
                >
                  <ChevronLeft />
                </IconButton>
                <Typography 
                  variant="subtitle2" 
                  sx={{ 
                    minWidth: 200,
                    textAlign: 'center',
                    color: '#1E293B',
                    fontWeight: 500
                  }}
                >
                  {selectedDate.toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                  })}
                </Typography>
                <IconButton 
                  size="small" 
                  onClick={() => handleDateChange('next')}
                  disabled={selectedDate >= new Date()}
                  sx={{ color: '#64748B' }}
                >
                  <ChevronRight />
                </IconButton>
              </Box>
              <Button 
                variant="outlined" 
                size="small" 
                startIcon={<FileDownload fontSize="small" />}
                onClick={handleExportToExcel}
                disabled={timelineLoading}
                sx={{
                  textTransform: 'none',
                  borderColor: '#E2E8F0',
                  color: '#64748B',
                  fontWeight: 500,
                  fontSize: '0.875rem',
                }}
              >
                Export to Excel
              </Button>
              <IconButton
                size="small"
                onClick={() => fetchTimelineForDate(selectedDate)}
                disabled={timelineLoading}
                sx={{ 
                  color: '#0288d1',
                  '&:hover': { color: '#01579b' }
                }}
              >
                <Refresh />
              </IconButton>
            </Box>
          </Box>

          {/* Timeline Content */}
          <Paper
            elevation={0}
            sx={{
              p: 3,
              borderRadius: 2,
              bgcolor: '#fff',
              border: '1px solid #E2E8F0',
              minHeight: 200,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: timelineLoading || todayTimeline.length === 0 ? 'center' : 'flex-start',
              alignItems: timelineLoading || todayTimeline.length === 0 ? 'center' : 'stretch'
            }}
          >
            {timelineLoading ? (
              <CircularProgress size={30} />
            ) : todayTimeline.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <History sx={{ fontSize: 48, color: '#CBD5E1', mb: 2 }} />
                <Typography variant="body1" color="text.secondary">
                  No timeline data available for {
                    selectedDate.toLocaleDateString('en-US', {
                      month: 'long',
                      day: 'numeric',
                      year: 'numeric'
                    })
                  }
                </Typography>
              </Box>
            ) : (
              <Box>
                {todayTimeline.map((item, index) => (
                  <Box 
                    key={item.id} 
                    sx={{ 
                      display: 'flex', 
                      mb: index < todayTimeline.length - 1 ? 2 : 0,
                      py: 1,
                      position: 'relative'
                    }}
                  >
                    <Box sx={{ 
                      width: '80px', 
                      textAlign: 'right', 
                      pr: 2, 
                      color: '#64748B',
                      position: 'relative',
                      zIndex: 1
                    }}>
                      <Typography variant="body2" fontWeight="500">
                        {item.time}
                      </Typography>
                    </Box>
                    <Box sx={{ 
                      position: 'relative',
                      borderLeft: '2px solid',
                      borderColor: item.status === 'On Duty' ? '#4CAF50' : 
                                item.status === 'On Break' ? '#FF9800' : 
                                item.status === 'Off Duty' ? '#F44336' : 
                                '#9E9E9E',
                      pl: 3,
                      pb: index < todayTimeline.length - 1 ? 3 : 0,
                      flex: 1
                    }}>
                      <Box sx={{
                        width: 10,
                        height: 10,
                        borderRadius: '50%',
                        bgcolor: item.status === 'On Duty' ? '#4CAF50' : 
                                item.status === 'On Break' ? '#FF9800' : 
                                item.status === 'Off Duty' ? '#F44336' : 
                                '#9E9E9E',
                        position: 'absolute',
                        left: -6,
                        top: 5,
                        boxShadow: `0 0 10px ${
                          item.status === 'On Duty' ? '#4CAF50' : 
                          item.status === 'On Break' ? '#FF9800' : 
                          item.status === 'Off Duty' ? '#F44336' : 
                          '#9E9E9E'
                        }`,
                        zIndex: 1
                      }} />
                      <Box sx={{
                        bgcolor: item.status === 'On Duty' ? '#F0FDF4' : 
                                item.status === 'On Break' ? '#FFF7ED' : 
                                item.status === 'Off Duty' ? '#FEF2F2' : 
                                '#F8FAFC',
                        p: 2,
                        borderRadius: 1,
                        border: '1px solid',
                        borderColor: item.status === 'On Duty' ? '#DCFCE7' : 
                                   item.status === 'On Break' ? '#FFEDD5' : 
                                   item.status === 'Off Duty' ? '#FEE2E2' : 
                                   '#E2E8F0'
                      }}>
                        <Typography variant="body2" fontWeight="600" color="#1E293B">
                          {item.description}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {item.name}
                        </Typography>
                      </Box>
                    </Box>
                  </Box>
                ))}
              </Box>
            )}
          </Paper>
        </Box>

        {/* Attendance Status History Section */}
        <Box sx={{ mb: 4 }}>
          <Box sx={{ 
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 2
          }}>
            <Typography variant="h5" fontWeight="600" color="#1E293B">
              Attendance Status History
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
              <Button 
                variant="outlined" 
                size="small" 
                startIcon={<FileDownload fontSize="small" />}
                onClick={() => handleExportDayToExcel(selectedDate)}
                disabled={timelineLoading}
                sx={{
                  textTransform: 'none',
                  borderColor: '#E2E8F0',
                  color: '#64748B',
                  fontWeight: 500,
                  fontSize: '0.875rem',
                }}
              >
                Export Current Day
              </Button>
              <IconButton
                size="small"
                onClick={() => fetchTimelineForDate(selectedDate)}
                disabled={timelineLoading}
                sx={{ 
                  color: '#0288d1',
                  '&:hover': { color: '#01579b' }
                }}
              >
                <Refresh />
              </IconButton>
            </Box>
          </Box>

          {/* History Table */}
          <Paper
            elevation={0}
            sx={{
              borderRadius: 2,
              bgcolor: '#fff',
              border: '1px solid #E2E8F0',
              overflow: 'hidden'
            }}
          >
            {historyLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4 }}>
                <CircularProgress size={30} />
              </Box>
            ) : todayTimeline.length === 0 ? (
              <Box sx={{ p: 4, textAlign: 'center' }}>
                <History sx={{ fontSize: 48, color: '#CBD5E1', mb: 2 }} />
                <Typography variant="body1" color="text.secondary">
                  No attendance history available for {
                    selectedDate.toLocaleDateString('en-US', {
                      month: 'long',
                      day: 'numeric',
                      year: 'numeric'
                    })
                  }
                </Typography>
              </Box>
            ) : (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Time</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Faculty Name</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Activity</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Department</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {todayTimeline.map((entry) => {
                      const faculty = professors.find(p => p.userId === entry.userId);
                      return (
                        <TableRow key={entry.id} sx={{ '&:hover': { bgcolor: '#F1F5F9' } }}>
                          <TableCell>{entry.time}</TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Avatar
                                src={faculty?.profilePictureUrl}
                                alt={entry.name}
                                sx={{ width: 24, height: 24 }}
                              >
                                {entry.name.charAt(0)}
                              </Avatar>
                              {entry.name}
                            </Box>
                          </TableCell>
                          <TableCell>{entry.description}</TableCell>
                          <TableCell>
                            <Chip 
                              label={entry.status} 
                              size="small"
                              sx={{ 
                                bgcolor: entry.status === 'On Duty' ? '#F0FDF4' : 
                                        entry.status === 'On Break' ? '#FFF7ED' : 
                                        entry.status === 'Off Duty' ? '#FEF2F2' : 
                                        '#F8FAFC',
                                color: entry.status === 'On Duty' ? '#15803D' : 
                                       entry.status === 'On Break' ? '#9A3412' : 
                                       entry.status === 'Off Duty' ? '#991B1B' : 
                                       '#64748B',
                                fontWeight: 500,
                                fontSize: '0.75rem'
                              }}
                            />
                          </TableCell>
                          <TableCell>
                            {faculty?.department?.name || 'N/A'}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Paper>
        </Box>
      </TabPanel>

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
              <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
                <Box sx={{ position: 'relative' }}>
                  <Avatar
                    src={newProfilePicture ? URL.createObjectURL(newProfilePicture) : ''}
                    alt="New faculty"
                    sx={{ 
                      width: 100, 
                      height: 100,
                      mb: 1
                    }}
                  />
                  <input
                    accept="image/*"
                    style={{ display: 'none' }}
                    id="new-profile-picture-upload"
                    type="file"
                    onChange={(e) => {
                      if (e.target.files[0]) {
                        setNewProfilePicture(e.target.files[0]);
                      }
                    }}
                  />
                  <label htmlFor="new-profile-picture-upload">
                    <IconButton
                      component="span"
                      sx={{
                        position: 'absolute',
                        bottom: 0,
                        right: 0,
                        bgcolor: '#0288d1',
                        color: 'white',
                        '&:hover': {
                          bgcolor: '#0277bd',
                        },
                      }}
                    >
                      <PhotoCamera fontSize="small" />
                    </IconButton>
                  </label>
                </Box>
              </Box>
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
              <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
                <Box sx={{ position: 'relative' }}>
                  <Avatar
                    src={editProfilePicture ? URL.createObjectURL(editProfilePicture) : editingProfessor.profilePictureUrl}
                    alt={`${editingProfessor.firstName} ${editingProfessor.lastName}`}
                    sx={{ 
                      width: 100, 
                      height: 100,
                      mb: 1
                    }}
                  />
                  <input
                    accept="image/*"
                    style={{ display: 'none' }}
                    id="edit-profile-picture-upload"
                    type="file"
                    onChange={(e) => {
                      if (e.target.files[0]) {
                        setEditProfilePicture(e.target.files[0]);
                      }
                    }}
                  />
                  <label htmlFor="edit-profile-picture-upload">
                    <IconButton
                      component="span"
                      sx={{
                        position: 'absolute',
                        bottom: 0,
                        right: 0,
                        bgcolor: '#0288d1',
                        color: 'white',
                        '&:hover': {
                          bgcolor: '#0277bd',
                        },
                      }}
                    >
                      <PhotoCamera fontSize="small" />
                    </IconButton>
                  </label>
                </Box>
              </Box>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  School ID
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  name="schoolId"
                  placeholder="Enter school ID (e.g., 22-2220-759)"
                  value={editingProfessor.schoolId}
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
          width: 480,
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          p: 0,
          overflow: 'hidden'
        }}>
          <Box sx={{ 
            p: 2, 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center', 
            borderBottom: '1px solid #E2E8F0',
            bgcolor: '#F8FAFC'
          }}>
            <Typography variant="h6" fontWeight="600">
              Faculty Details
            </Typography>
            <IconButton onClick={() => setShowViewModal(false)}>
              <Close />
            </IconButton>
          </Box>
          {viewingProfessor && (
            <Box sx={{ p: 3 }}>
              <Box sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                <Avatar 
                  src={viewingProfessor.profilePictureUrl}
                  sx={{ 
                    width: 64, 
                    height: 64,
                    bgcolor: '#0288d1',
                    fontSize: '1.5rem'
                  }}
                >
                  {viewingProfessor.firstName?.[0]}{viewingProfessor.lastName?.[0]}
                </Avatar>
                <Box>
                  <Typography variant="h6" fontWeight="600">
                    {`${viewingProfessor.firstName} ${viewingProfessor.lastName}`}
                  </Typography>
                  <Chip 
                    label={viewingProfessor.role === 'ADMIN' ? 'Administrator' : 'Faculty'} 
                    size="small"
                    sx={{ 
                      bgcolor: viewingProfessor.role === 'ADMIN' ? '#EFF6FF' : '#ECFDF5',
                      color: viewingProfessor.role === 'ADMIN' ? '#3B82F6' : '#10B981',
                      fontWeight: 500,
                      fontSize: '0.75rem'
                    }}
                  />
                </Box>
              </Box>
              
              <Divider sx={{ my: 2 }} />
              
              <Box sx={{ mb: 3 }}>
                <List disablePadding>
                  <ListItem disablePadding sx={{ mb: 2 }}>
                    <ListItemIcon sx={{ minWidth: 36 }}>
                      <Badge fontSize="small" sx={{ color: '#64748B' }} />
                    </ListItemIcon>
                    <ListItemText 
                      primary={
                        <Typography variant="body2" fontWeight="600" color="#1E293B">
                          School ID
                        </Typography>
                      }
                      secondary={viewingProfessor.schoolId || 'Not specified'}
                      secondaryTypographyProps={{ color: '#64748B' }}
                    />
                  </ListItem>
                  
                  <ListItem disablePadding sx={{ mb: 2 }}>
                    <ListItemIcon sx={{ minWidth: 36 }}>
                      <Group fontSize="small" sx={{ color: '#64748B' }} />
                    </ListItemIcon>
                    <ListItemText 
                      primary={
                        <Typography variant="body2" fontWeight="600" color="#1E293B">
                          Department
                        </Typography>
                      }
                      secondary={
                        viewingProfessor.department?.departmentId 
                          ? `${getDepartmentName(viewingProfessor.department.departmentId)} (${getDepartmentAbbreviation(viewingProfessor.department.departmentId)})`
                          : viewingProfessor.role === 'ADMIN' ? 'Administrator' : 'Not assigned'
                      }
                      secondaryTypographyProps={{ color: '#64748B' }}
                    />
                  </ListItem>
                  
                  <ListItem disablePadding sx={{ mb: 2 }}>
                    <ListItemIcon sx={{ minWidth: 36 }}>
                      <Email fontSize="small" sx={{ color: '#64748B' }} />
                    </ListItemIcon>
                    <ListItemText 
                      primary={
                        <Typography variant="body2" fontWeight="600" color="#1E293B">
                          Email
                        </Typography>
                      }
                      secondary={viewingProfessor.email || 'Not specified'}
                      secondaryTypographyProps={{ color: '#64748B' }}
                    />
                  </ListItem>
                </List>
              </Box>
            </Box>
          )}
        </Box>
      </Modal>

      {/* Attended Events Modal */}
      <Modal
        open={showAttendedEventsModal}
        onClose={() => setShowAttendedEventsModal(false)}
        aria-labelledby="attended-events-modal-title"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 700,
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          p: 0,
          overflow: 'hidden',
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
            <Typography variant="h6" fontWeight="600" color="black">
              {viewingProfessor ? `Events Attended by ${viewingProfessor.firstName} ${viewingProfessor.lastName}` : 'Attended Events'}
            </Typography>
            <IconButton onClick={() => setShowAttendedEventsModal(false)}>
              <Close />
            </IconButton>
          </Box>
          
          <Box sx={{ flexGrow: 1, overflow: 'auto', p: 0 }}>
            {loadingEvents ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200 }}>
                <CircularProgress size={30} />
              </Box>
            ) : attendedEvents.length === 0 ? (
              <Box sx={{ p: 3, textAlign: 'center', color: '#64748B' }}>
                <EventAvailable sx={{ fontSize: 48, color: '#CBD5E1', mb: 2 }} />
                <Typography variant="body1" fontWeight="500">
                  No events attended yet
                </Typography>
                <Typography variant="body2" color="#94A3B8" sx={{ mt: 1 }}>
                  This faculty member hasn't attended any events
                </Typography>
              </Box>
            ) : (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Event Name</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Date</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Time</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Attendance Status</TableCell>
                      <TableCell sx={{ fontWeight: 600, backgroundColor: '#F8FAFC' }}>Entry Type</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {attendedEvents.map((event) => (
                      <TableRow key={event.eventId} sx={{ '&:hover': { bgcolor: '#F1F5F9' } }}>
                        <TableCell>{event.eventName}</TableCell>
                        <TableCell>{formatDate(event.eventDate)}</TableCell>
                        <TableCell>{`${formatTime(event.timeIn)} - ${formatTime(event.timeOut)}`}</TableCell>
                        <TableCell>
                          <Chip 
                            icon={<CheckCircleOutline fontSize="small" />}
                            label="Present" 
                            size="small"
                            sx={{ 
                              bgcolor: '#ECFDF5',
                              color: '#10B981',
                              fontWeight: 500,
                              fontSize: '0.75rem'
                            }}
                          />
                        </TableCell>
                        <TableCell sx={{ fontWeight: 200, backgroundColor: '#F8FAFC' }}>
                          {event.manualEntry ?'Manual Entry'  : 'QR'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        </Box>
      </Modal>

      {/* Profile Picture Zoom Modal */}
      <Modal
        open={showProfileModal}
        onClose={() => setShowProfileModal(false)}
        aria-labelledby="profile-picture-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          bgcolor: 'background.paper',
          boxShadow: 24,
          p: 0,
          borderRadius: 1,
          maxWidth: '90vw',
          maxHeight: '90vh',
          overflow: 'hidden'
        }}>
          <Box sx={{ 
            p: 2, 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            borderBottom: '1px solid #E2E8F0'
          }}>
            <Typography variant="h6" fontWeight="600">
              Profile Picture
            </Typography>
            <IconButton onClick={() => setShowProfileModal(false)}>
              <Close />
            </IconButton>
          </Box>
          
          {selectedProfile && (
            <Box sx={{ p: 3, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
              <Avatar
                src={selectedProfile.profilePictureUrl}
                alt={`${selectedProfile.firstName} ${selectedProfile.lastName}`}
                sx={{ 
                  width: 200, 
                  height: 200,
                  boxShadow: '0 0 10px rgba(0,0,0,0.1)'
                }}
              />
              <Typography variant="h6">
                {`${selectedProfile.firstName} ${selectedProfile.lastName}`}
              </Typography>
              
              <Box>
                <input
                  accept="image/*"
                  style={{ display: 'none' }}
                  id="profile-picture-upload"
                  type="file"
                  onChange={(e) => {
                    if (e.target.files[0]) {
                      handleProfilePictureUpload(selectedProfile.userId, e.target.files[0]);
                      setShowProfileModal(false);
                    }
                  }}
                />
                <label htmlFor="profile-picture-upload">
                  <Button
                    variant="contained"
                    component="span"
                    startIcon={<PhotoCamera />}
                    sx={{
                      bgcolor: '#0288d1',
                      '&:hover': {
                        bgcolor: '#0277bd',
                      },
                      textTransform: 'none',
                      fontWeight: 500
                    }}
                  >
                    Change Profile Picture
                  </Button>
                </label>
              </Box>
            </Box>
          )}
        </Box>
      </Modal>

      {/* Notification Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
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