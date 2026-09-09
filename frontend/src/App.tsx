import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { HomePage } from './pages/HomePage';
import { ResumeReviewPage } from './pages/ResumeReviewPage';
import { AccountPage } from './pages/AccountPage';
import { InterviewChatPage } from './pages/InterviewChatPage';
import { InterviewReportPage } from './pages/InterviewReportPage';
import { PublicReportPage } from './pages/PublicReportPage';

export function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
        <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
        <Route path="/relatorio-publico/:token" element={<PublicReportPage />} />
        <Route path="/" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
        <Route path="/curriculo" element={<ProtectedRoute><ResumeReviewPage /></ProtectedRoute>} />
        <Route path="/conta" element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
        <Route path="/interview/:id" element={<ProtectedRoute><InterviewChatPage /></ProtectedRoute>} />
        <Route path="/interview/:id/report" element={<ProtectedRoute><InterviewReportPage /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
