import { useState, useEffect } from 'react'
import './App.css'
import Header from './components/Header'
import FormCard from './components/FormCard'
import ResultsCard from './components/ResultsCard'
import HowToUse from './components/HowToUse'
import { demoResume, demoJobPosting } from './data/demoData'

function App() {
  const [resumeText, setResumeText] = useState('')
  const [jobPostingText, setJobPostingText] = useState('')
  const [includeCoverLetter, setIncludeCoverLetter] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)

  const handleDemo = () => {
    setResumeText(demoResume)
    setJobPostingText(demoJobPosting)
    setError('')
    setResult(null)
  }

  const handleAnalyze = async () => {
    setError('')
    setResult(null)

    if (!resumeText.trim() || !jobPostingText.trim()) {
      setError('Please fill in both resume and job posting fields.')
      return
    }

    setLoading(true)

    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/ai/analyze`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          resumeText,
          jobPostingText,
          includeCoverLetter,
        }),
      })

      if (!response.ok) {
        if (response.status === 400) {
          setError('Please check your input and try again.')
        } else {
          setError('Something went wrong. Please try again later.')
        }
        setLoading(false)
        return
      }

      const data = await response.json()
      setResult(data)
    } catch (err) {
      setError('Something went wrong. Please try again later.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(()=>{
    // Respect prefers-reduced-motion
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if(prefersReduced){
      document.body.setAttribute('data-loaded','true')
      return
    }
    // small timeout to allow mount paint before animating
    const t = setTimeout(()=> document.body.setAttribute('data-loaded','true'), 80)
    return ()=> clearTimeout(t)
  },[])

  return (
    <div className="app-container">
      <div className="content">
        <Header title="JobMatch." subtitle="See how well your resume matches a job posting." />

        <FormCard
          resumeText={resumeText}
          setResumeText={setResumeText}
          jobPostingText={jobPostingText}
          setJobPostingText={setJobPostingText}
          includeCoverLetter={includeCoverLetter}
          setIncludeCoverLetter={setIncludeCoverLetter}
          handleAnalyze={handleAnalyze}
          handleDemo={handleDemo}
          loading={loading}
          error={error}
        />

        <ResultsCard result={result} />

        <HowToUse />
      </div>
    </div>
  )
}

export default App
