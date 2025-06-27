// Simple event emitter for cross-component communication
class EventEmitter {
  constructor() {
    this.events = {};
  }

  on(event, callback) {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(callback);
  }

  off(event, callback) {
    if (!this.events[event]) return;
    
    this.events[event] = this.events[event].filter(
      existingCallback => existingCallback !== callback
    );
  }

  emit(event, data) {
    if (!this.events[event]) return;
    
    this.events[event].forEach(callback => {
      try {
        callback(data);
      } catch (error) {
        console.error('Error in event callback:', error);
      }
    });
  }
}

// Create a singleton instance
const eventEmitter = new EventEmitter();

// Define event constants
export const EVENTS = {
  ACCOUNT_APPROVED: 'account_approved',
  ACCOUNT_REJECTED: 'account_rejected',
  USER_CREATED: 'user_created',
  USER_UPDATED: 'user_updated',
  USER_DELETED: 'user_deleted',
  REFRESH_ACCOUNTS: 'refresh_accounts',
  REFRESH_ACCOUNT_REQUESTS: 'refresh_account_requests'
};

export default eventEmitter; 