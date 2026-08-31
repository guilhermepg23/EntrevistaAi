import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useSlowRequestHint } from './useSlowRequestHint';

describe('useSlowRequestHint', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('começa false e continua false enquanto não passa o delay', () => {
    const { result } = renderHook(() => useSlowRequestHint(true, 4000));
    expect(result.current).toBe(false);

    act(() => vi.advanceTimersByTime(3999));
    expect(result.current).toBe(false);
  });

  it('vira true depois que o request pendente passa do delay', () => {
    const { result } = renderHook(() => useSlowRequestHint(true, 4000));

    act(() => vi.advanceTimersByTime(4000));
    expect(result.current).toBe(true);
  });

  it('nunca fica true se o request termina antes do delay', () => {
    const { result, rerender } = renderHook(({ active }) => useSlowRequestHint(active, 4000), {
      initialProps: { active: true },
    });

    act(() => vi.advanceTimersByTime(2000));
    rerender({ active: false });
    act(() => vi.advanceTimersByTime(5000));

    expect(result.current).toBe(false);
  });

  it('reseta pra false quando active volta a false depois de já ter mostrado o aviso', () => {
    const { result, rerender } = renderHook(({ active }) => useSlowRequestHint(active, 1000), {
      initialProps: { active: true },
    });

    act(() => vi.advanceTimersByTime(1000));
    expect(result.current).toBe(true);

    rerender({ active: false });
    expect(result.current).toBe(false);
  });
});
