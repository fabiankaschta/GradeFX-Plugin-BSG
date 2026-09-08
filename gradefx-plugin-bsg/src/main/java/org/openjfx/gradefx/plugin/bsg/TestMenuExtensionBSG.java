package org.openjfx.gradefx.plugin.bsg;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationStrikeout;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.openjfx.gradefx.model.Grade;
import org.openjfx.gradefx.model.Group;
import org.openjfx.gradefx.model.Student;
import org.openjfx.gradefx.model.Test;
import org.openjfx.gradefx.view.menu.TestMenu;
import org.openjfx.gradefx.view.menu.TestMenu.TestMenuExtensionPoint;
import org.openjfx.gradefx.view.pane.GroupsPane;
import org.openjfx.kafx.controller.ConfigController;
import org.openjfx.kafx.controller.ExceptionController;
import org.openjfx.kafx.controller.LogController;
import org.openjfx.kafx.controller.TranslationController;
import org.openjfx.kafx.converter.BigDecimalConverter;
import org.pf4j.Extension;

import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.converter.LocalDateStringConverter;

@Extension
public class TestMenuExtensionBSG implements TestMenuExtensionPoint {

	@Override
	public void addMenuItem(TestMenu testMenu) {
		testMenu.getItems().add(new SeparatorMenuItem());

		MenuItem menuItemExportBSG = new MenuItem(TranslationController.translate("menu_test_exportBSG"));
		menuItemExportBSG.setOnAction(_ -> {
			FileChooser fileChooser = new FileChooser();
			if (ConfigController.exists("LAST_FILE")) {
				fileChooser.setInitialDirectory(new File(ConfigController.get("LAST_FILE")).getParentFile());
			} else {
				fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			}
			fileChooser.getExtensionFilters().add(new ExtensionFilter("PDF", "*.pdf"));
			File file = fileChooser.showSaveDialog(testMenu.getParentPopup());
			if (file != null) {
				export(file, GroupsPane.getSelectedGroup(), GroupsPane.getSelectedTest());
			}
		});

		testMenu.getItems().add(menuItemExportBSG);
	}

	private void export(File output, Group group, Test test) {
		try {
			LogController.log(LogController.DEBUG, "creating pdf wrapper");
			PDDocument document;
			if (group.getGradeSystem().getPossibleGradesASC().length == 6) {
				LogController.log(LogController.DEBUG, "loading BSG-Umschlag.pdf");
				document = Loader.loadPDF(new RandomAccessReadBuffer(TestMenuExtensionBSG.class
						.getResourceAsStream("/org/openjfx/gradefx/plugin/bsg/pdf/BSG-Umschlag.pdf")));

				LogController.log(LogController.DEBUG, "pdf loaded (BSG-Umschlag.pdf)");
				this.setValuesOneToSix(document, group, test);
				LogController.log(LogController.DEBUG, "pdf filled (BSG-Umschlag.pdf");
			} else if (group.getGradeSystem().getPossibleGradesASC().length == 16) {
				LogController.log(LogController.DEBUG, "loading BSG-Umschlag-Oberstufe.pdf");
				document = Loader.loadPDF(new RandomAccessReadBuffer(TestMenuExtensionBSG.class
						.getResourceAsStream("/org/openjfx/gradefx/plugin/bsg/pdf/BSG-Umschlag-Oberstufe.pdf")));
				LogController.log(LogController.DEBUG, "pdf loaded (BSG-Umschlag-Oberstufe.pdf)");
				this.setValuesFifteenPoints(document, group, test);
				LogController.log(LogController.DEBUG, "pdf filled (BSG-Umschlag-Oberstufe.pdf");
			} else {
				throw new IllegalArgumentException("unknown grade system: " + group.getGradeSystem());
			}
			document.save(output);
			document.close();
			LogController.log(LogController.DEBUG, "pdf exported");
		} catch (Throwable t) {
			ExceptionController.exception(t);
		}
	}

