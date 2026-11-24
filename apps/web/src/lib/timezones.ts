export const TIMEZONES = getTimezones()

function getTimezones(): string[] {
  const supported = typeof Intl.supportedValuesOf === 'function'
    ? Intl.supportedValuesOf('timeZone')
    : ['UTC', 'Asia/Dhaka', 'Asia/Kolkata', 'Asia/Dubai', 'Europe/London', 'America/New_York', 'America/Los_Angeles']
  return ['Asia/Dhaka', 'UTC', ...supported.filter((zone) => zone !== 'Asia/Dhaka' && zone !== 'UTC')]
}
