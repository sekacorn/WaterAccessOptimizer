/**
 * Admin Dashboard Page
 *
 * Features for ADMIN and SUPER_ADMIN roles:
 * - User management (view, create, edit, delete users)
 * - Role assignment
 * - System statistics
 * - Audit logs
 * - Enterprise management
 */

import React, { useState, useEffect } from 'react'
import { Users, Shield, TrendingUp, Database, Activity, AlertCircle } from 'lucide-react'
import './Admin.css'

const Admin = ({ mbtiType }) => {
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    totalEnterprises: 0,
    dataUploadsToday: 0,
    moderators: 0,
    admins: 0
  })

  const [users, setUsers] = useState([])
  const [selectedUser, setSelectedUser] = useState(null)
  const [showUserModal, setShowUserModal] = useState(false)

  // Load admin statistics
  useEffect(() => {
    loadStats()
    loadUsers()
  }, [])

  const loadStats = async () => {
    // In production, fetch from API
    // Mock data for demonstration
    setStats({
      totalUsers: 1245,
      activeUsers: 892,
      totalEnterprises: 34,
      dataUploadsToday: 127,
      moderators: 15,
      admins: 5
    })
  }

  const loadUsers = async () => {
    // In production, fetch from API: GET /api/admin/users
    // Mock data for demonstration
    setUsers([
      { id: 1, username: 'john_doe', email: 'john@example.com', role: 'USER', isActive: true, loginCount: 45 },
      { id: 2, username: 'jane_admin', email: 'jane@example.com', role: 'ADMIN', isActive: true, loginCount: 234 },
      { id: 3, username: 'mod_user', email: 'mod@example.com', role: 'MODERATOR', isActive: true, loginCount: 156 },
    ])
  }

  const handleRoleChange = async (userId, newRole) => {
    // In production: POST /api/admin/users/{userId}/role
    console.log(`Changing user ${userId} role to ${newRole}`)
    alert(`Role changed to ${newRole} - This would update in production`)
    loadUsers()
  }

  const handleToggleUserStatus = async (userId, currentStatus) => {
    // In production: POST /api/admin/users/{userId}/toggle-status
    console.log(`Toggling user ${userId} active status`)
    alert(`User ${currentStatus ? 'deactivated' : 'activated'} - This would update in production`)
    loadUsers()
  }

  const handleDeleteUser = async (userId) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      // In production: DELETE /api/admin/users/{userId}
      console.log(`Deleting user ${userId}`)
      alert('User deleted - This would delete in production')
      loadUsers()
    }
  }

  return (
    <div className="admin-page">
      <div className="container">
        <h1><Shield size={32} /> Admin Dashboard</h1>
        <p className="page-subtitle">
          System administration and user management
        </p>

        {/* Statistics Cards */}
        <div className="stats-grid">
          <div className="stat-card card">
            <div className="stat-icon">
              <Users size={32} />
            </div>
            <div className="stat-content">
              <h3>{stats.totalUsers}</h3>
              <p>Total Users</p>
              <span className="stat-detail">{stats.activeUsers} active</span>
            </div>
          </div>

          <div className="stat-card card">
            <div className="stat-icon">
              <Database size={32} />
            </div>
            <div className="stat-content">
              <h3>{stats.totalEnterprises}</h3>
              <p>Enterprises</p>
              <span className="stat-detail">Organizations using the platform</span>
            </div>
          </div>

          <div className="stat-card card">
            <div className="stat-icon">
              <TrendingUp size={32} />
            </div>
            <div className="stat-content">
              <h3>{stats.dataUploadsToday}</h3>
              <p>Data Uploads Today</p>
              <span className="stat-detail">Hydrological, community, infrastructure</span>
            </div>
          </div>

          <div className="stat-card card">
            <div className="stat-icon">
              <Shield size={32} />
            </div>
            <div className="stat-content">
              <h3>{stats.moderators + stats.admins}</h3>
              <p>Staff Members</p>
              <span className="stat-detail">{stats.moderators} moderators, {stats.admins} admins</span>
            </div>
          </div>
        </div>

        {/* User Management Section */}
        <div className="management-section card">
          <h2><Users size={24} /> User Management</h2>

          <div className="action-buttons">
            <button className="button button-primary" onClick={() => setShowUserModal(true)}>
              Create New User
            </button>
            <button className="button button-secondary">
              Export User List
            </button>
          </div>

          {/* Users Table */}
          <div className="table-container">
            <table className="users-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Login Count</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map(user => (
                  <tr key={user.id}>
                    <td>{user.username}</td>
                    <td>{user.email}</td>
                    <td>
                      <select
                        value={user.role}
                        onChange={(e) => handleRoleChange(user.id, e.target.value)}
                        className="role-select"
                      >
                        <option value="USER">User</option>
                        <option value="MODERATOR">Moderator</option>
                        <option value="ADMIN">Admin</option>
                        <option value="ENTERPRISE_ADMIN">Enterprise Admin</option>
                        <option value="SUPER_ADMIN">Super Admin</option>
                      </select>
                    </td>
                    <td>
                      <span className={`status-badge ${user.isActive ? 'active' : 'inactive'}`}>
                        {user.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>{user.loginCount}</td>
                    <td className="action-buttons-cell">
                      <button
                        className="button button-sm"
                        onClick={() => setSelectedUser(user)}
                      >
                        View
                      </button>
                      <button
                        className="button button-sm button-warning"
                        onClick={() => handleToggleUserStatus(user.id, user.isActive)}
                      >
                        {user.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        className="button button-sm button-danger"
                        onClick={() => handleDeleteUser(user.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="quick-actions">
          <h2>Quick Actions</h2>
          <div className="actions-grid">
            <div className="action-card card">
              <Activity size={24} />
              <h3>View Audit Logs</h3>
              <p>Review system activity and user actions</p>
              <button className="button button-sm">View Logs</button>
            </div>

            <div className="action-card card">
              <AlertCircle size={24} />
              <h3>Pending Reports</h3>
              <p>Review and resolve user reports</p>
              <button className="button button-sm">View Reports</button>
            </div>

            <div className="action-card card">
              <Database size={24} />
              <h3>Enterprise Management</h3>
              <p>Manage enterprise accounts and subscriptions</p>
              <button className="button button-sm">Manage</button>
            </div>
          </div>
        </div>

        {/* User Details Modal */}
        {selectedUser && (
          <div className="modal-overlay" onClick={() => setSelectedUser(null)}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
              <h2>User Details</h2>
              <div className="user-details">
                <p><strong>Username:</strong> {selectedUser.username}</p>
                <p><strong>Email:</strong> {selectedUser.email}</p>
                <p><strong>Role:</strong> {selectedUser.role}</p>
                <p><strong>Status:</strong> {selectedUser.isActive ? 'Active' : 'Inactive'}</p>
                <p><strong>Login Count:</strong> {selectedUser.loginCount}</p>
              </div>
              <button className="button" onClick={() => setSelectedUser(null)}>Close</button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Admin
