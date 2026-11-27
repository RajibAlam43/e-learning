'use client'
import Link from 'next/link'
import { useState } from 'react'
import { useFacultyAdmin, useFacultyMutation, facultyError, type FacultyGroup, type FacultyMember } from '@/services/faculty.service'
import { FacultyGroupEditor, FacultyPersonEditor } from './FacultyEditors'
const button = 'rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-[#1c3873] hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-50'
export function FacultyAdmin() {
  const query = useFacultyAdmin(), mutation = useFacultyMutation()
  const [editor, setEditor] = useState<{ kind: 'group'; group?: FacultyGroup } | { kind: 'person'; groupId?: string; person?: FacultyMember }>()
  const [error, setError] = useState('')
  async function remove(kind: 'groups' | 'members', id: string, name: string) {
    if (!window.confirm(`Remove ${name}?${kind === 'members' ? ' Linked teacher accounts and profiles are kept.' : ''}`)) return
    setError(''); try { await mutation.mutateAsync({ kind, id }) } catch (cause) { setError(facultyError(cause)) }
  }
  const groups = query.data ?? []
  return <div className="mx-auto grid w-full max-w-5xl min-w-0 gap-6"><header className="flex flex-wrap items-start justify-between gap-4"><div><h1 className="font-heading text-2xl font-semibold text-[#1c3873]">Faculty</h1><p className="mt-1 max-w-prose text-sm text-slate-600">Manage groups, people and where their profiles appear. Changes are public after saving.</p></div><div className="flex flex-wrap gap-2"><Link href="/faculty" className={button}>View faculty page</Link><button className={button} onClick={() => setEditor({ kind: 'group' })}>Add group</button><button disabled={!groups.length} className={button} onClick={() => setEditor({ kind: 'person' })}>Add person</button></div></header>
    {query.isLoading && <p role="status">Loading faculty…</p>}{query.isError && <p role="alert">Faculty could not be loaded. <button onClick={() => query.refetch()} className="underline">Retry</button></p>}{error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    {editor?.kind === 'group' && <FacultyGroupEditor key={editor.group?.id ?? 'new-group'} group={editor.group} nextPosition={Math.max(0, ...groups.map(g => g.position ?? 0)) + 1} close={() => setEditor(undefined)} />}
    {editor?.kind === 'person' && <FacultyPersonEditor key={editor.person?.id ?? `new-person-${editor.groupId ?? ''}`} person={editor.person} groupId={editor.groupId} groups={groups} close={() => setEditor(undefined)} />}
    {!query.isLoading && !query.isError && !groups.length && <p className="rounded-xl border border-dashed border-slate-300 p-8 text-slate-600">Add a group to start your faculty directory.</p>}
    {groups.map(g => <section key={g.id} aria-labelledby={`admin-group-${g.id}`} className="min-w-0 rounded-xl border border-slate-200 bg-white"><header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 p-5"><div><h2 id={`admin-group-${g.id}`} className="font-heading text-lg font-semibold text-[#1c3873]">{g.nameEn || g.name}</h2><p className="text-xs text-slate-500">Position {g.position} · {g.visible ? 'Visible group' : 'Hidden group'}</p></div><div className="flex flex-wrap gap-2"><button className={button} onClick={() => setEditor({ kind: 'person', groupId: g.id })}>Add person to {g.nameEn || g.name}</button><button className={button} onClick={() => setEditor({ kind: 'group', group: g })}>Edit group</button><button disabled={mutation.isPending} className={button} onClick={() => g.id && void remove('groups', g.id, g.nameEn || g.name || 'group')}>Delete group</button></div></header>
      {!g.members?.length ? <p className="p-5 text-sm text-slate-500">No people added yet.</p> : <ul className="divide-y divide-slate-100">{g.members.map(p => <li key={p.id} className="flex min-w-0 flex-wrap items-center justify-between gap-3 p-5"><div className="min-w-0"><h3 className="break-words font-semibold text-slate-800">{p.name}</h3><p className="text-xs text-slate-500">{p.instructorId ? 'Teacher profile' : 'Manual profile'} · Position {p.position}</p><p className="mt-1 text-xs text-slate-600">Faculty: {p.showOnFaculty ? 'shown' : 'hidden'} · Homepage: {p.showOnHomepage ? `shown (${p.homepagePosition})` : 'hidden'}{!p.profilePublic && ' · Teacher is private or inactive'}</p></div><div className="flex gap-2"><button className={button} aria-label={`Edit ${p.name}`} onClick={() => setEditor({ kind: 'person', person: p })}>Edit person</button><button disabled={mutation.isPending} className={button} aria-label={`Remove ${p.name}`} onClick={() => p.id && void remove('members', p.id, p.name || 'person')}>Remove person</button></div></li>)}</ul>}
    </section>)}
  </div>
}
