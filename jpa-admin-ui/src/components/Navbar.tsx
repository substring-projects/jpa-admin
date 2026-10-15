import React from 'react';
import { useAdmin } from '../context/AdminContext';
import { Sun, Moon, LayoutDashboard, ChevronRight, ShieldCheck } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';

export const Navbar: React.FC = () => {
  const { isDark, toggleTheme, currentEntity, currentView, navigateToDashboard, navigateToList } = useAdmin();

  return (
    <header className="h-16 bg-background/80 backdrop-blur-md sticky top-0 z-30 px-3 sm:px-6 flex items-center justify-between gap-2">
      {/* Left: Breadcrumbs */}
      <nav aria-label="Breadcrumb" className="flex items-center gap-2 min-w-0 overflow-hidden text-xs sm:text-sm text-muted-foreground">
        <Button variant="ghost" size="sm"
          onClick={navigateToDashboard}
          className="flex items-center gap-1.5 font-medium hover:text-foreground transition-colors"
        >
          <LayoutDashboard className="w-4 h-4 text-primary" />
          <span className="hidden sm:inline">Dashboard</span><span className="sr-only sm:hidden">Dashboard</span>
        </Button>

        {currentEntity && (
          <>
            <ChevronRight className="w-3.5 h-3.5 text-muted-foreground/60" />
            <Button variant="ghost" size="sm"
              onClick={() => navigateToList(currentEntity.entityName)}
              className="min-w-0 truncate block px-2 font-medium"
            >
              {currentEntity.pluralName || currentEntity.displayName}
            </Button>
          </>
        )}

        {currentView === 'create' && (
          <>
            <ChevronRight className="w-3.5 h-3.5 text-muted-foreground/60" />
            <span className="text-foreground font-semibold whitespace-nowrap">New Record</span>
          </>
        )}

        {currentView === 'edit' && (
          <>
            <ChevronRight className="w-3.5 h-3.5 text-muted-foreground/60" />
            <span className="text-foreground font-semibold whitespace-nowrap">Edit Record</span>
          </>
        )}
      </nav>

      {/* Right: Actions & Theme toggle */}
      <div className="flex shrink-0 items-center gap-3">
        <Badge variant="outline" className="hidden xl:flex items-center gap-1.5 py-1 px-3 border-emerald-500/30 text-emerald-600 dark:text-emerald-400 bg-emerald-500/10">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Spring Boot JPA Admin</span>
        </Badge>

        <Button
          variant="ghost"
          size="icon"
          onClick={toggleTheme}
          aria-label="Toggle dark mode"
          className="rounded-xl text-muted-foreground hover:text-foreground"
        >
          {isDark ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5" />}
        </Button>
      </div>
    </header>
  );
};
