import React from 'react';
import { Navigate } from 'react-router-dom';

// ProtectedRoute component to allow access only for authenticated Admin users
const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem('token'); // Check if user is logged in
  const userRole = localStorage.getItem('role'); // Get user role from localStorage

  // If not logged in or not an Admin, redirect to login page
  if (!token || userRole !== 'ADMIN') {
    return <Navigate to="/login" replace />;
  }

  // If logged in and role is ADMIN, render the protected component
  return children;
};

export default ProtectedRoute;
