package com.hyperativa.desafio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperativa.desafio.dto.BatchUploadResponse;
import com.hyperativa.desafio.dto.CardRequest;
import com.hyperativa.desafio.dto.CardResponse;
import com.hyperativa.desafio.exception.BatchAlreadyExistsException;
import com.hyperativa.desafio.exception.CardNotFoundException;
import com.hyperativa.desafio.exception.InvalidBatchFileException;
import com.hyperativa.desafio.exception.InvalidCardNumberException;
import com.hyperativa.desafio.security.JwtService;
import com.hyperativa.desafio.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtService jwtService;

    // ---------- POST /api/v1/cards ----------

    @Test
    @WithMockUser
    void createCardReturns201WithIdAndCreatedAt() throws Exception {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        when(cardService.registerSingle("4456897999999999"))
                .thenReturn(new CardResponse(id, now));

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardRequest("4456897999999999"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/cards/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser
    void createCardIdempotent() throws Exception {
        UUID existingId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(cardService.registerSingle(anyString()))
                .thenReturn(new CardResponse(existingId, OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardRequest("4456897999999999"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(existingId.toString()));
    }

    @Test
    @WithMockUser
    void createCardInvalidNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardRequest("abc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createCardServiceRejectsInvalidNumberReturnsBadRequest() throws Exception {
        when(cardService.registerSingle(anyString()))
                .thenThrow(new InvalidCardNumberException("Numero invalido"));

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardRequest("4456897999999999"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Numero invalido"));
    }

    @Test
    void createCardUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CardRequest("4456897999999999"))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /api/v1/cards/{cardNumber} ----------

    @Test
    @WithMockUser
    void findCardReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(cardService.find("4456897999999999"))
                .thenReturn(new CardResponse(id, OffsetDateTime.now()));

        mockMvc.perform(get("/api/v1/cards/4456897999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser
    void findCardNotFoundReturns404() throws Exception {
        when(cardService.find(anyString())).thenThrow(new CardNotFoundException());

        mockMvc.perform(get("/api/v1/cards/9999999999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findCardUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/cards/4456897999999999"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/v1/cards/batch ----------

    @Test
    @WithMockUser
    void uploadBatchReturns201WithSummary() throws Exception {
        UUID batchId = UUID.randomUUID();
        when(cardService.uploadFile(any()))
                .thenReturn(new BatchUploadResponse(batchId, "LOTE0001", 10, 9, 1));

        MockMultipartFile file = new MockMultipartFile(
                "file", "lote.txt", "text/plain", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/cards/batch").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.batchNumber").value("LOTE0001"))
                .andExpect(jsonPath("$.totalRecords").value(10))
                .andExpect(jsonPath("$.inserted").value(9))
                .andExpect(jsonPath("$.duplicates").value(1));
    }

    @Test
    @WithMockUser
    void uploadBatchEmptyFileReturnsBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "lote.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/v1/cards/batch").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void uploadBatchInvalidFileReturnsBadRequest() throws Exception {
        when(cardService.uploadFile(any()))
                .thenThrow(new InvalidBatchFileException("Header invalido"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "lote.txt", "text/plain", "invalido".getBytes());

        mockMvc.perform(multipart("/api/v1/cards/batch").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Header invalido"));
    }

    @Test
    @WithMockUser
    void uploadBatchDuplicateLoteReturnsConflict() throws Exception {
        when(cardService.uploadFile(any()))
                .thenThrow(new BatchAlreadyExistsException("LOTE0001"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "lote.txt", "text/plain", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/cards/batch").file(file))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void uploadBatchUnauthenticatedReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lote.txt", "text/plain", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/cards/batch").file(file))
                .andExpect(status().isUnauthorized());
    }
}
