package harvard.robobees.simbeeotic;


import harvard.robobees.simbeeotic.util.DocUtils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * @author bkate
 */
public class Simbeeotic {

    private static Logger logger = Logger.getLogger(Simbeeotic.class);

    private static final String OPTION_SCENARIO = "scenario";
    private static final String OPTION_WORLD = "world";
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

        // load scenario
        Document scenario = null;

        if (opts.has(OPTION_SCENARIO)) {

            try {

                InputStream stream = new FileInputStream((File)opts.valueOf(OPTION_SCENARIO));

                scenario = DocUtils.getDocumentFromXml(stream);
            }
            catch(FileNotFoundException fnf) {

                logger.fatal("Could not open the scenario XML file.", fnf);
                return;
            }
        }

        if (scenario == null) {

            logger.fatal("Must supply a scenario XML file.");
            return;
        }

        // load the world description
        Document world = null;

        if (opts.has(OPTION_WORLD)) {

            try {

                InputStream stream = new FileInputStream((File)opts.valueOf(OPTION_WORLD));

                world = DocUtils.getDocumentFromXml(stream);
            }
            catch(FileNotFoundException fnf) {

                logger.fatal("Could not open the scenario XML file.", fnf);
                return;
            }
        }

        if (world == null) {

            logger.fatal("Must supply a world XML file.");
            return;
        }

        // start up the simulation
        SimController sim = new SimController();

        sim.runSim(scenario, world);
    }
}
