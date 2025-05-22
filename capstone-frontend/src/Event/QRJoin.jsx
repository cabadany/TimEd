import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';

export default function QRJoin() {
  const { eventId } = useParams();
  const [error, setError] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const handleTimeIn = async () => {
      if (!isMounted || isProcessing) return;

      setIsProcessing(true);
      const userId = localStorage.getItem("userId");
      console.log("User ID:", userId);

      try {
        const timeInResponse = await axios.post(`http://localhost:8080/api/attendance/${eventId}/${userId}`);
        console.log('Time in response:', timeInResponse.data);

        if (isMounted) {
          if (timeInResponse.data.includes("Already timed in")) {
            setError("You have already timed in for this event and received a certificate.");
            alert('You have already timed in for this event and received a certificate.');
          } else {
            alert('Successfully timed in! A certificate will be sent to your email.');
          }

          // Try to close window
          window.close();

          // If close doesn't work, show fallback
          setTimeout(() => {
            if (!window.closed) {
              alert('Browser prevented automatic tab close. Please close this tab manually. Thank you!');
            }
          }, 500);
        }
      } catch (error) {
        console.error('Error details:', error.response?.data || error.message);
        if (isMounted) {
          setError(error.response?.data || error.message);
          alert(`Failed to time in: ${error.response?.data || error.message}`);
        }
      } finally {
        if (isMounted) {
          setIsProcessing(false);
        }
      }
    };

    handleTimeIn();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleCloseWindow = () => {
    window.close();
    setTimeout(() => {
      if (!window.closed) {
        alert('You can now close this Tab manually. Thank you!');
      }
    }, 500);
  };

  return (
    <div style={styles.container}>
      {error ? (
        <div style={styles.errorContainer}>
          <p style={styles.errorText}>{error}</p>
          <button
            onClick={handleCloseWindow}
            style={styles.button}
          >
            Close Window
          </button>
        </div>
      ) : (
        <p style={styles.text}>Joining event… please wait.</p>
      )}
    </div>
  );
}

const styles = {
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
  },
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
    backgroundColor: '#f0f0f0',
    color: '#333',
    fontFamily: 'Arial, sans-serif',
  },
  text: {
    fontSize: '20px',
    fontWeight: 'bold',
  }
};
