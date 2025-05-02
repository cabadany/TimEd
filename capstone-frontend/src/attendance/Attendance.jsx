import { useState, useEffect } from 'react';
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
  Chip
} from '@mui/material';
import {
  ArrowBack,
  Event,
  AccessTime,
  CalendarToday,
  School,
  Group
} from '@mui/icons-material';
import './attendance.css';

export default function Attendance() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  
  const [event, setEvent] = useState(null);
  const [department, setDepartment] = useState(null);
  const [attendees, setAttendees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
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

  const handleNavigateBack = () => {
    navigate('/dashboard');
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
  const formatTimeoutStatus = (timeOut) => {
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
    return formatTimestamp(timeOut);
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
        alignItems: 'center'
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
            <Typography variant="h5" fontWeight="600" color="#1E293B" sx={{ mb: 3 }}>
              Event Attendance Details
            </Typography>
            
            {/* Event Information */}
            <Grid container spacing={3} sx={{ mb: 4 }}>
              <Grid item xs={12} md={6}>
                <Card sx={{ boxShadow: 'none', border: '1px solid #E2E8F0', height: '100%' }}>
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
                              backgroundColor: `${getStatusColor(event?.status)}10`,
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
                <Card sx={{ boxShadow: 'none', border: '1px solid #E2E8F0', height: '100%' }}>
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
            
            {/* Attendees Table */}
            <Box sx={{ mb: 2 }}>
              <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
                <Group sx={{ color: '#0288d1' }} />
                Faculty     Attendees
                <Chip 
                  label={attendees.length}
                  size="small"
                  sx={{
                    backgroundColor: '#EFF6FF',
                    color: '#3B82F6',
                    fontWeight: 600,
                    ml: 1
                  }}
                />
              </Typography>
              
              <TableContainer 
                component={Paper} 
                sx={{ 
                  boxShadow: 'none', 
                  border: '1px solid #E2E8F0',
                  borderRadius: '4px',
                  overflow: 'hidden',
                  maxHeight: 'calc(100vh - 400px)'
                }}
              >
                {attendees.length === 0 ? (
                  <Box sx={{ p: 3, textAlign: 'center', color: '#64748B' }}>
                    <Typography>No attendees found for this event</Typography>
                  </Box>
                ) : (
                  <Table stickyHeader>
                    <TableHead>
                      <TableRow>
                        {/*<TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>School ID</TableCell>*/}
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>First Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Last Name</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Email</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Department</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Time In</TableCell>
                        <TableCell sx={{ fontWeight: 600, color: '#475569', bgcolor: '#F8FAFC' }}>Time Out</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {attendees.map((attendee, index) => (
                        <TableRow key={index} sx={{ '&:hover': { bgcolor: '#F8FAFC' } }}>
                          {/*<TableCell sx={{ color: '#64748B' }}>{attendee.schoolId || 'N/A'}</TableCell>*/}
                          <TableCell sx={{ color: '#1E293B' }}>{attendee.firstName || 'N/A'}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>{attendee.lastName || 'N/A'}</TableCell>
                          <TableCell sx={{ color: '#64748B' }}>{attendee.email || 'N/A'}</TableCell>
                          <TableCell sx={{ color: '#1E293B' }}>
                            {attendee.department ? (
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
                          <TableCell sx={{ color: '#64748B' }}>{formatTimestamp(attendee.timeIn)}</TableCell>
                          <TableCell sx={{ color: '#64748B' }}>{formatTimeoutStatus(attendee.timeOut)}</TableCell>
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
    </Box>
  );
}