package com.water.data.service;

import com.water.data.model.RiskAssessment;
import com.water.data.model.RiskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting risk assessment reports to PDF and Excel formats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final RiskAssessmentService assessmentService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Exports risk assessment to Excel format (.xlsx).
     *
     * @param assessmentId Assessment ID
     * @param userId User ID (for access control)
     * @param riskLevel Optional filter by risk level
     * @return Excel file as byte array
     */
    public byte[] exportToExcel(String assessmentId, String userId, RiskAssessment.RiskLevel riskLevel) throws IOException {
        // Get assessment and results
        RiskAssessment assessment = assessmentService.getAssessment(assessmentId, userId);
        List<RiskResult> results = riskLevel != null
            ? assessmentService.getResultsByRiskLevel(assessmentId, userId, riskLevel)
            : assessmentService.getAssessmentResults(assessmentId, userId);

        RiskAssessmentService.AssessmentSummary summary =
            assessmentService.getAssessmentSummary(assessmentId, userId);

        log.info("Generating Excel export for assessment {} ({} results)", assessmentId, results.size());

        // Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle summaryLabelStyle = createSummaryLabelStyle(workbook);
            CellStyle summaryValueStyle = createSummaryValueStyle(workbook);
            CellStyle highRiskStyle = createRiskStyle(workbook, IndexedColors.RED);
            CellStyle mediumRiskStyle = createRiskStyle(workbook, IndexedColors.ORANGE);
            CellStyle lowRiskStyle = createRiskStyle(workbook, IndexedColors.GREEN);

            // Sheet 1: Summary
            createSummarySheet(workbook, assessment, summary, summaryLabelStyle, summaryValueStyle);

            // Sheet 2: Detailed Results
            createResultsSheet(workbook, results, headerStyle, highRiskStyle, mediumRiskStyle, lowRiskStyle);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Exports risk assessment to PDF format.
     *
     * @param assessmentId Assessment ID
     * @param userId User ID (for access control)
     * @param riskLevel Optional filter by risk level
     * @return PDF file as byte array
     */
    public byte[] exportToPdf(String assessmentId, String userId, RiskAssessment.RiskLevel riskLevel) throws Exception {
        // Get assessment and results
        RiskAssessment assessment = assessmentService.getAssessment(assessmentId, userId);
        List<RiskResult> results = riskLevel != null
            ? assessmentService.getResultsByRiskLevel(assessmentId, userId, riskLevel)
            : assessmentService.getAssessmentResults(assessmentId, userId);

        RiskAssessmentService.AssessmentSummary summary =
            assessmentService.getAssessmentSummary(assessmentId, userId);

        log.info("Generating PDF export for assessment {} ({} results)", assessmentId, results.size());

        // Generate HTML content
        String htmlContent = generateHtmlReport(assessment, summary, results, riskLevel);

        // Convert HTML to PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Generates filename for export.
     */
    public String generateFilename(RiskAssessment assessment, String format, RiskAssessment.RiskLevel riskLevel) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        String assessmentName = assessment.getName().replaceAll("[^a-zA-Z0-9]", "_");
        String filterSuffix = riskLevel != null ? "_" + riskLevel.name() : "";

        return String.format("risk_assessment_%s%s_%s.%s",
            assessmentName, filterSuffix, timestamp, format);
    }

    // ==================== Excel Helper Methods ====================

    private void createSummarySheet(Workbook workbook, RiskAssessment assessment,
                                   RiskAssessmentService.AssessmentSummary summary,
                                   CellStyle labelStyle, CellStyle valueStyle) {
        Sheet sheet = workbook.createSheet("Summary");
        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Risk Assessment Summary Report");
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // Blank row

        // Assessment Info
        addSummaryRow(sheet, rowNum++, "Assessment Name:", assessment.getName(), labelStyle, valueStyle);
        addSummaryRow(sheet, rowNum++, "Description:", assessment.getDescription(), labelStyle, valueStyle);
        addSummaryRow(sheet, rowNum++, "Algorithm Version:", assessment.getAlgorithmVersion(), labelStyle, valueStyle);
        addSummaryRow(sheet, rowNum++, "Created At:", assessment.getCreatedAt().format(DATE_FORMATTER), labelStyle, valueStyle);
        addSummaryRow(sheet, rowNum++, "Total Communities:", summary.getTotalCommunities().toString(), labelStyle, valueStyle);
        addSummaryRow(sheet, rowNum++, "Calculation Duration:", summary.getCalculationDurationMs() + " ms", labelStyle, valueStyle);

        rowNum++; // Blank row

        // Risk Distribution
        addSummaryRow(sheet, rowNum++, "High Risk Count:", summary.getHighRiskCount().toString(), labelStyle, valueStyle);
        addSummaryRow(sheet, rowNum++, "Medium Risk Count:", summary.getMediumRiskCount().toString(), labelStyle, valueStyle);
        addSummaryRow(sheet, rowNum++, "Low Risk Count:", summary.getLowRiskCount().toString(), labelStyle, valueStyle);

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createResultsSheet(Workbook workbook, List<RiskResult> results,
                                   CellStyle headerStyle, CellStyle highRiskStyle,
                                   CellStyle mediumRiskStyle, CellStyle lowRiskStyle) {
        Sheet sheet = workbook.createSheet("Results");
        int rowNum = 0;

        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
            "Community ID", "Risk Score", "Risk Level", "Water Quality",
            "Access Distance", "Infrastructure", "Population Pressure",
            "Confidence", "Samples", "Calculated At"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (RiskResult result : results) {
            Row row = sheet.createRow(rowNum++);

            // Determine risk style
            CellStyle riskStyle = switch (result.getRiskLevel()) {
                case HIGH -> highRiskStyle;
                case MEDIUM -> mediumRiskStyle;
                case LOW -> lowRiskStyle;
            };

            // Community ID
            row.createCell(0).setCellValue(result.getCommunityId());

            // Risk Score
            Cell scoreCell = row.createCell(1);
            scoreCell.setCellValue(result.getRiskScore());
            scoreCell.setCellStyle(riskStyle);

            // Risk Level
            Cell levelCell = row.createCell(2);
            levelCell.setCellValue(result.getRiskLevel().name());
            levelCell.setCellStyle(riskStyle);

            // Component scores
            row.createCell(3).setCellValue(result.getWaterQualityScore());
            row.createCell(4).setCellValue(result.getAccessDistanceScore());
            row.createCell(5).setCellValue(result.getInfrastructureScore());
            row.createCell(6).setCellValue(result.getPopulationPressureScore());

            // Confidence and samples
            row.createCell(7).setCellValue(result.getConfidenceLevel().name());
            row.createCell(8).setCellValue(result.getSampleCount());

            // Calculated at
            row.createCell(9).setCellValue(result.getCalculatedAt().format(DATE_FORMATTER));
        }

        // Auto-size all columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);
    }

    private void addSummaryRow(Sheet sheet, int rowNum, String label, String value,
                              CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        if (value != null && !value.equals("null")) {
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(value);
            valueCell.setCellStyle(valueStyle);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createSummaryLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createSummaryValueStyle(Workbook workbook) {
        return workbook.createCellStyle();
    }

    private CellStyle createRiskStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(color.getIndex());
        style.setFont(font);
        return style;
    }

    // ==================== PDF Helper Methods ====================

    private String generateHtmlReport(RiskAssessment assessment,
                                     RiskAssessmentService.AssessmentSummary summary,
                                     List<RiskResult> results,
                                     RiskAssessment.RiskLevel riskLevel) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'/>");
        html.append("<style>");
        html.append(getPdfStyles());
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Title
        html.append("<h1>Risk Assessment Report</h1>");

        // Summary section
        html.append("<div class='section'>");
        html.append("<h2>Assessment Summary</h2>");
        html.append("<table class='summary-table'>");
        addHtmlSummaryRow(html, "Assessment Name", assessment.getName());
        if (assessment.getDescription() != null) {
            addHtmlSummaryRow(html, "Description", assessment.getDescription());
        }
        addHtmlSummaryRow(html, "Algorithm Version", assessment.getAlgorithmVersion());
        addHtmlSummaryRow(html, "Created At", assessment.getCreatedAt().format(DATE_FORMATTER));
        addHtmlSummaryRow(html, "Total Communities", summary.getTotalCommunities().toString());
        addHtmlSummaryRow(html, "Calculation Duration", summary.getCalculationDurationMs() + " ms");
        html.append("</table>");
        html.append("</div>");

        // Risk distribution
        html.append("<div class='section'>");
        html.append("<h2>Risk Distribution</h2>");
        html.append("<table class='summary-table'>");
        addHtmlSummaryRow(html, "High Risk Communities", summary.getHighRiskCount().toString());
        addHtmlSummaryRow(html, "Medium Risk Communities", summary.getMediumRiskCount().toString());
        addHtmlSummaryRow(html, "Low Risk Communities", summary.getLowRiskCount().toString());
        html.append("</table>");
        html.append("</div>");

        // Results table
        if (riskLevel != null) {
            html.append("<h2>").append(riskLevel.name()).append(" Risk Communities</h2>");
        } else {
            html.append("<h2>Detailed Results</h2>");
        }

        html.append("<table class='results-table'>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Community ID</th>");
        html.append("<th>Risk Score</th>");
        html.append("<th>Risk Level</th>");
        html.append("<th>Water Quality</th>");
        html.append("<th>Access Distance</th>");
        html.append("<th>Infrastructure</th>");
        html.append("<th>Population</th>");
        html.append("<th>Confidence</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        for (RiskResult result : results) {
            String riskClass = result.getRiskLevel().name().toLowerCase();
            html.append("<tr>");
            html.append("<td>").append(result.getCommunityId()).append("</td>");
            html.append("<td class='risk-").append(riskClass).append("'>")
                .append(result.getRiskScore()).append("</td>");
            html.append("<td class='risk-").append(riskClass).append("'>")
                .append(result.getRiskLevel().name()).append("</td>");
            html.append("<td>").append(result.getWaterQualityScore()).append("</td>");
            html.append("<td>").append(result.getAccessDistanceScore()).append("</td>");
            html.append("<td>").append(result.getInfrastructureScore()).append("</td>");
            html.append("<td>").append(result.getPopulationPressureScore()).append("</td>");
            html.append("<td>").append(result.getConfidenceLevel().name()).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Generated on ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("</p>");
        html.append("<p>WaterAccessOptimizer Risk Assessment Report</p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private void addHtmlSummaryRow(StringBuilder html, String label, String value) {
        html.append("<tr>");
        html.append("<td class='label'>").append(label).append(":</td>");
        html.append("<td>").append(value).append("</td>");
        html.append("</tr>");
    }

    private String getPdfStyles() {
        return """
            body {
                font-family: Arial, Helvetica, sans-serif;
                margin: 40px;
                color: #333;
            }
            h1 {
                color: #2c3e50;
                border-bottom: 3px solid #3498db;
                padding-bottom: 10px;
            }
            h2 {
                color: #34495e;
                margin-top: 30px;
                border-bottom: 1px solid #bdc3c7;
                padding-bottom: 5px;
            }
            .section {
                margin: 20px 0;
            }
            .summary-table {
                border-collapse: collapse;
                margin: 15px 0;
            }
            .summary-table td {
                padding: 8px 12px;
                border-bottom: 1px solid #ecf0f1;
            }
            .summary-table td.label {
                font-weight: bold;
                width: 200px;
            }
            .results-table {
                width: 100%;
                border-collapse: collapse;
                margin: 15px 0;
                font-size: 11px;
            }
            .results-table th {
                background-color: #34495e;
                color: white;
                padding: 10px 8px;
                text-align: left;
                font-weight: bold;
            }
            .results-table td {
                padding: 8px;
                border-bottom: 1px solid #ecf0f1;
            }
            .results-table tr:nth-child(even) {
                background-color: #f8f9fa;
            }
            .risk-high {
                color: #c0392b;
                font-weight: bold;
            }
            .risk-medium {
                color: #f39c12;
                font-weight: bold;
            }
            .risk-low {
                color: #27ae60;
                font-weight: bold;
            }
            .footer {
                margin-top: 40px;
                padding-top: 20px;
                border-top: 1px solid #bdc3c7;
                text-align: center;
                font-size: 10px;
                color: #7f8c8d;
            }
            """;
    }
}
