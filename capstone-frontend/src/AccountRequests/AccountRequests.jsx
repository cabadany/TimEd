import React, { useState, useEffect } from 'react';
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
  Close
} from '@mui/icons-material';
import { useTheme } from '../contexts/ThemeContext';
import axios from 'axios';
import { getApiUrl, API_ENDPOINTS } from '../utils/api';
import './AccountRequests.css';

const AccountRequests = () => {
  const { darkMode } = useTheme();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showDetailsDialog, setShowDetailsDialog] = useState(false);
  const [showReviewDialog, setShowReviewDialog] = useState(false);
  const [reviewAction, setReviewAction] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [processing, setProcessing] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });
  const [tabValue, setTabValue] = useState(0);

  useEffect(() => {
    fetchAccountRequests();
  }, [tabValue]);

  const fetchAccountRequests = async () => {
    setLoading(true);
    try {
      const endpoint = tabValue === 0 ? 
        API_ENDPOINTS.GET_PENDING_ACCOUNT_REQUESTS : 
        API_ENDPOINTS.GET_ALL_ACCOUNT_REQUESTS;
        
      const response = await axios.get(getApiUrl(endpoint));
      
      if (response.data.success) {
        setRequests(response.data.requests || []);
      } else {
        showSnackbar('Failed to fetch account requests', 'error');
      }
    } catch (error) {
      console.error('Error fetching account requests:', error);
      showSnackbar('Error fetching account requests: ' + error.message, 'error');
    } finally {
      setLoading(false);
    }
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

      const response = await axios.put(getApiUrl(API_ENDPOINTS.REVIEW_ACCOUNT_REQUEST), reviewData);
      
      if (response.data.success) {
        showSnackbar(response.data.message, 'success');
        setShowReviewDialog(false);
        fetchAccountRequests(); // Refresh the list
      } else {
        showSnackbar(response.data.message, 'error');
      }
    } catch (error) {
      console.error('Error reviewing request:', error);
      showSnackbar('Error reviewing request: ' + error.message, 'error');
    } finally {
      setProcessing(false);
    }
  };

  const showSnackbar = (message, severity) => {
    setSnackbar({ open: true, message, severity });
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
  };

  if (loading) {
    return (
      <Box 
        display="flex" 
        justifyContent="center" 
        alignItems="center" 
        minHeight="400px"
        sx={{ bgcolor: darkMode ? 'var(--background-primary)' : 'background.paper' }}
      >
        <CircularProgress />
      </Box>
    );
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
        <Typography 
          variant="h4" 
          fontWeight="600" 
          sx={{ 
            mb: 1,
            color: darkMode ? 'var(--text-primary)' : '#1E293B',
            display: 'flex',
            alignItems: 'center',
            gap: 2
          }}
        >
          <PersonAdd />
          Account Requests
        </Typography>
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
            label={`Pending Requests (${tabValue === 0 ? requests.length : 0})`} 
            sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}
          />
          <Tab 
            label="All Requests" 
            sx={{ color: darkMode ? 'var(--text-primary)' : 'text.primary' }}
          />
        </Tabs>
      </Box>

      {/* Requests Table */}
      <Card sx={{ 
        boxShadow: darkMode ? 'var(--shadow)' : '0 4px 20px rgba(0,0,0,0.05)',
        border: darkMode ? '1px solid var(--border-color)' : '1px solid rgba(0,0,0,0.05)',
        bgcolor: darkMode ? 'var(--card-bg)' : 'background.paper'
      }}>
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
                  Name
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
                    colSpan={7} 
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
                      {request.firstName} {request.lastName}
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
                  This will create a new user account and send a confirmation email to the applicant.
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