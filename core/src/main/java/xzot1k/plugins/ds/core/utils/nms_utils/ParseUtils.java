package xzot1k.plugins.ds.core.utils.nms_utils;

public class ParseUtils {

    private static final char SPACE = ' ';
    private static final char MINUS = '-';
    private static final char PLUS = '+';

    public static int getInt(CharSequence text) {
        if (text == null)
            return 0;
        return getInt(text, 0, text.length());
    }

    public static int getInt(CharSequence text, int start, int end) {
        if (text == null)
            return 0;
        return parseNonDecimalNumber((byte) 2, 10, text, start, end).intValue();
    }

    private static Number parseNonDecimalNumber(byte type, int totalDigits, CharSequence text, int start, int end) {
        if (text == null)
            return 0;
        Number result = 0;
        boolean minus = false;
        byte totalWidth = 0;
        byte overLimit = 0;
        boolean onLimit = false;
        int limit;

        for (int i = start; i < end; ++i) {
            char c = text.charAt(i);
            switch (c) {
                case SPACE:
                    continue;
                case PLUS:
                    if (totalWidth == 0)
                        continue;
                    else
                        break;
                case MINUS:
                    if (minus) {
                        if (text.charAt(i - 1) == MINUS || text.charAt(i - 1) == PLUS) {
                            minus = false;
                            switch (type) {
                                case 0: // Byte
                                    result = -result.byteValue();
                                    break;
                                case 1: // Short
                                    result = -result.shortValue();
                                    break;
                                case 2: // Integer
                                    result = -result.intValue();
                                    break;
                                case 3: // Long
                                    result = -result.longValue();
                                    break;
                            }
                            continue;
                        }
                        break;
                    }
                    minus = true;
                    switch (type) {
                        case 0: // Byte
                            result = -result.byteValue();
                            break;
                        case 1: // Short
                            result = -result.shortValue();
                            break;
                        case 2: // Integer
                            result = -result.intValue();
                            break;
                        case 3: // Long
                            result = -result.longValue();
                            break;
                    }
                    continue;
            }
            if (c < 48 || c > 57)
                continue;
            if (totalWidth == 0) {
                if (c == 48)
                    continue;
                onLimit = isOnLimit(type, c);
            }
            int digit = c - 48;

            if (onLimit) {
                limit = checkOverLimit(type, minus, totalWidth);
                if (digit != limit)
                    if (digit > limit)
                        overLimit = 1;
                    else
                        onLimit = false;
            }
            if (++totalWidth > totalDigits || totalWidth == totalDigits && overLimit == 1)
                return getInfinityOf(type, minus);

            result = multiplyTen(type, result, minus ? -digit : digit);
        }
        return result;
    }

    private static boolean isOnLimit(byte type, char c) {
        return switch (type) {
            case 0 -> // Byte
                    c == 49;
            case 1 -> // Short
                    c == 51;
            case 2 -> // Integer
                    c == 50;
            case 3 -> // Long
                    c == 57;
            default -> false;
        };
    }

    private static Number getInfinityOf(byte type, boolean minus) {
        return switch (type) {
            case 0 -> // Byte
                    minus ? Byte.MIN_VALUE : Byte.MAX_VALUE;
            case 1 -> // Short
                    minus ? Short.MIN_VALUE : Short.MAX_VALUE;
            case 2 -> // Integer
                    minus ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            case 3 -> // Long
                    minus ? Long.MIN_VALUE : Long.MAX_VALUE;
            default -> 0;
        };
    }

    private static Number multiplyTen(byte type, Number result, int digit) {
        return switch (type) {
            case 0 -> // Byte
                    result.byteValue() * 10 + digit;
            case 1 -> // Short
                    result.shortValue() * 10 + digit;
            case 2 -> // Integer
                    result.intValue() * 10 + digit;
            case 3 -> // Long
                    result.longValue() * 10 + digit;
            default -> 0;
        };
    }

    private static int checkOverLimit(byte type, boolean minus, int totalWidth) {
        return switch (type) {
            case 0 -> // Byte
                    overByteLimit(minus, totalWidth);
            case 1 -> // Short
                    overShortLimit(minus, totalWidth);
            case 2 -> // Integer
                    overIntLimit(minus, totalWidth);
            case 3 -> // Long
                    overLongLimit(minus, totalWidth);
            default -> 0;
        };
    }

    private static int overByteLimit(boolean minus, int pos) {
        return switch (pos) {
            case 1 -> 2;
            case 2 -> minus ? 8 : 7;
            default -> 1;
        };
    }

    private static int overLongLimit(boolean minus, int pos) {
        return switch (pos) {
            case 1, 2, 6 -> 2;
            case 3, 4, 8 -> 3;
            case 5, 13, 14 -> 7;
            case 7, 17 -> 0;
            case 9 -> 6;
            case 10, 16 -> 8;
            case 11, 15 -> 5;
            case 12 -> 4;
            case 18 -> minus ? 8 : 7;
            default -> 9;
        };
    }

    private static int overIntLimit(boolean minus, int pos) {
        return switch (pos) {
            case 1 -> 1;
            case 2, 4, 8 -> 4;
            case 3 -> 7;
            case 5 -> 8;
            case 6 -> 3;
            case 7 -> 6;
            case 9 -> minus ? 8 : 7;
            default -> 2;
        };
    }

    private static int overShortLimit(boolean minus, int pos) {
        return switch (pos) {
            case 1 -> 2;
            case 2 -> 7;
            case 3 -> 6;
            case 4 -> minus ? 8 : 7;
            default -> 3;
        };
    }

}
