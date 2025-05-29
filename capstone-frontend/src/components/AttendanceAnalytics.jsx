import React from 'react';
import { Box, Typography, useTheme } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts';

export default function AttendanceAnalytics({ data, title }) {
  const theme = useTheme();
  const darkMode = theme.palette.mode === 'dark' || theme.darkMode;

  return (
    <Box sx={{
      borderRadius: 2,
      boxShadow: darkMode ? '0 2px 8px rgba(0,0,0,0.25)' : '0 2px 8px rgba(0,0,0,0.05)',
      p: 3,
      mb: 3
    }}>
      <Typography variant="h6" fontWeight={600} sx={{ mb: 2, color: darkMode ? 'white' : '#1E293B' }}>
        {title || 'Attendance Analytics'}
      </Typography>
      
      {/* Centering wrapper */}
      <Box sx={{
        display: 'flex',
        justifyContent: 'center',
        width: '100%'
      }}>
        <ResponsiveContainer width="80%" height={200}>
          <BarChart data={data} margin={{ top: 16, right: 24, left: 0, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={darkMode ? '#333' : '#eee'} />
            <XAxis dataKey="time" stroke={darkMode ? '#fff' : '#333'} />
            <YAxis stroke={darkMode ? '#fff' : '#333'} allowDecimals={false} />
            <Tooltip contentStyle={{ background: darkMode ? '#222' : '#fff', color: darkMode ? '#fff' : '#333' }} />
            <Legend />
            <Bar dataKey="timeInCount" fill={darkMode ? '#90caf9' : '#1976d2'} name="Time-In Count" />
            <Bar dataKey="timeOutCount" fill={darkMode ? '#f48fb1' : '#d32f2f'} name="Time-Out Count" />
          </BarChart>
        </ResponsiveContainer>
      </Box>
    </Box>
  );
}