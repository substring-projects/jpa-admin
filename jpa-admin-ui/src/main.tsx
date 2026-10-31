import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router';
import { ThemeProvider } from '@/lib/theme';
import { AuthProvider } from '@/context/AuthContext';
import { AdminProvider } from '@/context/AdminContext';
import { Toaster } from '@/components/ui/sonner';
import { TooltipProvider } from '@/components/ui/tooltip';
import { router } from '@/router';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <TooltipProvider delayDuration={300}>
        <AuthProvider>
          <AdminProvider>
            <RouterProvider router={router} />
          </AdminProvider>
        </AuthProvider>
        <Toaster position="bottom-right" richColors closeButton />
      </TooltipProvider>
    </ThemeProvider>
  </StrictMode>
);
