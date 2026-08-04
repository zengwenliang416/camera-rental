import type { ButtonHTMLAttributes, ReactNode } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'glass';
type ButtonSize = 'sm' | 'md' | 'lg' | 'icon';

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'sc-button-primary',
  secondary: 'sc-button-secondary',
  outline: 'sc-button-outline',
  ghost: 'sc-button-ghost',
  danger: 'sc-button-danger',
  glass: 'sc-glass-control',
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'min-h-9 rounded-lg px-3 text-[11px]',
  md: 'min-h-11 rounded-xl px-4 text-xs',
  lg: 'min-h-12 rounded-xl px-5 text-sm',
  icon: 'h-11 w-11 rounded-xl',
};

export function Button({
  variant = 'outline',
  size = 'md',
  icon,
  children,
  className = '',
  type = 'button',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  icon?: ReactNode;
}) {
  return (
    <button
      type={type}
      className={`sc-button ${variantClasses[variant]} ${sizeClasses[size]} ${className}`}
      {...props}
    >
      {icon}
      {children}
    </button>
  );
}
