import { type FormEvent, useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'

type FolderKind = 'todolist' | 'companytracker'

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

const COMPANY_STATUSES: { value: string; label: string }[] = [
  { value: 'watching', label: '想投' },
  { value: 'applied', label: '已投' },
  { value: 'interview', label: '面试' },
  { value: 'offer', label: 'Offer' },
  { value: 'rejected', label: '拒信' },
  { value: 'on_hold', label: '暂缓' },
]

export function RoadmapPage() {
  const [folders, setFolders] = useState<Folder[]>([])
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null)
  const [kind, setKind] = useState<FolderKind>('todolist')
  const [todos, setTodos] = useState<Todo[]>([])
  const [companies, setCompanies] = useState<Company[]>([])
  const [msg, setMsg] = useState<string | null>(null)
  const [draftName, setDraftName] = useState('')
  const [draftCompanyName, setDraftCompanyName] = useState('')
  const [draftFolderName, setDraftFolderName] = useState('')
  const [draftFolderKind, setDraftFolderKind] = useState<FolderKind>('todolist')
  const [renamingId, setRenamingId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')

  const selected = folders.find((f) => f.folderId === selectedFolderId) || null
  const isCompany = kind === 'companytracker'

  async function load(folderId?: string | null) {
    try {
      const qs = folderId ? `?folderId=${encodeURIComponent(folderId)}` : ''
      const data = await api.get<{
        folderId: string
        kind?: FolderKind
        folders: Folder[]
        todos: Todo[]
        companies?: Company[]
      }>(`/roadmap${qs}`)
      setFolders(data.folders || [])
      setSelectedFolderId(data.folderId)
      setKind(data.kind || 'todolist')
      setTodos(data.todos || [])
      setCompanies(data.companies || [])
      setMsg(null)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : 'Load failed')
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function selectFolder(folderId: string) {
    setSelectedFolderId(folderId)
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
    const what = folder.kind === 'companytracker' ? '公司记录' : '任务'
    if (!confirm(`删除文件夹「${folder.name}」及其全部${what}？`)) return
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
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed')
    }
  }

  async function toggle(todo: Todo) {
    try {
      await api.post(`/roadmap/todos/${todo.todoId}/toggle`)
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Toggle failed')
    }
  }

  async function removeTodo(todo: Todo) {
    if (!confirm(`删除「${todo.name}」？`)) return
    try {
      await api.delete(`/roadmap/todos/${todo.todoId}`)
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete failed')
    }
  }

  async function addCompany(e: FormEvent) {
    e.preventDefault()
    if (!draftCompanyName.trim() || !selectedFolderId) return
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
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed')
    }
  }

  async function removeCompany(company: Company) {
    if (!confirm(`删除「${company.companyName}」？`)) return
    try {
      await api.delete(`/roadmap/companies/${company.companyId}`)
      await load(selectedFolderId)
    } catch (err) {
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete failed')
    }
  }

  function kindLabel(k: FolderKind) {
    return k === 'companytracker' ? '公司' : 'Todo'
  }

  return (
    <div className="page roadmap-folders-page">
      <div className="topbar">
        <h1>Roadmap</h1>
        <span style={{ fontSize: 12, color: 'var(--ink-muted)' }}>TodoList / CompanyTracker</span>
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

          {isCompany ? (
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
                      key={`${c.companyId}-name-${c.companyName}`}
                      onBlur={(e) => {
                        if (e.target.value.trim() && e.target.value !== c.companyName) {
                          void patchCompany(c, { companyName: e.target.value.trim() })
                        }
                      }}
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
                      key={`${c.companyId}-contact-${c.contact || ''}`}
                      placeholder="联系人…"
                      onBlur={(e) => {
                        if (e.target.value !== (c.contact || '')) {
                          void patchCompany(c, { contact: e.target.value })
                        }
                      }}
                    />
                    <textarea
                      className="notion-comment"
                      defaultValue={c.note || ''}
                      key={`${c.companyId}-note-${c.note || ''}`}
                      rows={2}
                      placeholder="备注…"
                      onBlur={(e) => {
                        if (e.target.value !== (c.note || '')) {
                          void patchCompany(c, { note: e.target.value })
                        }
                      }}
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
                      key={`${t.todoId}-name-${t.name}`}
                      onBlur={(e) => {
                        if (e.target.value.trim() && e.target.value !== t.name) {
                          void patchTodo(t, { name: e.target.value.trim() })
                        }
                      }}
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
                      key={`${t.todoId}-c-${t.comment || ''}`}
                      rows={2}
                      placeholder="备注…"
                      onBlur={(e) => {
                        if (e.target.value !== (t.comment || '')) {
                          void patchTodo(t, { comment: e.target.value })
                        }
                      }}
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
