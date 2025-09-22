package org.openjfx.gradefx.plugin.bsg;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.openjfx.gradefx.model.Group;
import org.openjfx.gradefx.model.Test;
import org.openjfx.gradefx.view.menu.TestMenu;
import org.openjfx.gradefx.view.menu.TestMenu.TestMenuExtensionPoint;
import org.openjfx.gradefx.view.pane.GroupsPane;
import org.openjfx.kafx.controller.ConfigController;
import org.openjfx.kafx.controller.ExceptionController;
import org.openjfx.kafx.controller.TranslationController;
import org.openjfx.kafx.view.converter.BigDecimalConverter;
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
			PDDocument document;
			PDAcroForm acroForm;
			PDField[] fields;
			switch (group.getGradeSystem()) {
			case ONE_TO_SIX:
				document = Loader.loadPDF(new RandomAccessReadBuffer(TestMenuExtensionBSG.class
						.getResourceAsStream("/org/openjfx/gradefx/plugin/bsg/pdf/BSG-Umschlag.pdf")));
				acroForm = document.getDocumentCatalog().getAcroForm();
				fields = acroForm.getFields().toArray(n -> new PDField[n]);
				this.setValuesOneToSix(fields, group, test);
				break;
			case FIFTEEN_POINTS:
				document = Loader.loadPDF(new RandomAccessReadBuffer(TestMenuExtensionBSG.class
						.getResourceAsStream("/org/openjfx/gradefx/plugin/bsg/pdf/BSG-Umschlag-Oberstufe.pdf")));
				acroForm = document.getDocumentCatalog().getAcroForm();
				fields = acroForm.getFields().toArray(n -> new PDField[n]);
				this.setValuesFifteenPoints(fields, group, test);
				break;
			default:
				throw new IllegalArgumentException("unknown grade system: " + group.getGradeSystem());
			}
			document.save(output);
			document.close();
		} catch (Exception e) {
			ExceptionController.exception(e);
		}
	}

	private void setValuesOneToSix(PDField[] fields, Group group, Test test) throws IOException {
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
		fields[5].setValue(String.valueOf(group.getSubject().getName()));
		fields[10].setValue(new LocalDateStringConverter().toString(test.getDate()));
		int amount = (int) test.getGrades().values().stream().filter(g -> g.get() != null).count();
		int grade1 = (int) test.getGrades().values().stream()
				.filter(g -> g.get() != null && g.get().getNumericalValue() == 1).count();
		int grade2 = (int) test.getGrades().values().stream()
				.filter(g -> g.get() != null && g.get().getNumericalValue() == 2).count();
		int grade3 = (int) test.getGrades().values().stream()
				.filter(g -> g.get() != null && g.get().getNumericalValue() == 3).count();
		int grade4 = (int) test.getGrades().values().stream()
				.filter(g -> g.get() != null && g.get().getNumericalValue() == 4).count();
		int grade5 = (int) test.getGrades().values().stream()
				.filter(g -> g.get() != null && g.get().getNumericalValue() == 5).count();
		int grade6 = (int) test.getGrades().values().stream()
				.filter(g -> g.get() != null && g.get().getNumericalValue() == 6).count();
		fields[15].setValue(String.valueOf(amount));
		fields[16].setValue(String.valueOf(grade1));
		fields[17].setValue(String.valueOf(grade2));
		fields[18].setValue(String.valueOf(grade3));
		fields[19].setValue(String.valueOf(grade4));
		fields[20].setValue(String.valueOf(grade5));
		fields[21].setValue(String.valueOf(grade6));
		BigDecimal amountBigDecimal = BigDecimal.valueOf(amount);
		BigDecimal sumBigDecimal = BigDecimal
				.valueOf(grade1 * 1 + grade2 * 2 + grade3 * 3 + grade4 * 4 + grade5 * 5 + grade6 * 6);
		BigDecimalConverter converter = new BigDecimalConverter();
		converter.getDecimalFormat().setMinimumFractionDigits(2);
		converter.getDecimalFormat().setMaximumFractionDigits(2);
		converter.getDecimalFormat().setRoundingMode(RoundingMode.DOWN);
		fields[22].setValue(converter.toString(sumBigDecimal.divide(amountBigDecimal, 7, RoundingMode.DOWN)));
		converter.getDecimalFormat().setRoundingMode(RoundingMode.HALF_UP);
		fields[23].setValue(converter.toString(
				BigDecimal.valueOf(100 * (grade5 + grade6)).divide(amountBigDecimal, 7, RoundingMode.HALF_UP)));
	}

	private void setValuesFifteenPoints(PDField[] fields, Group group, Test test) throws IOException {
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
		int year = test.getDate().getYear() - 2000;
		if (test.getDate().getMonth().compareTo(Month.AUGUST) < 0) {
			fields[0].setValue(String.valueOf(year - 1));
			fields[1].setValue(String.valueOf(year));
		} else {
			fields[0].setValue(String.valueOf(year));
			fields[1].setValue(String.valueOf(year + 1));
		}
		fields[2].setValue(group.getName().split(" ")[0]);
		fields[3].setValue(new LocalDateStringConverter().toString(test.getDate()));
		fields[11].setValue(String.valueOf(group.getSubject().getName()));
		fields[12].setValue(String.valueOf(group.getStudents().size()));
		int amount = (int) test.getGrades().values().stream().filter(g -> g.get() != null).count();
		BigDecimal amountBigDecimal = BigDecimal.valueOf(amount);
		fields[13].setValue(String.valueOf(amount));
		fields[30].setValue(String.valueOf(amount));
		int[] grades = new int[16];
		BigDecimal sumBigDecimal = BigDecimal.ZERO;
		for (int i = 0; i < 16; i++) {
			final int n = i;
			grades[i] = (int) test.getGrades().values().stream()
					.filter(g -> g.get() != null && g.get().getNumericalValue() == n).count();
			fields[14 + 15 - i].setValue(String.valueOf(grades[i]));
			sumBigDecimal = sumBigDecimal.add(BigDecimal.valueOf(grades[i] * i));
		}
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
