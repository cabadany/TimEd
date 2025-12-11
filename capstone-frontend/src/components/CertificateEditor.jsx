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
import { useTheme } from '../contexts/ThemeContext';
import { API_BASE_URL, getApiUrl, API_ENDPOINTS } from '../utils/api';

const fontFamilies = [
  'Arial',
  'Times New Roman',
  'Georgia',
  'Palatino',
  'Garamond',
  'Bookman',
  'Baskerville',
  'Didot',
  'Cambria',
  'Copperplate',
  'Optima', 
  'Calibri',
  'Candara',
  'Century Gothic',
  'Futura',
  'Helvetica',
  'Montserrat',
  'Open Sans',
  'Roboto',
  'Segoe UI'
];

const defaultCertificate = {
  title: 'CERTIFICATE',
  subtitle: 'OF ACHIEVEMENT',
  recipientText: 'THIS CERTIFICATE IS PROUDLY PRESENTED TO',
  recipientName: '{LastName, FirstName}',
  description: 'For outstanding participation in the event and demonstrating exceptional dedication throughout the program.',
  signatories: [
    { name: 'John Doe', title: 'REPRESENTATIVE' },
    { name: 'Jane Smith', title: 'REPRESENTATIVE' }
  ],
  eventName: '{Event Name}',
  eventDate: '{Event Date}',
  certificateNumber: '{Certificate Number}',
  backgroundColor: '#ffffff',
  headerColor: '#1E3A8A',
  textColor: '#333333',
  fontFamily: 'Georgia',
  fontSize: 12,
  dateFormat: 'MMMM dd, yyyy',
  showBorder: true,
  borderWidth: 1,
  borderColor: '#D1D5DB',
  borderStyle: 'solid',
  frameStyle: 'classic',
  showDecorations: true,
  decorativeCorners: true,
  showRibbon: false,
  ribbonPosition: 'bottom-center',
  ribbonColor: '#D4AF37',
  showSeal: false,
  sealPosition: 'bottom-right',
  sealColor: '#C0C0C0',
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
  const { darkMode } = useTheme();

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
        getApiUrl(API_ENDPOINTS.CERTIFICATE_IMAGES(certificate.eventId)),
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
        getApiUrl(API_ENDPOINTS.CERTIFICATE_IMAGES(certificate.eventId)),
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
  const CertificatePreview = () => {
    // Helper function to render decorative corners
    const renderCorners = () => {
      if (!certificate.decorativeCorners) return null;
      
      const cornerSize = 30;
      const cornerColor = certificate.headerColor || '#1E3A8A';
      
      return (
        <>
          {/* Top Left Corner */}
          <Box sx={{
            position: 'absolute',
            top: 10,
            left: 10,
            width: cornerSize,
            height: cornerSize,
            zIndex: 2,
            borderLeft: `2px solid ${cornerColor}`,
            borderTop: `2px solid ${cornerColor}`,
          }} />
          
          {/* Top Right Corner */}
          <Box sx={{
            position: 'absolute',
            top: 10,
            right: 10,
            width: cornerSize,
            height: cornerSize,
            zIndex: 2,
            borderRight: `2px solid ${cornerColor}`,
            borderTop: `2px solid ${cornerColor}`,
          }} />
          
          {/* Bottom Left Corner */}
          <Box sx={{
            position: 'absolute',
            bottom: 10,
            left: 10,
            width: cornerSize,
            height: cornerSize,
            zIndex: 2,
            borderLeft: `2px solid ${cornerColor}`,
            borderBottom: `2px solid ${cornerColor}`,
          }} />
          
          {/* Bottom Right Corner */}
          <Box sx={{
            position: 'absolute',
            bottom: 10,
            right: 10,
            width: cornerSize,
            height: cornerSize,
            zIndex: 2,
            borderRight: `2px solid ${cornerColor}`,
            borderBottom: `2px solid ${cornerColor}`,
          }} />
        </>
      );
    };
    
    // Helper function to render frame based on style
    const renderFrame = () => {
      if (!certificate.showBorder || !certificate.frameStyle || certificate.frameStyle === 'none') {
        return null;
      }
      
      const borderColor = certificate.borderColor || '#D1D5DB';
      const borderWidth = certificate.borderWidth || 1;
      
      switch (certificate.frameStyle) {
        case 'classic':
          return (
            <Box sx={{
              position: 'absolute',
              top: 15,
              left: 15,
              right: 15,
              bottom: 15,
              border: `${borderWidth}px solid ${borderColor}`,
              zIndex: 1,
              pointerEvents: 'none',
            }} />
          );
        
        case 'double':
          return (
            <>
              <Box sx={{
                position: 'absolute',
                top: 15,
                left: 15,
                right: 15,
                bottom: 15,
                border: `${borderWidth}px solid ${borderColor}`,
                zIndex: 1,
                pointerEvents: 'none',
              }} />
              <Box sx={{
                position: 'absolute',
                top: 20,
                left: 20,
                right: 20,
                bottom: 20,
                border: `${borderWidth}px solid ${borderColor}`,
                zIndex: 1,
                pointerEvents: 'none',
              }} />
            </>
          );
        
        case 'ornate':
          return (
            <Box sx={{
              position: 'absolute',
              top: 12,
              left: 12,
              right: 12,
              bottom: 12,
              border: `${borderWidth}px solid ${borderColor}`,
              borderImage: `linear-gradient(45deg, ${borderColor} 0%, rgba(0,0,0,0.1) 50%, ${borderColor} 100%) 1`,
              zIndex: 1,
              pointerEvents: 'none',
              '&::before': {
                content: '""',
                position: 'absolute',
                top: 5,
                left: 5,
                right: 5,
                bottom: 5,
                border: `${borderWidth}px solid ${borderColor}`,
                opacity: 0.6,
              }
            }} />
          );
          
        case 'modern':
          return (
            <Box sx={{
              position: 'absolute',
              top: 15,
              left: 15,
              right: 15,
              bottom: 15,
              borderLeft: `${borderWidth}px solid ${borderColor}`,
              borderRight: `${borderWidth}px solid ${borderColor}`,
              '&::before': {
                content: '""',
                position: 'absolute',
                top: 10,
                left: 30,
                right: 30,
                height: `${borderWidth}px`,
                backgroundColor: borderColor,
              },
              '&::after': {
                content: '""',
                position: 'absolute',
                bottom: 10,
                left: 30,
                right: 30,
                height: `${borderWidth}px`,
                backgroundColor: borderColor,
              },
              zIndex: 1,
              pointerEvents: 'none',
            }} />
          );
          
        default:
          return null;
      }
    };
    
    // Helper function to render ribbon
    const renderRibbon = () => {
      if (!certificate.showRibbon) return null;
      
      const ribbonColor = certificate.ribbonColor || '#D4AF37';
      
      switch(certificate.ribbonPosition) {
        case 'bottom-center':
          return (
            <Box sx={{
              position: 'absolute',
              bottom: '15%',
              left: '50%',
              transform: 'translateX(-50%)',
              zIndex: 5,
              width: '120px',
              height: '120px',
            }}>
              <Box sx={{
                position: 'relative',
                width: '100%',
                height: '100%',
              }}>
                {/* Ribbon Base */}
                <Box sx={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
                  width: '80px',
                  height: '80px',
                  borderRadius: '50%',
                  background: `radial-gradient(circle, ${ribbonColor} 0%, ${ribbonColor}CC 60%, ${ribbonColor}88 100%)`,
                  boxShadow: '0 4px 8px rgba(0,0,0,0.2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  '&::before': {
                    content: '""',
                    position: 'absolute',
                    width: '60px',
                    height: '60px',
                    borderRadius: '50%',
                    border: '2px solid rgba(255,255,255,0.5)',
                  }
                }}>
                  {/* Inner Star */}
                  <Box sx={{
                    width: '40px',
                    height: '40px',
                    background: 'rgba(255,255,255,0.9)',
                    clipPath: 'polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%)',
                  }} />
                </Box>
                {/* Left Ribbon */}
                <Box sx={{
                  position: 'absolute',
                  bottom: '35%',
                  left: '5%',
                  width: '30px',
                  height: '60px',
                  backgroundColor: ribbonColor,
                  transform: 'rotate(-30deg)',
                  clipPath: 'polygon(0 0, 100% 0, 100% 80%, 50% 100%, 0 80%)',
                  boxShadow: '2px 2px 5px rgba(0,0,0,0.2)',
                }} />
                {/* Right Ribbon */}
                <Box sx={{
                  position: 'absolute',
                  bottom: '35%',
                  right: '5%',
                  width: '30px',
                  height: '60px',
                  backgroundColor: ribbonColor,
                  transform: 'rotate(30deg)',
                  clipPath: 'polygon(0 0, 100% 0, 100% 80%, 50% 100%, 0 80%)',
                  boxShadow: '-2px 2px 5px rgba(0,0,0,0.2)',
                }} />
              </Box>
            </Box>
          );
        default:
          return null;
      }
    };
    
    // Helper function to render seal
    const renderSeal = () => {
      if (!certificate.showSeal) return null;
      
      const sealColor = certificate.sealColor || '#C0C0C0';
      let sealPosition = {};
      
      switch(certificate.sealPosition) {
        case 'bottom-right':
          sealPosition = {
            bottom: '50px',
            right: '40px',
          };
          break;
        case 'bottom-left':
          sealPosition = {
            bottom: '50px',
            left: '40px',
          };
          break;
        case 'top-right':
          sealPosition = {
            top: '50px',
            right: '40px',
          };
          break;
        case 'top-left':
          sealPosition = {
            top: '50px',
            left: '40px',
          };
          break;
        default:
          sealPosition = {
            bottom: '50px',
            right: '40px',
          };
      }
      
      return (
        <Box sx={{
          position: 'absolute',
          ...sealPosition,
          width: '80px',
          height: '80px',
          zIndex: 5,
        }}>
          {/* Outer Ring */}
          <Box sx={{
            position: 'relative',
            width: '100%',
            height: '100%',
            borderRadius: '50%',
            background: `conic-gradient(from 0deg, ${sealColor}, ${sealColor}88, ${sealColor}, ${sealColor}88, ${sealColor})`,
            boxShadow: '0 2px 10px rgba(0,0,0,0.15)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            '&::after': {
              content: '""',
              position: 'absolute',
              width: '70px',
              height: '70px',
              borderRadius: '50%',
              background: sealColor,
              boxShadow: 'inset 0 0 15px rgba(0,0,0,0.1)',
            },
            '&::before': {
              content: '""',
              position: 'absolute',
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              border: '2px dashed rgba(255,255,255,0.6)',
              zIndex: 2,
            }
          }}>
            {/* Embossed Effect */}
            <Box sx={{
              position: 'absolute',
              width: '58px',
              height: '58px',
              borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(255,255,255,0.8) 0%, rgba(255,255,255,0) 70%)',
              zIndex: 3,
              transform: 'translateY(-2px)'
            }} />
          </Box>
        </Box>
      );
    };
    
    return (
      <Box
        sx={{
          width: '100%',
          height: '100%',
          backgroundColor: certificate.backgroundColor || '#ffffff',
          position: 'relative',
          overflow: 'hidden',
          border: certificate.showBorder && (!certificate.frameStyle || certificate.frameStyle === 'none') ? 
            `${certificate.borderWidth}px ${certificate.borderStyle || 'solid'} ${certificate.borderColor || '#000000'}` : 'none',
          borderRadius: '8px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'space-between',
          color: certificate.textColor || '#000000',
          fontFamily: certificate.fontFamily || 'Times New Roman',
          padding: '20px 24px',
          boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.05)',
          backgroundImage: 'linear-gradient(0deg, rgba(250,250,250,0.4) 0%, rgba(255,255,255,0) 100%)',
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

        {/* Frame and Decorations */}
        {renderFrame()}
        {certificate.showDecorations && renderCorners()}
        {renderRibbon()}
        {renderSeal()}

        {/* Decorative elements */}
        {certificate.showDecorations && (
          <>
            <Box sx={{ 
              position: 'absolute', 
              top: 0, 
              left: 0, 
              right: 0, 
              height: '6px', 
              background: `linear-gradient(90deg, transparent, ${certificate.headerColor || '#000'}, transparent)`,
              opacity: 0.5,
              zIndex: 1
            }} />

            <Box sx={{ 
              position: 'absolute', 
              bottom: 0, 
              left: 0, 
              right: 0, 
              height: '6px', 
              background: `linear-gradient(90deg, transparent, ${certificate.headerColor || '#000'}, transparent)`,
              opacity: 0.5,
              zIndex: 1
            }} />
          </>
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
        
        <Box sx={{ 
          zIndex: 2, 
          width: '100%', 
          textAlign: 'center',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          flex: 1,
          position: 'relative',
          paddingTop: '20px'
        }}>
          <Box sx={{ mb: 2 }}>
            <Typography 
              variant="h4" 
              component="div"
              sx={{ 
                fontWeight: 'bold', 
                fontSize: { xs: '26px', sm: '32px' },
                letterSpacing: '2px',
                color: certificate.headerColor || certificate.textColor || '#000000',
                textTransform: 'uppercase',
                position: 'relative',
                display: 'inline-block',
              }}
            >
              {certificate.title}
            </Typography>
            <Typography 
              variant="h5" 
              component="div"
              sx={{ 
                fontWeight: 'bold', 
                fontSize: { xs: '18px', sm: '22px' },
                letterSpacing: '3px',
                mb: 1,
                textTransform: 'uppercase',
              }}
            >
              {certificate.subtitle}
            </Typography>
          </Box>
          
          <Box sx={{ 
            width: '180px', 
            margin: '0 auto', 
            mb: 3,
            position: 'relative',
          }}>
            <Divider sx={{ 
              borderWidth: '2px',
              borderColor: certificate.textColor || '#000000',
              opacity: 0.6
            }} />
          </Box>
          
          <Typography 
            variant="body2"
            sx={{ 
              fontSize: { xs: '13px', sm: '15px' },
              mb: 0.5,
              fontWeight: 500,
              letterSpacing: '0.5px'
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
              fontSize: { xs: '20px', sm: '26px' },
              mb: 2.5,
              color: certificate.headerColor || certificate.textColor || '#000000',
              position: 'relative',
              '&::after': {
                content: '""',
                position: 'absolute',
                bottom: '-10px',
                left: '50%',
                transform: 'translateX(-50%)',
                width: '40%',
                height: '1px',
                backgroundColor: 'rgba(0,0,0,0.2)',
              }
            }}
          >
            {certificate.recipientName}
          </Typography>
          
          <Typography 
            variant="body2"
            sx={{ 
              fontSize: { xs: '11px', sm: '14px' },
              mb: 2.5,
              maxWidth: '85%',
              margin: '0 auto',
              lineHeight: 1.6,
              fontStyle: 'italic',
              position: 'relative',
               color:'#1e1e1e'
            }}
          >
            {certificate.description}
          </Typography>
          
          <Box sx={{
            mb: 1,
            backgroundColor: 'rgba(0,0,0,0.02)',
            padding: '8px 16px',
            borderRadius: '4px',
            display: 'inline-block',
            margin: '0 auto',
            color:'#1e1e1e'
          }}>
            <Typography 
              variant="body2"
              sx={{ 
                fontSize: { xs: '13px', sm: '16px' },
                fontWeight: 'bold',
                mb: 0.5
              }}
            >
              {certificate.eventName}
            </Typography>
            
            <Typography 
              variant="caption"
              sx={{ 
                fontSize: { xs: '11px', sm: '13px' },
                opacity: 0.8
              }}
            >
              {certificate.eventDate}
            </Typography>
          </Box>

          {/* Signatures */}
          <Box sx={{ 
            display: 'flex', 
            justifyContent: 'space-around', 
            width: '100%', 
            mt: 4 
          }}>
            {certificate.signatories.map((signatory, index) => (
              <Box key={index} sx={{ 
                textAlign: 'center', 
                minWidth: 180,
                padding: '10px',
                borderTop: '1px dotted rgba(0,0,0,0.1)'
              }}>
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
                <Typography variant="subtitle1" sx={{ fontWeight: 'bold', fontSize: '14px' }}>
                  {signatory.name}
                </Typography>
                <Typography variant="body2" sx={{ fontSize: '12px', letterSpacing: '0.5px' }}>
                  {signatory.title}
                </Typography>
              </Box>
            ))}
          </Box>

          {/* Certificate number - bottom left */}
          <Typography 
            variant="caption"
            sx={{ 
              position: 'absolute',
              bottom: 0,
              left: 0,
              fontSize: '10px',
              opacity: 0.7
            }}
          >
            {certificate.certificateNumber}
          </Typography>

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
                zIndex: 2,
                boxShadow: '0 2px 6px rgba(0,0,0,0.1)',
                backgroundColor: 'rgba(255,255,255,0.8)',
                padding: '4px',
                borderRadius: '4px'
              }}
            >
              {/* QR Code will be added by the backend */}
              <Box
                sx={{
                  width: 70,
                  height: 70,
                  border: '1px solid rgba(0,0,0,0.1)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  backgroundColor: '#fff'
                }}
              >
                <QrCode2 sx={{ opacity: 0.7 }} />
              </Box>
            </Box>
          )}
        </Box>
      </Box>
    );
  };

  return (
    <Box sx={{ 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
    }}>
      {/* Header */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        p: 2, 
        borderBottom: '1px solid',
        borderColor: darkMode ? '#333333' : '#E2E8F0',
        bgcolor: darkMode ? '#1e1e1e' : '#ffffff'
      }}>
        <Typography variant="h6" sx={{ 
          color: darkMode ? '#f5f5f5' : 'inherit'
        }}>
          Certificate Template Editor
        </Typography>
        <Box>
          {onApply && (
            <Button 
              variant="outlined" 
              startIcon={<BrandingWatermark />} 
              onClick={handleApply}
              sx={{
                mr: 1,
                borderColor: darkMode ? '#555555' : '#E2E8F0',
                color: darkMode ? '#90caf9' : '#0288d1',
                '&:hover': {
                  borderColor: darkMode ? '#90caf9' : '#0288d1',
                  bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'rgba(2, 136, 209, 0.04)'
                }
              }}
            >
              Apply Template
            </Button>
          )}
          <Button 
            variant="contained" 
            startIcon={<Save />} 
            onClick={handleSave}
            sx={{
              mr: 1,
              bgcolor: darkMode ? '#90caf9' : '#0288d1',
              color: darkMode ? '#1e1e1e' : '#ffffff',
              '&:hover': {
                bgcolor: darkMode ? '#42a5f5' : '#0277bd'
              }
            }}
          >
            Save Template
          </Button>
          <IconButton 
            onClick={onClose}
            sx={{ 
              color: darkMode ? '#aaaaaa' : '#64748B',
              '&:hover': {
                bgcolor: darkMode ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.04)'
              }
            }}
          >
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
        height: 'calc(100% - 60px)',
        bgcolor: darkMode ? '#1e1e1e' : '#ffffff'
      }}>
        {/* Left column - Certificate Preview */}
        <Box sx={{ 
          width: '45%', 
          p: 2, 
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderRight: '1px solid',
          borderColor: darkMode ? '#333333' : '#E2E8F0',
          bgcolor: darkMode ? '#2d2d2d' : '#f8fafc',
             color: darkMode ? '#2d2d2d' : '#f8fafc'
        }}>
          <Paper 
            elevation={3} 
            ref={certificateRef}
            sx={{ 
              width: '100%',
              maxHeight: '100%',
              display: 'flex',
              overflow: 'hidden',
              bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
              border: '1px solid',
              borderColor: darkMode ? '#333333' : '#E2E8F0'
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
          overflow: 'hidden',
          bgcolor: darkMode ? '#1e1e1e' : '#ffffff'
        }}>
          {/* Tabs for editing */}
          <Tabs 
            value={activeTab} 
            onChange={handleTabChange}
            sx={{
              borderBottom: '1px solid',
              borderColor: darkMode ? '#333333' : '#E2E8F0',
              '& .MuiTab-root': {
                color: darkMode ? '#aaaaaa' : '#64748B',
                '&.Mui-selected': {
                  color: darkMode ? '#90caf9' : '#0288d1'
                }
              },
              '& .MuiTabs-indicator': {
                bgcolor: darkMode ? '#90caf9' : '#0288d1'
              }
            }}
          >
            <Tab 
              label="Content" 
              sx={{ 
                textTransform: 'none',
                fontSize: '0.875rem',
                fontWeight: 500
              }} 
            />
            <Tab 
              label="Style" 
              sx={{ 
                textTransform: 'none',
                fontSize: '0.875rem',
                fontWeight: 500
              }} 
            />
            <Tab 
              label="Images" 
              sx={{ 
                textTransform: 'none',
                fontSize: '0.875rem',
                fontWeight: 500
              }} 
            />
            <Tab 
              label="Signatories" 
              sx={{ 
                textTransform: 'none',
                fontSize: '0.875rem',
                fontWeight: 500
              }} 
            />
          </Tabs>

          {/* Tab content area with scrolling */}
          <Box sx={{ 
            flex: 1, 
            overflow: 'auto',
            p: 2,
            bgcolor: darkMode ? '#2d2d2d' : '#f8fafc',
            '&::-webkit-scrollbar': {
              width: '8px',
            },
            '&::-webkit-scrollbar-track': {
              background: darkMode ? '#1e1e1e' : '#f1f1f1',
            },
            '&::-webkit-scrollbar-thumb': {
              background: darkMode ? '#555555' : '#cbd5e1',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: darkMode ? '#777777' : '#94a3b8',
            }
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
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                        '& fieldset': {
                          borderColor: darkMode ? '#333333' : '#E2E8F0',
                        },
                        '&:hover fieldset': {
                          borderColor: darkMode ? '#555555' : '#CBD5E1',
                        },
                        '&.Mui-focused fieldset': {
                          borderColor: darkMode ? '#90caf9' : '#0288d1',
                        },
                      },
                      '& .MuiInputLabel-root': {
                        color: darkMode ? '#aaaaaa' : '#64748B',
                        '&.Mui-focused': {
                          color: darkMode ? '#90caf9' : '#0288d1',
                        },
                      },
                      '& input': {
                        color: darkMode ? '#f5f5f5' : 'inherit',
                      },
                    }}
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
              <Box sx={{ p: 3, overflow: 'auto', height: '100%' }}>
                <Typography variant="h6" sx={{ 
                  mb: 2, 
                  fontWeight: 500,
                  color: darkMode ? '#f5f5f5' : 'inherit'
                }}>
                  Style Settings
                </Typography>
                
                <Box sx={{ mb: 4 }}>
                  <Typography variant="subtitle1" sx={{ 
                    mb: 1,
                    color: darkMode ? '#f5f5f5' : 'inherit'
                  }}>
                    Colors
                  </Typography>
                  
                  <Grid container spacing={2}>
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Text Color"
                        value={certificate.textColor || '#000000'}
                        onChange={(e) => handleColorChange('textColor', e.target.value)}
                        sx={{
                          '& .MuiOutlinedInput-root': {
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            '& fieldset': {
                              borderColor: darkMode ? '#333333' : '#E2E8F0',
                            },
                            '&:hover fieldset': {
                              borderColor: darkMode ? '#555555' : '#CBD5E1',
                            },
                            '&.Mui-focused fieldset': {
                              borderColor: darkMode ? '#90caf9' : '#0288d1',
                            },
                          },
                          '& .MuiInputLabel-root': {
                            color: darkMode ? '#aaaaaa' : '#64748B',
                            '&.Mui-focused': {
                              color: darkMode ? '#90caf9' : '#0288d1',
                            },
                          },
                          '& input': {
                            color: darkMode ? '#f5f5f5' : 'inherit',
                          },
                        }}
                        InputProps={{
                          startAdornment: (
                            <Box
                              sx={{
                                width: 20,
                                height: 20,
                                mr: 1,
                                bgcolor: certificate.textColor || '#000000',
                                border: '1px solid',
                                borderColor: darkMode ? '#555555' : '#ddd',
                                borderRadius: '4px'
                              }}
                            />
                          )
                        }}
                      />
                    </Grid>
                    
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Header Color"
                        value={certificate.headerColor || '#000000'}
                        onChange={(e) => handleColorChange('headerColor', e.target.value)}
                        sx={{
                          '& .MuiOutlinedInput-root': {
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            '& fieldset': {
                              borderColor: darkMode ? '#333333' : '#E2E8F0',
                            },
                            '&:hover fieldset': {
                              borderColor: darkMode ? '#555555' : '#CBD5E1',
                            },
                            '&.Mui-focused fieldset': {
                              borderColor: darkMode ? '#90caf9' : '#0288d1',
                            },
                          },
                          '& .MuiInputLabel-root': {
                            color: darkMode ? '#aaaaaa' : '#64748B',
                            '&.Mui-focused': {
                              color: darkMode ? '#90caf9' : '#0288d1',
                            },
                          },
                          '& input': {
                            color: darkMode ? '#f5f5f5' : 'inherit',
                          },
                        }}
                        InputProps={{
                          startAdornment: (
                            <Box
                              sx={{
                                width: 20,
                                height: 20,
                                mr: 1,
                                bgcolor: certificate.headerColor || '#000000',
                                border: '1px solid',
                                borderColor: darkMode ? '#555555' : '#ddd',
                                borderRadius: '4px'
                              }}
                            />
                          )
                        }}
                      />
                    </Grid>
                    
                    <Grid item xs={6}>
                      <TextField
                        fullWidth
                        size="small"
                        label="Background Color"
                        value={certificate.backgroundColor || '#ffffff'}
                        onChange={(e) => handleColorChange('backgroundColor', e.target.value)}
                        sx={{
                          '& .MuiOutlinedInput-root': {
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            '& fieldset': {
                              borderColor: darkMode ? '#333333' : '#E2E8F0',
                            },
                            '&:hover fieldset': {
                              borderColor: darkMode ? '#555555' : '#CBD5E1',
                            },
                            '&.Mui-focused fieldset': {
                              borderColor: darkMode ? '#90caf9' : '#0288d1',
                            },
                          },
                          '& .MuiInputLabel-root': {
                            color: darkMode ? '#aaaaaa' : '#64748B',
                            '&.Mui-focused': {
                              color: darkMode ? '#90caf9' : '#0288d1',
                            },
                          },
                          '& input': {
                            color: darkMode ? '#f5f5f5' : 'inherit',
                          },
                        }}
                        InputProps={{
                          startAdornment: (
                            <Box
                              sx={{
                                width: 20,
                                height: 20,
                                mr: 1,
                                bgcolor: certificate.backgroundColor || '#ffffff',
                                border: '1px solid',
                                borderColor: darkMode ? '#555555' : '#ddd',
                                borderRadius: '4px'
                              }}
                            />
                          )
                        }}
                      />
                    </Grid>
                  </Grid>
                </Box>

                {/* Font Settings */}
                <Box sx={{ mb: 4 }}>
                  <Typography variant="subtitle1" sx={{ 
                    mb: 1,
                    color: darkMode ? '#f5f5f5' : 'inherit'
                  }}>
                    Typography
                  </Typography>
                  
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <FormControl fullWidth size="small">
                        <InputLabel sx={{
                          color: darkMode ? '#aaaaaa' : '#64748B',
                          '&.Mui-focused': {
                            color: darkMode ? '#90caf9' : '#0288d1',
                          },
                        }}>
                          Font Family
                        </InputLabel>
                        <Select
                          value={certificate.fontFamily || 'Times New Roman'}
                          onChange={(e) => handleTextChange('fontFamily', e.target.value)}
                          label="Font Family"
                          sx={{
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            color: darkMode ? '#f5f5f5' : 'inherit',
                            '& .MuiOutlinedInput-notchedOutline': {
                              borderColor: darkMode ? '#333333' : '#E2E8F0',
                            },
                            '&:hover .MuiOutlinedInput-notchedOutline': {
                              borderColor: darkMode ? '#555555' : '#CBD5E1',
                            },
                            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                              borderColor: darkMode ? '#90caf9' : '#0288d1',
                            },
                            '& .MuiSvgIcon-root': {
                              color: darkMode ? '#aaaaaa' : 'inherit',
                            },
                          }}
                        >
                          {fontFamilies.map((font) => (
                            <MenuItem 
                              key={font} 
                              value={font} 
                              sx={{ 
                                fontFamily: font,
                                bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                                color: darkMode ? '#f5f5f5' : 'inherit',
                                '&:hover': {
                                  bgcolor: darkMode ? '#333333' : '#F8FAFC',
                                },
                                '&.Mui-selected': {
                                  bgcolor: darkMode ? '#333333' : '#F8FAFC',
                                  '&:hover': {
                                    bgcolor: darkMode ? '#404040' : '#E2E8F0',
                                  },
                                },
                              }}
                            >
                              {font}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Grid>
                  </Grid>
                </Box>
                
                {/* Border Settings */}
                <Box sx={{ mb: 4 }}>
                  <Typography variant="subtitle1" sx={{ 
                    mb: 1,
                    color: darkMode ? '#f5f5f5' : 'inherit'
                  }}>
                    Border & Frame
                  </Typography>
                  
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <FormControlLabel
                        control={
                          <Switch
                            checked={certificate.showBorder || false}
                            onChange={(e) => handleTextChange('showBorder', e.target.checked)}
                            sx={{
                              '& .MuiSwitch-switchBase': {
                                color: darkMode ? '#aaaaaa' : '#CBD5E1',
                                '&.Mui-checked': {
                                  color: darkMode ? '#90caf9' : '#0288d1',
                                },
                                '&.Mui-checked + .MuiSwitch-track': {
                                  bgcolor: darkMode ? 'rgba(144, 202, 249, 0.5)' : 'rgba(2, 136, 209, 0.5)',
                                },
                              },
                              '& .MuiSwitch-track': {
                                bgcolor: darkMode ? '#333333' : '#E2E8F0',
                              },
                            }}
                          />
                        }
                        label={
                          <Typography sx={{ color: darkMode ? '#f5f5f5' : 'inherit' }}>
                            Show Border
                          </Typography>
                        }
                      />
                    </Grid>
                  </Grid>
                </Box>
              </Box>
            )}

            {activeTab === 2 && (
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom sx={{ 
                    color: darkMode ? '#f5f5f5' : 'inherit'
                  }}>
                    Background Image
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Button
                      variant="outlined"
                      component="label"
                      startIcon={<Upload />}
                      sx={{
                        borderColor: darkMode ? '#555555' : '#E2E8F0',
                        color: darkMode ? '#90caf9' : '#0288d1',
                        '&:hover': {
                          borderColor: darkMode ? '#90caf9' : '#0288d1',
                          bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'rgba(2, 136, 209, 0.04)'
                        }
                      }}
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
                        <IconButton 
                          onClick={() => handleImageDelete('background')}
                          sx={{
                            color: darkMode ? '#ef5350' : '#ef5350',
                            '&:hover': {
                              bgcolor: darkMode ? 'rgba(239, 83, 80, 0.08)' : 'rgba(239, 83, 80, 0.04)'
                            }
                          }}
                        >
                          <Delete />
                        </IconButton>
                        <Typography variant="body2" sx={{ 
                          color: darkMode ? '#aaaaaa' : '#64748B'
                        }}>
                          Opacity:
                        </Typography>
                        <Slider
                          value={certificate.backgroundImageOpacity}
                          onChange={(e, value) => handleTextChange('backgroundImageOpacity', value)}
                          min={0}
                          max={1}
                          step={0.1}
                          sx={{
                            width: 100,
                            color: darkMode ? '#90caf9' : '#0288d1',
                            '& .MuiSlider-rail': {
                              bgcolor: darkMode ? '#333333' : '#E2E8F0',
                            },
                            '& .MuiSlider-track': {
                              bgcolor: darkMode ? '#90caf9' : '#0288d1',
                            },
                            '& .MuiSlider-thumb': {
                              bgcolor: darkMode ? '#90caf9' : '#0288d1',
                            },
                          }}
                        />
                      </>
                    )}
                  </Box>
                </Grid>

                <Grid item xs={12}>
                  <Typography variant="subtitle2" gutterBottom sx={{ 
                    color: darkMode ? '#f5f5f5' : 'inherit'
                  }}>
                    Logo
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Button
                      variant="outlined"
                      component="label"
                      startIcon={<Upload />}
                      sx={{
                        borderColor: darkMode ? '#555555' : '#E2E8F0',
                        color: darkMode ? '#90caf9' : '#0288d1',
                        '&:hover': {
                          borderColor: darkMode ? '#90caf9' : '#0288d1',
                          bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'rgba(2, 136, 209, 0.04)'
                        }
                      }}
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
                        <IconButton 
                          onClick={() => handleImageDelete('logo')}
                          sx={{
                            color: darkMode ? '#ef5350' : '#ef5350',
                            '&:hover': {
                              bgcolor: darkMode ? 'rgba(239, 83, 80, 0.08)' : 'rgba(239, 83, 80, 0.04)'
                            }
                          }}
                        >
                          <Delete />
                        </IconButton>
                        <FormControl size="small" sx={{ minWidth: 120 }}>
                          <Select
                            value={certificate.logoPosition}
                            onChange={(e) => handleTextChange('logoPosition', e.target.value)}
                            sx={{
                              bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                              color: darkMode ? '#f5f5f5' : 'inherit',
                              '& .MuiOutlinedInput-notchedOutline': {
                                borderColor: darkMode ? '#333333' : '#E2E8F0',
                              },
                              '&:hover .MuiOutlinedInput-notchedOutline': {
                                borderColor: darkMode ? '#555555' : '#CBD5E1',
                              },
                              '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                                borderColor: darkMode ? '#90caf9' : '#0288d1',
                              },
                              '& .MuiSvgIcon-root': {
                                color: darkMode ? '#aaaaaa' : 'inherit',
                              },
                            }}
                          >
                            <MenuItem value="top-left" sx={{
                              bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                              color: darkMode ? '#f5f5f5' : 'inherit',
                              '&:hover': {
                                bgcolor: darkMode ? '#333333' : '#F8FAFC',
                              },
                              '&.Mui-selected': {
                                bgcolor: darkMode ? '#333333' : '#F8FAFC',
                              },
                            }}>Top Left</MenuItem>
                            <MenuItem value="top-center" sx={{
                              bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                              color: darkMode ? '#f5f5f5' : 'inherit',
                              '&:hover': {
                                bgcolor: darkMode ? '#333333' : '#F8FAFC',
                              },
                              '&.Mui-selected': {
                                bgcolor: darkMode ? '#333333' : '#F8FAFC',
                              },
                            }}>Top Center</MenuItem>
                            <MenuItem value="top-right" sx={{
                              bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                              color: darkMode ? '#f5f5f5' : 'inherit',
                              '&:hover': {
                                bgcolor: darkMode ? '#333333' : '#F8FAFC',
                              },
                              '&.Mui-selected': {
                                bgcolor: darkMode ? '#333333' : '#F8FAFC',
                              },
                            }}>Top Right</MenuItem>
                          </Select>
                        </FormControl>
                        <TextField
                          type="number"
                          label="Width"
                          value={certificate.logoWidth}
                          onChange={(e) => handleTextChange('logoWidth', Number(e.target.value))}
                          size="small"
                          sx={{
                            width: 100,
                            '& .MuiOutlinedInput-root': {
                              bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                              '& fieldset': {
                                borderColor: darkMode ? '#333333' : '#E2E8F0',
                              },
                              '&:hover fieldset': {
                                borderColor: darkMode ? '#555555' : '#CBD5E1',
                              },
                              '&.Mui-focused fieldset': {
                                borderColor: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& .MuiInputLabel-root': {
                              color: darkMode ? '#aaaaaa' : '#64748B',
                              '&.Mui-focused': {
                                color: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& input': {
                              color: darkMode ? '#f5f5f5' : 'inherit',
                            },
                          }}
                        />
                        <TextField
                          type="number"
                          label="Height"
                          value={certificate.logoHeight}
                          onChange={(e) => handleTextChange('logoHeight', Number(e.target.value))}
                          size="small"
                          sx={{
                            width: 100,
                            '& .MuiOutlinedInput-root': {
                              bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                              '& fieldset': {
                                borderColor: darkMode ? '#333333' : '#E2E8F0',
                              },
                              '&:hover fieldset': {
                                borderColor: darkMode ? '#555555' : '#CBD5E1',
                              },
                              '&.Mui-focused fieldset': {
                                borderColor: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& .MuiInputLabel-root': {
                              color: darkMode ? '#aaaaaa' : '#64748B',
                              '&.Mui-focused': {
                                color: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& input': {
                              color: darkMode ? '#f5f5f5' : 'inherit',
                            },
                          }}
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
                  <Typography variant="subtitle2" gutterBottom sx={{ 
                    color: darkMode ? '#f5f5f5' : 'inherit'
                  }}>
                    QR Code Settings
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={certificate.showQRCode}
                          onChange={(e) => handleTextChange('showQRCode', e.target.checked)}
                          sx={{
                            '& .MuiSwitch-switchBase': {
                              color: darkMode ? '#aaaaaa' : '#CBD5E1',
                              '&.Mui-checked': {
                                color: darkMode ? '#90caf9' : '#0288d1',
                              },
                              '&.Mui-checked + .MuiSwitch-track': {
                                bgcolor: darkMode ? 'rgba(144, 202, 249, 0.5)' : 'rgba(2, 136, 209, 0.5)',
                              },
                            },
                            '& .MuiSwitch-track': {
                              bgcolor: darkMode ? '#333333' : '#E2E8F0',
                            },
                          }}
                        />
                      }
                      label={
                        <Typography sx={{ color: darkMode ? '#f5f5f5' : 'inherit' }}>
                          Show QR Code
                        </Typography>
                      }
                    />
                    {certificate.showQRCode && (
                      <FormControl size="small" sx={{ minWidth: 120 }}>
                        <Select
                          value={certificate.qrCodePosition}
                          onChange={(e) => handleTextChange('qrCodePosition', e.target.value)}
                          sx={{
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            color: darkMode ? '#f5f5f5' : 'inherit',
                            '& .MuiOutlinedInput-notchedOutline': {
                              borderColor: darkMode ? '#333333' : '#E2E8F0',
                            },
                            '&:hover .MuiOutlinedInput-notchedOutline': {
                              borderColor: darkMode ? '#555555' : '#CBD5E1',
                            },
                            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                              borderColor: darkMode ? '#90caf9' : '#0288d1',
                            },
                            '& .MuiSvgIcon-root': {
                              color: darkMode ? '#aaaaaa' : 'inherit',
                            },
                          }}
                        >
                          <MenuItem value="bottom-right" sx={{
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            color: darkMode ? '#f5f5f5' : 'inherit',
                            '&:hover': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                            '&.Mui-selected': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                          }}>Bottom Right</MenuItem>
                          <MenuItem value="bottom-left" sx={{
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            color: darkMode ? '#f5f5f5' : 'inherit',
                            '&:hover': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                            '&.Mui-selected': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                          }}>Bottom Left</MenuItem>
                          <MenuItem value="top-right" sx={{
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            color: darkMode ? '#f5f5f5' : 'inherit',
                            '&:hover': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                            '&.Mui-selected': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                          }}>Top Right</MenuItem>
                          <MenuItem value="top-left" sx={{
                            bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                            color: darkMode ? '#f5f5f5' : 'inherit',
                            '&:hover': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                            '&.Mui-selected': {
                              bgcolor: darkMode ? '#333333' : '#F8FAFC',
                            },
                          }}>Top Left</MenuItem>
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
                  <Typography variant="subtitle1" sx={{ color: darkMode ? '#f5f5f5' : 'inherit' }}>
                    {certificate.signatories.length} of 3 signatories
                  </Typography>
                  <Button 
                    size="small" 
                    startIcon={<Add />} 
                    onClick={handleAddSignatory}
                    disabled={certificate.signatories.length >= 3}
                    variant="outlined"
                    sx={{
                      borderColor: darkMode ? '#555555' : '#E2E8F0',
                      color: darkMode ? '#90caf9' : '#0288d1',
                      '&:hover': {
                        borderColor: darkMode ? '#90caf9' : '#0288d1',
                        bgcolor: darkMode ? 'rgba(144, 202, 249, 0.08)' : 'rgba(2, 136, 209, 0.04)'
                      },
                      '&.Mui-disabled': {
                        borderColor: darkMode ? '#333333' : '#E2E8F0',
                        color: darkMode ? '#555555' : '#CBD5E1'
                      }
                    }}
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
                      borderRadius: '8px',
                      bgcolor: darkMode ? '#1e1e1e' : '#ffffff',
                      border: '1px solid',
                      borderColor: darkMode ? '#333333' : '#E2E8F0'
                    }}
                  >
                    <Box sx={{ 
                      display: 'flex', 
                      justifyContent: 'space-between', 
                      alignItems: 'center',
                      mb: 2
                    }}>
                      <Typography variant="subtitle2" sx={{ color: darkMode ? '#f5f5f5' : 'inherit' }}>
                        Signatory {index + 1}
                      </Typography>
                      <IconButton 
                        size="small" 
                        onClick={() => handleRemoveSignatory(index)}
                        disabled={certificate.signatories.length <= 1}
                        sx={{
                          color: darkMode ? '#ef5350' : '#ef5350',
                          '&:hover': {
                            bgcolor: darkMode ? 'rgba(239, 83, 80, 0.08)' : 'rgba(239, 83, 80, 0.04)'
                          },
                          '&.Mui-disabled': {
                            color: darkMode ? '#555555' : '#CBD5E1'
                          }
                        }}
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
                          sx={{
                            '& .MuiOutlinedInput-root': {
                              bgcolor: darkMode ? '#2d2d2d' : '#ffffff',
                              '& fieldset': {
                                borderColor: darkMode ? '#333333' : '#E2E8F0',
                              },
                              '&:hover fieldset': {
                                borderColor: darkMode ? '#555555' : '#CBD5E1',
                              },
                              '&.Mui-focused fieldset': {
                                borderColor: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& .MuiInputLabel-root': {
                              color: darkMode ? '#aaaaaa' : '#64748B',
                              '&.Mui-focused': {
                                color: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& input': {
                              color: darkMode ? '#f5f5f5' : 'inherit',
                            },
                          }}
                        />
                      </Grid>
                      <Grid item xs={12} md={6}>
                        <TextField
                          fullWidth
                          label="Title"
                          value={signatory.title}
                          onChange={(e) => handleSignatoryChange(index, 'title', e.target.value)}
                          size="small"
                          sx={{
                            '& .MuiOutlinedInput-root': {
                              bgcolor: darkMode ? '#2d2d2d' : '#ffffff',
                              '& fieldset': {
                                borderColor: darkMode ? '#333333' : '#E2E8F0',
                              },
                              '&:hover fieldset': {
                                borderColor: darkMode ? '#555555' : '#CBD5E1',
                              },
                              '&.Mui-focused fieldset': {
                                borderColor: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& .MuiInputLabel-root': {
                              color: darkMode ? '#aaaaaa' : '#64748B',
                              '&.Mui-focused': {
                                color: darkMode ? '#90caf9' : '#0288d1',
                              },
                            },
                            '& input': {
                              color: darkMode ? '#f5f5f5' : 'inherit',
                            },
                          }}
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