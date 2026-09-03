import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './features/auth/AuthContext';
import { Layout } from './components/layout/Layout';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { LoginPage } from './features/auth/LoginPage';
import { RegisterPage } from './features/auth/RegisterPage';
import { RecipientListPage } from './features/recipients/RecipientListPage';
import { MilestoneTimeline } from './features/milestones/MilestoneTimeline';
import { GenerateWishPage } from './features/wishes/GenerateWishPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/" element={<Navigate to="/recipients" replace />} />

            <Route element={<ProtectedRoute />}>
              <Route path="/recipients" element={<RecipientListPage />} />
              <Route path="/recipients/:recipientId/milestones" element={<MilestoneTimeline />} />
              <Route path="/recipients/:recipientId/wishes" element={<GenerateWishPage />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
