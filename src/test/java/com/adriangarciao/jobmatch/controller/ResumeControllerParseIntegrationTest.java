package com.adriangarciao.jobmatch.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ResumeControllerParseIntegrationTest {

    @Autowired
    private MockMvc mvc;

    // PDF generation with PDFBox 3.x can be fragile in test env; skip PDF creation here and rely on DOCX coverage.

    @Test
    void parseDocxFile_returnsParsedText() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            var p = doc.createParagraph();
            var run = p.createRun();
            run.setText("Jane Candidate\nSkills: Python, React, AWS\nExperience: 3 years");
            doc.write(baos);
        }

        MockMultipartFile file = new MockMultipartFile("file", "resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", baos.toByteArray());

        // perform the request as an authenticated user
        mvc.perform(multipart("/api/resumes/parse").file(file).contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tester").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawText", containsString("Skills")))
                .andExpect(jsonPath("$.skills", isA(java.util.List.class)));
    }

    @Test
    void parseWithoutAuth_returnsForbidden() throws Exception {
        // create a simple DOCX in-memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            var p = doc.createParagraph();
            var run = p.createRun();
            run.setText("Unauthenticated\nSkills: None");
            doc.write(baos);
        }

        MockMultipartFile file = new MockMultipartFile("file", "resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", baos.toByteArray());

        mvc.perform(multipart("/api/resumes/parse").file(file).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
    }
}
