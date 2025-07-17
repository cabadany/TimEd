// API Configuration
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://timed-utd9.onrender.com/api';

// Helper function to construct API URLs
export const getApiUrl = (endpoint) => {
  return `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
};

// Common API endpoints
export const API_ENDPOINTS = {
  // Auth
  LOGIN_BY_SCHOOL_ID: '/auth/login-by-schoolId',
  EMAIL_BY_SCHOOL_ID: '/auth/email-by-schoolId',
  REGISTER: '/auth/register',
  
  // Users
  GET_ALL_USERS: '/user/getAll',
  GET_USER: (userId) => `/user/getUser/${userId}`,
  GET_USER_BY_SCHOOL_ID: (schoolId) => `/user/getBySchoolId/${schoolId}`,
  UPDATE_USER: (userId) => `/user/updateUser/${userId}`,
  UPDATE_PROFILE_PICTURE: (userId) => `/user/updateProfilePicture/${userId}`,
  DELETE_USER: (userId) => `/user/deleteUser/${userId}`,
  
  // Events
  GET_EVENTS_PAGINATED: '/events/getPaginated',
  GET_ALL_EVENTS: '/events/getAll',
  GET_EVENTS_BY_DATE_RANGE: '/events/getByDateRange',
  CREATE_EVENT: '/events/createEvent',
  DELETE_EVENT: (eventId) => `/events/deleteEvent/${eventId}`,
  UPDATE_EVENT: (eventId) => `/events/update/${eventId}`,
  UPDATE_EVENT_STATUS: (eventId) => `/events/updateStatus/${eventId}`,
  EVENT_QR: (eventId) => `/events/qr/${eventId}`,
  
  // Departments
  GET_DEPARTMENTS: '/departments',
  GET_DEPARTMENT: (deptId) => `/departments/${deptId}`,
  CREATE_DEPARTMENT: '/departments',
  UPDATE_DEPARTMENT: (deptId) => `/departments/${deptId}`,
  DELETE_DEPARTMENT: (deptId) => `/departments/${deptId}`,
  
  // Attendance
  ATTENDANCE_TIME_IN: (eventId, userId) => `/attendance/${eventId}/${userId}`,
  ATTENDANCE_MANUAL_TIME_IN: (eventId, userId) => `/attendance/${eventId}/${userId}/manual/timein`,
  ATTENDANCE_MANUAL_TIME_OUT: (eventId, userId) => `/attendance/${eventId}/${userId}/manual/timeout`,
  GET_ATTENDEES: (eventId) => `/attendance/${eventId}/attendees`,
  GET_ATTENDANCE_LOGS: '/attendance/logs',
  GET_USER_ATTENDED_EVENTS: (userId) => `/attendance/user/${userId}/attended-events`,
  
  // Certificates
  GET_CERTIFICATES: '/certificates',
  GET_CERTIFICATE_BY_EVENT: (eventId) => `/certificates/getByEventId/${eventId}`,
  CREATE_CERTIFICATE: '/certificates',
  UPDATE_CERTIFICATE: (certId) => `/certificates/${certId}`,
  DELETE_CERTIFICATE: (certId) => `/certificates/delete/${certId}`,
  LINK_CERTIFICATE_TO_EVENT: '/certificates/linkToEvent',
  CERTIFICATE_IMAGES: (eventId) => `/certificates/${eventId}/images`,
  CREATE_EVENT_CERTIFICATE: (eventId) => `/events/${eventId}/certificate`,
  
  // Email
  SEND_EMAIL: '/email/send',
  
  // Account Requests
  GET_ALL_ACCOUNT_REQUESTS: '/account-requests/all',
  GET_PENDING_ACCOUNT_REQUESTS: '/account-requests/pending',
  GET_ACCOUNT_REQUEST: (requestId) => `/account-requests/${requestId}`,
  REVIEW_ACCOUNT_REQUEST: '/account-requests/review',
  CREATE_ACCOUNT_REQUEST: '/account-requests/create',
  
  // Excuse Letters
  GET_ALL_EXCUSE_LETTERS: '/excuse-letters/getAll',
  GENERATE_OTP: '/auth/generate-otp',
  VERIFY_OTP: '/auth/verify-otp',
}; 