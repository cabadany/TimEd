import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';
import axios from 'axios';
import { API_BASE_URL, getApiUrl, API_ENDPOINTS } from '../utils/api';
import { Modal, Box, Typography, Button, TextField, CircularProgress, Paper, Fade, Zoom, Grid, Avatar, IconButton, AppBar, Toolbar, Popover, Chip } from '@mui/material';
import { getAuth, sendPasswordResetEmail, signInWithEmailAndPassword } from "firebase/auth";
import { Email, GitHub, LinkedIn, Language, Info, Close } from '@mui/icons-material';


function LoginPage() {
  const navigate = useNavigate();
  const [idNumber, setIdNumber] = useState('');
  const [password, setPassword] = useState('');
  const [notification, setNotification] = useState({
    visible: false,
    message: '',
    type: ''
  });
  const [isAnimating, setIsAnimating] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [formShake, setFormShake] = useState(false);

  // Password reset states
  const [openPasswordModal, setOpenPasswordModal] = useState(false);
  const [email, setEmail] = useState('');
  const [emailSent, setEmailSent] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);

  // Mobile app modal state
  const [openMobileModal, setOpenMobileModal] = useState(false);
  
  // User info modal state
  const [openUserInfoModal, setOpenUserInfoModal] = useState(false);
  const [userInfo, setUserInfo] = useState(null);
  const [userInfoLoading, setUserInfoLoading] = useState(false);

  // OTP states
  const [showOtpInput, setShowOtpInput] = useState(false);
  const [otp, setOtp] = useState('');
  const [tempToken, setTempToken] = useState(null);
  const [tempUserId, setTempUserId] = useState(null);

  // Firebase auth instance
  const auth = getAuth();

  // Refs for input fields
  const idNumberRef = useRef(null);
  const passwordRef = useRef(null);
  const loginBtnRef = useRef(null);
  const forgotPasswordRef = useRef(null);
  const emailInputRef = useRef(null);

  // Auto focus on ID field on initial load
  useEffect(() => {
    setTimeout(() => {
      if (idNumberRef.current) {
        idNumberRef.current.focus();
      }
    }, 500); // Delay to allow animations to complete
  }, []);

  // Check if browser prefers dark mode
  useEffect(() => {
    const darkModeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    setIsDarkMode(darkModeMediaQuery.matches);

    const handleDarkModeChange = (e) => {
      setIsDarkMode(e.matches);
    };

    darkModeMediaQuery.addEventListener('change', handleDarkModeChange);

    return () => {
      darkModeMediaQuery.removeEventListener('change', handleDarkModeChange);
    };
  }, []);

  const showNotification = (message, type = 'error') => {
    setNotification({
      visible: true,
      message,
      type
    });

    // Shake the form if there's an error
    if (type === 'error') {
      setFormShake(true);
      setTimeout(() => setFormShake(false), 500);

      // Focus back on the appropriate field when there's an error
      if (!idNumber) {
        setTimeout(() => idNumberRef.current?.focus(), 600);
      } else if (!password) {
        setTimeout(() => passwordRef.current?.focus(), 600);
      }
    }

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
      setNotification(prev => ({ ...prev, visible: false }));
    }, 5000);
  };

  const closeNotification = () => {
    setNotification(prev => ({ ...prev, visible: false }));
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
    // Keep focus on password field after toggling
    passwordRef.current?.focus();
  };
  
  // Function to fetch user details by schoolId
  const fetchUserBySchoolId = async () => {
    if (!idNumber) {
      showNotification('Please enter ID Number first', 'error');
      return;
    }
    
    setUserInfoLoading(true);
    try {
      // First, try to get the user's email using the existing endpoint
      const response = await axios.get(getApiUrl(API_ENDPOINTS.EMAIL_BY_SCHOOL_ID), {
        params: { schoolId: idNumber }
      });
      
      if (response.data) {
        try {
          // Get the user's login info first
          const loginResponse = await axios.post(getApiUrl(API_ENDPOINTS.LOGIN_BY_SCHOOL_ID), {
            schoolId: idNumber
          });
          
          if (loginResponse.data && loginResponse.data.userId) {
            // Now use the userId to get full user details
            const userDetailsResponse = await axios.get(getApiUrl(API_ENDPOINTS.GET_USER(loginResponse.data.userId)), {
              headers: {
                'Authorization': `Bearer ${localStorage.getItem('token') || ''}`,
                'Content-Type': 'application/json'
              }
            }).catch(() => ({ data: null }));
            
            // Combine data from both responses
            setUserInfo({
              firstName: userDetailsResponse.data?.firstName || loginResponse.data.firstName || "User",
              lastName: userDetailsResponse.data?.lastName || loginResponse.data.lastName || "",
              email: response.data,
              role: loginResponse.data.role || "USER",
              userId: loginResponse.data.userId || ""
            });
            setOpenUserInfoModal(true);
          } else {
            showNotification('No user found with this ID', 'error');
          }
        } catch (error) {
          console.error('Error fetching user details:', error);
          showNotification('Error retrieving user information', 'error');
        }
      } else {
        showNotification('No user found with this ID', 'error');
      }
    } catch (error) {
      console.error('Error fetching user:', error);
      showNotification('No user found with this ID', 'error');
    } finally {
      setUserInfoLoading(false);
    }
  };
  
  // Handle close user info modal
  const handleCloseUserInfoModal = () => {
    setOpenUserInfoModal(false);
    setUserInfo(null);
  };

  // For tabbing from ID to password field
  const handleIdKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleLogin();
    } else if (e.key === 'Tab' && !e.shiftKey) {
      e.preventDefault();
      passwordRef.current?.focus();
    }
  };

  // For tabbing from password to forgot password link
  const handlePasswordKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleLogin();
    } else if (e.key === 'Tab' && !e.shiftKey) {
      e.preventDefault();
      forgotPasswordRef.current?.focus();
    } else if (e.key === 'Tab' && e.shiftKey) {
      e.preventDefault();
      idNumberRef.current?.focus();
    }
  };

  // For tabbing from forgot password to login button
  const handleForgotPasswordKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleOpenPasswordModal();
    } else if (e.key === 'Tab' && !e.shiftKey) {
      e.preventDefault();
      loginBtnRef.current?.focus();
    } else if (e.key === 'Tab' && e.shiftKey) {
      e.preventDefault();
      passwordRef.current?.focus();
    }
  };

  // Memoize the handleLogin function to avoid dependency issues
  const handleLogin = useCallback(async () => {
    if (!idNumber || !password) {
      showNotification('Please enter both ID Number and Password');
      return;
    }
  
    setIsLoading(true);
  
    try {
      // Get email from backend using schoolId
      const emailResponse = await axios.get(getApiUrl(API_ENDPOINTS.EMAIL_BY_SCHOOL_ID), {
        params: { schoolId: idNumber }
      });
  
      const email = emailResponse.data;
      
      // Authenticate using Firebase Authentication with email
      await signInWithEmailAndPassword(auth, email, password);
  
      // If login success, call backend to get user role & token
      const response = await axios.post(getApiUrl(API_ENDPOINTS.LOGIN_BY_SCHOOL_ID), {
        schoolId: idNumber
      });

      const data = response.data;
  
      if (data.success) {
        if (data.role === 'ADMIN') {
          // For admin users, request OTP
          try {
            await axios.post(getApiUrl(API_ENDPOINTS.GENERATE_OTP), {
              schoolId: idNumber
            });
            
            // Store temporary data and show OTP input
            setTempToken(data.token);
            setTempUserId(data.userId);
            setShowOtpInput(true);
            setIsLoading(false);
            showNotification('Please check your email for OTP', 'info');
          } catch (error) {
            setIsLoading(false);
            showNotification('Failed to send OTP. Please try again.', 'error');
          }
        } else {
          setIsLoading(false);
          showNotification('Access denied. Only admins can log in.');
          localStorage.clear();
        }
      } else {
        setIsLoading(false);
        showNotification(data.message || 'Invalid login credentials');
      }
  
    } catch (error) {
      console.error('Login failed:', error);
      setIsLoading(false);
  
      if (error.code === 'auth/user-not-found') {
        showNotification('No Firebase account found with that school ID');
      } else if (error.code === 'auth/wrong-password') {
        showNotification('Wrong password.');
      } else {
        showNotification('Login failed: Check Credentials or Network Connection');
      }
    }
  }, [idNumber, password, navigate]);

  const handleVerifyOtp = async () => {
    if (!otp) {
      showNotification('Please enter OTP');
      return;
    }

    setIsLoading(true);

    try {
      const response = await axios.post(getApiUrl(API_ENDPOINTS.VERIFY_OTP), {
        schoolId: idNumber,
        otp: otp
      });

      if (response.data === 'OTP verified successfully') {
        // Complete the login process
        localStorage.setItem('token', tempToken);
        localStorage.setItem('userId', tempUserId);
        localStorage.setItem('role', 'ADMIN');
        
        // Dispatch auth change event
        window.dispatchEvent(new CustomEvent('auth-change', { detail: { userId: tempUserId } }));
        
        setIsAnimating(true);
        setTimeout(() => {
          navigate('/dashboard');
        }, 800);
      } else {
        showNotification('Invalid OTP. Please try again.', 'error');
      }
    } catch (error) {
      console.error('OTP verification failed:', error);
      showNotification('Failed to verify OTP. Please try again.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // Handle already logged in functionality
  const handleAlreadyLoggedIn = () => {
    // Check if there's an existing admin session
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    const role = localStorage.getItem('role');

    if (token && userId && role === 'ADMIN') {
      // Admin is already logged in, redirect to dashboard
      setIsAnimating(true);
      setTimeout(() => {
        navigate('/dashboard');
      }, 800);
    } else {
      // No admin logged in, show error
      showNotification('There is no administrator logged into this device', 'error');
    }
  };

  // Add useEffect to handle the 'keydown' event for the entire component
  useEffect(() => {
    const handleGlobalKeyDown = (e) => {
      if (e.key === 'Enter') {
        handleLogin();
      }
    };

    // Add event listener to window
    window.addEventListener('keydown', handleGlobalKeyDown);

    // Clean up event listener
    return () => {
      window.removeEventListener('keydown', handleGlobalKeyDown);
    };
  }, [handleLogin]);

  // Password reset modal handlers
  const handleOpenPasswordModal = () => {
    setOpenPasswordModal(true);
    // Focus on email input field after modal opens
    setTimeout(() => {
      emailInputRef.current?.focus();
    }, 100);
  };

  const handleClosePasswordModal = () => {
    setOpenPasswordModal(false);
    setEmail('');
    setEmailSent(false);
  };

  // Mobile app modal handlers  
  const handleOpenMobileModal = () => setOpenMobileModal(true);

  const handleCloseMobileModal = () => setOpenMobileModal(false);

  // Password reset function
  const handleForgotPassword = async () => {
    if (!email) {
      showNotification('Please enter your email address');
      return;
    }

    if (!isValidEmail(email)) {
      showNotification('Please enter a valid email address');
      return;
    }

    setResetLoading(true);

    try {
      await sendPasswordResetEmail(auth, email);
      setResetLoading(false);
      setEmailSent(true);
    } catch (error) {
      setResetLoading(false);
      
      if (error.code === 'auth/user-not-found') {
        showNotification('No account found with that email address', 'error');
      } else {
        showNotification('Error sending reset email: ' + error.message, 'error');
      }
    }
  };

  // Email validation function
  const isValidEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  // Add new state for About Us modal
  const [openAboutModal, setOpenAboutModal] = useState(false);
  // Add state for team member details modal
  const [selectedMember, setSelectedMember] = useState(null);
  const [openMemberModal, setOpenMemberModal] = useState(false);

  const handleOpenAboutModal = () => {
    setOpenAboutModal(true);
  };

  const handleCloseAboutModal = () => {
    setOpenAboutModal(false);
  };

  const handleOpenMemberModal = (member) => {
    setSelectedMember(member);
    setOpenMemberModal(true);
  };

  const handleCloseMemberModal = () => {
    setOpenMemberModal(false);
    setSelectedMember(null);
  };

  return (
    <div className={`login-page ${isDarkMode ? 'dark-mode' : ''}`}>
      {/* About Us Modal */}
      <Modal
        open={openAboutModal}
        onClose={handleCloseAboutModal}
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          '& .MuiBackdrop-root': {
            backgroundColor: isDarkMode ? 'rgba(0, 0, 0, 0.01)' : 'rgba(255, 255, 255, 0.01)',
            backdropFilter: 'blur(4px)'
          }
        }}
        onClick={(e) => {
          // Close modal when clicking the backdrop
          if (e.target === e.currentTarget) {
            handleCloseAboutModal();
          }
        }}
      >
        <Box sx={{
          width: '100%',
          height: '100%',
          bgcolor: isDarkMode ? 'rgba(18, 18, 18, 0.8)' : 'rgba(255, 255, 255, 0.8)',
          overflow: 'auto',
          position: 'relative'
        }}>
          <IconButton
            onClick={handleCloseAboutModal}
            sx={{
              position: 'fixed',
              top: 20,
              right: 20,
              bgcolor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
              color: isDarkMode ? '#fff' : '#000',
              '&:hover': {
                bgcolor: isDarkMode ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.2)',
              }
            }}
          >
            <Close />
          </IconButton>

          <Box sx={{ maxWidth: '1200px', margin: '0 auto', p: 4, pt: 8 }}>
            <Fade in={true} timeout={800}>
              <Typography variant="h3" fontWeight="700" color="primary" mb={4} className="section-title about-title">
                About TimeED
              </Typography>
            </Fade>
            
            <Zoom in={true} style={{ transitionDelay: '300ms' }}>
              <Paper 
                sx={{ 
                  p: 4, 
                  mb: 5, 
                  borderRadius: 3, 
                  boxShadow: '0 8px 32px rgba(0,0,0,0.08)',
                  background: isDarkMode ? 'linear-gradient(145deg, #1e1e1e 0%, #262626 100%)' : 'linear-gradient(145deg, #ffffff 0%, #f9fafc 100%)',
                  position: 'relative',
                  overflow: 'hidden',
                  '&::before': {
                    content: '""',
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    height: '5px',
                    background: 'linear-gradient(90deg, #304FFF 0%, #8C9EFF 100%)',
                  }
                }}
                className="mission-paper"
              >
                <Typography variant="h5" fontWeight="600" color="text.primary" mb={3} className="pulse-animation">
                  Our Mission
                </Typography>
                <Typography variant="body1" color="text.secondary" paragraph className="mission-statement" sx={{ fontSize: '1.1rem', lineHeight: 1.8 }}>
                  TimeED is a comprehensive time management and educational platform designed to help educational institutions 
                  streamline their operations. Our system provides tools for event management, attendance tracking, 
                  certificate generation, and department organization to enhance productivity and efficiency in educational settings.
                </Typography>
                <Typography variant="body1" color="text.secondary" paragraph sx={{ fontSize: '1.05rem' }}>
                  We are committed to creating intuitive, user-friendly solutions that address the unique challenges faced by schools, 
                  colleges, and universities in managing their day-to-day activities.
                </Typography>
              </Paper>
            </Zoom>
            
            <Fade in={true} timeout={1000} style={{ transitionDelay: '600ms' }}>
              <Typography variant="h4" fontWeight="700" color="primary" mb={4} className="section-title team-title">
                Development Team
              </Typography>
            </Fade>
            
            <Box 
              sx={{ 
                mb: 5, 
                position: 'relative',
                background: '#0d0d0d',
                borderRadius: '16px',
                overflow: 'hidden',
                p: 0
              }}
              className="team-showcase"
            >
              <Box 
                sx={{ 
                  position: 'absolute', 
                  top: 0, 
                  left: 0, 
                  right: 0, 
                  bottom: 0, 
                  background: 'url(/wavy-pattern.svg), linear-gradient(to right, rgba(30,30,30,0.7), rgba(30,30,30,0.7))',
                  opacity: 0.1,
                  zIndex: 0
                }} 
                className="background-pattern"
              />
              
              <Box 
                sx={{ 
                  display: 'flex',
                  flexDirection: { xs: 'column', md: 'row' },
                  alignItems: 'stretch',
                  position: 'relative',
                  zIndex: 1
                }}
              >
                {[
               
                  {
                    name: 'Cabana, Danisse',
                    firstName: 'Danisse',
                    lastName: 'Cabana',
                    role: 'Mobile Backend Developer',
                    image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F494828260_646748648390700_2831467056740054150_n.jpg?alt=media&token=d613d9b2-88ff-4c59-9c3a-2d5f1825f3da',
                    description: 'Backend developer focusing on mobile app integration, database connectivity, and secure API development for seamless mobile experiences.',
                    skills: ['Firebase', 'Spring Boot', 'RESTful APIs', 'Mobile Integration', 'Authentication', 'Database Management']
                  },
                  {
                    name: 'Tumungha, Alexa',
                    firstName: 'Alexa',
                    lastName: 'Tumungha',
                    role: 'Project Manager',
                    image: 'https://randomuser.me/api/portraits/women/68.jpg',
                    description: 'Organized and goal-driven manager with expertise in managing project timelines, coordinating teams, and ensuring successful project delivery.',
                    skills: ['Project Management', 'Scrum', 'Agile Methodologies', 'Team Leadership', 'Jira', 'Communication']
                  },
                  {
                    name: 'Navaroo, Mikhail James',
                    firstName: 'Mikhail James',
                    lastName: 'Navaroo',
                    role: 'Mobile Frontend Developer',
                    image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F495270598_1925613518207533_3638793039034373708_n.png?alt=media&token=d7aaf893-9208-4f51-b771-c9838034ffdc',
                    description: 'Frontend developer focused on building intuitive, responsive mobile applications with attention to detail and performance.',
                    skills: ['React Native', 'JavaScript', 'UI Components', 'Mobile Design Patterns', 'Expo', 'CSS-in-JS']
                  },
                  {
                    name: 'Largo, John Wayne',
                    firstName: 'John Wayne',
                    lastName: 'Largo',
                    role: 'Web Backend Developer',
                    image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F434168392_2189176254770061_5356900714852223221_n.jpg?alt=media&token=9410fa9a-fd06-40e7-bbb3-5e53aab0262d',
                    description: 'Backend specialist for web systems, focused on building scalable infrastructure and secure APIs with modern tech stack.',
                    skills: ['Spring Boot', 'Firebase', 'PostgreSQL', 'JWT Authentication', 'Docker', 'API Security']
                  },
                  {
                    name: 'Gemongala, Clark',
                    firstName: 'Clark',
                    lastName: 'Gemongala',
                    role: 'Web Frontend Developer',
                    image: 'https://firebasestorage.googleapis.com/v0/b/timed-system.firebasestorage.app/o/AboutusImage%2F126269967.jfif?alt=media&token=0079cc0e-5fcc-41c6-b131-d2832f359eb1',
                    description: 'Frontend developer with strong attention to responsive design, user experience, and clean code implementation for web platforms.',
                    skills: ['React.js', 'HTML5', 'CSS3', 'Tailwind CSS', 'JavaScript', 'Version Control']
                  }
                ].map((member, index) => (
                  <Box 
                    key={index}
                    onClick={() => handleOpenMemberModal(member)}
                    sx={{
                      flex: 1,
                      position: 'relative',
                      height: { xs: '60vw', sm: '50vw', md: '400px' },
                      maxHeight: { xs: '350px', sm: '400px', md: '500px' },
                      overflow: 'hidden',
                      cursor: 'pointer',
                      transition: 'all 0.3s ease',
                      filter: 'grayscale(100%)',
                      '&:hover': {
                        filter: 'grayscale(0%)',
                        flex: { md: 1.2 },
                      },
                      '&:hover .member-info': {
                        opacity: 1,
                        transform: 'translateY(0)'
                      },
                      '&:before': {
                        content: '""',
                        position: 'absolute',
                        bottom: 0,
                        left: 0,
                        right: 0,
                        height: '70%',
                        background: 'linear-gradient(to top, rgba(0,0,0,0.8), rgba(0,0,0,0))',
                        zIndex: 1
                      }
                    }}
                    className="team-member-banner"
                  >
                    <Box
                      component="img"
                      src={member.image}
                      alt={member.name}
                      sx={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                        transition: 'all 0.3s ease'
                      }}
                    />
                    <Box
                      className="member-info"
                      sx={{
                        position: 'absolute',
                        bottom: 0,
                        left: 0,
                        right: 0,
                        p: 3,
                        color: 'white',
                        zIndex: 2,
                        opacity: 0,
                        transform: 'translateY(20px)',
                        transition: 'all 0.3s ease'
                      }}
                    >
                      <Typography variant="h6" fontWeight="600" sx={{ mb: 1 }}>
                        {member.firstName} {member.lastName}
                      </Typography>
                      <Typography variant="body2" sx={{ opacity: 0.8, mb: 2 }}>
                        {member.role}
                      </Typography>
                      <Typography variant="body2" sx={{ opacity: 0.7 }}>
                        {member.description}
                      </Typography>
                    </Box>
                  </Box>
                ))}
              </Box>
            </Box>

            <Fade in={true} timeout={1000} style={{ transitionDelay: '900ms' }}>
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  p: 4,
                  mt: 3,
                  mb: 4,
                  borderRadius: 3,
                  background: isDarkMode ? 'rgba(144, 202, 249, 0.05)' : 'rgba(48, 79, 255, 0.03)',
                  textAlign: 'center'
                }}
                className="contact-section"
              >
                <Typography variant="h5" fontWeight="600" color="primary.main" mb={2}>
                  Want to connect with our team?
                </Typography>
                <Typography variant="body1" color="text.secondary" mb={3} sx={{ maxWidth: '800px' }}>
                  We're always looking to improve TimeED. If you have suggestions, questions, or would like to learn more about our platform, please reach out!
                </Typography>
                <Box sx={{ display: 'flex', gap: 2 }}>
                <IconButton 
  color="primary" 
  component="a" 
  href="https://github.com/cabadany/TimEd" 
  target="_blank" 
  rel="noopener noreferrer"
