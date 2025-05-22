import { useState, useEffect } from 'react';
import { 
  Avatar, 
  IconButton, 
  Dialog, 
  DialogTitle, 
  DialogContent, 
  DialogActions, 
  Button,
  Box,
  Typography,
  CircularProgress,
  Snackbar,
  Alert,
  Paper,
  Divider,
  useTheme,
  styled
} from '@mui/material';
import { Edit, CloudUpload, InsertPhoto } from '@mui/icons-material';
import { 
  uploadProfilePicture, 
  deleteOldProfilePicture, 
  canUpdateProfilePicture 
} from '../firebase/profileUtils';
import axios from 'axios';
import './ProfilePicture.css';
import { useUser } from '../contexts/UserContext';

// Styled components for drag and drop
const DropZone = styled(Paper)(({ theme, isDragActive }) => ({
  padding: theme.spacing(3),
  border: '2px dashed',
  borderColor: isDragActive ? theme.palette.primary.main : theme.palette.divider,
  borderRadius: theme.shape.borderRadius,
  backgroundColor: isDragActive ? theme.palette.primary.lighter || 'rgba(3, 169, 244, 0.04)' : theme.palette.background.default,
  textAlign: 'center',
  cursor: 'pointer',
  transition: 'all 0.3s ease',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  minHeight: '200px',
  width: '100%',
  '&:hover': {
    borderColor: theme.palette.primary.main,
    backgroundColor: theme.palette.primary.lighter || 'rgba(3, 169, 244, 0.04)'
  }
}));

/**
 * Reusable Profile Picture component with upload functionality
 * @param {Object} props - Component props
 * @param {string} props.userId - User ID
 * @param {string} props.src - Current avatar image source
 * @param {number} props.size - Avatar size in pixels
 * @param {Function} props.onPictureChange - Callback after successful picture change
 * @param {boolean} props.editable - Whether the avatar can be edited
 */
