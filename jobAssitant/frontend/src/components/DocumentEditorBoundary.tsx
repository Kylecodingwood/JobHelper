import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = {
  documentId: string
  children: ReactNode
}

type State = {
  error: Error | null
}

/** Catches TipTap / editor crashes so the whole Roadmap page doesn't go white. */
export class DocumentEditorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Document editor crashed', error, info.componentStack)
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.documentId !== this.props.documentId && this.state.error) {
      this.setState({ error: null })
    }
  }

  render() {
    if (this.state.error) {
      return (
        <div className="doc-editor-error">
          <p className="empty-hint">编辑器出错了，请换一篇文档或刷新页面重试。</p>
          <button type="button" className="btn btn-sm" onClick={() => this.setState({ error: null })}>
            重试
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
