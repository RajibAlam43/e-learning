/** Convert an instant to the wall-clock value displayed by datetime-local. */
export function toDateTimeInput(iso: string | undefined, timeZone: string): string {
  if (!iso) return ''
  const instant = new Date(iso)
  if (!Number.isFinite(instant.getTime())) return ''
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone, year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).formatToParts(instant)
  const value = (type: string) => parts.find(part => part.type === type)?.value ?? ''
  return `${value('year')}-${value('month')}-${value('day')}T${value('hour')}:${value('minute')}`
}

/** Interpret the input in the course timezone, preserving unchanged instants exactly. */
export function dateTimeInputToIso(value: string, timeZone: string, original?: string): string | undefined {
  if (!value) return undefined
  if (original && value === toDateTimeInput(original, timeZone)) return original
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) throw new Error('Enter a valid date and time.')
  const desired = Date.parse(`${value}:00Z`)
  if (!Number.isFinite(desired)) throw new Error('Enter a valid date and time.')
  let instant = desired
  for (let attempt = 0; attempt < 4; attempt++) {
    const displayed = toDateTimeInput(new Date(instant).toISOString(), timeZone)
    if (displayed === value) return new Date(instant).toISOString()
    instant += desired - Date.parse(`${displayed}:00Z`)
  }
  throw new Error('This time does not exist in the selected timezone. Choose another time.')
}
