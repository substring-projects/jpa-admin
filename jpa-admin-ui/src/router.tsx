import { createBrowserRouter } from 'react-router';
import { ADMIN_BASE } from '@/lib/base-path';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { NotFound } from '@/components/PageStates';
import { DashboardView } from '@/views/DashboardView';
import { EntityDetailView } from '@/views/EntityDetailView';
import { EntityFormView } from '@/views/EntityFormView';
import { EntityListView } from '@/views/EntityListView';

export const router = createBrowserRouter([
  {
    element: <AdminLayout />,
    children: [
      { index: true, element: <DashboardView /> },
      { path: ':entity', element: <EntityListView /> },
      { path: ':entity/new', element: <EntityFormView /> },
      { path: ':entity/:id', element: <EntityDetailView /> },
      { path: ':entity/:id/edit', element: <EntityFormView /> },
      { path: '*', element: <NotFound /> },
    ],
  },
], { basename: ADMIN_BASE });
