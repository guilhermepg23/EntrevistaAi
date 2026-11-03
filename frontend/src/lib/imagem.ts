import { API_BASE } from '../api/config';

// Imagens guardadas no backend (logo da empresa; depois, foto dos recrutadores).
// O backend aceita PNG, JPEG ou WebP até 300 KB; aqui a imagem é reduzida antes
// de subir, para que uma foto de celular (vários MB) não seja recusada.

export const TIPOS_ACEITOS = ['image/png', 'image/jpeg', 'image/webp'];
export const MAX_BYTES = 300 * 1024;
const LADO_MAX = 512;

export function urlImagem(id: string): string {
  return `${API_BASE}/images/${id}`;
}

export class ImagemInvalida extends Error {}

// Reduz para caber em 512x512 sem cortar (uma logo larga continua inteira) e
// converte para WebP. Se o navegador não gerar WebP, o toBlob devolve PNG, que
// o backend também aceita. Se mesmo assim passar do limite, baixa a qualidade.
export async function prepararImagem(arquivo: File): Promise<Blob> {
  if (!TIPOS_ACEITOS.includes(arquivo.type)) {
    throw new ImagemInvalida('Formato não aceito. Use PNG, JPEG ou WebP.');
  }
  const bitmap = await createImageBitmap(arquivo).catch(() => {
    throw new ImagemInvalida('Não foi possível ler essa imagem.');
  });
  const escala = Math.min(1, LADO_MAX / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(bitmap.width * escala));
  canvas.height = Math.max(1, Math.round(bitmap.height * escala));
  canvas.getContext('2d')!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();

  for (const qualidade of [0.9, 0.75, 0.6, 0.45]) {
    const blob = await new Promise<Blob | null>(r => canvas.toBlob(r, 'image/webp', qualidade));
    if (blob && blob.size <= MAX_BYTES) return blob;
  }
  throw new ImagemInvalida('A imagem ficou grande demais mesmo reduzida. Tente outra.');
}
