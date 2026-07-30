import { useId, type TextareaHTMLAttributes } from 'react';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export function Textarea({ label, error, id, className = '', rows = 4, ...props }: TextareaProps) {
  const generatedId = useId();
  const textareaId = id ?? generatedId;
  const errorId = `${textareaId}-error`;

  const borderClass = error ? 'border-brand-900' : 'border-line-100 focus:border-brand-800';

  return (
    <div className="flex flex-col gap-2">
      {label && (
        <label htmlFor={textareaId} className="text-sm font-semibold text-ink-900">
          {label}
        </label>
      )}

      <textarea
        id={textareaId}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`resize-none rounded-lg border px-3 py-2 text-sm text-ink-900 placeholder-ink-500 transition-colors duration-150 focus:outline-none disabled:cursor-not-allowed disabled:bg-fill-100 disabled:text-ink-500 ${borderClass} ${className}`}
        {...props}
      />

      {error && (
        <p id={errorId} className="text-xs text-brand-900">
          {error}
        </p>
      )}
    </div>
  );
}
