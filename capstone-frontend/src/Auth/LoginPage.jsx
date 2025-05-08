import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';
import axios from 'axios';

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
  
  // Refs for input fields
  const idNumberRef = useRef(null);
  const passwordRef = useRef(null);
  const loginBtnRef = useRef(null);
  const forgotPasswordRef = useRef(null);

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
      setNotification(prev => ({...prev, visible: false}));
    }, 5000);
  };

  const closeNotification = () => {
    setNotification(prev => ({...prev, visible: false}));
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
    // Keep focus on password field after toggling
    passwordRef.current?.focus();
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleLogin();
    }
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
      // Handle forgot password action
      console.log('Forgot password clicked');
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
      const response = await axios.post('http://localhost:8080/api/auth/login', {
        schoolId: idNumber,
        password: password
      });
  
      const data = response.data;
  
      if (data.success) {
        const userRole = data.user?.role;
  
        // Save the auth details to localStorage
        localStorage.setItem('token', data.token);
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('role', data.role);
  
        // Debugging: log what's being stored
        console.log('Stored token:', localStorage.getItem('token'));
        console.log('Stored userId:', localStorage.getItem('userId'));
        console.log('Stored role:', localStorage.getItem('role'));
  
        if (data.role === 'ADMIN') {
          console.log('Admin login success:', data);
  
          setIsAnimating(true);
          setTimeout(() => {
            navigate('/dashboard');
          }, 800);
        } else {
          setIsLoading(false);
          showNotification('Access denied. Only admins can log in.');
          // Optional: clear any stored auth for non-admins
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
          localStorage.removeItem('role');
        }
  
      } else {
        setIsLoading(false);
        showNotification(data.message || 'Invalid login credentials');
      }
  
    } catch (error) {
      console.error('Login failed:', error);
      setIsLoading(false);
      showNotification(
        error.response?.data?.message || 'Something went wrong. Probably sabotage.'
      );
    }
  }, [idNumber, password, navigate, showNotification]);

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
  }, [handleLogin]); // Now we only depend on the memoized callback

  return (
    <div className={`login-page ${isAnimating ? 'fade-out' : ''} ${isDarkMode ? 'dark-mode' : ''}`}>
      {notification.visible && (
        <div className={`notification ${notification.type}`}>
          <div className="notification-icon">
            {notification.type === 'error' && (
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            )}
          </div>
          <div className="notification-content">
            {notification.message}
          </div>
          <button className="notification-close" onClick={closeNotification}>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      )}
      
      <div className="login-left">
        <img src="/timed 1.png" alt="TimEd Logo" className="logo animate-fade-in" />
        
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
            role="button"
            ref={forgotPasswordRef}
            onClick={() => console.log('Forgot password clicked')}
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
        </div>
      </div>
      
      <div className="login-right">
        <img 
          src="/login-illustration.png" 
          alt="Login Illustration" 
          className="illustration animate-fade-in"
        />
      </div>
    </div>
  );
}

export default LoginPage;