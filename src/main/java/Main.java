import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;

public class Main {
    protected static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args)   {
        Configurator.initialize(
                "CONFIG",
                null,
                ".");
        logger.info("starting miranda");
        Miranda miranda = new Miranda();
        miranda = Miranda.getInstance();

        try {
            miranda.startUp(args);
        } catch (Exception e) {
            logger.error("Exception during startup", e);
            System.exit(1);
        }
        try {
            for (;;) {
                miranda.mainLoop();
            }
        } catch (LtsllcException | IOException e) {
            logger.error ("Exception while running", e);
            System.exit(1);
        }

        System.exit(Miranda.exitCode);
    }
}
