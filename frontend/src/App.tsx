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
import { RecruiterHomePage } from './pages/RecruiterHomePage';
import { TeamPage } from './pages/TeamPage';
import { CompanySettingsPage } from './pages/CompanySettingsPage';
import { TeamInviteAcceptPage } from './pages/TeamInviteAcceptPage';
import { AssessmentsPage } from './pages/AssessmentsPage';
import { AssessmentFormPage } from './pages/AssessmentFormPage';
import { AssessmentDetailPage } from './pages/AssessmentDetailPage';
import { AssessmentInvitePage } from './pages/AssessmentInvitePage';
import { useAuth } from './hooks/useAuth';

// "/" é a raiz de quem está logado, e cada tipo de conta tem a sua: o candidato
// cai no treino de entrevista, o recrutador no painel da empresa.
function HomePorTipo() {
  const { tipo } = useAuth();
  return tipo === 'RECRUTADOR' ? <RecruiterHomePage /> : <HomePage />;
}

export function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
        <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
        <Route path="/relatorio-publico/:token" element={<PublicReportPage />} />
        <Route path="/convite-equipe/:token" element={<TeamInviteAcceptPage />} />
        {/* Pública: quem abre o link do teste pode não estar logado (a página manda pro login/cadastro). */}
        <Route path="/teste/:token" element={<AssessmentInvitePage />} />
        <Route path="/empresa/testes" element={<ProtectedRoute tipo="RECRUTADOR"><AssessmentsPage /></ProtectedRoute>} />
        <Route path="/empresa/testes/novo" element={<ProtectedRoute tipo="RECRUTADOR"><AssessmentFormPage /></ProtectedRoute>} />
        <Route path="/empresa/testes/:id" element={<ProtectedRoute tipo="RECRUTADOR"><AssessmentDetailPage /></ProtectedRoute>} />
        <Route path="/empresa/testes/:id/editar" element={<ProtectedRoute tipo="RECRUTADOR"><AssessmentFormPage /></ProtectedRoute>} />
        <Route path="/empresa/equipe" element={<ProtectedRoute tipo="RECRUTADOR"><TeamPage /></ProtectedRoute>} />
        <Route path="/empresa/configuracoes" element={<ProtectedRoute tipo="RECRUTADOR"><CompanySettingsPage /></ProtectedRoute>} />
        <Route path="/" element={<ProtectedRoute><HomePorTipo /></ProtectedRoute>} />
        <Route path="/curriculo" element={<ProtectedRoute tipo="CANDIDATO"><ResumeReviewPage /></ProtectedRoute>} />
        <Route path="/conta" element={<ProtectedRoute><AccountPage /></ProtectedRoute>} />
        <Route path="/interview/:id" element={<ProtectedRoute tipo="CANDIDATO"><InterviewChatPage /></ProtectedRoute>} />
        <Route path="/interview/:id/report" element={<ProtectedRoute tipo="CANDIDATO"><InterviewReportPage /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
