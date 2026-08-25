import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { useEffect, useRef } from 'react'

type Props = {
  documentId: string
  initialHtml: string
  onChangeHtml: (html: string) => void
  onBlurFlush: () => void
}

/**
 * TipTap editor that does NOT re-apply initialHtml on every autosave.
 * Re-syncing saved HTML back into the editor causes getHTML()/setContent
 * normalization loops and intermittent white screens.
 */
export function DocumentEditor({ documentId, initialHtml, onChangeHtml, onBlurFlush }: Props) {
  const onChangeRef = useRef(onChangeHtml)
  const onBlurRef = useRef(onBlurFlush)
  onChangeRef.current = onChangeHtml
  onBlurRef.current = onBlurFlush

  const editor = useEditor(
    {
      immediatelyRender: false,
      shouldRerenderOnTransaction: false,
      extensions: [StarterKit],
      content: initialHtml || '<p></p>',
      editorProps: {
        attributes: {
          class: 'doc-editor-surface',
        },
        handleDOMEvents: {
          blur: () => {
            onBlurRef.current()
            return false
          },
        },
      },
      onUpdate: ({ editor: ed }) => {
        onChangeRef.current(ed.getHTML())
      },
    },
    [documentId],
  )

  // Load content only when switching documents.
  useEffect(() => {
    if (!editor || editor.isDestroyed) return
    try {
      editor.commands.setContent(initialHtml || '<p></p>', { emitUpdate: false })
    } catch {
      try {
        editor.commands.setContent('<p></p>', { emitUpdate: false })
      } catch {
        /* ignore */
      }
    }
    // intentionally only documentId — not initialHtml
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editor, documentId])

  if (!editor || editor.isDestroyed) {
    return <p className="empty-hint">Loading editor…</p>
  }

  return (
    <div className="doc-editor">
      <div className="doc-toolbar">
        <button
          type="button"
          className={editor.isActive('bold') ? 'on' : undefined}
          onClick={() => editor.chain().focus().toggleBold().run()}
        >
          B
        </button>
        <button
          type="button"
          className={editor.isActive('italic') ? 'on' : undefined}
          onClick={() => editor.chain().focus().toggleItalic().run()}
        >
          I
        </button>
        <button
          type="button"
          className={editor.isActive('bulletList') ? 'on' : undefined}
          onClick={() => editor.chain().focus().toggleBulletList().run()}
        >
          • List
        </button>
        <button
          type="button"
          className={editor.isActive('orderedList') ? 'on' : undefined}
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
        >
          1. List
        </button>
        <button
          type="button"
          className={editor.isActive('heading', { level: 2 }) ? 'on' : undefined}
          onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        >
          H2
        </button>
        <button type="button" onClick={() => editor.chain().focus().unsetAllMarks().clearNodes().run()}>
          Clear
        </button>
      </div>
      <EditorContent editor={editor} />
    </div>
  )
}
