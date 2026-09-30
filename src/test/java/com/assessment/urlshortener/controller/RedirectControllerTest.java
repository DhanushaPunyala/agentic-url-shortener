package com.assessment.urlshortener.controller;

import com.assessment.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.assessment.urlshortener.model.Url;
import com.assessment.urlshortener.exception.GlobalExceptionHandler;
import com.assessment.urlshortener.exception.ShortUrlNotFoundException;
import com.assessment.urlshortener.exception.ShortUrlExpiredException;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RedirectControllerTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        RedirectController redirectController =
                new RedirectController(urlShortenerService);

        mockMvc = MockMvcBuilders
        .standaloneSetup(redirectController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    }
    @Test
void shouldRedirectToOriginalUrl() throws Exception {

    Url url = new Url(
            "https://www.google.com",
            "abc1234",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(7));

    when(urlShortenerService.getUrlByShortCode("abc1234"))
            .thenReturn(url);

    mockMvc.perform(get("/abc1234"))
            .andExpect(status().isFound())
            .andExpect(header().string(
                    "Location",
                    "https://www.google.com"));
}
@Test
void shouldReturn404WhenShortCodeNotFound() throws Exception {

    when(urlShortenerService.getUrlByShortCode("invalid"))
            .thenThrow(new ShortUrlNotFoundException(
                    "Short URL not found: invalid"));

    mockMvc.perform(get("/invalid"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message")
                .value("Short URL not found: invalid"));
}
@Test
void shouldReturn410WhenShortUrlIsExpired() throws Exception {

    when(urlShortenerService.getUrlByShortCode("expired1"))
            .thenThrow(new ShortUrlExpiredException(
                    "Short URL has expired: expired1"));

    mockMvc.perform(get("/expired1"))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(jsonPath("$.error").value("Gone"))
        .andExpect(jsonPath("$.message")
                .value("Short URL has expired: expired1"));
}
}