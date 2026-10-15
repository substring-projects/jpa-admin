import React from 'react';
import { useAdmin } from '../context/AdminContext';
import { DynamicIcon } from '../components/DynamicIcon';
import { Plus, ArrowRight, Layers, Table as TableIcon, Sparkles } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';

export const DashboardView: React.FC = () => {
  const { metadata, navigateToList, navigateToCreate } = useAdmin();

  const groups = Array.from(new Set(metadata.map(e => e.group || 'General')));

  return (
    <div className="p-4 sm:p-8 max-w-7xl mx-auto space-y-8 animate-fade-in">
      {/* Welcome Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-emerald-600 via-teal-600 to-emerald-700 p-5 sm:p-8 text-white shadow-xl shadow-emerald-900/10">
        <div className="relative z-10 max-w-2xl space-y-3">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md text-xs font-semibold text-white border border-white/20">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>Zero Configuration JPA Admin</span>
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight sm:text-4xl text-white">
            Welcome to your JPA Admin Console
          </h1>
          <p className="text-emerald-50 text-sm sm:text-base leading-relaxed">
            All your JPA entities are automatically discovered and ready for full CRUD operations, search, filters, and custom actions.
          </p>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        <Card className="flex items-center p-6 gap-4">
          <div className="p-3 rounded-2xl bg-primary/10 text-primary">
            <TableIcon className="w-6 h-6" />
          </div>
          <div>
            <div className="text-3xl font-extrabold tracking-tight">{metadata.length}</div>
            <div className="text-xs text-muted-foreground font-medium">Entities Discovered</div>
          </div>
        </Card>

        <Card className="flex items-center p-6 gap-4">
          <div className="p-3 rounded-2xl bg-teal-500/10 text-teal-600 dark:text-teal-400">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <div className="text-3xl font-extrabold tracking-tight">{groups.length}</div>
            <div className="text-xs text-muted-foreground font-medium">Domain Groups</div>
          </div>
        </Card>

        <Card className="flex items-center p-6 gap-4">
          <div className="p-3 rounded-2xl bg-blue-500/10 text-blue-600 dark:text-blue-400">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <div className="text-3xl font-extrabold tracking-tight">Active</div>
            <div className="text-xs text-muted-foreground font-medium">Metamodel Status</div>
          </div>
        </Card>
      </div>

      {/* Entity Cards Grid */}
      <div className="space-y-4">
        <div className="flex flex-wrap gap-3 items-center justify-between">
          <h2 className="text-xl font-bold tracking-tight">Registered Entities</h2>
          <span className="text-xs text-muted-foreground">{metadata.length} entities managed</span>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
          {metadata.map(entity => (
            <Card
              key={entity.entityName}
              className="group flex flex-col justify-between hover:shadow-md hover:border-primary/50 transition-all"
            >
              <CardHeader className="p-6 pb-4">
                <div className="flex items-center justify-between mb-2">
                  <div className="p-2.5 rounded-xl bg-muted text-muted-foreground group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                    <DynamicIcon name={entity.icon || 'database'} className="w-5 h-5" />
                  </div>
                  <Badge variant="secondary" className="font-medium text-xs">
                    {entity.group || 'General'}
                  </Badge>
                </div>

                <CardTitle className="text-lg group-hover:text-primary transition-colors">
                  {entity.pluralName || entity.displayName}
                </CardTitle>
                <CardDescription className="text-xs">
                  {entity.fields.length} attributes &bull; {entity.inlines.length} inlines
                </CardDescription>
              </CardHeader>

              <CardContent className="p-6 pt-0">
                <div className="flex flex-wrap gap-1.5 pt-2">
                  {entity.fields.slice(0, 4).map(f => (
                    <Badge variant="secondary"
                      key={f.name}
                      className="text-[11px] px-2 py-0.5 rounded-md bg-muted/60 text-muted-foreground font-mono"
                    >
                      {f.name}
                    </Badge>
                  ))}
                  {entity.fields.length > 4 && (
                    <span className="text-[11px] px-2 py-0.5 rounded-md bg-muted/60 text-muted-foreground">
                      +{entity.fields.length - 4} more
                    </span>
                  )}
                </div>
              </CardContent>

              <CardFooter className="p-6 pt-3 border-t flex flex-wrap gap-3 items-center justify-between">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigateToList(entity.entityName)}
                  className="gap-1.5 text-xs font-semibold text-primary hover:text-primary"
                >
                  <span>Browse Table</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Button>

                {entity.creatable && !entity.readOnly && (
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => navigateToCreate(entity.entityName)}
                    className="h-8 w-8 rounded-lg"
                    title={`Create New ${entity.displayName}`}
                  >
                    <Plus className="w-4 h-4" />
                  </Button>
                )}
              </CardFooter>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
};
