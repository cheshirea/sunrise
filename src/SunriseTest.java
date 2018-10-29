import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SunriseTest {

    //можно сделать property file или загружать из аргументов main
    private static final String sourceFileName = "example.csv";
    private static final String destFileName = "output.csv";
    private static final String delimiter = ";";

    // (([+-]?\d+(.\d+)?)|([+-]?\\.\\d+?)) - если будут числа вида "-.123"
    // все числа в 10-чной системе?
    private static Predicate<String> isNumberPredicate = Pattern.compile("^[+-]?\\d+([,.]\\d+)?$").asPredicate();
    //    private static Function<String, Boolean> isNum = isNumberPredicate::test; //shortcut
    private static Pattern cellLinkPattern = Pattern.compile("(([A-Z]+)(\\d+))");

    private static List<Double[]> result;
    private static List<String[]> sourceData;


    public static void main(String[] args) throws IOException {
        Path sourceFile = Paths.get(sourceFileName);
        // предположим что исходные файлы не такие большие
        List<String> inputData = Files.readAllLines(sourceFile);

        result = new ArrayList<>(inputData.size());
        sourceData = new ArrayList<>(inputData.size());
        inputData
                .stream()
                .filter(line -> line != null && !line.isEmpty())
                .map(line -> line.split(delimiter))
                .forEachOrdered(cells -> {
                    result.add(new Double[cells.length]);
                    sourceData.add(cells);
                });

//        if (inputData.isEmpty()){
//            throw new InvalidObjectException("Source data is empty.");
//        }
//        IntSummaryStatistics intSummaryStatistics = inputData
//                .stream()
//                .mapToInt(cells -> cells.length)
//                .summaryStatistics();
//        if (intSummaryStatistics.getMin() != intSummaryStatistics.getMax())
//            throw new InvalidObjectException("Lines has different length.");

        for (int rowIndex = 0; rowIndex < result.size(); rowIndex++) {
            for (int colIndex = 0; colIndex < result.get(rowIndex).length; colIndex++) {
                if (result.get(rowIndex)[colIndex] == null) {
                    result.get(rowIndex)[colIndex] = resolveCell(rowIndex, colIndex, new LinkedList<>());
                }
            }
        }

//        if (logger.isDebugEnabled) {
//            System.out.printf("%n%n%n");
//            sourceData
//                    .stream()
//                    .map(Arrays::asList)
//                    .forEach(row -> {
//                        row.forEach(cell -> {
//                            System.out.printf("%s%-15s", isNumberPredicate.test(cell) ? "(n)" : "(f)", cell); // number/formula
//                        });
//                        System.out.println();
//                    });
//            System.out.printf("%n%n%n");
//            result
//                    .stream()
//                    .map(Arrays::asList)
//                    .forEach(row -> {
//                        row.forEach(cell -> System.out.printf("%-15f", cell));
//                        System.out.println();
//                    });
//        }

        Path output = Paths.get(destFileName);
        DecimalFormat df = new DecimalFormat();
        df.setDecimalSeparatorAlwaysShown(false);
        List<String> toWrite = result
                .stream()
                .map(row -> Stream.of(row).map(df::format).collect(Collectors.toList()))
                .map(row -> String.join(delimiter, row))
                .collect(Collectors.toList());

        Files.write(output, toWrite, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    private static double resolveCell(int rowIndex, int colIndex, List<String> dependentCellsStack) {
        String currentAddress = rowIndexNumberToString(rowIndex) + (colIndex + 1);

        dependentCellsStack.add(currentAddress);
        int indexOfCurrentAddress = dependentCellsStack.indexOf(currentAddress);

        if (indexOfCurrentAddress != dependentCellsStack.size() - 1) {
            IntStream.range(0, indexOfCurrentAddress).forEach(dependentCellsStack::remove); // необязательно, просто стек в e.message понятнее
            throw new RuntimeException("Can't pass through data, recursion in " + String.join(" -> ", dependentCellsStack));
        }
        if (sourceData.size() <= rowIndex || sourceData.get(rowIndex).length <= colIndex)
            throw new RuntimeException("Invalid link: " + currentAddress);

        System.out.printf("%s: ", String.join(" -> ", dependentCellsStack));
        String cellWithSourceData = sourceData.get(rowIndex)[colIndex];
        if (result.get(rowIndex)[colIndex] != null) {
            System.out.println(" - already resolved");
            dependentCellsStack.remove(currentAddress);
            return result.get(rowIndex)[colIndex];
        }
        if (isNumberPredicate.test(cellWithSourceData)) {
            dependentCellsStack.remove(currentAddress);
            System.out.println(" - resolved");
            return Double.parseDouble(cellWithSourceData);
        }

        String preparedFormulaString = cellWithSourceData;


//        если будут десятичные с разделителем в виде ","то надо заменить, и лучше в начале программы
//        cellWithSourceData = cellWithSourceData.replaceAll(",", ".");

        Matcher matcher = cellLinkPattern.matcher(cellWithSourceData);

        System.out.printf(" - parsing %s(%s):%n", currentAddress, cellWithSourceData);
        while (matcher.find()) {
            int newRowIndex = rowIndexStringToNumber(matcher.group(2));
            int newColIndex = Integer.parseInt(matcher.group(3)) - 1;

            double resolvedCell = resolveCell(newRowIndex, newColIndex, dependentCellsStack);
            result.get(newRowIndex)[newColIndex] = resolvedCell;

            preparedFormulaString = preparedFormulaString.replaceAll(matcher.group(1), String.valueOf(resolvedCell));
        }

        dependentCellsStack.remove(currentAddress);
        return evaluateFormula(preparedFormulaString);
    }

    private static double evaluateFormula(String expression) {
        // подсмотрел готовое решение
        javax.script.ScriptEngineManager sem = new ScriptEngineManager();
        javax.script.ScriptEngine e = sem.getEngineByName("JavaScript");
        try {
            return (double) e.eval(expression);
        } catch (ScriptException e1) {
            throw new RuntimeException("Can't evaluate expression: " + expression);
        }
    }

    private static String rowIndexNumberToString(int rowIndex) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(alphabet.charAt(rowIndex % 26));
            rowIndex = rowIndex / 26 - 1;
        } while (rowIndex >= 0);
        return sb.reverse().toString();
    }

    private static int rowIndexStringToNumber(String rowIndex) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int result = 0;
        for (int i = 0; i < rowIndex.length(); i++) {
            result *= 26;
            result += alphabet.indexOf(rowIndex.charAt(i)) + 1;
        }
        return result - 1;
    }


    //TODO tests
}
