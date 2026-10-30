import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useNovaVersao } from './useNovaVersao';

const CINCO_MINUTOS = 5 * 60 * 1000;

function respostaComBuild(buildId: string) {
  return { ok: true, json: () => Promise.resolve({ buildId }) };
}

async function passarTempo(ms: number) {
  await act(() => vi.advanceTimersByTimeAsync(ms));
}

describe('useNovaVersao', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    vi.useFakeTimers();
    // Nos testes o Vite marca DEV=true, e o hook não rodaria.
    vi.stubEnv('DEV', false);
    vi.stubGlobal('fetch', fetchMock);
    fetchMock.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('não avisa quando o build é o mesmo', async () => {
    fetchMock.mockResolvedValue(respostaComBuild(__BUILD_ID__));
    const { result } = renderHook(() => useNovaVersao());

    await passarTempo(CINCO_MINUTOS);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(result.current).toBe(false);
  });

  it('avisa quando o build é outro e para de checar', async () => {
    fetchMock.mockResolvedValue(respostaComBuild('build-novo'));
    const { result } = renderHook(() => useNovaVersao());

    await passarTempo(CINCO_MINUTOS);
    expect(result.current).toBe(true);

    await passarTempo(CINCO_MINUTOS * 3);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('não pede o version.json do cache', async () => {
    fetchMock.mockResolvedValue(respostaComBuild(__BUILD_ID__));
    renderHook(() => useNovaVersao());

    await passarTempo(CINCO_MINUTOS);

    const [url, opcoes] = fetchMock.mock.calls[0];
    expect(url).toMatch(/^\/version\.json\?t=\d+$/);
    expect(opcoes).toEqual({ cache: 'no-store' });
  });

  it('erro de rede não avisa, e a checagem seguinte ainda funciona', async () => {
    fetchMock.mockRejectedValueOnce(new Error('sem rede'));
    fetchMock.mockResolvedValueOnce(respostaComBuild('build-novo'));
    const { result } = renderHook(() => useNovaVersao());

    await passarTempo(CINCO_MINUTOS);
    expect(result.current).toBe(false);

    await passarTempo(CINCO_MINUTOS);
    expect(result.current).toBe(true);
  });

  it('não avisa quando a resposta não é um version.json válido', async () => {
    fetchMock.mockResolvedValueOnce({ ok: false });
    fetchMock.mockResolvedValueOnce({ ok: true, json: () => Promise.reject(new SyntaxError('html')) });
    fetchMock.mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({}) });
    const { result } = renderHook(() => useNovaVersao());

    await passarTempo(CINCO_MINUTOS * 3);

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(result.current).toBe(false);
  });

  it('checa ao voltar para a aba, mas não ao sair dela', async () => {
    fetchMock.mockResolvedValue(respostaComBuild('build-novo'));
    const visibilidade = vi.spyOn(document, 'visibilityState', 'get');
    const { result } = renderHook(() => useNovaVersao());

    visibilidade.mockReturnValue('hidden');
    document.dispatchEvent(new Event('visibilitychange'));
    await passarTempo(0);
    expect(fetchMock).not.toHaveBeenCalled();

    visibilidade.mockReturnValue('visible');
    document.dispatchEvent(new Event('visibilitychange'));
    await passarTempo(0);
    expect(result.current).toBe(true);
  });

  it('não checa em desenvolvimento', async () => {
    vi.stubEnv('DEV', true);
    renderHook(() => useNovaVersao());

    await passarTempo(CINCO_MINUTOS);
    document.dispatchEvent(new Event('visibilitychange'));

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('para de checar ao desmontar', async () => {
    fetchMock.mockResolvedValue(respostaComBuild(__BUILD_ID__));
    const { unmount } = renderHook(() => useNovaVersao());

    unmount();
    await passarTempo(CINCO_MINUTOS);
    document.dispatchEvent(new Event('visibilitychange'));

    expect(fetchMock).not.toHaveBeenCalled();
  });
});
