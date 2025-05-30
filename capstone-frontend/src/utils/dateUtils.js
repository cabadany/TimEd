// Date utility functions for consistent timezone handling across the application

/**
 * Format date for display in Philippines timezone
 * @param {string|Date} dateInput - Date string or Date object
 * @param {Object} options - Additional locale options
 * @returns {string} Formatted date string
 */
export const formatDatePH = (dateInput, options = {}) => {
  if (!dateInput) return 'N/A';
  
  try {
    const date = new Date(dateInput);
    
    if (isNaN(date.getTime())) {
      return 'Invalid Date';
    }
    
    const defaultOptions = {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      second: '2-digit',
      hour12: true,
      timeZone: 'Asia/Manila'
    };
    
    const mergedOptions = { ...defaultOptions, ...options };
    
    return date.toLocaleString('en-US', mergedOptions);
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Date Error';
  }
};

/**
 * Format date without time for display
 * @param {string|Date} dateInput - Date string or Date object
 * @returns {string} Formatted date string (date only)
 */
export const formatDateOnlyPH = (dateInput) => {
  return formatDatePH(dateInput, {
    hour: undefined,
    minute: undefined,
    second: undefined,
    hour12: undefined
  });
};

/**
 * Format time only for display
 * @param {string|Date} dateInput - Date string or Date object
 * @returns {string} Formatted time string
 */
export const formatTimePH = (dateInput) => {
  return formatDatePH(dateInput, {
    year: undefined,
    month: undefined,
    day: undefined
  });
};

/**
 * Format date for compact display (notifications, etc.)
 * @param {string|Date} dateInput - Date string or Date object
 * @returns {string} Formatted date string (compact)
 */
export const formatDateCompactPH = (dateInput) => {
  return formatDatePH(dateInput, {
    month: 'short',
    second: undefined
  });
};

/**
 * Create a date object preserving local time (for backend submission)
 * @param {string} dateTimeLocal - DateTime local string from input (YYYY-MM-DDTHH:mm)
 * @returns {string} ISO string preserving local time components
 */
export const createLocalDateISO = (dateTimeLocal) => {
  try {
    // Parse the datetime-local string directly to avoid timezone issues
    // Input format: "2025-05-30T20:49" (from datetime-local input)
    
    if (!dateTimeLocal || typeof dateTimeLocal !== 'string') {
      return null;
    }
    
    // Split the date and time parts
    const [datePart, timePart] = dateTimeLocal.split('T');
    
    if (!datePart || !timePart) {
      return null;
    }
    
    // Parse date parts
    const [year, month, day] = datePart.split('-');
    
    // Parse time parts (add seconds if not present)
    const timeWithSeconds = timePart.includes(':') && timePart.split(':').length === 2 
      ? `${timePart}:00` 
      : timePart;
    
    // Return the formatted string directly without Date object conversion
    return `${year}-${month}-${day}T${timeWithSeconds}`;
    
  } catch (error) {
    console.error('Error creating local date ISO:', error);
    return null;
  }
}; 