package org.dromara.crehn.review.sheet;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.crehn.domain.vo.ReviewScoreSheetColumnVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetFooterFieldVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetFooterRowVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetTemplateVo;
import org.dromara.crehn.domain.vo.ReviewTaskVo;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class ReviewScoreSheetExcelWriter {

    public void write(List<ReviewTaskVo> tasks, ReviewScoreSheetTemplateVo template, String reviewerName,
                       String categoryName, String exportTime, java.io.OutputStream outputStream) throws IOException {
        write(tasks, template, reviewerName, categoryName, exportTime, null, outputStream);
    }

    /**
     * Generates the exact saved-sheet snapshot. A signature is anchored from
     * slot-relative coordinates, so it does not depend on the browser layout.
     */
    public void write(List<ReviewTaskVo> tasks, ReviewScoreSheetTemplateVo template, String reviewerName,
                      String categoryName, String exportTime, ReviewScoreSheetSignatureImage signatureImage,
                      java.io.OutputStream outputStream) throws IOException {
        List<ReviewScoreSheetColumnVo> columns = template.getColumns().stream()
            .filter(column -> Boolean.TRUE.equals(column.getVisible()))
            .toList();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(
                StringUtils.blankToDefault(categoryName, "评审打分表")));
            CellStyle titleStyle = titleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle dataStyle = dataStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle leftDataStyle = dataStyle(workbook, HorizontalAlignment.LEFT);
            CellStyle leftFooterStyle = footerStyle(workbook, HorizontalAlignment.LEFT);
            CellStyle rightFooterStyle = footerStyle(workbook, HorizontalAlignment.RIGHT);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            for (int index = 0; index < columns.size(); index++) {
                Cell cell = titleRow.createCell(index);
                cell.setCellStyle(titleStyle);
            }
            titleRow.getCell(0).setCellValue(template.getTitle());
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columns.size() - 1));

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(25);
            for (int index = 0; index < columns.size(); index++) {
                ReviewScoreSheetColumnVo column = columns.get(index);
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(column.getLabel());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, columnWidth(column.getKey()));
            }

            int rowIndex = 2;
            for (ReviewTaskVo task : tasks) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(23);
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    ReviewScoreSheetColumnVo column = columns.get(columnIndex);
                    Cell cell = row.createCell(columnIndex);
                    cell.setCellValue(columnValue(column.getKey(), task, reviewerName));
                    cell.setCellStyle("projectName".equals(column.getKey()) ? leftDataStyle : dataStyle);
                }
            }

            int footerRowIndex = rowIndex + 1;
            int leftEnd = Math.max(0, (columns.size() + 1) / 2 - 1);
            int rightStart = leftEnd + 1;
            int lastRowIndex = Math.max(1, rowIndex - 1);
            List<ReviewScoreSheetFooterRowVo> footerRows = template.getFooterRows() == null ? List.of() : template.getFooterRows();
            FooterSignatureSlot signatureSlot = null;
            for (ReviewScoreSheetFooterRowVo footerConfig : footerRows) {
                Row footerRow = sheet.createRow(footerRowIndex);
                footerRow.setHeightInPoints(28);
                for (int index = 0; index < columns.size(); index++) {
                    footerRow.createCell(index).setCellStyle(index < rightStart ? leftFooterStyle : rightFooterStyle);
                }
                footerRow.getCell(0).setCellValue(footerText(footerConfig.getLeft(), exportTime, signatureImage));
                mergeIfNeeded(sheet, footerRowIndex, 0, leftEnd);
                FooterSignatureSlot currentSignatureSlot = null;
                if (matchesSignatureSlot(footerConfig.getLeft(), signatureImage)) {
                    currentSignatureSlot = new FooterSignatureSlot(footerRowIndex, 0, leftEnd, true, footerConfig.getLeft());
                }
                if (rightStart < columns.size()) {
                    footerRow.getCell(rightStart).setCellValue(footerText(footerConfig.getRight(), exportTime, signatureImage));
                    mergeIfNeeded(sheet, footerRowIndex, rightStart, columns.size() - 1);
                    if (matchesSignatureSlot(footerConfig.getRight(), signatureImage)) {
                        currentSignatureSlot = new FooterSignatureSlot(footerRowIndex, rightStart, columns.size() - 1, false,
                            footerConfig.getRight());
                    }
                }
                lastRowIndex = footerRowIndex++;
                if (currentSignatureSlot != null) {
                    signatureSlot = currentSignatureSlot;
                    // The browser signature slot is deliberately taller than a
                    // normal footer line; reserve comparable vertical space in
                    // Excel so the PNG remains legible and draggable placement
                    // does not cover the field label.
                    footerRow.setHeightInPoints(54);
                    if (signatureImage.timeVisible() && StringUtils.isNotBlank(signatureImage.signedTime())) {
                        lastRowIndex = writeSignatureTimeRow(sheet, footerRowIndex++, columns.size(), rightStart,
                            currentSignatureSlot, signatureImage.signedTime(), currentSignatureSlot.field().getSignatureTimeText(),
                            leftFooterStyle, rightFooterStyle);
                    }
                }
            }
            if (signatureImage != null && signatureSlot != null) {
                insertSignatureImage(workbook, sheet, signatureImage, signatureSlot);
            }

            sheet.createFreezePane(0, 2);
            sheet.setAutobreaks(true);
            sheet.setFitToPage(true);
            sheet.setHorizontallyCenter(true);
            sheet.setMargin(Sheet.LeftMargin, 0.35);
            sheet.setMargin(Sheet.RightMargin, 0.35);
            sheet.setMargin(Sheet.TopMargin, 0.5);
            sheet.setMargin(Sheet.BottomMargin, 0.5);
            PrintSetup printSetup = sheet.getPrintSetup();
            printSetup.setLandscape(true);
            printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
            printSetup.setFitWidth((short) 1);
            printSetup.setFitHeight((short) 0);
            workbook.setPrintArea(0, 0, columns.size() - 1, 0, lastRowIndex);
            workbook.write(outputStream);
        }
    }

    private String footerText(ReviewScoreSheetFooterFieldVo field, String exportTime, ReviewScoreSheetSignatureImage signatureImage) {
        if (field == null || StringUtils.isBlank(field.getLabel())) {
            return "";
        }
        if ("signature".equals(field.getType()) && !shouldRenderSignatureField(field, signatureImage)) {
            return "";
        }
        if ("exportTime".equals(field.getType())) {
            return field.getLabel() + StringUtils.blankToDefault(exportTime, "-");
        }
        int lineLength = switch (StringUtils.blankToDefault(field.getLineLength(), "none")) {
            case "short" -> 8;
            case "medium" -> 14;
            case "long" -> 22;
            default -> 0;
        };
        return lineLength == 0 ? field.getLabel() : field.getLabel() + " " + "_".repeat(lineLength);
    }

    private boolean matchesSignatureSlot(ReviewScoreSheetFooterFieldVo field, ReviewScoreSheetSignatureImage signatureImage) {
        return field != null
            && signatureImage != null
            && "signature".equals(field.getType())
            && signatureImage.slotKey() != null
            && signatureImage.slotKey().equals(field.getSlotKey());
    }

    /** Optional signature slots stay completely absent from the immutable XLSX when the submission is unsigned. */
    private boolean shouldRenderSignatureField(ReviewScoreSheetFooterFieldVo field, ReviewScoreSheetSignatureImage signatureImage) {
        return matchesSignatureSlot(field, signatureImage) || !Boolean.FALSE.equals(field.getSignatureRequired());
    }

    /**
     * A signed snapshot always exposes its server time, even when a custom
     * template omitted the ordinary export-time footer field.
     */
    private int writeSignatureTimeRow(Sheet sheet, int rowIndex, int columnCount, int rightStart,
                                      FooterSignatureSlot slot, String signedTime, String signatureTimeText, CellStyle leftStyle,
                                      CellStyle rightStyle) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(18);
        for (int column = 0; column < columnCount; column++) {
            row.createCell(column).setCellStyle(column < rightStart ? leftStyle : rightStyle);
        }
        row.getCell(slot.firstColumn()).setCellValue((signatureTimeText == null ? "签名时间：" : signatureTimeText) + signedTime);
        mergeIfNeeded(sheet, rowIndex, slot.firstColumn(), slot.lastColumn());
        return rowIndex;
    }

    private void insertSignatureImage(Workbook workbook, Sheet sheet, ReviewScoreSheetSignatureImage signatureImage,
                                      FooterSignatureSlot slot) {
        byte[] image = signatureImage.pngBytes();
        if (image == null || image.length == 0 || signatureImage.placement() == null) {
            return;
        }
        int pictureIndex = workbook.addPicture(image, Workbook.PICTURE_TYPE_PNG);
        double totalWidth = 0;
        for (int column = slot.firstColumn(); column <= slot.lastColumn(); column++) {
            totalWidth += columnWidthPixels(sheet, column);
        }
        if (totalWidth <= 0) {
            return;
        }
        double rowHeight = Math.max(1d, sheet.getRow(slot.rowIndex()).getHeightInPoints() * 96d / 72d);
        double x = normalized(signatureImage.placement().getX());
        double y = normalized(signatureImage.placement().getY());
        double width = normalized(signatureImage.placement().getWidth());
        double height = normalized(signatureImage.placement().getHeight());
        double slotWidth = Math.min(totalWidth, signatureSlotWidthPixels(slot.field()));
        double slotOffset = signatureSlotOffsetPixels(totalWidth, slotWidth, slot);
        ColumnAnchor start = resolveColumnAnchor(sheet, slot, slotOffset + slotWidth * x);
        ColumnAnchor end = resolveColumnAnchor(sheet, slot, slotOffset + slotWidth * Math.min(1d, x + width));
        ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        anchor.setCol1(start.column());
        anchor.setDx1(start.offsetEmu());
        anchor.setCol2(end.column());
        anchor.setDx2(end.offsetEmu());
        // Both y values are within the one footer row, so use the same row
        // index instead of allowing the image to spill into the next footer.
        anchor.setRow1(slot.rowIndex());
        anchor.setDy1((int) Math.round(rowHeight * y * Units.EMU_PER_PIXEL));
        anchor.setRow2(slot.rowIndex());
        anchor.setDy2((int) Math.round(rowHeight * Math.min(1d, y + height) * Units.EMU_PER_PIXEL));
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        Picture picture = drawing.createPicture(anchor, pictureIndex);
        picture.getClientAnchor().setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
    }

    private ColumnAnchor resolveColumnAnchor(Sheet sheet, FooterSignatureSlot slot, double offsetPixels) {
        double consumed = 0;
        for (int column = slot.firstColumn(); column <= slot.lastColumn(); column++) {
            double width = columnWidthPixels(sheet, column);
            if (offsetPixels < consumed + width || column == slot.lastColumn()) {
                double local = Math.max(0, Math.min(width, offsetPixels - consumed));
                return new ColumnAnchor(column, (int) Math.round(local * Units.EMU_PER_PIXEL));
            }
            consumed += width;
        }
        return new ColumnAnchor(slot.lastColumn() + 1, 0);
    }

    private double columnWidthPixels(Sheet sheet, int column) {
        // Excel's column width unit is 1/256th of the default character width.
        // The approximation is sufficient because the persisted coordinates are
        // relative to the same footer slot rather than to page pixels.
        return Math.max(1d, sheet.getColumnWidth(column) / 256d * 7d + 5d);
    }

    /** Mirrors the fixed short/medium/long signature slots used by the Vue preview. */
    private double signatureSlotWidthPixels(ReviewScoreSheetFooterFieldVo field) {
        if (field == null) {
            return 180d;
        }
        return switch (StringUtils.blankToDefault(field.getLineLength(), "long")) {
            case "short" -> 72d;
            case "medium" -> 120d;
            default -> 180d;
        };
    }

    private double signatureSlotOffsetPixels(double totalWidth, double slotWidth, FooterSignatureSlot slot) {
        if (!slot.leftSide()) {
            return Math.max(0d, totalWidth - slotWidth);
        }
        String label = slot.field() == null ? "" : StringUtils.blankToDefault(slot.field().getLabel(), "");
        // A Chinese footer character is roughly 11px in the configured font.
        // Cap it at the available area so a narrow custom layout stays valid.
        return Math.min(Math.max(0d, totalWidth - slotWidth), label.length() * 11d + 6d);
    }

    private double normalized(java.math.BigDecimal value) {
        if (value == null) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, value.doubleValue()));
    }

    private String columnValue(String key, ReviewTaskVo task, String reviewerName) {
        return switch (key) {
            case "projectNo" -> StringUtils.blankToDefault(task.getProjectNo(), "-");
            case "projectName" -> StringUtils.blankToDefault(task.getProjectName(), "-");
            case "categoryName" -> StringUtils.blankToDefault(task.getCategoryName(), "-");
            case "scoreMode" -> "grade".equals(task.getScoreMode()) ? "等级制" : "百分制";
            case "status" -> "已提交";
            case "scoreResult" -> scoreResult(task);
            case "scoreTime" -> formatDate(task.getScoreSubmittedAt());
            case "reviewerName" -> StringUtils.blankToDefault(reviewerName, "-");
            default -> "";
        };
    }

    private String scoreResult(ReviewTaskVo task) {
        if ("grade".equals(task.getScoreMode())) {
            return StringUtils.blankToDefault(task.getGradeValue(), "-");
        }
        return task.getScoreValue() == null ? "-" : task.getScoreValue().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private int columnWidth(String key) {
        return switch (key) {
            case "projectNo" -> 20 * 256;
            case "projectName" -> 30 * 256;
            case "categoryName" -> 18 * 256;
            case "scoreTime" -> 22 * 256;
            case "reviewerName" -> 18 * 256;
            default -> 15 * 256;
        };
    }

    private void mergeIfNeeded(Sheet sheet, int rowIndex, int firstColumn, int lastColumn) {
        if (lastColumn > firstColumn) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
        }
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = dataStyle(workbook, HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle dataStyle(Workbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle footerStyle(Workbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setShrinkToFit(true);
        style.setWrapText(false);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        return style;
    }

    private record FooterSignatureSlot(int rowIndex, int firstColumn, int lastColumn, boolean leftSide,
                                       ReviewScoreSheetFooterFieldVo field) {
    }

    private record ColumnAnchor(int column, int offsetEmu) {
    }
}
