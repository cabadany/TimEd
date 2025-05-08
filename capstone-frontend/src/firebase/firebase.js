// Firebase configuration
import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
  apiKey: "AIzaSyANuuJPD7wg8SisClDlm7OQ1tZghvmg80E", // From google-services.json
  authDomain: "timed-system.firebaseapp.com",
  projectId: "timed-system", // From FirebaseConfig.java
  storageBucket: "timed-system.firebasestorage.app", // From google-services.json
  messagingSenderId: "678688322328", // From google-services.json project_number
  appId: "1:678688322328:web:a2f289bcfa58a27eb4a46d", // From google-services.json
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const storage = getStorage(app);

export { app, auth, storage }; 