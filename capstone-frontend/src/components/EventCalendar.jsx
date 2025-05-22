import React, { useState, useEffect } from 'react';
import { 
  Box, 
  Typography, 
  Paper, 
  useTheme,
  alpha,
  CircularProgress
} from '@mui/material';
import './EventCalendar.css';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import PlaceIcon from '@mui/icons-material/Place';

const EventCalendar = ({ 
  events, 
  onEventClick
}) => {
  const theme = useTheme();
  
  const [isLoading, setIsLoading] = useState(true);
  
  // Simulate loading effect
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 800);
    
    return () => clearTimeout(timer);
  }, []);
  
  // Get color based on event status
  const getStatusColor = (status) => {
    if (!status) return theme.palette.grey[500];
    
    // Normalize status to lowercase for case-insensitive comparison
    const normalizedStatus = status.toLowerCase();
    
    if (normalizedStatus.includes('schedul') || normalizedStatus.includes('upcoming')) {
      return theme.palette.primary.main;
    } else if (normalizedStatus.includes('ongoing') || normalizedStatus.includes('active')) {
      return theme.palette.success.main;
    } else if (normalizedStatus.includes('end') || normalizedStatus.includes('complet')) {
      return theme.palette.error.main;
    } else if (normalizedStatus.includes('cancel')) {
      return theme.palette.warning.main;
    } else {
      return theme.palette.grey[500];
    }
  };
  
  // Get event priority class based on event type or importance
  const getEventPriorityClass = (event) => {
    // This can be customized based on your event properties
    if (event.priority === 'high' || event.status === 'Ongoing') {
      return 'event-priority-high';
    } else if (event.priority === 'medium' || event.status === 'Scheduled') {
      return 'event-priority-medium';
    } else if (event.priority === 'low') {
      return 'event-priority-low';
    }
    return '';
  };
  
  // Format events for FullCalendar
  const calendarEvents = events.map(event => {
    // Debug logging
    console.log('Processing event for calendar:', event);
    
    // Parse date from event
    let eventDate;
    try {
      if (typeof event.date === 'string') {
        // Handle different date formats
        if (event.date.includes('at')) {
          // Format like "May 20, 2025 at 12:18:00 AM"
          const [datePart, timePart] = event.date.split(' at ');
          eventDate = new Date(`${datePart} ${timePart}`);
        } else if (event.date.match(/([A-Za-z]+\s\d+,\s\d{4})/)) {
          // Format like "May 5, 2023 10:30 AM"
          const dateMatch = event.date.match(/([A-Za-z]+\s\d+,\s\d{4})/);
          const timeMatch = event.date.match(/(\d{1,2}:\d{2}(?::\d{2})?\s[AP]M)/);
          
          if (dateMatch && timeMatch) {
            const dateStr = dateMatch[0];
            const timeStr = timeMatch[0];
            eventDate = new Date(`${dateStr} ${timeStr}`);
          } else {
            // Fallback to original date string
            eventDate = new Date(event.date);
          }
        } else {
          // Standard date formats (ISO, etc.)
          eventDate = new Date(event.date);
        }
      } else {
        // If it's already a Date object or timestamp
        eventDate = new Date(event.date);
      }
      
      // Validate the parsed date
      if (isNaN(eventDate.getTime())) {
        console.warn('Invalid date parsed for event:', event);
        eventDate = new Date(); // Fallback to current date
      }
    } catch (error) {
      console.error('Error parsing event date:', error, event);
      eventDate = new Date(); // Fallback to current date
    }
    
    // Calculate end time using duration
    let endDate = new Date(eventDate);
    try {
      if (event.duration) {
        const durationParts = event.duration.split(':').map(Number);
        const hours = durationParts[0] || 0;
        const minutes = durationParts[1] || 0;
        const seconds = durationParts[2] || 0;
        
        endDate.setHours(endDate.getHours() + hours);
        endDate.setMinutes(endDate.getMinutes() + minutes);
        endDate.setSeconds(endDate.getSeconds() + seconds);
      } else {
        // Default 1 hour duration if not specified
        endDate.setHours(endDate.getHours() + 1);
      }
    } catch (error) {
      console.error('Error calculating end date:', error, event);
      endDate.setHours(endDate.getHours() + 1); // Default 1 hour
    }
    
    // Ensure we have a valid title and ID
    const title = event.eventName || event.name || 'Unnamed Event';
    const id = event.eventId || event.id || `event-${Math.random().toString(36).substr(2, 9)}`;
    const status = event.status || 'Unknown';
    
    console.log(`Calendar event processed: "${title}" (${id}), Start: ${eventDate.toLocaleString()}, Status: ${status}`);
    
    // Return formatted event for calendar
    return {
      id: id,
      title: title,
      start: eventDate,
      end: endDate,
      backgroundColor: alpha(getStatusColor(status), 0.8),
      borderColor: getStatusColor(status),
      textColor: theme.palette.getContrastText(getStatusColor(status)),
      classNames: [getEventPriorityClass(event)],
      extendedProps: {
        ...event,
        formattedDate: eventDate.toLocaleString()
      }
    };
  });
  
  // Handler for event click
  const handleEventClick = (info) => {
    if (onEventClick) {
      onEventClick(info.event.extendedProps);
    }
  };
  
  // Custom render for calendar header
  const renderCalendarHeader = () => {
    return {
      start: 'prev,next today',
      center: 'title',
      end: 'dayGridMonth,timeGridWeek,timeGridDay'
    };
  };
  
  return (
    <Paper 
      elevation={2} 
      className="calendar-container"
      sx={{ 
        p: 3, 
        borderRadius: 2, 
        boxShadow: theme => `0 4px 20px 0 ${alpha(theme.palette.grey[500], 0.2)}`,
        overflow: 'hidden',
        position: 'relative'
      }}
    >
      {isLoading && (
        <Box className="fc-loading">
          <CircularProgress size={40} color="primary" />
        </Box>
      )}
    
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5" fontWeight="600" color="primary">Event Calendar</Typography>
      </Box>
      
      <Box sx={{ 
        height: '650px',
        '& .fc-header-toolbar': {
          mb: 2,
          '& .fc-button': {
            textTransform: 'capitalize',
            boxShadow: 'none',
            borderRadius: 1.5,
            fontSize: '0.85rem',
            py: 1,
            '&:focus': {
              boxShadow: 'none'
            }
          },
          '& .fc-button-primary': {
            bgcolor: theme => alpha(theme.palette.primary.main, 0.1),
            color: 'primary.main',
            borderColor: 'transparent',
            '&:hover': {
              bgcolor: theme => alpha(theme.palette.primary.main, 0.2),
              borderColor: 'transparent'
            },
            '&.fc-button-active': {
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              '&:hover': {
                bgcolor: 'primary.dark'
              }
            }
          }
        },
        '& .fc-day-today': {
          bgcolor: theme => alpha(theme.palette.primary.main, 0.05),
        },
        '& .fc-event': {
          borderRadius: 1,
          cursor: 'pointer',
          transition: 'transform 0.15s ease-in-out',
          '&:hover': {
            transform: 'translateY(-2px)',
            boxShadow: theme => `0 4px 8px 0 ${alpha(theme.palette.common.black, 0.1)}`
          }
        },
        '& .fc-cell-shaded': {
          bgcolor: theme => alpha(theme.palette.grey[100], 0.3)
        },
        '& .fc-daygrid-day-number, & .fc-col-header-cell-cushion': {
          color: 'text.primary',
          textDecoration: 'none',
          fontWeight: '500',
          padding: '4px 8px',
          borderRadius: 1
        },
        '& .fc-toolbar-title': {
          fontSize: '1.5rem',
          fontWeight: 600
        }
      }}>
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="dayGridMonth"
          headerToolbar={renderCalendarHeader()}
          events={calendarEvents}
          eventClick={handleEventClick}
          height="100%"
          eventTimeFormat={{
            hour: '2-digit',
            minute: '2-digit',
            meridiem: 'short'
          }}
          dayMaxEvents={3}
          eventContent={(info) => {
            const timeText = info.timeText;
            const title = info.event.title;
            
            return (
              <Box sx={{ 
                p: 0.5, 
                width: '100%', 
                overflow: 'hidden', 
                textOverflow: 'ellipsis',
                display: 'flex',
                flexDirection: 'column',
                gap: 0.5
              }}>
                {timeText && (
                  <Typography variant="caption" fontWeight="medium" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <AccessTimeIcon fontSize="inherit" />
                    {timeText}
                  </Typography>
                )}
                <Typography variant="body2" fontWeight="medium" noWrap>
                  {title}
                </Typography>
                {info.event.extendedProps.location && (
                  <Typography variant="caption" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }} noWrap>
                    <PlaceIcon fontSize="inherit" />
                    {info.event.extendedProps.location}
                  </Typography>
                )}
              </Box>
            );
          }}
        />
      </Box>
    </Paper>
  );
};

export default EventCalendar; 