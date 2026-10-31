import { Link, useMatch, useNavigate } from 'react-router';
import { ChevronRight, ChevronsUpDown, LayoutDashboard, LogOut, Monitor, Moon, Search, Sun } from 'lucide-react';
import { useTheme } from '@/lib/theme';
import { useAdmin } from '@/context/AdminContext';
import { useAuth } from '@/context/AuthContext';
import { BrandMark } from '@/components/BrandMark';
import { EntityIcon } from '@/components/DynamicIcon';
import { formatCount, initials } from '@/lib/format';
import { useStored } from '@/lib/storage';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuRadioGroup,
  DropdownMenuRadioItem, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Kbd } from '@/components/ui/kbd';
import {
  Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarHeader,
  SidebarMenu, SidebarMenuBadge, SidebarMenuButton, SidebarMenuItem, SidebarMenuSkeleton, SidebarRail, useSidebar,
} from '@/components/ui/sidebar';
import { toast } from 'sonner';
import { openCommandPalette } from './CommandPalette';

export function AppSidebar() {
  const { metadata, loading, counts } = useAdmin();
  const { title, logoUrl, username, signOut } = useAuth();
  const { theme, setTheme } = useTheme();
  const { isMobile, setOpenMobile } = useSidebar();
  const navigate = useNavigate();
  const onDashboard = useMatch({ path: '/', end: true });
  const entityMatch = useMatch({ path: '/:entity/*' });
  const activeEntity = entityMatch?.params.entity?.toLowerCase();
  const [collapsedGroups, setCollapsedGroups] = useStored<string[]>('jpa-admin-collapsed-groups', []);

  const groups = Array.from(new Set(metadata.map(entity => entity.group || 'General')));
  const closeMobile = () => { if (isMobile) setOpenMobile(false); };
  const toggleGroup = (group: string, open: boolean) =>
    setCollapsedGroups(open ? collapsedGroups.filter(g => g !== group) : [...collapsedGroups, group]);

  return <Sidebar collapsible="icon" variant="inset">
    <SidebarHeader>
      <SidebarMenu>
        <SidebarMenuItem>
          <SidebarMenuButton size="lg" asChild tooltip={title}>
            <Link to="/" onClick={closeMobile}>
              <BrandMark logoUrl={logoUrl} title={title} />
              <div className="grid flex-1 text-left leading-tight">
                <span className="truncate text-sm font-semibold">{title}</span>
                <span className="truncate text-xs text-muted-foreground">Administration</span>
              </div>
            </Link>
          </SidebarMenuButton>
        </SidebarMenuItem>
        <SidebarMenuItem>
          <SidebarMenuButton tooltip="Search (⌘K)" onClick={() => { closeMobile(); openCommandPalette(); }}
            className="text-muted-foreground border bg-background shadow-xs hover:bg-background group-data-[collapsible=icon]:border-0">
            <Search />
            <span className="flex-1">Search…</span>
            <Kbd className="group-data-[collapsible=icon]:hidden">⌘K</Kbd>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarHeader>

    <SidebarContent>
      <SidebarGroup>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton asChild isActive={!!onDashboard} tooltip="Dashboard">
              <Link to="/" onClick={closeMobile}><LayoutDashboard /><span>Dashboard</span></Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarGroup>

      {loading && <SidebarGroup><SidebarMenu>{Array.from({ length: 5 }, (_, i) => <SidebarMenuItem key={i}><SidebarMenuSkeleton showIcon /></SidebarMenuItem>)}</SidebarMenu></SidebarGroup>}

      {!loading && groups.map(group => {
        const open = !collapsedGroups.includes(group);
        return <Collapsible key={group} open={open} onOpenChange={next => toggleGroup(group, next)} className="group/collapsible">
          <SidebarGroup className="py-1">
            <SidebarGroupLabel asChild>
              <CollapsibleTrigger className="w-full">
                {group}
                <ChevronRight className="ml-auto transition-transform group-data-[state=open]/collapsible:rotate-90" />
              </CollapsibleTrigger>
            </SidebarGroupLabel>
            <CollapsibleContent>
              <SidebarGroupContent>
                <SidebarMenu>
                  {metadata.filter(entity => (entity.group || 'General') === group).map(entity => {
                    const label = entity.pluralName || entity.displayName;
                    const count = counts?.[entity.entityName];
                    return <SidebarMenuItem key={entity.entityName}>
                      <SidebarMenuButton asChild tooltip={label} isActive={activeEntity === entity.entityName.toLowerCase()}>
                        <Link to={`/${entity.entityName}`} onClick={closeMobile}>
                          <EntityIcon entity={entity} />
                          <span>{label}</span>
                        </Link>
                      </SidebarMenuButton>
                      {count != null && <SidebarMenuBadge className="tabular text-muted-foreground">{formatCount(count)}</SidebarMenuBadge>}
                    </SidebarMenuItem>;
                  })}
                </SidebarMenu>
              </SidebarGroupContent>
            </CollapsibleContent>
          </SidebarGroup>
        </Collapsible>;
      })}
    </SidebarContent>

    <SidebarFooter>
      <SidebarMenu>
        <SidebarMenuItem>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <SidebarMenuButton size="lg" className="data-[state=open]:bg-sidebar-accent">
                <Avatar className="size-8 rounded-lg"><AvatarFallback className="rounded-lg bg-primary/10 text-primary text-xs font-semibold">{initials(username)}</AvatarFallback></Avatar>
                <div className="grid flex-1 text-left text-sm leading-tight">
                  <span className="truncate font-medium">{username}</span>
                  <span className="truncate text-xs text-muted-foreground">Administrator</span>
                </div>
                <ChevronsUpDown className="ml-auto size-4" />
              </SidebarMenuButton>
            </DropdownMenuTrigger>
            <DropdownMenuContent side={isMobile ? 'bottom' : 'right'} align="end" sideOffset={4} className="w-56">
              <DropdownMenuLabel className="font-normal">
                <div className="text-sm font-medium">{username}</div>
                <div className="text-xs text-muted-foreground">Signed in to {title}</div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuLabel className="text-xs font-medium text-muted-foreground">Theme</DropdownMenuLabel>
              <DropdownMenuRadioGroup value={theme ?? 'system'} onValueChange={setTheme}>
                <DropdownMenuRadioItem value="light"><Sun />Light</DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="dark"><Moon />Dark</DropdownMenuRadioItem>
                <DropdownMenuRadioItem value="system"><Monitor />System</DropdownMenuRadioItem>
              </DropdownMenuRadioGroup>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={async () => {
                try { await signOut(); navigate('/'); } catch (err) { toast.error(err instanceof Error ? err.message : 'Unable to sign out'); }
              }}><LogOut />Sign out</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarFooter>
    <SidebarRail />
  </Sidebar>;
}
