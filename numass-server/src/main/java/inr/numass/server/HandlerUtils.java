/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.server;

import hep.dataforge.storage.api.StateLoader;

/**
 *
 * @author Alexander Nozik
 */
public class HandlerUtils {

    public static String renderStates(StateLoader states) {
        StringBuilder b = new StringBuilder();
        b.append("<div class=\"shifted\">\n");
        states.getValueStream().forEach(pair->{
            String color;
            switch (pair.getSecond().getType()) {
                case NUMBER:
                    color = "blue";
                    break;
                case BOOLEAN:
                    color = "red";
                    break;
                case TIME:
                    color = "magenta";
                    break;
                default:
                    color = "brown";

            }
            b.append(String.format("<p> <strong>%s</strong> : <font color= \"%s\">%s</font> </p>%n",
                    pair.getFirst(), color, pair.getSecond().getString()));
        });
        b.append("</div>\n");
        return b.toString();
    }

    public static void renderHTMLHeader(StringBuilder b) {
        b.append("\n<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "    <meta charset=\"utf-8\">\n"
                + "    <meta http-equiv=\"refresh\" content=\"30\">"
                + "    <title>Numass storage</title>\n"
                + "</head>\n"
                + "  <style>\n"
                + "   .shifted { \n"
                + "    margin: 20px;\n"
                + "   }\n"
                + "  </style>"
                + "<body>\n");
    }

    public static void renderHTMLFooter(StringBuilder b) {
        b.append("</body>\n"
                + "</html>");
    }

    public static void renderHeader(StringBuilder b, String header, int level) {
        b.append(String.format("<h%d>%s</h%d>%n", level, header, level));
    }
}
