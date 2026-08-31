import { useCallback, useEffect, useRef, useState } from 'react';

// Grava áudio do microfone via MediaRecorder e, ao parar, entrega o Blob pra
// quem chamou (o AnswerInput manda pro backend transcrever). Não faz nada além
// de gravar: a transcrição acontece no servidor (POST /interviews/transcribe).
// Funciona em todos os navegadores modernos (Chrome, Firefox, Safari 14.1+) —
// diferente do reconhecimento nativo, que só o Chrome/Edge tinham.

interface Options {
  onRecordingComplete: (audio: Blob) => void;
}

interface AudioRecorderState {
  supported: boolean;
  recording: boolean;
  error: string | null;
  start: () => void;
  stop: () => void;
}

function pickMimeType(): string {
  if (typeof MediaRecorder === 'undefined') return '';
  // Ordem de preferência: webm/opus (Chrome/Firefox) e mp4 (Safari) são os que
  // o backend/OpenAI aceitam sem conversão.
  for (const type of ['audio/webm', 'audio/mp4', 'audio/ogg']) {
    if (MediaRecorder.isTypeSupported(type)) return type;
  }
  return '';
}

export function useAudioRecorder({ onRecordingComplete }: Options): AudioRecorderState {
  const supported =
    typeof window !== 'undefined' &&
    typeof MediaRecorder !== 'undefined' &&
    !!navigator.mediaDevices?.getUserMedia;

  const [recording, setRecording] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const recorderRef = useRef<MediaRecorder | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const onCompleteRef = useRef(onRecordingComplete);
  useEffect(() => {
    onCompleteRef.current = onRecordingComplete;
  });

  const encerrarStream = useCallback(() => {
    streamRef.current?.getTracks().forEach(t => t.stop());
    streamRef.current = null;
    recorderRef.current = null;
  }, []);

  const stop = useCallback(() => {
    if (recorderRef.current && recorderRef.current.state !== 'inactive') {
      recorderRef.current.stop(); // dispara onstop -> monta o Blob
    }
    setRecording(false);
  }, []);

  const start = useCallback(async () => {
    if (!supported || recorderRef.current) return;
    setError(null);

    let stream: MediaStream;
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (err) {
      setError(
        err instanceof DOMException && (err.name === 'NotAllowedError' || err.name === 'SecurityError')
          ? 'Permissão de microfone negada.'
          : 'Não foi possível acessar o microfone.',
      );
      return;
    }

    streamRef.current = stream;
    chunksRef.current = [];
    const mimeType = pickMimeType();
    const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);

    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunksRef.current.push(e.data);
    };
    recorder.onstop = () => {
      const blob = new Blob(chunksRef.current, { type: mimeType || 'audio/webm' });
      encerrarStream();
      if (blob.size > 0) onCompleteRef.current(blob);
    };
    recorder.onerror = () => {
      setError('Erro ao gravar o áudio.');
      setRecording(false);
      encerrarStream();
    };

    recorderRef.current = recorder;
    recorder.start();
    setRecording(true);
  }, [supported, encerrarStream]);

  useEffect(() => {
    return () => {
      if (recorderRef.current && recorderRef.current.state !== 'inactive') {
        recorderRef.current.stop();
      }
      encerrarStream();
    };
  }, [encerrarStream]);

  return { supported, recording, error, start, stop };
}
