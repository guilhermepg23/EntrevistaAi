import { fireEvent, render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LogoEmpresa } from './LogoEmpresa';

describe('LogoEmpresa', () => {
  it('mostra a logo pelo id', () => {
    const { container } = render(<LogoEmpresa logoId="img-1" nome="Acme" />);

    const img = container.querySelector('img')!;
    expect(img.getAttribute('src')).toMatch(/\/images\/img-1$/);
    expect(img).toHaveAttribute('alt', '');
  });

  it('sem logo, ou se ela não carregar, mostra a inicial', () => {
    const { container, rerender } = render(<LogoEmpresa logoId={null} nome="acme" />);
    expect(container).toHaveTextContent('A');
    expect(container.querySelector('img')).toBeNull();

    rerender(<LogoEmpresa logoId="img-quebrada" nome="Beta" />);
    fireEvent.error(container.querySelector('img')!);
    expect(container.querySelector('img')).toBeNull();
    expect(container).toHaveTextContent('B');
  });
});
