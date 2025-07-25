import { useState, useEffect, memo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useTheme } from '../contexts/ThemeContext';
import {
  Box,
  Typography,
  Button,
  IconButton,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Breadcrumbs,
  Link,
  Card,
  CardContent,
  Grid,
  Chip,
  Tooltip,
  Snackbar,
  Alert,
  TextField,
  InputAdornment,
  Modal,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Avatar
} from '@mui/material';
import {
  ArrowBack,
  Event,
  AccessTime,
  CalendarToday,
  School,
  Group,
  Login,
  Logout,
  CheckCircle,
  Search,
  ManageAccounts,
  Close,
  FileDownload
} from '@mui/icons-material';
import './attendance.css';
import AttendanceAnalytics from '../components/AttendanceAnalytics';
import { API_BASE_URL, getApiUrl, API_ENDPOINTS } from '../utils/api';
import * as XLSX from 'xlsx';
import html2canvas from 'html2canvas';

// Separate Modal Component with its own state
const AttendanceModal = memo(({ 
  open, 
  onClose, 
  attendees, 
  onTimeInOut, 
  actionLoading,
  formatTimeInStatus,
  formatTimeoutStatus,
  eventId
}) => {
  const { darkMode } = useTheme();
  const [modalSearchQuery, setModalSearchQuery] = useState('');
  const [allUsers, setAllUsers] = useState([]);
  const [modalFilteredUsers, setModalFilteredUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);
  const [loading, setLoading] = useState(false);

  // Reset filtered attendees when modal opens or attendees change
  useEffect(() => {
    setModalFilteredUsers(allUsers);
    setModalSearchQuery('');
  }, [allUsers, open]);

  useEffect(() => {
    if (open) {
      fetchAllUsers();
    }
  }, [open]);

  const fetchAllUsers = async () => {
    try {
      setLoading(true);
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_ALL_USERS));
      
      // Create a map of existing attendees by userId for quick lookup
      const attendeeMap = new Map();
      attendees.forEach(attendee => {
        attendeeMap.set(attendee.userId, attendee);
      });
      
      // Merge user data with attendance data if available
      const usersWithAttendance = response.data.map(user => {
        const attendeeData = attendeeMap.get(user.userId);
        
        // Handle different data structures for time in/out
        let timeIn = 'N/A';
        let timeOut = 'N/A';
        
        if (attendeeData) {
          // Handle new structure with hasTimedOut, timeOutTimestamp, and timestamp
          if (attendeeData.hasTimedOut !== undefined) {
            timeIn = attendeeData.timestamp || attendeeData.timeIn || 'N/A';
            timeOut = attendeeData.hasTimedOut ? (attendeeData.timeOutTimestamp || 'N/A') : 'N/A';
          }
          // Handle old structure (has timeIn in ISO format)
          else if (attendeeData.timeIn && attendeeData.timeIn.includes('T')) {
            timeIn = attendeeData.timeIn;
            timeOut = attendeeData.timeOut || 'N/A';
          }
          // Handle other structures
          else {
            timeIn = attendeeData.timeIn || attendeeData.timestamp || 'N/A';
            timeOut = attendeeData.timeOut || 'N/A';
          }
        }
        
        return {
          ...user,
          // Include attendance data if user is already an attendee
          timeIn: timeIn,
          timeOut: timeOut,
          manualEntry: attendeeData?.manualEntry || 'false',
          // Make sure department is displayed correctly
          department: user.department?.name || 'N/A'
        };
      });
      
      setAllUsers(usersWithAttendance);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching users:', error);
      setLoading(false);
    }
  };

  // Handle search input change for modal
  const handleModalSearchChange = (event) => {
    const query = event.target.value.toLowerCase();
    setModalSearchQuery(query);
    
    if (query.trim() === '') {
      setModalFilteredUsers(allUsers);
    } else {
      const filtered = allUsers.filter(
        user => 
          (user.firstName && user.firstName.toLowerCase().includes(query)) ||
          (user.lastName && user.lastName.toLowerCase().includes(query)) ||
          (user.email && user.email.toLowerCase().includes(query)) ||
          (user.department && user.department.toLowerCase().includes(query)) ||
          (user.schoolId && user.schoolId.toLowerCase().includes(query))
      );
      setModalFilteredUsers(filtered);
    }
  };
  // Open confirmation dialog
  const handleOpenConfirmDialog = (user, action) => {
    setSelectedUser(user);
    setConfirmAction(action);
    setConfirmDialogOpen(true);
  };
  
  // Close confirmation dialog
  const handleCloseConfirmDialog = () => {
    setConfirmDialogOpen(false);
  };

  // Handle confirmation dialog confirm button
  const handleConfirmAction = () => {
    if (!selectedUser) return;
    
    if (confirmAction === 'timein') {
      onTimeInOut(selectedUser.userId, 'timein');
    } else if (confirmAction === 'timeout') {
      onTimeInOut(selectedUser.userId, 'timeout');
    }
    
    setConfirmDialogOpen(false);
  };

  return (
    <>
      <Modal
        open={open}
        onClose={onClose}
        aria-labelledby="manage-attendance-modal"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: '90%',
          maxWidth: 900,
          maxHeight: '90vh',
          bgcolor: darkMode ? '#1e1e1e' : 'background.paper',
          boxShadow: '0 10px 25px rgba(0,0,0,0.15)',
          borderRadius: '12px',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <Box sx={{ 
            p: 2.5, 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            borderBottom: '1px solid',
            borderColor: darkMode ? '#333333' : '#E2E8F0',
            bgcolor: darkMode ? '#1e1e1e' : '#F8FAFC'
          }}>
            <Typography variant="h6" fontWeight="600" sx={{ display: 'flex', alignItems: 'center', gap: 1, color: darkMode ? '#f5f5f5' : '#1E293B' }}>
              <ManageAccounts sx={{ color: darkMode ? '#90caf9' : '#0288d1' }} /> 
              Manage Event Attendance
            </Typography>
            <IconButton 
              onClick={onClose} 
              sx={{ 
                bgcolor: darkMode ? '#333333' : '#F1F5F9', 
                color: darkMode ? '#f5f5f5' : 'inherit',
                '&:hover': { bgcolor: darkMode ? '#404040' : '#E2E8F0' },
                transition: 'all 0.2s ease',
              }}
            >
              <Close />
            </IconButton>
          </Box>
          
          <Box sx={{ p: 2.5, borderBottom: '1px solid', borderColor: darkMode ? '#333333' : '#E2E8F0', bgcolor: darkMode ? '#1e1e1e' : '#FFFFFF' }}>
            <TextField
              placeholder="Search faculty members..."
              variant="outlined"
              size="small"
              value={modalSearchQuery}
              onChange={handleModalSearchChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search sx={{ color: darkMode ? '#90caf9' : '#64748B', fontSize: 20 }} />
                  </InputAdornment>
                ),
              }}
              sx={{
                width: '100%',
                maxWidth: '350px',
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                  backgroundColor: darkMode ? '#333333' : '#F8FAFC',
                  '& fieldset': {
                    borderColor: darkMode ? '#404040' : 'inherit',
                  },
                  '&:hover fieldset': {
                    borderColor: darkMode ? '#90caf9' : '#0288d1',
                  },
                  '&.Mui-focused fieldset': {
                    borderColor: darkMode ? '#90caf9' : '#0288d1',
                  },
                  '& input': {
                    color: darkMode ? '#f5f5f5' : 'inherit',
                  }
                }
              }}
            />
          </Box>

          <Box sx={{ 
            flex: 1, 
            overflowY: 'auto',
            p: 0,
            bgcolor: darkMode ? '#1e1e1e' : '#FFFFFF'
          }}>
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 6 }}>
                <CircularProgress size={32} sx={{ color: '#0288d1' }} />
              </Box>
            ) : (
              <TableContainer>
                <Table stickyHeader size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC', py: 1.5 }}>Name</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC', py: 1.5 }}>School ID</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC', py: 1.5 }}>Email</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC', py: 1.5 }}>Department</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC', py: 1.5 }}>Time In</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC', py: 1.5 }}>Time Out</TableCell>
                      <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC', width: 140, py: 1.5 }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {modalFilteredUsers.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={7} align="center" sx={{ py: 3 }}>
                          {allUsers.length === 0 ? (
                            <Typography>No faculty members found</Typography>
                          ) : (
                            <Typography>No faculty members match your search</Typography>
                          )}
                        </TableCell>
                      </TableRow>
                    ) : (
                      modalFilteredUsers.map((user, index) => (
                        <TableRow 
                          key={index} 
                          sx={{ 
                            '&:hover': { bgcolor: '#F8FAFC' },
                            bgcolor: index % 2 === 0 ? 'white' : '#F9FAFB'
                          }}
                        >
                          <TableCell sx={{ color: '#1E293B', fontWeight: 500 }}>
                            {user.firstName} {user.lastName}
                          </TableCell>
                          <TableCell sx={{ color: '#64748B' }}>{user.schoolId || 'N/A'}</TableCell>
                          <TableCell sx={{ color: '#64748B' }}>{user.email}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>
                            {user.department !== 'N/A' ? (
                              <Chip 
                                label={user.department}
                                size="small"
                                sx={{
                                  backgroundColor: '#F0FDF4',
                                  color: '#16A34A',
                                  fontWeight: 500,
                                  fontSize: '0.75rem',
                                  height: 24,
                                  borderRadius: '4px'
                                }}
                              />
                            ) : 'N/A'}
                          </TableCell>
                          <TableCell sx={{ color: '#64748B' }}>
                            {formatTimeInStatus(user.timeIn, user.manualEntry)}
                          </TableCell>
                          <TableCell sx={{ color: '#64748B' }}>
                            {formatTimeoutStatus(user.timeOut, user.manualEntry)}
                          </TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', gap: 1 }}>
                              <Tooltip title="Manual Time In">
                                <span>
                                  <Button
                                    size="small"
                                    variant="contained"
                                    color="primary"
                                    onClick={() => handleOpenConfirmDialog(user, 'timein')}
                                    disabled={actionLoading}
                                    startIcon={<Login sx={{ fontSize: 16 }} />}
                                    sx={{ 
                                      fontSize: '0.75rem',
                                      py: 0.5,
                                      textTransform: 'none',
                                      borderRadius: '6px',
                                      boxShadow: '0 2px 4px rgba(2,136,209,0.2)',
                                      '&:hover': {
                                        boxShadow: '0 4px 6px rgba(2,136,209,0.25)',
                                      }
                                    }}
                                  >
                                    Time In
                                  </Button>
                                </span>
                              </Tooltip>
                              <Tooltip title="Manual Time Out">
                                <span>
                                  <Button
                                    size="small"
                                    variant="contained"
                                    color="warning"
                                    onClick={() => handleOpenConfirmDialog(user, 'timeout')}
                                    disabled={actionLoading || user.timeIn === 'N/A'}
                                    startIcon={<Logout sx={{ fontSize: 16 }} />}
                                    sx={{ 
                                      fontSize: '0.75rem',
                                      py: 0.5,
                                      textTransform: 'none',
                                      borderRadius: '6px',
                                      boxShadow: '0 2px 4px rgba(237,137,54,0.2)',
                                      '&:hover': {
                                        boxShadow: '0 4px 6px rgba(237,137,54,0.25)',
                                      }
                                    }}
                                  >
                                    Time Out
                                  </Button>
                                </span>
                              </Tooltip>
                            </Box>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        </Box>
      </Modal>
      
      {/* Confirmation Dialog */}
      <Dialog
        open={confirmDialogOpen}
        onClose={handleCloseConfirmDialog}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: '12px',
            boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
            bgcolor: darkMode ? '#1e1e1e' : 'background.paper',
          }
        }}
      >
        <DialogTitle sx={{ 
          borderBottom: '1px solid #E2E8F0', 
          bgcolor: darkMode ? '#333333' : '#F8FAFC',
          py: 2,
          px: 3
        }}>
          <Typography variant="h6" fontWeight="600">
            Confirm {confirmAction === 'timein' ? 'Time In' : 'Time Out'}
          </Typography>
        </DialogTitle>
        <DialogContent sx={{ py: 3, px: 3, mt: 1 }}>
          <Typography variant="body1">
            Are you sure you want to manually {confirmAction === 'timein' ? 'time in' : 'time out'} <Box component="span" fontWeight="600">{selectedUser?.firstName} {selectedUser?.lastName}</Box>?
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid #E2E8F0' }}>
          <Button 
            onClick={handleCloseConfirmDialog} 
            variant="outlined"
            sx={{
              borderColor: '#CBD5E1',
              color: '#64748B',
              '&:hover': {
                borderColor: '#94A3B8',
                bgcolor: '#F8FAFC'
              }
            }}
          >
            Cancel
          </Button>
          <Button 
            onClick={handleConfirmAction} 
            variant="contained" 
            color="primary"
            disabled={actionLoading}
            sx={{
              fontWeight: 600,
              boxShadow: '0 2px 4px rgba(2,136,209,0.2)',
              '&:hover': {
                boxShadow: '0 4px 6px rgba(2,136,209,0.25)',
              }
            }}
          >
            {actionLoading ? 'Processing...' : confirmAction === 'timein' ? 'Time In' : 'Time Out'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
});

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

