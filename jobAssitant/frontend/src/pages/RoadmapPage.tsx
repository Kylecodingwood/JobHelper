import { type FormEvent, useEffect, useRef, useState } from 'react'
import { api, ApiError } from '../api/client'
import { DocumentEditor } from '../components/DocumentEditor'
import { DocumentEditorBoundary } from '../components/DocumentEditorBoundary'

type FolderKind = 'todolist' | 'companytracker' | 'document'

type Folder = {
  folderId: string
  name: string
  kind: FolderKind
  itemCount?: number
  todoCount?: number
  sortOrder?: number
}

type Todo = {
  todoId: string
  folderId: string
  name: string
  dueAt?: string | null
  comment?: string | null
  done: boolean
  sortOrder: number
}

type Company = {
  companyId: string
  folderId: string
  companyName: string
  status: string
  contact?: string | null
  note?: string | null
  sortOrder: number
}

type Doc = {
  documentId: string
  folderId: string
  title: string
  bodyHtml: string
  sortOrder: number
  updatedAt?: string
}

const COMPANY_STATUSES: { value: string; label: string }[] = [
  { value: 'watching', label: '想投' },
  { value: 'applied', label: '已投' },
  { value: 'interview', label: '面试' },
  { value: 'offer', label: 'Offer' },
  { value: 'rejected', label: '拒信' },
  { value: 'on_hold', label: '暂缓' },
]

const AUTOSAVE_MS = 700

type PendingSave = {
  timer: ReturnType<typeof setTimeout> | null
  run: (opts?: { keepalive?: boolean }) => Promise<void>
}

