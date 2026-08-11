import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api/client'

const FOCUS_KEY_STORAGE = 'homeFocusKey'

type Action = {
  actionId: string
  title: string
  sourceDomain: string
  priorityBand: string
  primaryReason?: string
  pinned?: boolean
  version: number
  status: string
}

type Job = {
  jobId: string
  title: string
  company?: string
  location?: string
  rankTier?: string
}

type HomeFeed = {
  actions: { content: Action[]; hasMore?: boolean }
  newJobs: { content: Job[] }
  generatedAt: string
}

type PriorityEvidence = {
  calculationId: string
  priorityBand: string
  ruleVersion: string
  factors: Array<{
    factor: string
    effectiveOrder: number
    factValue?: string
    explanation?: string
  }>
}

export function HomePage() {
  const [feed, setFeed] = useState<HomeFeed | null>(null)
  const [degraded, setDegraded] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [evidenceAction, setEvidenceAction] = useState<Action | null>(null)
  const [evidence, setEvidence] = useState<PriorityEvidence | null>(null)
  const [evidenceLoading, setEvidenceLoading] = useState(false)
  const [expanded, setExpanded] = useState(false)
  const navigate = useNavigate()
  const focusApplied = useRef(false)

  async function loadDegraded() {
    const [actionsPage, newJobs] = await Promise.all([
      api.get<{ content: Action[] }>('/actions?active=true&page=0&size=3&sort=prioritySortKey,asc'),
      api.get<Job[] | { content: Job[] }>('/jobs/new?limit=10'),
    ])
    const jobs = Array.isArray(newJobs) ? newJobs : newJobs.content || []
    setFeed({
      actions: { content: actionsPage.content, hasMore: actionsPage.content.length >= 3 },
      newJobs: { content: jobs },
      generatedAt: new Date().toISOString(),
    })
    setDegraded(true)
  }

  async function load(limit = 3) {
    try {
      setError(null)
      setDegraded(false)
      const data = await api.get<HomeFeed>(`/home?actionLimit=${limit}`)
      setFeed(data)
      setExpanded(limit > 3)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        navigate('/profile')
        return
      }
      try {
        await loadDegraded()
        setError('Home 聚合暂不可用，已降级加载 Actions + New Jobs')
      } catch (fallbackErr) {
        setError(fallbackErr instanceof Error ? fallbackErr.message : 'Failed to load home')
      }
    }
  }

  useEffect(() => {
    void load(3)
  }, [])

  useEffect(() => {
    if (focusApplied.current) return
    const focusKey = sessionStorage.getItem(FOCUS_KEY_STORAGE)
    if (!focusKey) return
    focusApplied.current = true
    sessionStorage.removeItem(FOCUS_KEY_STORAGE)
    requestAnimationFrame(() => {
      const el = document.querySelector(`[data-focus-key="${CSS.escape(focusKey)}"]`)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        el.classList.add('selected')
      }
    })
  }, [feed])

  async function onOpen(action: Action) {
    try {
      const nav = await api.get<{ route: string; focusKey?: string }>(`/actions/${action.actionId}/navigation`)
      if (nav.focusKey) {
        sessionStorage.setItem(FOCUS_KEY_STORAGE, nav.focusKey)
      }
      navigate(nav.route || '/')
    } catch {
      navigate('/jobs')
    }
  }

  async function decide(action: Action, operation: string) {
    try {
      await api.post(`/actions/${action.actionId}/decisions`, {
        operation,
        reason: null,
        expectedVersion: action.version,
      })
      await load(expanded ? 20 : 3)
    } catch (e) {
      alert(e instanceof ApiError ? `${e.code}: ${e.message}` : 'Decision failed')
    }
  }

  async function openEvidence(action: Action, e: React.MouseEvent) {
    e.stopPropagation()
    setEvidenceAction(action)
    setEvidence(null)
    setEvidenceLoading(true)
    try {
      const data = await api.get<PriorityEvidence>(`/actions/${action.actionId}/priority-evidence`)
      setEvidence(data)
    } catch (err) {
      alert(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Load evidence failed')
      setEvidenceAction(null)
    } finally {
      setEvidenceLoading(false)
    }
  }

  const bandClass = (band: string) => {
    if (band === 'DEADLINE') return 'band-deadline'
    if (band === 'BLOCKER') return 'band-blocker'
    if (band === 'USER_PIN') return 'band-pin'
    return 'band-suggest'
  }

  return (
    <div className="page">
      <div className="topbar">
        <h1>今日优先</h1>
        <div className="spacer" />
        <Link className="btn btn-sm" to="/roadmap">
          Roadmap
        </Link>
        <Link className="btn btn-sm" to="/jobs">
          Jobs
        </Link>
        <button className="btn btn-sm" type="button" onClick={() => void load(expanded ? 20 : 3)}>
          ↻ 刷新
        </button>
      </div>
      <div className="main-col home-compact">
        {degraded && error && <div className="banner">{error}</div>}
        {!degraded && error && <div className="banner banner-error">{error}</div>}
        <section className="section">
          <div className="section-head">
            <h2>今日优先</h2>
            <span className="count">默认 3 条</span>
          </div>
          {!feed?.actions.content.length && (
            <div className="empty">
              <h3>暂无待办 Action</h3>
              <p>完善 Profile、添加 Roadmap 待办，或同步职位后会出现优先事项。</p>
              <div className="row">
                <Link className="btn" to="/profile">
                  Profile
                </Link>
                <Link className="btn btn-primary" to="/jobs/sources">
                  Sources
                </Link>
              </div>
            </div>
          )}
          <div className="action-list action-list-compact">
            {feed?.actions.content.map((a) => (
              <article
                key={a.actionId}
                className={`action-card compact ${bandClass(a.priorityBand)}`}
                data-focus-key={a.actionId}
                onClick={() => void onOpen(a)}
              >
                <div>
                  <h3 className="action-title">{a.title}</h3>
                  <div className="action-meta">
                    <span className={`badge ${a.sourceDomain === 'JOB' ? 'badge-job' : 'badge-roadmap'}`}>
                      {a.sourceDomain}
                    </span>
                    {a.pinned && <span className="badge badge-pin">PIN</span>}
                    <span className="reason-inline">{a.primaryReason || a.priorityBand}</span>
                  </div>
                </div>
                <div className="action-actions" onClick={(e) => e.stopPropagation()}>
                  <button className="btn btn-sm" type="button" onClick={(e) => void openEvidence(a, e)}>
                    为何
                  </button>
                  <button className="btn btn-sm" type="button" onClick={() => void decide(a, a.pinned ? 'UNPIN' : 'PIN')}>
                    {a.pinned ? 'Unpin' : 'Pin'}
                  </button>
                  <button className="btn btn-sm btn-primary" type="button" onClick={() => void decide(a, 'COMPLETE')}>
                    完成
                  </button>
                </div>
              </article>
            ))}
          </div>
          {(feed?.actions.hasMore || expanded) && (
            <div className="expand-row">
              {!expanded ? (
                <button className="btn btn-sm" type="button" onClick={() => void load(20)}>
                  展开更多
                </button>
              ) : (
                <button className="btn btn-sm" type="button" onClick={() => void load(3)}>
                  收起
                </button>
              )}
            </div>
          )}
        </section>
        <section className="section">
          <div className="section-head">
            <h2>新职位</h2>
            <Link className="link" to="/jobs">
              查看全部 →
            </Link>
          </div>
          <div className="new-jobs">
            {(feed?.newJobs.content || []).map((j) => (
              <Link key={j.jobId} className="new-job" to={`/jobs?jobId=${j.jobId}`}>
                <span className={`tier-dot tier-${tierClass(j.rankTier)}`} />
                <div>
                  <h3>{j.title}</h3>
                  <p>
                    {j.company} · {j.location} · {j.rankTier || 'UNRANKED'}
                  </p>
                </div>
              </Link>
            ))}
            {!feed?.newJobs.content?.length && (
              <p style={{ color: 'var(--ink-muted)' }}>暂无 NEW 职位 — 可在 Jobs 保存 URL 或等待同步。</p>
            )}
          </div>
        </section>
      </div>

      <div
        className={`overlay ${evidenceAction ? 'open' : ''}`}
        onClick={() => {
          setEvidenceAction(null)
          setEvidence(null)
        }}
      >
        <div className="drawer" onClick={(e) => e.stopPropagation()}>
          <h2>优先级解释</h2>
          <p className="sub">{evidenceAction?.title}</p>
          {evidenceLoading && <p>加载中…</p>}
          {evidence && (
            <div className="evidence">
              <p style={{ fontSize: 12, color: 'var(--ink-muted)', marginBottom: 12 }}>
                Band: {evidence.priorityBand} · {evidence.ruleVersion}
              </p>
              <ol>
                {evidence.factors.map((f) => (
                  <li key={`${f.factor}-${f.effectiveOrder}`}>
                    <strong>
                      {f.factor} (#{f.effectiveOrder})
                    </strong>
                    {f.factValue && <span> — {f.factValue}</span>}
                    {f.explanation && <div>{f.explanation}</div>}
                  </li>
                ))}
              </ol>
            </div>
          )}
          <button
            className="btn btn-sm"
            type="button"
            style={{ marginTop: 16 }}
            onClick={() => {
              setEvidenceAction(null)
              setEvidence(null)
            }}
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  )
}

function tierClass(tier?: string) {
  if (!tier) return 'none'
  const t = tier.toUpperCase()
  if (t.includes('HIGH')) return 'high'
  if (t.includes('MED')) return 'med'
  if (t.includes('LOW')) return 'low'
  return 'none'
}
