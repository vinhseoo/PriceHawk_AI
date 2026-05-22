# Frontend — CLAUDE.md

## Stack

Next.js 14 App Router, TypeScript, Tailwind CSS, ShadcnUI, TanStack Query, Zustand, Socket.io

## Commands

```bash
cd frontend
npm install
npm run dev      # port 3000
npm run build
npm run lint
npm run type-check  # tsc --noEmit
```

## Architecture Rules

- **Server state** (API data): TanStack Query (`useQuery`, `useMutation`) only. No manual `useState` for API data.
- **Global client state**: Zustand only. Stores in `src/stores/`.
- **Data fetching**: Only inside `src/hooks/` or Server Components. Never fetch in UI components directly.
- All API types in `src/types/index.ts`. Never define ad-hoc interfaces in components.
- Components must be < 200 lines. Extract sub-components if larger.

## File Conventions

```
src/app/          ← Pages (App Router) — layout, page, loading, error
src/components/   ← UI components (never fetch data here)
src/hooks/        ← useXxx hooks — data fetching, WebSocket
src/lib/          ← apiClient, authHelpers, utils
src/stores/       ← Zustand stores
src/types/        ← ALL TypeScript interfaces
```

## API Client Pattern

```typescript
// src/lib/apiClient.ts handles base URL, auth headers, error parsing
import { apiClient } from '@/lib/apiClient';

// In hooks only:
const { data } = useQuery({
  queryKey: ['product', slug],
  queryFn: () => apiClient.get<Product>(`/api/v1/products/${slug}`)
});
```

## Key Types (from src/types/index.ts)

`Product`, `SellerListing`, `ScraperConfig`, `PriceHistory`, `Review`
- `SellerListing.scraperTier`: `'API_BASED' | 'CONFIG_BASED' | 'AI_GENERIC'`
- `SellerListing.sourceType`: `'MARKETPLACE' | 'RETAILER' | 'UNKNOWN'`

## Naming Conventions

- Components: PascalCase (`SellerTable.tsx`)
- Hooks: camelCase with `use` prefix (`useProductDetail.ts`)
- Zustand stores: camelCase file (`authStore.ts`), export `useAuthStore`
- Page routes: kebab-case folders (`/auth/login/`, `/products/[slug]/`)

## ShadcnUI

Add components via: `npx shadcn-ui@latest add <component>`
Installed components are in `src/components/ui/` — never edit these files directly.

## WebSocket (Notification Service)

```typescript
// src/hooks/useWebSocket.ts
const socket = io('http://localhost:8085', { auth: { token } });
socket.on('analysis.completed', (data) => queryClient.invalidateQueries(['product', data.productId]));
```
