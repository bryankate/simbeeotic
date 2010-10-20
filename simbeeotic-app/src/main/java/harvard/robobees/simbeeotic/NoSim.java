package harvard.robobees.simbeeotic;


/**
 * Some syntactic sugar for invoking the Simbeeotic framework without
 * running an actual simulation.
 *
 * @author bkate
 */
public class NoSim {

    public static void main(String[] args) {

        System.setProperty("simbeeotic.nosim", "true");

        Simbeeotic.main(args);
    }

}
