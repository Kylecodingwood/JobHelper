import { type FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api/client'

type Experience = { company: string; title: string; startDate: string; endDate: string; summary: string }
type Education = {
  institutionName: string
  programmeName: string
  startDate: string
  expectedGraduationDate: string
}
type Lang = { languageCode: string; proficiency: string }

type Profile = {
  profileId: string
  profileVersion: number
  lifecycle: string
  nationality?: string
  identityStatus?: string
  identityValidUntil?: string
  targetCountry?: string
  jobSeekingGoal?: string
  educationPeriods: Array<Education & { isPrimary?: boolean }>
  workExperiences: Experience[]
  skills: string[]
  languageProficiencies: Lang[]
}

type FormState = {
  nationality: string
  identityStatus: string
  identityValidUntil: string
  targetCountry: string
  jobSeekingGoal: string
  education: Education
  experiences: Experience[]
  skills: string[]
  languages: Lang[]
}

const emptyForm = (): FormState => ({
  nationality: 'CN',
  identityStatus: 'Stamp 1G',
  identityValidUntil: '2027-03-01',
  targetCountry: 'IE',
  jobSeekingGoal: 'Graduate / junior software roles in Ireland',
  education: {
    institutionName: 'University College Dublin',
    programmeName: 'Computer Science',
    startDate: '2022-09-01',
    expectedGraduationDate: '2026-06-15',
  },
  experiences: [],
  skills: ['Java', 'SQL'],
  languages: [
    { languageCode: 'en', proficiency: 'B2' },
    { languageCode: 'zh', proficiency: 'NATIVE' },
  ],
})

type SaveState = 'idle' | 'saving' | 'saved' | 'error'

export function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [missing, setMissing] = useState(false)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [msg, setMsg] = useState<string | null>(null)
  const [saveState, setSaveState] = useState<SaveState>('idle')
  const [backups, setBackups] = useState<Array<{ backupId: string; backupType: string; createdAt: string }>>([])

  async function load() {
    try {
      const p = await api.get<Profile>('/profile')
      setProfile(p)
      setMissing(false)
      const edu = p.educationPeriods[0]
      setForm({
        nationality: p.nationality || 'CN',
        identityStatus: p.identityStatus || 'Stamp 1G',
        identityValidUntil: p.identityValidUntil || '',
        targetCountry: p.targetCountry || 'IE',
        jobSeekingGoal: p.jobSeekingGoal || '',
        education: {
          institutionName: edu?.institutionName || '',
          programmeName: edu?.programmeName || '',
          startDate: edu?.startDate || '',
          expectedGraduationDate: edu?.expectedGraduationDate || '',
        },
        experiences: (p.workExperiences || []).map((e) => ({
          company: e.company || '',
          title: e.title || '',
          startDate: e.startDate || '',
          endDate: e.endDate || '',
          summary: e.summary || '',
        })),
        skills: p.skills?.length ? [...p.skills] : [''],
        languages: (p.languageProficiencies || []).map((l) => ({
          languageCode: l.languageCode,
          proficiency: l.proficiency,
        })),
      })
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setMissing(true)
        setProfile(null)
        setForm(emptyForm())
        return
      }
      setSaveState('error')
      setMsg(e instanceof Error ? e.message : 'Load failed')
    }
  }

  async function loadBackups() {
    try {
      const data = await api.get<{ items: Array<{ backupId: string; backupType: string; createdAt: string }> }>(
        '/profile/backups',
      )
      setBackups(data.items || [])
    } catch {
      /* ignore */
    }
  }

  useEffect(() => {
    void load()
    void loadBackups()
  }, [])

  async function onSave(e: FormEvent) {
    e.preventDefault()
    setSaveState('saving')
    setMsg(null)
    const body = {
      expectedProfileVersion: missing ? null : profile?.profileVersion,
      nationality: form.nationality,
      identityStatus: form.identityStatus,
      identityValidUntil: form.identityValidUntil || null,
      targetCountry: form.targetCountry,
      jobSeekingGoal: form.jobSeekingGoal,
      educationPeriods: [
        {
          institutionName: form.education.institutionName,
          programmeName: form.education.programmeName,
          startDate: form.education.startDate || null,
          expectedGraduationDate: form.education.expectedGraduationDate || null,
          countryCode: form.targetCountry || 'IE',
          isPrimary: true,
        },
      ],
      workExperiences: form.experiences.filter((x) => x.company.trim()),
      skills: form.skills.map((s) => s.trim()).filter(Boolean),
      languageProficiencies: form.languages.filter((l) => l.languageCode.trim()),
    }
    try {
      const res = await api.put<{ profileVersion: number }>('/profile', body)
      setSaveState('saved')
      setMsg(`画像已保存 · v${res.profileVersion}`)
      await load()
    } catch (err) {
      setSaveState('error')
      setMsg(err instanceof ApiError ? `${err.code}: ${err.message}` : '保存失败')
    }
  }

  return (
    <div className="page">
      <div className="topbar">
        <h1>Profile</h1>
        <span style={{ fontSize: 12, color: 'var(--ink-muted)' }}>
          {missing ? '创建画像' : `v${profile?.profileVersion ?? '-'} · 用户画像 / AI prompt 基座`}
        </span>
        <div className="spacer" />
        <button className="btn btn-sm btn-primary" type="submit" form="profile-form" disabled={saveState === 'saving'}>
          {saveState === 'saving' ? '保存中…' : '保存'}
        </button>
      </div>
      {msg && <div className={`banner ${saveState === 'error' ? 'banner-error' : 'banner-ok'}`}>{msg}</div>}

      <form id="profile-form" className="profile-split" onSubmit={(e) => void onSave(e)}>
        <div>
          <div className="form-card">
            <h3>基础身份</h3>
            <div className="field-row">
              <div className="field">
                <label>国籍</label>
                <input value={form.nationality} onChange={(e) => setForm({ ...form, nationality: e.target.value })} />
              </div>
              <div className="field">
                <label>目标国家</label>
                <input value={form.targetCountry} onChange={(e) => setForm({ ...form, targetCountry: e.target.value })} />
              </div>
            </div>
            <div className="field-row">
              <div className="field">
                <label>身份（含工作许可）</label>
                <select
                  value={form.identityStatus}
                  onChange={(e) => setForm({ ...form, identityStatus: e.target.value })}
                >
                  <option>Stamp 1</option>
                  <option>Stamp 1G</option>
                  <option>Stamp 2</option>
                  <option>EU Citizen</option>
                  <option>Other</option>
                </select>
              </div>
              <div className="field">
                <label>身份有效至</label>
                <input
                  type="date"
                  value={form.identityValidUntil}
                  onChange={(e) => setForm({ ...form, identityValidUntil: e.target.value })}
                />
              </div>
            </div>
          </div>

          <div className="form-card">
            <h3>当前求职目标</h3>
            <textarea
              rows={3}
              style={{ width: '100%' }}
              value={form.jobSeekingGoal}
              onChange={(e) => setForm({ ...form, jobSeekingGoal: e.target.value })}
              placeholder="例如：Dublin graduate backend / full-stack…"
            />
          </div>

          <div className="form-card">
            <h3>教育</h3>
            <div className="field-row">
              <div className="field">
                <label>院校</label>
                <input
                  value={form.education.institutionName}
                  onChange={(e) => setForm({ ...form, education: { ...form.education, institutionName: e.target.value } })}
                />
              </div>
              <div className="field">
                <label>专业</label>
                <input
                  value={form.education.programmeName}
                  onChange={(e) => setForm({ ...form, education: { ...form.education, programmeName: e.target.value } })}
                />
              </div>
            </div>
          </div>

          <div className="form-card">
            <div className="section-head">
              <h3 style={{ margin: 0 }}>工作经验</h3>
              <button
                className="btn btn-sm"
                type="button"
                onClick={() =>
                  setForm({
                    ...form,
                    experiences: [...form.experiences, { company: '', title: '', startDate: '', endDate: '', summary: '' }],
                  })
                }
              >
                + 添加
              </button>
            </div>
            {form.experiences.map((exp, i) => (
              <div key={i} className="role-block">
                <div className="field-row">
                  <div className="field">
                    <label>公司</label>
                    <input
                      value={exp.company}
                      onChange={(e) => {
                        const experiences = [...form.experiences]
                        experiences[i] = { ...exp, company: e.target.value }
                        setForm({ ...form, experiences })
                      }}
                    />
                  </div>
                  <div className="field">
                    <label>职位</label>
                    <input
                      value={exp.title}
                      onChange={(e) => {
                        const experiences = [...form.experiences]
                        experiences[i] = { ...exp, title: e.target.value }
                        setForm({ ...form, experiences })
                      }}
                    />
                  </div>
                </div>
                <textarea
                  rows={2}
                  style={{ width: '100%' }}
                  placeholder="简述"
                  value={exp.summary}
                  onChange={(e) => {
                    const experiences = [...form.experiences]
                    experiences[i] = { ...exp, summary: e.target.value }
                    setForm({ ...form, experiences })
                  }}
                />
                <button
                  className="btn btn-sm"
                  type="button"
                  onClick={() => setForm({ ...form, experiences: form.experiences.filter((_, j) => j !== i) })}
                >
                  删除
                </button>
              </div>
            ))}
          </div>

          <div className="form-card">
            <div className="section-head" style={{ marginBottom: 12 }}>
              <h3 style={{ margin: 0 }}>Skills</h3>
              <button
                className="btn btn-sm"
                type="button"
                onClick={() => setForm({ ...form, skills: [...form.skills, ''] })}
              >
                + 添加
              </button>
            </div>
            {form.skills.map((s, i) => (
              <div key={i} className="field-row">
                <div className="field">
                  <label>技能 #{i + 1}</label>
                  <input
                    value={s}
                    placeholder="e.g. Java"
                    onChange={(e) => {
                      const skills = [...form.skills]
                      skills[i] = e.target.value
                      setForm({ ...form, skills })
                    }}
                  />
                </div>
                <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
                  <button
                    className="btn btn-sm"
                    type="button"
                    disabled={form.skills.length <= 1}
                    onClick={() => setForm({ ...form, skills: form.skills.filter((_, j) => j !== i) })}
                  >
                    删除
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="form-card">
            <div className="section-head">
              <h3 style={{ margin: 0 }}>语言（可多条）</h3>
              <button
                className="btn btn-sm"
                type="button"
                onClick={() =>
                  setForm({ ...form, languages: [...form.languages, { languageCode: '', proficiency: 'B1' }] })
                }
              >
                + 添加
              </button>
            </div>
            {form.languages.map((l, i) => (
              <div key={i} className="field-row">
                <div className="field">
                  <label>语言</label>
                  <input
                    value={l.languageCode}
                    onChange={(e) => {
                      const languages = [...form.languages]
                      languages[i] = { ...l, languageCode: e.target.value }
                      setForm({ ...form, languages })
                    }}
                  />
                </div>
                <div className="field">
                  <label>水平</label>
                  <input
                    value={l.proficiency}
                    onChange={(e) => {
                      const languages = [...form.languages]
                      languages[i] = { ...l, proficiency: e.target.value }
                      setForm({ ...form, languages })
                    }}
                  />
                </div>
                <button
                  className="btn btn-sm"
                  type="button"
                  disabled={form.languages.length <= 1}
                  onClick={() => setForm({ ...form, languages: form.languages.filter((_, j) => j !== i) })}
                >
                  删除
                </button>
              </div>
            ))}
          </div>
        </div>

        <div>
          <div className="side-panel">
            <h3>说明</h3>
            <p>Profile 与 Roadmap / Sources 解耦。搜索词请到 Sources 大搜索框自行填写。</p>
            <Link className="btn btn-sm" to="/jobs/sources">
              去 Sources
            </Link>
          </div>
          <div className="side-panel">
            <h3>备份</h3>
            <button
              className="btn btn-sm"
              type="button"
              onClick={() => {
                void (async () => {
                  try {
                    await api.post('/profile/backups/export', {})
                    setMsg('备份已导出')
                    await loadBackups()
                  } catch (e) {
                    setMsg(e instanceof ApiError ? e.message : '备份失败')
                  }
                })()
              }}
            >
              导出备份
            </button>
            <ul className="term-list">
              {backups.slice(0, 4).map((b) => (
                <li key={b.backupId}>
                  {b.backupType} · {b.createdAt?.slice(0, 19)}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </form>
    </div>
  )
}
