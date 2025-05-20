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
  Container,
  FormControlLabel,
  Switch
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
  BrandingWatermark,
  Upload,
  QrCode2
} from '@mui/icons-material';
import '../certificate/certificate.css';
import axios from 'axios';

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
  headerColor: '#000000',
  textColor: '#000000',
  fontFamily: 'Times New Roman',
  fontSize: 12,
  dateFormat: 'MMMM dd, yyyy',
  showBorder: true,
  borderWidth: 2,
  margins: { top: 50, right: 50, bottom: 50, left: 50 },
  backgroundImage: null,
  backgroundImageOpacity: 0.3,
  logoImage: null,
  logoWidth: 100,
  logoHeight: 100,
  logoPosition: 'top-center',
  watermarkImage: null,
  watermarkImageOpacity: 0.1,
  signatureImages: {},
  showQRCode: true,
  qrCodePosition: 'bottom-right'
};

export default function CertificateEditor({ initialData, onSave, onClose, onApply }) {
  const [certificate, setCertificate] = useState(initialData || defaultCertificate);
  const [activeTab, setActiveTab] = useState(0);
  const certificateRef = useRef(null);

  useEffect(() => {
    if (initialData) {
      console.log('CertificateEditor initialData:', initialData);
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
      
      // Ensure all the data is properly set before saving
      const finalCertificate = {
        ...certificate,
        // Make sure the title is set
        title: certificate.title.trim() ? certificate.title : 'CERTIFICATE',
        // Ensure we have a subtitle
        subtitle: certificate.subtitle || 'OF ACHIEVEMENT',
        // Make sure eventName is not the placeholder if we're in an event context
        eventName: certificate.eventName === '{Event Name}' && initialData?.eventName 
          ? initialData.eventName 
          : certificate.eventName
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
        subtitle: certificate.subtitle || 'OF ACHIEVEMENT',
        // Make sure eventName is not the placeholder if we're in an event context
        eventName: certificate.eventName === '{Event Name}' && initialData?.eventName 
          ? initialData.eventName 
          : certificate.eventName
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

  // Add new functions for image handling
  const handleImageUpload = async (type, file, signatoryName = null) => {
    if (!file) return;

    try {
      const formData = new FormData();
      
      if (signatoryName) {
        formData.append('signatures[' + signatoryName + ']', file);
      } else {
        formData.append(type, file);
      }

      const response = await axios.post(
        `http://localhost:8080/api/certificates/${certificate.eventId}/images`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      // Update the certificate state with the new image
      if (response.status === 200) {
        const reader = new FileReader();
        reader.onloadend = () => {
          const base64String = reader.result.split(',')[1];
          
          if (signatoryName) {
            setCertificate(prev => ({
              ...prev,
              signatureImages: {
                ...prev.signatureImages,
                [signatoryName]: base64String
              }
            }));
          } else {
            setCertificate(prev => ({
              ...prev,
              [type + 'Image']: base64String
            }));
          }
        };
        reader.readAsDataURL(file);
      }
    } catch (error) {
      console.error('Error uploading image:', error);
    }
  };

  const handleImageDelete = async (type, signatoryName = null) => {
    try {
      const params = {};
      if (signatoryName) {
        params.signatory = signatoryName;
      } else {
        params[type] = true;
      }

      await axios.delete(
        `http://localhost:8080/api/certificates/${certificate.eventId}/images`,
        { params }
      );

      // Update the certificate state
      if (signatoryName) {
        setCertificate(prev => ({
          ...prev,
          signatureImages: {
            ...prev.signatureImages,
            [signatoryName]: null
          }
        }));
      } else {
        setCertificate(prev => ({
          ...prev,
          [type + 'Image']: null
        }));
      }
    } catch (error) {
      console.error('Error deleting image:', error);
    }
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
        border: certificate.showBorder ? `${certificate.borderWidth}px solid ${certificate.borderColor || '#000000'}` : 'none',
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
      {/* Background Image */}
      {certificate.backgroundImage && (
        <Box
          component="img"
          src={`data:image/png;base64,${certificate.backgroundImage}`}
          sx={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            opacity: certificate.backgroundImageOpacity,
            zIndex: 0
          }}
        />
      )}

      {/* Logo */}
      {certificate.logoImage && (
        <Box
          component="img"
          src={`data:image/png;base64,${certificate.logoImage}`}
          sx={{
            position: 'absolute',
            width: certificate.logoWidth,
            height: certificate.logoHeight,
            objectFit: 'contain',
            zIndex: 2,
            ...(certificate.logoPosition === 'top-left' && {
              top: 20,
              left: 20
            }),
            ...(certificate.logoPosition === 'top-center' && {
              top: 20,
              left: '50%',
              transform: 'translateX(-50%)'
            }),
            ...(certificate.logoPosition === 'top-right' && {
              top: 20,
              right: 20
            })
          }}
        />
      )}

      {/* Watermark */}
      {certificate.watermarkImage && (
        <Box
          component="img"
          src={`data:image/png;base64,${certificate.watermarkImage}`}
          sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: '50%',
            height: 'auto',
            opacity: certificate.watermarkImageOpacity,
            zIndex: 1
          }}
        />
      )}
      
      <Box sx={{ 
        zIndex: 2, 
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

        {/* Signatures */}
        <Box sx={{ 
          display: 'flex', 
          justifyContent: 'space-around', 
          width: '100%', 
          mt: 4 
        }}>
          {certificate.signatories.map((signatory, index) => (
            <Box key={index} sx={{ textAlign: 'center', minWidth: 200 }}>
              {certificate.signatureImages?.[signatory.name] ? (
                <Box
                  component="img"
                  src={`data:image/png;base64,${certificate.signatureImages[signatory.name]}`}
                  sx={{
                    width: 150,
                    height: 60,
                    objectFit: 'contain',
                    mb: 1
                  }}
                />
              ) : (
                <Divider sx={{ width: 150, margin: '0 auto', mb: 1 }} />
              )}
              <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
                {signatory.name}
              </Typography>
              <Typography variant="body2">
                {signatory.title}
              </Typography>
            </Box>
          ))}
        </Box>

        {/* QR Code */}
        {certificate.showQRCode && (
          <Box
            sx={{
              position: 'absolute',
              ...(certificate.qrCodePosition === 'bottom-right' && {
                bottom: 20,
                right: 20
              }),
              ...(certificate.qrCodePosition === 'bottom-left' && {
                bottom: 20,
                left: 20
              }),
              ...(certificate.qrCodePosition === 'top-right' && {
                top: 20,
                right: 20
              }),
              ...(certificate.qrCodePosition === 'top-left' && {
                top: 20,
                left: 20
              }),
              zIndex: 2
            }}
          >
            {/* QR Code will be added by the backend */}
            <Box
              sx={{
                width: 80,
                height: 80,
                border: '1px solid #000',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <QrCode2 />
            </Box>
          </Box>
        )}
      </Box>
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
              onChange={(e, newValue) => setActiveTab(newValue)}
              sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
            >
              <Tab label="Content" icon={<TextFormat />} iconPosition="start" />
              <Tab label="Style" icon={<ColorLens />} iconPosition="start" />
              <Tab label="Images" icon={<BrandingWatermark />} iconPosition="start" />
              <Tab label="Signatories" icon={<Edit />} iconPosition="start" />
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
                   {/*   <Typography variant="body2">Background</Typography>
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
                      </Box>*/}
                    </Grid>
                    <Grid item xs={12} sm={6}>
                     {/* <Typography variant="body2">Border</Typography>
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
                          placeholder="#000000"
                          inputProps={{ pattern: '^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$' }}
                        />
                      </Box>*/}
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
                          placeholder="#000000"
                          inputProps={{ pattern: '^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$' }}
                        />
                      </Box>
                    </Grid>
                  </Grid>
                </Grid>
              </Grid>
            )}

            {activeTab === 2 && (
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>Background Image</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Button
                      variant="outlined"
                      component="label"
                      startIcon={<Upload />}
                    >
                      Upload Background
                      <input
                        type="file"
                        hidden
                        accept="image/*"
                        onChange={(e) => handleImageUpload('background', e.target.files[0])}
                      />
                    </Button>
                    {certificate.backgroundImage && (
                      <>
                        <IconButton onClick={() => handleImageDelete('background')}>
                          <Delete />
                        </IconButton>
                        <Typography variant="body2" color="text.secondary">
                          Opacity:
                        </Typography>
                        <Slider
                          value={certificate.backgroundImageOpacity}
                          onChange={(e, value) => handleTextChange('backgroundImageOpacity', value)}
                          min={0}
                          max={1}
                          step={0.1}
                          sx={{ width: 100 }}
                        />
                      </>
                    )}
                  </Box>
                </Grid>

                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>Logo</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Button
                      variant="outlined"
                      component="label"
                      startIcon={<Upload />}
                    >
                      Upload Logo
                      <input
                        type="file"
                        hidden
                        accept="image/*"
                        onChange={(e) => handleImageUpload('logo', e.target.files[0])}
                      />
                    </Button>
                    {certificate.logoImage && (
                      <>
                        <IconButton onClick={() => handleImageDelete('logo')}>
                          <Delete />
                        </IconButton>
                        <FormControl size="small" sx={{ minWidth: 120 }}>
                          <Select
                            value={certificate.logoPosition}
                            onChange={(e) => handleTextChange('logoPosition', e.target.value)}
                          >
                            <MenuItem value="top-left">Top Left</MenuItem>
                            <MenuItem value="top-center">Top Center</MenuItem>
                            <MenuItem value="top-right">Top Right</MenuItem>
                          </Select>
                        </FormControl>
                        <TextField
                          type="number"
                          label="Width"
                          value={certificate.logoWidth}
                          onChange={(e) => handleTextChange('logoWidth', Number(e.target.value))}
                          size="small"
                          sx={{ width: 100 }}
                        />
                        <TextField
                          type="number"
                          label="Height"
                          value={certificate.logoHeight}
                          onChange={(e) => handleTextChange('logoHeight', Number(e.target.value))}
                          size="small"
                          sx={{ width: 100 }}
                        />
                      </>
                    )}
                  </Box>
                </Grid>

                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>Watermark</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Button
                      variant="outlined"
                      component="label"
                      startIcon={<Upload />}
                    >
                      Upload Watermark
                      <input
                        type="file"
                        hidden
                        accept="image/*"
                        onChange={(e) => handleImageUpload('watermark', e.target.files[0])}
                      />
                    </Button>
                    {certificate.watermarkImage && (
                      <>
                        <IconButton onClick={() => handleImageDelete('watermark')}>
                          <Delete />
                        </IconButton>
                        <Typography variant="body2" color="text.secondary">
                          Opacity:
                        </Typography>
                        <Slider
                          value={certificate.watermarkImageOpacity}
                          onChange={(e, value) => handleTextChange('watermarkImageOpacity', value)}
                          min={0}
                          max={1}
                          step={0.1}
                          sx={{ width: 100 }}
                        />
                      </>
                    )}
                  </Box>
                </Grid>

                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom>QR Code Settings</Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={certificate.showQRCode}
                          onChange={(e) => handleTextChange('showQRCode', e.target.checked)}
                        />
                      }
                      label="Show QR Code"
                    />
                    {certificate.showQRCode && (
                      <FormControl size="small" sx={{ minWidth: 120 }}>
                        <Select
                          value={certificate.qrCodePosition}
                          onChange={(e) => handleTextChange('qrCodePosition', e.target.value)}
                        >
                          <MenuItem value="bottom-right">Bottom Right</MenuItem>
                          <MenuItem value="bottom-left">Bottom Left</MenuItem>
                          <MenuItem value="top-right">Top Right</MenuItem>
                          <MenuItem value="top-left">Top Left</MenuItem>
                        </Select>
                      </FormControl>
                    )}
                  </Box>
                </Grid>
              </Grid>
            )}

            {activeTab === 3 && (
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