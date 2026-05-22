import type { Metadata } from 'next';

interface Props {
  params: { slug: string };
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  return { title: params.slug.replace(/-/g, ' ') };
}

export default function ProductDetailPage({ params }: Props) {
  return (
    <main className="container mx-auto px-4 py-8">
      <p className="text-gray-500">Loading product: {params.slug}</p>
      {/* TODO Phase 7: ProductHeader, SellerTable, PriceHistoryChart, ReviewAnalysis */}
    </main>
  );
}
