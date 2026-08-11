import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { api, ApiError } from '../api/client'

type Tab = 'questions' | 'evidence' | 'practice'

type Question = {
  questionId: string
  text: string
  competencyTopic: string
  sourceType: string
  visibility: string
}

type EvidenceRevision = {
  revisionId: string
  situation: string
  task: string
  action: string
  result: string
  competencyTags: string[]
}

type Evidence = {
  evidenceId: string
  title: string
  status: string
  currentRevision?: EvidenceRevision
}

type AnswerVersion = {
  versionId: string
  versionNumber: number
  bodyText: string
  questionTextSnapshot?: string
}

type Answer = {
  answerId: string
  questionId: string
  currentVersionId?: string
  currentVersion?: AnswerVersion
}

type FeedbackItem = {
  itemId: string
  dimension: string
  ruleId: string
  issue: string
  rationale: string
  suggestion: string
  origin: string
  decision?: string | null
}

type Feedback = {
  feedbackId: string
  ruleVersion: string
  items: FeedbackItem[]
}

const TOPICS = ['teamwork', 'leadership', 'conflict', 'failure', 'communication', 'ownership'] as const

const emptyStar = { title: '', situation: '', task: '', action: '', result: '', tags: '' }

export function BehavioralPage() {
  const [tab, setTab] = useState<Tab>('questions')
  const [msg, setMsg] = useState<string | null>(null)
  const [ok, setOk] = useState(false)

  const [topic, setTopic] = useState('')
  const [questions, setQuestions] = useState<Question[]>([])
  const [newQText, setNewQText] = useState('')
  const [newQTopic, setNewQTopic] = useState('teamwork')

  const [evidence, setEvidence] = useState<Evidence[]>([])
  const [star, setStar] = useState(emptyStar)
  const [editingId, setEditingId] = useState<string | null>(null)

  const [selectedQ, setSelectedQ] = useState<string>('')
  const [answers, setAnswers] = useState<Answer[]>([])
  const [bodyText, setBodyText] = useState('')
  const [selectedEvidence, setSelectedEvidence] = useState<string[]>([])
  const [activeAnswer, setActiveAnswer] = useState<Answer | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  function flash(error: unknown, success?: string) {
    if (success) {
      setOk(true)
      setMsg(success)
      return
    }
    setOk(false)
    setMsg(error instanceof ApiError ? `${error.code}: ${error.message}` : error instanceof Error ? error.message : 'Failed')
  }

  async function loadQuestions() {
    const qs = topic ? `?topic=${encodeURIComponent(topic)}` : ''
    const data = await api.get<{ items: Question[] }>(`/behavioral/questions${qs}`)
    setQuestions(data.items || [])
  }

  async function loadEvidence() {
    const data = await api.get<{ items: Evidence[] }>('/behavioral/evidence')
    setEvidence(data.items || [])
  }

  async function loadAnswers(questionId?: string) {
    const qs = questionId ? `?questionId=${encodeURIComponent(questionId)}` : ''
    const data = await api.get<{ items: Answer[] }>(`/behavioral/answers${qs}`)
    setAnswers(data.items || [])
  }

  async function refreshAll() {
    try {
      await Promise.all([loadQuestions(), loadEvidence(), loadAnswers(selectedQ || undefined)])
      setMsg(null)
      setOk(false)
    } catch (e) {
      flash(e)
    }
  }

  useEffect(() => {
    void refreshAll()
  }, [])

  useEffect(() => {
    void loadQuestions().catch((e) => flash(e))
  }, [topic])

  useEffect(() => {
    void loadAnswers(selectedQ || undefined).catch((e) => flash(e))
  }, [selectedQ])

  const selectedQuestion = useMemo(
    () => questions.find((q) => q.questionId === selectedQ),
    [questions, selectedQ],
  )

  async function createQuestion(e: FormEvent) {
    e.preventDefault()
    try {
      await api.post('/behavioral/questions', { text: newQText.trim(), competencyTopic: newQTopic })
      setNewQText('')
      flash(null, '题目已添加')
      await loadQuestions()
    } catch (err) {
      flash(err)
    }
  }

  async function hideQuestion(q: Question) {
    try {
      const next = q.visibility === 'HIDDEN' ? 'ACTIVE' : 'HIDDEN'
      await api.patch(`/behavioral/questions/${q.questionId}`, { visibility: next })
      await loadQuestions()
    } catch (err) {
      flash(err)
    }
  }

  async function deleteQuestion(q: Question) {
    if (q.sourceType === 'CURATED') return
    if (!confirm('删除自定义题？')) return
    try {
      await api.delete(`/behavioral/questions/${q.questionId}`)
      await loadQuestions()
    } catch (err) {
      flash(err)
    }
  }

  async function saveEvidence(e: FormEvent) {
    e.preventDefault()
    const payload = {
      title: star.title.trim(),
      situation: star.situation.trim(),
      task: star.task.trim(),
      action: star.action.trim(),
      result: star.result.trim(),
      competencyTags: star.tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
    }
    try {
      if (editingId) {
        await api.put(`/behavioral/evidence/${editingId}`, payload)
        flash(null, '已新建修订')
      } else {
        await api.post('/behavioral/evidence', payload)
        flash(null, 'Evidence 已创建')
      }
      setStar(emptyStar)
      setEditingId(null)
      await loadEvidence()
    } catch (err) {
      flash(err)
    }
  }

  function startEdit(ev: Evidence) {
    const r = ev.currentRevision
    setEditingId(ev.evidenceId)
    setStar({
      title: ev.title,
      situation: r?.situation || '',
      task: r?.task || '',
      action: r?.action || '',
      result: r?.result || '',
      tags: (r?.competencyTags || []).join(', '),
    })
  }

  async function removeEvidence(ev: Evidence) {
    if (!confirm(`删除「${ev.title}」？`)) return
    try {
      await api.delete(`/behavioral/evidence/${ev.evidenceId}`)
      await loadEvidence()
    } catch (err) {
      flash(err)
    }
  }

  function toggleEvidence(id: string) {
    setSelectedEvidence((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  async function saveAnswer(e: FormEvent) {
    e.preventDefault()
    if (!selectedQ) {
      flash(new Error('请先选题'))
      return
    }
    try {
      if (activeAnswer) {
        const version = await api.post<AnswerVersion & { answerId: string }>(
          `/behavioral/answers/${activeAnswer.answerId}/versions`,
          { bodyText: bodyText.trim(), evidenceIds: selectedEvidence },
        )
        const detail = await api.get<Answer>(`/behavioral/answers/${activeAnswer.answerId}`)
        setActiveAnswer(detail)
        setFeedback(null)
        flash(null, `已保存 v${version.versionNumber}`)
      } else {
        const created = await api.post<Answer>('/behavioral/answers', {
          questionId: selectedQ,
          bodyText: bodyText.trim(),
          evidenceIds: selectedEvidence,
        })
        setActiveAnswer(created)
        setFeedback(null)
        flash(null, '答案已创建')
      }
      await loadAnswers(selectedQ)
    } catch (err) {
      flash(err)
    }
  }

  async function openAnswer(a: Answer) {
    try {
      const detail = await api.get<Answer>(`/behavioral/answers/${a.answerId}`)
      setActiveAnswer(detail)
      setSelectedQ(detail.questionId)
      setBodyText(detail.currentVersion?.bodyText || '')
      setFeedback(null)
      setTab('practice')
    } catch (err) {
      flash(err)
    }
  }

  async function runLocalFeedback() {
    const versionId = activeAnswer?.currentVersionId || activeAnswer?.currentVersion?.versionId
    if (!activeAnswer || !versionId) {
      flash(new Error('无当前版本'))
      return
    }
    try {
      const fb = await api.post<Feedback>(
        `/behavioral/answers/${activeAnswer.answerId}/versions/${versionId}/feedback`,
      )
      setFeedback(fb)
      flash(null, `本地反馈 ${fb.ruleVersion}`)
    } catch (err) {
      flash(err)
    }
  }

  async function decide(item: FeedbackItem, decision: 'ACCEPT' | 'REJECT' | 'DEFER') {
    if (!feedback) return
    try {
      const updated = await api.post<FeedbackItem>(
        `/behavioral/feedback/${feedback.feedbackId}/items/${item.itemId}/decision`,
        { decision },
      )
      setFeedback({
        ...feedback,
        items: feedback.items.map((x) => (x.itemId === item.itemId ? { ...x, ...updated } : x)),
      })
    } catch (err) {
      flash(err)
    }
  }

  return (
    <div className="page">
      <div className="topbar">
        <h1>Behavioral</h1>
        <span style={{ fontSize: 12, color: 'var(--ink-muted)' }}>题库 · STAR · 本地反馈 · AI 未启用</span>
        <div className="spacer" />
        <button className="btn btn-sm" type="button" onClick={() => void refreshAll()}>
          刷新
        </button>
      </div>
      {msg && <div className={`banner ${ok ? 'banner-ok' : 'banner-error'}`}>{msg}</div>}

      <div className="main-col">
        <div className="bhv-tabs">
          {(['questions', 'evidence', 'practice'] as Tab[]).map((t) => (
            <button
              key={t}
              type="button"
              className={`btn btn-sm ${tab === t ? 'btn-primary' : ''}`}
              onClick={() => setTab(t)}
            >
              {t === 'questions' ? 'Questions' : t === 'evidence' ? 'Evidence' : 'Practice'}
            </button>
          ))}
          <button className="btn btn-sm" type="button" disabled title="AI Port only">
            AI 增强 · 未启用
          </button>
        </div>

        {tab === 'questions' && (
          <section className="section">
            <div className="section-head">
              <h2>Questions</h2>
              <span className="count">{questions.length}</span>
            </div>
            <div className="bhv-row">
              <label>
                Topic{' '}
                <select value={topic} onChange={(e) => setTopic(e.target.value)}>
                  <option value="">全部</option>
                  {TOPICS.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <form className="bhv-form" onSubmit={(e) => void createQuestion(e)}>
              <textarea
                rows={2}
                placeholder="自定义题目"
                value={newQText}
                onChange={(e) => setNewQText(e.target.value)}
                required
              />
              <select value={newQTopic} onChange={(e) => setNewQTopic(e.target.value)}>
                {TOPICS.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
              <button className="btn btn-primary" type="submit">
                添加自定义题
              </button>
            </form>
            <ul className="bhv-list">
              {questions.map((q) => (
                <li key={q.questionId}>
                  <div>
                    <strong>[{q.competencyTopic}]</strong> {q.text}
                    <div className="hint">
                      {q.sourceType} · {q.visibility}
                    </div>
                  </div>
                  <div className="cv-actions">
                    <button className="btn btn-sm" type="button" onClick={() => void hideQuestion(q)}>
                      {q.visibility === 'HIDDEN' ? '显示' : '隐藏'}
                    </button>
                    {q.sourceType === 'USER_DEFINED' && (
                      <button className="btn btn-sm" type="button" onClick={() => void deleteQuestion(q)}>
                        删除
                      </button>
                    )}
                    <button
                      className="btn btn-sm btn-primary"
                      type="button"
                      onClick={() => {
                        setSelectedQ(q.questionId)
                        setActiveAnswer(null)
                        setBodyText('')
                        setFeedback(null)
                        setTab('practice')
                      }}
                    >
                      练习
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}

        {tab === 'evidence' && (
          <section className="section">
            <div className="section-head">
              <h2>STAR Evidence</h2>
              <span className="count">{evidence.length}</span>
            </div>
            <form className="bhv-form stacked" onSubmit={(e) => void saveEvidence(e)}>
              <input
                placeholder="Title"
                value={star.title}
                onChange={(e) => setStar({ ...star, title: e.target.value })}
                required
              />
              <textarea
                rows={2}
                placeholder="Situation"
                value={star.situation}
                onChange={(e) => setStar({ ...star, situation: e.target.value })}
                required
              />
              <textarea
                rows={2}
                placeholder="Task"
                value={star.task}
                onChange={(e) => setStar({ ...star, task: e.target.value })}
                required
              />
              <textarea
                rows={2}
                placeholder="Action"
                value={star.action}
                onChange={(e) => setStar({ ...star, action: e.target.value })}
                required
              />
              <textarea
                rows={2}
                placeholder="Result"
                value={star.result}
                onChange={(e) => setStar({ ...star, result: e.target.value })}
                required
              />
              <input
                placeholder="Tags（逗号分隔）"
                value={star.tags}
                onChange={(e) => setStar({ ...star, tags: e.target.value })}
              />
              <div className="bhv-row">
                <button className="btn btn-primary" type="submit">
                  {editingId ? '保存新修订' : '创建 Evidence'}
                </button>
                {editingId && (
                  <button
                    className="btn"
                    type="button"
                    onClick={() => {
                      setEditingId(null)
                      setStar(emptyStar)
                    }}
                  >
                    取消编辑
                  </button>
                )}
              </div>
            </form>
            <ul className="bhv-list">
              {evidence.map((ev) => (
                <li key={ev.evidenceId}>
                  <div>
                    <strong>{ev.title}</strong>
                    <div className="hint">
                      {(ev.currentRevision?.competencyTags || []).join(', ') || 'no tags'}
                    </div>
                  </div>
                  <div className="cv-actions">
                    <button className="btn btn-sm" type="button" onClick={() => startEdit(ev)}>
                      修订
                    </button>
                    <button className="btn btn-sm" type="button" onClick={() => void removeEvidence(ev)}>
                      删除
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}

        {tab === 'practice' && (
          <section className="section">
            <div className="section-head">
              <h2>Practice</h2>
            </div>
            <label className="bhv-row">
              题目{' '}
              <select value={selectedQ} onChange={(e) => setSelectedQ(e.target.value)}>
                <option value="">选择题目</option>
                {questions.map((q) => (
                  <option key={q.questionId} value={q.questionId}>
                    [{q.competencyTopic}] {q.text.slice(0, 80)}
                  </option>
                ))}
              </select>
            </label>
            {selectedQuestion && <p className="bhv-prompt">{selectedQuestion.text}</p>}

            <div className="section-head" style={{ marginTop: 16 }}>
              <h2>关联 Evidence</h2>
            </div>
            <div className="bhv-checks">
              {evidence.map((ev) => (
                <label key={ev.evidenceId}>
                  <input
                    type="checkbox"
                    checked={selectedEvidence.includes(ev.evidenceId)}
                    onChange={() => toggleEvidence(ev.evidenceId)}
                  />
                  {ev.title}
                </label>
              ))}
              {evidence.length === 0 && <span className="hint">暂无 Evidence，可先到 Evidence 页创建</span>}
            </div>

            <form className="bhv-form stacked" onSubmit={(e) => void saveAnswer(e)}>
              <textarea
                rows={8}
                placeholder="写下 STAR 答案…"
                value={bodyText}
                onChange={(e) => setBodyText(e.target.value)}
                required
              />
              <div className="bhv-row">
                <button className="btn btn-primary" type="submit">
                  {activeAnswer ? '保存新版本' : '创建答案'}
                </button>
                <button className="btn" type="button" onClick={() => void runLocalFeedback()} disabled={!activeAnswer}>
                  本地反馈
                </button>
                <button className="btn" type="button" disabled title="AI_NOT_ENABLED">
                  AI 反馈 · 未启用
                </button>
                {activeAnswer && (
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => {
                      setActiveAnswer(null)
                      setBodyText('')
                      setFeedback(null)
                    }}
                  >
                    新开答案
                  </button>
                )}
              </div>
            </form>

            {answers.length > 0 && (
              <>
                <div className="section-head">
                  <h2>已有答案</h2>
                  <span className="count">{answers.length}</span>
                </div>
                <ul className="bhv-list">
                  {answers.map((a) => (
                    <li key={a.answerId}>
                      <div className="hint">
                        {a.answerId.slice(0, 8)}… · version {a.currentVersion?.versionNumber ?? '—'}
                      </div>
                      <button className="btn btn-sm" type="button" onClick={() => void openAnswer(a)}>
                        打开
                      </button>
                    </li>
                  ))}
                </ul>
              </>
            )}

            {feedback && (
              <div className="bhv-feedback">
                <div className="section-head">
                  <h2>Feedback</h2>
                  <span className="count">{feedback.ruleVersion}</span>
                </div>
                {feedback.items.length === 0 ? (
                  <p className="hint">无问题项</p>
                ) : (
                  <ul className="bhv-list">
                    {feedback.items.map((item) => (
                      <li key={item.itemId} className="stacked-item">
                        <div>
                          <strong>
                            {item.dimension} · {item.ruleId}
                          </strong>
                          <div>{item.issue}</div>
                          <div className="hint">{item.suggestion}</div>
                          <div className="hint">decision: {item.decision || 'PENDING'}</div>
                        </div>
                        <div className="cv-actions">
                          <button className="btn btn-sm" type="button" onClick={() => void decide(item, 'ACCEPT')}>
                            ACCEPT
                          </button>
                          <button className="btn btn-sm" type="button" onClick={() => void decide(item, 'REJECT')}>
                            REJECT
                          </button>
                          <button className="btn btn-sm" type="button" onClick={() => void decide(item, 'DEFER')}>
                            DEFER
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </section>
        )}
      </div>
    </div>
  )
}