export function RoadmapPage() {
  const [folders, setFolders] = useState<Folder[]>([])
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null)
  const [kind, setKind] = useState<FolderKind>('todolist')
  const [todos, setTodos] = useState<Todo[]>([])
  const [companies, setCompanies] = useState<Company[]>([])
  const [documents, setDocuments] = useState<Doc[]>([])
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)
  const [draftName, setDraftName] = useState('')
  const [draftCompanyName, setDraftCompanyName] = useState('')
  const [draftFolderName, setDraftFolderName] = useState('')
  const [draftFolderKind, setDraftFolderKind] = useState<FolderKind>('todolist')
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const [saveHint, setSaveHint] = useState<string | null>(null)

  const pendingRef = useRef(new Map<string, PendingSave>())

  const selected = folders.find((f) => f.folderId === selectedFolderId) || null
  const isCompany = kind === 'companytracker'
  const isDocument = kind === 'document'
  const selectedDoc = documents.find((d) => d.documentId === selectedDocId) || null

  async function load(folderId?: string | null) {
    await flushAllPending()
    try {
      const qs = folderId ? `?folderId=${encodeURIComponent(folderId)}` : ''
      const data = await api.get<{
        folderId: string
        kind?: FolderKind
        folders: Folder[]
        todos: Todo[]
        companies?: Company[]
        documents?: Doc[]
      }>(`/roadmap${qs}`)
      setFolders(data.folders || [])
      setSelectedFolderId(data.folderId)
      setKind(data.kind || 'todolist')
      setTodos(data.todos || [])
      setCompanies(data.companies || [])
      const docs = data.documents || []
      setDocuments(docs)
      setSelectedDocId((prev) => {
        if (data.kind !== 'document') return null
        if (prev && docs.some((d) => d.documentId === prev)) return prev
        return docs[0]?.documentId ?? null
      })
      setMsg(null)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Load failed')
    }
  }

  useEffect(() => {
    void load()
  }, [])

  useEffect(() => {
    const onVis = () => {
      if (document.visibilityState === 'hidden') {
        void flushAllPending({ keepalive: true })
      }
    }
    const onUnload = () => {
      void flushAllPending({ keepalive: true })
    }
    document.addEventListener('visibilitychange', onVis)
    window.addEventListener('pagehide', onUnload)
    window.addEventListener('beforeunload', onUnload)
    return () => {
      document.removeEventListener('visibilitychange', onVis)
      window.removeEventListener('pagehide', onUnload)
      window.removeEventListener('beforeunload', onUnload)
      void flushAllPending({ keepalive: true })
    }
  }, [])

  async function flushKey(key: string, opts?: { keepalive?: boolean }) {
    const pending = pendingRef.current.get(key)
    if (!pending) return
    if (pending.timer) clearTimeout(pending.timer)
    pendingRef.current.delete(key)
    try {
      await pending.run(opts)
    } catch {
      /* error already surfaced by run */
    }
  }

  async function flushAllPending(opts?: { keepalive?: boolean }) {
    const keys = [...pendingRef.current.keys()]
    await Promise.all(keys.map((k) => flushKey(k, opts)))
  }

  function scheduleSave(key: string, run: (opts?: { keepalive?: boolean }) => Promise<void>) {
    const existing = pendingRef.current.get(key)
    if (existing?.timer) clearTimeout(existing.timer)
    const timer = setTimeout(() => {
      void flushKey(key)
    }, AUTOSAVE_MS)
    pendingRef.current.set(key, { timer, run })
    setSaveHint('Saving…')
  }

  async function patchTodoSilent(
    todoId: string,
    body: Partial<Todo>,
    baseline: Partial<Todo>,
    opts?: { keepalive?: boolean },
  ) {
    const changed = Object.entries(body).some(([k, v]) => {
      const prev = baseline[k as keyof Todo]
      return String(v ?? '') !== String(prev ?? '')
    })
    if (!changed) {
      setSaveHint(null)
      return
    }
    try {
      if (opts?.keepalive) {
        await patchKeepalive(`/roadmap/todos/${todoId}`, body)
      } else {
        await api.patch(`/roadmap/todos/${todoId}`, body)
      }
      setTodos((prev) => prev.map((t) => (t.todoId === todoId ? { ...t, ...body } : t)))
      setSaveHint('Saved')
      window.setTimeout(() => setSaveHint(null), 1200)
    } catch (err) {
      setSaveHint(null)
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed')
      throw err
    }
  }

  async function patchCompanySilent(
    companyId: string,
    body: Partial<Company>,
    baseline: Partial<Company>,
    opts?: { keepalive?: boolean },
  ) {
    const changed = Object.entries(body).some(([k, v]) => {
      const prev = baseline[k as keyof Company]
      return String(v ?? '') !== String(prev ?? '')
    })
    if (!changed) {
      setSaveHint(null)
      return
    }
    try {
      if (opts?.keepalive) {
        await patchKeepalive(`/roadmap/companies/${companyId}`, body)
      } else {
        await api.patch(`/roadmap/companies/${companyId}`, body)
      }
      setCompanies((prev) => prev.map((c) => (c.companyId === companyId ? { ...c, ...body } : c)))
      setSaveHint('Saved')
      window.setTimeout(() => setSaveHint(null), 1200)
    } catch (err) {
      setSaveHint(null)
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed')
      throw err
    }
  }

  async function selectFolder(folderId: string) {
    await flushAllPending()
    const folder = folders.find((f) => f.folderId === folderId)
    setSelectedFolderId(folderId)
    if (folder) {
      setKind(folder.kind || 'todolist')
      if (folder.kind !== 'document') {
        setDocuments([])
        setSelectedDocId(null)
      }
      if (folder.kind !== 'companytracker') {
        setCompanies([])
      }
      if (folder.kind !== 'todolist') {
        setTodos([])
      }
    }
    await load(folderId)
  }

  async function addFolder(e: FormEvent) {
    e.preventDefault()
    if (!draftFolderName.trim()) return
    try {
      const created = await api.post<Folder>('/roadmap/folders', {
        name: draftFolderName.trim(),
        kind: draftFolderKind,
      })
      setDraftFolderName('')
      setDraftFolderKind('todolist')
      await load(created.folderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Create folder failed')
    }
  }

  async function saveRename(folder: Folder) {
    const name = renameValue.trim()
    setRenamingId(null)
    if (!name || name === folder.name) return
    try {
      await api.patch(`/roadmap/folders/${folder.folderId}`, { name })
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Rename failed')
    }
  }

  async function removeFolder(folder: Folder) {
    if (folders.length <= 1) {
      setMsg('至少保留一个文件夹')
      return
    }
    const what =
      folder.kind === 'companytracker' ? '公司记录' : folder.kind === 'document' ? '文档' : '任务'
    if (!confirm(`删除文件夹「${folder.name}」及其全部${what}？`)) return
    await flushAllPending()
    try {
      await api.delete(`/roadmap/folders/${folder.folderId}`)
      await load(null)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete folder failed')
    }
  }

  async function addTodo(e: FormEvent) {
    e.preventDefault()
    if (!draftName.trim() || !selectedFolderId) return
    await flushAllPending()
    try {
      await api.post('/roadmap/todos', { name: draftName.trim(), folderId: selectedFolderId })
      setDraftName('')
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Create failed')
    }
  }

  async function patchTodo(todo: Todo, body: Partial<Todo>) {
    try {
      await api.patch(`/roadmap/todos/${todo.todoId}`, body)
      setTodos((prev) => prev.map((t) => (t.todoId === todo.todoId ? { ...t, ...body } : t)))
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed')
    }
  }

  async function toggle(todo: Todo) {
    try {
      await api.post(`/roadmap/todos/${todo.todoId}/toggle`)
      setTodos((prev) => prev.map((t) => (t.todoId === todo.todoId ? { ...t, done: !t.done } : t)))
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Toggle failed')
    }
  }

  async function removeTodo(todo: Todo) {
    if (!confirm(`删除「${todo.name}」？`)) return
    await flushKey(`todo:${todo.todoId}:comment`)
    await flushKey(`todo:${todo.todoId}:name`)
    try {
      await api.delete(`/roadmap/todos/${todo.todoId}`)
      setTodos((prev) => prev.filter((t) => t.todoId !== todo.todoId))
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete failed')
    }
  }

  async function addCompany(e: FormEvent) {
    e.preventDefault()
    if (!draftCompanyName.trim() || !selectedFolderId) return
    await flushAllPending()
    try {
      await api.post('/roadmap/companies', {
        companyName: draftCompanyName.trim(),
        folderId: selectedFolderId,
        status: 'watching',
      })
      setDraftCompanyName('')
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Create company failed')
    }
  }

  async function patchCompany(company: Company, body: Partial<Company>) {
    try {
      await api.patch(`/roadmap/companies/${company.companyId}`, body)
      setCompanies((prev) => prev.map((c) => (c.companyId === company.companyId ? { ...c, ...body } : c)))
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed')
    }
  }

  async function removeCompany(company: Company) {
    if (!confirm(`删除「${company.companyName}」？`)) return
    await flushKey(`company:${company.companyId}:note`)
    await flushKey(`company:${company.companyId}:contact`)
    await flushKey(`company:${company.companyId}:name`)
    try {
      await api.delete(`/roadmap/companies/${company.companyId}`)
      setCompanies((prev) => prev.filter((c) => c.companyId !== company.companyId))
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete failed')
    }
  }

  async function addDocument() {
    if (!selectedFolderId) return
    await flushAllPending()
    try {
      const created = await api.post<Doc>('/roadmap/documents', {
        folderId: selectedFolderId,
        title: 'Untitled',
        bodyHtml: '',
      })
      setDocuments((prev) => [...prev, created])
      setSelectedDocId(created.documentId)
      setFolders((prev) =>
        prev.map((f) =>
          f.folderId === selectedFolderId
            ? { ...f, itemCount: (f.itemCount ?? f.todoCount ?? 0) + 1 }
            : f,
        ),
      )
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Create document failed')
    }
  }

  async function removeDocument(doc: Doc) {
    if (!confirm(`删除文档「${doc.title}」？`)) return
    await flushKey(`doc:${doc.documentId}:title`)
    await flushKey(`doc:${doc.documentId}:body`)
    try {
      await api.delete(`/roadmap/documents/${doc.documentId}`)
      setDocuments((prev) => {
        const next = prev.filter((d) => d.documentId !== doc.documentId)
        setSelectedDocId((cur) => (cur === doc.documentId ? next[0]?.documentId ?? null : cur))
        return next
      })
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete failed')
    }
  }

  async function patchDocumentSilent(
    documentId: string,
    body: Partial<Doc>,
    baseline: Partial<Doc>,
    opts?: { keepalive?: boolean },
  ) {
    const changed = Object.entries(body).some(([k, v]) => {
      const prev = baseline[k as keyof Doc]
      return String(v ?? '') !== String(prev ?? '')
    })
    if (!changed) {
      setSaveHint(null)
      return
    }
    try {
      if (opts?.keepalive) {
        await patchKeepalive(`/roadmap/documents/${documentId}`, body)
      } else {
        await api.patch(`/roadmap/documents/${documentId}`, body)
      }
      setDocuments((prev) => prev.map((d) => (d.documentId === documentId ? { ...d, ...body } : d)))
      setSaveHint('Saved')
      window.setTimeout(() => setSaveHint(null), 1200)
    } catch (err) {
      setSaveHint(null)
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed')
      throw err
    }
  }

  function kindLabel(k: FolderKind) {
    if (k === 'companytracker') return '公司'
    if (k === 'document') return '文档'
    return 'Todo'
  }

  function queueTodoField(todo: Todo, field: 'comment' | 'name', value: string, immediate = false) {
    if (field === 'name' && !value.trim()) return
    const key = `todo:${todo.todoId}:${field}`
    const baseline = field === 'comment' ? { comment: todo.comment } : { name: todo.name }
    const body = field === 'comment' ? { comment: value } : { name: value.trim() }
    const run = (opts?: { keepalive?: boolean }) => patchTodoSilent(todo.todoId, body, baseline, opts)
    if (immediate) {
      const existing = pendingRef.current.get(key)
      if (existing?.timer) clearTimeout(existing.timer)
      pendingRef.current.set(key, { timer: null, run })
      void flushKey(key)
      return
    }
    scheduleSave(key, run)
  }

  function queueCompanyField(
    company: Company,
    field: 'note' | 'contact' | 'companyName',
    value: string,
    immediate = false,
  ) {
    if (field === 'companyName' && !value.trim()) return
    const key = `company:${company.companyId}:${field === 'companyName' ? 'name' : field}`
    const baseline =
      field === 'note'
        ? { note: company.note }
        : field === 'contact'
          ? { contact: company.contact }
          : { companyName: company.companyName }
    const body =
      field === 'note'
        ? { note: value }
        : field === 'contact'
          ? { contact: value }
          : { companyName: value.trim() }
    const run = (opts?: { keepalive?: boolean }) => patchCompanySilent(company.companyId, body, baseline, opts)
    if (immediate) {
      const existing = pendingRef.current.get(key)
      if (existing?.timer) clearTimeout(existing.timer)
      pendingRef.current.set(key, { timer: null, run })
      void flushKey(key)
      return
    }
    scheduleSave(key, run)
  }

  function queueDocField(doc: Doc, field: 'title' | 'bodyHtml', value: string, immediate = false) {
    if (field === 'title' && !value.trim()) return
    const key = `doc:${doc.documentId}:${field === 'bodyHtml' ? 'body' : 'title'}`
    const baseline = field === 'title' ? { title: doc.title } : { bodyHtml: doc.bodyHtml }
    const body = field === 'title' ? { title: value.trim() } : { bodyHtml: value }
    const run = (opts?: { keepalive?: boolean }) => patchDocumentSilent(doc.documentId, body, baseline, opts)
    if (immediate) {
      const existing = pendingRef.current.get(key)
      if (existing?.timer) clearTimeout(existing.timer)
      pendingRef.current.set(key, { timer: null, run })
      void flushKey(key)
      return
    }
    scheduleSave(key, run)
  }

  return (
    <div className="page roadmap-folders-page">
      <div className="topbar">
        <h1>Roadmap</h1>
        <span style={{ fontSize: 12, color: 'var(--ink-muted)' }}>TodoList / Company / Document</span>
        {saveHint && (
          <span style={{ fontSize: 12, color: 'var(--ink-muted)', marginLeft: 8 }}>{saveHint}</span>
        )}
        <div className="spacer" />
        <button className="btn btn-sm" type="button" onClick={() => void load(selectedFolderId)}>
          刷新
        </button>
      </div>
      {msg && <div className="banner">{msg}</div>}

      <div className="roadmap-split">
        <aside className="roadmap-folder-pane">
          <div className="roadmap-folder-head">文件夹</div>
          <ul className="roadmap-folder-list">
            {folders.map((f) => (
              <li key={f.folderId} className={f.folderId === selectedFolderId ? 'active' : undefined}>
                {renamingId === f.folderId ? (
                  <input
                    className="roadmap-folder-rename"
                    value={renameValue}
                    autoFocus
                    onChange={(e) => setRenameValue(e.target.value)}
                    onBlur={() => void saveRename(f)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        void saveRename(f)
                      }
                      if (e.key === 'Escape') setRenamingId(null)
                    }}
                  />
                ) : (
                  <button type="button" className="roadmap-folder-btn" onClick={() => void selectFolder(f.folderId)}>
                    <span className="roadmap-folder-name">
                      <span className="roadmap-folder-kind">{kindLabel(f.kind || 'todolist')}</span>
                      {f.name}
                    </span>
                    <span className="count">{f.itemCount ?? f.todoCount ?? 0}</span>
                  </button>
                )}
                <div className="roadmap-folder-actions">
                  <button
                    type="button"
                    className="btn btn-sm link"
                    title="重命名"
                    onClick={() => {
                      setRenamingId(f.folderId)
                      setRenameValue(f.name)
                    }}
                  >
                    改名
                  </button>
                  <button type="button" className="btn btn-sm link" onClick={() => void removeFolder(f)}>
                    删
                  </button>
                </div>
              </li>
            ))}
            <li className="roadmap-folder-add-item">
              <form className="roadmap-folder-add" onSubmit={(e) => void addFolder(e)}>
                <select
                  value={draftFolderKind}
                  onChange={(e) => setDraftFolderKind(e.target.value as FolderKind)}
                  aria-label="文件夹类型"
                >
                  <option value="todolist">TodoList</option>
                  <option value="companytracker">CompanyTracker</option>
                  <option value="document">Document</option>
                </select>
                <input
                  value={draftFolderName}
                  onChange={(e) => setDraftFolderName(e.target.value)}
                  placeholder="新建文件夹…"
                  required
                />
                <button className="btn btn-sm btn-primary" type="submit">
                  +
                </button>
              </form>
            </li>
          </ul>
        </aside>

        <section className="roadmap-task-pane">
          <div className="roadmap-task-head">
            <h2>{selected?.name || (isCompany ? '公司' : '任务')}</h2>
          </div>

          {isDocument ? (
            <>
              <div className="doc-layout">
                <aside className="doc-list-pane">
                  <button className="btn btn-sm btn-primary" type="button" onClick={() => void addDocument()} disabled={!selectedFolderId}>
                    + New doc
                  </button>
                  <ul className="doc-list">
                    {documents.map((d) => (
                      <li key={d.documentId}>
                        <button
                          type="button"
                          className={`doc-list-item${d.documentId === selectedDocId ? ' active' : ''}`}
                          onClick={() => {
                            void flushAllPending().then(() => setSelectedDocId(d.documentId))
                          }}
                        >
                          <span className="doc-list-title">{d.title || 'Untitled'}</span>
                          <span
                            className="doc-list-del"
                            onClick={(e) => {
                              e.stopPropagation()
                              void removeDocument(d)
                            }}
                          >
                            删
                          </span>
                        </button>
                      </li>
                    ))}
                    {!documents.length && <li className="empty-hint">No documents yet</li>}
                  </ul>
                </aside>
                <div className="doc-editor-pane">
                  {!selectedDoc ? (
                    <p className="empty-hint">Create a document to paste AI tips or long notes.</p>
                  ) : (
                    <>
                      <input
                        className="doc-title-input"
                        key={`${selectedDoc.documentId}-title`}
                        defaultValue={selectedDoc.title}
                        placeholder="Title"
                        onChange={(e) => queueDocField(selectedDoc, 'title', e.target.value)}
                        onBlur={(e) => queueDocField(selectedDoc, 'title', e.target.value, true)}
                      />
                      <DocumentEditorBoundary documentId={selectedDoc.documentId}>
                        <DocumentEditor
                          key={selectedDoc.documentId}
                          documentId={selectedDoc.documentId}
                          initialHtml={selectedDoc.bodyHtml || ''}
                          onChangeHtml={(html) => queueDocField(selectedDoc, 'bodyHtml', html)}
                          onBlurFlush={() => void flushKey(`doc:${selectedDoc.documentId}:body`)}
                        />
                      </DocumentEditorBoundary>
                    </>
                  )}
                </div>
              </div>
            </>
          ) : isCompany ? (
            <>
              <form className="notion-add" onSubmit={(e) => void addCompany(e)}>
                <input
                  value={draftCompanyName}
                  onChange={(e) => setDraftCompanyName(e.target.value)}
                  placeholder="新建公司…"
                  required
                  disabled={!selectedFolderId}
                />
                <button className="btn btn-primary" type="submit" disabled={!selectedFolderId}>
                  新建
                </button>
              </form>

              <div className="notion-table company-table">
                <div className="notion-head company-head">
                  <span>Company</span>
                  <span>Status</span>
                  <span>Contact</span>
                  <span>Note</span>
                  <span />
                </div>
                {companies.map((c) => (
                  <div key={c.companyId} className="notion-row company-row">
                    <input
                      className="notion-name"
                      defaultValue={c.companyName}
                      key={`${c.companyId}-name`}
                      onChange={(e) => queueCompanyField(c, 'companyName', e.target.value)}
                      onBlur={(e) => queueCompanyField(c, 'companyName', e.target.value, true)}
                    />
                    <select
                      value={c.status}
                      onChange={(e) => void patchCompany(c, { status: e.target.value })}
                    >
                      {COMPANY_STATUSES.map((s) => (
                        <option key={s.value} value={s.value}>
                          {s.label}
                        </option>
                      ))}
                    </select>
                    <input
                      defaultValue={c.contact || ''}
                      key={`${c.companyId}-contact`}
                      placeholder="联系人…"
                      onChange={(e) => queueCompanyField(c, 'contact', e.target.value)}
                      onBlur={(e) => queueCompanyField(c, 'contact', e.target.value, true)}
                    />
                    <textarea
                      className="notion-comment"
                      defaultValue={c.note || ''}
                      key={`${c.companyId}-note`}
                      rows={2}
                      placeholder="备注…"
                      onChange={(e) => queueCompanyField(c, 'note', e.target.value)}
                      onBlur={(e) => queueCompanyField(c, 'note', e.target.value, true)}
                    />
                    <button className="btn btn-sm" type="button" onClick={() => void removeCompany(c)}>
                      删除
                    </button>
                  </div>
                ))}
                {!companies.length && <p className="empty-hint">此文件夹暂无公司 — 在上方新建</p>}
              </div>
            </>
          ) : (
            <>
              <form className="notion-add" onSubmit={(e) => void addTodo(e)}>
                <input
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                  placeholder="在此文件夹新建任务…"
                  required
                  disabled={!selectedFolderId}
                />
                <button className="btn btn-primary" type="submit" disabled={!selectedFolderId}>
                  新建
                </button>
              </form>

              <div className="notion-table">
                <div className="notion-head">
                  <span />
                  <span>Task name</span>
                  <span>Due</span>
                  <span>Comment</span>
                  <span />
                </div>
                {todos.map((t) => (
                  <div key={t.todoId} className={`notion-row ${t.done ? 'done' : ''}`}>
                    <input type="checkbox" checked={t.done} onChange={() => void toggle(t)} aria-label="完成" />
                    <input
                      className="notion-name"
                      defaultValue={t.name}
                      key={`${t.todoId}-name`}
                      onChange={(e) => queueTodoField(t, 'name', e.target.value)}
                      onBlur={(e) => queueTodoField(t, 'name', e.target.value, true)}
                    />
                    <input
                      type="date"
                      defaultValue={t.dueAt ? t.dueAt.slice(0, 10) : ''}
                      key={`${t.todoId}-due-${t.dueAt || ''}`}
                      onChange={(e) => {
                        const v = e.target.value
                        void patchTodo(t, { dueAt: v ? `${v}T00:00:00Z` : null })
                      }}
                    />
                    <textarea
                      className="notion-comment"
                      defaultValue={t.comment || ''}
                      key={`${t.todoId}-c`}
                      rows={2}
                      placeholder="备注…"
                      onChange={(e) => queueTodoField(t, 'comment', e.target.value)}
                      onBlur={(e) => queueTodoField(t, 'comment', e.target.value, true)}
                    />
                    <button className="btn btn-sm" type="button" onClick={() => void removeTodo(t)}>
                      删除
                    </button>
                  </div>
                ))}
                {!todos.length && <p className="empty-hint">此文件夹暂无任务 — 在上方新建</p>}
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  )
}

async function patchKeepalive(path: string, body: unknown) {
  await fetch(`/api/v1${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    keepalive: true,
  })
}
