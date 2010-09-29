package harvard.robobees.simbeeotic;


import harvard.robobees.simbeeotic.util.DocUtil;
import harvard.robobees.simbeeotic.util.JaxbHelper;
import harvard.robobees.simbeeotic.configuration.scenario.Scenario;
import harvard.robobees.simbeeotic.configuration.world.World;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * @author bkate
 */
public class Simbeeotic {

    private static Logger logger = Logger.getLogger(Simbeeotic.class);

    private static final String OPTION_SCENARIO = "scenario";
    private static final String OPTION_WORLD = "world";
    private static final String OPTION_SCALE = "real-time-scale";
    private static final String OPTION_PAUSED = "paused";
    private static final String OPTION_LOG = "log";
    private static final String OPTION_HELP = "help";


    public static void main(String[] args) {

        // parse command line args
        OptionParser parser = new OptionParser();

        parser.accepts(OPTION_SCENARIO, "Scenario XML file.")
                .withRequiredArg()
                .ofType(File.class);

        parser.accepts(OPTION_WORLD, "World XML file.")
                .withRequiredArg()
                .ofType(File.class);

        parser.accepts(OPTION_SCALE, "Constrained real time scaling factor.")
                .withRequiredArg()
                .ofType(Double.class);

        parser.accepts(OPTION_PAUSED, "Start in a paused state.");

        parser.accepts(OPTION_LOG, "Log4j properties file (optional).")
                .withRequiredArg()
                .ofType(File.class);
        
        parser.accepts(OPTION_HELP, "Show help");

        OptionSet opts = parser.parse(args);

        // user asked for help?
        if (opts.has(OPTION_HELP)) {

            try {

                parser.printHelpOn(System.out);
                return;
            }
            catch(IOException ioe) {

                logger.fatal("Could not print help to stdout.", ioe);
                return;
            }
        }

        // load optional log4j properties
        if (opts.has(OPTION_LOG)) {

            Properties logProps = new Properties();

            try {
                logProps.load(new FileInputStream((File)opts.valueOf(OPTION_LOG)));
            }
            catch(FileNotFoundException fnf) {

                logger.fatal("Could not open the log4j properties file.", fnf);
                return;
            }
            catch(IOException ioe) {

                logger.fatal("Could not load the log4j properties file.", ioe);
                return;
            }

            Logger.getRootLogger().removeAllAppenders();
            PropertyConfigurator.configure(logProps);
        }

        // load scenario
        Document scenarioDoc = null;

        if (opts.has(OPTION_SCENARIO)) {

            try {

                InputStream stream = new FileInputStream((File)opts.valueOf(OPTION_SCENARIO));

                scenarioDoc = DocUtil.getDocumentFromXml(stream);
            }
            catch(FileNotFoundException fnf) {

                logger.fatal("Could not open the scenario XML file.", fnf);
                return;
            }
        }

        if (scenarioDoc == null) {

            logger.fatal("Must supply a scenario XML file.");
            return;
        }

        // load the world description
        Document worldDoc = null;

        if (opts.has(OPTION_WORLD)) {

            try {

                InputStream stream = new FileInputStream((File)opts.valueOf(OPTION_WORLD));

                worldDoc = DocUtil.getDocumentFromXml(stream);
            }
            catch(FileNotFoundException fnf) {

                logger.fatal("Could not open the scenario XML file.", fnf);
                return;
            }
        }

        if (worldDoc == null) {

            logger.fatal("Must supply a world XML file.");
            return;
        }


        Scenario scenario;
        World world;

        // parse the scenario and world documents
        try {

            scenario = JaxbHelper.objectFromNode(scenarioDoc, Scenario.class);
            world = JaxbHelper.objectFromNode(worldDoc, World.class);
        }
        catch(JAXBException je) {
            throw new RuntimeException("Could not parse the given scenario or world file.", je);
        }

        double scale = 0;

        if (opts.has(OPTION_SCALE)) {
            scale = (Double)opts.valueOf(OPTION_SCALE);
        }

        // start up the simulation
        SimController sim = new SimController();

        sim.runSim(scenario, world, scale, opts.has(OPTION_PAUSED));

        // explicitly exit so that the AWT threads will shutdown
        System.exit(0);
    }
}
