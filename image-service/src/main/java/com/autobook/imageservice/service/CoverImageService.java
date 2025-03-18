package com.autobook.imageservice.service;

import com.autobook.imageservice.client.FreepikClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoverImageService {

    private final FreepikClient freepikClient;

    public Map<String, String> generateCoverImage(String title, String style) {
        try {
            // Generate and download the base image using FreepikClient
            String imagePath = freepikClient.generateAndDownloadImage(title, style);
            if (imagePath == null) {
                log.error("Failed to download base image for cover");
                throw new RuntimeException("Failed to generate cover image");
            }

            // Add the title overlay
            String outputPath = addTextOverlay(imagePath, title);

            Map<String, String> result = new HashMap<>();
            result.put("coverImagePath", outputPath);
            return result;
        } catch (Exception e) {
            log.error("Error generating cover image", e);
            throw new RuntimeException("Failed to generate cover image", e);
        }
    }

    private String addTextOverlay(String imagePath, String title) {
        try {
            // Read the base image
            BufferedImage originalImage = ImageIO.read(new File(imagePath));

            // Create a new image with the same dimensions
            BufferedImage newImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            // Create a graphics context for the new image
            Graphics2D g2d = newImage.createGraphics();

            // Draw the original image
            g2d.drawImage(originalImage, 0, 0, null);

            // Set up the font and color for the title
            Font titleFont = new Font("Arial", Font.BOLD, 48);
            g2d.setFont(titleFont);
            g2d.setColor(Color.WHITE);

            // Add shadow to make text readable
            g2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            // Calculate position for the title (center horizontally, towards the bottom)
            FontMetrics fm = g2d.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            int x = (originalImage.getWidth() - titleWidth) / 2;
            int y = originalImage.getHeight() - 100; // 100 pixels from the bottom

            // Draw shadow
            g2d.setColor(Color.BLACK);
            g2d.drawString(title, x + 2, y + 2);

            // Draw actual text
            g2d.setColor(Color.WHITE);
            g2d.drawString(title, x, y);

            // Clean up
            g2d.dispose();

            // Save the new image
            String outputFilename = "cover_with_text_" + UUID.randomUUID() + ".png";
            Path outputPath = Paths.get(imagePath).getParent().resolve(outputFilename);
            File outputFile = outputPath.toFile();
            ImageIO.write(newImage, "png", outputFile);

            return outputPath.toString();
        } catch (Exception e) {
            log.error("Error adding text overlay to cover image", e);
            return imagePath; // Return original image if overlay fails
        }
    }
}