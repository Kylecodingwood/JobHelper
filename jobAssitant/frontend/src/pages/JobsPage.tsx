import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'

type JobSummary = {
  jobId: string
  title: string
  company?: string
  location?: string
  jobStatus: string
  gateStatus?: string
  rankTier?: string
  preferredSourceCode?: string
  hasPendingDuplicate?: boolean
  version: number
}

type JobMini = {
  jobId: string
  title?: string
  company?: string
  location?: string
  jobStatus?: string
}

type PendingDuplicate = {
  duplicateId: string
  status?: string
  leftJobId?: string
  rightJobId?: string
  leftTitle?: string
  rightTitle?: string
  leftJob?: JobMini
  rightJob?: JobMini
  otherJobId?: string
  otherJobTitle?: string
  otherJobCompany?: string
  signals?: unknown
}

function dupLeftId(d: PendingDuplicate) {
  return d.leftJob?.jobId || d.leftJobId || ''
}
function dupRightId(d: PendingDuplicate) {
  return d.rightJob?.jobId || d.rightJobId || ''
}
function dupLeftLabel(d: PendingDuplicate) {
  return d.leftJob?.title || d.leftTitle || dupLeftId(d).slice(0, 8) || 'left'
}
function dupRightLabel(d: PendingDuplicate) {
  return d.rightJob?.title || d.rightTitle || dupRightId(d).slice(0, 8) || 'right'
}
function dupSignalsText(signals: unknown): string {
  if (!signals) return ''
  if (Array.isArray(signals)) return signals.map(String).join(', ')
  if (typeof signals === 'object') {
    const s = signals as Record<string, unknown>
    const bits = [
      s.detectionMethod,
      s.titleSimilarity != null ? `sim=${s.titleSimilarity}` : null,
      s.companyMatch ? 'same-company' : null,
    ].filter(Boolean)
    return bits.join(' · ')
  }
  return String(signals)
}

type JobDetail = JobSummary & {
  description?: string
  canonicalApplyUrl?: string
  gateDimensions?: Array<{ dimension: string; status?: string; outcome?: string; explanation?: string }>
  rankFactors?: Array<{ factor: string; vote: string }>
  pendingDuplicates?: PendingDuplicate[]
}

type Source = {
  sourceId: string
  code: string
  enabled: boolean
  role: string
  effectiveSearchTerms: string[]
  riskNote?: string
}

type Filters = {
  status: string
  gateStatus: string
  rankTier: string
  hasPendingDuplicate: boolean
  q: string
  /** Cap band: junior (default) | mid | all */
  fit: string
}

const STATUS_OPTIONS = ['', 'NEW', 'SHORTLISTED', 'IGNORED', 'APPLIED', 'ARCHIVED']
const GATE_OPTIONS = ['', 'PASSED', 'FAILED', 'NEEDS_CONFIRMATION', 'OVERRIDDEN']
const RANK_OPTIONS = ['', 'HIGH', 'MEDIUM', 'LOW', 'UNRANKED']
const FIT_OPTIONS = [
  { value: 'junior', label: 'Junior / Grad' },
  { value: 'mid', label: 'Mid / 未分级' },
  { value: 'all', label: '全部可见' },
]

const emptyFilters = (): Filters => ({
  status: '',
  gateStatus: '',
  rankTier: '',
  hasPendingDuplicate: false,
  q: '',
  fit: 'junior',
})

