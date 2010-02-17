package harvard.robobees.simbeeotic.model;


import java.util.Set;
import java.util.Properties;
import java.util.HashSet;


/**
 * @author bkate
 */
public class EntityInfo {

    private Properties properties = new Properties();
    private Set<Contact> contactPoints = new HashSet<Contact>();

    // todo: add pointer to 3d visualization


    public EntityInfo() {
    }


    public EntityInfo(final Properties props) {
        properties = props;
    }


    public Set<Contact> getContactPoints() {
        return contactPoints;
    }


    public Properties getProperties() {
        return properties;
    }
}
