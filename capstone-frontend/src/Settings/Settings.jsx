// src/pages/Settings.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Settings.css';

const Settings = () => {
  return (
    <div className="settings-container">
      <h1>⚙️ Settings</h1>

      <section>
        <h2>👥 User Management</h2>
        <ul>
          <li>Add/edit/remove employees or users</li>
          <li>Assign user roles (e.g., Admin, Employee, Manager)</li>
          <li>Import/export user data (CSV/Excel)</li>
          <li>Reset user passwords</li>
        </ul>
      </section>

      <section>
        <h2>⏰ Work Schedule & Time Rules</h2>
        <ul>
          <li>Set work shifts (e.g., 9AM–6PM, night shifts)</li>
          <li>Define grace periods (e.g., 5-min late tolerance)</li>
          <li>Overtime rules (e.g., OT starts after 8 hrs)</li>
          <li>Break time and lunch settings</li>
          <li>Flexible vs. fixed schedules</li>
        </ul>
      </section>

      <section>
        <h2>🧾 Attendance Rules</h2>
        <ul>
          <li>Late, undertime, and absent thresholds</li>
          <li>Auto/manual approval for time-in/out edits</li>
          <li>Rules for holidays and rest days</li>
          <li>Auto-logout/timeout after a set time</li>
        </ul>
      </section>

      <section>
        <h2>📈 Reports & Logs Configuration</h2>
        <ul>
          <li>Enable/disable specific reports (e.g., Daily, Weekly, Monthly)</li>
          <li>Set export formats (PDF, Excel)</li>
          <li>Log retention settings (e.g., store logs for 6 months)</li>
          <li>IP or device logging (for remote clock-ins)</li>
        </ul>
      </section>

      <section>
        <h2>🔐 System & Security</h2>
        <ul>
          <li>Role-based access control (RBAC)</li>
          <li>Allowed IP addresses or geofencing</li>
          <li>Enable 2FA or biometric requirements</li>
          <li>Device registration (approved logins only)</li>
        </ul>
      </section>

      <section>
        <h2>🔔 Notifications & Approvals</h2>
        <ul>
          <li>Set email or SMS alerts (e.g., late arrivals, absences)</li>
          <li>Customize notification recipients</li>
          <li>Set rules for manual edit or leave approvals</li>
        </ul>
      </section>

      <section>
        <h2>🗓️ Holidays & Leave Settings</h2>
        <ul>
          <li>Add public and custom holidays</li>
          <li>Set leave types (sick, vacation, unpaid)</li>
          <li>Set approvers for leave requests</li>
        </ul>
      </section>

      <section>
        <h2>👨‍🏫 Professor Account Management (Admin Panel)</h2>
        <ul>
          <li>Create New Professor Account
            <ul>
              <li>Full Name</li>
              <li>Employee ID / Faculty Code</li>
              <li>Department</li>
              <li>Email (optional for notifications)</li>
              <li>Username (manually set by admin)</li>
              <li>Password (admin-defined or auto-generated)</li>
            </ul>
          </li>
          <li>Edit Existing Account
            <ul>
              <li>Update name, username, department, password, or role</li>
              <li>Reset Password (generate temporary password)</li>
              <li>Force password change on next login</li>
            </ul>
          </li>
          <li>Deactivate / Archive Account
            <ul>
              <li>For retired/resigned professors</li>
              <li>Option to retain attendance records</li>
            </ul>
          </li>
          <li>Bulk Upload Professors (CSV/Excel)</li>
        </ul>
      </section>
    </div>
  );
};

export default Settings;