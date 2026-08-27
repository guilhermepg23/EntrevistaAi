import { useNavigate, useParams } from 'react-router-dom';
import { InterviewChat } from './InterviewChat';

export function InterviewChatPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  if (!id) return null;

  return (
    <InterviewChat
      interviewId={id}
      onFinished={() => navigate(`/interview/${id}/report`)}
      onExit={() => navigate('/')}
    />
  );
}
