/**
 * Zustand Store for Global State Management
 *
 * Manages authentication, user preferences, and app-wide state.
 */

import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

const fallbackStorage = {
  getItem: () => null,
  setItem: () => {},
  removeItem: () => {},
}

let notificationSequence = 0

const getBrowserStorage = () => {
  if (typeof window !== 'undefined' && window.localStorage && typeof window.localStorage.getItem === 'function') {
    return window.localStorage
  }

  if (typeof globalThis !== 'undefined' && globalThis.localStorage && typeof globalThis.localStorage.getItem === 'function') {
    return globalThis.localStorage
  }

  return fallbackStorage
}

export const createInitialState = () => ({
  user: null,
  token: null,
  isAuthenticated: false,
  sidebarOpen: true,
  mapCenter: [36.8219, -1.2921],
  mapZoom: 10,
  uploads: [],
  assessments: [],
  currentAssessment: null,
  quotaInfo: null,
  notifications: [],
})

const useStore = create(
  persist(
    (set, get) => ({
      // ==================== AUTH STATE ====================
      ...createInitialState(),

      setUser: (user) => set({ user, isAuthenticated: true }),
      setToken: (token) => {
        getBrowserStorage().setItem('authToken', token)
        set({ token, isAuthenticated: true })
      },
      logout: () => {
        getBrowserStorage().removeItem('authToken')
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
        notificationSequence += 1
        const id = notificationSequence
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
      resetStore: () => set(createInitialState()),
    }),
    {
      name: 'water-optimizer-storage',
      storage: createJSONStorage(getBrowserStorage),
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
