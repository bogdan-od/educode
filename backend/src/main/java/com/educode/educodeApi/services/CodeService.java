package com.educode.educodeApi.services;

import com.educode.educodeApi.exceptions.ContainerBuildingException;
import com.educode.educodeApi.exceptions.ContainerExecutionException;
import com.educode.educodeApi.exceptions.ContainerTimeoutException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Сервіс для виконання коду в різних мовах програмування через Docker контейнери.
 * Підтримує широкий спектр мов та забезпечує безпечне виконання коду з обмеженнями по пам'яті та часу.
 */
@Service
public class CodeService {
    /**
     * Виконує код у Docker контейнері для вказаної мови програмування.
     *
     * @param lang Мова програмування для виконання коду
     * @param langVersion Версія мови програмування
     * @param code Код для виконання
     * @param inputData Вхідні дані для програми
     * @param memoryLimit Обмеження пам'яті в мегабайтах
     * @param timeLimit Обмеження часу виконання в секундах
     * @return Результат виконання коду
     * @throws Exception Різні винятки, що можуть виникнути під час виконання
     */
    public String executeCode(String lang, String langVersion, String code, String inputData, int memoryLimit, float timeLimit) throws Exception {
        final double finalTimeLimit = timeLimit * 1000.0f;
        final String[] output = {""};

        // Список підтримуваних мов програмування
        List<String> supportedLanguages = Arrays.asList(
                "perl",
                "haskell",
                "dart",
                "c",
                "java",
                "mono",
                "cpp",
                "node",
                "pypy",
                "rust",
                "go",
                "d-gdc",
                "cpp-with-gmp",
                "php",
                "ruby",
                "dotnet",
                "python",
                "swift",
                "kotlin",
                "assembler",
                "pascal",
                "lua"
        );

        // Список підтримуваних Docker контейнерів
        List<String> supportedContainers = Arrays.asList(
                "assembler-compiler:latest",
                "cpp-with-gmp-compiler:14.2",
                "dotnet-compiler:8.0",
                "java-compiler:23",
                "node-compiler:22.11.0",
                "pypy-compiler:3.10",
                "rust-compiler:1.82.0",
                "c-compiler:10.2",
                "dart-compiler:3.5",
                "go-compiler:1.20",
                "kotlin-compiler:2.0.21",
                "perl-compiler:5.32",
                "python-compiler:3.11",
                "swift-compiler:5.6",
                "cpp-compiler:14.2",
                "d-gdc-compiler:14.2",
                "haskell-compiler:8.8",
                "mono-compiler:6.12",
                "php-compiler:8.2",
                "ruby-compiler:3.3",
                "lua-compiler:latest",
                "pascal-compiler:latest"
        );

        String dockerImage = lang + "-compiler:" + langVersion;

        // Перевірка підтримки мови
        if (!supportedLanguages.contains(lang) || !supportedContainers.contains(dockerImage))
            throw new IllegalArgumentException("Unsupported language: " + lang);

        String containerName = UUID.randomUUID().toString();

        // Створення контейнера -
        // --memory - обмеження пам'яті
        // --network none - не дозволяє контейнеру використовувати мережу
        // tail -f /dev/null - запуск контейнера в режимі очікування
        ProcessBuilder createContainer = new ProcessBuilder(
                "docker", "run", "--name", containerName, String.format("--memory=%dm", memoryLimit), "-d", "--network", "none", dockerImage, "tail -f /dev/null"
        );

        // Запуск контейнера
        createContainer.start().waitFor();

        // Збірка коду в контейнері за допомогою entrypoint.sh з опцією build
        ProcessBuilder buildContainer = new ProcessBuilder(
                "docker", "exec", "-i", containerName, "./entrypoint.sh", "build", code
        );

        Process buildProcess = buildContainer.start();

        // Перевірка на помилки при збірці коду
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(buildProcess.getErrorStream()))) {
            StringBuilder errorOutput = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine).append("\n");
            }

            int buildExitCode = buildProcess.waitFor();
            if (buildExitCode != 0) {
                throw new ContainerBuildingException(String.valueOf(errorOutput), buildExitCode);
            }
        } finally {
            buildProcess.destroy();
        }

        // Запуск коду в контейнері за допомогою entrypoint.sh з опцією run
        ProcessBuilder startContainer = new ProcessBuilder(
                "docker", "exec", "-i", containerName, "./entrypoint.sh", "run"
        );

        Process containerProcess = startContainer.start();
        long startTime = System.currentTimeMillis();
        Exception[] endException = new Exception[1];

        // Потік для відстеження часу виконання
        Thread timeThread = new Thread(() -> {
            try {
                while (System.currentTimeMillis() - startTime < finalTimeLimit) {
                    if (!containerProcess.isAlive())
                        return; // Завершуємо потік, якщо контейнер завершився

                    Thread.sleep(100);
                }

                // Якщо час вийшов, зупиняємо контейнер
                if (System.currentTimeMillis() - startTime >= finalTimeLimit) {
                    containerProcess.destroy();
                    throw new ContainerTimeoutException("Container execution timed out after " + finalTimeLimit + " milliseconds.");
                }
            } catch (InterruptedException | ContainerTimeoutException e) {
                endException[0] = e;
                Thread.currentThread().interrupt();
            }
        });

        // Потік для читання виводу контейнера
        Thread readThread = new Thread(() -> {
            try (OutputStream os = containerProcess.getOutputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(containerProcess.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(containerProcess.getErrorStream()))) {

                // Передаємо вхідні дані в контейнер
                if (inputData != null && !inputData.isEmpty()) {
                    os.write((inputData + "\n").getBytes());
                    os.flush();
                }

                // Читаємо вивід контейнера
                StringBuilder outputBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
                output[0] = outputBuilder.toString();
                if (output[0].endsWith("\n")) {
                    output[0] = output[0].substring(0, output[0].length() - 1);
                }

                // Читаємо помилки контейнера (якщо є)
                StringBuilder errorOutput = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append("\n");
                }

                // Очікуємо завершення контейнера
                int exitCode = containerProcess.waitFor();
                if (exitCode != 0) {
                    throw new ContainerExecutionException(String.valueOf(errorOutput), exitCode);
                }

            } catch (InterruptedException | IOException | ContainerExecutionException e) {
                endException[0] = e;
            } finally {
                containerProcess.destroy(); // Зупиняємо контейнер
            }
        });

        timeThread.start();
        readThread.start();

        // Видалення контейнер після виконання
        // Опція -f використовується для безумовного видалення контейнера, навіть якщо він не завершився
        ProcessBuilder removeContainer = new ProcessBuilder(
                "docker", "rm", "-f", containerName
        );

        // Очікуємо завершення потоків
        try {
            timeThread.join();
        } catch (InterruptedException exception) { // Коли ми у timeThread викликаємо interrupt, то тут спрацює цей виняток
            readThread.interrupt();
            removeContainer.start().waitFor();
            if (endException[0] != null)
                throw endException[0];

            return output[0];
        }

        removeContainer.start().waitFor();
        if (endException[0] != null)
            throw endException[0];

        return output[0];
    }
}
