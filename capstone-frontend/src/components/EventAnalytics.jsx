import React from 'react';
import { Box, Typography, useTheme } from '@mui/material';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const COLORS = ['#1976d2', '#ffa726', '#388e3c', '#d32f2f', '#7b1fa2'];

export default function EventAnalytics({ data, title }) {
  const theme = useTheme();
  const darkMode = theme.palette.mode === 'dark' || theme.darkMode;

  return (
    <Box sx={{
      borderRadius: 2,
      boxShadow: darkMode ? '0 2px 8px rgba(0,0,0,0.25)' : '0 2px 8px rgba(0,0,0,0.05)',
      p: 3,
      mb: 3
    }}>
      <Typography variant="h6" fontWeight={600} sx={{ mb: 2, color: darkMode ? 'white' : '#1E293B' }}>{title || 'Event Analytics'}</Typography>
      <ResponsiveContainer width="100%" height={300}>
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={100}
            fill="#8884d8"
            label
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip contentStyle={{ background: darkMode ? '#222' : '#fff', color: darkMode ? '#fff' : '#333' }} />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </Box>
  );
} 