const ProfilePicture = ({ 
  userId, 
  src = null, 
  size = 120, 
  onPictureChange, 
  editable = true 
}) => {
  const theme = useTheme();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [loading, setLoading] = useState(false);
  const [isDragActive, setIsDragActive] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'info'
  });
  
  // Use the user context
  const { updateProfilePicture: updateGlobalProfilePicture } = useUser();

  // Check Firebase auth on mount
  useEffect(() => {
    const checkFirebaseAccess = async () => {
      try {
        // Try to access a small file or test storage to verify permissions
        // This is just a check - it should fail gracefully if there are issues
        const token = localStorage.getItem('token');
        if (!token) {
          console.log('No auth token found, users may have limited storage access');
        }
      } catch (error) {
        console.warn('Firebase storage access check failed:', error);
      }
    };
    
    checkFirebaseAccess();
  }, []);

  // Handle file selection
  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
      // Show preview in the same dialog instead of opening a new one
      // setUploadDialogOpen(true);
    }
  };

  // Handle drag events
  const handleDragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);
    
    const file = e.dataTransfer.files[0];
    if (file && (file.type === 'image/jpeg' || file.type === 'image/png')) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
      // Show preview in the same dialog instead of opening a new one
      // setUploadDialogOpen(true);
    } else {
      setSnackbar({
        open: true,
        message: 'Please select a valid JPG or PNG file',
        severity: 'error'
      });
    }
  };

  // Close upload dialog
  const handleCloseUploadDialog = () => {
    setUploadDialogOpen(false);
    URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setSelectedFile(null);
  };

  // Reset file selection
  const handleCancelFileSelection = () => {
    URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setSelectedFile(null);
  };

  // Open edit dialog
  const handleEditClick = () => {
    // Check if user can update before opening dialog
    if (userId) {
      const { canUpdate, remainingDays } = canUpdateProfilePicture(userId);
      if (!canUpdate) {
        setSnackbar({
          open: true,
          message: `You can only change your profile picture every 3 days. Please wait ${remainingDays} more day(s).`,
          severity: 'warning'
        });
        return;
      }
    }
    setDialogOpen(true);
    setPreviewUrl(null);
    setSelectedFile(null);
  };

  // Upload selected picture with enhanced error handling
  const handleUpload = async () => {
    if (!selectedFile || !userId) return;
    
    setLoading(true);
    try {
      // Check authentication status
      const token = localStorage.getItem('token');
      if (!token) {
        // Still try to upload, but warn the user
        console.warn('No authentication token found, upload may fail');
      }
      
      // Delete previous profile picture if it exists
      try {
        await deleteOldProfilePicture(userId);
      } catch (deleteError) {
        // Continue even if delete fails - just log it
        console.warn('Could not delete old profile picture:', deleteError);
      }
      
      // Upload new profile picture
      const downloadUrl = await uploadProfilePicture(userId, selectedFile);
      
      // Update in global context
      updateGlobalProfilePicture(downloadUrl);
      
      // Update the user record in the database
      try {
        await axios.put(`http://localhost:8080/api/user/updateProfilePicture/${userId}`, {
          profilePictureUrl: downloadUrl
        });
      } catch (apiError) {
        console.error('Error updating profile picture URL in database:', apiError);
        // Continue with the function as the upload was successful
      }
      
      // Close dialogs
      handleCloseUploadDialog();
      setDialogOpen(false);
      
      // Call the callback with the new URL
      if (onPictureChange) {
        onPictureChange(downloadUrl);
      }
      
      // Show success message
      setSnackbar({
        open: true,
        message: 'Profile picture updated successfully!',
        severity: 'success'
      });
    } catch (error) {
      console.error('Error uploading profile picture:', error);
      
      // Format user-friendly error message
      let errorMessage = 'Failed to upload profile picture';
      
      // Check for specific Firebase Storage errors
      if (error.code === 'storage/unauthorized' || 
          (error.message && error.message.includes('permission')) || 
          (error.message && error.message.includes('unauthorized'))) {
        errorMessage = 'Storage access unauthorized. Please check your Firebase storage rules or sign in again.';
        
        // Suggest solutions
        console.error('Firebase storage permission error details:', error);
        console.info('Suggestions to fix: 1) Check Firebase storage rules 2) Verify user is authenticated 3) Confirm correct storage bucket');
      } else if (error.code === 'storage/quota-exceeded') {
        errorMessage = 'Storage quota exceeded. Please contact an administrator.';
      } else if (error.code === 'storage/invalid-url') {
        errorMessage = 'Invalid storage URL. The storage configuration may be incorrect.';
      } else if (error.code === 'storage/retry-limit-exceeded') {
        errorMessage = 'Network error. Please check your connection and try again.';
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setSnackbar({
        open: true,
        message: errorMessage,
        severity: 'error'
      });
    } finally {
      setLoading(false);
    }
  };

  // Close snackbar
  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  return (
    <>
      <Box sx={{ position: 'relative' }} className="profile-picture-container">
        <Avatar
          src={src}
          sx={{ 
            width: size, 
            height: size,
            bgcolor: 'primary.light',
            color: 'white',
            fontSize: size/2,
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)'
          }}
          className="profile-picture-avatar"
        >
          {/* Default avatar text - first letter of user ID or 'U' */}
          {userId ? userId.charAt(0).toUpperCase() : 'U'}
        </Avatar>
        
        {editable && (
          <IconButton
            sx={{
              position: 'absolute',
              bottom: 2,
              right: 2,
              bgcolor: 'primary.main',
              color: 'white',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.2)',
              transition: 'transform 0.2s ease',
              '&:hover': { 
                bgcolor: 'primary.dark',
                transform: 'scale(1.1)'
              }
            }}
            className="profile-picture-edit-button"
            onClick={handleEditClick}
          >
            <Edit sx={{ fontSize: 18 }} />
          </IconButton>
        )}
      </Box>

      {/* Enhanced Edit Profile Picture Dialog with integrated preview */}
      <Dialog 
        open={dialogOpen} 
        onClose={() => {
          setDialogOpen(false);
          handleCancelFileSelection();
        }}
        maxWidth="sm"
        PaperProps={{
          sx: {
            borderRadius: '12px',
            overflow: 'hidden'
          }
        }}
      >
        <DialogTitle sx={{ 
          bgcolor: 'primary.main', 
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <InsertPhoto /> Change Profile Picture
        </DialogTitle>
        
        <DialogContent sx={{ pt: 3, pb: 2, px: 3 }}>
          {!previewUrl ? (
            <>
              <Typography variant="body1" color="text.primary" sx={{ mb: 1 }}>
                Select a JPG or PNG file (maximum 3MB).
              </Typography>
              
              <Box sx={{ 
                bgcolor: theme.palette.warning.lighter || '#FFF9C4', 
                p: 2, 
                borderRadius: 1, 
                mb: 3,
                display: 'flex',
                alignItems: 'center',
                gap: 1
              }}>
                <Typography variant="body2" color="text.secondary">
                  <strong>Note:</strong> You can only change your profile picture once every 3 days.
                </Typography>
              </Box>
              
              {/* Drag and drop zone */}
              <DropZone
                isDragActive={isDragActive}
                onDragEnter={handleDragEnter}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                elevation={0}
                component="label"
                htmlFor="profile-picture-input"
                className="dropzone"
                sx={{ 
                  mb: 2,
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  alignItems: 'center',
                  minHeight: '200px'
                }}
              >
                <input
                  type="file"
                  id="profile-picture-input"
                  hidden
                  accept="image/jpeg,image/png,image/jpg"
                  onChange={handleFileSelect}
                  className="hidden-file-input"
                />
                <CloudUpload 
                  color="primary" 
                  className="dropzone-icon"
                  sx={{ 
                    fontSize: 48, 
                    mb: 2,
                    opacity: 0.8
                  }} 
                />
                <Typography 
                  variant="body1" 
                  color="text.primary" 
                  gutterBottom 
                  align="center"
                  className="dropzone-text"
                >
                  Drag & Drop your image here
                </Typography>
                <Typography 
                  variant="body2" 
                  color="text.secondary" 
                  gutterBottom 
                  align="center"
                  className="dropzone-or"
                >
                  or
                </Typography>
                <Button
                  variant="contained"
                  color="primary"
                  component="span"
                  className="dropzone-button"
                  sx={{ mt: 1 }}
                >
                  Browse Files
                </Button>
              </DropZone>
              
              <Divider sx={{ my: 2 }} />
              
              <Box sx={{ 
                bgcolor: 'background.paper',
                borderRadius: '8px',
                p: 2,
                textAlign: 'center'
              }}>
                <Typography variant="caption" color="text.secondary">
                  Supported file types: JPG, PNG
                </Typography>
              </Box>
            </>
          ) : (
            /* Preview section */
            <>
              <Typography variant="body1" color="text.primary" sx={{ mb: 2 }} align="center">
                Preview Your New Profile Picture
              </Typography>
              
              <Box sx={{ 
                display: 'flex', 
                flexDirection: 'column',
                alignItems: 'center', 
                mb: 3,
                padding: 2,
                bgcolor: theme.palette.grey[50],
                borderRadius: '8px',
                border: `1px solid ${theme.palette.divider}`
              }} className="preview-container">
                <Avatar
                  src={previewUrl}
                  sx={{ 
                    width: 200, 
                    height: 200,
                    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
                    mb: 2,
                    border: '4px solid white'
                  }}
                  className="preview-avatar"
                />
                <Typography variant="body1" fontWeight="medium" color="text.primary">
                  {selectedFile?.name}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {selectedFile ? `${(selectedFile.size / (1024 * 1024)).toFixed(2)} MB` : ''}
                </Typography>
              </Box>
              
              <Typography variant="body2" color="text.primary" sx={{ mb: 1 }} align="center">
                Do you want to use this as your new profile picture?
              </Typography>
              
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, mt: 2 }}>
                <Button
                  variant="outlined"
                  color="inherit"
                  onClick={handleCancelFileSelection}
                  startIcon={<Edit />}
                >
                  Choose Another
                </Button>
              </Box>
            </>
          )}
        </DialogContent>
        
        <DialogActions sx={{ px: 3, py: 2, bgcolor: 'background.paper' }}>
          <Button 
            onClick={() => {
              setDialogOpen(false);
              handleCancelFileSelection();
            }}
            variant="outlined"
            color="inherit"
          >
            Cancel
          </Button>
          
          {previewUrl && (
            <Button 
              onClick={handleUpload} 
              color="primary" 
              variant="contained"
              disabled={loading}
              startIcon={loading ? <CircularProgress size={20} /> : null}
            >
              {loading ? 'Uploading...' : 'Apply'}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Preview and Confirm Dialog - keeping for backward compatibility but not using it */}
      <Dialog 
        open={uploadDialogOpen} 
        onClose={handleCloseUploadDialog}
        maxWidth="sm"
        PaperProps={{
          sx: {
            borderRadius: '12px',
            overflow: 'hidden'
          }
        }}
      >
        <DialogTitle sx={{ 
          bgcolor: 'primary.main', 
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <InsertPhoto /> Confirm New Profile Picture
        </DialogTitle>
        
        <DialogContent sx={{ pt: 3, px: 3 }}>
          {previewUrl && (
            <Box sx={{ 
              display: 'flex', 
              flexDirection: 'column',
              alignItems: 'center', 
              mb: 3,
              padding: 2,
              bgcolor: theme.palette.background.paper,
              borderRadius: '8px'
            }} className="preview-container">
              <Avatar
                src={previewUrl}
                sx={{ 
                  width: 200, 
                  height: 200,
                  boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
                  mb: 2,
                  border: '4px solid white'
                }}
                className="preview-avatar"
              />
              <Typography variant="body1" fontWeight="medium" color="text.primary">
                {selectedFile?.name}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {selectedFile ? `${(selectedFile.size / (1024 * 1024)).toFixed(2)} MB` : ''}
              </Typography>
            </Box>
          )}
          <Typography variant="body1" color="text.primary" align="center">
            Do you want to use this as your new profile picture?
          </Typography>
        </DialogContent>
        
        <DialogActions sx={{ px: 3, py: 2, bgcolor: 'background.paper' }}>
          <Button 
            onClick={handleCloseUploadDialog} 
            disabled={loading}
            variant="outlined"
            color="inherit"
          >
            Cancel
          </Button>
          <Button 
            onClick={handleUpload} 
            color="primary" 
            variant="contained"
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} /> : null}
          >
            {loading ? 'Uploading...' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Notification Snackbar */}
      <Snackbar 
        open={snackbar.open} 
        autoHideDuration={6000} 
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity={snackbar.severity}
          variant="filled"
          sx={{ boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)', borderRadius: '8px' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
};

export default ProfilePicture; 