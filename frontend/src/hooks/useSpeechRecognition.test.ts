import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useSpeechRecognition } from './useSpeechRecognition';

class FakeRecognition {
  static instances: FakeRecognition[] = [];
  lang = '';
  continuous = false;
  interimResults = false;
  maxAlternatives = 1;
  onresult: ((ev: SpeechRecognitionEvent) => unknown) | null = null;
  onerror: ((ev: SpeechRecognitionErrorEvent) => unknown) | null = null;
  onend: ((ev: Event) => unknown) | null = null;
  onstart: ((ev: Event) => unknown) | null = null;
  start = vi.fn(() => { FakeRecognition.instances.push(this); });
  stop = vi.fn(() => this.onend?.(new Event('end')));
  abort = vi.fn();
  addEventListener = vi.fn();
  removeEventListener = vi.fn();
  dispatchEvent = vi.fn(() => true);

  emitFinal(text: string) {
    this.onresult?.({
      resultIndex: 0,
      results: { length: 1, 0: { 0: { transcript: text, confidence: 1 }, isFinal: true, length: 1 } },
    } as unknown as SpeechRecognitionEvent);
  }
}

function installFakeApi() {
  (window as { webkitSpeechRecognition?: unknown }).webkitSpeechRecognition = FakeRecognition;
}

describe('useSpeechRecognition', () => {
  afterEach(() => {
    FakeRecognition.instances = [];
    delete (window as { webkitSpeechRecognition?: unknown }).webkitSpeechRecognition;
    delete (window as { SpeechRecognition?: unknown }).SpeechRecognition;
  });

  it('supported = false quando o navegador não expõe a API', () => {
    const { result } = renderHook(() => useSpeechRecognition({ onResult: vi.fn() }));
    expect(result.current.supported).toBe(false);
  });

  it('supported = true e start() configura o recognition em pt-BR e contínuo', () => {
    installFakeApi();
    const { result } = renderHook(() => useSpeechRecognition({ onResult: vi.fn() }));

    expect(result.current.supported).toBe(true);
    act(() => result.current.start());

    expect(result.current.listening).toBe(true);
    const rec = FakeRecognition.instances[0];
    expect(rec.lang).toBe('pt-BR');
    expect(rec.continuous).toBe(true);
    expect(rec.interimResults).toBe(false);
  });

  it('entrega só os segmentos finais via onResult (com trim)', () => {
    installFakeApi();
    const onResult = vi.fn();
    const { result } = renderHook(() => useSpeechRecognition({ onResult }));

    act(() => result.current.start());
    act(() => FakeRecognition.instances[0].emitFinal('  minha fala  '));

    expect(onResult).toHaveBeenCalledWith('minha fala');
  });

  it('start() duas vezes seguidas não cria um segundo recognition', () => {
    installFakeApi();
    const { result } = renderHook(() => useSpeechRecognition({ onResult: vi.fn() }));

    act(() => result.current.start());
    act(() => result.current.start());

    expect(FakeRecognition.instances).toHaveLength(1);
  });

  it('stop() encerra a escuta', () => {
    installFakeApi();
    const { result } = renderHook(() => useSpeechRecognition({ onResult: vi.fn() }));

    act(() => result.current.start());
    act(() => result.current.stop());

    expect(FakeRecognition.instances[0].stop).toHaveBeenCalled();
    expect(result.current.listening).toBe(false);
  });

  it('onerror "not-allowed" vira uma mensagem amigável de permissão', () => {
    installFakeApi();
    const { result } = renderHook(() => useSpeechRecognition({ onResult: vi.fn() }));

    act(() => result.current.start());
    act(() => FakeRecognition.instances[0].onerror?.({ error: 'not-allowed' } as SpeechRecognitionErrorEvent));

    expect(result.current.error).toBe('Permissão de microfone negada.');
  });

  it('aborta o recognition ao desmontar', () => {
    installFakeApi();
    const { result, unmount } = renderHook(() => useSpeechRecognition({ onResult: vi.fn() }));

    act(() => result.current.start());
    const rec = FakeRecognition.instances[0];
    unmount();

    expect(rec.abort).toHaveBeenCalled();
  });
});
