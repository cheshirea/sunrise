package util;

import util.operators.OperatorHandler;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaHandler {
    private static final List<OperatorHandler> ops = new LinkedList<OperatorHandler>() {{
        //создаём обработчики в соответствии с приоритетом оперций
        add(new OperatorHandler(
                "*:/",
                (a, b) -> a * b,
                (a, b) -> a / b,
                (a, b) -> a / b)
        );
        add(new OperatorHandler(
                "+-",
                (a, b) -> a + b,
                (a, b) -> a - b)
        );
    }};

    private static Pattern bracketExpressionPattern = Pattern.compile("\\((?<inbrace>[^(]+)\\)");

    public static double evaluateFormula(String expression) {
        //сначала обработаем все скобки
        System.out.print(expression);
        Matcher bracketMatcher = bracketExpressionPattern.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (bracketMatcher.find()) {
            String bracketExpression = bracketMatcher.group();
            bracketExpression = bracketExpression.substring(1, bracketExpression.length()-1); // remove ()
            for (OperatorHandler op : ops) {
                bracketExpression = op.resolve(bracketExpression);
            }
            sb.setLength(0);
            expression = bracketMatcher.appendReplacement(sb, bracketExpression).appendTail(sb).toString();
            System.out.print(" -> " + expression);
            bracketMatcher = bracketExpressionPattern.matcher(expression);
        }

        //немного повтора кода, но теперь не осталось скобок и надо обработать оставшееся
        for (OperatorHandler op : ops) {
            expression= op.resolve(expression);
        }
        System.out.print(" -> " + expression);

        System.out.println();
        return Double.parseDouble(expression);
    }

}
