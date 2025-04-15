import React, { useState } from 'react';
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

  const showNotification = (message, type = 'error') => {
    setNotification({
      visible: true,
      message,
      type
    });
    
    // Auto-dismiss after 5 seconds
    setTimeout(() => {
      setNotification(prev => ({...prev, visible: false}));
    }, 5000);
  };

  const closeNotification = () => {
    setNotification(prev => ({...prev, visible: false}));
  };

  const handleLogin = async () => {
    if (!idNumber || !password) {
      showNotification('Please enter both ID Number and Password');
      return;
    }
  
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
          showNotification('Access denied. Only admins can log in.');
          // Optional: clear any stored auth for non-admins
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
          localStorage.removeItem('role');
        }
  
      } else {
        showNotification(data.message || 'Invalid login credentials');
      }
  
    } catch (error) {
      console.error('Login failed:', error);
      showNotification(
        error.response?.data?.message || 'Something went wrong. Probably sabotage.'
      );
    }
  };
  

  return (
    <div className={`login-page ${isAnimating ? 'fade-out' : ''}`}>
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
        <img src="/timed 1.png" alt="TimEd Logo" className="logo" />
        
        <div className="login-form">
          <h2 className="login-title">Login</h2>
          
          <div className="form-group">
            <label className="form-label">ID Number</label>
            <div className="input-container">
              <input
                type="text"
                className="form-input"
                placeholder="##-####-###"
                value={idNumber}
                onChange={(e) => setIdNumber(e.target.value)}
              />
              <button className="input-icon-btn">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                  <circle cx="12" cy="10" r="3"></circle>
                  <path d="M12 13c-2.67 0-8 1.34-8 4v3h16v-3c0-2.66-5.33-4-8-4z"></path>
                </svg>
              </button>
            </div>
          </div>
          
          <div className="form-group">
            <label className="form-label">Password</label>
            <div className="input-container">
              <input
                type="password"
                className="form-input"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button className="input-icon-btn">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
              </button>
            </div>
          </div>
          
          <div className="forgot-password">Forgot Password?</div>
          
          <button 
            className={`login-btn ${isAnimating ? 'btn-clicked' : ''}`} 
            onClick={handleLogin}
            disabled={isAnimating}
          >
            {isAnimating ? 'Logging in...' : 'Login Now'}
          </button>
        </div>
      </div>
      
      <div className="login-right">
        <img 
          src="/login-illustration.png" 
          alt="Login Illustration" 
          className="illustration"
        />
      </div>
    </div>
  );
}

export default LoginPage;