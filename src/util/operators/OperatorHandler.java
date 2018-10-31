package util.operators;


import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OperatorHandler {

    private String signs;
    private Pattern pattern;

//    private UnaryOperator<Double> unaryOperator;
//
//    public OperatorHandler(String regExSign, UnaryOperator<Double> unaryOperator) {
//        this.unaryOperator = unaryOperator;
//        this.sign = regExSign;
//        this.pattern = Pattern.compile("((?<=[(^])-)?(\\\\d+([,.]\\\\d+)?)" + sign);
//    }

    private BinaryOperator<Double>[] binaryOperator;

    @SafeVarargs
    //в первом аргументе надо учитывать, что строка напрямую идёт в регулярку, во втором что generic типы
    public OperatorHandler(String regExSigns, BinaryOperator<Double> ...binaryOperator) {
        if (regExSigns.length() != binaryOperator.length)
            throw new RuntimeException("Количество симолов операций не равно количеству функций");
        this.binaryOperator = binaryOperator;
        this.signs = regExSigns;
        this.pattern = Pattern.compile("(?<var1>((?<!\\d)-)?(\\d+([,.]\\d+)?))(?<sign>[" + regExSigns + "])(?<var2>-?(\\d+([,.]\\d+)?))");
    }

    //основной метод. можно создать интерфейс/абстрактный класс с ним и несколько реализаций
    public String resolve(String expression) {
        Matcher matcher = pattern.matcher(expression);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Double variable1 = Double.valueOf(matcher.group("var1"));
            Double variable2 = Double.valueOf(matcher.group("var2"));
            String sign = matcher.group("sign");
            Double result = binaryOperator[signs.indexOf(sign)].apply(variable1, variable2);
            sb.setLength(0);
            expression = matcher.appendReplacement(sb, String.valueOf(result)).appendTail(sb).toString();
            matcher = pattern.matcher(expression);
        }
        return expression;
    }
}
