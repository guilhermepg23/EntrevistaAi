import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { HomePage } from './pages/HomePage';
import { InterviewChatPage } from './pages/InterviewChatPage';
import { InterviewReportPage } from './pages/InterviewReportPage';
import { PublicReportPage } from './pages/PublicReportPage';

export function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/relatorio-publico/:token" element={<PublicReportPage />} />
        <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
        <Route path="/interview/:id" element={<ProtectedRoute><InterviewChatPage /></ProtectedRoute>} />
        <Route path="/interview/:id/report" element={<ProtectedRoute><InterviewReportPage /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
