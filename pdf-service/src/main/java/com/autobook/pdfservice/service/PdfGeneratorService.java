package com.autobook.pdfservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorService {

    @Value("${pdf.upload.dir}")
    private String pdfUploadDir;

    @Value("${preview.upload.dir}")
    private String previewUploadDir;

    @SuppressWarnings("unchecked")
    public Map<String, String> generatePdf(Map<String, Object> request) {
        try {
            Long bookId = (Long) request.get("bookId");
            String title = (String) request.get("title");
            String summary = (String) request.get("summary");
            String coverImagePath = (String) request.get("coverImagePath");
            List<Map<String, Object>> chapters = (List<Map<String, Object>>) request.get("chapters");
            String bookType = (String) request.get("bookType");

            Path pdfDir = Paths.get(pdfUploadDir);
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
            }

            PDDocument document = new PDDocument();

            if (coverImagePath != null) {
                addCoverPage(document, coverImagePath, title);
            }

            addTitlePage(document, title, summary);

            if (chapters != null) {
                addChapters(document, chapters, "TEXT_IMAGE".equals(bookType));
            }

            String pdfFilename = "book_" + bookId + "_" + UUID.randomUUID() + ".pdf";
            Path pdfPath = pdfDir.resolve(pdfFilename);
            document.save(pdfPath.toFile());
            document.close();

            log.info("Generated PDF: {}", pdfPath);

            Map<String, String> result = new HashMap<>();
            result.put("pdfPath", pdfPath.toString());

            return result;
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    public Map<String, String> generatePreviewImage(String pdfPath) {
        try {
            Path previewDir = Paths.get(previewUploadDir);
            if (!Files.exists(previewDir)) {
                Files.createDirectories(previewDir);
            }

            PDDocument document = PDDocument.load(new File(pdfPath));

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);

            String previewFilename = "preview_" + UUID.randomUUID() + ".png";
            Path previewPath = previewDir.resolve(previewFilename);
            ImageIO.write(image, "PNG", previewPath.toFile());

            document.close();

            log.info("Generated preview image: {}", previewPath);

            Map<String, String> result = new HashMap<>();
            result.put("previewImagePath", previewPath.toString());

            return result;
        } catch (IOException e) {
            log.error("Error generating preview image", e);
            throw new RuntimeException("Failed to generate preview image", e);
        }
    }

    private void addCoverPage(PDDocument document, String coverImagePath, String title) throws IOException {
        PDPage coverPage = new PDPage(PDRectangle.A4);
        document.addPage(coverPage);

        PDImageXObject coverImage = PDImageXObject.createFromFile(coverImagePath, document);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, coverPage)) {
            float pageWidth = coverPage.getMediaBox().getWidth();
            float pageHeight = coverPage.getMediaBox().getHeight();

            float imageWidth = coverImage.getWidth();
            float imageHeight = coverImage.getHeight();

            float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);

            float scaledWidth = imageWidth * scale;
            float scaledHeight = imageHeight * scale;

            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;

            contentStream.drawImage(coverImage, x, y, scaledWidth, scaledHeight);
        }
    }

    private void addTitlePage(PDDocument document, String title, String summary) throws IOException {
        PDPage titlePage = new PDPage(PDRectangle.A4);
        document.addPage(titlePage);

        PDFont titleFont = PDType1Font.HELVETICA_BOLD;
        PDFont textFont = PDType1Font.HELVETICA;

        try (PDPageContentStream contentStream = new PDPageContentStream(document, titlePage)) {
            float pageWidth = titlePage.getMediaBox().getWidth();
            float pageHeight = titlePage.getMediaBox().getHeight();

            float titleFontSize = 24;
            float titleWidth = titleFont.getStringWidth(title) / 1000 * titleFontSize;
            float titleX = (pageWidth - titleWidth) / 2;
            float titleY = pageHeight - 150;

            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.newLineAtOffset(titleX, titleY);
            contentStream.showText(title);
            contentStream.endText();

            if (summary != null && !summary.isEmpty()) {
                float summaryMargin = 50;
                float summaryY = titleY - 100;
                float summaryWidth = pageWidth - 2 * summaryMargin;
                float fontSize = 12;

                addWrappedText(document, contentStream, summary, textFont, fontSize,
                        summaryMargin, summaryY, summaryWidth, 15);
            }
        }
    }

    private void addChapters(PDDocument document, List<Map<String, Object>> chapters, boolean includeIllustrations)
            throws IOException {
        PDFont chapterTitleFont = PDType1Font.HELVETICA_BOLD;
        PDFont textFont = PDType1Font.HELVETICA;

        for (Map<String, Object> chapter : chapters) {
            Integer chapterNumber = (Integer) chapter.get("chapterNumber");
            String chapterTitle = (String) chapter.get("chapterTitle");
            String chapterContent = (String) chapter.get("chapterContent");
            String illustrationPath = (String) chapter.get("illustrationPath");

            PDPage chapterPage = new PDPage(PDRectangle.A4);
            document.addPage(chapterPage);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, chapterPage)) {
                float pageWidth = chapterPage.getMediaBox().getWidth();
                float pageHeight = chapterPage.getMediaBox().getHeight();

                String chapterText = "Chapter " + chapterNumber;
                float chapterNumFontSize = 16;
                float chapterNumWidth = chapterTitleFont.getStringWidth(chapterText) / 1000 * chapterNumFontSize;
                float chapterNumX = (pageWidth - chapterNumWidth) / 2;
                float chapterNumY = pageHeight - 150;

                contentStream.beginText();
                contentStream.setFont(chapterTitleFont, chapterNumFontSize);
                contentStream.newLineAtOffset(chapterNumX, chapterNumY);
                contentStream.showText(chapterText);
                contentStream.endText();

                float titleFontSize = 20;
                float titleWidth = chapterTitleFont.getStringWidth(chapterTitle) / 1000 * titleFontSize;
                float titleX = (pageWidth - titleWidth) / 2;
                float titleY = chapterNumY - 40;

                contentStream.beginText();
                contentStream.setFont(chapterTitleFont, titleFontSize);
                contentStream.newLineAtOffset(titleX, titleY);
                contentStream.showText(chapterTitle);
                contentStream.endText();

                float contentStartY = titleY - 80;
                if (includeIllustrations && illustrationPath != null) {
                    try {
                        PDImageXObject illustration = PDImageXObject.createFromFile(illustrationPath, document);

                        // Calculate image dimensions
                        float maxWidth = pageWidth - 100;
                        float maxHeight = 200;

                        float imageWidth = illustration.getWidth();
                        float imageHeight = illustration.getHeight();

                        float scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);

                        float scaledWidth = imageWidth * scale;
                        float scaledHeight = imageHeight * scale;

                        float imageX = (pageWidth - scaledWidth) / 2;
                        float imageY = titleY - 60 - scaledHeight;

                        contentStream.drawImage(illustration, imageX, imageY, scaledWidth, scaledHeight);

                        contentStartY = imageY - 20;
                    } catch (Exception e) {
                        log.warn("Failed to add illustration to chapter {}", chapterNumber, e);
                    }
                }

                if (chapterContent != null && !chapterContent.isEmpty()) {
                    float margin = 50;
                    float maxWidth = pageWidth - 2 * margin;
                    float fontSize = 12;

                    addWrappedText(document, contentStream, chapterContent, textFont, fontSize,
                            margin, contentStartY, maxWidth, 15);
                }
            }
        }
    }

    private void addWrappedText(PDDocument document, PDPageContentStream contentStream, String text, PDFont font,
                                float fontSize, float startX, float startY, float maxWidth,
                                float leading) throws IOException {
        String[] paragraphs = text.split("\n\n");
        float currentY = startY;

        contentStream.setFont(font, fontSize);

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split("\\s+");
            StringBuilder line = new StringBuilder();

            contentStream.beginText();
            contentStream.newLineAtOffset(startX, currentY);

            for (String word : words) {
                String testLine = line.toString() + (line.length() > 0 ? " " : "") + word;
                float lineWidth = font.getStringWidth(testLine) / 1000 * fontSize;

                if (lineWidth > maxWidth) {
                    contentStream.showText(line.toString());
                    contentStream.newLineAtOffset(0, -leading);
                    currentY -= leading;
                    line = new StringBuilder(word);
                } else {
                    if (line.length() > 0) {
                        line.append(" ");
                    }
                    line.append(word);
                }
            }

            if (line.length() > 0) {
                contentStream.showText(line.toString());
            }

            contentStream.endText();

            currentY -= leading * 2;

            if (currentY < 50) {
                contentStream.close();

                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);

                PDPageContentStream newContentStream = new PDPageContentStream(document, newPage);
                newContentStream.setFont(font, fontSize);

                currentY = newPage.getMediaBox().getHeight() - 50;

                return;
            }
        }
    }
}