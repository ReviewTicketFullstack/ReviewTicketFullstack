import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react';

interface CardBaseProps {
  children: ReactNode;
  className?: string;
}

/** 영역 전체가 터치 타깃인 카드. button으로 렌더되므로 children은 phrasing content만 둔다. */
type InteractiveCardProps = CardBaseProps &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'className'> & {
    interactive: true;
  };

/** 클릭 대상이 아닌 정보 카드. */
type StaticCardProps = CardBaseProps &
  Omit<HTMLAttributes<HTMLDivElement>, 'children' | 'className'> & {
    interactive?: false;
    /** 배경과의 구분이 필요할 때 Border를 얹는다. */
    bordered?: boolean;
  };

export type CardProps = InteractiveCardProps | StaticCardProps;

const BASE_CLASS = 'rounded-2xl bg-surface p-3';

export function Card(props: CardProps) {
  if (props.interactive) {
    const { children, className = '', interactive, ...rest } = props;
    void interactive;

    return (
      <button
        type="button"
        className={`${BASE_CLASS} shadow-raised text-left transition-transform active:scale-[0.99] ${className}`}
        {...rest}
      >
        {children}
      </button>
    );
  }

  const { children, className = '', interactive, bordered = false, ...rest } = props;
  void interactive;

  return (
    <div
      className={`${BASE_CLASS} ${bordered ? 'border border-line-100' : ''} ${className}`}
      {...rest}
    >
      {children}
    </div>
  );
}
