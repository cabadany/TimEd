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
      alert('Please enter both ID-Number and Password');
    }
  };

  return (
    <div className="login-container">
      <img src="/timed 1.png" alt="TimEd Logo" className="logo" />
      <div className="login-box">
        <h2>Log In</h2>
        <input
          type="text"
          placeholder="ID-Number"
          value={idNumber}
          onChange={(e) => setIdNumber(e.target.value)}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button onClick={handleLogin}>Log In</button>
        <p className="forgot-password">Forgot Password?</p>
      </div>
    </div>
  );
}

export default LoginPage;