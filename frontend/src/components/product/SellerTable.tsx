'use client';
import { useState, useMemo } from 'react';
import { ExternalLink, Star, ArrowUpDown } from 'lucide-react';
import type { SellerListing, ScraperTier } from '@/types';
import { formatVND, cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

type SortKey = 'price' | 'trustScore';
type SortDir = 'asc' | 'desc';

const TIER_CONFIG: Record<ScraperTier, { label: string; className: string }> = {
  API_BASED: { label: 'API', className: 'bg-green-100 text-green-800 border-green-200' },
  CONFIG_BASED: { label: 'Config', className: 'bg-blue-100 text-blue-800 border-blue-200' },
  AI_GENERIC: { label: 'AI', className: 'bg-purple-100 text-purple-800 border-purple-200' },
};

function TierBadge({ tier }: { tier: ScraperTier }) {
  const cfg = TIER_CONFIG[tier];
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium',
        cfg.className
      )}
    >
      {cfg.label}
    </span>
  );
}

function TrustBadge({ score }: { score: number | null }) {
  if (score == null)
    return <span className="text-xs text-gray-400">N/A</span>;

  const pct = Math.round(score * 100);
  const colorClass =
    score >= 0.75
      ? 'text-green-700 bg-green-50 border-green-200'
      : score >= 0.5
      ? 'text-amber-700 bg-amber-50 border-amber-200'
      : 'text-red-700 bg-red-50 border-red-200';

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold',
        colorClass
      )}
    >
      {pct}%
    </span>
  );
}

interface SellerTableProps {
  listings: SellerListing[];
  onSelectListing?: (listingId: string) => void;
  selectedListingId?: string | null;
}

export function SellerTable({
  listings,
  onSelectListing,
  selectedListingId,
}: SellerTableProps) {
  const [sortKey, setSortKey] = useState<SortKey>('price');
  const [sortDir, setSortDir] = useState<SortDir>('asc');

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const sorted = useMemo(() => {
    return [...listings].sort((a, b) => {
      let aVal: number;
      let bVal: number;
      if (sortKey === 'price') {
        aVal = a.currentPrice ?? Infinity;
        bVal = b.currentPrice ?? Infinity;
      } else {
        aVal = a.trustScore ?? 0;
        bVal = b.trustScore ?? 0;
      }
      return sortDir === 'asc' ? aVal - bVal : bVal - aVal;
    });
  }, [listings, sortKey, sortDir]);

  if (listings.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-gray-500">
        Chưa có người bán nào được liệt kê.
      </p>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-gray-200">
      <table className="w-full text-sm">
        <thead className="bg-gray-50 text-gray-600">
          <tr>
            <th className="px-4 py-3 text-left font-medium">Người bán</th>
            <th className="px-4 py-3 text-left font-medium">Nền tảng</th>
            <th className="px-4 py-3 text-left font-medium">
              <button
                onClick={() => toggleSort('price')}
                className="flex items-center gap-1 hover:text-gray-900"
              >
                Giá
                <ArrowUpDown className="h-3.5 w-3.5" />
              </button>
            </th>
            <th className="px-4 py-3 text-left font-medium">Khuyến mãi</th>
            <th className="px-4 py-3 text-left font-medium">
              <button
                onClick={() => toggleSort('trustScore')}
                className="flex items-center gap-1 hover:text-gray-900"
              >
                Tin cậy
                <ArrowUpDown className="h-3.5 w-3.5" />
              </button>
            </th>
            <th className="px-4 py-3 text-left font-medium">Nguồn</th>
            <th className="px-4 py-3 text-left font-medium">Xem</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {sorted.map((listing) => (
            <tr
              key={listing.id}
              onClick={() => onSelectListing?.(listing.id)}
              className={cn(
                'cursor-pointer transition-colors hover:bg-gray-50',
                selectedListingId === listing.id && 'bg-blue-50 hover:bg-blue-50'
              )}
            >
              {/* Seller name */}
              <td className="px-4 py-3">
                <div className="flex items-center gap-1.5">
                  {listing.isOfficialStore && (
                    <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400 shrink-0" />
                  )}
                  <span className="font-medium text-gray-900 truncate max-w-[140px]">
                    {listing.sellerName}
                  </span>
                </div>
              </td>

              {/* Domain */}
              <td className="px-4 py-3 text-gray-600 text-xs">{listing.domain}</td>

              {/* Price */}
              <td className="px-4 py-3">
                <div className="flex flex-col">
                  <span className="font-bold text-green-700">
                    {formatVND(listing.currentPrice)}
                  </span>
                  {listing.originalPrice && listing.originalPrice > (listing.currentPrice ?? 0) && (
                    <span className="text-xs text-gray-400 line-through">
                      {formatVND(listing.originalPrice)}
                    </span>
                  )}
                </div>
              </td>

              {/* Promotion */}
              <td className="px-4 py-3 text-xs text-gray-600 max-w-[120px] truncate">
                {listing.promotionInfo ?? '—'}
              </td>

              {/* Trust */}
              <td className="px-4 py-3">
                <TrustBadge score={listing.trustScore} />
              </td>

              {/* Tier */}
              <td className="px-4 py-3">
                <TierBadge tier={listing.scraperTier} />
              </td>

              {/* Action */}
              <td className="px-4 py-3">
                <Button
                  size="sm"
                  variant="outline"
                  className="gap-1 text-xs h-7 px-2"
                  onClick={(e) => {
                    e.stopPropagation();
                    window.open(listing.externalUrl, '_blank', 'noopener,noreferrer');
                  }}
                >
                  <ExternalLink className="h-3 w-3" />
                  Mua ngay
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
