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
  
  // Load user data on component mount
  useEffect(() => {
    const fetchUserData = async () => {
      setLoading(true);
      
      const userId = localStorage.getItem('userId');
      if (!userId) {
        setLoading(false);
        return;
      }
      
      try {
        // Fetch user data from the API
        const response = await axios.get(`http://localhost:8080/api/user/${userId}`);
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
  }, []);
  
  // Load profile picture from Firebase
  const loadProfilePictureFromFirebase = async (userId) => {
    if (!userId) return;
    
    try {
      try {
        // Try to get JPG version first
        const imageRef = ref(storage, `profilePictures/${userId}/profile.jpg`);
        const url = await getDownloadURL(imageRef);
        setProfilePictureUrl(url);
        return url;
      } catch (error) {
        try {
          // If JPG not found, try PNG
          const imageRef = ref(storage, `profilePictures/${userId}/profile.png`);
          const url = await getDownloadURL(imageRef);
          setProfilePictureUrl(url);
          return url;
        } catch (innerError) {
          // No profile picture found
          return null;
        }
      }
    } catch (error) {
      console.error('Error loading profile picture from Firebase:', error);
      return null;
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