'use client';
import Image from 'next/image';
import { Heart, Loader2, AlertCircle, ShieldCheck } from 'lucide-react';
import type { Product } from '@/types';
import { formatVND } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useAddWishlistItem } from '@/hooks/useWishlist';
import { useAuthStore } from '@/stores/authStore';
import { useToast } from '@/components/ui/toast';

interface ProductHeaderProps {
  product: Product;
}

function TrustScoreRing({ score }: { score: number }) {
  const pct = Math.round(score * 100);
  const radius = 28;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (pct / 100) * circumference;
  const color = score >= 0.75 ? '#16a34a' : score >= 0.5 ? '#d97706' : '#dc2626';

  return (
    <div className="flex flex-col items-center gap-1">
      <svg width="72" height="72" viewBox="0 0 72 72" className="-rotate-90">
        <circle cx="36" cy="36" r={radius} fill="none" stroke="#e5e7eb" strokeWidth="6" />
        <circle
          cx="36"
          cy="36"
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth="6"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
        />
      </svg>
      <span className="text-xs font-semibold" style={{ color }}>
        {pct}% tin cậy
      </span>
    </div>
  );
}

export function ProductHeader({ product }: ProductHeaderProps) {
  const { isAuthenticated } = useAuthStore();
  const { mutate: addToWishlist, isPending: isAddingWishlist } = useAddWishlistItem();
  const { toast } = useToast();

  const handleAddWishlist = () => {
    if (!isAuthenticated) {
      toast({
        title: 'Chưa đăng nhập',
        description: 'Vui lòng đăng nhập để thêm sản phẩm vào Wishlist.',
        variant: 'destructive',
      });
      return;
    }
    addToWishlist(
      { productId: product.id },
      {
        onSuccess: () => {
          toast({ title: 'Đã thêm vào Wishlist', variant: 'success' });
        },
        onError: () => {
          toast({
            title: 'Thêm thất bại',
            description: 'Không thể thêm sản phẩm. Vui lòng thử lại.',
            variant: 'destructive',
          });
        },
      }
    );
  };

  return (
    <div className="flex flex-col gap-6 md:flex-row">
      {/* Thumbnail */}
      <div className="relative h-64 w-full shrink-0 overflow-hidden rounded-xl bg-gray-100 md:h-80 md:w-80">
        {product.thumbnailUrl ? (
          <Image
            src={product.thumbnailUrl}
            alt={product.name}
            fill
            sizes="(max-width: 768px) 100vw, 320px"
            className="object-contain"
            priority
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-6xl text-gray-300">
            📦
          </div>
        )}
      </div>

      {/* Details */}
      <div className="flex flex-1 flex-col gap-3">
        {product.brand && (
          <Badge variant="secondary" className="w-fit">
            {product.brand}
          </Badge>
        )}

        <h1 className="text-2xl font-bold text-gray-900 leading-snug">{product.name}</h1>

        {/* Analysis in-progress banner */}
        {product.aiSummary === null && (
          <div className="flex items-center gap-2 rounded-lg bg-blue-50 border border-blue-200 px-4 py-3 text-sm text-blue-700">
            <Loader2 className="h-4 w-4 animate-spin shrink-0" />
            <span>AI đang phân tích sản phẩm này... Kết quả sẽ xuất hiện tự động.</span>
          </div>
        )}

        {/* Price */}
        {product.lowestPrice != null && (
          <div className="flex items-center gap-3">
            <span className="text-3xl font-bold text-green-700">
              {formatVND(product.lowestPrice)}
            </span>
            {product.lowestPriceSeller && (
              <span className="text-sm text-gray-500">tại {product.lowestPriceSeller}</span>
            )}
          </div>
        )}

        {/* AI summary + trust score */}
        {product.aiSummary && (
          <div className="flex items-start gap-4 rounded-xl bg-gray-50 border border-gray-200 p-4">
            {product.sentimentScore != null && (
              <TrustScoreRing score={product.sentimentScore} />
            )}
            <div className="flex-1">
              <div className="mb-1 flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-blue-600" />
                <span className="text-xs font-semibold uppercase tracking-wide text-blue-600">
                  Phân tích AI
                </span>
              </div>
              <p className="text-sm text-gray-700 leading-relaxed">{product.aiSummary}</p>
            </div>
          </div>
        )}

        {/* Review ratio warning */}
        {product.realReviewRatio != null && product.realReviewRatio < 0.7 && (
          <div className="flex items-center gap-2 rounded-lg bg-amber-50 border border-amber-200 px-4 py-3 text-sm text-amber-800">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>
              Cảnh báo: Chỉ {Math.round(product.realReviewRatio * 100)}% đánh giá được xác nhận
              là thật.
            </span>
          </div>
        )}

        {/* Actions */}
        <div className="mt-2 flex gap-3">
          <Button
            variant="outline"
            onClick={handleAddWishlist}
            disabled={isAddingWishlist}
            className="gap-2"
          >
            {isAddingWishlist ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Heart className="h-4 w-4" />
            )}
            Thêm vào Wishlist
          </Button>
        </div>
      </div>
    </div>
  );
}
