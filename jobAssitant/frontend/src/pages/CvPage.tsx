import { type FormEvent, useEffect, useRef, useState } from 'react'
import { renderAsync } from 'docx-preview'
import { api, ApiError } from '../api/client'

type CvDocument = {
  documentId: string
  displayName: string
  originalFilename: string
  fileKind?: string
  pdfReady: boolean
  sizeBytesOriginal: number
  sizeBytesPdf: number
  createdAt: string
  pdfUrl: string | null
  fileUrl: string
}

function formatBytes(n: number) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

function isDocx(doc: CvDocument) {
  return (doc.fileKind || '').toUpperCase() === 'DOCX'
}

function isPdf(doc: CvDocument) {
  return (doc.fileKind || '').toUpperCase() === 'PDF' || !!doc.pdfReady
}

/** Scale DOCX pages so a full page width fits the preview pane. */
function fitDocxToWidth(host: HTMLElement) {
  // renderAsync className "docx" → wrapper `.docx-wrapper`, pages `section.docx`
  const wrapper = host.querySelector('.docx-wrapper') as HTMLElement | null
  const page = host.querySelector('section.docx') as HTMLElement | null
  if (!wrapper || !page) return

  wrapper.style.transform = ''
  wrapper.style.width = ''
  wrapper.style.height = ''
  wrapper.style.marginBottom = ''
  wrapper.style.zoom = ''

  const pageW = page.scrollWidth || page.offsetWidth
  const pageH = wrapper.scrollHeight
  const avail = host.clientWidth
  if (pageW <= 0 || avail <= 0) return

  const scale = Math.min(1, (avail - 8) / pageW)
  wrapper.style.transformOrigin = 'top left'
  wrapper.style.transform = `scale(${scale})`
  // transform does not shrink layout box — collapse leftover space
  wrapper.style.marginBottom = `${(scale - 1) * pageH}px`
  wrapper.style.width = `${pageW}px`
}

/**
 * 打开原文件（不跳进无法渲染的空白页）:
 * - PDF → 新标签用浏览器 PDF 查看器（pdfUrl / file，inline）
 * - DOCX → 触发下载（浏览器不能直接显示 docx；用 <a download>，避免 window.open 黑页）
 */