	private PDAnnotationStrikeout getStrikeOut(TextPosition begin, TextPosition end) {
		PDAnnotationStrikeout strikeOut = new PDAnnotationStrikeout();
		strikeOut.setColor(new PDColor(new float[] { 0, 0, 0 }, PDDeviceRGB.INSTANCE)); // black
		PDBorderStyleDictionary borderStyle = new PDBorderStyleDictionary();
		borderStyle.setWidth(2f);
		strikeOut.setBorderStyle(borderStyle);

		float posXBegin = begin.getXDirAdj();
		float posXEnd = end.getXDirAdj() + end.getWidth();
		float posYBegin = begin.getPageHeight() - begin.getYDirAdj();
		float posYEnd = begin.getPageHeight() - end.getYDirAdj();
		float height = begin.getHeightDir();

		PDRectangle position = new PDRectangle();
		position.setLowerLeftX(posXBegin);
		position.setLowerLeftY(posYBegin);
		position.setUpperRightX(posXEnd);
		position.setUpperRightY(posYEnd + height);
		strikeOut.setRectangle(position);

		float quadPoints[] = { posXBegin, posYEnd + height + 2, posXEnd, posYEnd + height + 2, posXBegin, posYBegin,
				posXEnd, posYEnd };
		strikeOut.setQuadPoints(quadPoints);

		return strikeOut;
	}

