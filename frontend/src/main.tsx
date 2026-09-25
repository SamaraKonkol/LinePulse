import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { iconLogo } from './brand/iconAsset';
import './styles.css';
import './work-orders.css';
import './incident-history.css';
import './maintenance-history.css';
import './alerts.css';
import './audit.css';
import './brand/brand.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
});

const favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]') ?? document.createElement('link');
favicon.rel = 'icon';
favicon.type = 'image/webp';
favicon.href = iconLogo;
if (!favicon.parentNode) document.head.appendChild(favicon);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
);
