import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Box,
  Typography,
  IconButton,
  Badge,
  Menu,
  List,
  ListItem,
  Chip
} from '@mui/material';
import {
  Notifications,
  CalendarToday
} from '@mui/icons-material';

const NotificationSystem = () => {
  const navigate = useNavigate();
  
  // Notification state
  const [notificationAnchorEl, setNotificationAnchorEl] = useState(null);
  const notificationMenuOpen = Boolean(notificationAnchorEl);
  const [newNotificationsCount, setNewNotificationsCount] = useState(0);
  const [recentEvents, setRecentEvents] = useState([]);
  const [lastViewedEventIds, setLastViewedEventIds] = useState([]);

  // Notification handlers
  const handleNotificationClick = (event) => {
    setNotificationAnchorEl(event.currentTarget);
    if (newNotificationsCount > 0) {
      // Save the current events as viewed
      const currentEventIds = recentEvents.map(event => event.eventId);
      localStorage.setItem('lastViewedEventIds', JSON.stringify(currentEventIds));
      setLastViewedEventIds(currentEventIds);
      setNewNotificationsCount(0);
    }
  };

  const handleNotificationClose = () => {
    setNotificationAnchorEl(null);
  };

  // Fetch recent events for notifications
  const fetchRecentEvents = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/events/getAll');
      const allEvents = response.data;
      
      // Sort events by date created/added (most recent first)
      // If there's a createdAt field, use that, otherwise fall back to date
      const sortedEvents = [...allEvents].sort((a, b) => {
        // Use eventId as a proxy for creation order if both were created on the same day
        // Higher eventId values are likely more recently created
        if (a.eventId && b.eventId) {
          const idA = parseInt(a.eventId.toString().replace(/\D/g, ''), 10) || 0;
          const idB = parseInt(b.eventId.toString().replace(/\D/g, ''), 10) || 0;
          return idB - idA; // Descending order (higher IDs first)
        }
        return 0;
      });
      
      // Take only the 5 most recent events
      const recent = sortedEvents.slice(0, 5);
      setRecentEvents(recent);
      
      // Get previously viewed event IDs from localStorage
      const storedIds = localStorage.getItem('lastViewedEventIds');
      const previouslyViewedIds = storedIds ? JSON.parse(storedIds) : [];
      
      if (!lastViewedEventIds.length && previouslyViewedIds.length) {
        setLastViewedEventIds(previouslyViewedIds);
      }
      
      // Count new notifications
      if (recent.length > 0) {
        // Count events that haven't been viewed before
        const newEvents = recent.filter(event => 
          !previouslyViewedIds.includes(event.eventId)
        );
        
        setNewNotificationsCount(newEvents.length);
      }
    } catch (error) {
      console.error('Error fetching recent events:', error);
    }
  };

  // Check for new events periodically
  useEffect(() => {
    fetchRecentEvents();
    
    // Set up interval to check for new events every 5 minutes
    const interval = setInterval(() => {
      fetchRecentEvents();
    }, 5 * 60 * 1000);
    
    return () => clearInterval(interval);
  }, []);

  // Format event date for notifications
  const formatEventDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Navigate to event detail page when clicking on a notification
  const handleNotificationItemClick = (eventId) => {
    navigate(`/attendance/${eventId}`);
    handleNotificationClose();
  };

  // Function to get status color
  const getStatusColor = (status = 'Unknown') => {
    switch(status) {
      case 'Upcoming':
        return '#10B981';
      case 'Ongoing':
        return '#0288d1';
      case 'Ended':
        return '#EF4444';
      case 'Cancelled':
        return '#94A3B8';
      default:
        return '#64748B';
    }
  };

  // Define animation keyframes
  const pulseAnimation = {
    '@keyframes pulse': {
      '0%': {
        transform: 'scale(0.8)',
        opacity: 1
      },
      '50%': {
        transform: 'scale(1)',
        opacity: 0.8
      },
      '100%': {
        transform: 'scale(0.8)',
        opacity: 1
      }
    }
  };

  return (
    <>
      <IconButton 
        onClick={handleNotificationClick}
        size="small"
        sx={{ 
          backgroundColor: 'transparent',
          boxShadow: 'none',
          '&:hover': { backgroundColor: 'rgba(0,0,0,0.04)' },
          padding: '8px',
          borderRadius: '50%'
        }}
      >
        <Badge 
          badgeContent={newNotificationsCount > 0 ? newNotificationsCount : null}
          color="error"
          sx={{
            '& .MuiBadge-badge': {
              fontWeight: 'bold',
              fontSize: '0.7rem'
            }
          }}
        >
          <Notifications sx={{ color: '#64748B', fontSize: 20 }} />
        </Badge>
      </IconButton>
      
      {/* Notification Menu */}
      <Menu
        anchorEl={notificationAnchorEl}
        open={notificationMenuOpen}
        onClose={handleNotificationClose}
        PaperProps={{
          elevation: 3,
          sx: { 
            width: 320,
            mt: 1,
            maxHeight: 400,
            overflow: 'auto'
          }
        }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        <Box sx={{ p: 2, borderBottom: '1px solid #E2E8F0' }}>
          <Typography variant="subtitle1" fontWeight="600" color="#334155">
            Notifications
          </Typography>
          <Typography variant="body2" color="#64748B">
            Recently added events
          </Typography>
        </Box>
        
        {recentEvents.length === 0 ? (
          <Box sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="body2" color="#64748B">
              No recent events
            </Typography>
          </Box>
        ) : (
          <List sx={{ p: 0 }}>
            {recentEvents.map((event, index) => (
              <ListItem 
                key={event.eventId}
                onClick={() => handleNotificationItemClick(event.eventId)}
                sx={{ 
                  p: 2, 
                  borderBottom: index < recentEvents.length - 1 ? '1px solid #F1F5F9' : 'none',
                  cursor: 'pointer',
                  '&:hover': {
                    bgcolor: '#F8FAFC'
                  },
                  bgcolor: !lastViewedEventIds.includes(event.eventId) ? 'rgba(59, 130, 246, 0.08)' : 'transparent'
                }}
              >
                <Box sx={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                    <Typography variant="body2" fontWeight="600" color="#334155">
                      {event.eventName}
                    </Typography>
                    <Chip 
                      label={event.status} 
                      size="small"
                      sx={{
                        backgroundColor: `${getStatusColor(event.status)}20`,
                        color: getStatusColor(event.status),
                        fontWeight: 500,
                        fontSize: '0.625rem',
                        height: 20
                      }}
                    />
                  </Box>
                  <Typography variant="caption" color="#64748B">
                    {event.departmentName || (event.department ? event.department.name : '')}
                  </Typography>
                  <Typography variant="caption" color="#64748B" sx={{ display: 'flex', alignItems: 'center', mt: 0.5 }}>
                    <CalendarToday sx={{ fontSize: 12, mr: 0.5 }} />
                    {formatEventDate(event.date)}
                  </Typography>
                </Box>
              </ListItem>
            ))}
          </List>
        )}
      </Menu>
    </>
  );
};

export default NotificationSystem; 