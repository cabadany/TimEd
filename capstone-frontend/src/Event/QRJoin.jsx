import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function QRJoin() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const handleTimeIn = async () => {
      // If already processing or not mounted, don't proceed
      if (!isMounted || isProcessing) return;

      setIsProcessing(true);
      const userId = localStorage.getItem("userId");
      console.log("User ID:", userId);

      try {
        // Time in the user - certificate will be sent automatically by the backend
        const timeInResponse = await axios.post(`http://localhost:8080/api/attendance/${eventId}/${userId}`);
        console.log('Time in response:', timeInResponse.data);
        
        if (isMounted) {
          alert('✅ Successfully timed in! A certificate will be sent to your email.');
          navigate('/dashboard');
        }
      } catch (error) {
        console.error('Error details:', error.response?.data || error.message);
        if (isMounted) {
          setError(error.response?.data || error.message);
          alert(`❌ Failed to time in: ${error.response?.data || error.message}`);
        }
      } finally {
        if (isMounted) {
          setIsProcessing(false);
        }
      }
    };

    handleTimeIn();

    // Cleanup function to prevent memory leaks and multiple executions
    return () => {
      isMounted = false;
    };
  }, []); // Remove eventId and navigate from dependencies

  // Separate function for navigation
  const handleNavigateToDashboard = () => {
    navigate('/dashboard');
  };

  return (
    <div style={styles.container}>
      {error ? (
        <div style={styles.errorContainer}>
          <p style={styles.errorText}>Error: {error}</p>
          <button 
            onClick={handleNavigateToDashboard}
            style={styles.button}
          >
            Return to Dashboard
          </button>
        </div>
      ) : (
        <p style={styles.text}>Joining event… please wait.</p>
      )}
    </div>
  );
}

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh', // Full viewport height
    backgroundColor: '#f0f0f0', // Light grey background
    color: '#333', // Dark text color
    fontFamily: 'Arial, sans-serif',
  },
  text: {
    fontSize: '20px',
    fontWeight: 'bold',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '20px',
  },
  errorText: {
    color: '#dc2626',
    fontSize: '16px',
    textAlign: 'center',
    maxWidth: '80%',
  },
  button: {
    padding: '10px 20px',
    backgroundColor: '#0284c7',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 'bold',
  }
};
