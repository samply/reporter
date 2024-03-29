package de.samply.reporter.utils;

import de.samply.reporter.utils.poi.SheetSorter;
import de.samply.reporter.utils.poi.SortOrder;
import java.util.function.BiFunction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class SheetUtils {

  public static void sort(Sheet sheet, SortOrder order, Integer column) {
    SheetSorter sheetSorter = new SheetSorter();
    sheetSorter.addSortKey(column, order);
    sheetSorter.sortSheet(sheet);
  }

  public static void sort(Sheet sheet, SortOrder order, Integer column,
      BiFunction<Row, Integer, Comparable> rowColumnExtractor) {
    SheetSorter sheetSorter = new SheetSorter();
    sheetSorter.addSortKey(column, order, rowColumnExtractor);
    sheetSorter.sortSheet(sheet);
  }


}
