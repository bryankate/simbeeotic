/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
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
        boolean noSim = Boolean.parseBoolean(System.getProperty("simbeeotic.nosim", "false"));

        SimController sim = new SimController();

        if (noSim) {
            sim.runComponents(scenario, world);
        }
        else {
            sim.runSim(scenario, world, scale, opts.has(OPTION_PAUSED));
        }

        // explicitly exit so that the AWT threads will shutdown
        System.exit(0);
    }
}
