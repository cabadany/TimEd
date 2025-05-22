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
  Stack,
  FormHelperText
} from '@mui/material';
import { 
  MoreVert, 
  FilterList, 
  Search, 
  CheckCircle, 
  Cancel, 
  AccessTime,
  Add,
  AttachFile,
  OpenInNew
} from '@mui/icons-material';
import { getDatabase, ref, onValue, query, orderByChild, get, update, push, child } from 'firebase/database';
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

  // Function to validate letter data for display
  const isValidLetterForDisplay = (letter) => {
    return (
      letter &&
      letter.firstName && 
      letter.firstName.trim() !== '' && 
      letter.firstName.toLowerCase() !== 'unknown' &&
      letter.idNumber && 
      letter.idNumber.trim() !== '' && 
      letter.idNumber.toLowerCase() !== 'n/a' &&
      letter.date && 
      letter.date.trim() !== '' &&
      letter.reason && 
      letter.reason.trim() !== '' &&
      letter.details && 
      letter.details.trim() !== '' &&
      letter.submittedAt &&
      letter.status
    );
  };

  // Fetch excuse letters
  const fetchExcuseLetters = async (pageNumber = page, pageSize = rowsPerPage) => {
    try {
      setLoading(true);
      const db = getDatabase();
      const excuseLettersRef = ref(db, 'excuseLetters');
      
      // Get all excuse letters
      const snapshot = await get(excuseLettersRef);
      
      if (snapshot.exists()) {
        const allLetters = [];
        
        // Convert the nested structure into a flat array
        snapshot.forEach((userSnapshot) => {
          const userId = userSnapshot.key;
          
          userSnapshot.forEach((letterSnapshot) => {
            const letter = letterSnapshot.val();
            // Only add valid letters
            if (isValidLetterForDisplay(letter)) {
              allLetters.push({
                id: letterSnapshot.key,
                userId: userId,
                ...letter
              });
            }
          });
        });
        
        // Sort letters by submittedAt in descending order
        allLetters.sort((a, b) => b.submittedAt - a.submittedAt);
        
        // Filter by status if needed
        let filteredLetters = allLetters;
        if (statusFilter && statusFilter !== 'All') {
          filteredLetters = allLetters.filter(letter => letter.status === statusFilter);
        }
        
        // Filter by date if needed
        if (startDate && endDate) {
          const startTimestamp = startDate.getTime();
          const endTimestamp = endDate.getTime();
          filteredLetters = filteredLetters.filter(letter => {
            const letterTimestamp = letter.submittedAt;
            return letterTimestamp >= startTimestamp && letterTimestamp <= endTimestamp;
          });
        }
        
        // Handle pagination
        const start = pageNumber * pageSize;
        const paginatedLetters = filteredLetters.slice(start, start + pageSize);
        
        setLetters(paginatedLetters);
        setTotalElements(filteredLetters.length);
      } else {
        setLetters([]);
        setTotalElements(0);
      }
      
      setLoading(false);
    } catch (error) {
      console.error("Error fetching excuse letters:", error);
      setLoading(false);
      setSnackbar({
        open: true,
        message: `Failed to load excuse letters: ${error.message}`,
        severity: 'error'
      });
      setLetters([]);
      setTotalElements(0);
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

  // Update the formatDate function to handle invalid dates better
  const formatDate = (timestamp) => {
    if (!timestamp) return 'No date';
    try {
      const date = new Date(timestamp);
      if (isNaN(date.getTime())) return 'Invalid date';
      return format(date, 'MMM dd, yyyy hh:mm a');
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
    
    // Find and set the selected letter
    const letter = letters.find(l => l.id === currentLetterId);
    if (letter) {
      setSelectedLetter(letter);
    }
    
    setOpenStatusDialog(true);
    
    // Don't call handleMenuClose() here as it clears currentLetterId
    // Instead, just close the menu without clearing the ID
    setAnchorEl(null);
  };

  // Close status update dialog
  const handleCloseStatusDialog = () => {
    setOpenStatusDialog(false);
    setRejectionReason(''); // Clear rejection reason when dialog closes
    setCurrentLetterId(null); // Clear the current letter ID
    setSelectedLetter(null); // Clear the selected letter
  };

  // Update letter status
  const handleUpdateStatus = async () => {
    if (!currentLetterId || !selectedLetter) {
      console.error("No letter selected for status update");
      console.log(currentLetterId);
      console.log(selectedLetter);
      setSnackbar({
        open: true,
        message: "Error: No letter selected",
        severity: 'error'
      });
      setOpenStatusDialog(false);
      return;
    }

    try {
      const db = getDatabase();
      const letterRef = ref(db, `excuseLetters/${selectedLetter.userId}/${currentLetterId}`);
      
      // Update the status
      const updates = {
        status: newStatus
      };
      
      // Add rejection reason if status is 'Rejected'
      if (newStatus === 'Rejected' && rejectionReason) {
        updates.rejectionReason = rejectionReason;
      }
      
      await update(letterRef, updates);
      
      // Refresh the data
      fetchExcuseLetters();
      
      // Close the dialog
      setOpenStatusDialog(false);
      
      // Show success message
      setSnackbar({
        open: true,
        message: `Status updated to ${newStatus}`,
        severity: 'success'
      });
    } catch (error) {
      console.error("Error updating status:", error);
      setSnackbar({
        open: true,
        message: `Failed to update status: ${error.message}`,
        severity: 'error'
      });
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

  // Function to validate excuse letter data
  const validateExcuseLetter = (letter) => {
    const errors = [];
    
    // Check User ID
    if (!letter.userId || letter.userId.trim() === '') {
      errors.push('User ID is required');
    }
    
    // Check ID Number
    if (!letter.idNumber || letter.idNumber.trim() === '') {
      errors.push('ID Number is required');
    } else if (letter.idNumber.trim().toLowerCase() === 'n/a') {
      errors.push('ID Number cannot be "N/A"');
    }
    
    // Check First Name
    if (!letter.firstName || letter.firstName.trim() === '') {
      errors.push('First Name is required');
    } else if (letter.firstName.trim().toLowerCase() === 'unknown') {
      errors.push('First Name cannot be "Unknown"');
    }
    
    // Check Date
    if (!letter.date || letter.date.trim() === '') {
      errors.push('Date is required');
    } else {
      // Validate date format (dd/MM/yyyy)
      const dateRegex = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/;
      if (!dateRegex.test(letter.date.trim())) {
        errors.push('Invalid date format. Use dd/MM/yyyy');
      } else {
        const [, day, month, year] = letter.date.match(dateRegex);
        const date = new Date(year, month - 1, day);
        if (isNaN(date.getTime()) || date.getDate() !== parseInt(day) || 
            date.getMonth() !== parseInt(month) - 1 || date.getFullYear() !== parseInt(year)) {
          errors.push('Invalid date');
        }
      }
    }
    
    // Check Reason
    if (!letter.reason || letter.reason.trim() === '') {
      errors.push('Reason is required');
    }
    
    // Check Details
    if (!letter.details || letter.details.trim() === '') {
      errors.push('Details are required');
    } else if (letter.details.trim().length < 10) {
      errors.push('Details must be at least 10 characters long');
    }
    
    // Check Email
    if (!letter.email || letter.email.trim() === '') {
      errors.push('Email is required');
    } else {
      const emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
      if (!emailRegex.test(letter.email.trim())) {
        errors.push('Invalid email format');
      }
    }
    
    return errors;
  };

  // Function to create a new excuse letter
  const handleCreateLetter = async () => {
    try {
      setLoading(true);
      
      // Validate the letter data
      const validationErrors = validateExcuseLetter(newLetter);
      if (validationErrors.length > 0) {
        setSnackbar({
          open: true,
          message: validationErrors.join(', '),
          severity: 'error'
        });
        setLoading(false);
        return;
      }
      
      const db = getDatabase();
      const letterRef = ref(db, `excuseLetters/${newLetter.userId}`);
      
      // Create new letter data
      const letterData = {
        ...newLetter,
        status: 'Pending',
        submittedAt: Date.now(),
        attachmentUrl: newLetter.attachmentUrl || '',
        department: newLetter.department || 'N/A'
      };
      
      // Push the new letter to generate a unique key
      const newLetterRef = push(letterRef);
      await update(newLetterRef, letterData);
      
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
        reason: "Illness/Medical",
        firstName: "",
        idNumber: "",
        email: "",
        department: "N/A",
        attachmentUrl: ""
      });
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
                    <TableCell>ID Number</TableCell>
                    <TableCell>Name</TableCell>
                    <TableCell>Department</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Reason</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Attachment</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {letters.map((letter) => (
                    <TableRow key={letter.id}>
                      <TableCell>{letter.idNumber}</TableCell>
                      <TableCell>{letter.firstName}</TableCell>
                      <TableCell>{letter.department || 'N/A'}</TableCell>
                      <TableCell>{letter.date}</TableCell>
                      <TableCell>{letter.reason}</TableCell>
                      <TableCell>
                        <Chip
                          label={letter.status}
                          size="small"
                          sx={getStatusChipColor(letter.status)}
                        />
                      </TableCell>
                      <TableCell>
                        {letter.attachmentUrl ? (
                          <IconButton
                            size="small"
                            onClick={() => window.open(letter.attachmentUrl, '_blank')}
                            title="View Attachment"
                          >
                            <OpenInNew fontSize="small" />
                          </IconButton>
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            No attachment
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={(e) => handleMenuOpen(e, letter.id)}
                        >
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
      <Dialog
        open={openViewDialog}
        onClose={handleCloseViewDialog}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          Excuse Letter Details
        </DialogTitle>
        <DialogContent>
          {selectedLetter && (
            <Stack spacing={2}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Student Information
                </Typography>
                <Typography variant="body1">
                  {selectedLetter.firstName}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  ID: {selectedLetter.idNumber}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Department: {selectedLetter.department || 'N/A'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Email: {selectedLetter.email}
                </Typography>
              </Box>
              
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Excuse Details
                </Typography>
                <Typography variant="body2">
                  <strong>Date:</strong> {selectedLetter.date}
                </Typography>
                <Typography variant="body2">
                  <strong>Days Absent:</strong> {selectedLetter.daysAbsent || 'N/A'}
                </Typography>
                <Typography variant="body2">
                  <strong>Reason:</strong> {selectedLetter.reason}
                </Typography>
                <Typography variant="body2">
                  <strong>Details:</strong> {selectedLetter.details}
                </Typography>
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Supporting Document
                </Typography>
                {selectedLetter.attachmentUrl ? (
                  <Button
                    variant="outlined"
                    startIcon={<AttachFile />}
                    onClick={() => window.open(selectedLetter.attachmentUrl, '_blank')}
                    size="small"
                    sx={{ mt: 1 }}
                  >
                    View Attachment
                  </Button>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    No attachment provided
                  </Typography>
                )}
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Status Information
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                  <Chip
                    label={selectedLetter.status}
                    size="small"
                    sx={getStatusChipColor(selectedLetter.status)}
                  />
                  <Typography variant="body2" color="text.secondary">
                    Submitted on {formatDate(selectedLetter.submittedAt)}
                  </Typography>
                </Box>
                {selectedLetter.status === 'Rejected' && selectedLetter.rejectionReason && (
                  <Typography variant="body2" color="error" sx={{ mt: 1 }}>
                    Rejection Reason: {selectedLetter.rejectionReason}
                  </Typography>
                )}
              </Box>
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseViewDialog}>Close</Button>
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
              error={!newLetter.userId}
              helperText={!newLetter.userId ? "User ID is required" : ""}
            />
            
            <TextField
              name="firstName"
              label="First Name"
              value={newLetter.firstName}
              onChange={handleInputChange}
              fullWidth
              required
              error={!newLetter.firstName || newLetter.firstName.toLowerCase() === 'unknown'}
              helperText={!newLetter.firstName ? "First Name is required" : 
                         newLetter.firstName.toLowerCase() === 'unknown' ? "First Name cannot be 'Unknown'" : ""}
            />
            
            <TextField
              name="idNumber"
              label="ID Number"
              value={newLetter.idNumber}
              onChange={handleInputChange}
              fullWidth
              required
              error={!newLetter.idNumber || newLetter.idNumber.toLowerCase() === 'n/a'}
              helperText={!newLetter.idNumber ? "ID Number is required" : 
                         newLetter.idNumber.toLowerCase() === 'n/a' ? "ID Number cannot be 'N/A'" : ""}
            />
            
            <TextField
              name="email"
              label="Email"
              type="email"
              value={newLetter.email}
              onChange={handleInputChange}
              fullWidth
              required
              error={!newLetter.email || !/^[A-Za-z0-9+_.-]+@(.+)$/.test(newLetter.email)}
              helperText={!newLetter.email ? "Email is required" : 
                         !/^[A-Za-z0-9+_.-]+@(.+)$/.test(newLetter.email) ? "Invalid email format" : ""}
            />
            
            <TextField
              name="date"
              label="Date (dd/MM/yyyy)"
              value={newLetter.date}
              onChange={handleInputChange}
              fullWidth
              required
              error={!newLetter.date || !/^\d{1,2}\/\d{1,2}\/\d{4}$/.test(newLetter.date)}
              helperText={!newLetter.date ? "Date is required" : 
                         !/^\d{1,2}\/\d{1,2}\/\d{4}$/.test(newLetter.date) ? "Use format: dd/MM/yyyy" : ""}
            />
            
            <FormControl fullWidth required error={!newLetter.reason}>
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
              error={!newLetter.details || newLetter.details.length < 10}
              helperText={!newLetter.details ? "Details are required" : 
                         newLetter.details.length < 10 ? "Details must be at least 10 characters long" : ""}
            />
            
            <TextField
              name="department"
              label="Department"
              value={newLetter.department}
              onChange={handleInputChange}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCreateDialog(false)}>Cancel</Button>
          <Button 
            onClick={handleCreateLetter}
            variant="contained"
            color="primary"
            disabled={loading || !newLetter.userId || !newLetter.firstName || !newLetter.idNumber || 
                      !newLetter.email || !newLetter.date || !newLetter.reason || !newLetter.details}
          >
            {loading ? <CircularProgress size={24} /> : "Submit"}
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