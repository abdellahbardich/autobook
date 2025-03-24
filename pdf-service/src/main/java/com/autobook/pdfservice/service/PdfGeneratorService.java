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

    private static final String BASE_UPLOAD_DIR = "C:\\Users\\Youcode\\Desktop\\autobook\\s3\\uploads";
    private static final String IMAGES_DIR = "images";
    private static final String PDFS_DIR = "pdfs";
    private static final String PREVIEWS_DIR = "previews";

    @SuppressWarnings("unchecked")
    public Map<String, String> generatePdf(Map<String, Object> request) {
        PDDocument document = null;
        try {
            Long bookId = null;
            Object bookIdObj = request.get("bookId");
            if (bookIdObj instanceof Integer) {
                bookId = ((Integer) bookIdObj).longValue();
            } else if (bookIdObj instanceof Long) {
                bookId = (Long) bookIdObj;
            } else if (bookIdObj instanceof String) {
                bookId = Long.parseLong((String) bookIdObj);
            } else if (bookIdObj != null) {
                bookId = Long.valueOf(bookIdObj.toString());
            }
            if (bookId == null) {
                throw new IllegalArgumentException("Book ID is required and must be a number");
            }

            String title = (String) request.get("title");
            String summary = (String) request.get("summary");
            String coverImagePath = (String) request.get("coverImagePath");
            List<Map<String, Object>> chapters = (List<Map<String, Object>>) request.get("chapters");
            String bookType = (String) request.get("bookType");

            log.info("Original cover image path: {}", coverImagePath);

            coverImagePath = resolveImagePath(coverImagePath);
            log.info("Resolved cover image path: {}", coverImagePath);

            if (chapters != null) {
                for (Map<String, Object> chapter : chapters) {
                    if (chapter.containsKey("illustrationPath")) {
                        String illPath = (String) chapter.get("illustrationPath");
                        if (illPath != null) {
                            String resolvedPath = resolveImagePath(illPath);
                            chapter.put("illustrationPath", resolvedPath);
                            log.info("Resolved illustration path from '{}' to '{}'", illPath, resolvedPath);
                        }
                    }
                }
            }

            Path pdfDir = Paths.get(pdfUploadDir);
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
            }

            document = new PDDocument();

            if (coverImagePath != null && fileExists(coverImagePath)) {
                log.info("Using cover image at path: {}", coverImagePath);
                addFullPageCoverWithText(document, coverImagePath, title);
            } else {
                log.warn("Cover image not found at path: {}. Using placeholder instead.", coverImagePath);
                addPlaceholderCover(document, title);
            }

            addTitlePage(document, title, summary);

            if (chapters != null) {
                addChaptersWithFullPageImages(document, chapters, "TEXT_IMAGE".equals(bookType));
            }

            String pdfFilename = "book_" + bookId + "_" + UUID.randomUUID() + ".pdf";
            Path pdfPath = pdfDir.resolve(pdfFilename);
            document.save(pdfPath.toFile());
            log.info("Generated PDF: {}", pdfPath);

            Map<String, String> result = new HashMap<>();
            result.put("pdfPath", pdfPath.toString());
            return result;
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.error("Error closing PDF document", e);
                }
            }
        }
    }

    public Map<String, String> generatePreviewImage(String pdfPath) {
        PDDocument document = null;
        try {
            Path previewDir = Paths.get(previewUploadDir);
            if (!Files.exists(previewDir)) {
                Files.createDirectories(previewDir);
            }

            pdfPath = resolveCompletePdfPath(pdfPath);
            log.info("Resolved PDF path for preview generation: {}", pdfPath);

            if (!fileExists(pdfPath)) {
                log.error("PDF file not found at path: {}", pdfPath);
                throw new IOException("PDF file not found at: " + pdfPath);
            }

            document = PDDocument.load(new File(pdfPath));

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);

            String previewFilename = "preview_" + UUID.randomUUID() + ".png";
            Path previewPath = previewDir.resolve(previewFilename);
            ImageIO.write(image, "PNG", previewPath.toFile());
            log.info("Generated preview image: {}", previewPath);

            Map<String, String> result = new HashMap<>();
            result.put("previewImagePath", previewPath.toString());
            return result;
        } catch (IOException e) {
            log.error("Error generating preview image", e);
            throw new RuntimeException("Failed to generate preview image", e);
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.error("Error closing PDF document", e);
                }
            }
        }
    }


    private String resolveImagePath(String imagePath) {
        if (imagePath == null) return null;

        imagePath = imagePath.replaceAll("^/+", "");

        String filename = Paths.get(imagePath).getFileName().toString();

        if (imagePath.contains("uploads/images") || imagePath.contains("uploads\\images")) {
            return Paths.get(BASE_UPLOAD_DIR, imagePath.replace("uploads/", "").replace("uploads\\", "")).toString();
        }

        if (!imagePath.contains("/") && !imagePath.contains("\\")) {
            return Paths.get(BASE_UPLOAD_DIR, IMAGES_DIR, filename).toString();
        }

        if (imagePath.matches("^[A-Za-z]:.*")) {
            return imagePath;
        }

        return Paths.get(BASE_UPLOAD_DIR, IMAGES_DIR, filename).toString();
    }


    private String resolveCompletePdfPath(String pdfPath) {
        if (pdfPath == null) return null;

        String filename = Paths.get(pdfPath).getFileName().toString();

        if (!pdfPath.contains("/") && !pdfPath.contains("\\")) {
            Path fullPath = Paths.get(BASE_UPLOAD_DIR, PDFS_DIR, filename);
            log.info("Converting filename '{}' to full path: '{}'", pdfPath, fullPath);
            return fullPath.toString();
        }

        if (pdfPath.matches("^[A-Za-z]:.*")) {
            return pdfPath;
        }

        if (pdfPath.contains("uploads/pdfs") || pdfPath.contains("uploads\\pdfs")) {
            return Paths.get(BASE_UPLOAD_DIR, pdfPath.replace("uploads/", "").replace("uploads\\", "")).toString();
        }

        if (!pdfPath.startsWith("/") && !pdfPath.matches("^[A-Za-z]:.*")) {
            Path fullPath = Paths.get(BASE_UPLOAD_DIR, pdfPath);
            log.info("Converting relative path '{}' to full path: '{}'", pdfPath, fullPath);
            return fullPath.toString();
        }

        return pdfPath;
    }


    private void addFullPageCoverWithText(PDDocument document, String coverImagePath, String title) throws IOException {
        PDPage coverPage = new PDPage(PDRectangle.A4);
        document.addPage(coverPage);

        log.info("Loading cover image from: {}", coverImagePath);
        File imageFile = new File(coverImagePath);
        log.info("File exists: {}, Readable: {}, Size: {} bytes",
                imageFile.exists(), imageFile.canRead(),
                imageFile.exists() ? imageFile.length() : 0);

        if (!imageFile.exists() || !imageFile.canRead()) {
            log.warn("Cannot access cover image at: {}", coverImagePath);
            addPlaceholderCover(document, title);
            return;
        }

        PDImageXObject coverImage = PDImageXObject.createFromFile(coverImagePath, document);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, coverPage)) {
            float pageWidth = coverPage.getMediaBox().getWidth();
            float pageHeight = coverPage.getMediaBox().getHeight();

            contentStream.drawImage(coverImage, 0, 0, pageWidth, pageHeight);

            PDFont titleFont = PDType1Font.HELVETICA_BOLD;
            float titleFontSize = 28;

            String displayTitle = title;
            if (title.length() > 40) {
                displayTitle = title.substring(0, 37) + "...";
            }

            float titleWidth = titleFont.getStringWidth(displayTitle) / 1000 * titleFontSize;
            float titleX = (pageWidth - titleWidth) / 2;
            float titleY = 100;

            contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f, 0.6f);
            contentStream.addRect(titleX - 10, titleY - 10, titleWidth + 20, titleFontSize + 20);
            contentStream.fill();

            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.setNonStrokingColor(1f, 1f, 1f);
            contentStream.newLineAtOffset(titleX, titleY);
            contentStream.showText(displayTitle);
            contentStream.endText();
        }
    }

    private void addPlaceholderCover(PDDocument document, String title) throws IOException {
        PDPage coverPage = new PDPage(PDRectangle.A4);
        document.addPage(coverPage);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, coverPage)) {
            float pageWidth = coverPage.getMediaBox().getWidth();
            float pageHeight = coverPage.getMediaBox().getHeight();

            contentStream.setNonStrokingColor(230/255f, 230/255f, 250/255f);
            contentStream.addRect(0, 0, pageWidth, pageHeight);
            contentStream.fill();

            PDFont titleFont = PDType1Font.HELVETICA_BOLD;
            float titleFontSize = 28;

            String displayTitle = title;
            if (title.length() > 40) {
                displayTitle = title.substring(0, 37) + "...";
            }

            float titleWidth = titleFont.getStringWidth(displayTitle) / 1000 * titleFontSize;
            float titleX = (pageWidth - titleWidth) / 2;
            float titleY = pageHeight / 2 + 50;

            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.setNonStrokingColor(100/255f, 100/255f, 100/255f);
            contentStream.newLineAtOffset(titleX + 2, titleY - 2);
            contentStream.showText(displayTitle);
            contentStream.endText();

            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.setNonStrokingColor(0f, 0f, 0f);
            contentStream.newLineAtOffset(titleX, titleY);
            contentStream.showText(displayTitle);
            contentStream.endText();

            PDFont noteFont = PDType1Font.HELVETICA;
            float noteFontSize = 14;
            String noteText = "AutoBook Generated Cover";
            float noteWidth = noteFont.getStringWidth(noteText) / 1000 * noteFontSize;
            float noteX = (pageWidth - noteWidth) / 2;
            float noteY = 100;

            contentStream.beginText();
            contentStream.setFont(noteFont, noteFontSize);
            contentStream.setNonStrokingColor(100/255f, 100/255f, 100/255f);
            contentStream.newLineAtOffset(noteX, noteY);
            contentStream.showText(noteText);
            contentStream.endText();
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

    private void addChaptersWithFullPageImages(PDDocument document, List<Map<String, Object>> chapters, boolean includeIllustrations)
            throws IOException {
        PDFont chapterTitleFont = PDType1Font.HELVETICA_BOLD;
        PDFont textFont = PDType1Font.HELVETICA;

        for (Map<String, Object> chapter : chapters) {
            Integer chapterNumber = null;
            Object numObj = chapter.get("chapterNumber");
            if (numObj instanceof Integer) {
                chapterNumber = (Integer) numObj;
            } else if (numObj instanceof String) {
                chapterNumber = Integer.parseInt((String) numObj);
            } else if (numObj != null) {
                chapterNumber = Integer.parseInt(numObj.toString());
            }
            if (chapterNumber == null) {
                chapterNumber = 0;
            }

            String chapterTitle = (String) chapter.get("chapterTitle");
            String chapterContent = (String) chapter.get("chapterContent");
            String illustrationPath = (String) chapter.get("illustrationPath");

            log.info("Chapter {} illustration path: {}", chapterNumber, illustrationPath);

            boolean hasIllustration = includeIllustrations && illustrationPath != null && fileExists(illustrationPath);
            if (hasIllustration) {
                log.info("Confirmed illustration exists at: {}", illustrationPath);
            } else if (illustrationPath != null) {
                log.warn("Illustration file not found at: {}", illustrationPath);
            }

            PDPage chapterPage = new PDPage(PDRectangle.A4);
            document.addPage(chapterPage);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, chapterPage)) {
                float pageWidth = chapterPage.getMediaBox().getWidth();
                float pageHeight = chapterPage.getMediaBox().getHeight();

                if (hasIllustration) {
                    try {
                        PDPage illustrationPage = new PDPage(PDRectangle.A4);
                        document.addPage(illustrationPage);

                        try (PDPageContentStream illContentStream = new PDPageContentStream(document, illustrationPage)) {
                            log.info("Loading illustration from: {}", illustrationPath);
                            PDImageXObject illustration = PDImageXObject.createFromFile(illustrationPath, document);
                            illContentStream.drawImage(illustration, 0, 0, pageWidth, pageHeight);

                            float captionY = 20;
                            String caption = "Illustration for Chapter " + chapterNumber;

                            float captionWidth = textFont.getStringWidth(caption) / 1000 * 10;
                            float captionX = (pageWidth - captionWidth) / 2;

                            illContentStream.setNonStrokingColor(1f, 1f, 1f, 0.7f);
                            illContentStream.addRect(captionX - 5, captionY - 5, captionWidth + 10, 15);
                            illContentStream.fill();

                            illContentStream.beginText();
                            illContentStream.setFont(textFont, 10);
                            illContentStream.setNonStrokingColor(0f, 0f, 0f);
                            illContentStream.newLineAtOffset(captionX, captionY);
                            illContentStream.showText(caption);
                            illContentStream.endText();
                        }
                    } catch (Exception e) {
                        log.warn("Failed to add illustration to chapter {}: {}", chapterNumber, e.getMessage());
                        hasIllustration = false;
                    }
                }

                contentStream.setNonStrokingColor(1f, 1f, 1f);
                contentStream.addRect(0, 0, pageWidth, pageHeight);
                contentStream.fill();

                String chapterText = "Chapter " + chapterNumber;
                float chapterNumFontSize = 18;
                float chapterNumWidth = chapterTitleFont.getStringWidth(chapterText) / 1000 * chapterNumFontSize;
                float chapterNumX = (pageWidth - chapterNumWidth) / 2;
                float chapterNumY = pageHeight - 150;

                contentStream.beginText();
                contentStream.setFont(chapterTitleFont, chapterNumFontSize);
                contentStream.setNonStrokingColor(0f, 0f, 0f);
                contentStream.newLineAtOffset(chapterNumX, chapterNumY);
                contentStream.showText(chapterText);
                contentStream.endText();

                float titleFontSize = 22;
                float titleWidth = chapterTitleFont.getStringWidth(chapterTitle) / 1000 * titleFontSize;
                float titleX = (pageWidth - titleWidth) / 2;
                float titleY = chapterNumY - 40;

                contentStream.beginText();
                contentStream.setFont(chapterTitleFont, titleFontSize);
                contentStream.newLineAtOffset(titleX, titleY);
                contentStream.showText(chapterTitle);
                contentStream.endText();

                if (chapterContent != null && !chapterContent.isEmpty()) {
                    float margin = 50;
                    float contentStartY = titleY - 60;
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
        contentStream.setNonStrokingColor(0f, 0f, 0f);

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
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);

                PDPageContentStream newContentStream = new PDPageContentStream(document, newPage);
                newContentStream.setFont(font, fontSize);
                newContentStream.setNonStrokingColor(0f, 0f, 0f);
                newContentStream.close();
                return;
            }
        }
    }


    private boolean fileExists(String filePath) {
        if (filePath == null) return false;

        File file = new File(filePath);
        boolean exists = file.exists() && file.isFile() && file.canRead();

        if (!exists) {
            log.debug("File check failed. Path: {}, Exists: {}, IsFile: {}, Readable: {}",
                    filePath, file.exists(), file.exists() && file.isFile(), file.exists() && file.canRead());
        }

        return exists;
    }
}