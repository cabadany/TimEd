# 🎓 TimEd - Smart Educational Time Management System

<div align="center">
  <img src="https://img.shields.io/badge/Version-1.0-blue.svg" alt="Version 1.0">
  <img src="https://img.shields.io/badge/React-18.x-61DAFB.svg" alt="React">
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Android-Kotlin-A4C639.svg" alt="Android Kotlin">
  <img src="https://img.shields.io/badge/Firebase-Integrated-FFCA28.svg" alt="Firebase">
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="MIT License">
</div>

<div align="center">
  <h3>🚀 Revolutionizing Educational Institution Management</h3>
  <p><em>"Smarter Attendance Starts Here"</em></p>
</div>

---

## 🌟 Overview

**TimEd** is a comprehensive, multi-platform time management and educational platform designed to streamline operations for schools, colleges, and universities. Our system provides intelligent tools for event management, real-time attendance tracking, automated certificate generation, and seamless department organization.

### 🎯 Mission Statement
*To create intuitive, user-friendly solutions that address the unique challenges faced by educational institutions in managing their day-to-day activities, enhancing productivity and efficiency through modern technology.*

---

## ✨ Key Features

### 📊 **Real-Time Dashboard & Analytics**
- 📈 Comprehensive attendance analytics with visualizations
- 📅 Interactive calendar with event management
- 📧 Email status tracking and notifications
- 🎯 Performance metrics and insights
- 🌙 Dark/Light theme support

### 👥 **Attendance Management**
- 📱 QR Code-based check-in/check-out
- 📸 Facial recognition integration (Mobile)
- ⏰ Real-time attendance tracking
- 📊 Late arrival monitoring with customizable thresholds
- 📝 Digital excuse letter system

### 🎪 **Event Management**
- 📅 Full CRUD operations for events
- 🎫 QR code generation for event participation
- 📧 Automated email notifications
- 📊 Event analytics and reporting
- 🏆 Integrated certificate generation

### 🏛️ **Department & User Management**
- 👤 Role-based access control (Admin/Faculty)
- 🏢 Department organization and management
- 👨‍🏫 User profile management with photo uploads
- 🔐 Secure authentication system

### 🏆 **Certificate Generation**
- 🎨 Customizable certificate templates
- 📄 PDF generation with professional layouts
- 🏅 Automated certificate distribution
- 📧 Email delivery system

---

## 🏗️ System Architecture

### 🌐 **Multi-Platform Design**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Frontend  │    │  Mobile App     │    │  Backend API    │
│                 │    │                 │    │                 │
│  React.js       │◄──►│  Android        │◄──►│  Spring Boot    │
│  Material-UI    │    │  Kotlin         │    │  Java 17        │
│  Vite           │    │  Camera API     │    │  Firebase       │
│  Firebase Auth  │    │  QR Scanner     │    │  REST APIs      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │    Database     │
                    │                 │
                    │  Firebase       │
                    │  Firestore      │
                    │  Real-time DB   │
                    │  Storage        │
                    └─────────────────┘
