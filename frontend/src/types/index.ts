// ─── Core types — must mirror Java DTOs and Python Pydantic models ─────────────

export type SubscriptionPlan = 'FREE' | 'PREMIUM_USER';
export type AuthProvider = 'LOCAL' | 'GOOGLE' | 'FACEBOOK';
export type QueryType = 'URL' | 'IMAGE' | 'TEXT';
export type SourceType = 'MARKETPLACE' | 'RETAILER' | 'UNKNOWN';
export type ScraperTier = 'API_BASED' | 'CONFIG_BASED' | 'AI_GENERIC';
export type ScrapeStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
export type ConfigStatus = 'ACTIVE' | 'AI_GENERATED' | 'DISABLED';

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface User {
  id: string;
  email: string;
  fullName: string | null;
  avatarUrl: string | null;
  subscriptionPlan: SubscriptionPlan;
  dailySearchCount: number;
  authProvider: AuthProvider;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// ─── Product & Catalog ────────────────────────────────────────────────────────

export interface Category {
  id: string;
  name: string;
  slug: string;
  parentId: string | null;
  level: number;
}

export interface Product {
  id: string;
  name: string;
  slug: string;
  brand: string | null;
  description: string | null;
  thumbnailUrl: string | null;
  categoryId: string | null;
  aiSummary: string | null;
  sentimentScore: number | null;
  totalReviews: number;
  realReviewRatio: number | null;
  lowestPrice: number | null;
  lowestPriceSeller: string | null;
  lowestPriceSource: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSpecs {
  productId: string;
  specs: Record<string, unknown>;
}

export interface SellerListing {
  id: string;
  productId: string;
  domain: string;
  sourceType: SourceType;
  scraperTier: ScraperTier;
  sellerName: string;
  sellerId: string | null;
  sellerUrl: string | null;
  isOfficialStore: boolean;
  externalUrl: string;
  currentPrice: number | null;
  originalPrice: number | null;
  currency: string;
  promotionInfo: string | null;
  trustScore: number | null;
  reviewCount: number;
  averageRating: number | null;
  fakeReviewRatio: number | null;
  lastScrapedAt: string | null;
  scrapeStatus: ScrapeStatus;
}

export interface PriceHistory {
  id: string;
  sellerListingId: string;
  price: number;
  recordedAt: string;
}

export interface Review {
  id: string;
  sellerListingId: string;
  reviewerName: string | null;
  rating: number | null;
  content: string | null;
  reviewDate: string | null;
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL' | null;
  isLikelyFake: boolean;
  fakeReason: string | null;
}

// ─── Scraper Config (Admin) ────────────────────────────────────────────────────

export interface ScraperConfig {
  id: string;
  domain: string;
  name: string;
  config: {
    selectors: Record<string, string>;
    type: 'static' | 'dynamic';
    pagination?: Record<string, string>;
  };
  status: ConfigStatus;
  isActive: boolean;
  successCount: number;
  failCount: number;
  createdBy: 'ADMIN' | 'AI';
  lastUsedAt: string | null;
  createdAt: string;
}

// ─── Scrape Job ────────────────────────────────────────────────────────────────

export interface ScrapeJob {
  jobId: string;
  status: ScrapeStatus;
  message?: string;
  url?: string;
  productSlug?: string;
}

// ─── Search History ───────────────────────────────────────────────────────────

export interface SearchHistory {
  id: string;
  query: string;
  queryType: QueryType;
  createdAt: string;
}

// ─── WebSocket ────────────────────────────────────────────────────────────────

export interface WsNotification {
  type: 'ANALYSIS_COMPLETE' | 'PRICE_ALERT' | 'PRICE_UPDATE';
  productId: string;
  message: string;
  data: Record<string, unknown>;
  timestamp: string;
}

// ─── Wishlist ─────────────────────────────────────────────────────────────────

export interface Wishlist {
  id: string;
  userId: string;
  name: string;
  createdAt: string;
  items: WishlistItem[];
}

export interface WishlistItem {
  id: string;
  wishlistId: string;
  productId: string;
  targetPrice: number | null;
  createdAt: string;
}

// ─── API Response wrappers ────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
  errorCode: string | null;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
