import { useState, useRef, useEffect } from 'react';
import {
  Box,
  TextField,
  Button,
  Typography,
  Paper,
  IconButton,
  Grid,
  Select,
  MenuItem,
  InputLabel,
  FormControl,
  Slider,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tabs,
  Tab,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Container
} from '@mui/material';
import {
  Close,
  Save,
  ColorLens,
  TextFormat,
  FormatBold,
  FormatItalic,
  FormatUnderlined,
  FormatAlignLeft,
  FormatAlignCenter,
  FormatAlignRight,
  Add,
  Delete,
  Edit,
  ExpandMore,
  BrandingWatermark
} from '@mui/icons-material';
import '../certificate/certificate.css';

const fontFamilies = [
  'Arial',
  'Times New Roman',
  'Courier New',
  'Georgia',
  'Verdana',
  'Palatino',
  'Garamond',
  'Bookman',
  'Tahoma',
  'Trebuchet MS'
];

const defaultCertificate = {
  title: 'CERTIFICATE',
  subtitle: 'OF ACHIEVEMENT',
  recipientText: 'THIS CERTIFICATE IS PROUDLY PRESENTED TO',
  recipientName: '{Recipient Name}',
  description: 'For outstanding participation in the event and demonstrating exceptional dedication throughout the program.',
  signatories: [
    { name: 'John Doe', title: 'REPRESENTATIVE' },
    { name: 'Jane Smith', title: 'REPRESENTATIVE' }
  ],
  eventName: '{Event Name}',
  eventDate: '{Event Date}',
  certificateNumber: '{Certificate Number}',
  backgroundColor: '#ffffff',
  borderColor: '#0047AB',
  headerColor: '#0047AB',
  textColor: '#000000',
  fontFamily: 'Times New Roman'
};

