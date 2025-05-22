import { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import { storage } from '../firebase/firebase';
import { ref, getDownloadURL } from 'firebase/storage';

// Create the context
const UserContext = createContext();

// Custom hook to use the user context
export const useUser = () => useContext(UserContext);

// Provider component
export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [profilePictureUrl, setProfilePictureUrl] = useState(null);
  const [userId, setUserId] = useState(localStorage.getItem('userId'));
  
  // Watch for changes to userId in localStorage
  useEffect(() => {
    const checkUserIdChange = () => {
      const currentUserId = localStorage.getItem('userId');
      if (currentUserId !== userId) {
        setUserId(currentUserId);
      }
    };

    // Handler for auth-change custom event
    const handleAuthChange = (event) => {
      const newUserId = event.detail?.userId;
      if (newUserId && newUserId !== userId) {
        setUserId(newUserId);
      } else {
        // Force reload if same userId to handle re-login cases
        checkUserIdChange();
      }
    };

    // Check for changes to localStorage
    window.addEventListener('storage', checkUserIdChange);
    
    // Listen for our custom auth-change event
    window.addEventListener('auth-change', handleAuthChange);
    
    // Also check when the component mounts
    checkUserIdChange();
    
    return () => {
      window.removeEventListener('storage', checkUserIdChange);
      window.removeEventListener('auth-change', handleAuthChange);
    };
  }, [userId]);
  
  // Load user data when userId changes
  useEffect(() => {
    const fetchUserData = async () => {
      setLoading(true);
      
      if (!userId) {
        setUser(null);
        setProfilePictureUrl(null);
        setLoading(false);
        return;
      }
      
      try {
        // Fetch user data from the API - fixed endpoint to match backend controller
        const response = await axios.get(`http://localhost:8080/api/user/getUser/${userId}`);
        setUser(response.data);
        
        // Check if user has a profile picture URL
        if (response.data.profilePictureUrl) {
          setProfilePictureUrl(response.data.profilePictureUrl);
        } else {
          // Try to load from Firebase if not in user data
          await loadProfilePictureFromFirebase(userId);
        }
      } catch (error) {
        console.error('Error loading user data:', error);
        // Try to load profile picture anyway
        await loadProfilePictureFromFirebase(userId);
      } finally {
        setLoading(false);
      }
    };
    
    fetchUserData();
  }, [userId]);
  
  // Load profile picture from Firebase
  const loadProfilePictureFromFirebase = async (userId) => {
    if (!userId) return;
    
    try {
      // Try to get PNG version (as seen in Firebase)
      const imageRef = ref(storage, `profilePictures/${userId}/profile.png`);
      const url = await getDownloadURL(imageRef);
      setProfilePictureUrl(url);
      return url;
    } catch (error) {
      try {
        // If PNG not found, try JPG as fallback
        const imageRef = ref(storage, `profilePictures/${userId}/profile.jpg`);
        const url = await getDownloadURL(imageRef);
        setProfilePictureUrl(url);
        return url;
      } catch (innerError) {
        // No profile picture found, set default avatar
        console.log("No profile picture found for user, using default");
        // You could set a default avatar here if needed
        // setProfilePictureUrl('/default-avatar.png');
        return null;
      }
    }
  };
  
  // Update the profile picture URL
  const updateProfilePicture = async (url) => {
    setProfilePictureUrl(url);
    
    // If we have a user loaded, update the user object too
    if (user) {
      setUser({ ...user, profilePictureUrl: url });
      
      // Update in database
      const userId = localStorage.getItem('userId');
      if (userId) {
        try {
          await axios.put(`http://localhost:8080/api/user/updateProfilePicture/${userId}`, {
            profilePictureUrl: url
          });
        } catch (error) {
          console.error('Error updating profile picture in database:', error);
        }
      }
    }
  };
  
  // Value provided to consumers
  const value = {
    user,
    setUser,
    loading,
    profilePictureUrl,
    updateProfilePicture,
    loadProfilePictureFromFirebase,
  };
  
  return (
    <UserContext.Provider value={value}>
      {children}
    </UserContext.Provider>
  );
};

export default UserContext; 