>
  <GitHub />
</IconButton>
             
            
                </Box>
              </Box>
            </Fade>
          </Box>
        </Box>
      </Modal>

      {notification.visible && (
        <div className={`notification ${notification.type}`}>
          <div className="notification-icon">
            {notification.type === 'error' ? (
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            ) : (
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
            )}
          </div>
          <div className="notification-content">
            {notification.message}
          </div>
          <button className="notification-close" onClick={closeNotification} aria-label="Close notification">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      )}
      
      <div className="login-left">
        <img src="/timeed.png" alt="TimEd Logo" className="logo animate-fade-in" />
        
        <div className={`login-form animate-slide-up ${formShake ? 'shake' : ''} ${isLoading ? 'form-loading' : ''}`}>
          <h2 className="login-title">Login</h2>
          
          <div className="form-group animate-slide-up" style={{ animationDelay: '0.1s' }}>
            <label className="form-label" htmlFor="idNumber">ID Number</label>
            <div className="input-container">
              <input
                id="idNumber"
                ref={idNumberRef}
                type="text"
                className="form-input"
                placeholder="##-####-###"
                value={idNumber}
                onChange={(e) => setIdNumber(e.target.value)}
                onKeyDown={handleIdKeyDown}
                disabled={isLoading || isAnimating}
                aria-describedby="idNumberHelp"
              />
              <button 
                className="input-icon-btn" 
                disabled={isLoading || isAnimating || userInfoLoading || !idNumber}
                aria-label="ID Icon"
                type="button"
                onClick={fetchUserBySchoolId}
              >
                {userInfoLoading ? (
                  <div className="small-spinner">
                    <div className="bounce1"></div>
                    <div className="bounce2"></div>
                    <div className="bounce3"></div>
                  </div>
                ) : (
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                    <circle cx="12" cy="10" r="3"></circle>
                    <path d="M12 13c-2.67 0-8 1.34-8 4v3h16v-3c0-2.66-5.33-4-8-4z"></path>
                  </svg>
                )}
              </button>
            </div>
            <div id="idNumberHelp" className="form-help-text">Enter your school ID number</div>
          </div>
          
          <div className="form-group animate-slide-up" style={{ animationDelay: '0.2s' }}>
            <label className="form-label" htmlFor="password">Password</label>
            <div className="input-container">
              <input
                id="password"
                ref={passwordRef}
                type={showPassword ? "text" : "password"}
                className="form-input"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={handlePasswordKeyDown}
                disabled={isLoading || isAnimating}
                aria-describedby="passwordHelp"
              />
              <button 
                className="input-icon-btn password-toggle"
                onClick={togglePasswordVisibility}
                type="button"
                disabled={isLoading || isAnimating}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? (
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                  </svg>
                ) : (
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                )}
              </button>
            </div>
          </div>
          
          <div 
            className="forgot-password animate-slide-up" 
            style={{ animationDelay: '0.3s' }}
            tabIndex="0"
            ref={forgotPasswordRef}
            role="button"
            onClick={handleOpenPasswordModal}
            onKeyDown={handleForgotPasswordKeyDown}
            aria-label="Forgot Password"
          >
            Forgot Password?
          </div>
          
          <Box>
            {!showOtpInput ? (
              <button 
                ref={loginBtnRef}
                className={`login-btn ${isAnimating ? 'btn-clicked' : ''} ${isLoading ? 'loading' : ''}`}
                onClick={handleLogin}
                disabled={isAnimating || isLoading}
              >
                {isLoading ? (
                  <div className="spinner">
                    <div className="bounce1"></div>
                    <div className="bounce2"></div>
                    <div className="bounce3"></div>
                  </div>
                ) : isAnimating ? 'Logging in...' : 'Login Now'}
              </button>
            ) : (
              <Box sx={{ mt: 2 }}>
                <TextField
                  fullWidth
                  label="Enter OTP"
                  variant="outlined"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  type="text"
                  placeholder="Enter the OTP sent to your email"
                  sx={{ mb: 2 }}
                />
                <Button
                  fullWidth
                  variant="contained"
                  onClick={handleVerifyOtp}
                  disabled={isLoading}
                >
                  {isLoading ? <CircularProgress size={24} /> : 'Verify OTP'}
                </Button>
              </Box>
            )}
          </Box>
          
          <button 
            className="already-logged-in-btn"
            onClick={handleAlreadyLoggedIn}
            disabled={isAnimating || isLoading}
          >
            Already Logged in
          </button>
          
          <button 
            className="mobile-app-btn"
            onClick={handleOpenMobileModal}
            disabled={isAnimating || isLoading}
          >
            Mobile App
          </button>
        </div>
      </div>
      
      <div className="login-right">
        <div className="right-content">
          {/* Add Info icon button for About Us in the top-right */}
          <IconButton
            onClick={handleOpenAboutModal}
            sx={{
              position: 'absolute',
              top: 20,
              right: 20,
              color: isDarkMode ? '#fff' : '#3538CD',
              background: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(53,56,205,0.08)',
              '&:hover': {
                background: isDarkMode ? 'rgba(255,255,255,0.2)' : 'rgba(53,56,205,0.15)',
              },
              zIndex: 20 // Add higher z-index to make it clickable
            }}
            aria-label="About Us"
          >
            <Info />
          </IconButton>
          
          <div className="welcome-header">
            <h2 className="welcome-title">Welcome to TimeEd</h2>
            <p className="welcome-subtitle">Your Modern Event & Attendance System</p>
          </div>
          
          <div className="illustration-container">
            <div className="floating-elements">
              <div className="float-element calendar">
                <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="16" y1="2" x2="16" y2="6"></line>
                  <line x1="8" y1="2" x2="8" y2="6"></line>
                  <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
              </div>
              <div className="float-element chart">
                <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="20" x2="18" y2="10"></line>
                  <line x1="12" y1="20" x2="12" y2="4"></line>
                  <line x1="6" y1="20" x2="6" y2="14"></line>
                </svg>
              </div>
              <div className="float-element clock">
                <svg xmlns="http://www.w3.org/2000/svg" width="42" height="42" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
              </div>
              <div className="float-element user">
                <svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </div>
              <div className="float-element check">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
              </div>
              <div className="float-element people">
                <svg xmlns="http://www.w3.org/2000/svg" width="38" height="38" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
              </div>
              <div className="float-element file">
                <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
              </div>
              <div className="float-element mobile">
                <svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#3538CD" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                  <line x1="12" y1="18" x2="12.01" y2="18"></line>
                </svg>
              </div>
            </div>
            <div className="main-illustration">
              <img src="/timeed.png" alt="Event Management Illustration" />
            </div>
          </div>
          
          <div className="feature-labels">
            <div className="feature-label attendance">
              <div className="feature-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
              </div>
              <span>Attendance Tracking</span>
            </div>
            
            <div className="feature-label events">
              <div className="feature-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="16" y1="2" x2="16" y2="6"></line>
                  <line x1="8" y1="2" x2="8" y2="6"></line>
                  <line x1="3" y1="10" x2="21" y2="10"></line>
                </svg>
              </div>
              <span>Event Management</span>
            </div>
            
            <div className="feature-label reports">
              <div className="feature-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <line x1="16" y1="13" x2="8" y2="13"></line>
                  <line x1="16" y1="17" x2="8" y2="17"></line>
                  <polyline points="10 9 9 9 8 9"></polyline>
                </svg>
              </div>
              <span>Reporting</span>
            </div>
            
            <div className="feature-label mobile-access">
              <div className="feature-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect>
                  <line x1="12" y1="18" x2="12.01" y2="18"></line>
                </svg>
              </div>
              <span>Mobile Access</span>
            </div>
          </div>
          
          <div className="version-info">
            <p>Version 1.0 • TimEd System</p>
          </div>
        </div>

    
      </div>

      {/* Password Reset Modal */}
      <Modal
        open={openPasswordModal}
        onClose={handleClosePasswordModal}
        aria-labelledby="password-reset-modal-title"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 400,
          bgcolor: isDarkMode ? '#2d2d2d' : 'background.paper',
          boxShadow: 24,
          p: 4,
          borderRadius: 2,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 2,
          color: isDarkMode ? '#f0f0f0' : 'text.primary'
        }}>
          <Typography id="password-reset-modal-title" variant="h5" component="h2" sx={{ fontWeight: 600 }}>
            Reset Password
          </Typography>
          
          {!emailSent ? (
            <>
              <Typography variant="body2" sx={{ textAlign: 'center', mb: 1 }}>
                Enter your email address to receive a password reset link
              </Typography>
              
              <TextField
                inputRef={emailInputRef}
                label="Email Address"
                variant="outlined"
                fullWidth
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={resetLoading}
                error={email !== '' && !isValidEmail(email)}
                helperText={email !== '' && !isValidEmail(email) ? 'Please enter a valid email' : ''}
                sx={{
                  mb: 2,
                  '& .MuiOutlinedInput-root': {
                    '&:hover fieldset': {
                      borderColor: isDarkMode ? '#6b6ef7' : '#3538CD',
                    },
                  },
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleForgotPassword();
                  }
                }}
              />
              
              <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                <Button 
                  variant="outlined" 
                  onClick={handleClosePasswordModal}
                  disabled={resetLoading}
                  sx={{ 
                    borderColor: isDarkMode ? '#6b6ef7' : '#3538CD',
                    color: isDarkMode ? '#6b6ef7' : '#3538CD',
                    '&:hover': { 
                      borderColor: isDarkMode ? '#5254d4' : '#282aa3',
                      backgroundColor: isDarkMode ? 'rgba(107, 110, 247, 0.1)' : 'rgba(53, 56, 205, 0.1)'
                    }
                  }}
                >
                  Cancel
                </Button>
                
                <Button 
                  variant="contained" 
                  onClick={handleForgotPassword}
                  disabled={resetLoading || !email || !isValidEmail(email)}
                  sx={{ 
                    bgcolor: '#3538CD',
                    '&:hover': { bgcolor: '#2C2EA9' },
                    '&:disabled': {
                      bgcolor: isDarkMode ? '#4e4e4e' : '#c5c5c5',
                      color: isDarkMode ? '#a0a0a0' : '#ffffff',
                    }
                  }}
                >
                  {resetLoading ? (
                    <CircularProgress size={24} color="inherit" />
                  ) : (
                    'Send Reset Link'
                  )}
                </Button>
              </Box></>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50" viewBox="0 0 24 24" fill="none" stroke={isDarkMode ? "#6b6ef7" : "#3538CD"} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              
              <Typography variant="h6" sx={{ textAlign: 'center' }}>
                Reset Link Sent!
              </Typography>
              
              <Typography variant="body2" sx={{ textAlign: 'center', mb: 1 }}>
                Please check your email and follow the instructions to reset your password.
              </Typography>
              
              <Button 
                variant="contained" 
                onClick={handleClosePasswordModal}
                sx={{ 
                  bgcolor: '#3538CD',
                  '&:hover': { bgcolor: '#2C2EA9' }
                }}
              >
                Close
              </Button>
            </Box>
          )}
        </Box>
      </Modal>

       {/* Mobile App Modal */}
       <Modal
        open={openMobileModal}
        onClose={handleCloseMobileModal}
        aria-labelledby="mobile-app-modal-title"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 400,
          bgcolor: isDarkMode ? '#2d2d2d' : 'background.paper',
          boxShadow: 24,
          p: 4,
          borderRadius: 2,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 2,
          color: isDarkMode ? '#f0f0f0' : 'text.primary'
        }}>
          <Typography id="mobile-app-modal-title" variant="h5" component="h2" sx={{ fontWeight: 600 }}>
            Admin Portal Notice
          </Typography>
          
          <Typography variant="body1" sx={{ textAlign: 'center', mb: 2 }}>
            This website is for administrative access only. If you are a faculty member, please download our mobile app for the best experience.
          </Typography>
          
          <Box
            sx={{
              width: 200,
              height: 200,
              bgcolor: 'white',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              mb: 2,
              border: '1px solid',
              borderColor: isDarkMode ? '#4d4d4d' : '#e0e0e0',
              position: 'relative',
              overflow: 'hidden',
            }}
          >
            {/* Generate QR code that links to the app download */}
            <img 
              src={`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent('https://drive.google.com/drive/folders/1KoLXlydRPEy7w_N2Z9tatZQLISrotuQq?usp=sharing')}`}
              alt="QR Code for Mobile App"
              style={{ width: '100%', height: '100%', objectFit: 'contain' }}
            />
          </Box>
          
          <Typography variant="body2" sx={{ fontWeight: 500, textAlign: 'center' }}>
            Scan this QR code or click the button below to download
          </Typography>
          
          <Button 
            variant="contained" 
            href="https://drive.google.com/drive/folders/1KoLXlydRPEy7w_N2Z9tatZQLISrotuQq?usp=sharing"
            target="_blank"
            rel="noopener noreferrer"
            sx={{ 
              bgcolor: '#3538CD', 
              '&:hover': { bgcolor: '#2C2EA9' },
              mt: 1
            }}
          >
            Download Mobile App
          </Button>
          
          <Button 
            variant="text" 
            onClick={handleCloseMobileModal}
            sx={{ 
              color: isDarkMode ? '#6b6ef7' : '#3538CD',
              mt: 1
            }}
          >
            Close
          </Button>
        </Box>
        
      </Modal>
      
      {/* User Info Modal */}
      <Modal
        open={openUserInfoModal}
        onClose={handleCloseUserInfoModal}
        aria-labelledby="user-info-modal-title"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 350,
          bgcolor: isDarkMode ? '#2d2d2d' : 'background.paper',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.12)',
          borderRadius: 3,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          overflow: 'hidden',
          color: isDarkMode ? '#f0f0f0' : 'text.primary'
        }}>
          {/* Header with user icon */}
          <Box sx={{ 
            width: '100%', 
            py: 3,
            px: 2, 
            display: 'flex', 
            flexDirection: 'column',
            alignItems: 'center',
            position: 'relative',
            borderBottom: '1px solid',
            borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.06)'
          }}>
            {/* User avatar/icon */}
            <Box sx={{
              width: 70,
              height: 70,
              borderRadius: '50%',
              bgcolor: isDarkMode ? '#3538CD' : '#3f51b5',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              mb: 2,
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </Box>
            
            {/* User name */}
            <Typography 
              id="user-info-modal-title" 
              variant="h5" 
              component="h2" 
              sx={{ 
                fontWeight: 600, 
                textAlign: 'center',
                fontSize: '1.4rem'
              }}
            >
              You are {userInfo ? `${userInfo.firstName} ${userInfo.lastName}` : '...'}
            </Typography>
            
            {/* User email */}
            {userInfo && userInfo.email && (
              <Typography 
                variant="body2" 
                sx={{ 
                  color: isDarkMode ? '#a0a0a0' : '#666', 
                  textAlign: 'center',
                  mt: 0.5,
                  fontSize: '0.9rem'
                }}
              >
                {userInfo.email}
              </Typography>
            )}
          </Box>
          
          {/* Content area */}
          <Box sx={{ width: '100%', px: 3, py: 2 }}>
            {/* User role */}
            {userInfo && userInfo.role && (
              <Box sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                py: 1.5
              }}>
                <Box sx={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  px: 2,
                  py: 0.75,
                  borderRadius: 5,
                  bgcolor: userInfo.role === 'ADMIN' 
                    ? isDarkMode ? 'rgba(76, 175, 80, 0.15)' : 'rgba(76, 175, 80, 0.1)' 
                    : isDarkMode ? 'rgba(255, 152, 0, 0.15)' : 'rgba(255, 152, 0, 0.1)',
                  color: userInfo.role === 'ADMIN' ? '#4caf50' : '#ff9800',
                  fontWeight: 500,
                  fontSize: '0.875rem'
                }}>
                  {userInfo.role === 'ADMIN' ? (
                    <>
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '6px' }}>
                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="8.5" cy="7" r="4"></circle>
                        <polyline points="17 11 19 13 23 9"></polyline>
                      </svg>
                      Administrator Account
                    </>
                  ) : (
                    <>
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ marginRight: '6px' }}>
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                      </svg>
                      Faculty Account
                    </>
                  )}
                </Box>
              </Box>
            )}
            
            {/* Non-admin notice */}
            {userInfo && userInfo.role !== 'ADMIN' && (
              <Box sx={{ 
                bgcolor: isDarkMode ? 'rgba(255, 152, 0, 0.08)' : 'rgba(255, 152, 0, 0.05)', 
                p: 2, 
                borderRadius: 2,
                mt: 1,
                border: '1px solid',
                borderColor: isDarkMode ? 'rgba(255, 152, 0, 0.2)' : 'rgba(255, 152, 0, 0.15)'
              }}>
                <Typography variant="body2" sx={{ textAlign: 'center', fontSize: '0.875rem' }}>
                  Please note: This web portal is for administrators only. Faculty members should use our mobile app.
                </Typography>
                <Button 
                  variant="outlined" 
                  size="small" 
                  onClick={handleOpenMobileModal}
                  sx={{ 
                    mt: 1.5, 
                    display: 'block', 
                    mx: 'auto',
                    color: isDarkMode ? '#ff9800' : '#e65100',
                    borderColor: isDarkMode ? '#ff9800' : '#e65100',
                    '&:hover': {
                      borderColor: isDarkMode ? '#ffb74d' : '#ff9800',
                      backgroundColor: isDarkMode ? 'rgba(255, 152, 0, 0.08)' : 'rgba(255, 152, 0, 0.04)'
                    }
                  }}
                >
                  Get Mobile App
                </Button>
              </Box>
            )}
          </Box>
          
          {/* Footer with close button */}
          <Box sx={{ 
            width: '100%', 
            p: 2, 
            display: 'flex', 
            justifyContent: 'center',
            backgroundColor: isDarkMode ? 'rgba(0, 0, 0, 0.2)' : 'rgba(0, 0, 0, 0.02)',
            borderTop: '1px solid',
            borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.06)'
          }}>
            <Button 
              variant="contained" 
              onClick={handleCloseUserInfoModal}
              sx={{ 
                bgcolor: '#3538CD',
                '&:hover': { bgcolor: '#2C2EA9' },
                px: 4,
                py: 1,
                fontWeight: 500,
                letterSpacing: '0.5px',
                boxShadow: '0 4px 8px rgba(53, 56, 205, 0.25)',
                minWidth: '120px'
              }}
            >
              CLOSE
            </Button>
          </Box>
        </Box>
      </Modal>

      {/* Team Member Details Modal */}
      <Modal
        open={openMemberModal}
        onClose={handleCloseMemberModal}
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          '& .MuiBackdrop-root': {
            backgroundColor: isDarkMode ? 'rgba(0, 0, 0, 0.85)' : 'rgba(255, 255, 255, 0.85)',
            backdropFilter: 'blur(8px)'
          }
        }}
      >
        <Fade in={openMemberModal}>
          <Box
            sx={{
              position: 'relative',
              width: '90%',
              maxWidth: '600px',
              bgcolor: isDarkMode ? 'rgba(18, 18, 18, 0.95)' : 'rgba(255, 255, 255, 0.95)',
              borderRadius: 3,
              boxShadow: 24,
              p: 4,
              outline: 'none',
              overflow: 'hidden'
            }}
          >
            <IconButton
              onClick={handleCloseMemberModal}
              sx={{
                position: 'absolute',
                right: 8,
                top: 8,
                color: 'text.secondary'
              }}
            >
              <Close />
            </IconButton>

            {selectedMember && (
              <Grid container spacing={3}>
                <Grid item xs={12} sm={4} sx={{ textAlign: 'center' }}>
                  <Avatar
                    src={selectedMember.image}
                    alt={selectedMember.name}
                    sx={{
                      width: 120,
                      height: 120,
                      margin: '0 auto',
                      border: '4px solid',
                      borderColor: 'primary.main'
                    }}
                  />
                </Grid>
                <Grid item xs={12} sm={8}>
                  <Typography variant="h4" component="h2" gutterBottom>
                    {selectedMember.firstName} {selectedMember.lastName}
                  </Typography>
                  <Typography variant="h6" color="primary.main" gutterBottom>
                    {selectedMember.role}
                  </Typography>
                  <Typography variant="body1" paragraph>
                    {selectedMember.description}
                  </Typography>
                  
                  <Box sx={{ mt: 3 }}>
                    <Typography variant="h6" gutterBottom>
                      Contact & Social Media
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 2 }}>
                      <IconButton color="primary" size="large">
                        <GitHub />
                      </IconButton>
                      <IconButton color="primary" size="large">
                        <LinkedIn />
                      </IconButton>
                      <IconButton color="primary" size="large">
                        <Email />
                      </IconButton>
                    </Box>
                  </Box>

                  <Box sx={{ mt: 3 }}>
                    <Typography variant="h6" gutterBottom>
                      Skills & Expertise
                    </Typography>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                      {selectedMember.skills?.map((skill, index) => (
                        <Chip
                          key={index}
                          label={skill}
                          color="primary"
                          variant="outlined"
                          size="small"
                        />
                      ))}
                    </Box>
                  </Box>
                </Grid>
              </Grid>
            )}
          </Box>
        </Fade>
      </Modal>
    </div>
    
  );
}

export default LoginPage;