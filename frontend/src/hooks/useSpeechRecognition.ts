import { useCallback, useEffect, useRef, useState } from 'react';

// Ditado por voz via Web Speech API do navegador (Chrome/Edge). Não manda áudio
// pra lugar nenhum: a transcrição acontece no próprio navegador e o texto final
// é entregue via `onResult` pra quem chama (o AnswerInput anexa no textarea).
//
// `interimResults` fica desligado de propósito: só entregamos segmentos já
// finalizados, o que evita ter que rastrear/reescrever texto provisório no
// campo. O trade-off é um pequeno atraso até cada trecho "assentar".

interface Options {
  lang?: string;
  onResult: (finalText: string) => void;
}

interface SpeechRecognitionState {
  /** false em navegadores sem a API (ex.: Firefox) — o AnswerInput esconde o botão nesse caso. */
  supported: boolean;
  listening: boolean;
  error: string | null;
  start: () => void;
  stop: () => void;
}

function getRecognitionCtor(): SpeechRecognitionConstructor | undefined {
  if (typeof window === 'undefined') return undefined;
  return window.SpeechRecognition ?? window.webkitSpeechRecognition;
}

export function useSpeechRecognition({ lang = 'pt-BR', onResult }: Options): SpeechRecognitionState {
  const RecognitionCtor = getRecognitionCtor();
  const supported = RecognitionCtor !== undefined;

  const [listening, setListening] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const recognitionRef = useRef<SpeechRecognition | null>(null);
  // onResult costuma ser uma função nova a cada render (closure sobre o texto
  // atual do campo) — guardamos numa ref pra não ter que recriar o recognition
  // nem religar os handlers a cada digitação.
  const onResultRef = useRef(onResult);
  useEffect(() => {
    onResultRef.current = onResult;
  });

  const stop = useCallback(() => {
    recognitionRef.current?.stop();
    setListening(false);
  }, []);

  const start = useCallback(() => {
    if (!RecognitionCtor || recognitionRef.current) return;

    setError(null);
    const recognition = new RecognitionCtor();
    recognition.lang = lang;
    recognition.continuous = true;
    recognition.interimResults = false;

    recognition.onresult = (event) => {
      let finalText = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i];
        if (result.isFinal) finalText += result[0].transcript;
      }
      const trimmed = finalText.trim();
      if (trimmed) onResultRef.current(trimmed);
    };

    recognition.onerror = (event) => {
      setError(
        event.error === 'not-allowed' || event.error === 'service-not-allowed'
          ? 'Permissão de microfone negada.'
          : event.error === 'no-speech'
            ? 'Não captei nenhuma fala. Tente de novo.'
            : 'Erro no reconhecimento de voz.',
      );
    };

    recognition.onend = () => {
      recognitionRef.current = null;
      setListening(false);
    };

    recognitionRef.current = recognition;
    try {
      recognition.start();
      setListening(true);
    } catch {
      // start() lança se chamado enquanto ainda não parou de todo — trata como no-op.
      recognitionRef.current = null;
    }
  }, [RecognitionCtor, lang]);

  useEffect(() => {
    return () => {
      recognitionRef.current?.abort();
      recognitionRef.current = null;
    };
  }, []);

  return { supported, listening, error, start, stop };
}
