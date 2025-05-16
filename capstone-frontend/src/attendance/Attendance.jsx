import { useState, useEffect, memo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
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
  DialogActions
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
  Close
} from '@mui/icons-material';
import './attendance.css';

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
      const response = await axios.get('http://localhost:8080/api/user/getAll');
      
      // Create a map of existing attendees by userId for quick lookup
      const attendeeMap = new Map();
      attendees.forEach(attendee => {
        attendeeMap.set(attendee.userId, attendee);
      });
      
      // Merge user data with attendance data if available
      const usersWithAttendance = response.data.map(user => {
        const attendeeData = attendeeMap.get(user.userId);
        return {
          ...user,
          // Include attendance data if user is already an attendee
          timeIn: attendeeData?.timeIn || 'N/A',
          timeOut: attendeeData?.timeOut || 'N/A',
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
          bgcolor: 'background.paper',
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
            borderBottom: '1px solid #E2E8F0',
            bgcolor: '#F8FAFC'
          }}>
            <Typography variant="h6" fontWeight="600" sx={{ display: 'flex', alignItems: 'center', gap: 1, color: '#1E293B' }}>
              <ManageAccounts sx={{ color: '#0288d1' }} /> 
              Manage Event Attendance
            </Typography>
            <IconButton 
              onClick={onClose} 
              sx={{ 
                bgcolor: '#F1F5F9', 
                '&:hover': { bgcolor: '#E2E8F0' },
                transition: 'all 0.2s ease',
              }}
            >
              <Close />
            </IconButton>
          </Box>
          
          <Box sx={{ p: 2.5, borderBottom: '1px solid #E2E8F0', bgcolor: '#FFFFFF' }}>
            <TextField
              placeholder="Search faculty members..."
              variant="outlined"
              size="small"
              value={modalSearchQuery}
              onChange={handleModalSearchChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search sx={{ color: '#64748B', fontSize: 20 }} />
                  </InputAdornment>
                ),
              }}
              sx={{
                width: '100%',
                maxWidth: '350px',
                '& .MuiOutlinedInput-root': {
                  borderRadius: '8px',
                  backgroundColor: '#F8FAFC',
                  '&:hover': {
                    '& > fieldset': {
                      borderColor: '#0288d1',
                    }
                  },
                  '&.Mui-focused': {
                    '& > fieldset': {
                      borderColor: '#0288d1',
                      borderWidth: '1px',
                    }
                  }
                }
              }}
            />
          </Box>

          <Box sx={{ 
            flex: 1, 
            overflowY: 'auto',
            p: 0
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
          }
        }}
      >
        <DialogTitle sx={{ 
          borderBottom: '1px solid #E2E8F0', 
          bgcolor: '#F8FAFC',
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

export default function Attendance() {
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

  useEffect(() => {
    // Remove admin role check since this is already an admin dashboard
    setIsAdmin(true);
    
    const fetchEventData = async () => {
      try {
        setLoading(true);
        
        // Fetch all events to find the specific one
        const eventsResponse = await axios.get('http://localhost:8080/api/events/getAll');
        const foundEvent = eventsResponse.data.find(e => e.eventId === eventId);
        
        if (!foundEvent) {
          throw new Error('Event not found');
        }
        
        setEvent(foundEvent);
        
        // Fetch department details
        if (foundEvent.departmentId) {
          const departmentsResponse = await axios.get('http://localhost:8080/api/departments');
          const foundDepartment = departmentsResponse.data.find(d => d.departmentId === foundEvent.departmentId);
          setDepartment(foundDepartment);
        }
        
        // Fetch attendees
        const attendeesResponse = await axios.get(`http://localhost:8080/api/attendance/${eventId}/attendees`);
        setAttendees(attendeesResponse.data);
        setFilteredAttendees(attendeesResponse.data);
        
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
      const response = await axios.post(`http://localhost:8080/api/attendance/${eventId}/${userId}/manual/timein`);
      
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
        const attendeesResponse = await axios.get(`http://localhost:8080/api/attendance/${eventId}/attendees`);
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
      const response = await axios.post(`http://localhost:8080/api/attendance/${eventId}/${userId}/manual/timeout`);
      
      if (response.status === 200) {
        // Refresh attendee list
        const attendeesResponse = await axios.get(`http://localhost:8080/api/attendance/${eventId}/attendees`);
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
    if (!timestamp) return 'N/A';
    
    try {
      // Handle different timestamp formats
      let dateObj;
      
      if (typeof timestamp === 'string') {
        dateObj = new Date(timestamp);
      } else if (typeof timestamp === 'object') {
        if (timestamp.seconds && timestamp.nanoseconds) {
          dateObj = new Date(timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000);
        } else if (timestamp.toDate && typeof timestamp.toDate === 'function') {
          dateObj = timestamp.toDate();
        } else if (timestamp._seconds && timestamp._nanoseconds) {
          dateObj = new Date(timestamp._seconds * 1000 + timestamp._nanoseconds / 1000000);
        } else {
          return 'N/A';
        }
      } else {
        return 'N/A';
      }
      
      if (isNaN(dateObj.getTime())) {
        return 'N/A';
      }
      
      return dateObj.toLocaleString('en-PH', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
        timeZone: 'Asia/Manila'
      });
    } catch (e) {
      console.error('Error formatting timestamp:', e);
      return 'Error';
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

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100%', overflow: 'hidden' }}>
     
      {/* Header */}
      <Box sx={{ 
        py: 1.5, 
        px: 3,
        bgcolor: 'white', 
        borderBottom: '1px solid #EAECF0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
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
        bgcolor: '#FFFFFF' 
      }}>
         <Typography variant="h5" fontWeight="600" color="#1E293B" sx={{ mb: 3 }}>
              Event Attendance Details
            </Typography>
            
            {/* Event Information */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
              <Grid item xs={12} md={6}>
                <Card sx={{ 
                  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                  border: '1px solid #E2E8F0', 
                  height: '100%',
                  borderRadius: '12px',
                  transition: 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
                  '&:hover': {
                    transform: 'translateY(-2px)',
                    boxShadow: '0 6px 12px rgba(0,0,0,0.1)'
                  }
                }}>
                  <CardContent>
                    <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Event sx={{ color: '#0288d1' }} />
                      Event Information
                    </Typography>
                    
                    <Grid container spacing={2}>
                      <Grid item xs={6}>
                        <Typography variant="caption" color="#64748B">Event Name</Typography>
                        <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>
                          {event?.eventName || 'N/A'}
                        </Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="caption" color="#64748B">Event ID</Typography>
                        <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>
                          {event?.eventId || 'N/A'}
                        </Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="caption" color="#64748B">Duration</Typography>
                        <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B', display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <AccessTime sx={{ fontSize: 16, color: '#64748B' }} />
                          {event?.duration || 'N/A'}
                        </Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="caption" color="#64748B">Date</Typography>
                        <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B', display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <CalendarToday sx={{ fontSize: 16, color: '#64748B' }} />
                          {formatEventDate(event?.date)}
                        </Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="caption" color="#64748B">Status</Typography>
                        <Box sx={{ mt: 0.5 }}>
                          <Chip 
                            label={event?.status || 'Unknown'}
                            size="small"
                            sx={{
                              backgroundColor: `${getStatusColor(event?.status)}20`,
                              color: getStatusColor(event?.status),
                              fontWeight: 500,
                              fontSize: '0.75rem',
                              height: 24,
                              borderRadius: '4px'
                            }}
                          />
                        </Box>
                      </Grid>
                    </Grid>
                  </CardContent>
                </Card>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Card sx={{ 
                  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                  border: '1px solid #E2E8F0', 
                  height: '100%',
                  borderRadius: '12px',
                  transition: 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
                  '&:hover': {
                    transform: 'translateY(-2px)',
                    boxShadow: '0 6px 12px rgba(0,0,0,0.1)'
                  }
                }}>
                  <CardContent>
                    <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                      <School sx={{ color: '#0288d1' }} />
                      Hosted by Department
                    </Typography>
                    
                    <Grid container spacing={2}>
                      <Grid item xs={12}>
                        <Typography variant="caption" color="#64748B">Department Name</Typography>
                        <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>
                          {department?.name || 'N/A'}
                        </Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="caption" color="#64748B">Abbreviation</Typography>
                        <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B' }}>
                          {department?.abbreviation || 'N/A'}
                        </Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="caption" color="#64748B">Number of Faculty</Typography>
                        <Typography variant="body1" fontWeight="500" sx={{ color: '#1E293B', display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <Group sx={{ fontSize: 16, color: '#64748B' }} />
                          {department?.numberOfFaculty || 'N/A'}
                        </Typography>
                      </Grid>
                      <Grid item xs={12}>
                        <Typography variant="caption" color="#64748B">Programs Offered</Typography>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 0.5 }}>
                          {department?.offeredPrograms?.map((program, index) => (
                            <Chip 
                              key={index}
                              label={program}
                              size="small"
                              sx={{
                                backgroundColor: '#EFF6FF',
                                color: '#3B82F6',
                                fontWeight: 500,
                                fontSize: '0.75rem',
                                height: 24,
                                borderRadius: '4px'
                              }}
                            />
                          ))}
                          {(!department?.offeredPrograms || department.offeredPrograms.length === 0) && (
                            <Typography variant="body2" sx={{ color: '#64748B' }}>None specified</Typography>
                          )}
                        </Box>
                      </Grid>
                    </Grid>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
            <Button
                    variant="contained"
                    onClick={handleOpenManageModal}
                    startIcon={<ManageAccounts />}
                    sx={{
                      backgroundColor: '#0288d1',
                      '&:hover': {
                        backgroundColor: '#0277bd'
                      },
                      borderRadius: '8px',
                      whiteSpace: 'nowrap',
                      boxShadow: '0 4px 8px rgba(2,136,209,0.2)',
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
                  <Typography variant="h6" fontWeight="600" color="#1E293B">
                    <Group sx={{ color: '#0288d1', mr: 1, verticalAlign: 'middle' }} />
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
                          <Search sx={{ color: '#64748B', fontSize: 20 }} />
                        </InputAdornment>
                      ),
                    }}
                    sx={{
                      width: { xs: '100%', sm: '250px' },
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '8px',
                        backgroundColor: '#F8FAFC'
                      }
                    }}
                  />
                  
            
                </Box>
              </Box>
              
              <TableContainer 
                component={Paper} 
                sx={{ 
                  boxShadow: '0 4px 12px rgba(0,0,0,0.08)', 
                  border: '1px solid #E2E8F0',
                  borderRadius: '12px',
                  overflow: 'hidden',
                  maxHeight: 'calc(100vh - 400px)'
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
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>First Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Last Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Email</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Department</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Time In</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Time Out</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {filteredAttendees.map((attendee, index) => (
                        <TableRow 
                          key={index} 
                          sx={{ 
                            '&:hover': { bgcolor: '#F8FAFC' },
                            bgcolor: index % 2 === 0 ? 'white' : '#F9FAFB'
                          }}
                        >
                          <TableCell sx={{ color: '#1E293B' }}>{attendee.firstName || 'N/A'}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>{attendee.lastName || 'N/A'}</TableCell>
                          <TableCell sx={{ color: '#64748B' }}>{attendee.email || 'N/A'}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>
                            {attendee.department !== 'N/A' ? (
                              <Chip 
                                label={attendee.department}
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
                          <TableCell sx={{ color: '#64748B' }}>{formatTimeInStatus(attendee.timeIn, attendee.manualEntry)}</TableCell>
                          <TableCell sx={{ color: '#64748B' }}>{formatTimeoutStatus(attendee.timeOut, attendee.manualEntry)}</TableCell>
                        </TableRow>
                      ))}
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
    </Box>
  );
}