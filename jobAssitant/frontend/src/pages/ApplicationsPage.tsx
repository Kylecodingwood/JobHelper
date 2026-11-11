import { useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'

type App = {
  appId: string
  companyName: string
  roleName: string
  category: string
  isSwe: boolean
  status: string
  notes: string | null
}

type CategoryStats = { total: number; rejected: number; inPipeline: number }
type Stats = {
  total: number
  active: number
  rejected: number
  inPipeline: number
  byCategory: Record<string, CategoryStats>
  pipeline: { appId: string; companyName: string; roleName: string; status: string }[]
}

const CAT_LABEL: Record<string, string> = {
  grad_swe: 'Graduate · SWE',
  grad_tech: 'Graduate · Tech',
  grad_other: 'Graduate · Other',
  intern_summer: 'Intern · Summer',
  intern_long: 'Intern · Long-term',
  intern_unknown: 'Intern · Unknown',
  other: 'Other',
  unknown: 'Unknown Role',
}

const CAT_COLOR: Record<string, string> = {
  grad_swe: '#10b981',
  grad_tech: '#6366f1',
  grad_other: '#3b82f6',
  intern_summer: '#f59e0b',
  intern_long: '#14b8a6',
  intern_unknown: '#f97316',
  other: '#8b5cf6',
  unknown: '#94a3b8',
}

const STATUS_LABEL: Record<string, string> = {
  applied: 'Applied',
  rejected: 'Rejected',
  oa: 'OA',
  hackerrank: 'HackerRank',
  video: 'Video Interview',
  offer: 'Offer 🎉',
}

const STATUS_COLOR: Record<string, string> = {
  applied: '#6b7280',
  rejected: '#ef4444',
  oa: '#3b82f6',
  hackerrank: '#6366f1',
  video: '#8b5cf6',
  offer: '#10b981',
}

const CAT_ORDER = ['grad_swe','grad_tech','grad_other','intern_summer','intern_long','intern_unknown','other','unknown']

const CAT_GROUPS = [
  { label: 'Graduate', cats: ['grad_swe','grad_tech','grad_other'] },
  { label: 'Intern', cats: ['intern_summer','intern_long','intern_unknown'] },
  { label: 'Other', cats: ['other','unknown'] },
]

export function ApplicationsPage() {
  const [stats, setStats] = useState<Stats | null>(null)
  const [apps, setApps] = useState<App[]>([])
  const [filterCat, setFilterCat] = useState('all')
  const [filterStatus, setFilterStatus] = useState('all')
  const [search, setSearch] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  useEffect(() => { void load() }, [])

  async function load() {
    try {
      const [s, a] = await Promise.all([
        api.get<Stats>('/applications/stats'),
        api.get<App[]>('/applications'),
      ])
      setStats(s)
      setApps(a)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Load failed')
    }
  }

  async function updateStatus(app: App, status: string) {
    try {
      await api.patch(`/applications/${app.appId}`, { status })
      setApps(prev => prev.map(a => a.appId === app.appId ? { ...a, status } : a))
      setStats(null)
      const s = await api.get<Stats>('/applications/stats')
      setStats(s)
    } catch (e) {
      setMsg(e instanceof ApiError ? e.message : 'Update failed')
    }
  }

  const filtered = apps.filter(a => {
    if (filterCat !== 'all' && a.category !== filterCat) return false
    if (filterStatus !== 'all' && a.status !== filterStatus) return false
    if (search) {
      const q = search.toLowerCase()
      if (!a.companyName.toLowerCase().includes(q) && !a.roleName.toLowerCase().includes(q)) return false
    }
    return true
  })

  return (
    <div className="page apps-page">
      <div className="topbar">
        <h1>Applications</h1>
        <div className="spacer" />
        <button className="btn btn-sm" onClick={() => void load()}>刷新</button>
      </div>
      {msg && <div className="banner">{msg}</div>}

      {stats && (
        <>
          {/* Hero stats */}
          <div className="apps-hero">
            <div className="apps-stat-card">
              <div className="apps-stat-num">{stats.total}</div>
              <div className="apps-stat-label">Total</div>
            </div>
            <div className="apps-stat-card active">
              <div className="apps-stat-num">{stats.active}</div>
              <div className="apps-stat-label">Active</div>
            </div>
            <div className="apps-stat-card rejected">
              <div className="apps-stat-num">{stats.rejected}</div>
              <div className="apps-stat-label">Rejected</div>
            </div>
            <div className="apps-stat-card pipeline">
              <div className="apps-stat-num">{stats.inPipeline}</div>
              <div className="apps-stat-label">In Pipeline</div>
            </div>
          </div>

          {/* Category breakdown */}
          <div className="apps-section">
            <div className="apps-section-row">
              {CAT_GROUPS.map(group => {
                const groupTotal = group.cats.reduce((s, c) => s + (stats.byCategory[c]?.total ?? 0), 0)
                return (
                  <div key={group.label} className="apps-group-card">
                    <div className="apps-group-head">
                      <span className="apps-group-title">{group.label}</span>
                      <span className="apps-group-total">{groupTotal}</span>
                    </div>
                    {group.cats.map(cat => {
                      const cs = stats.byCategory[cat]
                      if (!cs || cs.total === 0) return null
                      const pct = Math.round(cs.total / stats.total * 100)
                      return (
                        <div key={cat} className="apps-cat-row">
                          <div className="apps-cat-meta">
                            <span className="apps-cat-dot" style={{ background: CAT_COLOR[cat] }} />
                            <span className="apps-cat-name">{CAT_LABEL[cat]}</span>
                            <span className="apps-cat-count">{cs.total}</span>
                          </div>
                          <div className="apps-bar-track">
                            <div className="apps-bar-fill" style={{ width: `${pct}%`, background: CAT_COLOR[cat] }} />
                          </div>
                          <div className="apps-cat-tags">
                            {cs.rejected > 0 && <span className="apps-tag red">{cs.rejected} rejected</span>}
                            {cs.inPipeline > 0 && <span className="apps-tag blue">{cs.inPipeline} in pipeline</span>}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )
              })}
            </div>
          </div>

          {/* Pipeline highlight */}
          {stats.pipeline.length > 0 && (
            <div className="apps-pipeline-section">
              <div className="apps-pipeline-title">🔥 Active Pipeline</div>
              <div className="apps-pipeline-list">
                {stats.pipeline.map(p => (
                  <div key={p.appId} className="apps-pipeline-item">
                    <span className="apps-pipeline-company">{p.companyName}</span>
                    <span className="apps-pipeline-role">{p.roleName}</span>
                    <span className="apps-status-badge" style={{ background: STATUS_COLOR[p.status] + '22', color: STATUS_COLOR[p.status] }}>
                      {STATUS_LABEL[p.status]}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {/* Filters + Table */}
      <div className="apps-table-section">
        <div className="apps-filters">
          <input className="apps-search" type="text" placeholder="Search company or role..." value={search} onChange={e => setSearch(e.target.value)} />
          <select value={filterCat} onChange={e => setFilterCat(e.target.value)}>
            <option value="all">All Categories</option>
            {CAT_ORDER.map(c => <option key={c} value={c}>{CAT_LABEL[c]}</option>)}
          </select>
          <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
            <option value="all">All Statuses</option>
            {Object.keys(STATUS_LABEL).map(s => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
          </select>
          <span className="apps-filter-count">{filtered.length} records</span>
        </div>

        <div className="apps-table">
          <div className="apps-table-head">
            <span>Company</span>
            <span>Role</span>
            <span>Category</span>
            <span>Status</span>
          </div>
          {filtered.map(a => (
            <div key={a.appId} className={`apps-table-row${a.status === 'rejected' ? ' rejected' : a.status !== 'applied' ? ' active' : ''}`}>
              <span className="apps-col-company">{a.companyName}</span>
              <span className="apps-col-role">{a.roleName}{a.notes ? <span className="apps-note" title={a.notes}>ⓘ</span> : null}</span>
              <span>
                <span className="apps-cat-dot-sm" style={{ background: CAT_COLOR[a.category] }} />
                {CAT_LABEL[a.category]}
              </span>
              <select
                className="apps-status-select"
                value={a.status}
                style={{ color: STATUS_COLOR[a.status] }}
                onChange={e => void updateStatus(a, e.target.value)}
              >
                {Object.entries(STATUS_LABEL).map(([v, l]) => (
                  <option key={v} value={v}>{l}</option>
                ))}
              </select>
            </div>
          ))}
          {!filtered.length && <div className="empty-hint">No applications match the filters</div>}
        </div>
      </div>
    </div>
  )
}
