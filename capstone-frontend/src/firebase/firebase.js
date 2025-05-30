// Firebase configuration
import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getStorage } from 'firebase/storage';
import { getDatabase } from 'firebase/database';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyANuuJPD7wg8SisClDlm7OQ1tZghvmg80E",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "timed-system.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "timed-system",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "timed-system.firebasestorage.app",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "678688322328",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:678688322328:web:a2f289bcfa58a27eb4a46d",
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL || "https://timed-system-default-rtdb.firebaseio.com",
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const storage = getStorage(app);
const database = getDatabase(app);
const firestore = getFirestore(app);

export { app, auth, storage, database, firestore }; 