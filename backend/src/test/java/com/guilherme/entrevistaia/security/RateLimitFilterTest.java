package com.guilherme.entrevistaia.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private MockHttpServletResponse passar(RateLimitFilter filter, String metodo, String path, String ip,
                                          FilterChain chain) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(metodo, path);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res;
    }

    @Test
    void liberaAteOLimiteEDepoisDevolve429NoBucketAuth() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3, 100);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            assertThat(passar(filter, "POST", "/auth/login", "1.1.1.1", chain).getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse bloqueada = passar(filter, "POST", "/auth/login", "1.1.1.1", chain);
        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getContentAsString()).contains("RATE_LIMITED");
        assertThat(bloqueada.getHeader("Retry-After")).isEqualTo("60");

        // A 4ª não chega a passar adiante na cadeia.
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void contadoresSaoIndependentesPorIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 100);
        FilterChain chain = mock(FilterChain.class);

        assertThat(passar(filter, "POST", "/auth/login", "1.1.1.1", chain).getStatus()).isEqualTo(200);
        assertThat(passar(filter, "POST", "/auth/login", "1.1.1.1", chain).getStatus()).isEqualTo(429);
        // Outro IP tem cota própria.
        assertThat(passar(filter, "POST", "/auth/login", "2.2.2.2", chain).getStatus()).isEqualTo(200);
    }

    @Test
    void bucketsAuthEAiSaoSeparados() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);
        FilterChain chain = mock(FilterChain.class);

        assertThat(passar(filter, "POST", "/auth/login", "9.9.9.9", chain).getStatus()).isEqualTo(200);
        // Estourou o bucket auth, mas o bucket ai ainda tem cota.
        assertThat(passar(filter, "POST", "/auth/login", "9.9.9.9", chain).getStatus()).isEqualTo(429);
        assertThat(passar(filter, "POST", "/resume-reviews", "9.9.9.9", chain).getStatus()).isEqualTo(200);
    }

    @Test
    void rotasForaDosBucketsNaoSaoLimitadas() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            assertThat(passar(filter, "GET", "/interviews", "5.5.5.5", chain).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void usaOPrimeiroIpDoXForwardedForQuandoPresente() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 100);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest r1 = new MockHttpServletRequest("POST", "/auth/login");
        r1.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        r1.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(r1, res1, chain);
        assertThat(res1.getStatus()).isEqualTo(200);

        // Mesmo client real (203.0.113.7), mesmo que o remote addr do proxy mude.
        MockHttpServletRequest r2 = new MockHttpServletRequest("POST", "/auth/login");
        r2.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.2");
        r2.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(r2, res2, chain);
        assertThat(res2.getStatus()).isEqualTo(429);
    }
}
