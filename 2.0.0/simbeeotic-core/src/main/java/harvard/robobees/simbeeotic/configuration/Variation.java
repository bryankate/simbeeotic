package harvard.robobees.simbeeotic.configuration;


import java.util.Map;


/**
 * @author bkate
 */
public interface Variation {

    public long getSeed();

    public Map<String, String> getVariables();
}
