'use client';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState, useRef } from 'react';
import { Bell, ChevronDown, LogOut, LayoutDashboard, Search } from 'lucide-react';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import * as Avatar from '@radix-ui/react-avatar';
import { useAuthStore } from '@/stores/authStore';
import { useLogout } from '@/hooks/useAuth';
import { useWebSocket } from '@/hooks/useWebSocket';
import { useNotificationStore } from '@/stores/notificationStore';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

function NotificationBell() {
  const latest = useNotificationStore((s) => s.latest);
  const clearNotification = useNotificationStore((s) => s.clear);
  const [open, setOpen] = useState(false);

  // Establish STOMP connection at root level
  useWebSocket();

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className={cn(
          'relative flex h-9 w-9 items-center justify-center rounded-full transition-colors',
          'hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
          latest && 'text-blue-600'
        )}
        aria-label="Thông báo"
      >
        <Bell className="h-5 w-5" />
        {latest && (
          <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-red-500" />
        )}
      </button>

      {open && latest && (
        <div className="absolute right-0 top-10 z-50 w-72 rounded-xl border border-gray-200 bg-white p-4 shadow-xl">
          <p className="mb-1 text-xs font-semibold uppercase tracking-wider text-gray-500">
            Thông báo mới nhất
          </p>
          <p className="text-sm text-gray-800">{latest.message}</p>
          <p className="mt-1 text-xs text-gray-400">
            {new Date(latest.timestamp).toLocaleString('vi-VN')}
          </p>
          <button
            className="mt-2 text-xs text-blue-500 hover:underline"
            onClick={() => { clearNotification(); setOpen(false); }}
          >
            Đóng
          </button>
        </div>
      )}
    </div>
  );
}

function UserMenu() {
  const { user } = useAuthStore();
  const { mutate: logout, isPending } = useLogout();

  const initials = user?.fullName
    ? user.fullName.split(' ').map((w) => w[0]).join('').slice(0, 2).toUpperCase()
    : user?.email?.slice(0, 2).toUpperCase() ?? 'U';

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button
          className="flex items-center gap-2 rounded-full p-1 hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          aria-label="Tài khoản"
        >
          <Avatar.Root className="h-8 w-8 overflow-hidden rounded-full">
            {user?.avatarUrl ? (
              <Avatar.Image
                src={user.avatarUrl}
                alt={user.fullName ?? user.email}
                className="h-full w-full object-cover"
              />
            ) : null}
            <Avatar.Fallback className="flex h-full w-full items-center justify-center bg-blue-600 text-xs font-semibold text-white">
              {initials}
            </Avatar.Fallback>
          </Avatar.Root>
          <ChevronDown className="h-4 w-4 text-gray-500" />
        </button>
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          className="z-50 min-w-48 overflow-hidden rounded-xl border border-gray-200 bg-white p-1 shadow-xl"
          sideOffset={8}
          align="end"
        >
          <div className="px-3 py-2 border-b border-gray-100 mb-1">
            <p className="text-sm font-semibold text-gray-900 truncate">
              {user?.fullName ?? 'Người dùng'}
            </p>
            <p className="text-xs text-gray-500 truncate">{user?.email}</p>
          </div>

          <DropdownMenu.Item asChild>
            <Link
              href="/dashboard"
              className="flex cursor-pointer items-center gap-2 rounded-md px-3 py-2 text-sm text-gray-700 outline-none hover:bg-gray-50"
            >
              <LayoutDashboard className="h-4 w-4" />
              Dashboard
            </Link>
          </DropdownMenu.Item>

          <DropdownMenu.Separator className="my-1 h-px bg-gray-100" />

          <DropdownMenu.Item asChild>
            <button
              onClick={() => logout()}
              disabled={isPending}
              className="flex w-full cursor-pointer items-center gap-2 rounded-md px-3 py-2 text-sm text-red-600 outline-none hover:bg-red-50 disabled:opacity-50"
            >
              <LogOut className="h-4 w-4" />
              {isPending ? 'Đang đăng xuất...' : 'Đăng xuất'}
            </button>
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}

function QuickSearch() {
  const router = useRouter();
  const [value, setValue] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed) return;
    // URL detection
    if (/^https?:\/\//.test(trimmed)) {
      router.push(`/?url=${encodeURIComponent(trimmed)}`);
    } else {
      router.push(`/search?q=${encodeURIComponent(trimmed)}`);
    }
    setValue('');
    inputRef.current?.blur();
  };

  return (
    <form onSubmit={handleSubmit} className="hidden sm:flex items-center">
      <div className="flex items-center gap-2 rounded-full border border-gray-200 bg-gray-50 px-3 py-1.5 w-64 focus-within:border-blue-400 focus-within:bg-white transition-colors">
        <Search className="h-4 w-4 text-gray-400 shrink-0" />
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Dán URL hoặc tìm theo tên..."
          className="flex-1 bg-transparent text-sm outline-none placeholder:text-gray-400"
        />
      </div>
    </form>
  );
}

export function Header() {
  const { isAuthenticated } = useAuthStore();

  return (
    <header className="sticky top-0 z-40 border-b border-gray-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6">
        {/* Logo */}
        <Link
          href="/"
          className="flex items-center gap-2 font-bold text-blue-600 text-lg shrink-0"
        >
          🦅 PriceHawk
        </Link>

        {/* Quick search */}
        <QuickSearch />

        {/* Right side */}
        <div className="flex items-center gap-2">
          {isAuthenticated ? (
            <>
              <NotificationBell />
              <UserMenu />
            </>
          ) : (
            <>
              <Button asChild variant="ghost" size="sm">
                <Link href="/auth/login">Đăng nhập</Link>
              </Button>
              <Button asChild size="sm">
                <Link href="/auth/register">Đăng ký</Link>
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
