'use client';
import { useState } from 'react';
import Link from 'next/link';
import { Eye, EyeOff } from 'lucide-react';
import { useRegister } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useToast } from '@/components/ui/toast';

interface FormErrors {
  fullName?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
}

function validate(
  fullName: string,
  email: string,
  password: string,
  confirmPassword: string
): FormErrors {
  const errors: FormErrors = {};
  if (!fullName.trim()) errors.fullName = 'Vui lòng nhập họ tên.';
  if (!email.trim()) {
    errors.email = 'Vui lòng nhập email.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = 'Email không hợp lệ.';
  }
  if (!password) {
    errors.password = 'Vui lòng nhập mật khẩu.';
  } else if (password.length < 8) {
    errors.password = 'Mật khẩu phải có ít nhất 8 ký tự.';
  }
  if (password !== confirmPassword) {
    errors.confirmPassword = 'Mật khẩu xác nhận không khớp.';
  }
  return errors;
}

export default function RegisterPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const { mutate: register, isPending, error } = useRegister();
  const { toast } = useToast();

  const fieldErrors = validate(fullName, email, password, confirmPassword);
  const hasErrors = Object.keys(fieldErrors).length > 0;

  const handleBlur = (field: string) =>
    setTouched((prev) => ({ ...prev, [field]: true }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setTouched({ fullName: true, email: true, password: true, confirmPassword: true });
    if (hasErrors) return;

    register(
      { fullName: fullName.trim(), email: email.trim(), password },
      {
        onError: (err) => {
          toast({
            title: 'Đăng ký thất bại',
            description: err.message || 'Không thể tạo tài khoản. Vui lòng thử lại.',
            variant: 'destructive',
          });
        },
        onSuccess: () => {
          toast({
            title: 'Đăng ký thành công',
            description: 'Tài khoản của bạn đã được tạo. Vui lòng đăng nhập.',
            variant: 'success',
          });
        },
      }
    );
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-8">
      <div className="w-full max-w-md">
        <div className="rounded-2xl bg-white p-8 shadow-lg border border-gray-100">
          {/* Logo */}
          <div className="mb-6 text-center">
            <span className="text-3xl">🦅</span>
            <h1 className="mt-2 text-2xl font-bold text-gray-900">Tạo tài khoản</h1>
            <p className="mt-1 text-sm text-gray-500">
              Tham gia PriceHawk AI để mua sắm thông minh hơn
            </p>
          </div>

          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            {/* Full name */}
            <div>
              <label htmlFor="fullName" className="mb-1 block text-sm font-medium text-gray-700">
                Họ và tên
              </label>
              <Input
                id="fullName"
                type="text"
                autoComplete="name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                onBlur={() => handleBlur('fullName')}
                placeholder="Nguyễn Văn A"
                disabled={isPending}
                className={touched.fullName && fieldErrors.fullName ? 'border-red-400' : ''}
              />
              {touched.fullName && fieldErrors.fullName && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.fullName}</p>
              )}
            </div>

            {/* Email */}
            <div>
              <label htmlFor="reg-email" className="mb-1 block text-sm font-medium text-gray-700">
                Email
              </label>
              <Input
                id="reg-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onBlur={() => handleBlur('email')}
                placeholder="ban@example.com"
                disabled={isPending}
                className={touched.email && fieldErrors.email ? 'border-red-400' : ''}
              />
              {touched.email && fieldErrors.email && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.email}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label htmlFor="reg-password" className="mb-1 block text-sm font-medium text-gray-700">
                Mật khẩu
              </label>
              <div className="relative">
                <Input
                  id="reg-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  onBlur={() => handleBlur('password')}
                  placeholder="Tối thiểu 8 ký tự"
                  disabled={isPending}
                  className={
                    touched.password && fieldErrors.password ? 'border-red-400 pr-10' : 'pr-10'
                  }
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {touched.password && fieldErrors.password && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.password}</p>
              )}
            </div>

            {/* Confirm Password */}
            <div>
              <label htmlFor="confirmPassword" className="mb-1 block text-sm font-medium text-gray-700">
                Xác nhận mật khẩu
              </label>
              <Input
                id="confirmPassword"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                onBlur={() => handleBlur('confirmPassword')}
                placeholder="Nhập lại mật khẩu"
                disabled={isPending}
                className={
                  touched.confirmPassword && fieldErrors.confirmPassword ? 'border-red-400' : ''
                }
              />
              {touched.confirmPassword && fieldErrors.confirmPassword && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.confirmPassword}</p>
              )}
            </div>

            {error && (
              <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700 border border-red-200">
                {error.message || 'Đăng ký thất bại. Vui lòng thử lại.'}
              </p>
            )}

            <Button
              type="submit"
              className="w-full"
              disabled={isPending}
            >
              {isPending ? 'Đang tạo tài khoản...' : 'Đăng ký'}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            Đã có tài khoản?{' '}
            <Link href="/auth/login" className="font-semibold text-blue-600 hover:underline">
              Đăng nhập
            </Link>
          </p>
        </div>
      </div>
    </main>
  );
}
