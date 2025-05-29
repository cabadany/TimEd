import React, { useState, useEffect } from 'react';
import { firestore } from '../firebase/firebase';
import { collection, query, orderBy, limit, onSnapshot, where } from 'firebase/firestore';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Chip,
  List,
  ListItem,
  ListItemText,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
  LinearProgress
} from '@mui/material';
import { Email, CheckCircle, Error, Schedule, Send } from '@mui/icons-material';

const EmailStatusTracker = () => {
  const [emails, setEmails] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedEmail, setSelectedEmail] = useState(null);
  const [detailsOpen, setDetailsOpen] = useState(false);

  useEffect(() => {
    const q = query(
      collection(firestore, 'mail'),
      orderBy('timestamp', 'desc'),
      limit(50)
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const emailData = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setEmails(emailData);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const getStatusChip = (email) => {
    const delivery = email.delivery;
    
    if (!delivery || delivery.state === 'PENDING') {
      return <Chip icon={<Schedule />} label="Pending" color="default" size="small" />;
    }
    
    if (delivery.state === 'PROCESSING') {
      return <Chip icon={<Send />} label="Processing" color="primary" size="small" />;
    }
    
    if (delivery.state === 'SUCCESS') {
      return <Chip icon={<CheckCircle />} label="Delivered" color="success" size="small" />;
    }
    
    if (delivery.state === 'ERROR') {
      return <Chip icon={<Error />} label="Failed" color="error" size="small" />;
    }
    
    return <Chip label="Unknown" color="default" size="small" />;
  };

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString();
  };

  const handleViewDetails = (email) => {
    setSelectedEmail(email);
    setDetailsOpen(true);
  };

  const getEmailSubject = (email) => {
    return email.message?.subject || 'No Subject';
  };

  const getRecipient = (email) => {
    if (email.to && Array.isArray(email.to)) {
      return email.to[0];
    }
    return email.to || 'Unknown';
  };

  if (loading) {
    return (
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Loading Email Status...
          </Typography>
          <LinearProgress />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center" marginBottom={2}>
          <Email sx={{ marginRight: 1 }} />
          <Typography variant="h6">
            Email Delivery Status
          </Typography>
        </Box>
        
        <List>
          {emails.map((email, index) => (
            <React.Fragment key={email.id}>
              <ListItem 
                button 
                onClick={() => handleViewDetails(email)}
                sx={{ 
                  borderRadius: 1, 
                  marginBottom: 1,
                  backgroundColor: 'rgba(0,0,0,0.02)',
                  '&:hover': {
                    backgroundColor: 'rgba(0,0,0,0.04)'
                  }
                }}
              >
                <ListItemText
                  primary={
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Typography variant="subtitle2">
                        {getEmailSubject(email)}
                      </Typography>
                      {getStatusChip(email)}
                    </Box>
                  }
                  secondary={
                    <Box>
                      <Typography variant="body2" color="textSecondary">
                        To: {getRecipient(email)}
                      </Typography>
                      <Typography variant="body2" color="textSecondary">
                        {formatTimestamp(email.timestamp)}
                      </Typography>
                      {email.eventId && (
                        <Typography variant="body2" color="textSecondary">
                          Event: {email.eventId}
                        </Typography>
                      )}
                    </Box>
                  }
                />
              </ListItem>
              {index < emails.length - 1 && <Divider />}
            </React.Fragment>
          ))}
        </List>

        {emails.length === 0 && (
          <Typography variant="body2" color="textSecondary" textAlign="center">
            No emails found
          </Typography>
        )}
      </CardContent>

      {/* Details Dialog */}
      <Dialog 
        open={detailsOpen} 
        onClose={() => setDetailsOpen(false)}
        maxWidth="md" 
        fullWidth
      >
        <DialogTitle>Email Details</DialogTitle>
        <DialogContent>
          {selectedEmail && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Subject: {getEmailSubject(selectedEmail)}
              </Typography>
              
              <Typography variant="body1" gutterBottom>
                <strong>Recipient:</strong> {getRecipient(selectedEmail)}
              </Typography>
              
              <Typography variant="body1" gutterBottom>
                <strong>Status:</strong> {getStatusChip(selectedEmail)}
              </Typography>
              
              <Typography variant="body1" gutterBottom>
                <strong>Sent:</strong> {formatTimestamp(selectedEmail.timestamp)}
              </Typography>
              
              {selectedEmail.eventId && (
                <Typography variant="body1" gutterBottom>
                  <strong>Event ID:</strong> {selectedEmail.eventId}
                </Typography>
              )}
              
              {selectedEmail.emailType && (
                <Typography variant="body1" gutterBottom>
                  <strong>Type:</strong> {selectedEmail.emailType}
                </Typography>
              )}

              {selectedEmail.delivery && (
                <Box mt={2}>
                  <Typography variant="h6" gutterBottom>
                    Delivery Information
                  </Typography>
                  
                  <Typography variant="body1">
                    <strong>State:</strong> {selectedEmail.delivery.state}
                  </Typography>
                  
                  {selectedEmail.delivery.startTime && (
                    <Typography variant="body1">
                      <strong>Start Time:</strong> {formatTimestamp(selectedEmail.delivery.startTime)}
                    </Typography>
                  )}
                  
                  {selectedEmail.delivery.endTime && (
                    <Typography variant="body1">
                      <strong>End Time:</strong> {formatTimestamp(selectedEmail.delivery.endTime)}
                    </Typography>
                  )}
                  
                  {selectedEmail.delivery.attempts && (
                    <Typography variant="body1">
                      <strong>Attempts:</strong> {selectedEmail.delivery.attempts}
                    </Typography>
                  )}
                  
                  {selectedEmail.delivery.error && (
                    <Typography variant="body1" color="error">
                      <strong>Error:</strong> {selectedEmail.delivery.error}
                    </Typography>
                  )}
                  
                  {selectedEmail.delivery.info && (
                    <Box mt={1}>
                      <Typography variant="body2">
                        <strong>Message ID:</strong> {selectedEmail.delivery.info.messageId}
                      </Typography>
                      {selectedEmail.delivery.info.accepted && (
                        <Typography variant="body2" color="success.main">
                          <strong>Accepted:</strong> {selectedEmail.delivery.info.accepted.join(', ')}
                        </Typography>
                      )}
                      {selectedEmail.delivery.info.rejected && selectedEmail.delivery.info.rejected.length > 0 && (
                        <Typography variant="body2" color="error">
                          <strong>Rejected:</strong> {selectedEmail.delivery.info.rejected.join(', ')}
                        </Typography>
                      )}
                    </Box>
                  )}
                </Box>
              )}

              {selectedEmail.message && (
                <Box mt={2}>
                  <Typography variant="h6" gutterBottom>
                    Message Preview
                  </Typography>
                  {selectedEmail.message.text && (
                    <Typography variant="body2" sx={{ 
                      backgroundColor: 'rgba(0,0,0,0.05)', 
                      padding: 1, 
                      borderRadius: 1,
                      whiteSpace: 'pre-wrap',
                      maxHeight: 200,
                      overflow: 'auto'
                    }}>
                      {selectedEmail.message.text}
                    </Typography>
                  )}
                  
                  {selectedEmail.message.attachments && selectedEmail.message.attachments.length > 0 && (
                    <Box mt={1}>
                      <Typography variant="body2">
                        <strong>Attachments:</strong>
                      </Typography>
                      {selectedEmail.message.attachments.map((attachment, index) => (
                        <Typography key={index} variant="body2" sx={{ ml: 2 }}>
                          • {attachment.filename} ({attachment.contentType})
                        </Typography>
                      ))}
                    </Box>
                  )}
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailsOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
};

export default EmailStatusTracker; 