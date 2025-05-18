import React, { useState } from 'react';
import { Box, Typography, Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField, FormControl, InputLabel, Select, MenuItem, Paper } from '@mui/material';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';

const EventCalendar = ({ 
  events, 
  departments = [], 
  onEventClick, 
  onAddEvent, 
  onOpenCertificateEditor 
}) => {
  // State for the event creation dialog
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [newEvent, setNewEvent] = useState({
    eventName: '',
    departmentId: '',
    date: '',
    time: '08:00',
    duration: '01:00:00', // HH:MM:SS format
    location: '',
    description: ''
  });
  
  // Get color based on event status
  const getStatusColor = (status) => {
    switch (status) {
      case 'Scheduled':
        return '#3788d8';
      case 'Ongoing':
        return '#2ca02c';
      case 'Ended':
        return '#d62728';
      case 'Canceled':
        return '#ff9800';
      default:
        return '#7F7F7F';
    }
  };
  
  // Format events for FullCalendar
  const calendarEvents = events.map(event => {
    // Parse date from event
    let eventDate;
    if (typeof event.date === 'string') {
      // Try to extract date from formatted string like "May 5, 2023 10:30 AM"
      const dateMatch = event.date.match(/([A-Za-z]+\s\d+,\s\d{4})/);
      const timeMatch = event.date.match(/(\d{1,2}:\d{2}\s[AP]M)/);
      
      if (dateMatch && timeMatch) {
        const dateStr = dateMatch[0];
        const timeStr = timeMatch[0];
        eventDate = new Date(`${dateStr} ${timeStr}`);
      } else {
        // Fallback to original date string
        eventDate = new Date(event.date);
      }
    } else {
      // If it's already a Date object or timestamp
      eventDate = new Date(event.date);
    }
    
    // Calculate end time using duration
    let endDate = new Date(eventDate);
    if (event.duration) {
      const [hours, minutes, seconds] = event.duration.split(':').map(Number);
      endDate.setHours(endDate.getHours() + (hours || 0));
      endDate.setMinutes(endDate.getMinutes() + (minutes || 0));
      endDate.setSeconds(endDate.getSeconds() + (seconds || 0));
    }
    
    // Return formatted event for calendar
    return {
      id: event.eventId || event.id,
      title: event.eventName || event.name,
      start: eventDate,
      end: endDate,
      backgroundColor: getStatusColor(event.status),
      borderColor: getStatusColor(event.status),
      textColor: '#ffffff',
      extendedProps: {
        ...event
      }
    };
  });
  
  // Handler for date click (to add a new event)
  const handleDateClick = (info) => {
    setSelectedDate(info.date);
    const formattedDate = info.date.toISOString().split('T')[0];
    setNewEvent({
      ...newEvent,
      date: formattedDate
    });
    setIsDialogOpen(true);
  };
  
  // Handler for event click
  const handleEventClick = (info) => {
    if (onEventClick) {
      onEventClick(info.event.extendedProps);
    }
  };
  
  // Handle input changes for new event
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setNewEvent({
      ...newEvent,
      [name]: value
    });
  };
  
  // Handle form submission for new event
  const handleSubmit = () => {
    // Combine date and time for the event
    const dateTime = new Date(newEvent.date);
    const [hours, minutes] = newEvent.time.split(':').map(Number);
    dateTime.setHours(hours, minutes, 0);
    
    // Format the event object for submission
    const formattedEvent = {
      ...newEvent,
      date: dateTime.toISOString(),
    };
    
    // Call the parent component's handler
    if (onAddEvent) {
      onAddEvent(formattedEvent);
    }
    
    // Close the dialog and reset form
    setIsDialogOpen(false);
    setSelectedDate(null);
    setNewEvent({
      eventName: '',
      departmentId: '',
      date: '',
      time: '08:00',
      duration: '01:00:00',
      location: '',
      description: ''
    });
  };
  
  // Handle dialog close
  const handleClose = () => {
    setIsDialogOpen(false);
    setSelectedDate(null);
  };
  
  // Handle the option to create a certificate template
  const handleCreateCertificate = () => {
    // Create certificate template for the new event
    if (onOpenCertificateEditor) {
      onOpenCertificateEditor(newEvent);
    }
    handleClose();
  };
  
  return (
    <Paper elevation={0} sx={{ p: 2, borderRadius: 2, border: '1px solid #e0e0e0' }}>
      <Box sx={{ height: '650px' }}>
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="dayGridMonth"
          headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay'
          }}
          events={calendarEvents}
          dateClick={handleDateClick}
          eventClick={handleEventClick}
          height="100%"
          eventTimeFormat={{
            hour: '2-digit',
            minute: '2-digit',
            meridiem: 'short'
          }}
        />
      </Box>
      
      {/* Dialog for adding new events */}
      <Dialog open={isDialogOpen} onClose={handleClose} fullWidth maxWidth="sm">
        <DialogTitle>Create New Event</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            <TextField
              name="eventName"
              label="Event Name"
              fullWidth
              value={newEvent.eventName}
              onChange={handleInputChange}
              required
            />
            
            <FormControl fullWidth required>
              <InputLabel>Department</InputLabel>
              <Select
                name="departmentId"
                value={newEvent.departmentId}
                onChange={handleInputChange}
                label="Department"
              >
                {departments.map((dept) => (
                  <MenuItem key={dept.id} value={dept.id}>
                    {dept.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                name="date"
                label="Date"
                type="date"
                fullWidth
                value={newEvent.date}
                onChange={handleInputChange}
                InputLabelProps={{
                  shrink: true,
                }}
                required
              />
              
              <TextField
                name="time"
                label="Time"
                type="time"
                fullWidth
                value={newEvent.time}
                onChange={handleInputChange}
                InputLabelProps={{
                  shrink: true,
                }}
                required
              />
            </Box>
            
            <TextField
              name="duration"
              label="Duration (HH:MM:SS)"
              fullWidth
              value={newEvent.duration}
              onChange={handleInputChange}
              placeholder="01:00:00"
              required
            />
            
            <TextField
              name="location"
              label="Location"
              fullWidth
              value={newEvent.location}
              onChange={handleInputChange}
            />
            
            <TextField
              name="description"
              label="Description"
              fullWidth
              multiline
              rows={3}
              value={newEvent.description}
              onChange={handleInputChange}
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2, display: 'flex', justifyContent: 'space-between' }}>
          <Button onClick={handleCreateCertificate} color="info" variant="outlined">
            Create Certificate Template
          </Button>
          <Box>
            <Button onClick={handleClose}>Cancel</Button>
            <Button 
              onClick={handleSubmit} 
              variant="contained" 
              disabled={!newEvent.eventName || !newEvent.departmentId || !newEvent.date || !newEvent.time || !newEvent.duration}
            >
              Create Event
            </Button>
          </Box>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};

export default EventCalendar; 