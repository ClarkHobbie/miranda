import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import com.ltsllc.miranda.MirandaThread;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class Main {
    protected static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args)   {
        Configurator.initialize(
                "CONFIG",
                null,
                ".");
        Map map = new HashMap<>();
        map.put("io.netty", Level.WARN);
        Configurator.setLevel(map);
        LoggerContext ctx = (LoggerContext) LogManager
                .getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("STDOUT");

        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();

        logger.info("starting miranda");
        Miranda miranda = new Miranda();

        miranda = Miranda.getInstance();
        int iterations = 0;
        /*
        LoggerContext lContect = LogManager.getContext();
        Field f = null;
        try {
            f = lContect.getClass().getDeclaredField("configuration");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        f.setAccessible(true);

        DefaultConfiguration defaultConfiguration = null;
        try {
            defaultConfiguration = (DefaultConfiguration) f.get(lContect);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Config File: " + defaultConfiguration.getName());

         */
        MirandaThread thread = new MirandaThread();
        thread.start();
    }
}