export default function CertificateEditor({ initialData, onSave, onClose, onApply }) {
  const [certificate, setCertificate] = useState(initialData || defaultCertificate);
  const [activeTab, setActiveTab] = useState(0);
  const certificateRef = useRef(null);

  useEffect(() => {
    if (initialData) {
      setCertificate(initialData);
    }
  }, [initialData]);

  const handleTextChange = (field, value) => {
    setCertificate(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleColorChange = (field, value) => {
    // Validate the color value is a proper hex color (with # prefix)
    const validColorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/;
    const colorToApply = validColorRegex.test(value) ? value : 
      (value.startsWith('#') ? value : `#${value}`);
    
    setCertificate(prev => ({
      ...prev,
      [field]: colorToApply
    }));
  };

  const handleSignatoryChange = (index, field, value) => {
    setCertificate(prev => {
      const updatedSignatories = [...prev.signatories];
      updatedSignatories[index] = {
        ...updatedSignatories[index],
        [field]: value
      };
      return {
        ...prev,
        signatories: updatedSignatories
      };
    });
  };

  const handleAddSignatory = () => {
    if (certificate.signatories.length < 3) {
      setCertificate(prev => ({
        ...prev,
        signatories: [...prev.signatories, { name: 'New Signatory', title: 'TITLE' }]
      }));
    }
  };

  const handleRemoveSignatory = (index) => {
    if (certificate.signatories.length > 1) {
      setCertificate(prev => {
        const updatedSignatories = [...prev.signatories];
        updatedSignatories.splice(index, 1);
        return {
          ...prev,
          signatories: updatedSignatories
        };
      });
    }
  };

  const handleSave = () => {
    try {
      // Add some basic validation
      if (!certificate.title.trim()) {
        alert('Please enter a certificate title');
        return;
      }
      
      // If eventName is provided in the form, use it for the certificate
      if (certificate.eventName === '{Event Name}' && document.querySelector('input[name="eventName"]')) {
        const eventNameInput = document.querySelector('input[name="eventName"]');
        if (eventNameInput && eventNameInput.value) {
          setCertificate(prev => ({
            ...prev,
            eventName: eventNameInput.value
          }));
        }
      }
      
      // Ensure all the data is properly set before saving
      const finalCertificate = {
        ...certificate,
        // Make sure the title is set
        title: certificate.title.trim() ? certificate.title : 'CERTIFICATE',
        // Ensure we have a subtitle
        subtitle: certificate.subtitle || 'OF ACHIEVEMENT'
      };
      
      // Call the parent component's save function with the complete data
      onSave(finalCertificate);
      
    } catch (error) {
      console.error('Error saving certificate template:', error);
      alert('Failed to save certificate template. Please try again.');
    }
  };

  const handleApply = () => {
    try {
      // Same validation as handleSave
      if (!certificate.title.trim()) {
        alert('Please enter a certificate title');
        return;
      }
      
      // Ensure all the data is properly set before applying
      const finalCertificate = {
        ...certificate,
        title: certificate.title.trim() ? certificate.title : 'CERTIFICATE',
        subtitle: certificate.subtitle || 'OF ACHIEVEMENT'
      };
      
      // Call the onApply function to apply the template to current event form
      if (onApply) {
        onApply(finalCertificate);
      }
      
    } catch (error) {
      console.error('Error applying certificate template:', error);
      alert('Failed to apply certificate template. Please try again.');
    }
  };

  const handleTabChange = (event, newValue) => {
    setActiveTab(newValue);
  };

  // Certificate component to show in preview
  const CertificatePreview = () => (
    <Box
      sx={{
        width: '100%',
        height: '100%',
        backgroundColor: certificate.backgroundColor || '#ffffff',
        position: 'relative',
        overflow: 'hidden',
        border: `12px solid ${certificate.borderColor || '#0047AB'}`,
        borderRadius: '8px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'space-between',
        color: certificate.textColor || '#000000',
        fontFamily: certificate.fontFamily || 'Times New Roman',
        padding: '16px 12px',
      }}
    >
      {/* Blue curved header background */}
      <Box 
        sx={{ 
          position: 'absolute', 
          top: -100, 
          left: -100, 
          width: 300, 
          height: 300, 
          backgroundColor: certificate.headerColor || '#0047AB',
          borderRadius: '50%',
          transform: 'rotate(-45deg)',
          zIndex: 0
        }} 
      />
      
      {/* Blue curved footer background */}
      <Box 
        sx={{ 
          position: 'absolute', 
          bottom: -100, 
          right: -100, 
          width: 300, 
          height: 300, 
          backgroundColor: certificate.headerColor || '#0047AB',
          borderRadius: '50%',
          transform: 'rotate(135deg)',
          zIndex: 0
        }} 
      />
      
      <Box sx={{ 
        zIndex: 1, 
        width: '100%', 
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        flex: 1
      }}>
        <Box sx={{ mb: 1 }}>
          <Typography 
            variant="h4" 
            component="div"
            sx={{ 
              fontWeight: 'bold', 
              fontSize: { xs: '24px', sm: '28px' },
              letterSpacing: '1px'
            }}
          >
            {certificate.title}
          </Typography>
          <Typography 
            variant="h5" 
            component="div"
            sx={{ 
              fontWeight: 'bold', 
              fontSize: { xs: '16px', sm: '20px' },
              letterSpacing: '2px',
              mb: 1
            }}
          >
            {certificate.subtitle}
          </Typography>
        </Box>
        
        <Divider sx={{ 
          width: '120px', 
          margin: '0 auto', 
          mb: 2,
          borderWidth: '1px',
          borderColor: certificate.textColor || '#000000',
          opacity: 0.6
        }} />
        
        <Typography 
          variant="body2"
          sx={{ 
            fontSize: { xs: '12px', sm: '14px' },
            mb: 0.5
          }}
        >
          {certificate.recipientText}
        </Typography>
        
        <Typography 
          variant="h6"
          component="div"
          sx={{ 
            fontStyle: 'italic',
            fontWeight: 'bold',
            fontSize: { xs: '18px', sm: '22px' },
            mb: 1.5
          }}
        >
          {certificate.recipientName}
        </Typography>
        
        <Typography 
          variant="body2"
          sx={{ 
            fontSize: { xs: '10px', sm: '12px' },
            mb: 1.5,
            maxWidth: '90%',
            margin: '0 auto'
          }}
        >
          {certificate.description}
        </Typography>
        
        <Typography 
          variant="body2"
          sx={{ 
            fontSize: { xs: '12px', sm: '14px' },
            fontWeight: 'bold',
            mb: 0.5
          }}
        >
          {certificate.eventName}
        </Typography>
        
        <Typography 
          variant="caption"
          sx={{ 
            fontSize: { xs: '10px', sm: '12px' },
            mb: 2
          }}
        >
          {certificate.eventDate}
        </Typography>
      </Box>
      
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-around', 
        width: '100%',
        mt: 'auto',
        mb: 1,
        zIndex: 1
      }}>
        {certificate.signatories.map((signatory, index) => (
          <Box 
            key={index} 
            sx={{ 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center',
              minWidth: '70px'
            }}
          >
            <Typography 
              variant="body2" 
              sx={{ 
                mb: 0.5, 
                fontStyle: 'italic',
                fontSize: { xs: '11px', sm: '14px' },
                borderBottom: '1px solid',
                pb: 0.5,
                width: '100%',
                textAlign: 'center'
              }}
            >
              {signatory.name}
            </Typography>
            <Typography 
              variant="caption" 
              sx={{ 
                fontSize: { xs: '8px', sm: '10px' }, 
                fontWeight: 'bold' 
              }}
            >
              {signatory.title}
            </Typography>
          </Box>
        ))}
      </Box>
      
      <Typography 
        variant="caption" 
        sx={{ 
          position: 'absolute',
          bottom: 5,
          right: 5,
          fontSize: '8px',
          opacity: 0.7,
          zIndex: 1
        }}
      >
        {certificate.certificateNumber}
      </Typography>
    </Box>
  );

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        p: 2, 
        borderBottom: '1px solid #eee' 
      }}>
        <Typography variant="h6">Certificate Template Editor</Typography>
        <Box>
          {onApply && (
            <Button 
              variant="outlined" 
              startIcon={<BrandingWatermark />} 
              onClick={handleApply}
              sx={{ mr: 1 }}
            >
              APPLY TEMPLATE
            </Button>
          )}
          <Button 
            variant="contained" 
            startIcon={<Save />} 
            onClick={handleSave}
            sx={{ mr: 1 }}
          >
            SAVE TEMPLATE
          </Button>
          <IconButton onClick={onClose}>
            <Close />
          </IconButton>
        </Box>
      </Box>

      {/* Main content - split into two columns */}
      <Box sx={{ 
        display: 'flex', 
        flexDirection: 'row', 
        flex: 1, 
        overflow: 'hidden',
        height: 'calc(100% - 60px)'
      }}>
        {/* Left column - Certificate Preview */}
        <Box sx={{ 
          width: '45%', 
          p: 2, 
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderRight: '1px solid #eee'
        }}>
          <Paper 
            elevation={3} 
            ref={certificateRef}
            sx={{ 
              width: '100%',
              maxHeight: '100%',
              display: 'flex',
              overflow: 'hidden'
            }}
          >
            <CertificatePreview />
          </Paper>
        </Box>

        {/* Right column - Controls */}
        <Box sx={{ 
          width: '55%', 
          display: 'flex', 
          flexDirection: 'column',
          overflow: 'hidden'
        }}>
          {/* Tabs for editing */}
          <Box sx={{ px: 2, pt: 2 }}>
            <Tabs 
              value={activeTab} 
              onChange={handleTabChange} 
              sx={{ borderBottom: '1px solid #eee' }}
              variant="fullWidth"
            >
              <Tab label="TEXT CONTENT" />
              <Tab label="STYLE OPTIONS" />
              <Tab label="SIGNATORIES" />
            </Tabs>
          </Box>

          {/* Tab panels */}
          <Box sx={{ 
            flex: 1, 
            p: 2, 
            overflow: 'auto',
            height: 'calc(100% - 48px)'
          }}>
            {activeTab === 0 && (
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Title"
                    value={certificate.title}
                    onChange={(e) => handleTextChange('title', e.target.value)}
                    variant="outlined"
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Subtitle"
                    value={certificate.subtitle}
                    onChange={(e) => handleTextChange('subtitle', e.target.value)}
                    variant="outlined"
                    size="small"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Recipient Text"
                    value={certificate.recipientText}
                    onChange={(e) => handleTextChange('recipientText', e.target.value)}
                    variant="outlined"
                    size="small"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Recipient Name Placeholder"
                    value={certificate.recipientName}
                    onChange={(e) => handleTextChange('recipientName', e.target.value)}
                    variant="outlined"
                    size="small"
                    helperText="This will be replaced with actual recipient names"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Description"
                    value={certificate.description}
                    onChange={(e) => handleTextChange('description', e.target.value)}
                    variant="outlined"
                    size="small"
                    multiline
                    rows={3}
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Event Name"
                    value={certificate.eventName}
                    onChange={(e) => handleTextChange('eventName', e.target.value)}
                    variant="outlined"
                    size="small"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Event Date"
                    value={certificate.eventDate}
                    onChange={(e) => handleTextChange('eventDate', e.target.value)}
                    variant="outlined"
                    size="small"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Certificate Number"
                    value={certificate.certificateNumber}
                    onChange={(e) => handleTextChange('certificateNumber', e.target.value)}
                    variant="outlined"
                    size="small"
                    helperText="This will be replaced with an actual certificate number"
                  />
                </Grid>
              </Grid>
            )}

            {activeTab === 1 && (
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>Font Family</Typography>
                  <FormControl fullWidth size="small">
                    <Select
                      value={certificate.fontFamily}
                      onChange={(e) => handleTextChange('fontFamily', e.target.value)}
                    >
                      {fontFamilies.map((font) => (
                        <MenuItem key={font} value={font} style={{ fontFamily: font }}>
                          {font}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>Colors</Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="body2">Background</Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <input
                          type="color"
                          value={certificate.backgroundColor}
                          onChange={(e) => handleColorChange('backgroundColor', e.target.value)}
                          style={{ marginRight: '8px', width: '40px', height: '30px', cursor: 'pointer' }}
                        />
                        <TextField 
                          value={certificate.backgroundColor}
                          onChange={(e) => handleColorChange('backgroundColor', e.target.value)}
                          size="small"
                          fullWidth
                          placeholder="#FFFFFF"
                          inputProps={{ pattern: '^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$' }}
                        />
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="body2">Border</Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <input
                          type="color"
                          value={certificate.borderColor}
                          onChange={(e) => handleColorChange('borderColor', e.target.value)}
                          style={{ marginRight: '8px', width: '40px', height: '30px', cursor: 'pointer' }}
                        />
                        <TextField 
                          value={certificate.borderColor}
                          onChange={(e) => handleColorChange('borderColor', e.target.value)}
                          size="small"
                          fullWidth
                          placeholder="#0047AB"
                          inputProps={{ pattern: '^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$' }}
                        />
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="body2">Text</Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <input
                          type="color"
                          value={certificate.textColor}
                          onChange={(e) => handleColorChange('textColor', e.target.value)}
                          style={{ marginRight: '8px', width: '40px', height: '30px', cursor: 'pointer' }}
                        />
                        <TextField 
                          value={certificate.textColor}
                          onChange={(e) => handleColorChange('textColor', e.target.value)}
                          size="small"
                          fullWidth
                          placeholder="#000000"
                          inputProps={{ pattern: '^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$' }}
                        />
                      </Box>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="body2">Header</Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <input
                          type="color"
                          value={certificate.headerColor}
                          onChange={(e) => handleColorChange('headerColor', e.target.value)}
                          style={{ marginRight: '8px', width: '40px', height: '30px', cursor: 'pointer' }}
                        />
                        <TextField 
                          value={certificate.headerColor}
                          onChange={(e) => handleColorChange('headerColor', e.target.value)}
                          size="small"
                          fullWidth
                          placeholder="#0047AB"
                          inputProps={{ pattern: '^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$' }}
                        />
                      </Box>
                    </Grid>
                  </Grid>
                </Grid>
              </Grid>
            )}

            {activeTab === 2 && (
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="subtitle1">
                    {certificate.signatories.length} of 3 signatories
                  </Typography>
                  <Button 
                    size="small" 
                    startIcon={<Add />} 
                    onClick={handleAddSignatory}
                    disabled={certificate.signatories.length >= 3}
                    variant="outlined"
                  >
                    Add Signatory
                  </Button>
                </Box>
                
                {certificate.signatories.map((signatory, index) => (
                  <Paper 
                    key={index} 
                    elevation={1}
                    sx={{ 
                      p: 2, 
                      mb: 2,
                      borderRadius: '8px'
                    }}
                  >
                    <Box sx={{ 
                      display: 'flex', 
                      justifyContent: 'space-between', 
                      alignItems: 'center',
                      mb: 2
                    }}>
                      <Typography variant="subtitle2">Signatory {index + 1}</Typography>
                      <IconButton 
                        size="small" 
                        onClick={() => handleRemoveSignatory(index)}
                        disabled={certificate.signatories.length <= 1}
                        color="error"
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Box>
                    
                    <Grid container spacing={2}>
                      <Grid item xs={12} md={6}>
                        <TextField
                          fullWidth
                          label="Name"
                          value={signatory.name}
                          onChange={(e) => handleSignatoryChange(index, 'name', e.target.value)}
                          size="small"
                        />
                      </Grid>
                      <Grid item xs={12} md={6}>
                        <TextField
                          fullWidth
                          label="Title"
                          value={signatory.title}
                          onChange={(e) => handleSignatoryChange(index, 'title', e.target.value)}
                          size="small"
                        />
                      </Grid>
                    </Grid>
                  </Paper>
                ))}
              </Box>
            )}
          </Box>
        </Box>
      </Box>
    </Box>
  );
} 