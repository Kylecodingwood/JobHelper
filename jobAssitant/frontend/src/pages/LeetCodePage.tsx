import { type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { api, ApiError } from '../api/client'

// Topic grouping — mirrors LeetCode Hot 100 categories.
// We scan ALL tags of a problem and pick the highest-priority match.
const TOPIC_PRIORITY = [
  'Sliding Window', 'Two Pointers', 'Binary Search', 'Divide and Conquer',
  'Stack', 'Monotonic Stack', 'Queue', 'Heap', 'Priority Queue',
  'Linked List',
  'Tree', 'Binary Search Tree', 'Trie',
  'Graph', 'Topological Sort',
  'DFS', 'BFS', 'Backtracking',
  'Dynamic Programming',
  'Bit Manipulation', 'Math',
  'Hash Table', 'String', 'Array',
]

const TOPIC_LABELS: Record<string, string> = {
  'Sliding Window': 'Sliding Window',
  'Two Pointers': 'Two Pointers',
  'Binary Search': 'Binary Search',
  'Divide and Conquer': 'Divide & Conquer',
  'Stack': 'Stack',
  'Monotonic Stack': 'Stack',
  'Queue': 'Queue',
  'Heap': 'Heap / Priority Queue',
  'Priority Queue': 'Heap / Priority Queue',
  'Linked List': 'Linked List',
  'Tree': 'Trees',
  'Binary Search Tree': 'Trees',
  'Trie': 'Trie',
  'Graph': 'Graphs',
  'Topological Sort': 'Graphs',
  'DFS': 'DFS / Backtracking',
  'BFS': 'BFS',
  'Backtracking': 'DFS / Backtracking',
  'Dynamic Programming': 'Dynamic Programming',
  'Bit Manipulation': 'Bit Manipulation',
  'Math': 'Math',
  'Hash Table': 'Hash Table',
  'String': 'Array & String',
  'Array': 'Array & String',
}

function resolveCategory(tags: string[]): string {
  for (const p of TOPIC_PRIORITY) {
    if (tags.includes(p)) return TOPIC_LABELS[p] ?? p
  }
  return 'Other'
}

function ProblemItem({ p, selectedId, masteryLabel, openProblem }: {
  p: Problem
  selectedId: string | null
  masteryLabel: (m?: string | null) => string
  openProblem: (id: string) => void
}) {
  return (
    <button
      type="button"
      className={`leetcode-item${p.problemId === selectedId ? ' active' : ''}`}
      onClick={() => openProblem(p.problemId)}
    >
      <span className="leetcode-item-main">
        <span className="leetcode-num">#{p.problemNumber}</span>
        <span className="leetcode-title">{p.title}</span>
      </span>
      <span className="leetcode-item-meta">
        <span className={`diff diff-${p.difficulty.toLowerCase()}`}>{p.difficulty}</span>
        <span className={`mastery mastery-${p.mastery || 'none'}`}>{masteryLabel(p.mastery)}</span>
        {p.hasReview && p.updatedAt && (
          <span style={{ fontSize: 11, color: 'var(--ink-muted)', marginLeft: 'auto' }}>
            {new Date(p.updatedAt).toLocaleDateString()}
          </span>
        )}
      </span>
    </button>
  )
}

function useVoiceInput(onResult: (field: string, text: string) => void) {
  const [activeField, setActiveField] = useState<string | null>(null)
  const recRef = useRef<any>(null)

  function start(field: string) {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
    if (!SR) { alert('Your browser does not support voice input. Try Chrome or Edge.'); return }
    stop()
    const rec = new SR()
    rec.continuous = true
    rec.interimResults = false
    rec.lang = 'en-US'
    rec.onresult = (e: any) => {
      const text = Array.from(e.results as any[])
        .slice(e.resultIndex)
        .map((r: any) => r[0].transcript as string)
        .join('')
      onResult(field, text)
    }
    rec.onend = () => setActiveField(null)
    rec.onerror = () => setActiveField(null)
    rec.start()
    recRef.current = rec
    setActiveField(field)
  }

  function stop() {
    recRef.current?.stop()
    recRef.current = null
    setActiveField(null)
  }

  return { activeField, start, stop }
}

function MicButton({ field, activeField, start, stop }: {
  field: string
  activeField: string | null
  start: (f: string) => void
  stop: () => void
}) {
  const active = activeField === field
  return (
    <button
      type="button"
      title={active ? 'Stop recording' : 'Voice input'}
      onClick={() => active ? stop() : start(field)}
      style={{
        position: 'absolute', bottom: 8, right: 8,
        background: active ? '#dc2626' : 'var(--primary, #1a4c38)',
        color: '#fff', border: 'none', borderRadius: 6,
        width: 28, height: 28, fontSize: 14, cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        opacity: activeField && !active ? 0.4 : 1,
      }}
    >
      {active ? '⏹' : '🎤'}
    </button>
  )
}

type Mastery = 'confident' | 'partial' | 'weak'

type Problem = {
  problemId: string
  problemNumber: number
  title: string
  difficulty: string
  tags: string[]
  url: string
  hasReview: boolean
  mastery?: Mastery | null
  nextReviewAt?: string | null
  updatedAt?: string | null
}

type Review = {
  reviewId: string
  confusion: string
  approach?: string | null
  keyCode?: string | null
  mastery: Mastery
  nextReviewAt?: string | null
}

type ProblemDetail = Problem & {
  review?: Review
  statementHtml?: string | null
  examples?: string | null
  hasContent?: boolean
  contentFetchedAt?: string | null
}

const emptyDraft = {
  confusion: '',
  approach: '',
  keyCode: '',
  mastery: 'partial' as Mastery,
  nextReviewAt: '',
}

export function LeetCodePage() {
  const [problems, setProblems] = useState<Problem[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detail, setDetail] = useState<ProblemDetail | null>(null)
  const [draft, setDraft] = useState(emptyDraft)
  const [q, setQ] = useState('')
  const [difficulty, setDifficulty] = useState('all')
  const [mastery, setMastery] = useState('all')
  const [review, setReview] = useState('all')
  const [msg, setMsg] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [groupByTopic, setGroupByTopic] = useState(false)
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set())

  const groupedProblems = useMemo(() => {
    if (!groupByTopic) return null
    const map = new Map<string, Problem[]>()
    for (const p of problems) {
      const cat = resolveCategory(p.tags)
      if (!map.has(cat)) map.set(cat, [])
      map.get(cat)!.push(p)
    }
    return map
  }, [problems, groupByTopic])

  function toggleCollapse(cat: string) {
    setCollapsed((prev) => {
      const next = new Set(prev)
      next.has(cat) ? next.delete(cat) : next.add(cat)
      return next
    })
  }

  const { activeField, start, stop } = useVoiceInput((field, text) => {
    setDraft((d) => ({ ...d, [field]: (d[field as keyof typeof d] as string) + (d[field as keyof typeof d] ? ' ' : '') + text }))
  })

  const selected = useMemo(
    () => problems.find((p) => p.problemId === selectedId) || null,
    [problems, selectedId],
  )

  const exampleBlocks = useMemo(() => {
    const raw = detail?.examples?.trim()
    if (!raw) return []
    return raw
      .split(/\n\s*\n/)
      .map((b) => b.trim())
      .filter(Boolean)
  }, [detail?.examples])

  async function loadList() {
    try {
      const params = new URLSearchParams()
      if (q.trim()) params.set('q', q.trim())
      if (difficulty !== 'all') params.set('difficulty', difficulty)
      if (mastery !== 'all') params.set('mastery', mastery)
      if (review !== 'all') params.set('review', review)
      const qs = params.toString()
      const data = await api.get<{ problems: Problem[]; total: number }>(
        `/leetcode/problems${qs ? `?${qs}` : ''}`,
      )
      setProblems(data.problems || [])
      setMsg(null)
      if (selectedId && !(data.problems || []).some((p) => p.problemId === selectedId)) {
        setSelectedId(null)
        setDetail(null)
      }
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Failed to load problems')
    }
  }

  async function openProblem(problemId: string) {
    setSelectedId(problemId)
    setLoadingDetail(true)
    try {
      const data = await api.get<ProblemDetail>(`/leetcode/problems/${problemId}`)
      setDetail(data)
      const r = data.review
      setDraft({
        confusion: r?.confusion || '',
        approach: r?.approach || '',
        keyCode: r?.keyCode || '',
        mastery: r?.mastery || 'partial',
        nextReviewAt: r?.nextReviewAt ? String(r.nextReviewAt).slice(0, 10) : '',
      })
      setMsg(data.hasContent ? null : 'Problem statement not cached yet — try Refresh statement')
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Failed to load problem')
    } finally {
      setLoadingDetail(false)
    }
  }

  async function refreshStatement() {
    if (!selectedId) return
    setLoadingDetail(true)
    try {
      const data = await api.post<ProblemDetail>(`/leetcode/problems/${selectedId}/content/refresh`)
      setDetail(data)
      setMsg(data.hasContent ? 'Statement saved to database' : 'Fetch failed — check network / LeetCode')
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Refresh failed')
    } finally {
      setLoadingDetail(false)
    }
  }

  useEffect(() => {
    void loadList()
  }, [])

  async function onSave(e: FormEvent) {
    e.preventDefault()
    if (!selectedId || !draft.confusion.trim()) return
    setSaving(true)
    try {
      const body = {
        confusion: draft.confusion.trim(),
        approach: draft.approach.trim() || null,
        keyCode: draft.keyCode.trim() || null,
        mastery: draft.mastery,
        nextReviewAt: draft.nextReviewAt || null,
      }
      const data = await api.put<ProblemDetail>(`/leetcode/problems/${selectedId}/review`, body)
      setDetail(data)
      setMsg('Review saved')
      await loadList()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  async function onDelete() {
    if (!selectedId || !detail?.hasReview) return
    if (!confirm('Delete this review?')) return
    try {
      await api.delete(`/leetcode/problems/${selectedId}/review`)
      setDraft(emptyDraft)
      setMsg('Review deleted')
      await openProblem(selectedId)
      await loadList()
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete failed')
    }
  }

  function masteryLabel(m?: string | null) {
    if (m === 'confident') return 'Confident'
    if (m === 'partial') return 'Partial'
    if (m === 'weak') return 'Weak'
    return '—'
  }

  return (
    <div className="page leetcode-page">
      <div className="topbar">
        <h1>LeetCode Review</h1>
        <span style={{ fontSize: 12, color: 'var(--ink-muted)' }}>Hot 100 · second pass notes</span>
        <div className="spacer" />
        <button className="btn btn-sm" type="button" onClick={() => void loadList()}>
          Refresh
        </button>
      </div>
      {msg && <div className="banner">{msg}</div>}

      <div className="leetcode-filters">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') void loadList()
          }}
          placeholder="Search number / title / tag…"
        />
        <select value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
          <option value="all">All difficulties</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
        <select value={mastery} onChange={(e) => setMastery(e.target.value)}>
          <option value="all">All mastery</option>
          <option value="confident">Confident</option>
          <option value="partial">Partial</option>
          <option value="weak">Weak</option>
        </select>
        <select value={review} onChange={(e) => setReview(e.target.value)}>
          <option value="all">All</option>
          <option value="reviewed">Reviewed</option>
          <option value="unreviewed">Unreviewed</option>
        </select>
        <button className="btn btn-sm btn-primary" type="button" onClick={() => void loadList()}>
          Filter
        </button>
      </div>

      <div className="leetcode-split">
        <aside className="leetcode-list-pane">
          <div className="leetcode-list-head">
            {problems.length} problems
            <button
              type="button"
              className={`btn btn-sm${groupByTopic ? ' btn-primary' : ''}`}
              style={{ marginLeft: 'auto', padding: '2px 8px', fontSize: 11 }}
              onClick={() => setGroupByTopic((v) => !v)}
            >
              Topics
            </button>
          </div>
          <ul className="leetcode-list">
            {groupedProblems
              ? Array.from(groupedProblems.entries()).map(([cat, catProblems]) => (
                  <li key={cat} className="leetcode-topic-group">
                    <button
                      type="button"
                      className="leetcode-topic-header"
                      onClick={() => toggleCollapse(cat)}
                    >
                      <span>{cat}</span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span style={{ fontSize: 11, color: 'var(--ink-muted)' }}>{catProblems.length}</span>
                        <span style={{ fontSize: 10 }}>{collapsed.has(cat) ? '▶' : '▼'}</span>
                      </span>
                    </button>
                    {!collapsed.has(cat) && catProblems.map((p) => (
                      <ProblemItem key={p.problemId} p={p} selectedId={selectedId} masteryLabel={masteryLabel} openProblem={openProblem} />
                    ))}
                  </li>
                ))
              : problems.map((p) => (
                  <li key={p.problemId}>
                    <ProblemItem p={p} selectedId={selectedId} masteryLabel={masteryLabel} openProblem={openProblem} />
                  </li>
                ))}
            {!problems.length && <li className="empty-hint">No problems match filters</li>}
          </ul>
        </aside>

        <section className="leetcode-review-pane">
          {!selected || !detail ? (
            <p className="empty-hint">Select a Hot 100 problem to write your review.</p>
          ) : (
            <>
              <div className="leetcode-review-head">
                <div>
                  <h2>
                    #{detail.problemNumber} {detail.title}
                  </h2>
                  <div className="leetcode-tags">
                    <span className={`diff diff-${detail.difficulty.toLowerCase()}`}>{detail.difficulty}</span>
                    {(detail.tags || []).map((t) => (
                      <span key={t} className="tag">
                        {t}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="leetcode-head-actions">
                  <button className="btn btn-sm" type="button" onClick={() => void refreshStatement()} disabled={loadingDetail}>
                    Refresh statement
                  </button>
                  <a className="btn btn-sm" href={detail.url} target="_blank" rel="noreferrer">
                    Open on LeetCode
                  </a>
                </div>
              </div>

              {loadingDetail && <p className="empty-hint">Loading statement…</p>}

              {detail.hasContent && (
                <div className="leetcode-content">
                  <h3>Problem</h3>
                  <div
                    className="leetcode-statement"
                    dangerouslySetInnerHTML={{ __html: detail.statementHtml || '' }}
                  />
                  {exampleBlocks.length > 0 && (
                    <div className="leetcode-examples">
                      <h3>Examples (raw testcases)</h3>
                      {exampleBlocks.map((block, i) => (
                        <pre key={i} className="leetcode-example-block">
                          {`Example ${i + 1}\n${block}`}
                        </pre>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <form className="leetcode-form" onSubmit={(e) => void onSave(e)}>
                <label>
                  Confusion / questions
                  <div style={{ position: 'relative', width: '100%' }}>
                    <textarea
                      required
                      rows={4}
                      value={draft.confusion}
                      onChange={(e) => setDraft({ ...draft, confusion: e.target.value })}
                      placeholder="What confused you on this pass?"
                      style={{ width: '100%', boxSizing: 'border-box', paddingRight: 40 }}
                    />
                    <MicButton field="confusion" activeField={activeField} start={start} stop={stop} />
                  </div>
                </label>
                <label>
                  Approach
                  <div style={{ position: 'relative', width: '100%' }}>
                    <textarea
                      rows={4}
                      value={draft.approach}
                      onChange={(e) => setDraft({ ...draft, approach: e.target.value })}
                      placeholder="Core idea, edge cases, complexity…"
                      style={{ width: '100%', boxSizing: 'border-box', paddingRight: 40 }}
                    />
                    <MicButton field="approach" activeField={activeField} start={start} stop={stop} />
                  </div>
                </label>
                <label>
                  Key code
                  <textarea
                    className="code-area"
                    rows={8}
                    value={draft.keyCode}
                    onChange={(e) => setDraft({ ...draft, keyCode: e.target.value })}
                    placeholder="Paste the snippet you want to remember…"
                  />
                </label>
                <div className="leetcode-form-row">
                  <label>
                    Mastery
                    <select
                      value={draft.mastery}
                      onChange={(e) => setDraft({ ...draft, mastery: e.target.value as Mastery })}
                    >
                      <option value="confident">Confident</option>
                      <option value="partial">Partial</option>
                      <option value="weak">Weak</option>
                    </select>
                  </label>
                  <label>
                    Next review
                    <input
                      type="date"
                      value={draft.nextReviewAt}
                      onChange={(e) => setDraft({ ...draft, nextReviewAt: e.target.value })}
                    />
                  </label>
                </div>
                <div className="leetcode-form-actions">
                  <button className="btn btn-primary" type="submit" disabled={saving}>
                    {saving ? 'Saving…' : 'Save review'}
                  </button>
                  {detail.hasReview && (
                    <button className="btn" type="button" onClick={() => void onDelete()}>
                      Delete review
                    </button>
                  )}
                </div>
              </form>
            </>
          )}
        </section>
      </div>
    </div>
  )
}