	private void setValuesOneToSix(PDDocument document, Group group, Test test) throws IOException {
		// 00 Schuljahr Start 20xx
		// 01 Schuljahr Ende 20xx
		// 02 Klasse
		// 03 Teilnehmerzahl
		// 04 NR
		// 05 Fach
		// 06 Anmerkungen
		// 07 Nachschrift (Checkbox)
		// 08 Fachlehrkraft
		// 09 Arbeitszeit
		// 10 gehalten am
		// 11 zurückgegeben am
		// 12 Lehrstoff
		// 13 ASV Eintrag am
		// 14 Vorlage FSL am
		// 15 abgegebene Arbeiten
		// 16 Anzahl Note 1
		// 17 Anzahl Note 2
		// 18 Anzahl Note 3
		// 19 Anzahl Note 4
		// 20 Anzahl Note 5
		// 21 Anzahl Note 6
		// 22 Durchschnitt Note
		// 23 Anteil Note 5+6

		PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
		PDField[] fields = acroForm.getFields().toArray(n -> new PDField[n]);

		LocalDateStringConverter dateConverter = new LocalDateStringConverter();

		int year = test.getDate().getYear() - 2000;
		if (test.getDate().getMonth().compareTo(Month.AUGUST) < 0) {
			fields[0].setValue(String.valueOf(year - 1));
			fields[1].setValue(String.valueOf(year));
		} else {
			fields[0].setValue(String.valueOf(year));
			fields[1].setValue(String.valueOf(year + 1));
		}
		fields[2].setValue(group.getName().split(" ")[0]);
		fields[3].setValue(String.valueOf(group.getStudents().size()));
		fields[4].setValue(test.getName().replaceAll("\\D+", ""));
		fields[5].setValue(String.valueOf(group.getSubject().getName()));

		int amount = 0;
		int[] grades = new int[6];
		StringBuilder annotationBuilder = new StringBuilder();
		for (Student student : group.getStudents()) {
			String annotation = test.getAnnotation(student);
			LocalDate date = test.getDate(student);
			if (annotation != null && annotation.length() > 0 || date != null && !date.equals(test.getDate())) {
				annotationBuilder.append(student.getLastName());
				annotationBuilder.append(',');
				annotationBuilder.append(' ');
				annotationBuilder.append(student.getFirstName());
				annotationBuilder.append(':');
				annotationBuilder.append(' ');
				if (annotation != null) {
					annotationBuilder.append(annotation);
					annotationBuilder.append(' ');
				}
				if (date != null && !date.equals(test.getDate())) {
					annotationBuilder.append('(');
					annotationBuilder.append(dateConverter.toString(test.getDate()));
					annotationBuilder.append(')');
				}
				annotationBuilder.append('\n');
			}
			if (!test.getOnlyDefaultDate() || date == null || date.equals(test.getDate())) {
				Grade grade = test.getGrade(student);
				if (grade != null) {
					amount++;
					grades[grade.getNumericalValue() - 1]++;
				}
			}
		}
		fields[6].setValue(annotationBuilder.toString());

		if (ConfigController.exists("TEACHER_NAME")) {
			fields[8].setValue(ConfigController.get("TEACHER_NAME"));
		}

		fields[10].setValue(dateConverter.toString(test.getDate()));

		fields[15].setValue(String.valueOf(amount));
		fields[16].setValue(String.valueOf(grades[0]));
		fields[17].setValue(String.valueOf(grades[1]));
		fields[18].setValue(String.valueOf(grades[2]));
		fields[19].setValue(String.valueOf(grades[3]));
		fields[20].setValue(String.valueOf(grades[4]));
		fields[21].setValue(String.valueOf(grades[5]));
		BigDecimal amountBigDecimal = BigDecimal.valueOf(amount);
		BigDecimal sumBigDecimal = BigDecimal
				.valueOf(grades[0] * 1 + grades[1] * 2 + grades[2] * 3 + grades[3] * 4 + grades[4] * 5 + grades[5] * 6);
		BigDecimalConverter converter = new BigDecimalConverter();
		converter.getDecimalFormat().setMinimumFractionDigits(2);
		converter.getDecimalFormat().setMaximumFractionDigits(2);
		converter.getDecimalFormat().setRoundingMode(RoundingMode.DOWN);
		fields[22].setValue(converter.toString(sumBigDecimal.divide(amountBigDecimal, 7, RoundingMode.DOWN)));
		converter.getDecimalFormat().setRoundingMode(RoundingMode.HALF_UP);
		fields[23].setValue(converter.toString(
				BigDecimal.valueOf(100 * (grades[4] + grades[5])).divide(amountBigDecimal, 7, RoundingMode.HALF_UP)));

		final String fullPattern = "SA/KA/Stgr.A/Test/Andere";
		String pattern;
		String testName = test.getName().toLowerCase();
		if (testName.contains("sa") || testName.contains("schulaufgabe") || testName.contains("klausur")) {
			pattern = "SA";
		} else if (testName.contains("ka") || testName.contains("kurzarbeit")) {
			pattern = "KA";
		} else if (testName.contains("stgr.a") || testName.contains("ex") || testName.contains("stegreifaufgabe")
				|| testName.contains("extemporale")) {
			pattern = "Stgr.A";
		} else if (testName.contains("test")) {
			pattern = "Test";
		} else {
			pattern = "Andere";
		}
		PDFTextStripper pdfStripper = new PDFTextStripper() {
			@Override
			protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
				int beginIndex = text.indexOf(fullPattern);
				// correct line is found
				if (beginIndex != -1) {
					List<PDAnnotation> annotationsInPage = this.getCurrentPage().getAnnotations();

					int endIndex = text.indexOf(fullPattern) + fullPattern.length() - 1;
					int patternBeginIndex = text.indexOf(pattern);
					int patternEndIndex = patternBeginIndex + pattern.length() - 1;

					if (pattern.equals("SA")) { // begin
						annotationsInPage
								.add(getStrikeOut(textPositions.get(patternEndIndex + 1), textPositions.get(endIndex)));
					} else if (pattern.equals("Andere")) { // end
						annotationsInPage.add(
								getStrikeOut(textPositions.get(beginIndex), textPositions.get(patternBeginIndex - 1)));
					} else { // mid
						annotationsInPage.add(
								getStrikeOut(textPositions.get(beginIndex), textPositions.get(patternBeginIndex - 1)));
						annotationsInPage
								.add(getStrikeOut(textPositions.get(patternEndIndex + 1), textPositions.get(endIndex)));
					}
				}
				super.writeString(text, textPositions);
			}
		};
		Writer writer = new OutputStreamWriter(new ByteArrayOutputStream());
		pdfStripper.writeText(document, writer);
	}

	private void setValuesFifteenPoints(PDDocument document, Group group, Test test) throws IOException {
		// 00 Schuljahr Start 20xx
		// 01 Schuljahr Ende 20xx
		// 02 Kurs
		// 03 gehalten am
		// 04 Arbeitszeit
		// 05 zurückgegeben am
		// 06 Vorlage FSL am
		// 07 ASV Eintrag am
		// 08 Lehrstoff Zeile 1
		// 09 Lehrstoff Zeile 2
		// 10 Vorlage SL am
		// 11 Fach
		// 12 Teilnehmerzahl
		// 13 abgegebene Arbeiten
		// 14 Anzahl 15 P.
		// 15 Anzahl 14 P.
		// 16 Anzahl 13 P.
		// 17 Anzahl 12 P.
		// 18 Anzahl 11 P.
		// 19 Anzahl 10 P.
		// 20 Anzahl 09 P.
		// 21 Anzahl 08 P.
		// 22 Anzahl 07 P.
		// 23 Anzahl 06 P.
		// 24 Anzahl 05 P.
		// 25 Anzahl 04 P.
		// 26 Anzahl 03 P.
		// 27 Anzahl 02 P.
		// 28 Anzahl 01 P.
		// 29 Anzahl 00 P.
		// 30 anwesend
		// 31 Durchschnitt Note
		// 32 Durchschnitt Punkte
		// 33 Anteil Note 5+6
		// 34 12/1 (checkbox)
		// 35 12/2 (checkbox)
		// 36 13/1 (checkbox)
		// 37 13/2 (checkbox)
		// 38 Nachschrift (checkbox)

		PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
		PDField[] fields = acroForm.getFields().toArray(n -> new PDField[n]);

		LocalDateStringConverter dateConverter = new LocalDateStringConverter();

		int year = test.getDate().getYear() - 2000;
		if (test.getDate().getMonth().compareTo(Month.AUGUST) < 0) {
			fields[0].setValue(String.valueOf(year - 1));
			fields[1].setValue(String.valueOf(year));
		} else {
			fields[0].setValue(String.valueOf(year));
			fields[1].setValue(String.valueOf(year + 1));
		}
		fields[2].setValue(group.getName().split(" ")[0]);
		fields[3].setValue(dateConverter.toString(test.getDate()));
		fields[11].setValue(String.valueOf(group.getSubject().getName()));
		fields[12].setValue(String.valueOf(group.getStudents().size()));
		
		int amount = 0;
		int[] grades = new int[16];
		BigDecimal sumBigDecimal = BigDecimal.ZERO;
		for (Student student : group.getStudents()) {
			LocalDate date = test.getDate(student);
			if (!test.getOnlyDefaultDate() || date == null || date.equals(test.getDate())) {
				Grade grade = test.getGrade(student);
				if (grade != null) {
					amount++;
					grades[grade.getNumericalValue()]++;
				}
			}
		}
		for (int i = 0; i < 16; i++) {
			fields[14 + 15 - i].setValue(String.valueOf(grades[i]));
			sumBigDecimal = sumBigDecimal.add(BigDecimal.valueOf(grades[i] * i));
		}
		fields[13].setValue(String.valueOf(amount));
		fields[30].setValue(String.valueOf(amount));
		BigDecimal amountBigDecimal = BigDecimal.valueOf(amount);
		
		BigDecimalConverter converter = new BigDecimalConverter();
		converter.getDecimalFormat().setMinimumFractionDigits(2);
		converter.getDecimalFormat().setMaximumFractionDigits(2);
		converter.getDecimalFormat().setRoundingMode(RoundingMode.DOWN);
		BigDecimal avgPoints = sumBigDecimal.divide(amountBigDecimal, 7, RoundingMode.DOWN);
		BigDecimal avgGrade = BigDecimal.valueOf(17).subtract(avgPoints).divide(BigDecimal.valueOf(3), 7,
				RoundingMode.DOWN);
		fields[31].setValue(converter.toString(avgGrade));
		fields[32].setValue(converter.toString(avgPoints));
		converter.getDecimalFormat().setRoundingMode(RoundingMode.HALF_UP);
		// six spaces needed since field is too large
		fields[33].setValue(converter.toString(BigDecimal.valueOf(100 * (grades[0] + grades[1] + grades[2] + grades[3]))
				.divide(amountBigDecimal, 7, RoundingMode.HALF_UP)) + "      ");

	}

}
