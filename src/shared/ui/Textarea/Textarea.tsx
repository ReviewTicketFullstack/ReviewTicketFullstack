import type { TextareaHTMLAttributes } from 'react';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export function Textarea({ label, error, id, className = '', rows = 4, ...props }: TextareaProps) {
  const textareaId = id || `textarea-${Math.random().toString(36).slice(2)}`;
  const borderClass = error ? 'border-brand-900' : 'border-line-100 focus-within:border-brand-800';

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
        className={`rounded-lg border px-3 py-2.5 font-sans text-sm text-ink-900 placeholder-ink-500 transition-colors duration-150 focus:outline-none disabled:cursor-not-allowed disabled:bg-fill-100 disabled:text-ink-500 resize-none ${borderClass} ${className}`}
        {...props}
      />
      {error && <p className="text-xs text-brand-900">{error}</p>}
    </div>
  );
}
