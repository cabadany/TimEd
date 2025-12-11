import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  CircularProgress,
  Snackbar,
  Alert,
  IconButton,
  Tooltip,
  Card,
  CardContent,
  Divider,
  Grid,
  Tabs,
  Tab
} from '@mui/material';
import {
  PersonAdd,
  Visibility,
  CheckCircle,
  Cancel,
  Email,
  Person,
  Badge,
  Business,
  Schedule,
  Close,
  Refresh,
  Outbox
} from '@mui/icons-material';
import { useTheme } from '../contexts/ThemeContext';
import axios from 'axios';
import { getApiUrl, API_ENDPOINTS } from '../utils/api';
import eventEmitter, { EVENTS } from '../utils/eventEmitter';
import './AccountRequests.css';

const AccountRequests = () => {
  const { darkMode } = useTheme();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showDetailsDialog, setShowDetailsDialog] = useState(false);
  const [showReviewDialog, setShowReviewDialog] = useState(false);
  const [reviewAction, setReviewAction] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [processing, setProcessing] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });
  const [tabValue, setTabValue] = useState(0);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  const [remindingRequestId, setRemindingRequestId] = useState(null);

  // Memoize fetchAccountRequests to prevent unnecessary re-renders
  const fetchAccountRequests = useCallback(async (isRefresh = false) => {
    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);
    
    try {
      const endpoint = tabValue === 0 ? 
        API_ENDPOINTS.GET_PENDING_ACCOUNT_REQUESTS : 
        API_ENDPOINTS.GET_ALL_ACCOUNT_REQUESTS;
        
      console.log('Fetching from endpoint:', getApiUrl(endpoint));
      
      const response = await axios.get(getApiUrl(endpoint), {
        timeout: 10000, // 10 second timeout
        headers: {
          'Content-Type': 'application/json',
        }
      });
      
      console.log('API Response:', response.data);
      
      if (response.data.success) {
        setRequests(response.data.requests || []);
        setRetryCount(0); // Reset retry count on success
        setError(null);
      } else {
        throw new Error(response.data.message || 'Failed to fetch account requests');
      }
    } catch (error) {
      console.error('Error fetching account requests:', error);
      setError(error);
      
      // If it's a network error or 500 error, we can retry
      if (error.response?.status >= 500 || error.code === 'NETWORK_ERROR' || error.code === 'ECONNABORTED') {
        if (retryCount < 3) {
          console.log(`Retrying request (attempt ${retryCount + 1})`);
          setRetryCount(prev => prev + 1);
          // Retry after a delay
          setTimeout(() => {
            fetchAccountRequests(isRefresh);
          }, 2000 * (retryCount + 1)); // Exponential backoff
          return;
        }
      }
      
      showSnackbar(
        `Error fetching account requests: ${error.response?.data?.message || error.message}`, 
        'error'
      );
      setRequests([]); // Set empty array on error
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [tabValue, retryCount]);

  useEffect(() => {
    fetchAccountRequests();
  }, [fetchAccountRequests]);

  // Manual refresh function
  const handleRefresh = () => {
    fetchAccountRequests(true);
  };

  const handleViewDetails = (request) => {
    setSelectedRequest(request);
    setShowDetailsDialog(true);
  };

  const handleReviewRequest = (request, action) => {
    setSelectedRequest(request);
    setReviewAction(action);
    setRejectionReason('');
    setShowReviewDialog(true);
  };

  const handleSubmitReview = async () => {
    if (reviewAction === 'REJECT' && !rejectionReason.trim()) {
      showSnackbar('Please provide a reason for rejection', 'warning');
      return;
    }

    setProcessing(true);
    try {
      const reviewData = {
        requestId: selectedRequest.requestId,
        action: reviewAction,
        reviewedBy: 'Admin', // You might want to get this from user context
        rejectionReason: reviewAction === 'REJECT' ? rejectionReason : null
      };

      console.log('Submitting review:', reviewData);

      const response = await axios.put(getApiUrl(API_ENDPOINTS.REVIEW_ACCOUNT_REQUEST), reviewData, {
        timeout: 15000, // 15 second timeout for review operations
        headers: {
          'Content-Type': 'application/json',
        }
      });
      
      console.log('Review response:', response.data);
      
      if (response.data.success) {
        if (reviewAction === 'APPROVE') {
          showSnackbar(
            `Account approved successfully! User ${selectedRequest.firstName} ${selectedRequest.lastName} has been created and can now log in using School ID: ${selectedRequest.schoolId}`, 
            'success'
          );
          
          // Emit event to notify other components that a new user was created
          eventEmitter.emit(EVENTS.ACCOUNT_APPROVED, {
            user: selectedRequest,
            message: 'New user account has been approved and created'
          });
          
          // Also emit general refresh event for accounts
          eventEmitter.emit(EVENTS.REFRESH_ACCOUNTS);
          
        } else {
          showSnackbar(response.data.message, 'success');
          
          // Emit event for rejected account
          eventEmitter.emit(EVENTS.ACCOUNT_REJECTED, {
            user: selectedRequest,
            reason: rejectionReason
          });
        }
        setShowReviewDialog(false);
        
        // Auto-refresh the data after successful review
        setTimeout(() => {
          fetchAccountRequests(true);
        }, 1000); // Small delay to ensure backend processing is complete
        
      } else {
        throw new Error(response.data.message || 'Failed to review request');
      }
    } catch (error) {
      console.error('Error reviewing request:', error);
      showSnackbar(
        `Error reviewing request: ${error.response?.data?.message || error.message}`, 
        'error'
      );
    } finally {
      setProcessing(false);
    }
  };

  const showSnackbar = (message, severity) => {
    setSnackbar({ open: true, message, severity });
  };

  const handleSendPendingReminder = async (request) => {
    setRemindingRequestId(request.requestId);
    try {
      const response = await axios.post(
        getApiUrl(API_ENDPOINTS.SEND_PENDING_REQUEST_REMINDER),
        null,
        {
          params: { requestId: request.requestId },
          timeout: 15000,
        }
      );

      if (response.data.success) {
        showSnackbar(`Reminder email sent to ${request.email}`, 'success');
      } else {
        throw new Error(response.data.message || 'Failed to send reminder email');
      }
    } catch (error) {
      console.error('Error sending reminder email:', error);
      showSnackbar(
        `Failed to send reminder email: ${error.response?.data?.message || error.message}`,
        'error'
      );
    } finally {
      setRemindingRequestId(null);
    }
  };

  const getStatusChip = (status) => {
    const statusConfig = {
      PENDING: { color: 'warning', icon: <Schedule fontSize="small" /> },
      APPROVED: { color: 'success', icon: <CheckCircle fontSize="small" /> },
      REJECTED: { color: 'error', icon: <Cancel fontSize="small" /> }
    };

    const config = statusConfig[status] || statusConfig.PENDING;
    
    return (
      <Chip
        label={status}
        color={config.color}
        icon={config.icon}
        variant="outlined"
        size="small"
      />
    );
  };

  const formatDate = (date) => {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
    setRetryCount(0); // Reset retry count when changing tabs
  };

  // Error retry component
  const ErrorRetryComponent = () => (
    <Box 
      display="flex" 
      flexDirection="column"
      justifyContent="center" 
      alignItems="center" 
      minHeight="400px"
      sx={{ bgcolor: darkMode ? 'var(--background-primary)' : 'background.paper' }}
    >
      <Typography variant="h6" color="error" gutterBottom>
        Failed to load account requests
      </Typography>
      <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
        {error?.response?.status === 500 ? 
          'Server error occurred. The server might be starting up.' : 
          error?.message || 'An unexpected error occurred'
        }
      </Typography>
      {retryCount > 0 && (
        <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
          Retrying... (Attempt {retryCount}/3)
        </Typography>
      )}
      <Button 
        variant="contained" 
        onClick={() => {
          setRetryCount(0);
          fetchAccountRequests();
        }}
        startIcon={<Refresh />}
      >
        Retry
      </Button>
    </Box>
  );

  if (loading && !refreshing) {
    return (
      <Box 
        display="flex" 
        justifyContent="center" 
        alignItems="center" 
        minHeight="400px"
        sx={{ bgcolor: darkMode ? 'var(--background-primary)' : 'background.paper' }}
      >
        <Box display="flex" flexDirection="column" alignItems="center">
          <CircularProgress />
          <Typography variant="body2" sx={{ mt: 2, color: darkMode ? 'var(--text-secondary)' : 'text.secondary' }}>
            Loading account requests...
          </Typography>
        </Box>
      </Box>
    );
  }

  if (error && !loading && retryCount >= 3) {
    return <ErrorRetryComponent />;
  }

  return (
    <Box 
      sx={{ 
        p: 3, 
        bgcolor: darkMode ? 'var(--background-primary)' : 'background.paper',
        minHeight: '100vh',
        color: darkMode ? 'var(--text-primary)' : 'text.primary'
      }}
      className={darkMode ? 'dark-mode' : ''}
    >
              {/* Header */}
        <Box sx={{ mb: 3 }}>
          <Box sx={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'flex-start',
            mb: 1
          }}>
            <Typography 
              variant="h4" 
              fontWeight="600" 
              sx={{ 
                color: darkMode ? 'var(--text-primary)' : '#1E293B',
                display: 'flex',
                alignItems: 'center',
                gap: 2
              }}
            >
              <PersonAdd />
              Account Requests
            </Typography>
            
            <Button
              variant="outlined"
              startIcon={refreshing ? <CircularProgress size={16} /> : <Refresh />}
              onClick={handleRefresh}
              disabled={refreshing || loading}
              sx={{
                color: darkMode ? 'var(--accent-color)' : 'primary.main',
                borderColor: darkMode ? 'var(--accent-color)' : 'primary.main',
                '&:hover': {
                  backgroundColor: darkMode ? 'rgba(var(--accent-color-rgb), 0.1)' : 'rgba(25, 118, 210, 0.04)'
                }
              }}
            >
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </Button>
          </Box>
          
          <Typography 
            variant="body1" 
            sx={{ 
              color: darkMode ? 'var(--text-secondary)' : 'text.secondary',
              mb: 2
            }}
          >
            Review and manage account creation requests from mobile app users
          </Typography>

                  {/* Tabs */}
          <Tabs 
            value={tabValue} 
            onChange={handleTabChange} 
            sx={{ 
              borderBottom: 1, 
              borderColor: darkMode ? 'var(--border-color)' : 'divider',
              mb: 3
            }}
          >
            <Tab 
              label={`Pending Requests ${!loading && !error ? `(${tabValue === 0 ? requests.length : requests.filter(r => r.status === 'PENDING').length})` : ''}`} 
              sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}
            />
            <Tab 
              label={`All Requests ${!loading && !error ? `(${tabValue === 1 ? requests.length : 0})` : ''}`} 
              sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}
            />
          </Tabs>
      </Box>

      {/* Requests Table */}
      <Card sx={{ 
        boxShadow: darkMode ? 'var(--shadow)' : '0 4px 20px rgba(0,0,0,0.05)',
        border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)',
        bgcolor: darkMode ? 'var(--card-bg)' : 'background.paper',
        position: 'relative'
      }}>
        {refreshing && (
          <Box
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundColor: 'rgba(0,0,0,0.1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 1000
            }}
          >
            <Box sx={{ textAlign: 'center' }}>
              <CircularProgress size={24} />
              <Typography variant="body2" sx={{ mt: 1, color: darkMode ? 'var(--text-primary)' : 'text.primary' }}>
                Refreshing...
              </Typography>
            </Box>
          </Box>
        )}
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow sx={{ 
                bgcolor: darkMode ? 'var(--table-header-bg)' : '#F8FAFC'
              }}>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  Last Name
                </TableCell>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  First Name
                </TableCell>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  School ID
                </TableCell>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  Email
                </TableCell>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  Department
                </TableCell>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  Status
                </TableCell>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  Request Date
                </TableCell>
                <TableCell sx={{ 
                  fontWeight: 600, 
                  color: darkMode ? 'var(--text-primary)' : '#334155'
                }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {requests.length === 0 ? (
                <TableRow>
                  <TableCell 
                    colSpan={8} 
                    align="center" 
                    sx={{ 
                      py: 4,
                      color: darkMode ? 'var(--text-secondary)' : 'text.secondary'
                    }}
                  >
                    <Typography variant="body1">
                      {tabValue === 0 ? 'No pending requests found' : 'No account requests found'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                requests.map((request) => (
                  <TableRow 
                    key={request.requestId}
                    sx={{ 
                      '&:hover': { 
                        bgcolor: darkMode ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)' 
                      },
                      borderBottom: darkMode ? '1px solid var(--table-border)' : '1px solid #E2E8F0'
                    }}
                  >
                    <TableCell sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}>
                      {request.lastName}
                    </TableCell>
                    <TableCell sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}>
                      {request.firstName}
                    </TableCell>
                    <TableCell sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}>
                      {request.schoolId}
                    </TableCell>
                    <TableCell sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}>
                      {request.email}
                    </TableCell>
                    <TableCell sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}>
                      {request.department}
                    </TableCell>
                    <TableCell>
                      {getStatusChip(request.status)}
                    </TableCell>
                    <TableCell sx={{ color: darkMode ? 'var(--text-secondary)' : 'text.secondary' }}>
                      {formatDate(request.requestDate)}
                    </TableCell>
                    <TableCell>
                      <Box display="flex" gap={1}>
                        <Tooltip title="View Details">
                          <IconButton
                            size="small"
                            onClick={() => handleViewDetails(request)}
                            sx={{ color: darkMode ? 'var(--accent-color)' : 'primary.main' }}
                          >
                            <Visibility fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        
                        {request.status === 'PENDING' && (
                          <>
                            <Tooltip title="Approve">
                              <IconButton
                                size="small"
                                onClick={() => handleReviewRequest(request, 'APPROVE')}
                                sx={{ color: '#10b981' }}
                              >
                                <CheckCircle fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            
                            <Tooltip title="Reject">
                              <IconButton
                                size="small"
                                onClick={() => handleReviewRequest(request, 'REJECT')}
                                sx={{ color: '#ef4444' }}
                              >
                                <Cancel fontSize="small" />
                              </IconButton>
                            </Tooltip>

                            <Tooltip title="Send Pending Reminder Email">
                              <span>
                                <IconButton
                                  size="small"
                                  onClick={() => handleSendPendingReminder(request)}
                                  sx={{ color: darkMode ? 'var(--accent-color)' : 'primary.main' }}
                                  disabled={remindingRequestId === request.requestId}
                                >
                                  {remindingRequestId === request.requestId ? (
                                    <CircularProgress size={16} />
                                  ) : (
                                    <Outbox fontSize="small" />
                                  )}
                                </IconButton>
                              </span>
                            </Tooltip>
                          </>
                        )}
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* Details Dialog */}
      <Dialog 
        open={showDetailsDialog} 
        onClose={() => setShowDetailsDialog(false)}
        maxWidth="md"
        fullWidth
        PaperProps={{
          sx: {
            bgcolor: darkMode ? 'var(--card-bg)' : 'background.paper',
            color: darkMode ? 'var(--text-primary)' : 'text.primary'
          }
        }}
      >
        <DialogTitle sx={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          borderBottom: darkMode ? '1px solid var(--border-color)' : '1px solid #E2E8F0'
        }}>
          <Typography variant="h6" fontWeight="600">
            Account Request Details
          </Typography>
          <IconButton onClick={() => setShowDetailsDialog(false)}>
            <Close />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          {selectedRequest && (
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card sx={{ 
                  p: 2, 
                  bgcolor: darkMode ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)',
                  border: darkMode ? '1px solid var(--border-color)' : '1px solid #E2E8F0'
                }}>
                  <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Person /> Personal Information
                  </Typography>
                  <Box sx={{ pl: 2 }}>
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>Full Name:</strong> {selectedRequest.firstName} {selectedRequest.lastName}
                    </Typography>
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>Email:</strong> {selectedRequest.email}
                    </Typography>
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>School ID:</strong> {selectedRequest.schoolId}
                    </Typography>
                    <Typography variant="body2">
                      <strong>Department:</strong> {selectedRequest.department}
                    </Typography>
                  </Box>
                </Card>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Card sx={{ 
                  p: 2, 
                  bgcolor: darkMode ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)',
                  border: darkMode ? '1px solid var(--border-color)' : '1px solid #E2E8F0'
                }}>
                  <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Schedule /> Request Information
                  </Typography>
                  <Box sx={{ pl: 2 }}>
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>Status:</strong> {getStatusChip(selectedRequest.status)}
                    </Typography>
                    <Typography variant="body2" sx={{ mb: 1 }}>
                      <strong>Request Date:</strong> {formatDate(selectedRequest.requestDate)}
                    </Typography>
                    {selectedRequest.reviewDate && (
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        <strong>Review Date:</strong> {formatDate(selectedRequest.reviewDate)}
                      </Typography>
                    )}
                    {selectedRequest.reviewedBy && (
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        <strong>Reviewed By:</strong> {selectedRequest.reviewedBy}
                      </Typography>
                    )}
                    {selectedRequest.rejectionReason && (
                      <Typography variant="body2" sx={{ color: '#ef4444' }}>
                        <strong>Rejection Reason:</strong> {selectedRequest.rejectionReason}
                      </Typography>
                    )}
                  </Box>
                </Card>
              </Grid>
            </Grid>
          )}
        </DialogContent>
      </Dialog>

      {/* Review Dialog */}
      <Dialog 
        open={showReviewDialog} 
        onClose={() => setShowReviewDialog(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: {
            bgcolor: darkMode ? 'var(--card-bg)' : 'background.paper',
            color: darkMode ? 'var(--text-primary)' : 'text.primary'
          }
        }}
      >
        <DialogTitle sx={{ 
          borderBottom: darkMode ? '1px solid var(--border-color)' : '1px solid #E2E8F0'
        }}>
          {reviewAction === 'APPROVE' ? 'Approve Account Request' : 'Reject Account Request'}
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          {selectedRequest && (
            <Box>
              <Typography variant="body1" sx={{ mb: 2 }}>
                Are you sure you want to {reviewAction.toLowerCase()} the account request for{' '}
                <strong>{selectedRequest.firstName} {selectedRequest.lastName}</strong>?
              </Typography>
              
              {reviewAction === 'APPROVE' && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  This will create a new user account in Firebase, add them to the users database, and send a confirmation email to the applicant. 
                  The user will then be able to log in using their School ID and password in the mobile app.
                </Alert>
              )}
              
              {reviewAction === 'REJECT' && (
                <Box>
                  <Alert severity="warning" sx={{ mb: 2 }}>
                    This will reject the account request and send a notification email to the applicant.
                  </Alert>
                  <TextField
                    fullWidth
                    multiline
                    rows={3}
                    label="Reason for Rejection *"
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    placeholder="Please provide a clear reason for rejecting this request..."
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        '& fieldset': {
                          borderColor: darkMode ? 'var(--border-color)' : '#E2E8F0',
                        },
                      },
                    }}
                  />
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0 }}>
          <Button 
            onClick={() => setShowReviewDialog(false)}
            disabled={processing}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSubmitReview}
            disabled={processing || (reviewAction === 'REJECT' && !rejectionReason.trim())}
            variant="contained"
            color={reviewAction === 'APPROVE' ? 'success' : 'error'}
            startIcon={processing ? <CircularProgress size={16} /> : null}
          >
            {processing ? 'Processing...' : reviewAction === 'APPROVE' ? 'Approve' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert 
          onClose={() => setSnackbar({ ...snackbar, open: false })} 
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default AccountRequests; 