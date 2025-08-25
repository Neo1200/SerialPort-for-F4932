package com.machine.f4932vend;

public final class Protocol {
    private Protocol() {}

    public static int asciiSum(String s) {
        int sum = 0;
        for (int i = 0; i < s.length(); i++) sum += (int) s.charAt(i);
        return sum;
    }

    private static String wrapAndBcc(String bodyWithPipes) {
        if (!(bodyWithPipes.startsWith("|") && bodyWithPipes.endsWith("|")))
            throw new IllegalArgumentException("Body must be like |X|...|");
        int bcc = asciiSum(bodyWithPipes);
        return "$$$" + bodyWithPipes + bcc + "%%%";
    }

    public static String raw(String body) { return wrapAndBcc(body); }

    // Queries
    public static String getPayInfo()     { return raw("|B|"); }
    public static String getSlotInfo()    { return raw("|C|"); }
    public static String getTemperature() { return raw("|D|"); }
    public static String getKeyInfo()     { return raw("|E|"); }
    public static String getOtherInfo()   { return raw("|F|"); }
    public static String getBalance()     { return raw("|G|"); }
    public static String clearBalance()   { return raw("|J|"); }

    // Select/payment flow
    public static String selectGoods(int slotNo)  { return raw("|H|" + slotNo + "|"); }
    public static String selectOk()               { return raw("|H|K|10|"); }
    public static String selectCancel()           { return raw("|H|K|11|"); }
    public static String takeGoodsByCode(String code) { return raw("|H|B|" + code + "|"); }

    // I/O etc.
    public static String reTime(int code)         { return raw("|I|" + code + "|"); }
    public static String returnCoin(String amt)   { return raw("|K|" + amt + "|"); }
    public static String networkConnected(int s)  { return raw("|L|" + s + "|"); }
    public static String lightOn()  { return raw("|M|O|"); }
    public static String lightOff() { return raw("|M|C|"); }
    public static String coolOn()   { return raw("|N|C|"); }
    public static String heatOn()   { return raw("|N|H|"); }
    public static String hvacStop() { return raw("|N|S|"); }

    // Read/Write menu
    public static String readMenu(int menu)            { return raw("|O|" + menu + "|"); }
    public static String writeMenu(int menu, String d) { return raw("|P|" + menu + "|" + d + "|"); }

    // Ship: A|price|slot|payMode|card|
    public static String ship(String price, String slot, int payMode, String cardOrNull) {
        String card = (cardOrNull == null) ? "" : cardOrNull;
        return raw("|A|" + price + "|" + slot + "|" + payMode + "|" + card + "|");
    }
}
