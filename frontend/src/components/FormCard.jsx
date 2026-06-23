import React, { useState } from 'react'
import { parsePdf } from '../utils/pdfParser'

export default function FormCard({
  resumeText,
  setResumeText,
  jobPostingText,
  setJobPostingText,
  includeCoverLetter,
  setIncludeCoverLetter,
  handleAnalyze,
  handleDemo,
  loading,
  error,
}){
  const [importName, setImportName] = useState('')
  const [importError, setImportError] = useState('')

  const handleFile = async (file) => {
    setImportError('')
    if (!file) return

    setImportName(file.name)

    // Only attempt to read plain text-based files client-side.
    if (file.type === 'text/plain' || file.name.endsWith('.txt') || file.name.endsWith('.md')) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const text = e.target.result || ''
        setResumeText(text)
      }
      reader.onerror = () => setImportError('Failed to read file')
      reader.readAsText(file)
      return
    }

    // PDFs and DOCX: upload to server-side parse endpoint
    if (file.name.endsWith('.pdf') || file.type === 'application/pdf') {
      setImportError('')

      // If file is reasonably small, attempt client-side parsing
      const MAX_LOCAL_BYTES = 5 * 1024 * 1024 // 5 MB
      if (file.size && file.size <= MAX_LOCAL_BYTES) {
        setImportError('Parsing PDF locally...')
        try {
          const ab = await file.arrayBuffer()
          const text = await parsePdf(ab)
          setImportError('')
          setResumeText(text || '')
          return
        } catch (e) {
          console.error('Local PDF parse failed, will fallback to server', e)
          setImportError('Local PDF parsing failed, uploading to server...')
          // fallthrough to server upload
        }
      }

      // Fallback: upload to server-side parse endpoint
      const fd = new FormData()
      fd.append('file', file)

      const base = (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_API_URL)
        ? import.meta.env.VITE_API_URL
        : 'http://localhost:8080'

      const token = window.localStorage.getItem('token')
        || window.localStorage.getItem('authToken')
        || window.localStorage.getItem('accessToken')
        || window.localStorage.getItem('jwt')

      const headers = {}
      if (token) headers['Authorization'] = `Bearer ${token}`

      fetch(`${base}/api/resumes/parse`, {
        method: 'POST',
        body: fd,
        headers,
      }).then(async (res) => {
        if (!res.ok) {
          const text = await res.text()
          setImportError('Server failed to parse file: ' + (text || res.status))
          return
        }
        const json = await res.json()
        setResumeText(json.rawText || '')
      }).catch((err) => {
        setImportError('Failed to upload for parsing')
      })
      return
    }

    setImportError('Unsupported file type for client import. Upload PDF/DOCX via the backend instead.')
  }

  const onFileChange = (e) => {
    const f = e.target.files && e.target.files[0]
    handleFile(f)
  }

  return (
    <div className="form-card">
      <div className="form-group">
        <label htmlFor="resume">Your Resume</label>

        <div className="file-import">
          <input
            id="resume-file"
            className="file-input-hidden"
            type="file"
            accept=".txt,.md,text/plain,.pdf,.doc,.docx"
            onChange={onFileChange}
            aria-label="Import resume file"
          />
          <label htmlFor="resume-file" className="import-button">Import Resume</label>
          {importName && <div className="import-name">Imported: {importName}</div>}
          {importError && <div className="import-error">{importError}</div>}
        </div>

        <textarea
          id="resume"
          placeholder="Paste your resume text here or import a .txt/.md file..."
          value={resumeText}
          onChange={(e) => setResumeText(e.target.value)}
          rows={8}
        />
      </div>

      <div className="form-group">
        <div className="field-header">
          <label htmlFor="job">Job Posting</label>
          {jobPostingText && (
            <button className="clear-button" onClick={() => setJobPostingText('')} type="button">
              Clear ✕
            </button>
          )}
        </div>
        <textarea
          id="job"
          placeholder="Paste the job posting text here..."
          value={jobPostingText}
          onChange={(e) => setJobPostingText(e.target.value)}
          rows={8}
        />
      </div>

      <div className="form-group checkbox-group">
        <label>
          <input
            type="checkbox"
            checked={includeCoverLetter}
            onChange={(e) => setIncludeCoverLetter(e.target.checked)}
          />
          <span>Include cover letter suggestions</span>
        </label>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="form-actions">
        <button className="demo-button" onClick={handleDemo} disabled={loading}>
          Try Demo
        </button>
        <button className="analyze-button" onClick={handleAnalyze} disabled={loading}>
          {loading ? 'Analyzing...' : 'Analyze'}
        </button>
      </div>
    </div>
  )
}
