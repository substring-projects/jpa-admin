import React from 'react';
import ReactDOM from 'react-dom/client';
import { AuthProvider } from './context/AuthContext';
import { App } from './App';
import { AdminProvider } from './context/AdminContext';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
    <AdminProvider>
      <App />
    </AdminProvider>
    </AuthProvider>
  </React.StrictMode>
);