```

### 🛠️ **Tech Stack**

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Frontend** | React 19, Material-UI, Vite | Responsive web interface |
| **Backend** | Spring Boot 3.4, Java 17 | RESTful API services |
| **Mobile** | Android, Kotlin | Native mobile application |
| **Database** | Firebase Firestore | NoSQL document database |
| **Authentication** | Firebase Auth | Secure user management |
| **Storage** | Firebase Storage | File and image storage |
| **PDF Generation** | iText7 | Certificate creation |
| **QR Codes** | ZXing | QR code generation/scanning |
| **Email** | SMTP (Gmail) | Notification system |

---

## 🚀 Quick Start

### 📋 Prerequisites

- **Node.js** 18+ and npm
- **Java** 17+
- **Android Studio** (for mobile development)
- **Firebase** project setup
- **Gmail** account for email services

### ⚡ Installation

#### 1️⃣ **Clone the Repository**
```bash
git clone https://github.com/cabadany/TimEd.git
cd TimEd
```

#### 2️⃣ **Backend Setup**
```bash
cd backend/TimEd
./mvnw clean install
./mvnw spring-boot:run
```

#### 3️⃣ **Frontend Setup**
```bash
cd capstone-frontend
npm install
npm run dev
```

#### 4️⃣ **Mobile Setup**
```bash
cd mobile
# Open in Android Studio
# Build and run on device/emulator
```

### 🔧 **Configuration**

#### Firebase Setup
1. Create a Firebase project
2. Enable Authentication, Firestore, and Storage
3. Download configuration files:
   - `google-services.json` for Android
   - Firebase config for Web

#### Environment Variables
```properties
# Backend (application.properties)
FRONTEND_BASE_URL=http://localhost:5173
GMAIL_USERNAME=your-email@gmail.com
GMAIL_APP_PASSWORD=your-app-password
```

---

## 📱 Platform Features

### 🌐 **Web Application**
- **Dashboard**: Real-time analytics and insights
- **Event Management**: Create, manage, and monitor events
- **User Management**: Account administration and profiles
- **Attendance Tracking**: Monitor faculty time-in/out
- **Certificate Generator**: Design and distribute certificates
- **Department Management**: Organize institutional structure

### 📱 **Mobile Application**
- **Quick Time-In/Out**: Camera-based attendance
- **QR Code Scanner**: Join events seamlessly
- **Profile Management**: Update personal information
- **Event Participation**: Real-time event engagement
- **Excuse Letters**: Submit digital excuse forms
- **Offline Support**: Continue working without internet

---

## 👥 Development Team

<div align="center">

| Role | Developer | Expertise |
|------|-----------|-----------|
| 🎨 **Web Frontend** | Clark Gemongala | React.js, UI/UX Design |
| 🛠️ **Web Backend** | John Wayne Largo | Spring Boot, API Development |
| 📱 **Mobile Frontend** | Mikhail James Navaroo | Android, Kotlin |
| 🔧 **Mobile Backend** | Danisse Cabana | Firebase, Mobile Integration |
| 📋 **Project Manager** | Alexa Tumungha | Agile, Team Leadership |

</div>

---

## 📊 System Metrics

### 🎯 **Performance Goals**
- ⚡ **Response Time**: < 200ms API responses
- 📱 **Mobile Performance**: 60fps smooth animations
- 🔄 **Real-time Updates**: < 1 second latency
- 💾 **Storage Efficiency**: Optimized Firebase usage
- 🔐 **Security**: End-to-end encryption

### 📈 **Scalability**
- 👥 **Concurrent Users**: 1000+ simultaneous
- 📚 **Data Capacity**: Unlimited with Firebase
- 🌍 **Global Access**: CDN-optimized delivery
- 📊 **Analytics**: Real-time dashboard updates

---

## 🛣️ Roadmap

### 🔮 **Upcoming Features**
- [ ] 📊 Advanced reporting and analytics
- [ ] 🤖 AI-powered attendance predictions
- [ ] 📧 Enhanced notification system
- [ ] 🌐 Multi-language support
- [ ] 📱 iOS mobile application
- [ ] 🔗 Third-party integrations (LMS)

### 🎯 **Version 2.0 Goals**
- [ ] 🧠 Machine learning insights
- [ ] 📈 Predictive analytics
- [ ] 🎮 Gamification features
- [ ] 🌟 Parent/Guardian portal
- [ ] 📚 Academic calendar integration

---

## 🤝 Contributing

We welcome contributions from the community! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting pull requests.

### 🔄 **Development Workflow**
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support & Contact

<div align="center">

### 🆘 **Need Help?**

📧 **Email**: timeedsystem@gmail.com  
🌐 **Website**: [TimeD System](https://timedsystem.netlify.app)  
🐛 **Issues**: [GitHub Issues](https://github.com/cabadany/TimEd/issues)  

</div>

---

<div align="center">
  <h3>🎓 Built with ❤️ for Educational Excellence</h3>
  <p><em>Empowering institutions through intelligent time management</em></p>
  
  ⭐ **Star this repository if you find it helpful!** ⭐
</div>

---

<div align="center">
  <img src="https://img.shields.io/github/stars/cabadany/TimEd?style=social" alt="GitHub stars">
  <img src="https://img.shields.io/github/forks/cabadany/TimEd?style=social" alt="GitHub forks">
  <img src="https://img.shields.io/github/watchers/cabadany/TimEd?style=social" alt="GitHub watchers">
</div>
