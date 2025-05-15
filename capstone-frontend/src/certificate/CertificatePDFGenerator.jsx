// CertificatePDFGenerator.jsx
import React, { useRef } from 'react';
import { useReactToPrint } from 'react-to-print';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import { Box, Button } from '@mui/material';
import { Download, Email } from '@mui/icons-material';

// This component renders your certificate and provides PDF download/email functionality
const CertificatePDFGenerator = ({ certificate, attendee}) => {
  const certificateRef = useRef(null);
  
  // Function to handle direct printing/PDF creation using react-to-print
  const handlePrint = useReactToPrint({
    content: () => certificateRef.current,
    documentTitle: `Certificate_${attendee?.firstName || 'Recipient'}_${attendee?.lastName || ''}`,
  });
  
  // Function to generate PDF using html2canvas and jsPDF
  const generatePDF = async () => {
    const canvas = await html2canvas(certificateRef.current, {
      scale: 2, // Higher scale for better quality
      useCORS: true,
      logging: false,
      backgroundColor: certificate.backgroundColor || '#ffffff'
    });
    
    const imgData = canvas.toDataURL('image/png');
    
    // A4 size: 210 x 297 mm
    const pdf = new jsPDF({
      orientation: 'landscape',
      unit: 'mm',
      format: 'a4'
    });
    
    // Calculate the width and height of the PDF
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = pdf.internal.pageSize.getHeight();
    
    // Calculate image width and height to maintain aspect ratio
    const imgWidth = canvas.width;
    const imgHeight = canvas.height;
    const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight);
    const imgX = (pdfWidth - imgWidth * ratio) / 2;
    const imgY = (pdfHeight - imgHeight * ratio) / 2;
    
    pdf.addImage(imgData, 'PNG', imgX, imgY, imgWidth * ratio, imgHeight * ratio);
    
    // Save the PDF
    pdf.save(`Certificate_${attendee?.firstName || 'Recipient'}_${attendee?.lastName || ''}.pdf`);
  };
  
  // Function to handle email sending
  const handleSendEmail = async () => {
    try {
      // Convert certificate to PDF binary data
      const canvas = await html2canvas(certificateRef.current, {
        scale: 2,
        useCORS: true,
        logging: false,
        backgroundColor: certificate.backgroundColor || '#ffffff'
      });
      
      const imgData = canvas.toDataURL('image/png');
      
      const pdf = new jsPDF({
        orientation: 'landscape',
        unit: 'mm',
        format: 'a4'
      });
      
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = pdf.internal.pageSize.getHeight();
      
      const imgWidth = canvas.width;
      const imgHeight = canvas.height;
      const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight);
      const imgX = (pdfWidth - imgWidth * ratio) / 2;
      const imgY = (pdfHeight - imgHeight * ratio) / 2;
      
      pdf.addImage(imgData, 'PNG', imgX, imgY, imgWidth * ratio, imgHeight * ratio);
      
      // Convert PDF to base64 string
      const pdfData = pdf.output('datauristring');
      
      // Send to your backend API
      const response = await fetch('http://localhost:8080/api/certificates/sendEmail', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: attendee.email,
          name: `${attendee.firstName} ${attendee.lastName}`,
          eventName: certificate.eventName,
          certificateData: pdfData,
          certificateId: certificate.id
        }),
      });
      
      if (response.ok) {
        alert('Certificate sent successfully to recipient email!');
      } else {
        throw new Error('Failed to send email');
      }
    } catch (error) {
      console.error('Error sending email:', error);
      alert('Failed to send certificate via email. Please try again.');
    }
  };

  // Render the certificate using your existing design
  const renderCertificate = () => {
    // Personalize certificate with attendee data if available
    const personalizedCertificate = {
      ...certificate,
      recipientName: attendee ? `${attendee.firstName} ${attendee.lastName}` : certificate.recipientName
    };
    
    return (
      <Box 
        sx={{
          width: '800px', // Fixed width for PDF generation
          height: '600px', // Fixed height for PDF generation
          position: 'relative',
          overflow: 'hidden',
          backgroundColor: personalizedCertificate.backgroundColor || '#ffffff',
          borderRadius: '16px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '40px',
          border: `12px solid ${personalizedCertificate.borderColor || '#0047AB'}`,
          color: personalizedCertificate.textColor || '#000000',
          fontFamily: personalizedCertificate.fontFamily || 'Arial'
        }}
      >
        {/* Header decoration */}
        {personalizedCertificate.headerColor && (
          <Box 
            sx={{ 
              position: 'absolute', 
              top: -100, 
              left: -100, 
              width: 400, 
              height: 200, 
              backgroundColor: personalizedCertificate.headerColor,
              borderRadius: '50%',
              transform: 'rotate(-45deg)',
              zIndex: 0
            }} 
          />
        )}
        
        {/* Footer decoration */}
        {personalizedCertificate.headerColor && (
          <Box 
            sx={{ 
              position: 'absolute', 
              bottom: -100, 
              right: -100, 
              width: 400, 
              height: 200, 
              backgroundColor: personalizedCertificate.headerColor,
              borderRadius: '50%',
              transform: 'rotate(135deg)',
              zIndex: 0
            }} 
          />
        )}
        
        <Box sx={{ zIndex: 1, textAlign: 'center', width: '100%' }}>
          {/* Certificate Title */}
          <Box sx={{ mb: 3 }}>
            <Box 
              component="h1" 
              sx={{ 
                fontWeight: 'bold', 
                fontSize: '42px',
                letterSpacing: '2px',
                lineHeight: 1.2,
                mb: 1,
                textTransform: 'uppercase',
                margin: 0
              }}
            >
              {personalizedCertificate.title || 'CERTIFICATE'}
            </Box>
            
            <Box 
              component="h2" 
              sx={{ 
                fontWeight: 'bold', 
                fontSize: '24px',
                letterSpacing: '2px',
                mb: 0,
                textTransform: 'uppercase',
                margin: 0
              }}
            >
              {personalizedCertificate.subtitle || 'OF ACHIEVEMENT'}
            </Box>
          </Box>
          
          {/* Recipient section */}
          <Box sx={{ mb: 4 }}>
            <Box 
              component="p" 
              sx={{ 
                fontSize: '14px',
                textTransform: 'uppercase',
                mb: 1,
                fontWeight: 500,
                margin: 0
              }}
            >
              {personalizedCertificate.recipientText || 'PRESENTED TO'}
            </Box>
            
            <Box 
              component="h3" 
              sx={{ 
                fontStyle: 'italic',
                fontWeight: 'bold',
                fontSize: '32px',
                mb: 0,
                borderBottom: `2px solid ${personalizedCertificate.textColor || '#000000'}`,
                display: 'inline-block',
                padding: '0 20px 5px',
                margin: 0
              }}
            >
              {personalizedCertificate.recipientName}
            </Box>
          </Box>
          
          {/* Description */}
          <Box 
            component="p" 
            sx={{ 
              fontSize: '16px',
              mb: 4,
              maxWidth: '80%',
              margin: '0 auto 2rem',
              lineHeight: 1.5
            }}
          >
            {personalizedCertificate.description || 'For outstanding participation in the event and demonstrating exceptional dedication throughout the program.'}
          </Box>
          
          {/* Event name and date */}
          <Box sx={{ mb: 4 }}>
            <Box 
              component="p" 
              sx={{ 
                fontWeight: 'bold',
                fontSize: '18px',
                margin: 0
              }}
            >
              {personalizedCertificate.eventName}
            </Box>
            
            <Box 
              component="p" 
              sx={{ 
                fontSize: '14px',
                margin: 0
              }}
            >
              Certificate #{personalizedCertificate.certificateNumber || 'CERT-001'}
            </Box>
          </Box>
          
          {/* Signatories */}
          <Box 
            sx={{ 
              display: 'flex', 
              justifyContent: 'space-around', 
              width: '100%', 
              mt: 4 
            }}
          >
            {(personalizedCertificate.signatories || []).map((signatory, index) => (
              <Box key={index} sx={{ textAlign: 'center', width: '200px' }}>
                <Box 
                  sx={{ 
                    borderTop: `2px solid ${personalizedCertificate.textColor || '#000000'}`,
                    width: '100%',
                    mb: 1,
                    pt: 1
                  }}
                />
                <Box component="p" sx={{ fontSize: '16px', fontWeight: 'bold', margin: 0 }}>
                  {signatory.name}
                </Box>
                <Box component="p" sx={{ fontSize: '14px', margin: 0 }}>
                  {signatory.title}
                </Box>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>
    );
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
      {/* Certificate preview */}
      <Box sx={{ width: '100%', overflow: 'auto', mb: 2 }}>
        <Box 
          ref={certificateRef} 
          sx={{ 
            margin: '0 auto',
            boxShadow: '0 4px 20px rgba(0,0,0,0.15)'
          }}
        >
          {renderCertificate()}
        </Box>
      </Box>
      
      {/* Action buttons */}
      <Box sx={{ display: 'flex', gap: 2 }}>
        <Button
          variant="contained"
          color="primary"
          onClick={generatePDF}
          startIcon={<Download />}
        >
          Download PDF
        </Button>
        
        {attendee?.email && (
          <Button
            variant="contained"
            color="secondary"
            onClick={handleSendEmail}
            startIcon={<Email />}
          >
            Send via Email
          </Button>
        )}
      </Box>
    </Box>
  );
};

export default CertificatePDFGenerator;