package com.webselenium.helpers;

import com.webselenium.models.DuplicateGame;
import com.webselenium.models.GameCheckResult;
import com.webselenium.models.GameInventoryItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameReportExporter {

    private static final String[] COLUMNS = {
            "#", "Lobby", "Provider", "Game", "Mode", "Status", "Reason", "Screenshot"
    };

    private static final String[] INVENTORY_COLUMNS = {
            "#", "Lobby", "Provider", "Game"
    };

    private GameReportExporter() {}

    /**
     * Xuất danh sách kết quả ra file Excel (.xlsx). Trả về absolute path file đã ghi.
     */
    public static String exportToExcel(List<GameCheckResult> results, String dir) {
        File outDir = new File(dir);
        if (!outDir.exists()) outDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File outFile = new File(outDir, "GameScanReport_" + timestamp + ".xlsx");

        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(outFile)) {
            Sheet sheet = wb.createSheet("Games");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // PASS / FAIL styles
            CellStyle passStyle = wb.createCellStyle();
            passStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle failStyle = wb.createCellStyle();
            failStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row header = sheet.createRow(0);
            for (int c = 0; c < COLUMNS.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(COLUMNS[c]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int i = 0; i < results.size(); i++) {
                GameCheckResult r = results.get(i);
                Row row = sheet.createRow(i + 1);
                CellStyle rowStyle = r.playable ? passStyle : failStyle;

                createCell(row, 0, String.valueOf(i + 1), rowStyle);
                createCell(row, 1, r.lobby, rowStyle);
                createCell(row, 2, r.provider, rowStyle);
                createCell(row, 3, r.gameName, rowStyle);
                createCell(row, 4, r.mode == null ? "" : r.mode.name(), rowStyle);
                createCell(row, 5, r.playable ? "PASS" : "FAIL", rowStyle);
                createCell(row, 6, r.reason, rowStyle);
                createCell(row, 7, r.screenshotPath == null ? "" : r.screenshotPath, rowStyle);
            }

            // Summary sheet
            Sheet summary = wb.createSheet("Summary");
            int total = results.size();
            long pass = results.stream().filter(r -> r.playable).count();
            long fail = total - pass;
            Row s0 = summary.createRow(0); s0.createCell(0).setCellValue("Total"); s0.createCell(1).setCellValue(total);
            Row s1 = summary.createRow(1); s1.createCell(0).setCellValue("PASS"); s1.createCell(1).setCellValue(pass);
            Row s2 = summary.createRow(2); s2.createCell(0).setCellValue("FAIL"); s2.createCell(1).setCellValue(fail);

            // Auto size
            for (int c = 0; c < COLUMNS.length; c++) sheet.autoSizeColumn(c);
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            wb.write(fos);
            LogUtils.info("Excel report exported: " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            LogUtils.error("Failed to export Excel report", e);
            return null;
        }
    }

    /**
     * Tạo HTML table (string) để nhúng vào ExtentReport thông qua log markup.
     */
    public static String buildExtentHtmlTable(List<GameCheckResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='table table-sm table-bordered' style='font-size:12px'>");
        sb.append("<thead style='background:#e9ecef'>");
        sb.append("<tr>");
        for (String c : COLUMNS) sb.append("<th>").append(c).append("</th>");
        sb.append("</tr></thead><tbody>");
        for (int i = 0; i < results.size(); i++) {
            GameCheckResult r = results.get(i);
            String bg = r.playable ? "#d4edda" : "#f8d7da";
            sb.append("<tr style='background:").append(bg).append("'>");
            sb.append("<td>").append(i + 1).append("</td>");
            sb.append("<td>").append(escape(r.lobby)).append("</td>");
            sb.append("<td>").append(escape(r.provider)).append("</td>");
            sb.append("<td>").append(escape(r.gameName)).append("</td>");
            sb.append("<td>").append(r.mode == null ? "" : r.mode.name()).append("</td>");
            sb.append("<td><b>").append(r.playable ? "PASS" : "FAIL").append("</b></td>");
            sb.append("<td>").append(escape(r.reason)).append("</td>");
            sb.append("<td>");
            if (r.screenshotPath != null && !r.screenshotPath.isBlank()) {
                sb.append("<a href='file://").append(escape(r.screenshotPath))
                        .append("' target='_blank'>view</a>");
            }
            sb.append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    /**
     * Overload không có duplicates info — dùng khi không cần phân tích trùng.
     */
    public static String exportInventoryToExcel(List<GameInventoryItem> items, String dir) {
        return exportInventoryToExcel(items, null, dir);
    }

    /**
     * Export inventory ra Excel: Sheet Games, By Provider, và (tùy chọn) Duplicates.
     */
    public static String exportInventoryToExcel(List<GameInventoryItem> items,
                                                List<DuplicateGame> duplicates,
                                                String dir) {
        File outDir = new File(dir);
        if (!outDir.exists()) outDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File outFile = new File(outDir, "GameInventory_" + timestamp + ".xlsx");

        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(outFile)) {
            Sheet sheet = wb.createSheet("Games");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row header = sheet.createRow(0);
            for (int c = 0; c < INVENTORY_COLUMNS.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(INVENTORY_COLUMNS[c]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < items.size(); i++) {
                GameInventoryItem g = items.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(g.lobby);
                row.createCell(2).setCellValue(g.provider);
                row.createCell(3).setCellValue(g.gameName);
            }
            for (int c = 0; c < INVENTORY_COLUMNS.length; c++) sheet.autoSizeColumn(c);

            // Sheet "By Provider": group by provider, sort by count desc
            Sheet providerSheet = wb.createSheet("By Provider");
            Row ph = providerSheet.createRow(0);
            ph.createCell(0).setCellValue("Provider");
            ph.createCell(1).setCellValue("Game Count");
            ph.createCell(2).setCellValue("Share %");
            for (int c = 0; c < 3; c++) ph.getCell(c).setCellStyle(headerStyle);

            Map<String, Integer> providerCounts = new LinkedHashMap<>();
            for (GameInventoryItem g : items) {
                String p = (g.provider == null || g.provider.isBlank() || "-".equals(g.provider))
                        ? "(unknown)" : g.provider;
                providerCounts.merge(p, 1, Integer::sum);
            }
            List<Map.Entry<String, Integer>> sortedProviders = new ArrayList<>(providerCounts.entrySet());
            sortedProviders.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            int row = 1;
            int totalGames = items.size();
            for (Map.Entry<String, Integer> e : sortedProviders) {
                Row r = providerSheet.createRow(row++);
                r.createCell(0).setCellValue(e.getKey());
                r.createCell(1).setCellValue(e.getValue());
                double pct = totalGames == 0 ? 0 : (e.getValue() * 100.0 / totalGames);
                r.createCell(2).setCellValue(String.format("%.2f%%", pct));
            }
            Row totalProviderRow = providerSheet.createRow(row);
            totalProviderRow.createCell(0).setCellValue("TOTAL (" + providerCounts.size() + " providers)");
            totalProviderRow.createCell(1).setCellValue(totalGames);
            totalProviderRow.createCell(2).setCellValue("100%");
            for (int c = 0; c < 3; c++) totalProviderRow.getCell(c).setCellStyle(headerStyle);
            for (int c = 0; c < 3; c++) providerSheet.autoSizeColumn(c);

            // Sheet "Duplicates" (nếu có dữ liệu)
            if (duplicates != null && !duplicates.isEmpty()) {
                Sheet dupSheet = wb.createSheet("Duplicates");
                Row dh = dupSheet.createRow(0);
                dh.createCell(0).setCellValue("#");
                dh.createCell(1).setCellValue("Game");
                dh.createCell(2).setCellValue("Count");
                dh.createCell(3).setCellValue("Providers");
                for (int c = 0; c < 4; c++) dh.getCell(c).setCellStyle(headerStyle);

                CellStyle dupHighlight = wb.createCellStyle();
                dupHighlight.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                dupHighlight.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (int i = 0; i < duplicates.size(); i++) {
                    DuplicateGame d = duplicates.get(i);
                    Row r = dupSheet.createRow(i + 1);
                    Cell c0 = r.createCell(0); c0.setCellValue(i + 1); c0.setCellStyle(dupHighlight);
                    Cell c1 = r.createCell(1); c1.setCellValue(d.gameName); c1.setCellStyle(dupHighlight);
                    Cell c2 = r.createCell(2); c2.setCellValue(d.count()); c2.setCellStyle(dupHighlight);
                    Cell c3 = r.createCell(3); c3.setCellValue(String.join(", ", d.providers)); c3.setCellStyle(dupHighlight);
                }
                for (int c = 0; c < 4; c++) dupSheet.autoSizeColumn(c);
            }

            wb.write(fos);
            int dupCount = duplicates == null ? 0 : duplicates.size();
            LogUtils.info("Excel inventory exported: " + outFile.getAbsolutePath()
                    + " | " + providerCounts.size() + " providers, " + totalGames + " games"
                    + (dupCount > 0 ? ", " + dupCount + " trùng tên" : ""));
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            LogUtils.error("Failed to export inventory Excel", e);
            return null;
        }
    }

    /**
     * Tìm các game trùng tên (case-insensitive). Trả về list các DuplicateGame
     * với danh sách provider tương ứng, theo thứ tự xuất hiện lần đầu.
     */
    public static List<DuplicateGame> findDuplicates(List<GameInventoryItem> items) {
        Map<String, DuplicateGame> map = new LinkedHashMap<>();
        for (GameInventoryItem g : items) {
            if (g.gameName == null) continue;
            String key = g.gameName.trim().toLowerCase().replaceAll("\\s+", " ");
            if (key.isEmpty() || key.equals("(unnamed)")) continue;
            DuplicateGame d = map.computeIfAbsent(key, k -> new DuplicateGame(g.gameName));
            d.providers.add(g.provider == null ? "-" : g.provider);
        }
        List<DuplicateGame> result = new ArrayList<>();
        for (DuplicateGame d : map.values()) {
            if (d.count() > 1) result.add(d);
        }
        // Sort theo count desc
        result.sort((a, b) -> Integer.compare(b.count(), a.count()));
        return result;
    }

    public static String buildDuplicatesHtml(List<DuplicateGame> dupes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='table table-sm table-bordered' style='font-size:12px'>");
        sb.append("<thead style='background:#fff3cd'><tr>");
        sb.append("<th>#</th><th>Game</th><th>Count</th><th>Providers</th>");
        sb.append("</tr></thead><tbody>");
        for (int i = 0; i < dupes.size(); i++) {
            DuplicateGame d = dupes.get(i);
            sb.append("<tr>");
            sb.append("<td>").append(i + 1).append("</td>");
            sb.append("<td>").append(escape(d.gameName)).append("</td>");
            sb.append("<td>").append(d.count()).append("</td>");
            sb.append("<td>").append(escape(String.join(", ", d.providers))).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    /**
     * Dedupe theo gameName (case-insensitive, normalize whitespace), giữ thứ tự gốc.
     * Nếu cùng tên xuất hiện nhiều lần, giữ entry đầu tiên (kèm provider của nó).
     */
    public static List<GameInventoryItem> dedupeByName(List<GameInventoryItem> items) {
        List<GameInventoryItem> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (GameInventoryItem g : items) {
            String key = g.gameName == null ? "" : g.gameName.trim().toLowerCase().replaceAll("\\s+", " ");
            if (key.isEmpty() || key.equals("(unnamed)")) {
                result.add(g);
                continue;
            }
            if (seen.add(key)) result.add(g);
        }
        return result;
    }

    /**
     * Build HTML table tổng hợp số game theo provider (sort desc) cho ExtentReport.
     */
    public static String buildProviderSummaryHtml(List<GameInventoryItem> items) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (GameInventoryItem g : items) {
            String p = (g.provider == null || g.provider.isBlank() || "-".equals(g.provider))
                    ? "(unknown)" : g.provider;
            counts.merge(p, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int total = items.size();
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='table table-sm table-bordered' style='font-size:12px'>");
        sb.append("<thead style='background:#e9ecef'><tr>");
        sb.append("<th>Provider</th><th>Game Count</th><th>Share %</th>");
        sb.append("</tr></thead><tbody>");
        for (Map.Entry<String, Integer> e : sorted) {
            double pct = total == 0 ? 0 : (e.getValue() * 100.0 / total);
            sb.append("<tr>");
            sb.append("<td>").append(escape(e.getKey())).append("</td>");
            sb.append("<td>").append(e.getValue()).append("</td>");
            sb.append("<td>").append(String.format("%.2f%%", pct)).append("</td>");
            sb.append("</tr>");
        }
        sb.append("<tr style='font-weight:bold;background:#e9ecef'>");
        sb.append("<td>TOTAL (").append(counts.size()).append(" providers)</td>");
        sb.append("<td>").append(total).append("</td>");
        sb.append("<td>100%</td>");
        sb.append("</tr>");
        sb.append("</tbody></table>");
        return sb.toString();
    }

    public static String buildInventoryHtmlTable(List<GameInventoryItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='table table-sm table-bordered' style='font-size:12px'>");
        sb.append("<thead style='background:#e9ecef'><tr>");
        for (String c : INVENTORY_COLUMNS) sb.append("<th>").append(c).append("</th>");
        sb.append("</tr></thead><tbody>");
        for (int i = 0; i < items.size(); i++) {
            GameInventoryItem g = items.get(i);
            sb.append("<tr>");
            sb.append("<td>").append(i + 1).append("</td>");
            sb.append("<td>").append(escape(g.lobby)).append("</td>");
            sb.append("<td>").append(escape(g.provider)).append("</td>");
            sb.append("<td>").append(escape(g.gameName)).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}