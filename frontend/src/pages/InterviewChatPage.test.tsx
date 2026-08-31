import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { InterviewChatPage } from './InterviewChatPage';

// InterviewChatPage é só a casca que liga a rota (useParams/useNavigate) ao
// <InterviewChat>. Mockamos o filho pra testar exatamente essa ligação.
const chatProps = vi.fn();
vi.mock('./InterviewChat', () => ({
  InterviewChat: (props: { interviewId: string; onFinished: () => void; onExit: () => void }) => {
    chatProps(props);
    return (
      <div>
        <span>chat de {props.interviewId}</span>
        <button onClick={props.onFinished}>finish</button>
        <button onClick={props.onExit}>exit</button>
      </div>
    );
  },
}));

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/interview/:id" element={<InterviewChatPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('InterviewChatPage', () => {
  beforeEach(() => {
    chatProps.mockReset();
    navigateSpy.mockReset();
  });
  afterEach(() => vi.clearAllMocks());

  it('passa o id da rota como interviewId', () => {
    renderAt('/interview/abc-123');
    expect(screen.getByText('chat de abc-123')).toBeInTheDocument();
  });

  it('onFinished navega pro relatório e onExit navega pra home', async () => {
    const user = userEvent.setup();
    renderAt('/interview/abc-123');

    await user.click(screen.getByRole('button', { name: 'finish' }));
    expect(navigateSpy).toHaveBeenCalledWith('/interview/abc-123/report');

    await user.click(screen.getByRole('button', { name: 'exit' }));
    expect(navigateSpy).toHaveBeenCalledWith('/');
  });
});