function openFile(doc: CvDocument) {
  if (isPdf(doc)) {
    const url = doc.pdfUrl || doc.fileUrl
    window.open(url, '_blank', 'noopener,noreferrer')
    return
  }
  const a = document.createElement('a')
  a.href = doc.fileUrl
  a.download = doc.originalFilename || 'cv.docx'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

export function CvPage() {
  const [items, setItems] = useState<CvDocument[]>([])
  const [msg, setMsg] = useState<string | null>(null)
  const [ok, setOk] = useState(false)
  const [busy, setBusy] = useState(false)
  const [previewId, setPreviewId] = useState<string | null>(null)
  const [docxBusy, setDocxBusy] = useState(false)
  const [docxError, setDocxError] = useState<string | null>(null)
  const [docxHost, setDocxHost] = useState<HTMLDivElement | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const renderGen = useRef(0)

  async function load() {
    try {
      const data = await api.get<{ items: CvDocument[] }>('/cv/documents')
      setItems(data.items || [])
      setMsg(null)
      setOk(false)
    } catch (e) {
      setOk(false)
      setMsg(e instanceof Error ? e.message : 'Load failed')
    }
  }

  useEffect(() => {
    void load()
  }, [])

  const preview = items.find((d) => d.documentId === previewId) ?? null

  useEffect(() => {
    if (!preview || !isDocx(preview) || !docxHost) {
      return
    }
    const gen = ++renderGen.current
    const host = docxHost
    setDocxBusy(true)
    setDocxError(null)
    host.innerHTML = ''

    const ro = new ResizeObserver(() => {
      if (gen === renderGen.current) fitDocxToWidth(host)
    })
    ro.observe(host)

    ;(async () => {
      try {
        const res = await fetch(preview.fileUrl)
        if (!res.ok) throw new Error(`加载失败 HTTP ${res.status}`)
        const buffer = await res.arrayBuffer()
        if (gen !== renderGen.current) return
        await renderAsync(buffer, host, undefined, {
          className: 'docx',
          inWrapper: true,
          ignoreWidth: false,
          breakPages: true,
          renderHeaders: true,
          renderFooters: true,
          useBase64URL: true,
        })
        if (gen !== renderGen.current) return
        fitDocxToWidth(host)
        requestAnimationFrame(() => {
          if (gen === renderGen.current) fitDocxToWidth(host)
        })
        // images/fonts can change measured width shortly after first paint
        window.setTimeout(() => {
          if (gen === renderGen.current) fitDocxToWidth(host)
        }, 150)
      } catch (e) {
        if (gen === renderGen.current) {
          setDocxError(e instanceof Error ? e.message : 'DOCX preview failed')
        }
      } finally {
        if (gen === renderGen.current) setDocxBusy(false)
      }
    })()

    return () => {
      renderGen.current += 1
      ro.disconnect()
      host.innerHTML = ''
    }
  }, [preview?.documentId, preview?.fileUrl, preview?.fileKind, docxHost])

  async function onUpload(e: FormEvent) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) {
      setMsg('请选择 .pdf 或 .docx 文件')
      setOk(false)
      return
    }
    const lower = file.name.toLowerCase()
    if (!lower.endsWith('.pdf') && !lower.endsWith('.docx')) {
      setMsg('仅支持 .pdf 或 .docx')
      setOk(false)
      return
    }
    const form = new FormData()
    form.append('file', file)
    setBusy(true)
    try {
      const doc = await api.upload<CvDocument>('/cv/documents', form)
      setOk(true)
      setMsg(
        isDocx(doc)
          ? `已保存 DOCX：${doc.displayName}`
          : `已保存 PDF：${doc.displayName}`,
      )
      if (fileRef.current) fileRef.current.value = ''
      await load()
      setPreviewId(doc.documentId)
    } catch (err) {
      setOk(false)
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Upload failed')
    } finally {
      setBusy(false)
    }
  }

  async function remove(doc: CvDocument) {
    if (!confirm(`删除「${doc.displayName}」？`)) return
    try {
      await api.delete(`/cv/documents/${doc.documentId}`)
      if (previewId === doc.documentId) setPreviewId(null)
      await load()
    } catch (err) {
      setOk(false)
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Delete failed')
    }
  }

  return (
    <div className="page">
      <div className="topbar">
        <h1>CV</h1>
        <span style={{ fontSize: 12, color: 'var(--ink-muted)' }}>
          预览：页内 · 打开：PDF 新标签 / DOCX 下载
        </span>
        <div className="spacer" />
        <button className="btn btn-sm" type="button" onClick={() => void load()}>
          刷新
        </button>
      </div>
      {msg && <div className={`banner ${ok ? 'banner-ok' : 'banner-error'}`}>{msg}</div>}
      <div className="main-col">
        <section className="section">
          <div className="section-head">
            <h2>Upload</h2>
          </div>
          <form className="cv-upload" onSubmit={(e) => void onUpload(e)}>
            <input
              ref={fileRef}
              type="file"
              accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            />
            <button className="btn btn-primary" type="submit" disabled={busy}>
              {busy ? '上传中…' : '上传'}
            </button>
            <p className="hint">
              DOCX / PDF 原样保存。「预览」在本页查看；「打开」：PDF 用浏览器打开，DOCX 下载到本地（浏览器无法直接显示 Word）。
            </p>
          </form>
        </section>

        <section className="section">
          <div className="section-head">
            <h2>Documents</h2>
            <span className="count">{items.length}</span>
          </div>
          {items.length === 0 ? (
            <p className="hint">暂无简历文件</p>
          ) : (
            <ul className="cv-list">
              {items.map((doc) => (
                <li key={doc.documentId} className={previewId === doc.documentId ? 'active' : undefined}>
                  <div className="cv-meta">
                    <strong>
                      {doc.displayName}
                      <span className="badge" style={{ marginLeft: 8 }}>
                        {doc.fileKind || (doc.pdfReady ? 'PDF' : 'DOCX')}
                      </span>
                    </strong>
                    <span>
                      {formatTime(doc.createdAt)} · {formatBytes(doc.sizeBytesOriginal)}
                    </span>
                  </div>
                  <div className="cv-actions">
                    <button className="btn btn-sm" type="button" onClick={() => setPreviewId(doc.documentId)}>
                      预览
                    </button>
                    <button
                      className="btn btn-sm"
                      type="button"
                      title={isPdf(doc) ? '在新标签打开 PDF' : '下载 DOCX 到本地'}
                      onClick={() => openFile(doc)}
                    >
                      打开
                    </button>
                    <button className="btn btn-sm" type="button" onClick={() => void remove(doc)}>
                      删除
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {preview && (
          <section className="section">
            <div className="section-head">
              <h2>Preview</h2>
              <span className="count">{preview.displayName}</span>
              <button className="btn btn-sm" type="button" onClick={() => openFile(preview)}>
                打开
              </button>
              <button
                className="btn btn-sm link"
                type="button"
                onClick={() => {
                  setPreviewId(null)
                  setDocxHost(null)
                }}
              >
                关闭
              </button>
            </div>
            {isPdf(preview) && preview.pdfUrl && (
              <iframe className="cv-preview" title="CV PDF" src={preview.pdfUrl} />
            )}
            {isDocx(preview) && (
              <div className="cv-preview cv-preview-docx">
                {docxBusy && <p className="hint">渲染 DOCX…</p>}
                {docxError && (
                  <p className="hint" style={{ color: 'var(--danger, #b00020)' }}>
                    {docxError}
                  </p>
                )}
                <div
                  ref={(node) => {
                    setDocxHost((prev) => (prev === node ? prev : node))
                  }}
                  className="cv-docx-host"
                />
              </div>
            )}
          </section>
        )}
      </div>
    </div>
  )
}
