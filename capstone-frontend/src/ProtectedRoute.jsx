import React from 'react';
import { Navigate } from 'react-router-dom';

// ProtectedRoute component to allow access only for authenticated users
const ProtectedRoute = ({ children, adminOnly = true }) => {
  const token = localStorage.getItem('token'); // Check if user is logged in
  const userRole = localStorage.getItem('role'); // Get user role from localStorage

  // If not logged in, redirect to login page
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // If adminOnly is true and role is not ADMIN, redirect to login page
  if (adminOnly && userRole !== 'ADMIN') {
    return <Navigate to="/login" replace />;
  }

  // If logged in and passes role check, render the protected component
  return children;
};

export default ProtectedRoute;
