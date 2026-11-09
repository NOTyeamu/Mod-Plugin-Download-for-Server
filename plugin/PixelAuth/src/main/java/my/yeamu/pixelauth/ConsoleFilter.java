package my.yeamu.pixelauth;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.regex.Pattern;

/** Прячет из консоли строки «…issued server command: /login пароль», чтобы пароли не попадали в логи. */
final class ConsoleFilter extends AbstractFilter {

    private static final Pattern SECRET = Pattern.compile(
            "(?i)issued server command: /(?:[a-z0-9_]+:)?(?:login|l|log|register|reg|changepassword|changepass|cp|pixelauth\\s+(?:register|changepass))\\b.*");

    static void install() {
        ((Logger) LogManager.getRootLogger()).addFilter(new ConsoleFilter());
    }

    private static Result check(String msg) {
        return msg != null && SECRET.matcher(msg).find() ? Result.DENY : Result.NEUTRAL;
    }

    @Override
    public Result filter(LogEvent event) {
        return event == null || event.getMessage() == null ? Result.NEUTRAL : check(event.getMessage().getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return msg == null ? Result.NEUTRAL : check(msg.getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return check(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return msg == null ? Result.NEUTRAL : check(msg.toString());
    }
}