// Helper to aggregate attendance by hour for analytics
const getAttendanceAnalyticsData = (attendees) => {
  const hourMap = {};
  attendees.forEach(att => {
    // Handle time in - check for different field names
    let timeIn = att.timeIn || att.timestamp;
    if (timeIn && timeIn !== 'N/A') {
      const date = new Date(timeIn);
      if (!isNaN(date.getTime())) {
        const hour = date.getHours();
        const label = `${hour.toString().padStart(2, '0')}:00`;
        if (!hourMap[label]) hourMap[label] = { time: label, timeInCount: 0, timeOutCount: 0 };
        hourMap[label].timeInCount++;
      }
    }
    
    // Handle time out - check for different field names and structures
    let timeOut = att.timeOut;
    if (att.hasTimedOut !== undefined && att.hasTimedOut) {
      timeOut = att.timeOutTimestamp;
    }
    
    if (timeOut && timeOut !== 'N/A') {
      const date = new Date(timeOut);
      if (!isNaN(date.getTime())) {
        const hour = date.getHours();
        const label = `${hour.toString().padStart(2, '0')}:00`;
        if (!hourMap[label]) hourMap[label] = { time: label, timeInCount: 0, timeOutCount: 0 };
        hourMap[label].timeOutCount++;
      }
    }
  });
  return Object.values(hourMap).sort((a, b) => a.time.localeCompare(b.time));
};

