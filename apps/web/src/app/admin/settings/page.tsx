'use client'

import { PartnerListEditor } from '@/components/admin/shared/PartnerListEditor'
import { useMemo, useState } from 'react'
import {
  Check,
  ExternalLink,
  Loader2,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  Save,
} from 'lucide-react'
import { toast } from 'sonner'
import { AdminPageHeader } from '@/components/admin/shared/AdminPageHeader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useAdminSettings, useUpsertSetting } from '@/hooks/useAdmin'
import { GROUPS, valueObject, validateGroup, type SettingsGroup, type SettingsValues, type SettingsValue, type FieldDefinition } from '@/config/admin-settings.config'

export default function SettingsPage() {
  const query = useAdminSettings()
  const saveMutation = useUpsertSetting()
  const [activeGroup, setActiveGroup] = useState<SettingsGroup>('general')
  const [drafts, setDrafts] = useState<Partial<Record<SettingsGroup, SettingsValues>>>({})
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saveError, setSaveError] = useState('')
  const [savedGroup, setSavedGroup] = useState<SettingsGroup | null>(null)

  const settingsByKey = useMemo(
    () => new Map((query.data ?? []).map((setting) => [setting.key, setting])),
    [query.data],
  )

  const savedForms = useMemo(() => Object.fromEntries(GROUPS.map((group) => {
      const saved = settingsByKey.get(group.key)
      return [group.id, { ...group.defaults, ...valueObject(saved?.value) }]
    })) as Record<SettingsGroup, SettingsValues>, [settingsByKey])

  const forms = useMemo(
    () => Object.fromEntries(GROUPS.map((item) => [item.id, { ...savedForms[item.id], ...drafts[item.id] }])) as Record<SettingsGroup, SettingsValues>,
    [drafts, savedForms],
  )

  const group = GROUPS.find((item) => item.id === activeGroup) ?? GROUPS[0]!
  const values = forms[group.id]
  const savedValues = savedForms[group.id]
  const isDirty = JSON.stringify(values) !== JSON.stringify(savedValues)

  function updateField(name: string, value: SettingsValue) {
    setDrafts((current) => ({
      ...current,
      [group.id]: { ...forms[group.id], [name]: value },
    }))
    setErrors((current) => {
      if (!current[name]) return current
      const next = { ...current }
      delete next[name]
      return next
    })
    setSaveError('')
    setSavedGroup(null)
  }

  function changeGroup(next: SettingsGroup) {
    setActiveGroup(next)
    setErrors({})
    setSaveError('')
    setSavedGroup(null)
  }

  async function saveGroup(event: React.FormEvent) {
    event.preventDefault()
    const fieldErrors = validateGroup(group, values)
    setErrors(fieldErrors)
    if (Object.keys(fieldErrors).length > 0) return

    setSaveError('')
    try {
      await saveMutation.mutateAsync({
        key: group.key,
        data: {
          value: values,
          description: group.description,
          isPublic: true,
        },
      })
      setDrafts((current) => {
        const next = { ...current }
        delete next[group.id]
        return next
      })
      setSavedGroup(group.id)
      toast.success(`${group.label} settings saved`)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to save settings'
      setSaveError(message)
      toast.error(message)
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-[1152px] flex-col gap-6 pb-10">
      <AdminPageHeader
        title="Settings"
        action={
          <Button
            variant="outline"
            size="icon"
            aria-label="Refresh settings"
            onClick={() => query.refetch()}
            disabled={query.isFetching}
          >
            <RefreshCw className={query.isFetching ? 'animate-spin' : ''} />
          </Button>
        }
      />

      {query.isError ? (
        <div role="alert" className="rounded-2xl border border-red-200 bg-white px-6 py-12 text-center shadow-sm">
          <p className="font-medium text-red-800">Unable to load settings.</p>
          <Button variant="outline" className="mt-4" onClick={() => query.refetch()}>Try again</Button>
        </div>
      ) : query.isLoading ? (
        <SettingsSkeleton />
      ) : (
        <Tabs value={activeGroup} onValueChange={(value) => changeGroup(value as SettingsGroup)} className="gap-5">
          <TabsList
            variant="line"
            className="h-auto w-full justify-start overflow-x-auto border-b border-[#e5e7eb] px-0 pb-0"
          >
            {GROUPS.map((item) => {
              const Icon = item.icon
              return (
                <TabsTrigger
                  key={item.id}
                  value={item.id}
                  className="h-12 flex-none gap-2 rounded-none px-4 text-[#667085] data-active:text-[#1c3873] after:bg-[#1c3873]"
                >
                  <Icon className="h-4 w-4" />
                  {item.label}
                </TabsTrigger>
              )
            })}
          </TabsList>

          {GROUPS.map((item) => (
            <TabsContent key={item.id} value={item.id} className="m-0">
              <form onSubmit={saveGroup} className="overflow-hidden rounded-2xl bg-white shadow-[0px_2px_8px_rgba(16,24,40,0.06)]">
                <div className="flex items-start gap-4 border-b border-[#eef0f4] px-6 py-5">
                  <div className="rounded-xl bg-[#edf4ff] p-3 text-[#3b82f6]">
                    <item.icon className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="font-heading text-xl font-semibold text-[#1c3873]">{item.label}</h2>
                  </div>
                </div>

                <div className="grid gap-5 p-6 md:grid-cols-2">
                  {item.fields.map((field) => (
                    <SettingsField
                      key={field.name}
                      field={field}
                      value={forms[item.id][field.name] ?? ''}
                      error={item.id === activeGroup ? errors[field.name] : undefined}
                      onChange={(value) => updateField(field.name, value)}
                    />
                  ))}
                </div>

                {saveError && (
                  <div role="alert" className="mx-6 mb-5 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                    {saveError}
                  </div>
                )}

                <div className="flex flex-col gap-3 border-t border-[#eef0f4] bg-[#fbfcfe] px-6 py-4 sm:flex-row sm:items-center sm:justify-end">
                  {savedGroup === item.id && (
                    <span className="inline-flex items-center gap-1.5 text-sm font-medium text-emerald-700">
                      <Check className="h-4 w-4" /> Saved
                    </span>
                  )}
                  <Button type="submit" disabled={saveMutation.isPending || !isDirty} className="btn-blue-gradient min-w-32 text-white">
                    {saveMutation.isPending ? <Loader2 className="animate-spin" /> : <Save />}
                    {saveMutation.isPending ? 'Saving...' : 'Save changes'}
                  </Button>
                </div>
              </form>
            </TabsContent>
          ))}
        </Tabs>
      )}
    </div>
  )
}

