import React, { useState } from 'react';
import { Button } from './components/ui/button';
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from './components/ui/sheet';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './components/ui/card';
import { DashboardSkeleton } from './components/LoadingSkeletons';
import { Menu } from 'lucide-react';
import { useAdmin } from './context/AdminContext';
import { Sidebar } from './components/Sidebar';
import { Navbar } from './components/Navbar';
import { ToastContainer } from './components/ToastContainer';
import { DashboardView } from './views/DashboardView';
import { EntityListView } from './views/EntityListView';
import { EntityFormView } from './views/EntityFormView';


export const App: React.FC = () => {
  const { loading, error, currentView, currentEntity, editingId, refreshMetadata } = useAdmin();

  const [menuOpen, setMenuOpen] = useState(false);

  if (error) {
    return (
      <div className="h-full flex flex-col items-center justify-center p-6 bg-slate-50 dark:bg-slate-950 text-slate-800 dark:text-slate-200">
        <Card className="max-w-md w-full text-center">
          <CardHeader><CardTitle>Failed to connect to Admin API</CardTitle><CardDescription className="break-words">{error}</CardDescription></CardHeader>
          <CardContent><Button onClick={refreshMetadata}>Retry Connection</Button></CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex h-dvh overflow-hidden bg-background text-foreground">
      <div className="hidden md:block"><Sidebar /></div>
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <div className="flex shrink-0 border-b items-center">
          <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
            <SheetTrigger asChild><Button variant="ghost" size="icon" className="md:hidden ml-2 shrink-0" aria-label="Open navigation"><Menu className="w-5 h-5" /></Button></SheetTrigger>
            <SheetContent aria-describedby={undefined}>
              <SheetTitle className="sr-only">Navigation</SheetTitle>
              <Sidebar onNavigate={() => setMenuOpen(false)} />
            </SheetContent>
          </Sheet>
          <div className="flex-1 min-w-0"><Navbar /></div>
        </div>
        <main className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden" aria-busy={loading}>
          {loading && <DashboardSkeleton />}
          {!loading && currentView === 'dashboard' && <DashboardView />}
          {!loading && currentView === 'list' && <EntityListView key={currentEntity?.entityName} />}
          {!loading && (currentView === 'create' || currentView === 'edit') && <EntityFormView key={`${currentEntity?.entityName}:${currentView}:${editingId}`} />}
        </main>
      </div>
      <ToastContainer />
    </div>
  );
};
