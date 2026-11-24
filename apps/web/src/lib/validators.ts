import { z } from 'zod/v4'

export const emailSchema = z.string().min(1, 'Email is required').email('Enter a valid email')

export const phoneSchema = z
  .string()
  .min(1, 'Phone number is required')
  .regex(/^(\+?880|01)\d{9,10}$/, 'Enter a valid BD phone number')

export const createAccountSchema = z.object({
  fullname: z.string().min(1, 'Full name is required').min(2, 'Name must be at least 2 characters'),
  channel: z.enum(['EMAIL', 'PHONE']),
  email: z.string().optional(),
  phone: z.string().optional(),
}).superRefine((data, ctx) => {
  if (data.channel === 'EMAIL' && !data.email?.trim()) {
    ctx.addIssue({ code: 'custom', message: 'Email is required', path: ['email'] })
  }
  if (data.channel === 'PHONE' && !data.phone?.trim()) {
    ctx.addIssue({ code: 'custom', message: 'Phone number is required', path: ['phone'] })
  }
})

export const passwordSchema = z.object({
  password: z
    .string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Z]/, 'One uppercase letter')
    .regex(/[a-z]/, 'One lowercase letter')
    .regex(/[0-9]/, 'One number'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  agreedToTerms: z.literal(true, { message: 'You must agree to the terms' }),
}).superRefine((data, ctx) => {
  if (data.password !== data.confirmPassword) {
    ctx.addIssue({ code: 'custom', message: 'Passwords do not match', path: ['confirmPassword'] })
  }
})

export type CreateAccountInput = z.infer<typeof createAccountSchema>
export type PasswordInput = z.infer<typeof passwordSchema>
