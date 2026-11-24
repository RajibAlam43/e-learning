import type { Metadata } from 'next'

export function detailMetadata(title: string, description: string | undefined, canonicalPath: string, image?: string, type: 'website' | 'profile' = 'website'): Metadata {
  const imageUrl = image || '/images/hero-bg.png'
  return {
    title,
    description,
    alternates: { canonical: canonicalPath },
    openGraph: { title, description, url: canonicalPath, type, images: [{ url: imageUrl, alt: title }] },
    twitter: { card: 'summary_large_image', title, description, images: [imageUrl] },
  }
}
