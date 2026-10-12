package tc.impact.farmmanagement.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import tc.impact.farmmanagement.dto.response.MonthlyReportResponse;
import tc.impact.farmmanagement.entity.Customer;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.entity.Sale;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ReportService reportService;
    private final FarmContext farmContext;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ============================================================
    // EXCEL EXPORT
    // ============================================================

    public byte[] exportReportAsExcel(LocalDate from, LocalDate to) {
        MonthlyReportResponse report = reportService.buildReport(from, to);
        Farm farm = farmContext.getCurrentFarm();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle labelStyle = createLabelStyle(workbook);

            Sheet summary = workbook.createSheet("Summary");
            int row = 0;

            org.apache.poi.ss.usermodel.Row titleRow = summary.createRow(row++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(farm.getName() + " — Profit & Loss Report");
            titleCell.setCellStyle(titleStyle);

            org.apache.poi.ss.usermodel.Row periodRow = summary.createRow(row++);
            periodRow.createCell(0).setCellValue("Period:");
            periodRow.getCell(0).setCellStyle(labelStyle);
            periodRow.createCell(1).setCellValue(from.format(DATE_FMT) + " to " + to.format(DATE_FMT));

            row++;

            row = writeSectionHeader(summary, row, "REVENUE", headerStyle);
            row = writeKeyValue(summary, row, "Gross Sales", report.getRevenue().getGrossSales(), currencyStyle, labelStyle);
            row = writeKeyValue(summary, row, "Cash Collected", report.getRevenue().getCashCollected(), currencyStyle, labelStyle);
            row = writeKeyValue(summary, row, "Outstanding Dues", report.getRevenue().getOutstandingDues(), currencyStyle, labelStyle);

            if (report.getRevenue().getByProduct() != null && !report.getRevenue().getByProduct().isEmpty()) {
                row = writeSubHeader(summary, row, "Breakdown by Product");
                for (Map.Entry<String, BigDecimal> e : report.getRevenue().getByProduct().entrySet()) {
                    row = writeKeyValue(summary, row, "  " + e.getKey(), e.getValue(), currencyStyle, labelStyle);
                }
            }

            row++;

            row = writeSectionHeader(summary, row, "EXPENSES", headerStyle);
            row = writeKeyValue(summary, row, "Total Expenses", report.getExpenses().getTotal(), currencyStyle, labelStyle);

            if (report.getExpenses().getByCategory() != null && !report.getExpenses().getByCategory().isEmpty()) {
                row = writeSubHeader(summary, row, "Breakdown by Category");
                for (Map.Entry<String, BigDecimal> e : report.getExpenses().getByCategory().entrySet()) {
                    row = writeKeyValue(summary, row, "  " + e.getKey(), e.getValue(), currencyStyle, labelStyle);
                }
            }

            row++;

            row = writeSectionHeader(summary, row, "PRODUCTION", headerStyle);
            row = writeKeyValueNumber(summary, row, "Milk Produced (L)", report.getProduction().getMilkLiters(), numberStyle, labelStyle);
            row = writeKeyValueNumber(summary, row, "Eggs Produced", report.getProduction().getEggs() != null ? report.getProduction().getEggs().doubleValue() : 0.0, numberStyle, labelStyle);

            row++;

            row = writeSectionHeader(summary, row, "PROFIT", headerStyle);
            row = writeKeyValue(summary, row, "Net Profit", report.getProfit().getNetProfit(), currencyStyle, labelStyle);
            row = writeKeyValuePercent(summary, row, "Profit Margin", report.getProfit().getProfitMargin(), labelStyle);

            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    // ============================================================
    // PDF EXPORT — P&L REPORT
    // ============================================================

    public byte[] exportReportAsPdf(LocalDate from, LocalDate to) {
        MonthlyReportResponse report = reportService.buildReport(from, to);
        Farm farm = farmContext.getCurrentFarm();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.ITALIC, new Color(100, 100, 100));
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(20, 40, 80));
            Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font profitFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 128, 0));
            Font lossFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(200, 0, 0));

            // Farm header with logo
            addFarmHeader(document, farm);

            Paragraph subtitle = new Paragraph(
                    "Profit & Loss Report — " + from.format(DATE_FMT) + " to " + to.format(DATE_FMT),
                    subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Revenue
            addSection(document, "Revenue", sectionFont);
            addKeyValue(document, "Gross Sales", formatMoney(report.getRevenue().getGrossSales()), labelFont, valueFont);
            addKeyValue(document, "Cash Collected", formatMoney(report.getRevenue().getCashCollected()), labelFont, valueFont);
            addKeyValue(document, "Outstanding Dues", formatMoney(report.getRevenue().getOutstandingDues()), labelFont, valueFont);

            if (report.getRevenue().getByProduct() != null && !report.getRevenue().getByProduct().isEmpty()) {
                Paragraph sub = new Paragraph("By Product:", labelFont);
                sub.setSpacingBefore(6);
                document.add(sub);
                for (Map.Entry<String, BigDecimal> e : report.getRevenue().getByProduct().entrySet()) {
                    addKeyValue(document, "   • " + e.getKey(), formatMoney(e.getValue()), labelFont, labelFont);
                }
            }

            document.add(Chunk.NEWLINE);

            // Expenses
            addSection(document, "Expenses", sectionFont);
            addKeyValue(document, "Total Expenses", formatMoney(report.getExpenses().getTotal()), labelFont, valueFont);

            if (report.getExpenses().getByCategory() != null && !report.getExpenses().getByCategory().isEmpty()) {
                Paragraph sub = new Paragraph("By Category:", labelFont);
                sub.setSpacingBefore(6);
                document.add(sub);
                for (Map.Entry<String, BigDecimal> e : report.getExpenses().getByCategory().entrySet()) {
                    addKeyValue(document, "   • " + e.getKey(), formatMoney(e.getValue()), labelFont, labelFont);
                }
            }

            document.add(Chunk.NEWLINE);

            // Production
            addSection(document, "Production", sectionFont);
            addKeyValue(document, "Milk Produced",
                    formatNumber(report.getProduction().getMilkLiters()) + " L", labelFont, valueFont);
            addKeyValue(document, "Eggs Produced",
                    report.getProduction().getEggs() != null ? report.getProduction().getEggs() + " pcs" : "0 pcs",
                    labelFont, valueFont);

            document.add(Chunk.NEWLINE);

            // Profit summary
            addSection(document, "Profit Summary", sectionFont);
            BigDecimal net = report.getProfit().getNetProfit();
            Font resultFont = net.signum() >= 0 ? profitFont : lossFont;
            String resultLabel = net.signum() >= 0 ? "NET PROFIT" : "NET LOSS";
            Paragraph result = new Paragraph(resultLabel + ": " + formatMoney(net.abs()), resultFont);
            result.setAlignment(Element.ALIGN_CENTER);
            result.setSpacingBefore(10);
            document.add(result);

            Paragraph margin = new Paragraph("Profit Margin: " + report.getProfit().getProfitMargin() + "%", valueFont);
            margin.setAlignment(Element.ALIGN_CENTER);
            document.add(margin);

            Paragraph footer = new Paragraph(
                    "\nGenerated on " + LocalDate.now().format(DATE_FMT) + " | Farm Management System",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(150, 150, 150)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF export", e);
        }
    }

    // ============================================================
    // PDF EXPORT — CUSTOMER STATEMENT
    // ============================================================

    public byte[] exportCustomerStatementAsPdf(
            Customer customer,
            java.util.List<Sale> sales,       // ← fully-qualified to avoid OpenPDF's List
            BigDecimal totalSales,
            BigDecimal totalPaid,
            BigDecimal totalDue,
            LocalDate from,
            LocalDate to) {

        Farm farm = farmContext.getCurrentFarm();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, out);
            document.open();

            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(20, 40, 80));
            Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font tableHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font tableCellFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            addFarmHeader(document, farm);

            Paragraph title = new Paragraph("STATEMENT OF ACCOUNT",
                    new Font(Font.HELVETICA, 14, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            title.setSpacingAfter(20);
            document.add(title);

            // Customer details
            addSection(document, "Customer Details", sectionFont);
            addKeyValue(document, "Name", customer.getName(), labelFont, valueFont);
            if (customer.getPhone() != null && !customer.getPhone().isBlank()) {
                addKeyValue(document, "Phone", customer.getPhone(), labelFont, valueFont);
            }
            if (customer.getAddress() != null && !customer.getAddress().isBlank()) {
                addKeyValue(document, "Address", customer.getAddress(), labelFont, valueFont);
            }
            addKeyValue(document, "Period",
                    from.format(DATE_FMT) + "  —  " + to.format(DATE_FMT),
                    labelFont, valueFont);

            document.add(Chunk.NEWLINE);

            // Sales table
            addSection(document, "Transaction History", sectionFont);

            if (sales.isEmpty()) {
                Paragraph noSales = new Paragraph("No sales in this period.", labelFont);
                noSales.setSpacingBefore(10);
                document.add(noSales);
            } else {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1.2f, 3f, 1.2f, 1.3f, 1.3f});
                table.setSpacingBefore(8);

                String[] headers = {"Date", "Item", "Qty", "Amount", "Paid"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, tableHeaderFont));
                    cell.setBackgroundColor(new Color(20, 40, 80));
                    cell.setPadding(6);
                    table.addCell(cell);
                }

                for (Sale sale : sales) {
                    addTableCell(table, sale.getDate().format(DATE_FMT), tableCellFont, Element.ALIGN_LEFT);

                    String itemName = sale.getCustomProductName() != null
                            ? sale.getCustomProductName()
                            : sale.getCategory().getName();
                    String qtyUnit = sale.getQuantity().stripTrailingZeros().toPlainString()
                            + " " + (sale.getUnit().getAbbreviation() != null
                            ? sale.getUnit().getAbbreviation()
                            : sale.getUnit().getName());

                    addTableCell(table, itemName, tableCellFont, Element.ALIGN_LEFT);
                    addTableCell(table, qtyUnit, tableCellFont, Element.ALIGN_LEFT);
                    addTableCell(table, formatMoney(sale.getTotalAmount()), tableCellFont, Element.ALIGN_RIGHT);
                    addTableCell(table, formatMoney(sale.getPaidAmount()), tableCellFont, Element.ALIGN_RIGHT);
                }

                document.add(table);
            }

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Summary
            addSection(document, "Summary", sectionFont);
            addKeyValue(document, "Total Sales", formatMoney(totalSales), labelFont, valueFont);
            addKeyValue(document, "Total Paid", formatMoney(totalPaid), labelFont, valueFont);

            Paragraph dueLabel = new Paragraph("Outstanding Due",
                    new Font(Font.HELVETICA, 12, Font.BOLD));
            dueLabel.setSpacingBefore(10);
            document.add(dueLabel);

            Font dueFont = totalDue.signum() > 0
                    ? new Font(Font.HELVETICA, 18, Font.BOLD, new Color(200, 100, 0))
                    : new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0, 128, 0));

            Paragraph dueAmount = new Paragraph(formatMoney(totalDue), dueFont);
            document.add(dueAmount);

            Paragraph footer = new Paragraph(
                    "\nGenerated on " + LocalDate.now().format(DATE_FMT)
                            + " · Farm Management System",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(150, 150, 150)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate customer statement", e);
        }
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    // ============================================================
    // EXCEL HELPERS (unchanged)
    // ============================================================

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createLabelStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createNumberStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private int writeSectionHeader(Sheet sheet, int rowNum, String label, CellStyle style) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue(label);
        cell.setCellStyle(style);
        row.createCell(1).setCellStyle(style);
        return rowNum;
    }

    private int writeSubHeader(Sheet sheet, int rowNum, String label) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue(label);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        org.apache.poi.ss.usermodel.Font font = sheet.getWorkbook().createFont();
        font.setItalic(true);
        font.setBold(true);
        style.setFont(font);
        cell.setCellStyle(style);
        return rowNum;
    }

    private int writeKeyValue(Sheet sheet, int rowNum, String key, BigDecimal value,
                              CellStyle valueStyle, CellStyle labelStyle) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
        Cell k = row.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(labelStyle);
        Cell v = row.createCell(1);
        if (value != null) {
            v.setCellValue(value.doubleValue());
        }
        v.setCellStyle(valueStyle);
        return rowNum;
    }

    private int writeKeyValueNumber(Sheet sheet, int rowNum, String key, Double value,
                                    CellStyle valueStyle, CellStyle labelStyle) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
        Cell k = row.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(labelStyle);
        Cell v = row.createCell(1);
        if (value != null) v.setCellValue(value);
        v.setCellStyle(valueStyle);
        return rowNum;
    }

    private int writeKeyValuePercent(Sheet sheet, int rowNum, String key, BigDecimal value, CellStyle labelStyle) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
        Cell k = row.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(labelStyle);
        row.createCell(1).setCellValue((value != null ? value : BigDecimal.ZERO) + "%");
        return rowNum;
    }

    // ============================================================
    // PDF HELPERS
    // ============================================================

    private void addFarmHeader(Document document, Farm farm) throws DocumentException, IOException {
        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(20, 40, 80));
        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.ITALIC, new Color(100, 100, 100));

        if (farm.getLogoUrl() != null && !farm.getLogoUrl().isBlank()) {
            try {
                Image logo = Image.getInstance(new java.net.URL(farm.getLogoUrl()));
                logo.scaleToFit(60, 60);

                PdfPTable headerTable = new PdfPTable(2);
                headerTable.setWidthPercentage(100);
                headerTable.setWidths(new float[]{1, 4});

                PdfPCell logoCell = new PdfPCell(logo, false);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoCell.setPaddingRight(15);
                headerTable.addCell(logoCell);

                Paragraph namePara = new Paragraph(farm.getName(), titleFont);
                PdfPCell textCell = new PdfPCell();
                textCell.setBorder(Rectangle.NO_BORDER);
                textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                textCell.addElement(namePara);
                if (farm.getLocation() != null && !farm.getLocation().isBlank()) {
                    textCell.addElement(new Paragraph(farm.getLocation(), subtitleFont));
                }
                headerTable.addCell(textCell);

                document.add(headerTable);
            } catch (Exception e) {
                log.warn("Failed to load farm logo, falling back to text only: {}", e.getMessage());
                Paragraph farmName = new Paragraph(farm.getName(), titleFont);
                farmName.setAlignment(Element.ALIGN_CENTER);
                document.add(farmName);
                if (farm.getLocation() != null && !farm.getLocation().isBlank()) {
                    Paragraph loc = new Paragraph(farm.getLocation(), subtitleFont);
                    loc.setAlignment(Element.ALIGN_CENTER);
                    document.add(loc);
                }
            }
        } else {
            Paragraph farmName = new Paragraph(farm.getName(), titleFont);
            farmName.setAlignment(Element.ALIGN_CENTER);
            document.add(farmName);
            if (farm.getLocation() != null && !farm.getLocation().isBlank()) {
                Paragraph loc = new Paragraph(farm.getLocation(), subtitleFont);
                loc.setAlignment(Element.ALIGN_CENTER);
                document.add(loc);
            }
        }

        document.add(Chunk.NEWLINE);
    }

    private void addSection(Document document, String label, Font font) throws DocumentException {
        Paragraph p = new Paragraph(label, font);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        document.add(p);
    }

    private void addKeyValue(Document document, String key, String value, Font keyFont, Font valueFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1});

        PdfPCell kc = new PdfPCell(new Phrase(key, keyFont));
        kc.setBorder(Rectangle.NO_BORDER);
        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(kc);
        table.addCell(vc);
        document.add(table);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return "BDT " + String.format("%,.2f", value);
    }

    private String formatNumber(Double value) {
        if (value == null) return "0.00";
        return String.format("%,.2f", value);
    }
}