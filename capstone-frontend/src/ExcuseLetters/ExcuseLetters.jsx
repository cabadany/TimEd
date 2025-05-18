import React, { useState, useEffect } from 'react';
import { 
  Box, 
  Typography, 
  Paper, 
  Button, 
  Table, 
  TableBody, 
  TableCell, 
  TableContainer, 
  TableHead, 
  TableRow,
  TablePagination,
  Chip,
  CircularProgress,
  IconButton,
  Menu,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  TextField,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Alert,
  Snackbar,
  Stack
} from '@mui/material';
import { 
  MoreVert, 
  FilterList, 
  Search, 
  CheckCircle, 
  Cancel, 
  AccessTime,
  Add
} from '@mui/icons-material';
import axios from 'axios';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { format, parseISO } from 'date-fns';

const ExcuseLetters = () => {
  // State variables
  const [letters, setLetters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState('All');
  const [openViewDialog, setOpenViewDialog] = useState(false);
  const [selectedLetter, setSelectedLetter] = useState(null);
  const [anchorEl, setAnchorEl] = useState(null);
  const [currentLetterId, setCurrentLetterId] = useState(null);
  const [openStatusDialog, setOpenStatusDialog] = useState(false);
  const [newStatus, setNewStatus] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);
  const [openDateFilter, setOpenDateFilter] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success'
  });
  const [openCreateDialog, setOpenCreateDialog] = useState(false);
  const [newLetter, setNewLetter] = useState({
    userId: "G1o0sCIanMSxIg9I5s4WfAxd9yD2", // Sample user ID
    eventId: "",
    date: new Date().toLocaleDateString(),
    details: "",
    reason: "Illness/Medical"
  });

  // Fetch excuse letters
  const fetchExcuseLetters = async (pageNumber = page, pageSize = rowsPerPage) => {
    try {
      setLoading(true);
      let url = `http://localhost:8080/api/excuse-letters/getAll?page=${pageNumber}&size=${pageSize}`;
      
      if (statusFilter && statusFilter !== 'All') {
        url += `&status=${statusFilter}`;
      }
      
      if (startDate && endDate) {
        const formattedStartDate = format(startDate, 'yyyy-MM-dd');
        const formattedEndDate = format(endDate, 'yyyy-MM-dd');
        url += `&startDate=${formattedStartDate}&endDate=${formattedEndDate}`;
      }
      
      console.log(`Fetching excuse letters from: ${url}`);
      
      try {
        const response = await axios.get(url);
        console.log('API Response:', response.data);
        
        // Make sure content exists and is an array
        const letterContent = Array.isArray(response.data.content) ? response.data.content : [];
        setLetters(letterContent);
        setTotalElements(response.data.totalElements || 0);
      } catch (apiError) {
        console.error("API Error:", apiError);
        
        // If API fails, check if we should display a mock placeholder
        if (apiError.code === 'ERR_NETWORK' || apiError.response?.status === 504) {
          // Mock data for testing UI
          const mockLetters = [
            {
              id: "mock-1",
              userId: "G1o0sCIanMSxIg9I5s4WfAxd9yD2",
              userName: "Test User",
              eventId: "0KP7a1qNMuFZ9jbYArXl",
              eventName: "Sample Event",
              date: "5/18/2025",
              details: "This is a mock excuse letter for testing",
              reason: "Family Emergency",
              status: "Pending",
              submittedAt: Date.now()
            }
          ];
          setLetters(mockLetters);
          setTotalElements(1);
          
          setSnackbar({
            open: true,
            message: "Backend unavailable - showing mock data",
            severity: 'warning'
          });
        } else {
          // For other errors, show error message and empty the letters
          setLetters([]);
          setSnackbar({
            open: true,
            message: `Failed to load excuse letters: ${apiError.response?.data?.message || apiError.message}`,
            severity: 'error'
          });
        }
      }
      
      setLoading(false);
    } catch (error) {
      console.error("Error in fetchExcuseLetters:", error);
      setLoading(false);
      setLetters([]);
    }
  };

  // Initial load
  useEffect(() => {
    fetchExcuseLetters();
  }, []);

  // Refetch when filters change
  useEffect(() => {
    fetchExcuseLetters();
  }, [page, rowsPerPage, statusFilter, startDate, endDate]);

  // Format date for display
  const formatDate = (timestamp) => {
    try {
      return format(new Date(timestamp), 'MMM dd, yyyy hh:mm a');
    } catch (error) {
      return 'Invalid date';
    }
  };

  // Handle page change
  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  // Handle rows per page change
  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  // Handle status filter change
  const handleStatusFilterChange = (event) => {
    setStatusFilter(event.target.value);
    setPage(0);
  };

  // Handle menu open
  const handleMenuOpen = (event, letterId) => {
    setAnchorEl(event.currentTarget);
    setCurrentLetterId(letterId);
  };

  // Handle menu close
  const handleMenuClose = () => {
    setAnchorEl(null);
    setCurrentLetterId(null);
  };

  // Open view dialog
  const handleViewLetter = (letter) => {
    setSelectedLetter(letter);
    setOpenViewDialog(true);
    handleMenuClose();
  };

  // Close view dialog
  const handleCloseViewDialog = () => {
    setOpenViewDialog(false);
    setSelectedLetter(null);
  };

  // Open status update dialog
  const handleOpenStatusDialog = (status) => {
    console.log(`Opening status dialog with letter ID: ${currentLetterId}, status: ${status}`);
    setNewStatus(status);
    setRejectionReason(''); // Reset rejection reason when dialog opens
    setOpenStatusDialog(true);
    
    // Don't call handleMenuClose() here as it clears currentLetterId
    // Instead, just close the menu without clearing the ID
    setAnchorEl(null);
  };

  // Close status update dialog
  const handleCloseStatusDialog = () => {
    setOpenStatusDialog(false);
    setRejectionReason(''); // Clear rejection reason when dialog closes
  };

  // Update letter status
  const handleUpdateStatus = async () => {
    console.log(`Starting status update with letter ID: ${currentLetterId}, status: ${newStatus}`);
    
    if (!currentLetterId) {
      console.error("No letter ID selected for status update");
      setSnackbar({
        open: true,
        message: "Error: No letter selected",
        severity: 'error'
      });
      setOpenStatusDialog(false);
      return;
    }

    try {
      console.log(`Updating excuse letter ${currentLetterId} to status: ${newStatus}`);
      
      // Include rejection reason if status is 'Rejected'
      const url = `http://localhost:8080/api/excuse-letters/${currentLetterId}/status?status=${newStatus}${newStatus === 'Rejected' && rejectionReason ? '&rejectionReason=' + encodeURIComponent(rejectionReason) : ''}`;
      
      const response = await axios.put(
        url,
        null,  // No body needed for this request
        {
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        }
      );
      
      console.log("Status update response:", response.data);
      
      // Check if the response has the expected success property
      if (response.data && response.data.success === true) {
        // Refresh the data
        fetchExcuseLetters();
        
        // Close the dialog
        setOpenStatusDialog(false);
        
        // Show success message
        setSnackbar({
          open: true,
          message: response.data.message || `Status updated to ${newStatus}`,
          severity: 'success'
        });
      } else {
        // Handle unexpected success response format
        console.warn("Unexpected response format:", response.data);
        
        // Still treat as success if we got a 200 OK
        fetchExcuseLetters();
        setOpenStatusDialog(false);
        setSnackbar({
          open: true,
          message: `Status updated to ${newStatus}`,
          severity: 'success'
        });
      }
    } catch (error) {
      console.error("Error updating status:", error);
      
      // Show more detailed error information
      let errorMessage = 'Failed to update status';
      if (error.response) {
        // The server responded with a status code outside the 2xx range
        errorMessage = `Error ${error.response.status}: ${error.response.data?.message || error.message}`;
        console.error('Response data:', error.response.data);
        console.error('Response status:', error.response.status);
      } else if (error.request) {
        // The request was made but no response was received
        errorMessage = 'No response received from server';
        console.error('Request:', error.request);
      }
      
      setSnackbar({
        open: true,
        message: errorMessage,
        severity: 'error'
      });
      
      // Still close the dialog
      setOpenStatusDialog(false);
    }
  };

  // Apply date filters
  const handleApplyDateFilter = () => {
    setPage(0);
    setOpenDateFilter(false);
    fetchExcuseLetters();
  };

  // Clear date filters
  const handleClearDateFilter = () => {
    setStartDate(null);
    setEndDate(null);
    setOpenDateFilter(false);
    fetchExcuseLetters();
  };

  // Get chip color based on status
  const getStatusChipColor = (status) => {
    switch(status) {
      case 'Approved':
        return {
          bgcolor: '#dcfce7',
          color: '#22c55e'
        };
      case 'Rejected':
        return {
          bgcolor: '#fee2e2',
          color: '#ef4444'
        };
      case 'Pending':
      default:
        return {
          bgcolor: '#fef9c3',
          color: '#ca8a04'
        };
    }
  };

  // Handle snackbar close
  const handleCloseSnackbar = () => {
    setSnackbar(prev => ({
      ...prev,
      open: false
    }));
  };

  // Function to handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setNewLetter(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // Function to create a new excuse letter
  const handleCreateLetter = async () => {
    try {
      setLoading(true);
      
      const payload = {
        ...newLetter,
        submittedAt: Date.now()
      };
      
      const response = await axios.post('http://localhost:8080/api/excuse-letters/create', payload);
      
      if (response.data.success) {
        setSnackbar({
          open: true,
          message: 'Excuse letter created successfully',
          severity: 'success'
        });
        
        // Refresh the list
        fetchExcuseLetters();
        
        // Close the dialog
        setOpenCreateDialog(false);
        
        // Reset the form
        setNewLetter({
          userId: "G1o0sCIanMSxIg9I5s4WfAxd9yD2",
          eventId: "",
          date: new Date().toLocaleDateString(),
          details: "",
          reason: "Illness/Medical"
        });
      } else {
        throw new Error(response.data.message || 'Failed to create excuse letter');
      }
    } catch (error) {
      console.error('Error creating excuse letter:', error);
      setSnackbar({
        open: true,
        message: `Failed to create excuse letter: ${error.message}`,
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ maxWidth: '100%', mb: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5" component="h1" fontWeight={600}>
          Excuse Letters
        </Typography>
        
        <Box sx={{ display: 'flex', gap: 2 }}>
          {/* Create Button */}
          <Button
            variant="contained"
            color="primary"
            startIcon={<Add />}
            onClick={() => setOpenCreateDialog(true)}
            size="small"
          >
            Create Test Letter
          </Button>
          
          {/* Status Filter */}
          <FormControl variant="outlined" size="small" sx={{ minWidth: 150 }}>
            <InputLabel id="status-filter-label">Status</InputLabel>
            <Select
              labelId="status-filter-label"
              id="status-filter"
              value={statusFilter}
              onChange={handleStatusFilterChange}
              label="Status"
            >
              <MenuItem value="All">All</MenuItem>
              <MenuItem value="Pending">Pending</MenuItem>
              <MenuItem value="Approved">Approved</MenuItem>
              <MenuItem value="Rejected">Rejected</MenuItem>
            </Select>
          </FormControl>
          
          {/* Date Filter Button */}
          <Button 
            variant="outlined" 
            startIcon={<FilterList />}
            onClick={() => setOpenDateFilter(true)}
            size="small"
          >
            Date Filter
          </Button>
        </Box>
      </Box>
      
      {/* Main Content */}
      <Paper elevation={0} sx={{ width: '100%', borderRadius: 2, border: '1px solid #e0e0e0' }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : letters.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}>
            <Typography color="textSecondary">
              No excuse letters found. Adjust filters or try again later.
            </Typography>
          </Box>
        ) : (
          <>
            <TableContainer>
              <Table sx={{ minWidth: 650 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Submitted by</TableCell>
                    <TableCell>ID Number</TableCell>
                    <TableCell>Date</TableCell>
                 {/*   <TableCell>Event</TableCell>*/}
                    <TableCell>Reason</TableCell>
                    <TableCell>Submitted on</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {letters.map((letter) => (
                    <TableRow key={letter.id} hover>
                      <TableCell>{letter.userName || letter.firstName || 'Unknown'}</TableCell>
                      <TableCell>{letter.idNumber || 'N/A'}</TableCell>
                      <TableCell>{letter.date}</TableCell>
                {/*      <TableCell>{letter.eventName || 'N/A'}</TableCell>*/}
                      <TableCell>{letter.reason}</TableCell>
                      <TableCell>{formatDate(letter.submittedAt)}</TableCell>
                      <TableCell>
                        <Chip 
                          label={letter.status} 
                          size="small" 
                          sx={{
                            ...getStatusChipColor(letter.status),
                            fontWeight: 500,
                            fontSize: '0.75rem'
                          }}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <IconButton onClick={(e) => handleMenuOpen(e, letter.id)}>
                          <MoreVert />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination
              rowsPerPageOptions={[5, 10, 25]}
              component="div"
              count={totalElements}
              rowsPerPage={rowsPerPage}
              page={page}
              onPageChange={handleChangePage}
              onRowsPerPageChange={handleChangeRowsPerPage}
            />
          </>
        )}
      </Paper>
      
      {/* Action Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => {
          const letter = letters.find(l => l.id === currentLetterId);
          if (letter) {
            handleViewLetter(letter);
          }
        }}>
          <Search fontSize="small" sx={{ mr: 1 }} />
          View Details
        </MenuItem>
        <MenuItem onClick={() => handleOpenStatusDialog('Approved')}>
          <CheckCircle fontSize="small" sx={{ mr: 1, color: '#22c55e' }} />
          Approve
        </MenuItem>
        <MenuItem onClick={() => handleOpenStatusDialog('Rejected')}>
          <Cancel fontSize="small" sx={{ mr: 1, color: '#ef4444' }} />
          Reject
        </MenuItem>
        <MenuItem onClick={() => handleOpenStatusDialog('Pending')}>
          <AccessTime fontSize="small" sx={{ mr: 1, color: '#ca8a04' }} />
          Mark as Pending
        </MenuItem>
      </Menu>
      
      {/* View Letter Dialog */}
      <Dialog open={openViewDialog} onClose={handleCloseViewDialog} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 600 }}>Excuse Letter Details</DialogTitle>
        <DialogContent>
          {selectedLetter && (
            <Box sx={{ pt: 1 }}>
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">Submitted By</Typography>
                <Typography variant="body1">{selectedLetter.userName || selectedLetter.firstName || 'Unknown'}</Typography>
              </Box>
              
              {selectedLetter.email && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">Email</Typography>
                  <Typography variant="body1">{selectedLetter.email}</Typography>
                </Box>
              )}
              
              {selectedLetter.idNumber && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">ID Number</Typography>
                  <Typography variant="body1">{selectedLetter.idNumber}</Typography>
                </Box>
              )}
              
              {selectedLetter.department && selectedLetter.department !== 'N/A' && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">Department</Typography>
                  <Typography variant="body1">{selectedLetter.department}</Typography>
                </Box>
              )}
              
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">Date</Typography>
                <Typography variant="body1">{selectedLetter.date}</Typography>
              </Box>
              {/*
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">Event</Typography>
                <Typography variant="body1">{selectedLetter.eventName || 'N/A'}</Typography>
              </Box>*/}
              
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">Reason</Typography>
                <Typography variant="body1">{selectedLetter.reason}</Typography>
              </Box>
              
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">Details</Typography>
                <Typography variant="body1">{selectedLetter.details}</Typography>
              </Box>
              
              {selectedLetter.attachmentUrl && selectedLetter.attachmentUrl !== "" && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">Attachment</Typography>
                  <Button 
                    variant="outlined" 
                    size="small" 
                    component="a" 
                    href={selectedLetter.attachmentUrl} 
                    target="_blank"
                    sx={{ mt: 1 }}
                  >
                    View Attachment
                  </Button>
                </Box>
              )}
              
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">Submitted On</Typography>
                <Typography variant="body1">{formatDate(selectedLetter.submittedAt)}</Typography>
              </Box>
              
              <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2" color="textSecondary">Status</Typography>
                <Chip 
                  label={selectedLetter.status} 
                  size="small" 
                  sx={{
                    ...getStatusChipColor(selectedLetter.status),
                    fontWeight: 500,
                    mt: 0.5
                  }}
                />
              </Box>
              
              {/* Display rejection reason if letter is rejected */}
              {selectedLetter.status === 'Rejected' && selectedLetter.rejectionReason && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="subtitle2" color="textSecondary">Rejection Reason</Typography>
                  <Typography variant="body1" color="error.main">{selectedLetter.rejectionReason}</Typography>
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseViewDialog}>Close</Button>
          {selectedLetter && selectedLetter.status === 'Pending' && (
            <>
              <Button 
                onClick={() => {
                  handleCloseViewDialog();
                  handleOpenStatusDialog('Approved');
                }}
                color="success"
              >
                Approve
              </Button>
              <Button 
                onClick={() => {
                  handleCloseViewDialog();
                  handleOpenStatusDialog('Rejected');
                }}
                color="error"
              >
                Reject
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>
      
      {/* Status Update Dialog */}
      <Dialog open={openStatusDialog} onClose={handleCloseStatusDialog}>
        <DialogTitle>
          {newStatus === 'Approved' ? 'Approve Letter' : 
           newStatus === 'Rejected' ? 'Reject Letter' : 'Update Status'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {newStatus === 'Approved' ? 
              'Are you sure you want to approve this excuse letter?' :
             newStatus === 'Rejected' ? 
              'Are you sure you want to reject this excuse letter?' :
              'Are you sure you want to mark this excuse letter as pending?'}
          </DialogContentText>
          
          {/* Add rejection reason field when rejecting */}
          {newStatus === 'Rejected' && (
            <TextField
              autoFocus
              margin="dense"
              id="rejectionReason"
              label="Reason for Rejection"
              type="text"
              fullWidth
              multiline
              rows={3}
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              sx={{ mt: 2 }}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseStatusDialog}>Cancel</Button>
          <Button 
            onClick={handleUpdateStatus} 
            color={
              newStatus === 'Approved' ? 'success' : 
              newStatus === 'Rejected' ? 'error' : 'primary'
            }
            autoFocus
            disabled={newStatus === 'Rejected' && rejectionReason.trim() === ''}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Date Filter Dialog */}
      <Dialog open={openDateFilter} onClose={() => setOpenDateFilter(false)}>
        <DialogTitle>Filter by Date</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 1, width: 300 }}>
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <Box sx={{ mb: 3 }}>
                <DatePicker
                  label="Start Date"
                  value={startDate}
                  onChange={(newValue) => setStartDate(newValue)}
                  slotProps={{ textField: { fullWidth: true } }}
                />
              </Box>
              <Box>
                <DatePicker
                  label="End Date"
                  value={endDate}
                  onChange={(newValue) => setEndDate(newValue)}
                  minDate={startDate}
                  slotProps={{ textField: { fullWidth: true } }}
                />
              </Box>
            </LocalizationProvider>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClearDateFilter}>Clear</Button>
          <Button onClick={() => setOpenDateFilter(false)}>Cancel</Button>
          <Button 
            onClick={handleApplyDateFilter} 
            variant="contained" 
            disabled={!startDate || !endDate}
          >
            Apply
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Add Create Dialog */}
      <Dialog open={openCreateDialog} onClose={() => setOpenCreateDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Test Excuse Letter</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              name="userId"
              label="User ID"
              value={newLetter.userId}
              onChange={handleInputChange}
              fullWidth
              required
            />
            {/*
            <TextField
              name="eventId"
              label="Event ID (optional)"
              value={newLetter.eventId}
              onChange={handleInputChange}
              fullWidth
            />*/}
            
            <TextField
              name="date"
              label="Date"
              value={newLetter.date}
              onChange={handleInputChange}
              fullWidth
              required
            />
            
            <FormControl fullWidth required>
              <InputLabel>Reason</InputLabel>
              <Select
                name="reason"
                value={newLetter.reason}
                onChange={handleInputChange}
                label="Reason"
              >
                <MenuItem value="Illness/Medical">Illness/Medical</MenuItem>
                <MenuItem value="Family Emergency">Family Emergency</MenuItem>
                <MenuItem value="Transportation Issue">Transportation Issue</MenuItem>
                <MenuItem value="Schedule Conflict">Schedule Conflict</MenuItem>
                <MenuItem value="Other">Other</MenuItem>
              </Select>
            </FormControl>
            
            <TextField
              name="details"
              label="Details"
              value={newLetter.details}
              onChange={handleInputChange}
              fullWidth
              multiline
              rows={4}
              required
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCreateDialog(false)}>Cancel</Button>
          <Button 
            onClick={handleCreateLetter}
            variant="contained"
            color="primary"
            disabled={!newLetter.userId || !newLetter.date || !newLetter.reason || !newLetter.details}
          >
            Submit
          </Button>
        </DialogActions>
      </Dialog>
      
      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={5000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity={snackbar.severity} 
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default ExcuseLetters; 