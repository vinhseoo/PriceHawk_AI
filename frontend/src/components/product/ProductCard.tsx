import Link from 'next/link';
import Image from 'next/image';
import { ShieldCheck } from 'lucide-react';
import type { Product } from '@/types';
import { formatVND, formatTrustScore } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';

interface ProductCardProps {
  product: Product;
}

function TrustBadge({ score }: { score: number | null }) {
  if (score == null) return null;
  const variant =
    score >= 0.75 ? 'success' : score >= 0.5 ? 'warning' : 'destructive';
  return (
    <Badge variant={variant} className="gap-1">
      <ShieldCheck className="h-3 w-3" />
      {formatTrustScore(score)}
    </Badge>
  );
}

export function ProductCard({ product }: ProductCardProps) {
  const thumbnail = product.thumbnailUrl;

  return (
    <Link
      href={`/products/${product.slug}`}
      className="group flex flex-col overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm transition-shadow hover:shadow-md"
    >
      {/* Thumbnail */}
      <div className="relative aspect-square w-full overflow-hidden bg-gray-100">
        {thumbnail ? (
          <Image
            src={thumbnail}
            alt={product.name}
            fill
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
            className="object-cover transition-transform group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-gray-300 text-4xl">
            📦
          </div>
        )}
      </div>

      {/* Info */}
      <div className="flex flex-col gap-2 p-3">
        {product.brand && (
          <Badge variant="secondary" className="w-fit text-xs">
            {product.brand}
          </Badge>
        )}

        <h3 className="line-clamp-2 text-sm font-medium text-gray-900 leading-snug">
          {product.name}
        </h3>

        <div className="flex items-center justify-between gap-2 mt-auto">
          <span className="text-base font-bold text-green-700">
            {formatVND(product.lowestPrice)}
          </span>
          <TrustBadge score={product.sentimentScore} />
        </div>

        {product.lowestPriceSeller && (
          <p className="text-xs text-gray-400 truncate">
            Tại: {product.lowestPriceSeller}
          </p>
        )}

        <span className="mt-1 inline-flex items-center justify-center rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-semibold text-white transition-colors group-hover:bg-blue-700">
          Xem chi tiết
        </span>
      </div>
    </Link>
  );
}
