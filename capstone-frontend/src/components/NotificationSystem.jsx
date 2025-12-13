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
import { formatDateCompactPH } from '../utils/dateUtils';

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
      const response = await axios.get('https://timed-utd9.onrender.com/api/events/getAll');
      const allEvents = response.data;

      // Sort events by date - most recent first
      const sortedEvents = [...allEvents].sort((a, b) => {
        const dateA = new Date(a.date);
        const dateB = new Date(b.date);
        return dateB - dateA; // Descending order (most recent first)
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

  // Navigate to event detail page when clicking on a notification
  const handleNotificationItemClick = (eventId) => {
    navigate(`/attendance/${eventId}`);
    handleNotificationClose();
  };

  // Function to get status color
  const getStatusColor = (status = 'Unknown') => {
    switch (status) {
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

  return (
    <>
      <IconButton
        onClick={handleNotificationClick}
        size="small"
        sx={{
          backgroundColor: newNotificationsCount > 0 ? 'rgba(59, 130, 246, 0.1)' : 'transparent',
          boxShadow: 'none',
          '&:hover': {
            backgroundColor: newNotificationsCount > 0 ? 'rgba(59, 130, 246, 0.15)' : 'rgba(0,0,0,0.04)',
            transform: 'scale(1.05)',
          },
          padding: '8px',
          borderRadius: '12px',
          transition: 'all 0.2s ease-in-out',
          ...(newNotificationsCount > 0 && {
            animation: 'gentlePulse 2s ease-in-out infinite',
            '@keyframes gentlePulse': {
              '0%, 100%': { transform: 'scale(1)' },
              '50%': { transform: 'scale(1.05)' }
            }
          })
        }}
      >
        <Badge
          badgeContent={newNotificationsCount > 0 ? newNotificationsCount : null}
          color="error"
          sx={{
            '& .MuiBadge-badge': {
              fontWeight: 'bold',
              fontSize: '0.65rem',
              minWidth: '18px',
              height: '18px',
              borderRadius: '9px',
              background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)',
              boxShadow: '0 2px 4px rgba(239, 68, 68, 0.4)',
              border: '2px solid white'
            }
          }}
        >
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 36,
              height: 36,
              borderRadius: '10px',
              background: newNotificationsCount > 0
                ? 'linear-gradient(135deg, #3B82F6 0%, #2563EB 100%)'
                : 'linear-gradient(135deg, #64748B 0%, #475569 100%)',
              boxShadow: newNotificationsCount > 0
                ? '0 2px 8px rgba(59, 130, 246, 0.3)'
                : '0 1px 3px rgba(0,0,0,0.1)',
              transition: 'all 0.2s ease-in-out'
            }}
          >
            <Notifications
              sx={{
                color: '#ffffff',
                fontSize: 20,
                ...(newNotificationsCount > 0 && {
                  animation: 'ring 0.5s ease-in-out',
                  '@keyframes ring': {
                    '0%': { transform: 'rotate(0)' },
                    '25%': { transform: 'rotate(15deg)' },
                    '50%': { transform: 'rotate(-15deg)' },
                    '75%': { transform: 'rotate(10deg)' },
                    '100%': { transform: 'rotate(0)' }
                  }
                })
              }}
            />
          </Box>
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
                    {formatDateCompactPH(event.date)}
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