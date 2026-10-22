import { createContext, createElement, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { companyApi } from '../api/companyApi';
import { ApiError } from '../api/client';
import { useAuth } from './useAuth';
import type { Company } from '../types/interview';

interface CompanyContextValue {
  // null enquanto carrega, pra candidato, ou quando o recrutador está sem empresa.
  company: Company | null;
  carregando: boolean;
  // Recrutador removido da equipe: a home mostra o formulário de criar empresa.
  semEmpresa: boolean;
  erro: string | null;
  recarregar: () => Promise<void>;
  // Depois de criar/editar a empresa, a tela já tem a versão nova — atualiza
  // o header na hora, sem outra ida ao servidor.
  setCompany: (company: Company) => void;
}

const CompanyContext = createContext<CompanyContextValue | null>(null);

// Context pelo mesmo motivo do AuthProvider: o header (nome da empresa) e as
// telas do modo empresa precisam ver a MESMA empresa — renomear em
// "Configurações da empresa" tem que refletir no header na hora.
// Só busca quando a sessão é de recrutador; pra candidato fica tudo vazio.
export function CompanyProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, tipo } = useAuth();
  const ativo = isAuthenticated && tipo === 'RECRUTADOR';
  const [company, setCompanyState] = useState<Company | null>(null);
  const [carregando, setCarregando] = useState(ativo);
  const [semEmpresa, setSemEmpresa] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  // Recarga silenciosa: NÃO volta `carregando` pra true. As telas trocam o
  // conteúdo inteiro por "Carregando..." enquanto ele vale — se cada recarga
  // (ex.: depois de convidar alguém) fizesse isso, a tela desmontaria e
  // perderia o próprio estado, como o link do convite recém-criado.
  // `carregando` é só da primeira carga (ver o useEffect abaixo).
  const recarregar = useCallback(async () => {
    setErro(null);
    try {
      setCompanyState(await companyApi.get());
      setSemEmpresa(false);
    } catch (err) {
      setCompanyState(null);
      if (err instanceof ApiError && err.errorCode === 'COMPANY_NOT_FOUND') {
        setSemEmpresa(true);
      } else {
        setErro(err instanceof Error ? err.message : 'Erro ao carregar a empresa');
      }
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    if (ativo) {
      setCarregando(true);
      void recarregar();
    } else {
      setCompanyState(null);
      setSemEmpresa(false);
      setErro(null);
      setCarregando(false);
    }
  }, [ativo, recarregar]);

  const setCompany = useCallback((nova: Company) => {
    setCompanyState(nova);
    setSemEmpresa(false);
  }, []);

  return createElement(
    CompanyContext.Provider,
    { value: { company, carregando, semEmpresa, erro, recarregar, setCompany } },
    children,
  );
}

export function useCompany() {
  const ctx = useContext(CompanyContext);
  if (!ctx) throw new Error('useCompany precisa ser usado dentro de <CompanyProvider>');
  return ctx;
}
