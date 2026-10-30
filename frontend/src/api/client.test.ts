import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, ErroDeRede, apiFetch, handleResponse } from './client';

describe('apiFetch', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('troca a falha do navegador por um ErroDeRede em português', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    const erro = await apiFetch('/x').catch(e => e);

    expect(erro).toBeInstanceOf(ErroDeRede);
    expect(erro.message).toMatch(/Não foi possível falar com o servidor/);
  });

  it('devolve a resposta, mesmo de erro: ela é do servidor', async () => {
    const resposta = new Response(null, { status: 404 });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta));

    await expect(apiFetch('/x', { method: 'POST' })).resolves.toBe(resposta);
    expect(fetch).toHaveBeenCalledWith('/x', { method: 'POST' });
  });
});

describe('handleResponse', () => {
  it('erro 5xx sem corpo vira uma mensagem legível', async () => {
    const erro = (await handleResponse(new Response(null, { status: 503 })).catch(e => e)) as ApiError;

    expect(erro).toBeInstanceOf(ApiError);
    expect(erro.status).toBe(503);
    expect(erro.message).toBe('O servidor está com problemas. Tente de novo em instantes.');
  });
});
