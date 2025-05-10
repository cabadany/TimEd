import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';
import axios from 'axios';
import { Modal, Box, Typography, Button, TextField, CircularProgress } from '@mui/material';
import { getAuth, sendPasswordResetEmail, signInWithEmailAndPassword } from "firebase/auth";

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
      // 🔄 Get email from backend using schoolId
      const emailResponse = await axios.get('http://localhost:8080/api/auth/auth/email-by-schoolId', {
        params: { schoolId: idNumber }
      });
  
      const email = emailResponse.data;
      console.log(email);
      console.log(password);
      // ✅ Authenticate using Firebase Authentication with email
      await signInWithEmailAndPassword(auth, email, password);
  
      // 🔓 If login success, call backend to get user role & token (now including password)
      const response = await axios.post('http://localhost:8080/api/auth/login-by-schoolId', {
        schoolId: idNumber
      });

      
      const data = response.data;
  
      if (data.success) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('role', data.role);
  
        if (data.role === 'ADMIN') {
          setIsAnimating(true);
          setTimeout(() => {
            navigate('/dashboard');
          }, 800);
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
      //  showNotification('Login failed: ' + (error.message || 'Unknown error'));
      showNotification('Login failed: Check Credentials or Network Connection');
      }
    }
  }, [idNumber, password, navigate]);
  
  

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
      setEmailSent(true);
      showNotification('Password reset email sent! Please check your inbox.', 'success');
    } catch (error) {
      console.error('Error sending password reset email:', error);
      let errorMessage = 'Failed to send password reset email.';

      // More specific error messages
      if (error.code === 'auth/user-not-found') {
        errorMessage = 'No account found with this email address.';
      } else if (error.code === 'auth/invalid-email') {
        errorMessage = 'Invalid email format.';
      } else if (error.code === 'auth/too-many-requests') {
        errorMessage = 'Too many attempts. Please try again later.';
      }

      showNotification(errorMessage);
    } finally {
      setResetLoading(false);
    }
  };

  // Email validation helper
  const isValidEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  return (
    <div className={`login-page ${isAnimating ? 'fade-out' : ''} ${isDarkMode ? 'dark-mode' : ''}`}>
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
                disabled={isLoading || isAnimating}
                aria-label="ID Icon"
                type="button"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <circle cx="12" cy="10" r="3"></circle>
                  <path d="M12 13c-2.67 0-8 1.34-8 4v3h16v-3c0-2.66-5.33-4-8-4z"></path>
                </svg>
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
      
    </div>
    
  );
}

export default LoginPage;