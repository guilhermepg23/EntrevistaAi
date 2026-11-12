import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AjudaChat } from './AjudaChat';
import { supportApi } from '../api/supportApi';
import { ApiError } from '../api/client';
import { lerRascunho, salvarRascunho } from '../lib/rascunho';

vi.mock('../api/supportApi', async importOriginal => ({
  ...(await importOriginal<typeof import('../api/supportApi')>()),
  supportApi: { conversar: vi.fn() },
}));

const conversar = vi.mocked(supportApi.conversar);

function responderCom(...pedacos: string[]) {
  conversar.mockImplementation(async (_m, _h, onDelta) => {
    pedacos.forEach(p => onDelta(p));
  });
}


// A mensagem final da conversa (com o prefixo do leitor de tela). O texto
// também aparece na região aria-live e na bolha de streaming, por isso não
// dá para usar findByText.
async function respostaNaConversa(texto: string) {
  await waitFor(() => expect(document.querySelector('.ajuda-conversa')).toHaveTextContent(`Ajuda: ${texto}`));
}
async function abrir() {
  const user = userEvent.setup();
  render(<AjudaChat tipo="CANDIDATO" />);
  await user.click(screen.getByRole('button', { name: 'Ajuda' }));
  return user;
}

describe('AjudaChat', () => {
  beforeEach(() => { conversar.mockReset(); });

  it('abre como diálogo, com o foco no campo e sugestões do tipo de conta', async () => {
    await abrir();

    expect(screen.getByRole('dialog', { name: 'Ajuda' })).toBeInTheDocument();
    expect(screen.getByLabelText('Sua dúvida')).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Onde vejo os testes das empresas?' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ajuda' })).toHaveAttribute('aria-expanded', 'true');
  });

  it('recrutador vê outras sugestões', async () => {
    const user = userEvent.setup();
    render(<AjudaChat tipo="RECRUTADOR" />);
    await user.click(screen.getByRole('button', { name: 'Ajuda' }));

    expect(screen.getByRole('button', { name: 'Como adiciono alguém à equipe?' })).toBeInTheDocument();
  });

  it('envia com Enter, mostra a resposta e anuncia quando termina', async () => {
    responderCom('Abra a aba', ' "Convites".');
    const user = await abrir();

    await user.type(screen.getByLabelText('Sua dúvida'), 'Onde vejo meus convites?{Enter}');

    await respostaNaConversa('Abra a aba "Convites".');
    expect(screen.getByText('Onde vejo meus convites?')).toBeInTheDocument();
    expect(conversar).toHaveBeenCalledWith('Onde vejo meus convites?', [], expect.any(Function), expect.any(AbortSignal));
    expect(screen.getByLabelText('Sua dúvida')).toHaveValue('');
    // Região aria-live recebe a resposta inteira.
    expect(document.querySelector('[aria-live="polite"]')).toHaveTextContent('Abra a aba "Convites".');
  });

  it('Shift+Enter quebra a linha em vez de enviar', async () => {
    const user = await abrir();

    await user.type(screen.getByLabelText('Sua dúvida'), 'linha 1{Shift>}{Enter}{/Shift}linha 2');

    expect(conversar).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Sua dúvida')).toHaveValue('linha 1\nlinha 2');
  });

  it('clicar numa sugestão envia a pergunta', async () => {
    responderCom('Na aba "Convites".');
    const user = await abrir();

    await user.click(screen.getByRole('button', { name: 'Onde vejo os testes das empresas?' }));

    await respostaNaConversa('Na aba "Convites".');
    expect(conversar.mock.calls[0][0]).toBe('Onde vejo os testes das empresas?');
  });

  it('manda a conversa anterior como histórico e a guarda na aba', async () => {
    responderCom('Resposta 1');
    const user = await abrir();
    await user.type(screen.getByLabelText('Sua dúvida'), 'Pergunta 1{Enter}');
    await respostaNaConversa('Resposta 1');

    responderCom('Resposta 2');
    await user.type(screen.getByLabelText('Sua dúvida'), 'Pergunta 2{Enter}');
    await respostaNaConversa('Resposta 2');

    expect(conversar.mock.calls[1][1]).toEqual([
      { papel: 'user', texto: 'Pergunta 1' },
      { papel: 'assistant', texto: 'Resposta 1' },
    ]);
    expect(lerRascunho('ajuda-chat')).toHaveLength(4);
  });

  it('recupera a conversa guardada e "Limpar conversa" apaga', async () => {
    salvarRascunho('ajuda-chat', [
      { papel: 'user', texto: 'Pergunta antiga' },
      { papel: 'assistant', texto: 'Resposta antiga' },
    ]);
    const user = await abrir();

    expect(screen.getByText('Resposta antiga')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Limpar conversa' }));

    expect(screen.queryByText('Resposta antiga')).not.toBeInTheDocument();
    expect(lerRascunho('ajuda-chat')).toBeNull();
  });

  it('falha mostra o erro e "Tentar de novo" reenvia a mesma pergunta', async () => {
    conversar.mockRejectedValueOnce(new Error('Não consegui responder agora. Tente de novo em instantes.'));
    const user = await abrir();
    await user.type(screen.getByLabelText('Sua dúvida'), 'Como troco a senha?{Enter}');

    expect(await screen.findByRole('alert')).toHaveTextContent('Não consegui responder agora.');
    // A pergunta sem resposta sai da conversa, para não ir duplicada no histórico.
    expect(screen.queryByText('Como troco a senha?')).not.toBeInTheDocument();

    responderCom('Em "Minha conta".');
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }));

    await respostaNaConversa('Em "Minha conta".');
    expect(conversar).toHaveBeenLastCalledWith('Como troco a senha?', [], expect.any(Function), expect.any(AbortSignal));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('429 vira mensagem de esperar um minuto', async () => {
    conversar.mockRejectedValueOnce(new ApiError('Muitas requisições.', 429, 'RATE_LIMITED'));
    const user = await abrir();
    await user.type(screen.getByLabelText('Sua dúvida'), 'oi{Enter}');

    expect(await screen.findByRole('alert')).toHaveTextContent('Espere um minuto');
  });

  it('não deixa enviar enquanto responde', async () => {
    let terminar: () => void = () => {};
    conversar.mockImplementation(() => new Promise<void>(r => { terminar = r; }));
    const user = await abrir();
    await user.type(screen.getByLabelText('Sua dúvida'), 'primeira{Enter}');
    await user.type(screen.getByLabelText('Sua dúvida'), 'segunda');

    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();
    terminar();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Enviar' })).toBeEnabled());
    expect(conversar).toHaveBeenCalledTimes(1);
  });

  it('Esc fecha e devolve o foco ao botão', async () => {
    const user = await abrir();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ajuda' })).toHaveFocus();
  });

  it('Tab fica preso dentro do painel', async () => {
    const user = await abrir();
    const fechar = screen.getByRole('button', { name: 'Fechar ajuda' });

    // O último focável é o campo (o Enviar está desabilitado sem texto).
    expect(screen.getByLabelText('Sua dúvida')).toHaveFocus();
    await user.tab();
    expect(fechar).toHaveFocus();
    await user.tab({ shift: true });
    expect(screen.getByLabelText('Sua dúvida')).toHaveFocus();
  });
});
