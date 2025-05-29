import { useState, useEffect, memo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
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
  Tabs,
  Tab,
  Pagination,
  Stack,
  Divider,
  Container
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
  Edit,
  Delete,
  Add,
  Download,
  Email,
  Home,
  QrCode2
} from '@mui/icons-material';
import './certificate.css';
import CertificateEditor from '../components/CertificateEditor';
import { useTheme } from '../contexts/ThemeContext';

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
      const response = await axios.get('https://timed-utd9.onrender.com/api/user/getAll');
      
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

export default function Certificate() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const { darkMode } = useTheme();
  
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

  // New state for certificates
  const [certificates, setCertificates] = useState([]);
  const [showCertificateEditor, setShowCertificateEditor] = useState(false);
  const [currentCertificateData, setCurrentCertificateData] = useState(null);
  const [eventForCertificate, setEventForCertificate] = useState(null);
  const [activeTab, setActiveTab] = useState(0);
  const [events, setEvents] = useState([]);

  // Add pagination state
  const [page, setPage] = useState(1);
  const [certificatesPerPage] = useState(6);
  const [searchCertQuery, setSearchCertQuery] = useState('');
  const [filteredCertificates, setFilteredCertificates] = useState([]);

  useEffect(() => {
    // Remove admin role check since this is already an admin dashboard
    setIsAdmin(true);
    
    const fetchEventData = async () => {
      try {
        setLoading(true);
        
        // Fetch all events to find the specific one
        const eventsResponse = await axios.get('https://timed-utd9.onrender.com/api/events/getAll');
        const foundEvent = eventsResponse.data.find(e => e.eventId === eventId);
        
        if (!foundEvent) {
          throw new Error('Event not found');
        }
        
        setEvent(foundEvent);
        
        // Fetch department details
        if (foundEvent.departmentId) {
          const departmentsResponse = await axios.get('https://timed-utd9.onrender.com/api/departments');
          const foundDepartment = departmentsResponse.data.find(d => d.departmentId === foundEvent.departmentId);
          setDepartment(foundDepartment);
        }
        
        // Fetch attendees
        const attendeesResponse = await axios.get(`https://timed-utd9.onrender.com/api/attendance/${eventId}/attendees`);
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
    } else {
      // If no eventId is provided, we're on the certificates page
      fetchCertificates();
      fetchEvents();
      setLoading(false);
    }
  }, [eventId]);

  // Filter certificates when search query changes
  useEffect(() => {
    if (certificates.length > 0) {
      filterCertificates();
    }
  }, [searchCertQuery, certificates]);

  // Filter certificates based on search query
  const filterCertificates = () => {
    if (searchCertQuery.trim() === '') {
      setFilteredCertificates(certificates);
    } else {
      const query = searchCertQuery.toLowerCase();
      const filtered = certificates.filter(
        cert => 
          (cert.title && cert.title.toLowerCase().includes(query)) ||
          (cert.eventName && cert.eventName.toLowerCase().includes(query)) ||
          (events.find(e => e.eventId === cert.eventId)?.eventName.toLowerCase().includes(query))
      );
      setFilteredCertificates(filtered);
    }
  };

  // Handle pagination change
  const handlePageChange = (event, value) => {
    setPage(value);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Handle search input change for certificates
  const handleCertSearchChange = (event) => {
    const query = event.target.value.toLowerCase();
    setSearchCertQuery(query);
  };

  // Fetch all certificates
  const fetchCertificates = async () => {
    try {
      // Add debug counter for monitoring
      console.log("[DEBUG] Starting certificate fetch");
      
      // First, fetch all events to get valid event IDs
      const eventsResponse = await axios.get('https://timed-utd9.onrender.com/api/events/getAll');
      const validEventIds = eventsResponse.data.map(event => event.eventId);
      console.log(`[DEBUG] Found ${validEventIds.length} valid events`);
      
      // Fetch all certificates
      const certificatesResponse = await axios.get('https://timed-utd9.onrender.com/api/certificates', {
        params: {
          includeImages: true // Request server to include all image data
        }
      });
      const allCertificates = certificatesResponse.data;
      console.log(`[DEBUG] Fetched ${allCertificates.length} total certificates`);
      
      // Filter to only include certificates that have an event ID AND the event still exists
      const filteredCertificates = allCertificates.filter(cert => 
        cert.eventId && validEventIds.includes(cert.eventId)
      );
      
      console.log(`[DEBUG] Filtered to ${filteredCertificates.length} certificates with valid events`);
      console.log(`[DEBUG] Removed ${allCertificates.length - filteredCertificates.length} certificates with missing events`);
      
      // Add debugging display for orphaned certificates
      const orphanedCertificates = allCertificates.filter(cert => 
        cert.eventId && !validEventIds.includes(cert.eventId)
      );
      
      if (orphanedCertificates.length > 0) {
        console.log("[DEBUG] Orphaned certificates detected (certificates with non-existent events):");
        orphanedCertificates.forEach(cert => {
          console.log(`[DEBUG] Certificate ID: ${cert.id}, Event ID: ${cert.eventId}, Title: ${cert.title}`);
        });
      }
      
      // Make sure we have all certificate images
      for (const cert of filteredCertificates) {
        // If we have an eventId but images are missing, try to fetch them separately
        if (cert.eventId && (!cert.backgroundImage || !cert.logoImage || !cert.watermarkImage)) {
          try {
            const detailResponse = await axios.get(`https://timed-utd9.onrender.com/api/certificates/getByEventId/${cert.eventId}`);
            if (detailResponse.data) {
              // Merge any image data that might be available in the detailed response
              if (detailResponse.data.backgroundImage) cert.backgroundImage = detailResponse.data.backgroundImage;
              if (detailResponse.data.logoImage) cert.logoImage = detailResponse.data.logoImage;
              if (detailResponse.data.watermarkImage) cert.watermarkImage = detailResponse.data.watermarkImage;
              if (detailResponse.data.signatureImages) cert.signatureImages = detailResponse.data.signatureImages;
            }
          } catch (detailError) {
            console.log(`[DEBUG] Could not fetch detailed certificate for event ID ${cert.eventId}`);
          }
        }
      }
      
      setCertificates(filteredCertificates);
    } catch (error) {
      console.error('Error fetching certificates:', error);
      showSnackbar('Failed to fetch certificates', 'error');
    }
  };

  // Fetch all events for certificate creation
  const fetchEvents = async () => {
    try {
      const response = await axios.get('https://timed-utd9.onrender.com/api/events/getAll');
      setEvents(response.data);
    } catch (error) {
      console.error('Error fetching events:', error);
      showSnackbar('Failed to fetch events', 'error');
    }
  };

  // Fetch certificate template for a specific event
  const fetchCertificateForEvent = async (eventId) => {
    try {
      setLoading(true);
      const response = await axios.get(`https://timed-utd9.onrender.com/api/certificates/getByEventId/${eventId}`);
      if (response.data) {
        setCurrentCertificateData(response.data);
      } else {
        // No certificate found, set to null to use default
        setCurrentCertificateData(null);
      }
    } catch (error) {
      console.error('Error fetching certificate:', error);
      showSnackbar('Failed to fetch certificate template', 'error');
      setCurrentCertificateData(null);
    } finally {
      setLoading(false);
    }
  };

  // Open certificate editor
  const openCertificateEditor = (certificate = null, event = null) => {
    if (certificate) {
      // Edit existing certificate
      setCurrentCertificateData(certificate);
      const relatedEvent = events.find(e => e.eventId === certificate.eventId);
      
      // Make sure event name is properly set
      if (relatedEvent && certificate.eventName === '{Event Name}') {
        console.log('Updating certificate event name from related event:', relatedEvent.eventName);
        setCurrentCertificateData({
          ...certificate,
          eventName: relatedEvent.eventName
        });
      }
      
      setEventForCertificate(relatedEvent);
    } else if (event) {
      // Create new certificate for specific event
      setEventForCertificate(event);
      fetchCertificateForEvent(event.eventId);
    } else {
      // Redirect to Events page if trying to create a certificate without an event
      showSnackbar('Please select an event to create a certificate template', 'info');
      // You can uncomment this to redirect to events page
      // navigate('/events');
      return; // Don't open the editor
    }
    
    setShowCertificateEditor(true);
  };

  // Close certificate editor
  const closeCertificateEditor = () => {
    setShowCertificateEditor(false);
    setCurrentCertificateData(null);
    setEventForCertificate(null);
  };

  // Save certificate template
  const saveCertificateTemplate = async (certificateData) => {
    try {
      setLoading(true);
      
      const payload = {
        ...certificateData,
        eventId: eventForCertificate ? eventForCertificate.eventId : null,
        eventName: eventForCertificate ? eventForCertificate.eventName : certificateData.eventName
      };
      
      let response;
      if (certificateData.id) {
        // Update existing certificate
        response = await axios.put(`https://timed-utd9.onrender.com/api/certificates/${certificateData.id}`, payload);
      } else {
        // Create new certificate
        response = await axios.post('https://timed-utd9.onrender.com/api/certificates', payload);
      }
      
      // Get the complete certificate data from response
      const savedCertificateData = response.data || certificateData;
      
      // Link certificate to event if both certificate and event exist
      if (savedCertificateData.id && savedCertificateData.eventId) {
        try {
          console.log('Linking certificate to event', savedCertificateData.id, savedCertificateData.eventId);
          await axios.post('https://timed-utd9.onrender.com/api/certificates/linkToEvent', {
            certificateId: savedCertificateData.id,
            eventId: savedCertificateData.eventId
          });
          console.log('Certificate linked to event successfully');
        } catch (linkError) {
          console.error('Error linking certificate to event:', linkError);
        }
      }
      
      // Verify the certificate was saved correctly
      if (payload.eventId) {
        try {
          console.log('Verifying certificate was saved correctly');
          const verifyResponse = await axios.get(`https://timed-utd9.onrender.com/api/certificates/getByEventId/${payload.eventId}`);
          console.log('Certificate verification response:', verifyResponse.data);
        } catch (verifyError) {
          console.error('Certificate verification failed:', verifyError);
        }
      }
      
      showSnackbar('Certificate template saved successfully', 'success');
      setShowCertificateEditor(false);
      fetchCertificates(); // Refresh the certificates list
      
    } catch (error) {
      console.error('Error saving certificate:', error);
      showSnackbar('Failed to save certificate template', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Delete certificate
  const deleteCertificate = async (certificateId) => {
    try {
      setLoading(true);
      await axios.delete(`https://timed-utd9.onrender.com/api/certificates/delete/${certificateId}`);
      
      // Remove certificate from state
      setCertificates(certificates.filter(cert => cert.id !== certificateId));
      showSnackbar('Certificate deleted successfully', 'success');
    } catch (error) {
      console.error('Error deleting certificate:', error);
      showSnackbar('Failed to delete certificate', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Send certificate to attendees
  const sendCertificates = async (certificateId, eventId) => {
    try {
      setActionLoading(true);
      // This would connect to your backend endpoint that triggers email sending
      const attendeesResponse = await axios.get(`https://timed-utd9.onrender.com/api/attendance/${eventId}/attendees`);
      const attendees = attendeesResponse.data;
      
      // Get certificate template
      const certificateTemplate = certificates.find(cert => cert.id === certificateId);
      if (!certificateTemplate) {
        throw new Error('Certificate template not found');
      }

      // Create a temporary div for certificate rendering
      const certificateDiv = document.createElement('div');
      certificateDiv.style.width = '800px';
      certificateDiv.style.height = '600px';
      certificateDiv.style.position = 'absolute';
      certificateDiv.style.left = '-9999px';
      document.body.appendChild(certificateDiv);

      // Process each attendee
      for (const attendee of attendees) {
        // Create certificate HTML for this attendee
        certificateDiv.innerHTML = `
          <div style="
            width: 800px;
            height: 600px;
            padding: 40px;
            text-align: center;
            border: 10px solid ${certificateTemplate.borderColor || '#0047AB'};
            background-color: ${certificateTemplate.backgroundColor || '#ffffff'};
            font-family: ${certificateTemplate.fontFamily || 'Arial'};
            color: ${certificateTemplate.textColor || '#000000'};
          ">
            <h1 style="font-size: 50px; font-weight: bold; margin-bottom: 20px;">
              ${certificateTemplate.title || 'CERTIFICATE'}
            </h1>
            <h2 style="font-size: 35px; font-weight: bold; margin-bottom: 20px;">
              ${certificateTemplate.subtitle || 'OF ACHIEVEMENT'}
            </h2>
            <p style="font-size: 25px; margin-bottom: 20px;">
              ${certificateTemplate.recipientText || 'PRESENTED TO'}
            </p>
            <h3 style="font-size: 40px; font-weight: bold; font-style: italic; margin-bottom: 20px;">
              ${attendee.firstName} ${attendee.lastName}
            </h3>
            <p style="font-size: 20px; margin-bottom: 40px;">
              ${certificateTemplate.description || 'For outstanding participation'}
            </p>
            <div style="margin-top: 40px; font-size: 18px;">
              <p>Date: ${new Date().toLocaleDateString()}</p>
            </div>
          </div>
        `;

        // Convert the certificate to PDF
        const canvas = await html2canvas(certificateDiv);
        const imgData = canvas.toDataURL('image/png');
        
        const pdf = new jsPDF('l', 'px', [800, 600]);
        pdf.addImage(imgData, 'PNG', 0, 0, 800, 600);
        
        // Convert PDF to base64
        const pdfData = pdf.output('datauristring');

        // Send email with certificate
        await axios.post('https://timed-utd9.onrender.com/api/email/send', {
          to: attendee.email,
          from: 'timedcit@outlook.com',
          subject: `Your Certificate for ${certificateTemplate.eventName}`,
          text: `Dear ${attendee.firstName} ${attendee.lastName},\n\nPlease find attached your certificate for ${certificateTemplate.eventName}.\n\nBest regards,\nTimEd Team`,
          attachments: [{
            filename: 'certificate.pdf',
            content: pdfData.split(',')[1],
            encoding: 'base64',
            contentType: 'application/pdf'
          }]
        });
      }

      // Clean up
      document.body.removeChild(certificateDiv);
      
      showSnackbar('Certificates have been generated and sent successfully', 'success');
    } catch (error) {
      console.error('Error sending certificates:', error);
      showSnackbar('Failed to send certificates', 'error');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle search input change
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
  
  // Filter attendees based on search query
  const filterAttendees = () => {
    if (searchQuery.trim() === '') {
      setFilteredAttendees(attendees);
    } else {
      const filtered = attendees.filter(
        attendee => 
          attendee.firstName.toLowerCase().includes(searchQuery) ||
          attendee.lastName.toLowerCase().includes(searchQuery) ||
          attendee.email.toLowerCase().includes(searchQuery) ||
          attendee.department.toLowerCase().includes(searchQuery)
      );
      setFilteredAttendees(filtered);
    }
  };

  // Manage attendance modal handlers
  const handleOpenManageModal = () => {
    setManageModalOpen(true);
  };
  
  const handleCloseManageModal = () => {
    setManageModalOpen(false);
  };

  // Handle time in/out actions
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
      const response = await axios.post(`https://timed-utd9.onrender.com/api/attendance/${eventId}/${userId}/manual/timein`);
      
      if (response.status === 200) {
        // Refresh attendee list
        const attendeesResponse = await axios.get(`https://timed-utd9.onrender.com/api/attendance/${eventId}/attendees`);
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
      const response = await axios.post(`https://timed-utd9.onrender.com/api/attendance/${eventId}/${userId}/manual/timeout`);
      
      if (response.status === 200) {
        // Refresh attendee list
        const attendeesResponse = await axios.get(`https://timed-utd9.onrender.com/api/attendance/${eventId}/attendees`);
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
  
  // Snackbar handlers
  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  // Show snackbar notification
  const showSnackbar = (message, severity = 'success') => {
    setSnackbar({
      open: true,
      message,
      severity,
    });
  };

  // Format timestamp for display
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

  // Format event date for display
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

  // Format timeout status for display
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

  // Format time in status for display
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

  // Get status display color
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

  // Handle tab change
  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  // Render certificate thumbnail
  const renderCertificateThumbnail = (certificate) => {
    return (
      <Box 
        sx={{
          width: '100%',
          height: '180px',
          position: 'relative',
          overflow: 'hidden',
          backgroundColor: certificate.backgroundColor || '#ffffff',
          borderRadius: '8px 8px 0 0',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '20px',
          border: certificate.showBorder !== false ? `${certificate.borderWidth || 5}px solid ${certificate.borderColor || '#000000'}` : 'none',
          color: certificate.textColor || '#000000',
          fontFamily: certificate.fontFamily || 'Arial'
        }}
      >
        {/* Background Image */}
        {certificate.backgroundImage && (
          <Box
            component="img"
            src={`data:image/png;base64,${certificate.backgroundImage}`}
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              opacity: certificate.backgroundImageOpacity || 0.3,
              zIndex: 0
            }}
          />
        )}
        
        {/* Logo */}
        {certificate.logoImage && (
          <Box
            component="img"
            src={`data:image/png;base64,${certificate.logoImage}`}
            sx={{
              position: 'absolute',
              width: certificate.logoWidth || 60,
              height: certificate.logoHeight || 60,
              objectFit: 'contain',
              zIndex: 2,
              ...(certificate.logoPosition === 'top-left' && {
                top: 10,
                left: 10
              }),
              ...(certificate.logoPosition === 'top-center' && {
                top: 10,
                left: '50%',
                transform: 'translateX(-50%)'
              }),
              ...(certificate.logoPosition === 'top-right' && {
                top: 10,
                right: 10
              }),
              ...((!certificate.logoPosition || certificate.logoPosition === '') && {
                top: 10,
                left: '50%',
                transform: 'translateX(-50%)'
              })
            }}
          />
        )}

        {/* Watermark */}
        {certificate.watermarkImage && (
          <Box
            component="img"
            src={`data:image/png;base64,${certificate.watermarkImage}`}
            sx={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              width: '50%',
              height: 'auto',
              opacity: certificate.watermarkImageOpacity || 0.1,
              zIndex: 1
            }}
          />
        )}
        
        <Box sx={{ zIndex: 1, textAlign: 'center' }}>
          <Typography 
            variant="h6" 
            sx={{ 
              fontWeight: 'bold', 
              fontSize: '18px',
              letterSpacing: '1px',
              lineHeight: 1.2,
              mb: 0.5,
             
            }}
          >
            {certificate.title || 'CERTIFICATE'}
          </Typography>
          
          <Typography 
            variant="subtitle2" 
            sx={{ 
              fontWeight: 'bold', 
              fontSize: '13px',
              letterSpacing: '1px',
              mb: 1.5,
              lineHeight: 1,
            }}
          >
            {certificate.subtitle || 'OF ACHIEVEMENT'}
          </Typography>
          
          <Typography 
            variant="caption" 
            sx={{ 
              fontSize: '8px',
              display: 'block',
              mb: 0.5,
              lineHeight: 1
            }}
          >
            {certificate.recipientText || 'PRESENTED TO'}
          </Typography>
          
          <Typography 
            variant="body2" 
            sx={{ 
              fontStyle: 'italic',
              fontWeight: 'bold',
              fontSize: '13px',
              mb: 1,
              lineHeight: 1
            }}
          >
            {certificate.recipientName || '{Recipient Name}'}
          </Typography>
          
          <Typography 
            sx={{ 
              fontSize: '6px',
              mb: 1,
              maxHeight: '12px',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
              lineHeight: 1.2
            }}
          >
            {certificate.description || 'For outstanding participation...'}
          </Typography>
          
          {/* Added event name to be displayed in the thumbnail */}
          <Typography 
            sx={{ 
              fontSize: '8px',
              fontWeight: 'bold',
              mb: 0.5,
              lineHeight: 1
            }}
          >
            {(certificate.eventName && certificate.eventName !== '{Event Name}') 
              ? certificate.eventName 
              : events.find(e => e.eventId === certificate.eventId)?.eventName 
              || 'Event'}
          </Typography>
        </Box>
        
        {/* QR Code (if enabled) */}
        {certificate.showQRCode && (
          <Box
            sx={{
              position: 'absolute',
              ...(certificate.qrCodePosition === 'bottom-right' && {
                bottom: 5,
                right: 5
              }),
              ...(certificate.qrCodePosition === 'bottom-left' && {
                bottom: 5,
                left: 5
              }),
              ...(certificate.qrCodePosition === 'top-right' && {
                top: 5,
                right: 5
              }),
              ...(certificate.qrCodePosition === 'top-left' && {
                top: 5,
                left: 5
              }),
              ...((!certificate.qrCodePosition || certificate.qrCodePosition === '') && {
                bottom: 5,
                right: 5
              }),
              zIndex: 2,
              width: 20,
              height: 20,
              border: '1px solid #000',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              backgroundColor: '#fff'
            }}
          >
            <QrCode2 sx={{ fontSize: 14 }} />
          </Box>
        )}
      </Box>
    );
  };

  // Get paginated certificates
  const indexOfLastCertificate = page * certificatesPerPage;
  const indexOfFirstCertificate = indexOfLastCertificate - certificatesPerPage;
  const currentCertificates = filteredCertificates.slice(indexOfFirstCertificate, indexOfLastCertificate);
  const totalPages = Math.ceil(filteredCertificates.length / certificatesPerPage);

  // If we're on the event attendance page
  if (eventId) {
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

       
      </Box>

      {/* Main Content */}
      <Box sx={{ 
        p: 3, 
        flex: 1, 
        overflow: 'auto', 
        bgcolor: '#FFFFFF' 
      }}>
         
        
             
      
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

      
    </Box></Box>
    );
  }

  // If we're on the certificates page
  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Paper elevation={1} sx={{ 
        p: 3, 
        borderRadius: '12px',
        bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
        border: '1px solid',
        borderColor: darkMode ? '#333333' : '#E2E8F0'
      }}>
        <Box sx={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          mb: 3,
          flexWrap: 'wrap',
          gap: 2
        }}>
          <Typography variant="h5" sx={{ 
            fontWeight: 600, 
            color: darkMode ? '#f5f5f5' : '#1E293B' 
          }}>
            Certificate Templates
          </Typography>
          
          <TextField
            placeholder="Search templates..."
            variant="outlined"
            size="small"
            value={searchCertQuery}
            onChange={handleCertSearchChange}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search sx={{ color: darkMode ? '#aaaaaa' : '#64748B', fontSize: 20 }} />
                </InputAdornment>
              ),
            }}
            sx={{
              width: { xs: '100%', sm: '250px' },
              '& .MuiOutlinedInput-root': {
                borderRadius: '8px',
                backgroundColor: darkMode ? '#2d2d2d' : '#F8FAFC',
                '& fieldset': {
                  borderColor: darkMode ? '#333333' : '#E2E8F0',
                },
                '&:hover fieldset': {
                  borderColor: darkMode ? '#555555' : '#CBD5E1',
                },
                '&.Mui-focused fieldset': {
                  borderColor: darkMode ? '#90caf9' : '#0288d1',
                },
                '& input': {
                  color: darkMode ? '#f5f5f5' : 'inherit',
                  '&::placeholder': {
                    color: darkMode ? '#aaaaaa' : '#64748B',
                    opacity: 1,
                  },
                },
              },
            }}
          />
        </Box>
        
        <Divider sx={{ mb: 3 }} />

        {/* Certificate templates section */}
        <Box sx={{ mt: 1 }}>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
              <CircularProgress size={40} sx={{ color: '#0288d1' }} />
            </Box>
          ) : filteredCertificates.length === 0 ? (
            <Box sx={{ 
              textAlign: 'center', 
              py: 4,
              bgcolor: darkMode ? '#2d2d2d' : '#F8FAFC',
              border: '1px dashed',
              borderColor: darkMode ? '#333333' : '#CBD5E1',
              borderRadius: '8px'
            }}>
              <Typography 
                variant="h6" 
                sx={{ 
                  color: darkMode ? '#f5f5f5' : '#64748B',
                  mb: 1
                }}
              >
                No certificate templates found
              </Typography>
              <Typography 
                variant="body2" 
                sx={{ 
                  color: darkMode ? '#aaaaaa' : '#94A3B8',
                  mb: 3
                }}
              >
                {searchCertQuery ? 'Try a different search term or' : 'Create templates by selecting an event in the Events page'}
              </Typography>
            </Box>
          ) : (
            <>
              <Grid container spacing={3}>
                {currentCertificates.map((certificate) => (
                  <Grid item xs={12} sm={6} md={4} key={certificate.id}>
                    <Card 
                      elevation={2} 
                      sx={{ 
                        borderRadius: '12px', 
                        overflow: 'hidden',
                        transition: 'all 0.3s ease',
                        bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                        border: '1px solid',
                        borderColor: darkMode ? '#333333' : '#E2E8F0',
                        '&:hover': {
                          transform: 'translateY(-8px)',
                          boxShadow: darkMode 
                            ? '0 10px 25px rgba(0,0,0,0.3)'
                            : '0 10px 25px rgba(0,0,0,0.08)'
                        }
                      }}
                    >
                      {renderCertificateThumbnail(certificate)}
                      
                      <CardContent sx={{ pt: 2.5, pb: 2.5 }}>
                        <Typography 
                          variant="h6" 
                          gutterBottom
                          sx={{ 
                            fontSize: '18px', 
                            fontWeight: 600,
                            whiteSpace: 'nowrap',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            color: darkMode ? '#f5f5f5' : '#1E293B'
                          }}
                        >
                          {certificate.title || 'Certificate Template'}
                        </Typography>
                        
                        <Typography 
                          variant="body2" 
                          color="text.secondary"
                          sx={{
                            display: 'flex',
                            alignItems: 'center',
                            mb: 2
                          }}
                        >
                          <Event fontSize="small" sx={{ mr: 0.5, fontSize: '16px', color: '#0288d1' }} />
                          {(certificate.eventName && certificate.eventName !== '{Event Name}')
                            ? certificate.eventName 
                            : events.find(e => e.eventId === certificate.eventId)?.eventName 
                            || 'Unknown Event'}
                        </Typography>
                        
                        <Box sx={{ 
                          display: 'flex', 
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          mt: 'auto'
                        }}>
                          <Button
                            variant="outlined"
                            size="small"
                            onClick={() => openCertificateEditor(certificate)}
                            sx={{
                              borderColor: darkMode ? '#555555' : '#E2E8F0',
                              color: darkMode ? '#90caf9' : '#0288d1',
                              '&:hover': {
                                borderColor: darkMode ? '#90caf9' : '#0288d1',
                                bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'rgba(2, 136, 209, 0.04)'
                              }
                            }}
                          >
                            Edit Template
                          </Button>
                          
                          <IconButton
                            onClick={() => deleteCertificate(certificate.id)}
                            sx={{ 
                              color: darkMode ? '#ef5350' : '#ef5350',
                              '&:hover': {
                                bgcolor: darkMode ? 'rgba(239, 83, 80, 0.08)' : 'rgba(239, 83, 80, 0.04)'
                              }
                            }}
                          >
                            <Delete fontSize="small" />
                          </IconButton>
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
              
              {/* Pagination */}
              <Box sx={{ 
                display: 'flex', 
                justifyContent: 'center', 
                mt: 4,
                '& .MuiPaginationItem-root': {
                  color: darkMode ? '#f5f5f5' : 'inherit',
                  borderColor: darkMode ? '#333333' : '#E2E8F0',
                  '&.Mui-selected': {
                    bgcolor: darkMode ? '#90caf9' : '#0288d1',
                    color: darkMode ? '#1e1e1e' : '#ffffff',
                    '&:hover': {
                      bgcolor: darkMode ? '#42a5f5' : '#0277bd'
                    }
                  },
                  '&:hover': {
                    bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'rgba(2, 136, 209, 0.04)'
                  }
                }
              }}>
                <Pagination
                  count={totalPages}
                  page={page}
                  onChange={handlePageChange}
                  color="primary"
                />
              </Box>
            </>
          )}
        </Box>
      </Paper>
      
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
          bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
          boxShadow: darkMode 
            ? '0 8px 32px rgba(0,0,0,0.4)'
            : '0 8px 32px rgba(0,0,0,0.1)',
          p: 0,
          borderRadius: 2,
          overflow: 'auto',
          border: '1px solid',
          borderColor: darkMode ? '#333333' : '#E2E8F0'
        }}>
          <CertificateEditor 
            initialData={currentCertificateData} 
            onSave={saveCertificateTemplate}
            onClose={closeCertificateEditor}
          />
        </Box>
      </Modal>
      
      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity={snackbar.severity}
          sx={{
            bgcolor: darkMode 
              ? snackbar.severity === 'success' ? '#1b5e20' : '#b71c1c'
              : undefined,
            color: darkMode ? '#ffffff' : undefined,
            '& .MuiAlert-icon': {
              color: darkMode ? '#ffffff' : undefined
            }
          }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
}