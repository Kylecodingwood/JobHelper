import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../api/client'

type Source = {
  sourceId: string
  code: string
  enabled: boolean
  role: string
  searchTerms?: string[]
  riskNote?: string
}

type Run = {
  runId: string
  sourceId: string
  status: string
  createdCount: number
  updatedCount: number
  validCount: number
  failedCount: number
  diagnostics?: string[] | string
  parametersSnapshot?: string
  startedAt?: string
  endedAt?: string
}

export function JobsSourcesPage() {
  const [params] = useSearchParams()
  const [sources, setSources] = useState<Source[]>([])
  const [runs, setRuns] = useState<Run[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [msg, setMsg] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [selectedRun, setSelectedRun] = useState<Run | null>(null)

  const selected = useMemo(
    () => sources.find((s) => s.sourceId === selectedId) || sources.find((s) => s.code === 'freehire') || sources[0],
    [sources, selectedId],
  )

  async function load() {
    try {
      const [s, r, terms] = await Promise.all([
        api.get<Source[]>('/job-sources'),
        api.get<{ content: Run[] }>('/job-source-runs'),
        api.get<{ searchTerms: string[] }>('/job-sources/search-terms'),
      ])
      setSources(s.filter((x) => x.code !== 'manual_url'))
      setRuns(r.content || [])
      setQuery((terms.searchTerms || []).join(', '))
      const focus = params.get('sourceRunId')
      if (focus) {
        const detail = await api.get<Run>(`/job-source-runs/${focus}`)
        setSelectedRun(detail)
      }
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Load failed')
    }
  }

  useEffect(() => {
    void load()
  }, [])

  useEffect(() => {
    if (selected) {
      setSelectedId(selected.sourceId)
    }
  }, [selected?.sourceId])

  async function saveTerms(e?: FormEvent) {
    e?.preventDefault()
    const terms = query
      .split(/[,\n]/)
      .map((t) => t.trim())
      .filter(Boolean)
    setBusy(true)
    setMsg(null)
    try {
      await api.put<{ searchTerms: string[] }>('/job-sources/search-terms', { searchTerms: terms })
      setMsg(`已保存全局搜索词（${terms.length}），适用于所有数据源`)
      await load()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Save failed')
    } finally {
      setBusy(false)
    }
  }

  async function sync() {
    if (!selected) return
    setBusy(true)
    setMsg(null)
    try {
      const run = await api.post<Run>('/job-source-runs', { sourceId: selected.sourceId })
      setMsg(`${selected.code} → ${run.status}`)
      for (let i = 0; i < 30; i++) {
        const detail = await api.get<Run>(`/job-source-runs/${run.runId}`)
        setSelectedRun(detail)
        if (!['RUNNING', 'QUEUED'].includes(detail.status)) break
        await new Promise((r) => setTimeout(r, 2000))
      }
      await load()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Sync failed')
    } finally {
      setBusy(false)
    }
  }

  async function syncAll() {
    const adapters = sources.filter((s) => s.code === 'freehire' || s.code === 'jobspy')
    if (adapters.length === 0) return
    setBusy(true)
    setMsg(null)
    try {
      const parts: string[] = []
      for (const src of adapters) {
        const run = await api.post<Run>('/job-source-runs', { sourceId: src.sourceId })
        for (let i = 0; i < 40; i++) {
          const detail = await api.get<Run>(`/job-source-runs/${run.runId}`)
          setSelectedRun(detail)
          if (!['RUNNING', 'QUEUED'].includes(detail.status)) {
            parts.push(`${src.code}:${detail.status}`)
            break
          }
          await new Promise((r) => setTimeout(r, 2000))
        }
      }
      setMsg(`全源同步 · ${parts.join(' · ')}`)
      await load()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Sync failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page sources-vertical">
      <div className="topbar">
        <h1>Job Sources</h1>
        <div className="spacer" />
        <button className="btn btn-sm" type="button" onClick={() => void load()}>
          刷新
        </button>
      </div>
      {msg && <div className="banner">{msg}</div>}

      <form className="linkedin-search" onSubmit={(e) => void saveTerms(e)}>
        <label>
          全局搜索词
          <span style={{ color: 'var(--ink-muted)', fontWeight: 400 }}>
            （FreeHire / JobSpy 共用；首次同步前必填并保存）
          </span>
        </label>
        <div className="linkedin-search-row">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="例如：software engineer, junior software engineer, graduate software developer"
          />
          <button className="btn btn-primary" type="submit" disabled={busy}>
            保存
          </button>
          <button className="btn" type="button" disabled={busy || !selected} onClick={() => void sync()}>
            {busy ? '同步中…' : `同步 ${selected?.code || ''}`}
          </button>
          <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void syncAll()}>
            {busy ? '同步中…' : '同步全源'}
          </button>
        </div>
      </form>

      <section className="section">
        <h2>数据源</h2>
        <div className="source-stack">
          {sources.map((s) => (
            <article
              key={s.sourceId}
              className={`source-card ${selected?.sourceId === s.sourceId ? 'selected' : ''}`}
              onClick={() => setSelectedId(s.sourceId)}
            >
              <div className="section-head">
                <strong>{s.code}</strong>
                <button
                  className="btn btn-sm"
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation()
                    void api.patch(`/job-sources/${s.sourceId}`, { enabled: !s.enabled }).then(load)
                  }}
                >
                  {s.enabled ? '已启用' : '已停用'}
                </button>
              </div>
              <p style={{ margin: 0, color: 'var(--ink-muted)', fontSize: 13 }}>
                {(s.searchTerms || []).join(' · ') || '尚未填写全局搜索词'}
              </p>
              {s.riskNote && <p style={{ margin: '6px 0 0', fontSize: 12 }}>{s.riskNote}</p>}
            </article>
          ))}
        </div>
      </section>

      <section className="section">
        <h2>运行记录</h2>
        <div className="source-stack">
          {runs.map((r) => (
            <button
              key={r.runId}
              type="button"
              className={`source-card run-row ${selectedRun?.runId === r.runId ? 'selected' : ''}`}
              onClick={() => void api.get<Run>(`/job-source-runs/${r.runId}`).then(setSelectedRun)}
            >
              <strong>{r.status}</strong>
              <span>
                created {r.createdCount} · valid {r.validCount} · failed {r.failedCount}
              </span>
            </button>
          ))}
        </div>
        {selectedRun && (
          <pre className="run-detail">
            {JSON.stringify(
              {
                status: selectedRun.status,
                parametersSnapshot: selectedRun.parametersSnapshot,
                diagnostics: selectedRun.diagnostics,
              },
              null,
              2,
            )}
          </pre>
        )}
      </section>
    </div>
  )
}
