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

    // Base directory for all uploads
    private static final String BASE_UPLOAD_DIR = "C:\\Users\\Youcode\\Desktop\\autobook\\s3\\uploads";
    private static final String IMAGES_DIR = "images";
    private static final String PDFS_DIR = "pdfs";
    private static final String PREVIEWS_DIR = "previews";

    @SuppressWarnings("unchecked")
    public Map<String, String> generatePdf(Map<String, Object> request) {
        PDDocument document = null;
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

            // Log original paths for debugging
            log.info("Original cover image path: {}", coverImagePath);

            // Resolve full paths for cover image
            coverImagePath = resolveImagePath(coverImagePath);
            log.info("Resolved cover image path: {}", coverImagePath);

            // Normalize illustration paths in chapters
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

            // Ensure directories exist
            Path pdfDir = Paths.get(pdfUploadDir);
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
            }

            // Create PDF document
            document = new PDDocument();

            // Add cover page with title overlay
            if (coverImagePath != null && fileExists(coverImagePath)) {
                log.info("Using cover image at path: {}", coverImagePath);
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
            log.info("Generated PDF: {}", pdfPath);

            // Return path to the PDF
            Map<String, String> result = new HashMap<>();
            result.put("pdfPath", pdfPath.toString());
            return result;
        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            // Always ensure the document is closed
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
            // Ensure preview directory exists
            Path previewDir = Paths.get(previewUploadDir);
            if (!Files.exists(previewDir)) {
                Files.createDirectories(previewDir);
            }

            // Get the properly resolved path to the PDF
            pdfPath = resolveCompletePdfPath(pdfPath);
            log.info("Resolved PDF path for preview generation: {}", pdfPath);

            // Check if PDF file exists
            if (!fileExists(pdfPath)) {
                log.error("PDF file not found at path: {}", pdfPath);
                throw new IOException("PDF file not found at: " + pdfPath);
            }

            // Open the PDF
            document = PDDocument.load(new File(pdfPath));

            // Render the first page as an image
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150); // render first page at 150 DPI

            // Save the image
            String previewFilename = "preview_" + UUID.randomUUID() + ".png";
            Path previewPath = previewDir.resolve(previewFilename);
            ImageIO.write(image, "PNG", previewPath.toFile());
            log.info("Generated preview image: {}", previewPath);

            // Return the path to the preview image
            Map<String, String> result = new HashMap<>();
            result.put("previewImagePath", previewPath.toString());
            return result;
        } catch (IOException e) {
            log.error("Error generating preview image", e);
            throw new RuntimeException("Failed to generate preview image", e);
        } finally {
            // Always ensure the document is closed
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.error("Error closing PDF document", e);
                }
            }
        }
    }

    /**
     * Resolve image paths (cover images and illustrations)
     */
    private String resolveImagePath(String imagePath) {
        if (imagePath == null) return null;

        // Remove any leading slashes for consistency
        imagePath = imagePath.replaceAll("^/+", "");

        // Extract just the filename if it's just a filename
        String filename = Paths.get(imagePath).getFileName().toString();

        // Check if the path already contains "uploads/images" or similar pattern
        if (imagePath.contains("uploads/images") || imagePath.contains("uploads\\images")) {
            // This is already a relative path with the correct structure
            // Convert to absolute path for loading
            return Paths.get(BASE_UPLOAD_DIR, imagePath.replace("uploads/", "").replace("uploads\\", "")).toString();
        }

        // If it's just a filename without path (e.g., "cover_123.png")
        if (!imagePath.contains("/") && !imagePath.contains("\\")) {
            return Paths.get(BASE_UPLOAD_DIR, IMAGES_DIR, filename).toString();
        }

        // If it's a full Windows path, use it directly
        if (imagePath.matches("^[A-Za-z]:.*")) {
            return imagePath;
        }

        // For any other case, try to make a reasonable guess
        return Paths.get(BASE_UPLOAD_DIR, IMAGES_DIR, filename).toString();
    }

    /**
     * Resolve a complete path to the PDF, handling both filenames and full paths
     */
    private String resolveCompletePdfPath(String pdfPath) {
        if (pdfPath == null) return null;

        // Extract just the filename
        String filename = Paths.get(pdfPath).getFileName().toString();

        // If this is just a filename (no path separators), prepend the PDF upload directory
        if (!pdfPath.contains("/") && !pdfPath.contains("\\")) {
            Path fullPath = Paths.get(BASE_UPLOAD_DIR, PDFS_DIR, filename);
            log.info("Converting filename '{}' to full path: '{}'", pdfPath, fullPath);
            return fullPath.toString();
        }

        // If it's a full Windows path, use it directly
        if (pdfPath.matches("^[A-Za-z]:.*")) {
            return pdfPath;
        }

        // If it contains "uploads/pdfs" or similar
        if (pdfPath.contains("uploads/pdfs") || pdfPath.contains("uploads\\pdfs")) {
            return Paths.get(BASE_UPLOAD_DIR, pdfPath.replace("uploads/", "").replace("uploads\\", "")).toString();
        }

        // If it's a relative path but not just a filename
        if (!pdfPath.startsWith("/") && !pdfPath.matches("^[A-Za-z]:.*")) {
            Path fullPath = Paths.get(BASE_UPLOAD_DIR, pdfPath);
            log.info("Converting relative path '{}' to full path: '{}'", pdfPath, fullPath);
            return fullPath.toString();
        }

        // It's already a full path
        return pdfPath;
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

        // Validate the file is accessible before proceeding
        if (!imageFile.exists() || !imageFile.canRead()) {
            log.warn("Cannot access cover image at: {}", coverImagePath);
            addPlaceholderCover(document, title);
            return;
        }

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
            contentStream.setNonStrokingColor(0.2f, 0.2f, 0.2f, 0.6f); // Semi-transparent dark gray
            contentStream.addRect(titleX - 10, titleY - 10, titleWidth + 20, titleFontSize + 20);
            contentStream.fill();

            // Draw title text
            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.setNonStrokingColor(1f, 1f, 1f); // White text
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
            contentStream.setNonStrokingColor(230/255f, 230/255f, 250/255f); // Light lavender
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
            contentStream.setNonStrokingColor(100/255f, 100/255f, 100/255f);
            contentStream.newLineAtOffset(titleX + 2, titleY - 2);
            contentStream.showText(displayTitle);
            contentStream.endText();

            // Main text
            contentStream.beginText();
            contentStream.setFont(titleFont, titleFontSize);
            contentStream.setNonStrokingColor(0f, 0f, 0f);
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

            // Log the illustration path for debugging
            log.info("Chapter {} illustration path: {}", chapterNumber, illustrationPath);

            // Check if we have a valid illustration to use as background
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

                // If we have an illustration, add it on its own page
                if (hasIllustration) {
                    try {
                        // Create a separate page for the illustration
                        PDPage illustrationPage = new PDPage(PDRectangle.A4);
                        document.addPage(illustrationPage);

                        try (PDPageContentStream illContentStream = new PDPageContentStream(document, illustrationPage)) {
                            log.info("Loading illustration from: {}", illustrationPath);
                            PDImageXObject illustration = PDImageXObject.createFromFile(illustrationPath, document);
                            illContentStream.drawImage(illustration, 0, 0, pageWidth, pageHeight);

                            // Add a small caption at the bottom if desired
                            float captionY = 20;
                            String caption = "Illustration for Chapter " + chapterNumber;

                            // Add the caption with a light background
                            float captionWidth = textFont.getStringWidth(caption) / 1000 * 10;
                            float captionX = (pageWidth - captionWidth) / 2;

                            // Light background for the caption
                            illContentStream.setNonStrokingColor(1f, 1f, 1f, 0.7f);
                            illContentStream.addRect(captionX - 5, captionY - 5, captionWidth + 10, 15);
                            illContentStream.fill();

                            // Caption text
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

                // Plain white background for text
                contentStream.setNonStrokingColor(1f, 1f, 1f);
                contentStream.addRect(0, 0, pageWidth, pageHeight);
                contentStream.fill();

                // Chapter number
                String chapterText = "Chapter " + chapterNumber;
                float chapterNumFontSize = 18;
                float chapterNumWidth = chapterTitleFont.getStringWidth(chapterText) / 1000 * chapterNumFontSize;
                float chapterNumX = (pageWidth - chapterNumWidth) / 2;
                float chapterNumY = pageHeight - 150;

                contentStream.beginText();
                contentStream.setFont(chapterTitleFont, chapterNumFontSize);
                contentStream.setNonStrokingColor(0f, 0f, 0f); // Black text
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
        contentStream.setNonStrokingColor(0f, 0f, 0f); // Ensure text is black

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
                // Create new page
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);

                // Create a new content stream for the new page
                PDPageContentStream newContentStream = new PDPageContentStream(document, newPage);
                newContentStream.setFont(font, fontSize);
                newContentStream.setNonStrokingColor(0f, 0f, 0f); // Ensure text is black
                newContentStream.close();
                return;
            }
        }
    }

    /**
     * Safely check if a file exists and is readable
     */
    private boolean fileExists(String filePath) {
        if (filePath == null) return false;

        File file = new File(filePath);
        boolean exists = file.exists() && file.isFile() && file.canRead();

        // Log detailed file info for debugging
        if (!exists) {
            log.debug("File check failed. Path: {}, Exists: {}, IsFile: {}, Readable: {}",
                    filePath, file.exists(), file.exists() && file.isFile(), file.exists() && file.canRead());
        }

        return exists;
    }
}