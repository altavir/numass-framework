package hep.dataforge.grind.terminal

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.LayoutBase
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import groovy.transform.CompileStatic
import org.jline.terminal.Terminal
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Logback log formatter for terminals
 * Created by darksnake on 06-Nov-16.
 */
@CompileStatic
class TerminalLogLayout extends LayoutBase<ILoggingEvent> {
    private static final AttributedStyle TIME = AttributedStyle.DEFAULT;
    private static final AttributedStyle THREAD = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
    private static final AttributedStyle DEBUG = AttributedStyle.DEFAULT;
    private static final AttributedStyle INFO = AttributedStyle.BOLD;
    private static final AttributedStyle WARN = AttributedStyle.BOLD.foreground(AttributedStyle.RED);
    private static
    final AttributedStyle ERR = AttributedStyle.BOLD.foreground(AttributedStyle.BLACK).background(AttributedStyle.RED);

    public static Appender<ILoggingEvent> buildAppender(LoggerContext context, Terminal terminal) {
        TerminalLogLayout layout = new TerminalLogLayout(terminal);
        layout.setContext(context);
        layout.start();
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(context);
        encoder.layout = new TerminalLogLayout(terminal);
        encoder.start()
        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setContext(context);
        appender.setOutputStream(terminal.output())
        appender.encoder = encoder;
        appender.name = "terminal"
        appender.start();
        return appender;
    }

    final Terminal terminal;
    boolean showThread = false;
    DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;

    TerminalLogLayout(Terminal terminal) {
        this.terminal = terminal
    }

    @Override
    String doLayout(ILoggingEvent event) {
        AttributedStringBuilder builder = new AttributedStringBuilder().with {
            append(timeFormatter.format(Instant.ofEpochMilli(event.timeStamp)), TIME);
            append("  ")
            if (showThread) {
                append("[${event.threadName}]  ", THREAD);
            }
            switch (event.level) {
                case Level.ERROR:
                    append(event.level.toString(), ERR);
                    break;
                case Level.WARN:
                    append(event.level.toString(), WARN);
                    break;
                case Level.DEBUG:
                    append(event.level.toString(), DEBUG);
                    break;
            }
            append("  ")
            append(event.formattedMessage);
        };
        return builder.toAnsi(terminal);
    }
}
