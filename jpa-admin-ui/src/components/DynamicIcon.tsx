import { useEffect, useState } from 'react';
import {
  Activity, Bell, Building2, Calendar, Contact, Database, FileText, Kanban, Mail, MapPin, MessageSquare, MessagesSquare,
  Package, Receipt, Settings, Shield, ShoppingCart, Star, Tag, Ticket, Users, type LucideIcon, type LucideProps,
} from 'lucide-react';
import type { EntityMetadata } from '@/types/metadata';
import { entityIcon } from '@/lib/format';

// Icons picked by name heuristics ship in the main bundle; any other annotation icon loads one shared chunk on demand.
const BUNDLED: Record<string, LucideIcon> = {
  activity: Activity, bell: Bell, 'building-2': Building2, calendar: Calendar, contact: Contact, database: Database,
  'file-text': FileText, kanban: Kanban, mail: Mail, 'map-pin': MapPin, 'message-square': MessageSquare,
  'messages-square': MessagesSquare, package: Package, receipt: Receipt, settings: Settings, shield: Shield,
  'shopping-cart': ShoppingCart, star: Star, tag: Tag, ticket: Ticket, users: Users,
};

type IconMap = Record<string, LucideIcon>;
let allIcons: IconMap | null = null;
let loading: Promise<IconMap> | null = null;
const loadAllIcons = () => (loading ??= import('lucide-react').then(m => (allIcons = m.icons as IconMap)));

const toKebab = (name: string) => name.trim().replace(/([a-z0-9])([A-Z])/g, '$1-$2').replace(/[\s_]+/g, '-').toLowerCase();
const toPascal = (kebab: string) => kebab.split('-').map(part => part.charAt(0).toUpperCase() + part.slice(1)).join('');

export function DynamicIcon({ name, ...props }: { name: string } & LucideProps) {
  const key = toKebab(name);
  const [, setLoaded] = useState(false);
  const bundled = BUNDLED[key];
  useEffect(() => {
    if (!bundled && !allIcons) void loadAllIcons().then(() => setLoaded(true));
  }, [bundled]);
  const Icon = bundled ?? allIcons?.[toPascal(key)] ?? Database;
  return <Icon {...props} />;
}

export function EntityIcon({ entity, ...props }: { entity: Pick<EntityMetadata, 'icon' | 'entityName'> } & LucideProps) {
  return <DynamicIcon name={entityIcon(entity)} {...props} />;
}
