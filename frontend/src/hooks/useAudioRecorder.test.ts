import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAudioRecorder } from './useAudioRecorder';

class FakeMediaRecorder {
  static instances: FakeMediaRecorder[] = [];
  static isTypeSupported = vi.fn(() => true);

  state: 'inactive' | 'recording' = 'inactive';
  mimeType?: string;
  ondataavailable: ((e: { data: Blob }) => void) | null = null;
  onstop: (() => void) | null = null;
  onerror: (() => void) | null = null;

  constructor(_stream: MediaStream, opts?: { mimeType?: string }) {
    this.mimeType = opts?.mimeType;
    FakeMediaRecorder.instances.push(this);
  }

  start = vi.fn(() => { this.state = 'recording'; });
  stop = vi.fn(() => {
    this.state = 'inactive';
    this.ondataavailable?.({ data: new Blob(['chunk'], { type: 'audio/webm' }) });
    this.onstop?.();
  });
}

const trackStop = vi.fn();
const getUserMedia = vi.fn(async () => ({ getTracks: () => [{ stop: trackStop }] }) as unknown as MediaStream);

describe('useAudioRecorder', () => {
  beforeEach(() => {
    FakeMediaRecorder.instances = [];
    trackStop.mockClear();
    getUserMedia.mockClear();
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder);
    Object.defineProperty(navigator, 'mediaDevices', { value: { getUserMedia }, configurable: true });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    // @ts-expect-error limpeza do stub
    delete navigator.mediaDevices;
  });

  it('supported = true quando há MediaRecorder e getUserMedia', () => {
    const { result } = renderHook(() => useAudioRecorder({ onRecordingComplete: vi.fn() }));
    expect(result.current.supported).toBe(true);
  });

  it('start(): pede o microfone, cria o MediaRecorder e passa a gravar', async () => {
    const { result } = renderHook(() => useAudioRecorder({ onRecordingComplete: vi.fn() }));

    await act(async () => { result.current.start(); });

    expect(getUserMedia).toHaveBeenCalledWith({ audio: true });
    expect(FakeMediaRecorder.instances).toHaveLength(1);
    expect(FakeMediaRecorder.instances[0].start).toHaveBeenCalled();
    await waitFor(() => expect(result.current.recording).toBe(true));
  });

  it('stop(): encerra a gravação e entrega o Blob via onRecordingComplete', async () => {
    const onRecordingComplete = vi.fn();
    const { result } = renderHook(() => useAudioRecorder({ onRecordingComplete }));

    await act(async () => { result.current.start(); });
    act(() => { result.current.stop(); });

    expect(onRecordingComplete).toHaveBeenCalledTimes(1);
    expect(onRecordingComplete.mock.calls[0][0]).toBeInstanceOf(Blob);
    expect(trackStop).toHaveBeenCalled(); // solta o microfone
    expect(result.current.recording).toBe(false);
  });

  it('permissão negada: expõe uma mensagem amigável e não grava', async () => {
    getUserMedia.mockRejectedValueOnce(
      Object.assign(new DOMException('no', 'NotAllowedError')),
    );
    const { result } = renderHook(() => useAudioRecorder({ onRecordingComplete: vi.fn() }));

    await act(async () => { result.current.start(); });

    await waitFor(() => expect(result.current.error).toBe('Permissão de microfone negada.'));
    expect(result.current.recording).toBe(false);
    expect(FakeMediaRecorder.instances).toHaveLength(0);
  });

  it('start() chamado duas vezes seguidas não cria um segundo recorder', async () => {
    const { result } = renderHook(() => useAudioRecorder({ onRecordingComplete: vi.fn() }));

    await act(async () => { result.current.start(); });
    await act(async () => { result.current.start(); });

    expect(FakeMediaRecorder.instances).toHaveLength(1);
  });

  it('desmontar durante a gravação encerra o recorder e solta o microfone', async () => {
    const { result, unmount } = renderHook(() => useAudioRecorder({ onRecordingComplete: vi.fn() }));

    await act(async () => { result.current.start(); });
    const recorder = FakeMediaRecorder.instances[0];
    unmount();

    expect(recorder.stop).toHaveBeenCalled();
    expect(trackStop).toHaveBeenCalled();
  });
});
