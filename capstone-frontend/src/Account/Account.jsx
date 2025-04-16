"use client"

import { useState } from "react"
import { PlusCircle, Search } from "lucide-react"
import "./Account.css"

const Account = () => {
  const [activeTab, setActiveTab] = useState("accounts")
  const [professors, setProfessors] = useState([
    { id: "1001", name: "Dr. Jane Smith", email: "jsmith@university.edu", department: "Computer Science" },
    { id: "1002", name: "Dr. Robert Johnson", email: "rjohnson@university.edu", department: "Mathematics" },
    { id: "1003", name: "Dr. Maria Garcia", email: "mgarcia@university.edu", department: "Physics" },
  ])
  const [searchTerm, setSearchTerm] = useState("")
  const [showAddModal, setShowAddModal] = useState(false)
  const [newProfessor, setNewProfessor] = useState({
    id: "",
    name: "",
    email: "",
    department: "",
    password: "",
  })

  const handleTabChange = (tab) => {
    setActiveTab(tab)
  }

  const handleSearch = (e) => {
    setSearchTerm(e.target.value)
  }

  const filteredProfessors = professors.filter(
    (professor) =>
      professor.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      professor.id.includes(searchTerm) ||
      professor.department.toLowerCase().includes(searchTerm.toLowerCase()),
  )

  const handleAddProfessor = () => {
    setProfessors([
      ...professors,
      { ...newProfessor, id: newProfessor.id || `${Math.floor(1000 + Math.random() * 9000)}` },
    ])
    setNewProfessor({ id: "", name: "", email: "", department: "", password: "" })
    setShowAddModal(false)
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setNewProfessor({ ...newProfessor, [name]: value })
  }

  return (
    <div className="account-container">
      {/* Sidebar */}
      <div className="sidebar">
        <nav className="sidebar-nav">
          <ul>
            <li className={activeTab === "dashboard" ? "active" : ""} onClick={() => handleTabChange("dashboard")}>
              <span className="icon">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                </svg>
              </span>
              <span>Dashboard</span>
            </li>
            <li className={activeTab === "event" ? "active" : ""} onClick={() => handleTabChange("event")}>
              <span className="icon">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
                </svg>
              </span>
              <span>Event</span>
            </li>
            <li className={activeTab === "accounts" ? "active" : ""} onClick={() => handleTabChange("accounts")}>
              <span className="icon">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke={activeTab === "accounts" ? "#0000ff" : "currentColor"}
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </span>
              <span style={{ color: activeTab === "accounts" ? "#0000ff" : "" }}>Accounts</span>
            </li>
            <li className={activeTab === "setting" ? "active" : ""} onClick={() => handleTabChange("setting")}>
              <span className="icon">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="24"
                  height="24"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <circle cx="12" cy="12" r="3"></circle>
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82V9a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                </svg>
              </span>
              <span>Setting</span>
            </li>
          </ul>
        </nav>
      </div>

      {/* Main Content */}
      <div className="main-content">
        <div className="header">
          <h1>Account Management</h1>
        </div>

        {activeTab === "accounts" && (
          <div className="professors-section">
            <div className="section-header">
              <h2>Professor Accounts</h2>
              <div className="actions">
                <div className="search-bar">
                  <Search size={18} />
                  <input type="text" placeholder="Search professors..." value={searchTerm} onChange={handleSearch} />
                </div>
                <button className="add-button" onClick={() => setShowAddModal(true)}>
                  <PlusCircle size={18} />
                  <span>Add Professor</span>
                </button>
              </div>
            </div>

            <div className="professors-table">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Department</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredProfessors.map((professor) => (
                    <tr key={professor.id}>
                      <td>{professor.id}</td>
                      <td>{professor.name}</td>
                      <td>{professor.email}</td>
                      <td>{professor.department}</td>
                      <td>
                        <button className="action-button edit">
                          <Search size={16} />
                        </button>
                        <button className="action-button delete">
                          <span>×</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === "dashboard" && (
          <div className="dashboard-section">
            <h2>Dashboard</h2>
            <p>Welcome to the admin dashboard. Manage professor accounts and system settings here.</p>
          </div>
        )}

        {activeTab === "event" && (
          <div className="event-section">
            <h2>Event Management</h2>
            <p>Manage events and schedules here.</p>
          </div>
        )}

        {activeTab === "setting" && (
          <div className="setting-section">
            <h2>Settings</h2>
            <p>Configure system settings and preferences.</p>
          </div>
        )}

        {/* Add Professor Modal */}
        {showAddModal && (
          <div className="modal-overlay">
            <div className="modal">
              <div className="modal-header">
                <h3>Add New Professor</h3>
                <button className="close-button" onClick={() => setShowAddModal(false)}>
                  ×
                </button>
              </div>
              <div className="modal-body">
                <div className="form-group">
                  <label>ID Number</label>
                  <input
                    type="text"
                    name="id"
                    placeholder="Enter ID number"
                    value={newProfessor.id}
                    onChange={handleInputChange}
                  />
                </div>
                <div className="form-group">
                  <label>Full Name</label>
                  <input
                    type="text"
                    name="name"
                    placeholder="Enter full name"
                    value={newProfessor.name}
                    onChange={handleInputChange}
                  />
                </div>
                <div className="form-group">
                  <label>Email</label>
                  <input
                    type="email"
                    name="email"
                    placeholder="Enter email address"
                    value={newProfessor.email}
                    onChange={handleInputChange}
                  />
                </div>
                <div className="form-group">
                  <label>Department</label>
                  <input
                    type="text"
                    name="department"
                    placeholder="Enter department"
                    value={newProfessor.department}
                    onChange={handleInputChange}
                  />
                </div>
                <div className="form-group">
                  <label>Password</label>
                  <input
                    type="password"
                    name="password"
                    placeholder="Enter password"
                    value={newProfessor.password}
                    onChange={handleInputChange}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button className="cancel-button" onClick={() => setShowAddModal(false)}>
                  Cancel
                </button>
                <button className="save-button" onClick={handleAddProfessor}>
                  Add Professor
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Account