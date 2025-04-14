import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './Dashboard/Dashboard';
import Event from './Event/Event';
<<<<<<< Updated upstream
import LoginPage from './Auth/LoginPage';
import Settings from './Settings/Settings';
=======
import Setting from './Setting/Setting';
>>>>>>> Stashed changes
import './App.css';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/event" element={<Event />} />
<<<<<<< Updated upstream
        <Route path="/settings" element={<Settings />} />
=======
        <Route path="/settings" element={<Setting />} />
>>>>>>> Stashed changes
      </Routes>
    </Router>
  );
}

export default App;