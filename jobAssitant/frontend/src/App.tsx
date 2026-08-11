import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { BehavioralPage } from './pages/BehavioralPage'
import { CvPage } from './pages/CvPage'
import { HomePage } from './pages/HomePage'
import { JobsPage } from './pages/JobsPage'
import { JobsSourcesPage } from './pages/JobsSourcesPage'
import { ProfilePage } from './pages/ProfilePage'
import { RoadmapPage } from './pages/RoadmapPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="jobs" element={<JobsPage />} />
        <Route path="jobs/sources" element={<JobsSourcesPage />} />
        <Route path="roadmap" element={<RoadmapPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="cv" element={<CvPage />} />
        <Route path="behavioral" element={<BehavioralPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
