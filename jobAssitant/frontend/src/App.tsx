import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { BehavioralPage } from './pages/BehavioralPage'
import { CvPage } from './pages/CvPage'
import { HomePage } from './pages/HomePage'
import { JobsPage } from './pages/JobsPage'
import { JobsSourcesPage } from './pages/JobsSourcesPage'
import { LeetCodePage } from './pages/LeetCodePage'
import { LoginPage } from './pages/LoginPage'
import { ProfilePage } from './pages/ProfilePage'
import { RoadmapPage } from './pages/RoadmapPage'
import { getToken } from './api/client'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  return getToken() ? <>{children}</> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="jobs" element={<JobsPage />} />
        <Route path="jobs/sources" element={<JobsSourcesPage />} />
        <Route path="roadmap" element={<RoadmapPage />} />
        <Route path="leetcode" element={<LeetCodePage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="cv" element={<CvPage />} />
        <Route path="behavioral" element={<BehavioralPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
