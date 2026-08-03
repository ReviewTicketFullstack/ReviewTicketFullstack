import { useId, type InputHTMLAttributes, type ReactNode } from 'react';
import { InputHelperText } from '../InputHelperText';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  /** error가 없을 때 showHelperText가 true면 노출되는 안내 문구 */
  helperText?: ReactNode;
  showHelperText?: boolean;
}

export function Input({
  label,
  error,
  helperText,
  showHelperText = false,
  id,
  className = '',
  ...props
}: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;
  const helperId = `${inputId}-helper`;

  const borderClass = error ? 'border-brand-900' : 'border-line-100 focus:border-brand-800';

  const isHelperVisible = !error && showHelperText && Boolean(helperText);

  return (
    <div className="flex flex-col gap-2">
      {label && (
        <label htmlFor={inputId} className="text-sm font-semibold text-ink-900">
          {label}
        </label>
      )}

      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : isHelperVisible ? helperId : undefined}
        className={`h-11 rounded-lg border px-3 text-sm text-ink-900 placeholder-ink-500 transition-colors duration-150 focus:outline-none disabled:cursor-not-allowed disabled:bg-fill-100 disabled:text-ink-500 ${borderClass} ${className}`}
        {...props}
      />

      {error && (
        <InputHelperText id={errorId} variant="error">
          {error}
        </InputHelperText>
      )}

      {isHelperVisible && (
        <InputHelperText id={helperId} variant="info">
          {helperText}
        </InputHelperText>
      )}
    </div>
  );
}
