package com.autobook.bookgenerationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class OllamaClient {

    // You can configure the CLI command and model via application.properties if desired.
    @Value("${ollama.cli.command:ollama}")
    private String ollamaCommand; // defaults to "ollama"

    @Value("${ollama.model:llama3.2}")
    private String model; // defaults to "llama3.2"

    /**
     * Generates text using the Ollama CLI.
     *
     * @param prompt The text prompt for Ollama.
     * @return The full generated response as a string.
     */
    public String generateText(String prompt) {
        try {
            // Build the command: e.g. "ollama run llama3.2 <prompt>"
            List<String> command = Arrays.asList(ollamaCommand, "run", model, prompt);
            log.info("Executing command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // Redirect error stream so that stderr is merged with stdout.
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read the entire output of the CLI process.
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            // Wait until the process completes.
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Ollama CLI returned non-zero exit code: {}", exitCode);
                return "Error: CLI exited with code " + exitCode;
            }

            String response = output.toString().trim();
            log.info("Received full response from Ollama CLI: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error generating text with Ollama CLI", e);
            return "Error generating text: " + e.getMessage();
        }
    }
}
