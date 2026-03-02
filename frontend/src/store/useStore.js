/**
 * Zustand Store for Global State Management
 *
 * Manages authentication, user preferences, and app-wide state.
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const useStore = create(
  persist(
    (set, get) => ({
      // ==================== AUTH STATE ====================
      user: null,
      token: null,
      isAuthenticated: false,

      setUser: (user) => set({ user, isAuthenticated: true }),
      setToken: (token) => {
        localStorage.setItem('authToken', token)
        set({ token, isAuthenticated: true })
      },
      logout: () => {
        localStorage.removeItem('authToken')
        set({ user: null, token: null, isAuthenticated: false })
      },

      // ==================== UI STATE ====================
      sidebarOpen: true,
      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),

      // Map center and zoom
      mapCenter: [36.8219, -1.2921], // Nairobi, Kenya
      mapZoom: 10,
      setMapCenter: (center) => set({ mapCenter: center }),
      setMapZoom: (zoom) => set({ mapZoom: zoom }),

      // ==================== DATA STATE ====================
      uploads: [],
      setUploads: (uploads) => set({ uploads }),
      addUpload: (upload) => set((state) => ({ uploads: [upload, ...state.uploads] })),
      removeUpload: (uploadId) => set((state) => ({
        uploads: state.uploads.filter(u => u.id !== uploadId)
      })),

      assessments: [],
      setAssessments: (assessments) => set({ assessments }),
      addAssessment: (assessment) => set((state) => ({
        assessments: [assessment, ...state.assessments]
      })),
      removeAssessment: (assessmentId) => set((state) => ({
        assessments: state.assessments.filter(a => a.id !== assessmentId)
      })),

      // Current assessment being viewed
      currentAssessment: null,
      setCurrentAssessment: (assessment) => set({ currentAssessment: assessment }),

      // ==================== STORAGE QUOTA ====================
      quotaInfo: null,
      setQuotaInfo: (quotaInfo) => set({ quotaInfo }),

      // ==================== NOTIFICATIONS ====================
      notifications: [],
      addNotification: (notification) => {
        const id = Date.now()
        set((state) => ({
          notifications: [...state.notifications, { ...notification, id }]
        }))
        // Auto-remove after 5 seconds
        setTimeout(() => {
          get().removeNotification(id)
        }, 5000)
      },
      removeNotification: (id) => set((state) => ({
        notifications: state.notifications.filter(n => n.id !== id)
      })),
    }),
    {
      name: 'water-optimizer-storage',
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
        sidebarOpen: state.sidebarOpen,
        mapCenter: state.mapCenter,
        mapZoom: state.mapZoom,
      }),
    }
  )
)

export default useStore
