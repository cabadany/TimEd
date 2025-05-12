import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function QRJoin() {
  const { eventId } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    const userId = localStorage.getItem("userId"); // or get from auth context
    console.log("User ID:", userId);

    axios.post(`http://localhost:8080/api/attendance/${eventId}/${userId}`)
      .then(() => {
        alert('✅ Successfully timed in!');
        console.log(eventId);
      })
      .catch(() => {
        alert('❌ Failed to time in. Try again later.');
      });
  }, [eventId, navigate]);

  return (
    <div style={styles.container}>
      <p style={styles.text}>Joining event… please wait.</p>
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
  }
};
