import React from 'react';
import { ArrowLeft, Search } from 'lucide-react';

const NotFoundPage = () => {
  const handleBackToLogin = () => {
    // Remove authentication token and user role from localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');
    console.log('Logging out...');
    
    // Navigate to login page - replace with your routing logic
    window.location.href = '/login';
  };

  return (
    <>
      <style>
        {`
          @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
          
          .not-found-container {
            font-family: 'Inter', sans-serif;
            min-height: 100vh;
            background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            position: relative;
            overflow: hidden;
          }
          
          .background-graphics {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
          }
          
          .floating-shape {
            position: absolute;
            border-radius: 50%;
            opacity: 0.1;
            animation: pulse 2s ease-in-out infinite;
          }
          
          .shape-1 {
            width: 192px;
            height: 192px;
            background-color: #0288d1;
            top: 25%;
            left: 25%;
            animation-delay: 0s;
          }
          
          .shape-2 {
            width: 128px;
            height: 128px;
            background-color: #0277bd;
            top: 75%;
            right: 25%;
            animation-delay: 1s;
          }
          
          .shape-3 {
            width: 96px;
            height: 96px;
            background-color: #01579b;
            top: 20%;
            right: 20%;
            animation-delay: 2s;
          }
          
          .shape-4 {
            width: 160px;
            height: 160px;
            background-color: #0288d1;
            bottom: 25%;
            left: 16.666667%;
            animation-delay: 0.5s;
          }
          
          .floating-lines {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
          }
          
          .line-1 {
            position: absolute;
            width: 1px;
            height: 128px;
            background: linear-gradient(to bottom, transparent, rgba(2, 136, 209, 0.2), transparent);
            top: 33.333333%;
            left: 33.333333%;
            transform: rotate(45deg);
          }
          
          .line-2 {
            position: absolute;
            width: 1px;
            height: 96px;
            background: linear-gradient(to bottom, transparent, rgba(2, 136, 209, 0.2), transparent);
            top: 66.666667%;
            right: 33.333333%;
            transform: rotate(-45deg);
          }
          
          .error-card {
            background-color: white;
            border-radius: 12px;
            border: 1px solid #e2e8f0;
            padding: 48px;
            text-align: center;
            max-width: 512px;
            width: 100%;
            position: relative;
            z-index: 10;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
            backdrop-filter: blur(16px);
          }
          
          .error-number {
            font-size: 128px;
            font-weight: 700;
            color: #3437c8;
            margin-bottom: 16px;
            position: relative;
            line-height: 1;
          }
          
          .error-number-shadow {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            font-size: 128px;
            font-weight: 700;
            color: #0288d1;
            opacity: 0.2;
            filter: blur(4px);
            animation: pulse 2s ease-in-out infinite;
          }
          
          .error-title {
            font-size: 32px;
            font-weight: 600;
            color: #1e293b;
            margin-bottom: 16px;
          }
          
          .error-description {
            color: #64748b;
            font-size: 16px;
            line-height: 1.6;
            margin-bottom: 32px;
          }
          
          .button-container {
            display: flex;
            flex-direction: column;
            gap: 16px;
            justify-content: center;
          }
          
          @media (min-width: 640px) {
            .button-container {
              flex-direction: row;
            }
          }
          
          .btn-primary {
            background-color: #3437c8;
            color: white;
            border: none;
            border-radius: 6px;
            padding: 12px 24px;
            font-weight: 500;
            font-size: 14px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            transition: all 0.2s ease;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
            transform: translateY(0);
          }
          
          .btn-primary:hover {
            background-color: #3437c8;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            transform: translateY(-2px);
          }
          
          .help-section {
            margin-top: 32px;
            padding-top: 24px;
            border-top: 1px solid #f1f5f9;
          }
          
          .help-text {
            color: #64748b;
            font-size: 14px;
            margin-bottom: 12px;
          }
          
          .search-button {
            background: none;
            border: none;
            color: #0288d1;
            font-size: 14px;
            font-weight: 500;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            margin: 0 auto;
            cursor: pointer;
            transition: color 0.2s ease;
          }
          
          .search-button:hover {
            color: #0277bd;
          }
          
          .decorative-dot {
            position: absolute;
            border-radius: 50%;
            animation: ping 1s cubic-bezier(0, 0, 0.2, 1) infinite;
          }
          
          .dot-1 {
            width: 8px;
            height: 8px;
            background-color: #0288d1;
            opacity: 0.3;
            top: 25%;
            left: 25%;
          }
          
          .dot-2 {
            width: 4px;
            height: 4px;
            background-color: #0277bd;
            opacity: 0.4;
            top: 75%;
            right: 33.333333%;
            animation-delay: 1s;
          }
          
          .dot-3 {
            width: 6px;
            height: 6px;
            background-color: #3437c8;
            opacity: 0.2;
            bottom: 25%;
            left: 33.333333%;
            animation-delay: 2s;
          }
          
          @keyframes pulse {
            0%, 100% {
              opacity: 0.1;
            }
            50% {
              opacity: 0.2;
            }
          }
          
          @keyframes ping {
            75%, 100% {
              transform: scale(2);
              opacity: 0;
            }
          }
        `}
      </style>
      
      <div className="not-found-container">
        {/* Background Graphics */}
        <div className="background-graphics">
          <div className="floating-shape shape-1"></div>
          <div className="floating-shape shape-2"></div>
          <div className="floating-shape shape-3"></div>
          <div className="floating-shape shape-4"></div>
          
          {/* Floating Lines */}
          <div className="floating-lines">
            <div className="line-1"></div>
            <div className="line-2"></div>
          </div>
        </div>

        {/* Main Error Card */}
        <div className="error-card">
          {/* 404 Number */}
          <div className="error-number">
            404
            <div className="error-number-shadow">404</div>
          </div>

          {/* Error Title */}
          <h1 className="error-title">Page Not Found</h1>

          {/* Error Description */}
          <p className="error-description">
            Oops! The page you're looking for seems to have wandered off. 
            Don't worry, it happens to the best of us. Let's get you back on track.
          </p>

          {/* Action Buttons */}
          <div className="button-container">
            <button onClick={handleBackToLogin} className="btn-primary">
              <ArrowLeft size={16} />
              Back to Login
            </button>
          </div>

          {/* Additional Help */}
          <div className="help-section">
            <p className="help-text">Still can't find what you're looking for?</p>
            <a
  href="https://www.youtube.com/watch?v=dQw4w9WgXcQ"
  target="_blank"           // Optional: opens in new tab
  rel="noopener noreferrer" // Security best practice
  className="search-button"
>
  <Search size={14} />
  Search our site
</a>
          </div>
        </div>

        {/* Decorative Elements */}
        <div className="decorative-dot dot-1"></div>
        <div className="decorative-dot dot-2"></div>
        <div className="decorative-dot dot-3"></div>
      </div>
    </>
  );
};

export default NotFoundPage;