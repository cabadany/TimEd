import { ref, uploadBytes, getDownloadURL, deleteObject } from 'firebase/storage';
import { storage } from './firebase';

// Maximum allowed file size (3MB)
const MAX_FILE_SIZE = 3 * 1024 * 1024; // 3MB in bytes

// Allowed file types
const ALLOWED_FILE_TYPES = ['image/jpeg', 'image/png', 'image/jpg'];

// User's last profile update timestamp storage key
const LAST_PROFILE_UPDATE_KEY = 'lastProfileUpdate';

/**
 * Validates the file before upload
 * @param {File} file - The file to validate
 * @returns {Object} - Object with validation result and message
 */
export const validateProfilePicture = (file) => {
  // Check if file exists
  if (!file) {
    return { valid: false, message: "No file selected" };
  }

  // Check file size
  if (file.size > MAX_FILE_SIZE) {
    return { 
      valid: false, 
      message: `File size exceeds 3MB limit (${(file.size / (1024 * 1024)).toFixed(2)}MB)` 
    };
  }

  // Check file type
  if (!ALLOWED_FILE_TYPES.includes(file.type)) {
    return { 
      valid: false, 
      message: "Only JPG and PNG files are allowed" 
    };
  }

  return { valid: true, message: "File is valid" };
};

/**
 * Checks if the user can update their profile picture based on cooldown period
 * @param {string} userId - The user's ID
 * @returns {Object} - Object with result and remaining days if applicable
 */
export const canUpdateProfilePicture = (userId) => {
  const updateHistory = JSON.parse(localStorage.getItem(LAST_PROFILE_UPDATE_KEY) || '{}');
  const lastUpdate = updateHistory[userId];
  
  if (!lastUpdate) {
    return { canUpdate: true };
  }
  
  const now = new Date().getTime();
  const daysSinceLastUpdate = Math.floor((now - lastUpdate) / (1000 * 60 * 60 * 24));
  const cooldownPeriod = 3; // 3 days cooldown
  
  if (daysSinceLastUpdate >= cooldownPeriod) {
    return { canUpdate: true };
  } else {
    return { 
      canUpdate: false, 
      remainingDays: cooldownPeriod - daysSinceLastUpdate 
    };
  }
};

/**
 * Updates the user's last profile update timestamp
 * @param {string} userId - The user's ID
 */
export const recordProfileUpdate = (userId) => {
  const updateHistory = JSON.parse(localStorage.getItem(LAST_PROFILE_UPDATE_KEY) || '{}');
  updateHistory[userId] = new Date().getTime();
  localStorage.setItem(LAST_PROFILE_UPDATE_KEY, JSON.stringify(updateHistory));
};

/**
 * Uploads a profile picture to Firebase Storage
 * @param {string} userId - The user's ID
 * @param {File} file - The file to upload
 * @param {Function} onProgress - Progress callback (optional)
 * @returns {Promise<string>} - Download URL of the uploaded file
 */
export const uploadProfilePicture = async (userId, file, onProgress) => {
  // First validate the file
  const validation = validateProfilePicture(file);
  if (!validation.valid) {
    throw new Error(validation.message);
  }
  
  // Check if user can update profile
  const updateCheck = canUpdateProfilePicture(userId);
  if (!updateCheck.canUpdate) {
    throw new Error(`You can only change your profile picture every 3 days. Please wait ${updateCheck.remainingDays} more day(s).`);
  }

  // Create a reference to the file location in Firebase Storage
  const fileExtension = file.name.split('.').pop();
  const storageRef = ref(storage, `profilePictures/${userId}/profile.${fileExtension}`);
  
  try {
    // Upload the file
    const snapshot = await uploadBytes(storageRef, file);
    
    // Get the download URL
    const downloadURL = await getDownloadURL(snapshot.ref);
    
    // Record the update timestamp
    recordProfileUpdate(userId);
    
    return downloadURL;
  } catch (error) {
    console.error("Error uploading profile picture:", error);
    throw error;
  }
};

/**
 * Deletes a user's previous profile picture if it exists
 * @param {string} userId - The user's ID
 */
export const deleteOldProfilePicture = async (userId) => {
  try {
    // Check for both possible extensions
    const jpgRef = ref(storage, `profilePictures/${userId}/profile.jpg`);
    const pngRef = ref(storage, `profilePictures/${userId}/profile.png`);
    
    try {
      await deleteObject(jpgRef);
    } catch (error) {
      // File might not exist, ignore error
    }
    
    try {
      await deleteObject(pngRef);
    } catch (error) {
      // File might not exist, ignore error
    }
  } catch (error) {
    console.error("Error deleting old profile picture:", error);
  }
}; 