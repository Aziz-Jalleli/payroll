package com.same.payroll.pdf;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.same.payroll.dto.PayrollCalculationDto;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Renders a single PayrollCalculationDto as a one-page payslip PDF,
 * modeled on the existing "FICHE DE PAIE" layout (company header, employee
 * identity block, a Rubriques/Base-Nbr/Taux/Gains/Retenues table, ending
 * in Salaire Net).
 *
 * Uses OpenPDF (LGPL/MPL) rather than iText 7, for license reasons on a
 * closed-source product - API is the iText-1.x style (com.lowagie.text.*).
 */
@Component
public class PayslipPdfGenerator {

    private static final String COMPANY_NAME = "ACME";
    private static final String CURRENCY_SUFFIX = "";

    private static final Font FONT_COMPANY = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.darkGray);
    private static final Font FONT_LABEL = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font FONT_VALUE = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font FONT_TABLE_HEADER = new Font(Font.HELVETICA, 8, Font.BOLD, Color.white);
    private static final Font FONT_ROW = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font FONT_ROW_BOLD = new Font(Font.HELVETICA, 8, Font.BOLD);

    private static final Color HEADER_BG = new Color(43, 58, 84);
    private static final Color SUBTOTAL_BG = new Color(240, 242, 245);

    /**
     * Generates the payslip PDF bytes for one employee/period calculation.
     * employeeCnss and isChefFamille aren't on PayrollCalculationDto - pass
     * them in separately (sourced from Employee/FamilySituation by the
     * caller) since the calculation DTO is deliberately just numbers.
     */
    public byte[] generate(PayrollCalculationDto calc, String fonction, String numeroCnss, boolean isChefFamille) {
        try {
            Document document = new Document(PageSize.A5);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.setMargins(28, 28, 24, 24);
            document.open();

            addHeader(document, calc);
            addIdentityBlock(document, calc, fonction, numeroCnss, isChefFamille);
            addRubriquesTable(document, calc);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate payslip PDF", e);
        }
    }

    private void addHeader(Document document, PayrollCalculationDto calc) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        Paragraph company = new Paragraph(COMPANY_NAME, FONT_COMPANY);
        document.add(company);

        Paragraph title = new Paragraph("FICHE DE PAIE", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(4);

        String monthLabel = Month.of(calc.getMonth())
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        monthLabel = capitalize(monthLabel);
        Paragraph period = new Paragraph(monthLabel + " " + calc.getYear(), FONT_SUBTITLE);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(8);

        document.add(title);
        document.add(period);

        LineSeparator separator = new LineSeparator();
        document.add(new Chunk(separator));
        document.add(Chunk.NEWLINE);
    }

    private void addIdentityBlock(Document document, PayrollCalculationDto calc, String fonction,
                                  String numeroCnss, boolean isChefFamille) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 2f});
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        addIdentityRow(table, "Matricule:", nullToDash(calc.getEmployeeCode()));
        addIdentityRow(table, "Nom & Prenom:", nullToDash(calc.getFullName()));
        addIdentityRow(table, "Fonction:", nullToDash(fonction));
        addIdentityRow(table, "N\u00b0 CNSS:", nullToDash(numeroCnss));
        addIdentityRow(table, "Chef de famille:", isChefFamille ? "Oui" : "Non");

        document.add(table);

        LineSeparator separator = new LineSeparator();
        document.add(new Chunk(separator));
        document.add(Chunk.NEWLINE);
    }

    private void addIdentityRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_VALUE));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(3);
        table.addCell(valueCell);
    }

    private void addRubriquesTable(Document document, PayrollCalculationDto calc) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.4f, 1.1f, 0.9f, 1.3f});

        addTableHeaderCell(table, "Rubriques");
        addTableHeaderCell(table, "Base / Nbr");
        addTableHeaderCell(table, "Taux");
        addTableHeaderCell(table, "Gains / Retenues");

        // Salaire de base
        addRow(table, "Salaire de base",
                formatDays(calc.getDaysWorked()) + " j", "-",
                formatAmount(calc.getEarnedBaseSalary()), false);

        // Allowances - PayrollCalculationDto only carries the aggregated
        // totalAllowances, not a per-type breakdown (Allowance entity has
        // TRANSPORT/HOUSING/MEAL/PHONE/REPRESENTATION/OTHER, but the calc
        // service sums them before this DTO is built). Shown as one line;
        // pass the Allowance list separately into this generator if you
        // want each type broken out like in the reference design.
        if (isPositive(calc.getTotalAllowances())) {
            addRow(table, "Primes et indemnites", "-", "-",
                    formatAmount(calc.getTotalAllowances()), false);
        }

        if (isPositive(calc.getOvertimePay())) {
            addRow(table, "Heures supplementaires", "-", "-",
                    formatAmount(calc.getOvertimePay()), false);
        }

        if (isPositive(calc.getBonusGross())) {
            addRow(table, "Prime (brut)", "-", "-", formatAmount(calc.getBonusGross()), false);
        }

        if (isPositive(calc.getBonusRappel())) {
            addRow(table, "Rappel de prime", "-", "-", formatAmount(calc.getBonusRappel()), false);
        }

        if (isPositive(calc.getLeavePay())) {
            addRow(table, "Indemnite de congcongé", "-", "-", formatAmount(calc.getLeavePay()), false);
        }

        addSubtotalRow(table, "Salaire brut", calc.getGrossSalary());

        // CNSS (deduction)
        BigDecimal cnssRatePercent = safeDivideToPercent(calc.getCnssEmployeeAmount(), calc.getCnssBase());
        addDeductionRow(table, "Retenue CNSS", formatAmount(calc.getCnssBase()),
                formatPercent(cnssRatePercent), formatAmount(calc.getCnssEmployeeAmount()));

        addSubtotalRow(table, "Salaire brut imposable", calc.getTaxableIncome());

        // IRPP (deduction)
        if (isPositive(calc.getIrppMonthly())) {
            addDeductionRow(table, "Retenue a la source (IRPP)",
                    formatAmount(calc.getTaxableIncome()), "-", formatAmount(calc.getIrppMonthly()));
        }

        if (isPositive(calc.getAdvanceDeduction())) {
            addDeductionRow(table, "Avance sur salaire", "-", "-",
                    formatAmount(calc.getAdvanceDeduction()));
        }

        addSubtotalRow(table, "Salaire Net", calc.getNetSalary());

        document.add(table);

        Paragraph employerCost = new Paragraph(
                "Cout total employeur: " + formatAmount(calc.getTotalEmployerCost()) + CURRENCY_SUFFIX,
                FONT_SUBTITLE
        );
        employerCost.setSpacingBefore(10);
        document.add(employerCost);
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addRow(PdfPTable table, String label, String base, String taux, String amount, boolean bold) {
        Font font = bold ? FONT_ROW_BOLD : FONT_ROW;
        addCell(table, label, font, Element.ALIGN_LEFT, null);
        addCell(table, base, font, Element.ALIGN_RIGHT, null);
        addCell(table, taux, font, Element.ALIGN_RIGHT, null);
        addCell(table, amount, font, Element.ALIGN_RIGHT, null);
    }

    private void addDeductionRow(PdfPTable table, String label, String base, String taux, String retenue) {
        // Retenues print in the same right-hand column as gains, per the
        // reference layout (single "Gains / Retenues" column, sign implied
        // by row context rather than two separate amount columns).
        addCell(table, label, FONT_ROW, Element.ALIGN_LEFT, null);
        addCell(table, base, FONT_ROW, Element.ALIGN_RIGHT, null);
        addCell(table, taux, FONT_ROW, Element.ALIGN_RIGHT, null);
        addCell(table, "-" + retenue, FONT_ROW, Element.ALIGN_RIGHT, null);
    }

    private void addSubtotalRow(PdfPTable table, String label, BigDecimal amount) {
        addCell(table, label, FONT_ROW_BOLD, Element.ALIGN_LEFT, SUBTOTAL_BG);
        addCell(table, "-", FONT_ROW_BOLD, Element.ALIGN_RIGHT, SUBTOTAL_BG);
        addCell(table, "-", FONT_ROW_BOLD, Element.ALIGN_RIGHT, SUBTOTAL_BG);
        addCell(table, formatAmount(amount), FONT_ROW_BOLD, Element.ALIGN_RIGHT, SUBTOTAL_BG);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(alignment);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        table.addCell(cell);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) return "-";
        return value.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDays(BigDecimal value) {
        if (value == null) return "-";
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "-";
        return value.setScale(2, java.math.RoundingMode.HALF_UP) + " %";
    }

    private BigDecimal safeDivideToPercent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator
                .divide(denominator, 6, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private String nullToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}