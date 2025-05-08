import { useState } from 'react';
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
  Alert
} from '@mui/material';
import { Edit } from '@mui/icons-material';
import { 
  uploadProfilePicture, 
  deleteOldProfilePicture, 
  canUpdateProfilePicture 
} from '../firebase/profileUtils';
import axios from 'axios';
import './ProfilePicture.css';
import { useUser } from '../contexts/UserContext';

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
  const [dialogOpen, setDialogOpen] = useState(false);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [loading, setLoading] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'info'
  });
  
  // Use the user context
  const { updateProfilePicture: updateGlobalProfilePicture } = useUser();

  // Handle file selection
  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
      setUploadDialogOpen(true);
    }
  };

  // Close upload dialog
  const handleCloseUploadDialog = () => {
    setUploadDialogOpen(false);
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
  };

  // Upload selected picture
  const handleUpload = async () => {
    if (!selectedFile || !userId) return;
    
    setLoading(true);
    try {
      // Delete previous profile picture if it exists
      await deleteOldProfilePicture(userId);
      
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
      setSnackbar({
        open: true,
        message: error.message || 'Failed to upload profile picture',
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
            fontSize: size/2
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
              bottom: 0,
              right: 0,
              bgcolor: 'primary.main',
              color: 'white',
              '&:hover': { bgcolor: 'primary.dark' }
            }}
            className="profile-picture-edit-button"
            onClick={handleEditClick}
          >
            <Edit sx={{ fontSize: 18 }} />
          </IconButton>
        )}
      </Box>

      {/* Edit Profile Picture Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>Change Profile Picture</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" paragraph>
            Select a JPG or PNG file (maximum 3MB).
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            <strong>Note:</strong> You can only change your profile picture once every 3 days.
          </Typography>
          <Button
            variant="outlined"
            component="label"
            fullWidth
            sx={{ mt: 2 }}
          >
            Choose File
            <input
              type="file"
              hidden
              accept="image/jpeg,image/png,image/jpg"
              onChange={handleFileSelect}
            />
          </Button>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
        </DialogActions>
      </Dialog>

      {/* Preview and Confirm Dialog */}
      <Dialog open={uploadDialogOpen} onClose={handleCloseUploadDialog}>
        <DialogTitle>Confirm New Profile Picture</DialogTitle>
        <DialogContent>
          {previewUrl && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }} className="preview-container">
              <Avatar
                src={previewUrl}
                sx={{ width: 200, height: 200 }}
                className="preview-avatar"
              />
            </Box>
          )}
          <Typography variant="body2" color="text.secondary">
            Do you want to use this as your new profile picture?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseUploadDialog} disabled={loading}>
            Cancel
          </Button>
          <Button 
            onClick={handleUpload} 
            color="primary" 
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
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert 
          onClose={handleCloseSnackbar} 
          severity={snackbar.severity}
          variant="filled"
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
};

export default ProfilePicture; 