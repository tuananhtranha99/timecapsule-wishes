import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';

export const Layout: React.FC = () => {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar />
      <main className="app-container" style={{ flex: 1, paddingTop: '2rem' }}>
        <Outlet />
      </main>
    </div>
  );
};
