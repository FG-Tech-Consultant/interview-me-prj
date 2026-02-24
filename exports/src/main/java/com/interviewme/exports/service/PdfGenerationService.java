package com.interviewme.exports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
@Slf4j
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    public PdfGenerationService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public byte[] generatePdf(String templateFile, Map<String, Object> contextVars) {
        log.info("Generating PDF from template: {}", templateFile);

        // 1. Render HTML
        Context context = new Context();
        context.setVariables(contextVars);
        String html = templateEngine.process(templateFile, context);

        // 2. Convert HTML to PDF using Flying Saucer
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);

            byte[] pdfBytes = os.toByteArray();
            log.info("PDF generated successfully: {} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Failed to generate PDF from template: {}", templateFile, e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}
