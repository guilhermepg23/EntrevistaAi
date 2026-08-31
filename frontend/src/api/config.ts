// URL base do backend.
//
// Em desenvolvimento e no build local vem de VITE_API_URL (ver frontend/.env).
// O fallback é o backend em produção no Render: garante que o app publicado
// funcione mesmo que a env var não entre no build da Vercel (o `||` só cai no
// default quando VITE_API_URL está ausente ou vazia — quando está definida,
// como no .env local, o valor dela é respeitado).
const DEFAULT_API_BASE = 'https://entrevista-ia-backend.onrender.com';

export const API_BASE = import.meta.env.VITE_API_URL || DEFAULT_API_BASE;