export default function Attendance() {
  const { darkMode } = useTheme();
  const { eventId } = useParams();
  const navigate = useNavigate();
  
  const [event, setEvent] = useState(null);
  const [department, setDepartment] = useState(null);
  const [attendees, setAttendees] = useState([]);
  const [filteredAttendees, setFilteredAttendees] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isAdmin, setIsAdmin] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [manageModalOpen, setManageModalOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  const [zoomImage, setZoomImage] = useState(null);
  const [exportLoading, setExportLoading] = useState(false);

  // Helper to fetch user details by userId
  const fetchUserById = async (userId) => {
    try {
      const response = await axios.get(getApiUrl(API_ENDPOINTS.GET_USER(userId)));
      return response.data;
    } catch (error) {
      console.error('Error fetching user by ID:', userId, error);
      return null;
    }
  };

  // Fetch event, department, and attendees (with profilePictureUrl)
  useEffect(() => {
    setIsAdmin(true);
    const fetchEventData = async () => {
      try {
        setLoading(true);
        // Fetch all events to find the specific one
        const eventsResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_ALL_EVENTS));
        const foundEvent = eventsResponse.data.find(e => e.eventId === eventId);
        if (!foundEvent) {
          throw new Error('Event not found');
        }
        setEvent(foundEvent);
        // Fetch department details
        if (foundEvent.departmentId) {
          const departmentsResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_DEPARTMENTS));
          const foundDepartment = departmentsResponse.data.find(d => d.departmentId === foundEvent.departmentId);
          setDepartment(foundDepartment);
        }
        // Fetch attendees
        const attendeesResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_ATTENDEES(eventId)));
        let attendeesData = attendeesResponse.data;
        // Fetch user details for each attendee to get profilePictureUrl
        const attendeesWithProfile = await Promise.all(attendeesData.map(async (att) => {
          if (att.profilePictureUrl) return att; // Already present
          const user = await fetchUserById(att.userId);
          return {
            ...att,
            profilePictureUrl: user?.profilePictureUrl || null,
            firstName: att.firstName || user?.firstName || '',
            lastName: att.lastName || user?.lastName || '',
            email: att.email || user?.email || '',
            department: att.department || user?.department?.name || 'N/A',
          };
        }));
        setAttendees(attendeesWithProfile);
        setFilteredAttendees(attendeesWithProfile);
        setLoading(false);
      } catch (error) {
        console.error('Error fetching data:', error);
        setError(error.message);
        setLoading(false);
      }
    };
    if (eventId) {
      fetchEventData();
    }
  }, [eventId]);

  // Handle search input change for main table
  const handleSearchChange = (event) => {
    const query = event.target.value.toLowerCase();
    setSearchQuery(query);
    
    if (query.trim() === '') {
      setFilteredAttendees(attendees);
    } else {
      const filtered = attendees.filter(
        attendee => 
          attendee.firstName.toLowerCase().includes(query) ||
          attendee.lastName.toLowerCase().includes(query) ||
          attendee.email.toLowerCase().includes(query) ||
          attendee.department.toLowerCase().includes(query)
      );
      setFilteredAttendees(filtered);
    }
  };
  
  // Open manage attendees modal
  const handleOpenManageModal = () => {
    setManageModalOpen(true);
  };
  
  // Close manage attendees modal
  const handleCloseManageModal = () => {
    setManageModalOpen(false);
  };

  // Handle time in/out from the modal component
  const handleModalTimeInOut = async (userId, action) => {
    if (action === 'timein') {
      await handleManualTimeIn(userId);
    } else if (action === 'timeout') {
      await handleManualTimeOut(userId);
    }
  };
  
  // Handle manual time in
  const handleManualTimeIn = async (userId) => {
    try {
      setActionLoading(true);
      const response = await axios.post(getApiUrl(API_ENDPOINTS.ATTENDANCE_MANUAL_TIME_IN(eventId, userId)));
      
      if (response.status === 200) {
        if (response.data.includes("already timed in")) {
          setSnackbar({
            open: true,
            message: 'User has already timed in for this event and received a certificate.',
            severity: 'warning'
          });
          return;
        }

        // Refresh attendee list
        const attendeesResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_ATTENDEES(eventId)));
        const newAttendees = attendeesResponse.data;
        setAttendees(newAttendees);
        
        // Update filtered attendees with search query applied
        if (searchQuery.trim() === '') {
          setFilteredAttendees(newAttendees);
        } else {
          const filtered = newAttendees.filter(
            attendee => 
              attendee.firstName.toLowerCase().includes(searchQuery) ||
              attendee.lastName.toLowerCase().includes(searchQuery) ||
              attendee.email.toLowerCase().includes(searchQuery) ||
              attendee.department.toLowerCase().includes(searchQuery)
          );
          setFilteredAttendees(filtered);
        }
        
        setSnackbar({
          open: true,
          message: 'Manual time-in recorded successfully',
          severity: 'success'
        });
      }
    } catch (error) {
      console.error('Error recording manual time-in:', error);
      setSnackbar({
        open: true,
        message: 'Failed to record manual time-in',
        severity: 'error'
      });
    } finally {
      setActionLoading(false);
    }
  };
  
  // Handle manual time out
  const handleManualTimeOut = async (userId) => {
    try {
      setActionLoading(true);
      const response = await axios.post(getApiUrl(API_ENDPOINTS.ATTENDANCE_MANUAL_TIME_OUT(eventId, userId)));
      
      if (response.status === 200) {
        // Refresh attendee list
        const attendeesResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_ATTENDEES(eventId)));
        const newAttendees = attendeesResponse.data;
        setAttendees(newAttendees);
        
        // Update filtered attendees with search query applied
        if (searchQuery.trim() === '') {
          setFilteredAttendees(newAttendees);
        } else {
          const filtered = newAttendees.filter(
            attendee => 
              attendee.firstName.toLowerCase().includes(searchQuery) ||
              attendee.lastName.toLowerCase().includes(searchQuery) ||
              attendee.email.toLowerCase().includes(searchQuery) ||
              attendee.department.toLowerCase().includes(searchQuery)
          );
          setFilteredAttendees(filtered);
        }
        
        setSnackbar({
          open: true,
          message: 'Manual time-out recorded successfully',
          severity: 'success'
        });
      }
    } catch (error) {
      console.error('Error recording manual time-out:', error);
      setSnackbar({
        open: true,
        message: 'Failed to record manual time-out',
        severity: 'error'
      });
    } finally {
      setActionLoading(false);
    }
  };
  
  // Close snackbar
  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  // Format timestamp to readable date and time
  const formatTimestamp = (timestamp) => {
    if (!timestamp || timestamp === 'N/A') return 'N/A';
    
    try {
      // Handle ISO format (old structure)
      if (timestamp.includes('T')) {
        const date = new Date(timestamp);
        if (!isNaN(date.getTime())) {
          return date.toLocaleString('en-PH', {
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
      
      // Handle "yyyy-MM-dd HH:mm:ss" format (new structure) - treat as Philippines time
      if (timestamp.includes('-') && timestamp.includes(':')) {
        const [datePart, timePart] = timestamp.split(' ');
        const [year, month, day] = datePart.split('-');
        const [hour, minute, second] = timePart.split(':');
        
        // Create date as if it's Philippines time (since backend now stores in Philippines timezone)
        const date = new Date(year, month - 1, day, hour, minute, second);
        if (!isNaN(date.getTime())) {
          return date.toLocaleString('en-PH', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            hour12: true
          });
        }
      }
      
      // Try direct parsing as a fallback
      const date = new Date(timestamp);
      if (!isNaN(date.getTime())) {
        return date.toLocaleString('en-PH', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: 'numeric',
          minute: '2-digit',
          hour12: true,
          timeZone: 'Asia/Manila'
        });
      }
      
      return timestamp; // Return original if all parsing attempts fail
    } catch (e) {
      console.error('Error formatting timestamp:', e);
      return timestamp;
    }
  };

  // Format event date
  const formatEventDate = (date) => {
    if (!date) return 'N/A';
    
    try {
      // Handle different date formats
      let dateObj;
      
      if (typeof date === 'string') {
        dateObj = new Date(date);
      } else if (typeof date === 'object') {
        if (date.seconds && date.nanoseconds) {
          dateObj = new Date(date.seconds * 1000 + date.nanoseconds / 1000000);
        } else if (date.toDate && typeof date.toDate === 'function') {
          dateObj = date.toDate();
        } else if (date._seconds && date._nanoseconds) {
          dateObj = new Date(date._seconds * 1000 + date._nanoseconds / 1000000);
        } else {
          return 'Invalid Date';
        }
      } else {
        return 'Invalid Date';
      }
      
      if (isNaN(dateObj.getTime())) {
        return 'Invalid Date';
      }
      
      return dateObj.toLocaleString('en-PH', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        timeZone: 'Asia/Manila'
      });
    } catch (e) {
      console.error('Error formatting date:', e);
      return 'Error';
    }
  };

  // Format timeout status
  const formatTimeoutStatus = (timeOut, manualEntry = 'false') => {
    if (!timeOut || timeOut === 'N/A') {
      return (
        <Chip 
          label="Not yet Timed out"
          size="small"
          sx={{
            backgroundColor: '#FEF3C7',
            color: '#D97706',
            fontWeight: 500,
            fontSize: '0.75rem',
            height: 24,
            borderRadius: '4px'
          }}
        />
      );
    }
    
    const formattedTime = formatTimestamp(timeOut);
    
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {formattedTime}
        {manualEntry === 'true' && (
          <Tooltip title="Manual entry">
            <CheckCircle 
              sx={{ 
                color: '#10B981', 
                fontSize: 16 
              }} 
            />
          </Tooltip>
        )}
      </Box>
    );
  };

  // Format timein status with manual entry indicator
  const formatTimeInStatus = (timeIn, manualEntry = 'false') => {
    const formattedTime = formatTimestamp(timeIn);
    
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {formattedTime}
        {manualEntry === 'true' && (
          <Tooltip title="Manual entry">
            <CheckCircle 
              sx={{ 
                color: '#10B981', 
                fontSize: 16 
              }} 
            />
          </Tooltip>
        )}
      </Box>
    );
  };

  // Get status color
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

  const handleNavigateBack = () => {
    navigate('/dashboard');
  };

  const formatAttendanceData = (attendee) => {
    const baseData = {
      userId: attendee.userId,
      firstName: attendee.firstName || '',
      lastName: attendee.lastName || '',
      email: attendee.email || '',
      department: attendee.department || 'N/A',
      selfieUrl: attendee.selfieUrl || null,
      profilePictureUrl: attendee.profilePictureUrl || null,
      type: attendee.type || 'event_time_in',
      manualEntry: String(attendee.manualEntry).toLowerCase() === 'true'
    };
    
    // Handle old structure (has timeIn in ISO format)
    if (attendee.timeIn && attendee.timeIn.includes('T')) {
      return {
        ...baseData,
        timeIn: attendee.timeIn,
        timeOut: attendee.timeOut || 'N/A'
      };
    }
    
    // Handle new structure with hasTimedOut, timeOutTimestamp, and timestamp
    if (attendee.hasTimedOut !== undefined) {
      return {
        ...baseData,
        timeIn: attendee.timestamp || attendee.timeIn || '',
        timeOut: attendee.hasTimedOut ? (attendee.timeOutTimestamp || 'N/A') : 'N/A'
      };
    }
    
    // Handle other structures
    return {
      ...baseData,
      timeIn: attendee.timeIn || attendee.timestamp || '',
      timeOut: attendee.timeOut || 'N/A'
    };
  };

  const handleExportData = async () => {
    try {
      setExportLoading(true);

      // Create workbook
      const wb = XLSX.utils.book_new();

      // Export attendees list
      const attendeesData = filteredAttendees.map(attendee => {
        const formattedAttendee = formatAttendanceData(attendee);
        return {
          'Name': `${formattedAttendee.firstName} ${formattedAttendee.lastName}`,
          'Email': formattedAttendee.email,
          'Department': formattedAttendee.department,
          'Time In': formatTimestamp(formattedAttendee.timeIn),
          'Time Out': formatTimestamp(formattedAttendee.timeOut),
          'Entry Type': formattedAttendee.manualEntry ? 'Manual Entry' : 'QR Scan'
        };
      });

      const ws = XLSX.utils.json_to_sheet(attendeesData);
      XLSX.utils.book_append_sheet(wb, ws, 'Attendees');

      // Export analytics data
      const analyticsData = getAttendanceAnalyticsData(attendees);
      const analyticsSheet = XLSX.utils.json_to_sheet(analyticsData.map(item => ({
        'Time': item.time,
        'Time In Count': item.timeInCount,
        'Time Out Count': item.timeOutCount
      })));
      XLSX.utils.book_append_sheet(wb, analyticsSheet, 'Analytics');

      // Capture the analytics graph
      const analyticsElement = document.querySelector('#attendance-analytics-chart');
      if (analyticsElement) {
        const canvas = await html2canvas(analyticsElement);
        const imgData = canvas.toDataURL('image/png');
        
        // Add the graph as a new sheet
        const imgWs = XLSX.utils.aoa_to_sheet([[{
          t: 's',
          v: 'Analytics Graph',
          s: { font: { bold: true, sz: 14 } }
        }]]);
        
        const imgId = wb.addImage({
          base64: imgData,
          extension: 'png',
        });
        
        const col = 'A';
        const row = 2;
        imgWs['!images'] = [{
          name: 'analytics.png',
          data: imgData,
          position: {
            type: 'twoCellAnchor',
            from: { col: 0, row: row },
            to: { col: 10, row: row + 20 }
          }
        }];
        
        XLSX.utils.book_append_sheet(wb, imgWs, 'Analytics Graph');
      }

      // Generate filename
      const filename = `${event?.eventName || 'Event'}_Attendance_${new Date().toISOString().split('T')[0]}.xlsx`;

      // Save the file
      XLSX.writeFile(wb, filename);
    } catch (error) {
      console.error('Error exporting data:', error);
      setSnackbar({
        open: true,
        message: 'Failed to export data',
        severity: 'error'
      });
    } finally {
      setExportLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100%', overflow: 'hidden' }}>
     
      {/* Header */}
      <Box sx={{ 
        py: 1.5, 
        px: 3,
        bgcolor: darkMode ? '#1e1e1e' : 'white', 
        borderBottom: '1px solid',
        borderColor: darkMode ? '#333333' : '#EAECF0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        boxShadow: darkMode ? '0 1px 2px rgba(0,0,0,0.2)' : '0 1px 2px rgba(0,0,0,0.05)'
      }}>

        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <IconButton onClick={handleNavigateBack} sx={{ mr: 1 }}>
            <ArrowBack sx={{ color: '#64748B' }} />
          </IconButton>
          <Breadcrumbs separator="›" aria-label="breadcrumb">
            <Link
              underline="hover"
              color="#64748B"
              sx={{ cursor: 'pointer', fontWeight: 500, fontSize: 14 }}
              onClick={handleNavigateBack}
            >
              Dashboard
            </Link>
            <Typography color="#0288d1" fontWeight={600} fontSize={14}>
              Attendance
            </Typography>
          </Breadcrumbs>
        </Box>
      </Box>

      {/* Main Content */}
      <Box sx={{ 
        p: 3, 
        flex: 1, 
        overflow: 'auto', 
        bgcolor: darkMode ? '#121212' : '#FFFFFF' 
      }}>
         <Typography variant="h4" fontWeight="700" color={darkMode ? '#f5f5f5' : '#1E293B'} sx={{ mb: 4, textAlign: 'center' }}>
  Event Attendance Details
</Typography>

{/* Event Information */}
<Grid container spacing={4} sx={{ mb: 5 }}>
  <Grid item xs={12} lg={7}>
    <Card sx={{ 
      boxShadow: darkMode ? '0 8px 32px rgba(0,0,0,0.3)' : '0 8px 32px rgba(0,0,0,0.08)',
      border: '1px solid',
      borderColor: darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.06)', 
      height: '100%',
      borderRadius: '16px',
      bgcolor: darkMode ? 'rgba(30,30,30,0.8)' : 'rgba(255,255,255,0.9)',
      backdropFilter: 'blur(10px)',
      transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
      position: 'relative',
      overflow: 'hidden',
      '&:hover': {
        transform: 'translateY(-4px)',
        boxShadow: darkMode ? '0 12px 48px rgba(0,0,0,0.4)' : '0 12px 48px rgba(0,0,0,0.12)'
      },
      '&::before': {
        content: '""',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        height: '4px',
        background: 'linear-gradient(90deg, #0288d1, #90caf9)',
      }
    }}>
      <CardContent sx={{ p: 4 }}>
        <Typography variant="h5" fontWeight="700" color={darkMode ? '#f5f5f5' : '#1E293B'} sx={{ 
          mb: 3, 
          display: 'flex', 
          alignItems: 'center', 
          gap: 1.5,
          letterSpacing: '-0.025em'
        }}>
          <Box sx={{ 
            p: 1.5, 
            borderRadius: '12px', 
            bgcolor: darkMode ? 'rgba(144,202,249,0.15)' : 'rgba(2,136,209,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Event sx={{ color: darkMode ? '#90caf9' : '#0288d1', fontSize: 24 }} />
          </Box>
          Event Information
        </Typography>
        
        <Grid container spacing={3}>
          <Grid item xs={6}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              height: '100%'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Event Name
              </Typography>
              <Typography variant="h6" fontWeight="600" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B', mt: 0.5 }}>
                {event?.eventName || 'N/A'}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              height: '100%'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Event ID
              </Typography>
              <Typography variant="body1" fontWeight="600" sx={{ color: darkMode ? '#90caf9' : '#0288d1', mt: 0.5, fontFamily: 'monospace' }}>
                {event?.eventId || 'N/A'}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              height: '100%'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Duration
              </Typography>
              <Typography variant="body1" fontWeight="600" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B', display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                <AccessTime sx={{ fontSize: 18, color: darkMode ? '#90caf9' : '#64748B' }} />
                {event?.duration || 'N/A'}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              height: '100%'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Date
              </Typography>
              <Typography variant="body1" fontWeight="600" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B', display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                <CalendarToday sx={{ fontSize: 18, color: darkMode ? '#90caf9' : '#64748B' }} />
                {formatEventDate(event?.date)}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={6}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              height: '100%'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Venue
              </Typography>
              <Typography variant="body1" fontWeight="600" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B', mt: 0.5 }}>
                {event?.venue || 'N/A'}
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={12}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              textAlign: 'center'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', mb: 1, display: 'block' }}>
                Status
              </Typography>
              <Chip 
                label={event?.status || 'Unknown'}
                size="medium"
                sx={{
                  backgroundColor: `${getStatusColor(event?.status)}15`,
                  color: getStatusColor(event?.status),
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  height: 32,
                  borderRadius: '8px',
                  border: `1px solid ${getStatusColor(event?.status)}30`,
                  px: 1
                }}
              />
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  </Grid>
  
  <Grid item xs={12} lg={5}>
    <Card sx={{ 
      boxShadow: darkMode ? '0 8px 32px rgba(0,0,0,0.3)' : '0 8px 32px rgba(0,0,0,0.08)',
      border: '1px solid',
      borderColor: darkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.06)', 
      height: '100%',
      borderRadius: '16px',
      bgcolor: darkMode ? 'rgba(30,30,30,0.8)' : 'rgba(255,255,255,0.9)',
      backdropFilter: 'blur(10px)',
      transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
      position: 'relative',
      overflow: 'hidden',
      '&:hover': {
        transform: 'translateY(-4px)',
        boxShadow: darkMode ? '0 12px 48px rgba(0,0,0,0.4)' : '0 12px 48px rgba(0,0,0,0.12)'
      },
      '&::before': {
        content: '""',
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        height: '4px',
        background: 'linear-gradient(90deg, #0288d1, #90caf9)',
      }
    }}>
      <CardContent sx={{ p: 4 }}>
        <Typography variant="h5" fontWeight="700" color={darkMode ? '#f5f5f5' : '#1E293B'} sx={{ 
          mb: 3, 
          display: 'flex', 
          alignItems: 'center', 
          gap: 1.5,
          letterSpacing: '-0.025em'
        }}>
          <Box sx={{ 
            p: 1.5, 
            borderRadius: '12px', 
            bgcolor: darkMode ? 'rgba(144,202,249,0.15)' : 'rgba(2,136,209,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <School sx={{ color: darkMode ? '#90caf9' : '#0288d1', fontSize: 24 }} />
          </Box>
          Department Host
        </Typography>
        
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.06)',
              transition: 'all 0.2s ease',
              textAlign: 'center'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Department Name
              </Typography>
              <Typography variant="h6" fontWeight="700" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B', mt: 0.5 }}>
                {department?.name || 'N/A'}
              </Typography>
            </Box>
          </Grid>
          
          <Grid item xs={6}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              height: '100%',
              textAlign: 'center'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Abbreviation
              </Typography>
              <Typography variant="body1" fontWeight="600" sx={{ color: darkMode ? '#90caf9' : '#0288d1', fontFamily: 'monospace', fontSize: '1.1rem', mt: 0.5 }}>
                {department?.abbreviation || 'N/A'}
              </Typography>
            </Box>
          </Grid>
          
   {/*        <Grid item xs={6}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              height: '100%',
              textAlign: 'center'
            }}>
             <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Faculty Count
              </Typography>
              <Typography variant="body1" fontWeight="600" sx={{ color: darkMode ? '#f5f5f5' : '#1E293B', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.8, mt: 0.5 }}>
                <Group sx={{ fontSize: 18, color: darkMode ? '#90caf9' : '#64748B' }} />
                {department?.numberOfFaculty || 'N/A'}
              </Typography>
            </Box>
          </Grid>*/}
          
          <Grid item xs={12}>
            <Box sx={{ 
              p: 2.5, 
              borderRadius: '12px', 
              bgcolor: darkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)',
              border: '1px solid',
              borderColor: darkMode ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.04)',
              transition: 'all 0.2s ease',
              textAlign: 'center'
            }}>
              <Typography variant="caption" sx={{ color: '#64748B', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', mb: 1.5, display: 'block' }}>
                Programs Offered
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, justifyContent: 'center' }}>
                {department?.offeredPrograms?.map((program, index) => (
                  <Chip 
                    key={index}
                    label={program}
                    size="medium"
                    sx={{
                      backgroundColor: darkMode ? 'rgba(59,130,246,0.15)' : '#EFF6FF',
                      color: darkMode ? '#93C5FD' : '#3B82F6',
                      fontWeight: 600,
                      fontSize: '0.8rem',
                      height: 32,
                      borderRadius: '8px',
                      border: darkMode ? '1px solid rgba(59,130,246,0.3)' : '1px solid rgba(59,130,246,0.2)',
                      px: 1.5,
                      transition: 'all 0.2s ease',
                      '&:hover': {
                        transform: 'translateY(-1px)',
                        boxShadow: darkMode ? '0 4px 8px rgba(59,130,246,0.2)' : '0 4px 8px rgba(59,130,246,0.15)'
                      }
                    }}
                  />
                ))}
                {(!department?.offeredPrograms || department.offeredPrograms.length === 0) && (
                  <Typography variant="body2" sx={{ color: '#64748B', fontStyle: 'italic' }}>
                    No programs specified
                  </Typography>
                )}
              </Box>
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  </Grid>
</Grid>
          {/*  <Button
                    variant="contained"
                    onClick={handleOpenManageModal}
                    startIcon={<ManageAccounts />}
                    sx={{
                      backgroundColor: darkMode ? '#90caf9' : '#0288d1',
                      color: darkMode ? '#1e1e1e' : '#ffffff',
                      '&:hover': {
                        backgroundColor: darkMode ? '#64b5f6' : '#0277bd'
                      },
                      borderRadius: '8px',
                      whiteSpace: 'nowrap',
                      boxShadow: darkMode ? '0 4px 8px rgba(144,202,249,0.2)' : '0 4px 8px rgba(2,136,209,0.2)',
                      transition: 'all 0.2s ease-in-out',
                      fontWeight: 600,
                      fontSize: '0.875rem',
                      textTransform: 'none',
                      padding: '8px 16px',
                      height: '40px',
                      '&:active': {
                        transform: 'scale(0.98)'
                      }
                    }}
                  >
                    Manage Attendance
                  </Button>
                  */}
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
            <CircularProgress size={32} sx={{ color: '#0288d1' }} />
          </Box>
        ) : error ? (
          <Box sx={{ 
            p: 3, 
            textAlign: 'center', 
            color: '#EF4444',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 2
          }}>
            <Typography variant="h6">No Participant in the List</Typography>
            <Button 
              variant="contained" 
              onClick={handleNavigateBack}
              startIcon={<ArrowBack />}
              sx={{
                backgroundColor: '#0288d1',
                '&:hover': {
                  backgroundColor: '#0277bd'
                }
              }}
            >
              Back to Dashboard
            </Button>
          </Box>
        ) : (
          <>
           {/* Attendance Analytics */}
    <AttendanceAnalytics
      data={getAttendanceAnalyticsData(attendees)}
      title="Time-In/Out Distribution for This Event"
    />
            {/* Attendees Table */}
            <Box sx={{ mb: 2 }}>
              <Box sx={{ 
                mb: 2, 
                display: 'flex', 
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 2
              }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography variant="h6" fontWeight="600" color={darkMode ? '#f5f5f5' : '#1E293B'}>
                    <Group sx={{ color: darkMode ? '#90caf9' : '#0288d1', mr: 1, verticalAlign: 'middle' }} />
                    Faculty Attendees
                  </Typography>
                  <Chip 
                    label={filteredAttendees.length}
                    size="small"
                    sx={{
                      backgroundColor: '#EFF6FF',
                      color: '#3B82F6',
                      fontWeight: 600,
                      ml: 1
                    }}
                  />
                </Box>
                
                <Box sx={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: 2, 
                  justifyContent: 'flex-end',
                  width: { xs: '100%', md: 'auto' }
                }}>
                  <TextField
                    placeholder="Search participants..."
                    variant="outlined"
                    size="small"
                    value={searchQuery}
                    onChange={handleSearchChange}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <Search sx={{ color: darkMode ? '#90caf9' : '#64748B', fontSize: 20 }} />
                        </InputAdornment>
                      ),
                    }}
                    sx={{
                      width: { xs: '100%', sm: '250px' },
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '8px',
                        backgroundColor: darkMode ? '#333333' : '#F8FAFC'
                      }
                    }}
                  />
                  
                  <Button
                    variant="contained"
                    onClick={handleExportData}
                    disabled={exportLoading || filteredAttendees.length === 0}
                    startIcon={exportLoading ? <CircularProgress size={20} color="inherit" /> : <FileDownload />}
                    sx={{
                      backgroundColor: darkMode ? '#90caf9' : '#0288d1',
                      color: darkMode ? '#1e1e1e' : '#ffffff',
                      '&:hover': {
                        backgroundColor: darkMode ? '#64b5f6' : '#0277bd'
                      },
                      borderRadius: '8px',
                      whiteSpace: 'nowrap',
                      boxShadow: darkMode ? '0 4px 8px rgba(144,202,249,0.2)' : '0 4px 8px rgba(2,136,209,0.2)',
                      transition: 'all 0.2s ease-in-out',
                      fontWeight: 600,
                      fontSize: '0.875rem',
                      textTransform: 'none',
                      padding: '8px 16px',
                      height: '40px',
                      '&:active': {
                        transform: 'scale(0.98)'
                      }
                    }}
                  >
                    {exportLoading ? 'Exporting...' : 'Export Data'}
                  </Button>
                </Box>
              </Box>
              
              <TableContainer 
                component={Paper} 
                sx={{ 
                  boxShadow: darkMode ? '0 4px 12px rgba(0,0,0,0.2)' : '0 4px 12px rgba(0,0,0,0.08)', 
                  border: '1px solid',
                  borderColor: darkMode ? '#333333' : '#E2E8F0',
                  borderRadius: '12px',
                  overflow: 'hidden',
                  maxHeight: 'calc(100vh - 400px)',
                  bgcolor: darkMode ? '#1e1e1e' : 'background.paper',
                  '& .MuiTableCell-root': {
                    borderColor: darkMode ? '#333333' : 'inherit',
                    color: darkMode ? '#f5f5f5' : 'inherit'
                  },
                  '& .MuiTableHead-root .MuiTableCell-root': {
                    bgcolor: darkMode ? '#333333' : '#F8FAFC',
                    color: darkMode ? '#90caf9' : '#475569'
                  }
                }}
              >
                {filteredAttendees.length === 0 ? (
                  <Box sx={{ p: 3, textAlign: 'center', color: '#64748B' }}>
                    {attendees.length === 0 ? (
                      <Typography>No attendees found for this event</Typography>
                    ) : (
                      <Typography>No attendees match your search</Typography>
                    )}
                  </Box>
                ) : (
                  <Table stickyHeader>
                    <TableHead>
                      <TableRow>
                        <TableCell>Photo/Selfie</TableCell>
                        <TableCell>Name</TableCell>
                        <TableCell>Email</TableCell>
                        <TableCell>Department</TableCell>
                        <TableCell>Time In</TableCell>
                       <TableCell>Time Out</TableCell>
                         {/*<TableCell>Status</TableCell>*/}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {filteredAttendees.map((attendee) => {
                        const formattedAttendee = formatAttendanceData(attendee);
                        
                        return (
                          <TableRow key={formattedAttendee.userId}>
                            <TableCell>
                              {formattedAttendee.profilePictureUrl ? (
                                <Avatar
                                  src={formattedAttendee.profilePictureUrl}
                                  alt={formattedAttendee.firstName}
                                  sx={{ width: 40, height: 40, cursor: 'pointer' }}
                                />
                              ) : formattedAttendee.selfieUrl ? (
                                <Box
                                  component="img"
                                  src={formattedAttendee.selfieUrl}
                                  alt="Verification selfie"
                                  sx={{
                                    width: 40,
                                    height: 40,
                                    borderRadius: '50%',
                                    objectFit: 'cover',
                                    cursor: 'pointer',
                                    '&:hover': {
                                      opacity: 0.8,
                                      transform: 'scale(1.1)',
                                      transition: 'all 0.2s ease-in-out'
                                    }
                                  }}
                                  onClick={() => setZoomImage(formattedAttendee.selfieUrl)}
                                />
                              ) : (
                                <Avatar sx={{ width: 40, height: 40 }}>
                                  {formattedAttendee.firstName?.charAt(0)}
                                </Avatar>
                              )}
                            </TableCell>
                            <TableCell>
                              {formattedAttendee.firstName} {formattedAttendee.lastName}
                            </TableCell>
                            <TableCell>{formattedAttendee.email}</TableCell>
                            <TableCell>{formattedAttendee.department}</TableCell>
                            <TableCell>
                              <Tooltip title="Time In" arrow>
                                <Chip 
                                  label={formatTimestamp(formattedAttendee.timeIn)}
                                  color="success"
                                  size="small"
                                  variant="outlined"
                                  icon={<AccessTime sx={{ fontSize: 16 }} />}
                                />
                              </Tooltip>
                            </TableCell>
                           <TableCell>
                              {formattedAttendee.timeOut !== 'N/A' ? (
                                <Tooltip title="Time Out" arrow>
                                  <Chip 
                                    label={formatTimestamp(formattedAttendee.timeOut)}
                                    color="error"
                                    size="small"
                                    variant="outlined"
                                    icon={<AccessTime sx={{ fontSize: 16 }} />}
                                  />
                                </Tooltip>
                              ) : (
                                <Chip 
                                  label="Not yet timed out"
                                  color="warning"
                                  size="small"
                                  variant="outlined"
                                />
                              )}
                            </TableCell>
                            {/*
                            <TableCell>
                              <Chip 
                                label={formattedAttendee.manualEntry ? "Manual Entry" : "QR Scan"}
                                color={formattedAttendee.manualEntry ? "warning" : "info"}
                                size="small"
                                variant="outlined"
                              />
                            </TableCell>*/}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </TableContainer>
            </Box>
          </>
        )}
      </Box>
      
      {/* Notification Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity={snackbar.severity} 
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>

      {/* Attendance Modal - Separate component with isolated state */}
       <AttendanceModal
      open={manageModalOpen}
      onClose={handleCloseManageModal}
      attendees={attendees}
      onTimeInOut={handleModalTimeInOut}
      actionLoading={actionLoading}
      formatTimeInStatus={formatTimeInStatus}
      formatTimeoutStatus={formatTimeoutStatus}
      eventId={eventId} // Pass the eventId to the modal
    />

    {/* Image Zoom Modal */}
    {zoomImage && (
      <ImageZoomModal
        imageUrl={zoomImage}
        onClose={() => setZoomImage(null)}
      />
    )}

    
    </Box>
    
  );
}