export function JobsPage() {
  const [params] = useSearchParams()
  const [filters, setFilters] = useState<Filters>(emptyFilters)
  const [jobs, setJobs] = useState<JobSummary[]>([])
  const [totalJobs, setTotalJobs] = useState(0)
  const [listPage, setListPage] = useState(0)
  const [loadingMore, setLoadingMore] = useState(false)
  const PAGE_SIZE = 50
  const [selected, setSelected] = useState<JobDetail | null>(null)
  const [url, setUrl] = useState('')
  const [msg, setMsg] = useState<string | null>(null)
  const [sources, setSources] = useState<Source[]>([])
  const [syncing, setSyncing] = useState(false)
  const [duplicates, setDuplicates] = useState<PendingDuplicate[]>([])
  const [showDupPanel, setShowDupPanel] = useState(false)
  const [gateReason, setGateReason] = useState('')
  const [dupConflict, setDupConflict] = useState<{ duplicateId: string; leftJobId: string; rightJobId: string } | null>(
    null,
  )
  const [conflictForm, setConflictForm] = useState({ survivorJobId: '', resolvedJobStatus: 'SHORTLISTED' })

  const buildQuery = useCallback(
    (f: Filters, page = 0, size = PAGE_SIZE) => {
      const qs = new URLSearchParams()
      if (f.status) qs.set('status', f.status)
      if (f.gateStatus) qs.set('gateStatus', f.gateStatus)
      if (f.rankTier) qs.set('rankTier', f.rankTier)
      if (f.hasPendingDuplicate) qs.set('hasPendingDuplicate', 'true')
      if (f.q.trim()) qs.set('q', f.q.trim())
      if (f.fit && f.fit !== 'all') qs.set('fit', f.fit)
      qs.set('page', String(page))
      qs.set('size', String(size))
      return qs.toString()
    },
    [],
  )

  async function openJob(jobId: string) {
    try {
      const detail = await api.get<JobDetail>(`/jobs/${jobId}`)
      setSelected(detail)
      setGateReason('')
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Load detail failed')
    }
  }

  async function loadList(f: Filters = filters) {
    const page = await api.get<{ content: JobSummary[]; totalElements: number }>(
      `/jobs?${buildQuery(f, 0, PAGE_SIZE)}`,
    )
    setJobs(page.content || [])
    setTotalJobs(page.totalElements ?? page.content?.length ?? 0)
    setListPage(0)
    const focus = params.get('jobId')
    if (focus) {
      await openJob(focus)
    } else if (selected && !(page.content || []).some((j) => j.jobId === selected.jobId)) {
      setSelected(null)
    } else if (!(page.content || []).length) {
      setSelected(null)
    }
  }

  async function loadMore() {
    if (loadingMore || jobs.length >= totalJobs) return
    setLoadingMore(true)
    try {
      const next = listPage + 1
      const page = await api.get<{ content: JobSummary[]; totalElements: number }>(
        `/jobs?${buildQuery(filters, next, PAGE_SIZE)}`,
      )
      const batch = page.content || []
      setJobs((prev) => {
        const seen = new Set(prev.map((j) => j.jobId))
        return [...prev, ...batch.filter((j) => !seen.has(j.jobId))]
      })
      setTotalJobs(page.totalElements ?? totalJobs)
      setListPage(next)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : '加载更多失败')
    } finally {
      setLoadingMore(false)
    }
  }

  async function loadDuplicates() {
    try {
      const list = await api.get<PendingDuplicate[]>('/jobs/duplicates?status=PENDING')
      setDuplicates(list)
    } catch {
      setDuplicates([])
    }
  }

  async function loadSources() {
    try {
      const list = await api.get<Source[]>('/job-sources')
      setSources(list)
    } catch {
      /* ignore */
    }
  }

  useEffect(() => {
    void loadList()
    void loadSources()
    void loadDuplicates()
  }, [])

  useEffect(() => {
    const focus = params.get('jobId')
    if (focus) void openJob(focus)
  }, [params])

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setSelected(null)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  async function syncAllSources() {
    const adapters = sources.filter((s) => s.code === 'freehire' || s.code === 'jobspy')
    if (adapters.length === 0) {
      setMsg('未找到 freehire / jobspy 来源')
      return
    }
    setSyncing(true)
    setMsg(null)
    try {
      const parts: string[] = []
      for (const src of adapters) {
        const run = await api.post<{
          status: string
          createdCount: number
          updatedCount: number
          validCount: number
          receivedCount?: number
        }>('/job-source-runs', { sourceId: src.sourceId })
        parts.push(
          `${src.code}:${run.status} +${run.createdCount}/~${run.updatedCount} recv=${run.receivedCount ?? run.validCount}`,
        )
      }
      setMsg(`全源同步 · ${parts.join(' · ')}`)
      await loadList()
      await loadDuplicates()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Sync failed')
    } finally {
      setSyncing(false)
    }
  }

  async function saveUrl(e: FormEvent) {
    e.preventDefault()
    setMsg(null)
    try {
      const created = await api.post<JobDetail>('/jobs/manual-url', { url })
      setMsg(`已保存 ${created.jobId}`)
      setUrl('')
      await loadList()
      await openJob(created.jobId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Save failed')
    }
  }

  async function setStatus(toStatus: string) {
    if (!selected) return
    try {
      const updated = await api.patch<JobDetail>(`/jobs/${selected.jobId}/status`, {
        toStatus,
        expectedVersion: selected.version,
      })
      setSelected(updated)
      await loadList()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Status update failed')
    }
  }

  async function gateOverride(e: FormEvent) {
    e.preventDefault()
    if (!selected || !gateReason.trim()) return
    try {
      const updated = await api.post<JobDetail>(`/jobs/${selected.jobId}/gate-override`, {
        reason: gateReason.trim(),
        expectedVersion: selected.version,
      })
      setSelected(updated)
      setGateReason('')
      setMsg('Gate 已覆盖')
      await loadList()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Gate override failed')
    }
  }

  async function validityCheck() {
    if (!selected) return
    try {
      const result = await api.post<{ validityStatus?: string; message?: string }>(
        `/jobs/${selected.jobId}/validity/check`,
      )
      setMsg(`有效性检查完成${result.validityStatus ? `: ${result.validityStatus}` : ''}`)
      const updated = await api.get<JobDetail>(`/jobs/${selected.jobId}`)
      setSelected(updated)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Validity check failed')
    }
  }

  async function confirmDuplicate(duplicateId: string, body?: { survivorJobId: string; resolvedJobStatus: string }) {
    try {
      await api.post(`/jobs/duplicates/${duplicateId}/confirm`, body ?? {})
      setDupConflict(null)
      setMsg('重复已确认合并')
      await loadDuplicates()
      await loadList()
      if (selected) await openJob(selected.jobId)
    } catch (err) {
      if (err instanceof ApiError && err.code === 'DUPLICATE_MERGE_CONFLICT') {
        const dup = duplicates.find((d) => d.duplicateId === duplicateId)
        const leftJobId = dup ? dupLeftId(dup) : ''
        const rightJobId = dup ? dupRightId(dup) : ''
        setDupConflict({
          duplicateId,
          leftJobId,
          rightJobId,
        })
        setConflictForm({ survivorJobId: leftJobId, resolvedJobStatus: 'SHORTLISTED' })
      } else {
        setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Confirm failed')
      }
    }
  }

  async function rejectDuplicate(duplicateId: string) {
    try {
      await api.post(`/jobs/duplicates/${duplicateId}/reject`)
      setMsg('已驳回重复')
      await loadDuplicates()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Reject failed')
    }
  }

  function applyFilters() {
    void loadList(filters)
  }

  function dimStatus(d: { status?: string; outcome?: string }) {
    return d.status || d.outcome || '—'
  }

  return (
    <div className="page split-page jobs-feed">
      <div className="topbar">
        <h1>Jobs</h1>
        <span className="count" style={{ marginLeft: 8 }}>
          {jobs.length}/{totalJobs}
          {filters.fit === 'junior' ? ' Junior/Grad' : filters.fit === 'mid' ? ' Mid/未分级' : ' 可见'}
        </span>
        <div className="spacer" />
        <Link className="btn btn-sm" to="/jobs/sources">
          Sources
        </Link>
        <button className="btn btn-sm btn-primary" type="button" disabled={syncing} onClick={() => void syncAllSources()}>
          {syncing ? '同步中…' : '同步全源'}
        </button>
        <button className="btn btn-sm" type="button" onClick={() => void loadList()}>
          刷新
        </button>
      </div>
      <div className="jobs-shell">
      {sources.length > 0 && (
        <div className="filterbar">
          {sources
            .filter((s) => s.code !== 'manual_url')
            .map((s) => (
              <span key={s.sourceId} className="chip on">
                {s.code}: {(s.effectiveSearchTerms || []).slice(0, 2).join(' · ') || '—'}
              </span>
            ))}
        </div>
      )}
      <div className="filterbar">
        <select
          value={filters.fit}
          onChange={(e) => {
            const fit = e.target.value
            setFilters((f) => ({ ...f, fit }))
            void loadList({ ...filters, fit })
          }}
          style={{ padding: '4px 8px', borderRadius: 6, border: '1px solid var(--line)' }}
          title="Cap 分层：默认只看 Junior/Grad"
        >
          {FIT_OPTIONS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
        <select
          value={filters.status}
          onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}
          style={{ padding: '4px 8px', borderRadius: 6, border: '1px solid var(--line)' }}
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s || 'all'} value={s}>
              {s || '全部 status'}
            </option>
          ))}
        </select>
        <select
          value={filters.gateStatus}
          onChange={(e) => setFilters((f) => ({ ...f, gateStatus: e.target.value }))}
          style={{ padding: '4px 8px', borderRadius: 6, border: '1px solid var(--line)' }}
        >
          {GATE_OPTIONS.map((s) => (
            <option key={s || 'all-gate'} value={s}>
              {s || '全部 gate'}
            </option>
          ))}
        </select>
        <select
          value={filters.rankTier}
          onChange={(e) => setFilters((f) => ({ ...f, rankTier: e.target.value }))}
          style={{ padding: '4px 8px', borderRadius: 6, border: '1px solid var(--line)' }}
        >
          {RANK_OPTIONS.map((s) => (
            <option key={s || 'all-rank'} value={s}>
              {s || '全部 rank'}
            </option>
          ))}
        </select>
        <label className="chip" style={{ cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={filters.hasPendingDuplicate}
            onChange={(e) => setFilters((f) => ({ ...f, hasPendingDuplicate: e.target.checked }))}
          />{' '}
          可能重复
        </label>
        <input
          value={filters.q}
          onChange={(e) => setFilters((f) => ({ ...f, q: e.target.value }))}
          placeholder="搜索标题/公司…"
          style={{ padding: '4px 10px', borderRadius: 6, border: '1px solid var(--line)', minWidth: 140 }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') applyFilters()
          }}
        />
        <button className="btn btn-sm" type="button" onClick={applyFilters}>
          筛选
        </button>
        {duplicates.length > 0 && (
          <button className="btn btn-sm" type="button" onClick={() => setShowDupPanel((v) => !v)}>
            重复 ({duplicates.length})
          </button>
        )}
      </div>
      <div className="filterbar">
        <form className="row" onSubmit={(e) => void saveUrl(e)} style={{ flex: 1, gap: 8, display: 'flex' }}>
          <input
            style={{ flex: 1, minWidth: 180, padding: '6px 10px', border: '1px solid var(--line)', borderRadius: 6 }}
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="粘贴职位 URL…"
            required
          />
          <button className="btn btn-sm btn-primary" type="submit">
            保存 URL
          </button>
        </form>
      </div>
      {msg && (
        <div className="banner" style={{ margin: '8px 16px' }}>
          {msg}
        </div>
      )}
      {showDupPanel && duplicates.length > 0 && (
        <div className="ev-section" style={{ margin: '8px 16px' }}>
          <h3>待处理重复</h3>
          {duplicates.map((d) => (
            <div key={d.duplicateId} className="dim" style={{ gridTemplateColumns: '1fr auto auto auto' }}>
              <span>
                {dupLeftLabel(d)} ↔ {dupRightLabel(d)}
                {dupSignalsText(d.signals) ? ` · ${dupSignalsText(d.signals)}` : ''}
              </span>
              <button className="btn btn-sm btn-primary" type="button" onClick={() => void confirmDuplicate(d.duplicateId)}>
                确认
              </button>
              <button className="btn btn-sm" type="button" onClick={() => void rejectDuplicate(d.duplicateId)}>
                驳回
              </button>
            </div>
          ))}
        </div>
      )}
      {dupConflict && (
        <div className="modal-center open">
          <div className="modal">
            <h2>合并冲突</h2>
            <p>请选择保留的职位及合并后状态</p>
            <div className="field">
              <label>Survivor Job</label>
              <select
                value={conflictForm.survivorJobId}
                onChange={(e) => setConflictForm((f) => ({ ...f, survivorJobId: e.target.value }))}
              >
                <option value={dupConflict.leftJobId}>{dupConflict.leftJobId.slice(0, 8)}… (left)</option>
                <option value={dupConflict.rightJobId}>{dupConflict.rightJobId.slice(0, 8)}… (right)</option>
              </select>
            </div>
            <div className="field">
              <label>Resolved Status</label>
              <select
                value={conflictForm.resolvedJobStatus}
                onChange={(e) => setConflictForm((f) => ({ ...f, resolvedJobStatus: e.target.value }))}
              >
                {['SHORTLISTED', 'IGNORED', 'APPLIED', 'ARCHIVED'].map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
            <div className="modal-actions">
              <button className="btn btn-sm" type="button" onClick={() => setDupConflict(null)}>
                取消
              </button>
              <button
                className="btn btn-sm btn-primary"
                type="button"
                onClick={() =>
                  void confirmDuplicate(dupConflict.duplicateId, {
                    survivorJobId: conflictForm.survivorJobId,
                    resolvedJobStatus: conflictForm.resolvedJobStatus,
                  })
                }
              >
                重试合并
              </button>
            </div>
          </div>
        </div>
      )}
      <div className={`split ${selected ? 'has-detail' : ''}`}>
        <aside className="list-pane">
          <div className="job-stack-head" style={{ padding: '8px 12px', fontSize: 12, color: 'var(--ink-muted)' }}>
            已加载 {jobs.length} / 共 {totalJobs}
            {filters.fit === 'junior' ? '（Junior/Grad）' : filters.fit === 'mid' ? '（Mid/未分级）' : '（全部可见）'}
          </div>
          <div className="job-stack">
            {jobs.map((j) => (
              <article
                key={j.jobId}
                className={`job-card ${selected?.jobId === j.jobId ? 'selected' : ''} tier-border-${tierClass(j.rankTier)}`}
                onClick={() => void openJob(j.jobId)}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void openJob(j.jobId)
                }}
              >
                <div className="job-card-main">
                  <h3 className="job-card-title">
                    {j.title}
                    {j.hasPendingDuplicate ? ' ⚠' : ''}
                  </h3>
                  <div className="job-card-meta">
                    <span className={`status-chip st-${statusClass(j.jobStatus)}`}>{j.jobStatus}</span>
                    {j.rankTier && <span className="badge badge-job">{j.rankTier}</span>}
                    {j.gateStatus && <span className="reason-inline">Gate {j.gateStatus}</span>}
                  </div>
                  <p className="job-card-sub">
                    {[j.company, j.location, j.preferredSourceCode].filter(Boolean).join(' · ') || '—'}
                  </p>
                </div>
              </article>
            ))}
            {!jobs.length && <p className="job-stack-empty">暂无职位</p>}
            {jobs.length > 0 && jobs.length < totalJobs && (
              <div style={{ padding: '12px', textAlign: 'center' }}>
                <button className="btn btn-sm" type="button" disabled={loadingMore} onClick={() => void loadMore()}>
                  {loadingMore ? '加载中…' : `加载更多（剩余 ${totalJobs - jobs.length}）`}
                </button>
              </div>
            )}
          </div>
        </aside>
        {selected && (
          <main className="detail-pane" aria-label="职位详情">
            <div className="detail-pane-head">
              <h2>{selected.title}</h2>
              <button className="btn btn-sm" type="button" onClick={() => setSelected(null)}>
                关闭
              </button>
            </div>
            <p className="company">
              {selected.company} · {selected.location} · {selected.preferredSourceCode} · Gate{' '}
              {selected.gateStatus} · Rank {selected.rankTier}
            </p>
            <div className="status-bar">
              <button className="btn btn-sm btn-primary" type="button" onClick={() => void setStatus('SHORTLISTED')}>
                SHORTLISTED
              </button>
              <button className="btn btn-sm" type="button" onClick={() => void setStatus('IGNORED')}>
                IGNORED
              </button>
              <button className="btn btn-sm" type="button" onClick={() => void setStatus('APPLIED')}>
                APPLIED
              </button>
              <button className="btn btn-sm" type="button" onClick={() => void setStatus('ARCHIVED')}>
                ARCHIVED
              </button>
              <button className="btn btn-sm" type="button" onClick={() => void validityCheck()}>
                有效性检查
              </button>
            </div>
            <form className="ev-section" onSubmit={(e) => void gateOverride(e)}>
              <h3>Gate 覆盖</h3>
              <div className="field">
                <input
                  value={gateReason}
                  onChange={(e) => setGateReason(e.target.value)}
                  placeholder="覆盖原因（必填）…"
                  required
                />
              </div>
              <button className="btn btn-sm btn-primary" type="submit" disabled={!gateReason.trim()}>
                提交覆盖
              </button>
            </form>
            {!!selected.pendingDuplicates?.length && (
              <div className="ev-section">
                <h3>疑似重复 ⚠</h3>
                {selected.pendingDuplicates.map((d) => {
                  const otherTitle =
                    d.otherJobTitle ||
                    d.leftTitle ||
                    d.rightTitle ||
                    (d.otherJobId || d.leftJobId || d.rightJobId || '').slice(0, 8) ||
                    '相关职位'
                  const otherCompany = d.otherJobCompany ? ` · ${d.otherJobCompany}` : ''
                  const otherId = d.otherJobId || d.rightJobId || d.leftJobId
                  return (
                    <div key={d.duplicateId} className="dim" style={{ gridTemplateColumns: '1fr auto' }}>
                      <span>
                        {otherTitle}
                        {otherCompany}
                      </span>
                      {otherId ? (
                        <button
                          className="btn btn-sm"
                          type="button"
                          onClick={() => void openJob(otherId)}
                        >
                          打开对照
                        </button>
                      ) : (
                        <span className="warn-t">{d.status || 'PENDING'}</span>
                      )}
                    </div>
                  )
                })}
              </div>
            )}
            {!!selected.gateDimensions?.length && (
              <div className="ev-section">
                <h3>Gate</h3>
                {selected.gateDimensions.map((d) => (
                  <div key={d.dimension} className="dim">
                    <span>{d.dimension}</span>
                    <span>{d.explanation}</span>
                    <span
                      className={
                        dimStatus(d) === 'PASS' || dimStatus(d) === 'PASSED'
                          ? 'ok'
                          : dimStatus(d) === 'FAIL' || dimStatus(d) === 'FAILED'
                            ? 'fail'
                            : 'warn-t'
                      }
                    >
                      {dimStatus(d)}
                    </span>
                  </div>
                ))}
              </div>
            )}
            {!!selected.rankFactors?.length && (
              <div className="ev-section">
                <h3>Rank</h3>
                {selected.rankFactors.map((f) => (
                  <div key={f.factor} className="dim">
                    <span>{f.factor}</span>
                    <span />
                    <span className={f.vote === 'POSITIVE' ? 'pos' : f.vote === 'NEGATIVE' ? 'neg' : 'neu'}>
                      {f.vote}
                    </span>
                  </div>
                ))}
              </div>
            )}
            {selected.canonicalApplyUrl && (
              <p>
                <a className="btn btn-sm btn-primary" href={selected.canonicalApplyUrl} target="_blank" rel="noreferrer">
                  打开原职位链接
                </a>
              </p>
            )}
            {!selected.canonicalApplyUrl && (
              <p style={{ color: 'var(--ink-muted)', fontSize: 12 }}>该职位暂无外部申请链接</p>
            )}
            <div className="ev-section">
              <h3>Description</h3>
              <pre style={{ whiteSpace: 'pre-wrap', margin: 0, font: 'inherit' }}>
                {selected.description || '无描述'}
              </pre>
            </div>
          </main>
        )}
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

function statusClass(status: string) {
  const s = status.toUpperCase()
  if (s === 'NEW') return 'new'
  if (s === 'APPLIED' || s === 'SHORTLISTED') return s === 'APPLIED' ? 'applied' : 'short'
  return 'ignore'
}
