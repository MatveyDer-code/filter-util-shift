package org.example;

import org.apache.commons.cli.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("o", "output", true, "Каталог для выходных файлов");
        options.addOption("p", "prefix", true, "Префикс для выходных файлов");
        options.addOption("a", "append", false, "Режим добавления в файл");
        options.addOption("s", "short-stats", false, "Краткая статистика");
        options.addOption("f", "full-stats", false, "Полная статистика");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;


        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Ошибка при разборе аргументов командной строки: " + e.getMessage());
            formatter.printHelp("utility-name", options);
            return;
        }

        if (cmd.getArgList().isEmpty()) {
            System.out.println("Файлы для обработки не указаны.");
            formatter.printHelp("utility-name", options);
            return;
        }

        String outputPath = cmd.hasOption("o") ? cmd.getOptionValue("o") : ".";
        Path outputDir = Paths.get(outputPath);
        if (!Files.exists(outputDir)) {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                System.err.println("Ошибка при создании директории: " + outputPath + ". Используется дефолтная директория.");
                outputPath = ".";
            }
        }

        String prefix = cmd.hasOption("p") ? cmd.getOptionValue("p") : "";
        boolean append = cmd.hasOption("a");
        boolean shortStats = cmd.hasOption("s");
        boolean fullStats = cmd.hasOption("f");
        if (shortStats && fullStats) {
            shortStats = false;
        }

        processFiles(cmd.getArgList(), outputPath, prefix, append, shortStats, fullStats);
    }

    private static void processFiles(Iterable<String> inputFiles, String outputPath, String prefix, boolean append, boolean shortStats, boolean fullStats) {
        BigInteger minInt = null, maxInt = null, sumInt = BigInteger.ZERO;
        BigDecimal minFloat = null, maxFloat = null, sumFloat = BigDecimal.ZERO;
        int countInt = 0, countFloat = 0, countStr = 0, minStrLen = Integer.MAX_VALUE, maxStrLen = Integer.MIN_VALUE;

        StringBuilder intData = new StringBuilder();
        StringBuilder floatData = new StringBuilder();
        StringBuilder strData = new StringBuilder();

        for (String inputFile : inputFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    try {
                        BigDecimal bigDecimal = new BigDecimal(line);
                        if (bigDecimal.stripTrailingZeros().scale() <= 0) {
                            BigInteger value = bigDecimal.toBigIntegerExact();
                            intData.append(value.toString()).append("\n");
                            countInt++;
                            sumInt = sumInt.add(value);
                            minInt = (minInt == null || value.compareTo(minInt) < 0) ? value : minInt;
                            maxInt = (maxInt == null || value.compareTo(maxInt) > 0) ? value : maxInt;
                        } else {
                            floatData.append(bigDecimal.toString()).append("\n");
                            countFloat++;
                            sumFloat = sumFloat.add(bigDecimal);
                            minFloat = (minFloat == null || bigDecimal.compareTo(minFloat) < 0) ? bigDecimal : minFloat;
                            maxFloat = (maxFloat == null || bigDecimal.compareTo(maxFloat) > 0) ? bigDecimal : maxFloat;
                        }
                    } catch (NumberFormatException e) {
                        strData.append(line).append("\n");
                        countStr++;
                        int length = line.length();
                        minStrLen = Math.min(minStrLen, length);
                        maxStrLen = Math.max(maxStrLen, length);
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка при чтении файла: " + inputFile + ". Файл будет пропущен.");
            }
        }

        try {
            if (countInt > 0) {
                writeFile(outputPath, prefix + "integers.txt", intData.toString(), append);
            }
            if (countFloat > 0) {
                writeFile(outputPath, prefix + "floats.txt", floatData.toString(), append);
            }
            if (countStr > 0) {
                writeFile(outputPath, prefix + "strings.txt", strData.toString(), append);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при записи данных в файлы.");
        }

        if (shortStats || fullStats) {
            System.out.println("Количество целых чисел: " + countInt);
            System.out.println("Количество чисел с плавающей запятой: " + countFloat);
            System.out.println("Количество строк: " + countStr);
        }

        if (fullStats) {
            if (countInt > 0) {
                System.out.println("Минимальное целое число: " + minInt);
                System.out.println("Максимальное целое число: " + maxInt);
                System.out.println("Сумма целых чисел: " + sumInt);
                System.out.println("Среднее целых чисел: " + new BigDecimal(sumInt).divide(new BigDecimal(countInt), 10, RoundingMode.HALF_UP));
            }
            if (countFloat > 0) {
                System.out.println("Минимальное число с плавающей запятой: " + minFloat);
                System.out.println("Максимальное число с плавающей запятой: " + maxFloat);
                System.out.println("Сумма чисел с плавающей запятой: " + sumFloat);
                System.out.println("Среднее чисел с плавающей запятой: " + sumFloat.divide(new BigDecimal(countFloat), 10, RoundingMode.HALF_UP));
            }
            if (countStr > 0) {
                System.out.println("Минимальная длина строки: " + minStrLen);
                System.out.println("Максимальная длина строки: " + maxStrLen);
            }
        }
    }

    private static void writeFile(String outputPath, String fileName, String data, boolean append) throws IOException {
        if (!data.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(outputPath, fileName).toString(), append))) {
                writer.write(data);
            }
        }
    }
}
