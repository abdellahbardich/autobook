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
import java.awt.*;
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
            // Safely convert bookId to Long regardless of whether it's Integer or Long
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

            // Extract other parameters
            String title = (String) request.get("title");
            String summary = (String) request.get("summary");
            String coverImagePath = (String) request.get("coverImagePath");
            List<Map<String, Object>> chapters = (List<Map<String, Object>>) request.get("chapters");
            String bookType = (String) request.get("bookType");

            // Normalize file paths - ensure they use proper directory separators for the OS
            coverImagePath = normalizePath(coverImagePath);
            if (chapters != null) {
                for (Map<String, Object> chapter : chapters) {
                    if (chapter.containsKey("illustrationPath")) {
                        String illPath = (String) chapter.get("illustrationPath");
                        if (illPath != null) {
                            chapter.put("illustrationPath", normalizePath(illPath));
                        }
                    }
                }
            }

            // Ensure directories exist
            Path pdfDir = Paths.get(pdfUploadDir);
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
            }

            // Create PDF document
            PDDocument document = new PDDocument();

            // Add cover page with title overlay
            if (coverImagePath != null && fileExists(coverImagePath)) {
                addFullPageCoverWithText(document, coverImagePath, title);
            } else {
                log.warn("Cover image not found at path: {}. Using placeholder instead.", coverImagePath);
                addPlaceholderCover(document, title);
            }

            // Add title page with summary
            addTitlePage(document, title, summary);

            // Add chapters - with images as full page backgrounds where available
            if (chapters != null) {
                addChaptersWithFullPageImages(document, chapters, "TEXT_IMAGE".equals(bookType));
            }

            // Save PDF to file
            String pdfFilename = "book_" + bookId + "_" + UUID.randomUUID() + ".pdf";
            Path pdfPath = pdfDir.resolve(pdfFilename);
            document.save(pdfPath.toFile());
            document.close();

            log.info("Generated PDF: {}", pdfPath);

            // Return path to the PDF
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
            // Ensure preview directory exists
            Path previewDir = Paths.get(previewUploadDir);
            if (!Files.exists(previewDir)) {
                Files.createDirectories(previewDir);
            }

            // Normalize path
            pdfPath = normalizePath(pdfPath);

            // Check if PDF file exists
            if (!fileExists(pdfPath)) {
                log.error("PDF file not found at path: {}", pdfPath);
                throw new IOException("PDF file not found at: " + pdfPath);
            }

            // Open the PDF
            PDDocument document = PDDocument.load(new File(pdfPath));

            // Render the first page as an image
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150); // render first page at 150 DPI

            // Save the image
            String previewFilename = "preview_" + UUID.randomUUID() + ".png";
            Path previewPath = previewDir.resolve(previewFilename);
            ImageIO.write(image, "PNG", previewPath.toFile());

            document.close();

            log.info("Generated preview image: {}", previewPath);

            // Return the path to the preview image
            Map<String, String> result = new HashMap<>();
            result.put("previewImagePath", previewPath.toString());

            return result;
        } catch (IOException e) {
            log.error("Error generating preview image", e);
            throw new RuntimeException("Failed to generate preview image", e);
        }
    }

    /**
     * Add a cover page with the image taking up the full page and text overlaid on top
     */
    private void addFullPageCoverWithText(PDDocument document, String coverImagePath, String title) throws IOException {
        PDPage coverPage = new PDPage(PDRectangle.A4);
        document.addPage(coverPage);

        // Log detailed information about the file we're trying to access
        log.info("Loading cover image from: {}", coverImagePath);
        File imageFile = new File(coverImagePath);
        log.info("File exists: {}, Readable: {}, Size: {} bytes",
                imageFile.exists(), imageFile.canRead(),
                imageFile.exists() ? imageFile.length() : 0);

        PDImageXObject coverImage = PDImageXObject.createFromFile(coverImagePath, document);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, coverPage)) {
            float pageWidth = coverPage.getMediaBox().getWidth();
            float pageHeight = coverPage.getMediaBox().getHeight();

            // Draw the image to fill the entire page
            contentStream.drawImage(coverImage, 0, 0, pageWidth, pageHeight);

            // Add title text overlay with semi-transparent background for readability
            PDFont titleFont = PDType1Font.HELVETICA_BOLD;
            float titleFontSize = 28;

            // Ensure title isn't too long for display
            String displayTitle = title;
            if (title.length() > 40) {
                displayTitle = title.substring(0, 37) + "...";
            }

            float titleWidth = titleFont.getStringWidth(displayTitle) / 1000 * titleFontSize;
            float titleX = (pageWidth - titleWidth) / 2;
            float titleY = 100; // Position near the bottom of the page

            // Add semi-transparent background rectangle for better text readability
            contentStream.setNonStrokingColor(0, 0, 0, 0.5f); // Semi-transparent black
            contentStream.addRect(titleX - 10, titleY - 10, titleWidth + 20, titleFontSize + 20);
            contentStream.fill();

            // Draw title text
            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.setNonStrokingColor(255, 255, 255); // White text
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

            // Draw a colored rectangle as background
            contentStream.setNonStrokingColor(230, 230, 250); // Light lavender
            contentStream.addRect(0, 0, pageWidth, pageHeight);
            contentStream.fill();

            // Add title text
            PDFont titleFont = PDType1Font.HELVETICA_BOLD;
            float titleFontSize = 28;

            // Ensure title isn't too long for display
            String displayTitle = title;
            if (title.length() > 40) {
                displayTitle = title.substring(0, 37) + "...";
            }

            float titleWidth = titleFont.getStringWidth(displayTitle) / 1000 * titleFontSize;
            float titleX = (pageWidth - titleWidth) / 2;
            float titleY = pageHeight / 2 + 50;

            // Draw title with shadow effect
            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);

            // Shadow
            contentStream.setNonStrokingColor(100, 100, 100);
            contentStream.newLineAtOffset(titleX + 2, titleY - 2);
            contentStream.showText(displayTitle);
            contentStream.endText();

            // Main text
            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.newLineAtOffset(titleX, titleY);
            contentStream.showText(displayTitle);
            contentStream.endText();

            // Add "Generated Cover" text
            PDFont noteFont = PDType1Font.HELVETICA;
            float noteFontSize = 14;
            String noteText = "AutoBook Generated Cover";
            float noteWidth = noteFont.getStringWidth(noteText) / 1000 * noteFontSize;
            float noteX = (pageWidth - noteWidth) / 2;
            float noteY = 100;

            contentStream.beginText();
            contentStream.setFont(noteFont, noteFontSize);
            contentStream.setNonStrokingColor(100, 100, 100);
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

            // Add title
            float titleFontSize = 24;
            float titleWidth = titleFont.getStringWidth(title) / 1000 * titleFontSize;
            float titleX = (pageWidth - titleWidth) / 2;
            float titleY = pageHeight - 150;

            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.newLineAtOffset(titleX, titleY);
            contentStream.showText(title);
            contentStream.endText();

            // Add summary with proper text wrapping
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
            // Safely convert chapterNumber to Integer
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
                chapterNumber = 0; // Default chapter number if missing
            }

            String chapterTitle = (String) chapter.get("chapterTitle");
            String chapterContent = (String) chapter.get("chapterContent");
            String illustrationPath = (String) chapter.get("illustrationPath");

            // Check if we have a valid illustration to use as background
            boolean hasIllustration = includeIllustrations && illustrationPath != null && fileExists(illustrationPath);

            PDPage chapterPage = new PDPage(PDRectangle.A4);
            document.addPage(chapterPage);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, chapterPage)) {
                float pageWidth = chapterPage.getMediaBox().getWidth();
                float pageHeight = chapterPage.getMediaBox().getHeight();

                // If we have an illustration, use it as a full-page background
                if (hasIllustration) {
                    try {
                        PDImageXObject illustration = PDImageXObject.createFromFile(illustrationPath, document);
                        contentStream.drawImage(illustration, 0, 0, pageWidth, pageHeight);

                        // Add semi-transparent overlay for text readability
                        contentStream.setNonStrokingColor(255, 255, 255, 0.7f); // Semi-transparent white
                        contentStream.addRect(30, 30, pageWidth - 60, pageHeight - 60);
                        contentStream.fill();
                    } catch (Exception e) {
                        log.warn("Failed to add illustration to chapter {}: {}", chapterNumber, e.getMessage());
                        hasIllustration = false;
                    }
                }

                // Chapter number
                String chapterText = "Chapter " + chapterNumber;
                float chapterNumFontSize = 18;
                float chapterNumWidth = chapterTitleFont.getStringWidth(chapterText) / 1000 * chapterNumFontSize;
                float chapterNumX = (pageWidth - chapterNumWidth) / 2;
                float chapterNumY = pageHeight - 150;

                contentStream.beginText();
                contentStream.setFont(chapterTitleFont, chapterNumFontSize);
                contentStream.setNonStrokingColor(0, 0, 0); // Black text
                contentStream.newLineAtOffset(chapterNumX, chapterNumY);
                contentStream.showText(chapterText);
                contentStream.endText();

                // Chapter title
                float titleFontSize = 22;
                float titleWidth = chapterTitleFont.getStringWidth(chapterTitle) / 1000 * titleFontSize;
                float titleX = (pageWidth - titleWidth) / 2;
                float titleY = chapterNumY - 40;

                contentStream.beginText();
                contentStream.setFont(chapterTitleFont, titleFontSize);
                contentStream.newLineAtOffset(titleX, titleY);
                contentStream.showText(chapterTitle);
                contentStream.endText();

                // Chapter content
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
        contentStream.setNonStrokingColor(0, 0, 0); // Ensure text is black

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

            // If we're too close to the bottom, create a new page
            if (currentY < 50) {
                // Close current content stream
                contentStream.close();

                // Create new page
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);

                // Create new content stream for the new page
                PDPageContentStream newContentStream = new PDPageContentStream(document, newPage);
                newContentStream.setFont(font, fontSize);
                newContentStream.setNonStrokingColor(0, 0, 0); // Ensure text is black

                // Reset Y position to top of page
                currentY = newPage.getMediaBox().getHeight() - 50;

                // Use the new content stream for remaining text
                return; // Exit this method as we can't continue with the closed stream
            }
        }
    }

    /**
     * Safely check if a file exists and is readable
     */
    private boolean fileExists(String filePath) {
        if (filePath == null) return false;

        File file = new File(filePath);
        return file.exists() && file.isFile() && file.canRead();
    }

    /**
     * Normalize file paths to ensure compatibility with the operating system
     */
    private String normalizePath(String path) {
        if (path == null) return null;

        // Replace backslashes with forward slashes for consistency
        path = path.replace('\\', '/');

        // If the path doesn't start with a drive letter (Windows) or / (Unix),
        // assume it's relative and make it absolute
        if (!path.matches("^[A-Za-z]:.*") && !path.startsWith("/")) {
            // This is a relative path - check if it exists directly or needs conversion
            File directFile = new File(path);
            if (directFile.exists()) {
                return directFile.getAbsolutePath();
            }

            // Try to resolve with the application's working directory
            File workingDirFile = new File(System.getProperty("user.dir"), path);
            if (workingDirFile.exists()) {
                return workingDirFile.getAbsolutePath();
            }

            // Return as is - it will be checked for existence later
            return path;
        }

        return path;
    }
}