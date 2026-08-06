import { createContext, useContext, useState, type ReactNode } from 'react';

interface StoreLogoContextType {
  logo: string | null;
  setLogo: (logo: string) => void;
}

const StoreLogoContext = createContext<StoreLogoContextType | null>(null);

// 가게 로고를 Sidebar/StoreManagementPage가 같이 보게 하는 Owner 스코프 전용 상태
export function StoreLogoProvider({ children }: { children: ReactNode }) {
  const [logo, setLogo] = useState<string | null>(null);
  return (
    <StoreLogoContext.Provider value={{ logo, setLogo }}>
      {children}
    </StoreLogoContext.Provider>
  );
}

export function useStoreLogo() {
  const context = useContext(StoreLogoContext);
  if (!context) throw new Error('useStoreLogo는 StoreLogoProvider 내부에서만 사용할 수 있습니다.');
  return context;
}
