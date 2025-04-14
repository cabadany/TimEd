import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';

function LoginPage() {
  const navigate = useNavigate();
  const [idNumber, setIdNumber] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = () => {
    // Placeholder logic for login
    if (idNumber && password) {
      navigate('/dashboard');
    } else {
      alert('Please enter both ID Number and Password');
    }
  };

  return (
    <div className="login-page">
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
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                  <polyline points="22,6 12,13 2,6"></polyline>
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
          
          <button className="login-btn" onClick={handleLogin}>
            Login Now
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