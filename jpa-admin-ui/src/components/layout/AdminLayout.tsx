import { Fragment, useState } from 'react';
import { Link, Outlet, useLocation, useOutletContext, useParams } from 'react-router';
import { Moon, Sun } from 'lucide-react';
import { useTheme } from '@/lib/theme';
import { useAdmin, useEntity } from '@/context/AdminContext';
import { shortId } from '@/lib/format';
import { Button } from '@/components/ui/button';
import {
  Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { Separator } from '@/components/ui/separator';
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar';
import { AppSidebar } from './AppSidebar';
import { CommandPalette } from './CommandPalette';
import { ShortcutsDialog } from './ShortcutsDialog';
import { ErrorState } from '@/components/PageStates';

interface LayoutContext { setRecordLabel: (label: string) => void }
export const useLayout = () => useOutletContext<LayoutContext>();

function Crumbs({ recordLabel }: { recordLabel: string }) {
  const { entity: entityParam, id } = useParams();
  const entity = useEntity(entityParam);
  const { pathname } = useLocation();
  const crumbs: Array<{ label: string; to?: string }> = [{ label: 'Dashboard', to: '/' }];
  if (entity) {
    crumbs.push({ label: entity.pluralName || entity.displayName, to: `/${entity.entityName}` });
    if (pathname.endsWith('/new')) crumbs.push({ label: `New ${entity.displayName}` });
    if (id) {
      crumbs.push({ label: recordLabel || shortId(id), to: `/${entity.entityName}/${encodeURIComponent(id)}` });
      if (pathname.endsWith('/edit')) crumbs.push({ label: 'Edit' });
    }
  }
  const last = crumbs.length - 1;
  return <Breadcrumb className="min-w-0">
    <BreadcrumbList className="flex-nowrap">
      {crumbs.map((crumb, index) => <Fragment key={index}>
        {index > 0 && <BreadcrumbSeparator className="hidden md:block" />}
        <BreadcrumbItem className={index < last ? 'hidden md:inline-flex' : 'min-w-0'}>
          {index === last || !crumb.to
            ? <BreadcrumbPage className="truncate">{crumb.label}</BreadcrumbPage>
            : <BreadcrumbLink asChild><Link to={crumb.to} className="max-w-48 truncate">{crumb.label}</Link></BreadcrumbLink>}
        </BreadcrumbItem>
      </Fragment>)}
    </BreadcrumbList>
  </Breadcrumb>;
}

function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme();
  const dark = resolvedTheme === 'dark';
  return <Button variant="ghost" size="icon-sm" aria-label={dark ? 'Switch to light theme' : 'Switch to dark theme'} onClick={() => setTheme(dark ? 'light' : 'dark')}>
    {dark ? <Sun /> : <Moon />}
  </Button>;
}

export function AdminLayout() {
  const { error, refreshMetadata } = useAdmin();
  const [recordLabel, setRecordLabel] = useState('');
  const { pathname } = useLocation();
  const [labelPath, setLabelPath] = useState('');
  // Labels belong to the page that set them; stale labels must not leak into the next route's breadcrumbs.
  const label = labelPath === pathname.replace(/\/edit$/, '') ? recordLabel : '';
  const context: LayoutContext = {
    setRecordLabel: (next: string) => { setRecordLabel(next); setLabelPath(pathname.replace(/\/edit$/, '')); },
  };

  return <SidebarProvider>
    <AppSidebar />
    <SidebarInset className="min-w-0">
      <header className="sticky top-0 z-20 flex h-14 shrink-0 items-center gap-2 rounded-t-xl border-b bg-background/95 px-4 backdrop-blur supports-[backdrop-filter]:bg-background/80">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="mr-2 data-[orientation=vertical]:h-4" />
        <Crumbs recordLabel={label} />
        <div className="ml-auto flex items-center gap-1"><ThemeToggle /></div>
      </header>
      <main className="min-w-0 flex-1">
        {error ? <ErrorState title="Couldn't connect to the admin API" description={error} onRetry={refreshMetadata} /> : <Outlet context={context} />}
      </main>
    </SidebarInset>
    <CommandPalette />
    <ShortcutsDialog />
  </SidebarProvider>;
}