function SettingsField({
  field,
  value,
  error,
  onChange,
}: {
  field: FieldDefinition
  value: SettingsValue
  error?: string
  onChange: (value: SettingsValue) => void
}) {
  const isWide = field.type === 'textarea' || field.type === 'partners'
  const inputId = `setting-${field.name}`
  const Icon = field.type === 'email'
    ? Mail
    : field.type === 'tel'
      ? Phone
      : field.type === 'url'
        ? ExternalLink
        : field.name.toLowerCase().includes('address')
          ? MapPin
          : null

  if (field.type === 'toggle') return <label className="flex items-center gap-3 rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-[#344054]"><input id={inputId} type="checkbox" role="switch" checked={value === true} onChange={e => onChange(e.target.checked)} className="h-4 w-4 accent-[#1c3873]" />{field.label}</label>
  if (field.type === 'partners') return <div className="grid gap-3 md:col-span-2"><h3 className="font-heading text-lg font-semibold text-[#1c3873]">{field.label}</h3><PartnerListEditor value={value} onChange={onChange} />{error && <p role="alert" className="text-sm text-red-600">{error}</p>}</div>

  return (
    <div className={`grid gap-2 ${isWide ? 'md:col-span-2' : ''}`}>
      <Label htmlFor={inputId} className="text-sm font-medium text-[#344054]">
        {field.label}{field.required ? ' *' : ''}
      </Label>
      <div className="relative">
        {Icon && <Icon className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-[#98a2b3]" />}
        {field.type === 'textarea' ? (
          <Textarea
            id={inputId}
            value={String(value)}
            onChange={(event) => onChange(event.target.value)}
            placeholder={field.placeholder}
            rows={4}
            aria-invalid={!!error}
            aria-describedby={error ? `${inputId}-error` : undefined}
            className={error ? 'border-red-400 focus-visible:ring-red-200' : ''}
          />
        ) : (
          <Input
            id={inputId}
            type={field.type ?? 'text'}
            value={String(value)}
            onChange={(event) => onChange(event.target.value)}
            placeholder={field.placeholder}
            aria-invalid={!!error}
            aria-describedby={error ? `${inputId}-error` : undefined}
            className={`${Icon ? 'pl-9' : ''} ${error ? 'border-red-400 focus-visible:ring-red-200' : ''}`}
          />
        )}
      </div>
      {error && <p id={`${inputId}-error`} className="text-xs font-medium text-red-600">{error}</p>}
    </div>
  )
}

function SettingsSkeleton() {
  return (
    <div className="space-y-5">
      <div className="flex gap-3 border-b pb-3">
        {[1, 2, 3, 4, 5].map((item) => <Skeleton key={item} className="h-8 w-24" />)}
      </div>
      <div className="rounded-2xl bg-white p-6">
        <Skeleton className="h-7 w-44" />
        <Skeleton className="mt-2 h-4 w-72" />
        <div className="mt-8 grid gap-5 md:grid-cols-2">
          {[1, 2, 3, 4, 5, 6].map((item) => <Skeleton key={item} className="h-10 w-full" />)}
        </div>
      </div>
    </div>
  )
}
