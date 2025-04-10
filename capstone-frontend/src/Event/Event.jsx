import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  IconButton,
  InputBase,
  Paper,
  TextField,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Avatar,
  Badge,
  Modal
} from '@mui/material';
import {
  Search,
  Settings,
  Notifications,
  FilterList,
  Home,
  Event,
  People,
  CalendarToday,
  AccessTime,
  Group,
  Upload,
  Close
} from '@mui/icons-material';
import './Event.css';

export default function EventPage() {
  const navigate = useNavigate();
  const location = useLocation();
  
  // State for form fields
  const [eventName, setEventName] = useState('CCS Party');
  const [college, setCollege] = useState('CCS');
  const [faculty, setFaculty] = useState('Mr. Frederick');
  const [date, setDate] = useState('25 January 2025');
  
  // State for upload modal
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadedFile, setUploadedFile] = useState(null);
  
  // Filter menu state
  const [filterAnchorEl, setFilterAnchorEl] = useState(null);
  const filterMenuOpen = Boolean(filterAnchorEl);
  const [activeFilter, setActiveFilter] = useState('');

  // Navigation handlers
  const handleNavigateToEvent = () => {
    navigate('/event');
  };

  const handleNavigateToDashboard = () => {
    navigate('/dashboard');
  };

  // Filter menu handlers
  const handleFilterClick = (event) => {
    setFilterAnchorEl(event.currentTarget);
  };

  const handleFilterClose = () => {
    setFilterAnchorEl(null);
  };

  const handleFilterSelect = (filterType) => {
    setActiveFilter(filterType);
    handleFilterClose();
  };

  // File upload handlers
  const handleUploadClick = () => {
    setShowUploadModal(true);
  };

  const handleFileChange = (event) => {
    setUploadedFile(event.target.files[0]);
  };

  const handleUploadSubmit = () => {
    // Handle the file upload logic here
    console.log('File uploaded:', uploadedFile);
    setShowUploadModal(false);
  };

  // Form submit handler
  const handleAddEvent = () => {
    // Handle form submission
    console.log('Event added:', { eventName, college, faculty, date });
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh', width: '100%', overflow: 'hidden' }}>
      {/* Sidebar */}
      <Box sx={{ 
        width: 260, 
        bgcolor: 'white', 
        borderRight: '1px solid #EAECF0',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0
      }}>
        <Box sx={{ p: 3, borderBottom: '1px solid #EAECF0', display: 'flex', justifyContent: 'center' }}>
            <img src="/timed 1.png" alt="TimeED Logo" style={{ height: 80 }} />
        </Box>
        <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
          <Button 
            startIcon={<Home />} 
            onClick={handleNavigateToDashboard}
            sx={{ 
              justifyContent: 'flex-start', 
              color: location.pathname === '/' ? '#0288d1' : '#64748B',
              fontWeight: location.pathname === '/' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            DASHBOARD
          </Button>
          <Button 
            startIcon={<Event />} 
            onClick={handleNavigateToEvent}
            sx={{ 
              justifyContent: 'flex-start', 
              color: location.pathname === '/event' ? '#0288d1' : '#64748B',
              fontWeight: location.pathname === '/event' ? 600 : 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            EVENT
          </Button>
          <Button 
            startIcon={<People />} 
            sx={{ 
              justifyContent: 'flex-start', 
              color: '#64748B',
              fontWeight: 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            ACCOUNTS
          </Button>
          <Button 
            startIcon={<Settings />} 
            sx={{ 
              justifyContent: 'flex-start', 
              color: '#64748B',
              fontWeight: 500,
              py: 1.5,
              px: 2,
              textAlign: 'left'
            }}
          >
            SETTING
          </Button>
        </Box>
      </Box>

      {/* Main Content */}
      <Box sx={{ 
        flex: 1, 
        display: 'flex', 
        flexDirection: 'column', 
        height: '100vh',
        overflow: 'hidden'
      }}>
        {/* Top Bar */}
        <Box sx={{ 
          py: 1.5, 
          px: 3,
          bgcolor: 'white', 
          borderBottom: '1px solid #EAECF0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <Typography variant="h5" fontWeight="600" color="#1E293B">
            Events
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Paper
              elevation={0}
              sx={{ 
                p: '2px 4px', 
                display: 'flex', 
                alignItems: 'center', 
                width: 300, 
                bgcolor: '#F8FAFC',
                border: '1px solid #E2E8F0',
                borderRadius: '4px'
              }}
            >
              <IconButton sx={{ p: '10px' }} aria-label="search">
                <Search sx={{ color: '#64748B' }} />
              </IconButton>
              <InputBase
                sx={{ ml: 1, flex: 1, fontSize: 14 }}
                placeholder="Search for something"
              />
            </Paper>
            <Button 
              variant="outlined" 
              startIcon={<FilterList />}
              size="small"
              onClick={handleFilterClick}
              sx={{
                borderColor: activeFilter ? '#0288d1' : '#E2E8F0',
                color: activeFilter ? '#0288d1' : '#64748B',
                textTransform: 'none',
                fontWeight: 500
              }}
            >
              {activeFilter || 'FILTER'}
            </Button>
            <Menu
              anchorEl={filterAnchorEl}
              open={filterMenuOpen}
              onClose={handleFilterClose}
              PaperProps={{
                elevation: 3,
                sx: { 
                  width: 180,
                  mt: 1,
                  '& .MuiMenuItem-root': {
                    fontSize: 14,
                    py: 1
                  }
                }
              }}
            >
              <MenuItem onClick={() => handleFilterSelect('Date')}>
                <ListItemIcon>
                  <CalendarToday fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Date</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => handleFilterSelect('Organizer')}>
                <ListItemIcon>
                  <Group fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Organizer</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => handleFilterSelect('Time')}>
                <ListItemIcon>
                  <AccessTime fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Time</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => handleFilterSelect('Event')}>
                <ListItemIcon>
                  <Event fontSize="small" sx={{ color: '#64748B' }} />
                </ListItemIcon>
                <ListItemText>Event</ListItemText>
              </MenuItem>
            </Menu>
            <IconButton>
              <Settings sx={{ color: '#64748B', fontSize: 20 }} />
            </IconButton>
            <IconButton>
              <Badge badgeContent="" color="error" variant="dot">
                <Notifications sx={{ color: '#64748B', fontSize: 20 }} />
              </Badge>
            </IconButton>
            <Avatar 
              sx={{ 
                width: 36, 
                height: 36,
                bgcolor: '#CBD5E1',
                color: 'white'
              }}
            >
              P
            </Avatar>
          </Box>
        </Box>

        {/* Event Content */}
        <Box sx={{ 
          p: 3, 
          flex: 1, 
          overflow: 'auto', 
          bgcolor: '#FFFFFF' 
        }}>
          <Typography variant="h6" fontWeight="600" color="#1E293B" sx={{ mb: 3 }}>
            Add New Event
          </Typography>

          <Paper 
            elevation={0} 
            sx={{ 
              p: 4, 
              mb: 3, 
              borderRadius: '8px', 
              border: '1px solid #E2E8F0'
            }}
          >
            <Typography variant="body2" color="#64748B" sx={{ mb: 4 }}>
              With just one click, users can initiate the setup of meetings, parties, webinars, or 
              social gatherings. This button typically opens a form where essential details—such 
              as the event name, date, time, location, and description—can be entered.
            </Typography>

            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 3 }}>
              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  Event Name
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  value={eventName}
                  onChange={(e) => setEventName(e.target.value)}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                      '&:hover fieldset': {
                        borderColor: '#CBD5E1',
                      },
                      '&.Mui-focused fieldset': {
                        borderColor: '#0288d1',
                      },
                    },
                  }}
                />
              </Box>

              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  College
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  select
                  value={college}
                  onChange={(e) => setCollege(e.target.value)}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                      '&:hover fieldset': {
                        borderColor: '#CBD5E1',
                      },
                      '&.Mui-focused fieldset': {
                        borderColor: '#0288d1',
                      },
                    },
                  }}
                >
                  <MenuItem value="CCS">CCS</MenuItem>
                  <MenuItem value="CEA">CEA</MenuItem>
                  <MenuItem value="CBA">CBA</MenuItem>
                </TextField>
              </Box>

              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                  <Typography variant="body2" fontWeight="500" color="#1E293B">
                    Faculty
                  </Typography>
                  <Button 
                    onClick={handleUploadClick}
                    sx={{ 
                      fontSize: '12px', 
                      textTransform: 'none', 
                      color: '#0288d1', 
                      fontWeight: 500,
                      p: 0,
                      height: 'auto'
                    }}
                  >
                    Upload Faculty List
                  </Button>
                </Box>
                <TextField
                  fullWidth
                  variant="outlined"
                  value={faculty}
                  onChange={(e) => setFaculty(e.target.value)}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                      '&:hover fieldset': {
                        borderColor: '#CBD5E1',
                      },
                      '&.Mui-focused fieldset': {
                        borderColor: '#0288d1',
                      },
                    },
                  }}
                />
              </Box>

              <Box>
                <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                  Date
                </Typography>
                <TextField
                  fullWidth
                  variant="outlined"
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      borderRadius: '4px',
                      fontSize: '14px',
                      '& fieldset': {
                        borderColor: '#E2E8F0',
                      },
                      '&:hover fieldset': {
                        borderColor: '#CBD5E1',
                      },
                      '&.Mui-focused fieldset': {
                        borderColor: '#0288d1',
                      },
                    },
                  }}
                  InputLabelProps={{
                    shrink: true,
                  }}
                />
              </Box>
            </Box>

            <Box sx={{ mt: 4 }}>
              <Button
                variant="contained"
                onClick={handleAddEvent}
                sx={{
                  bgcolor: '#0288d1',
                  '&:hover': {
                    bgcolor: '#0277bd',
                  },
                  textTransform: 'none',
                  borderRadius: '4px',
                  fontWeight: 500,
                  px: 4
                }}
              >
                Add Event
              </Button>
            </Box>
          </Paper>
        </Box>
      </Box>

      {/* Upload Modal */}
      <Modal
        open={showUploadModal}
        onClose={() => setShowUploadModal(false)}
        aria-labelledby="upload-modal-title"
      >
        <Box sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 500,
          bgcolor: 'background.paper',
          boxShadow: 24,
          borderRadius: 1,
          p: 0,
          overflow: 'hidden'
        }}>
          <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #E2E8F0' }}>
            <Typography variant="h6" fontWeight="600">
              Upload Faculty List
            </Typography>
            <IconButton onClick={() => setShowUploadModal(false)}>
              <Close />
            </IconButton>
          </Box>
          <Box sx={{ p: 3 }}>
            <Typography variant="body2" color="#64748B" sx={{ mb: 3 }}>
              Upload a CSV or Excel file containing the faculty list. The file should include columns for faculty name, department, and contact information.
            </Typography>
            
            <Box 
              sx={{ 
                border: '2px dashed #E2E8F0', 
                borderRadius: '4px', 
                p: 3, 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'center',
                mb: 3
              }}
            >
              <Upload sx={{ fontSize: 40, color: '#94A3B8', mb: 2 }} />
              <Typography variant="body2" fontWeight="500" color="#1E293B" sx={{ mb: 1 }}>
                {uploadedFile ? uploadedFile.name : 'Drag and drop your file here'}
              </Typography>
              <Typography variant="body2" color="#64748B" sx={{ mb: 2 }}>
                Supported formats: .csv, .xlsx, .xls
              </Typography>
              <Button
                variant="outlined"
                component="label"
                sx={{
                  borderColor: '#0288d1',
                  color: '#0288d1',
                  '&:hover': {
                    borderColor: '#0277bd',
                    bgcolor: 'rgba(2, 136, 209, 0.04)',
                  },
                  textTransform: 'none',
                  fontWeight: 500
                }}
              >
                Browse Files
                <input
                  type="file"
                  hidden
                  accept=".csv,.xlsx,.xls"
                  onChange={handleFileChange}
                />
              </Button>
            </Box>
          </Box>
          <Box sx={{ p: 2, bgcolor: '#F8FAFC', display: 'flex', justifyContent: 'flex-end', gap: 2, borderTop: '1px solid #E2E8F0' }}>
            <Button 
              variant="outlined" 
              onClick={() => setShowUploadModal(false)}
              sx={{
                borderColor: '#E2E8F0',
                color: '#64748B',
                '&:hover': {
                  borderColor: '#CBD5E1',
                  bgcolor: 'transparent',
                },
                textTransform: 'none',
                fontWeight: 500
              }}
            >
              Cancel
            </Button>
            <Button 
              variant="contained" 
              onClick={handleUploadSubmit}
              disabled={!uploadedFile}
              sx={{
                bgcolor: '#0288d1',
                '&:hover': {
                  bgcolor: '#0277bd',
                },
                textTransform: 'none',
                fontWeight: 500
              }}
            >
              Upload
            </Button>
          </Box>
        </Box>
      </Modal>
    </Box>
  );
}