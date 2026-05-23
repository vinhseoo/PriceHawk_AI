import type { Metadata } from 'next';
import { ProductDetailClient } from '@/components/product/ProductDetailClient';

interface Props {
  params: { slug: string };
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

interface ProductSummary {
  id: string;
  name: string;
  slug: string;
  brand: string | null;
  description: string | null;
  thumbnailUrl: string | null;
  lowestPrice: number | null;
  sentimentScore: number | null;
  totalReviews: number;
  realReviewRatio: number | null;
}

async function fetchProduct(slug: string): Promise<ProductSummary | null> {
  try {
    const res = await fetch(`${API_URL}/api/v1/products/slug/${slug}`, {
      next: { revalidate: 300 }, // 5-min ISR
    });
    if (!res.ok) return null;
    const body = await res.json();
    return body?.data ?? null;
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const product = await fetchProduct(params.slug);

  if (!product) {
    const readableName = params.slug
      .split('-')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
    return { title: readableName };
  }

  return {
    title: product.name,
    description:
      product.description ??
      `So sánh giá ${product.name} tại PriceHawk AI. ${
        product.lowestPrice != null
          ? `Giá từ ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.lowestPrice)}.`
          : ''
      }`,
    openGraph: {
      title: product.name,
      images: product.thumbnailUrl ? [{ url: product.thumbnailUrl }] : [],
    },
  };
}

export default async function ProductDetailPage({ params }: Props) {
  const product = await fetchProduct(params.slug);

  // JSON-LD structured data for SEO
  const jsonLd = product
    ? {
        '@context': 'https://schema.org',
        '@type': 'Product',
        name: product.name,
        description: product.description ?? undefined,
        image: product.thumbnailUrl ?? undefined,
        brand: product.brand
          ? { '@type': 'Brand', name: product.brand }
          : undefined,
        ...(product.lowestPrice != null && {
          offers: {
            '@type': 'AggregateOffer',
            lowPrice: product.lowestPrice,
            priceCurrency: 'VND',
            offerCount: 1,
          },
        }),
        ...(product.totalReviews > 0 && {
          aggregateRating: {
            '@type': 'AggregateRating',
            ratingValue:
              product.sentimentScore != null
                ? (product.sentimentScore * 5).toFixed(1)
                : undefined,
            reviewCount: product.totalReviews,
          },
        }),
      }
    : null;

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      {jsonLd && (
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      )}
      <ProductDetailClient slug={params.slug} />
    </main>
  );
}
