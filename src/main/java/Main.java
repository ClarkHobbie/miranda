import com.ltsllc.commons.LtsllcException;
import com.ltsllc.miranda.Miranda;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    protected static final Logger logger = LogManager.getLogger(Main.class);


    public static void main4 (String[] args) {
        int i;
        i = 5;
        i++;
    }

    public static void main(String[] args) throws Exception {
        Miranda miranda = Miranda.getInstance();
        try {
            miranda.startUp(args);
            for (;;) {
                miranda.mainLoop();
            }
        } catch (LtsllcException ltsllcException) {
            logger.error ("Exception during start up " + ltsllcException.getMessage() + ltsllcException.getStackTrace());
        }
    }
}
