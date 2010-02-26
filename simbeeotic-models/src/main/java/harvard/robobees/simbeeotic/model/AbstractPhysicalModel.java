package harvard.robobees.simbeeotic.model;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimEngine;
import harvard.robobees.simbeeotic.comms.PropagationModel;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;

import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;


/**
 * A convenience class that bridges the functionality of {@link AbstractPhysicalEntity} with
 * base functionality needed by all {@link Model} implementations.
 *
 * @author bkate
 */
public abstract class AbstractPhysicalModel extends AbstractPhysicalEntity implements PhysicalModel {

    /**
     * A mapping of reflected methods to use for any given event type.  The list shall contain
     * handlers for the specific type mapped in order from subclass to the superclass implementations.
     */
    private Map<Class, List<Method>> eventHandlers = new HashMap<Class, List<Method>>();

    private PropagationModel commModel;
    private SimEngine simEngine;
    private int modelId;

    private static Logger logger = Logger.getLogger(AbstractPhysicalModel.class);


    protected AbstractPhysicalModel() {

        // inspect the class and find any event handlers
        reflectOnEventHandlers();
    }


    /**
     * {@inheritDoc}
     *
     * This implementation uses reflection and annotations to locate the
     * appropriate event handler for a given {@link Event} type.
     */
    @Override
    public final void processEvent(final SimTime time, final Event event) {

        Method handler = findHandler(event.getClass());

        if (handler == null) {
            throw new EventNotSupportedException("No handler was found for an event of type: " + event.getClass());
        }

        try {
            handler.invoke(this, time, event);
        }
        catch(IllegalAccessException iae) {

            throw new RuntimeModelingException("Could not invoke event handler.\n" +
                                               "Event: " + event.getClass() + "\n" +
                                               "Target: " + this.getClass());
        }
        catch(InvocationTargetException ite) {

            RuntimeException newE = new RuntimeModelingException("Exception occured while processing event.\n" +
                                                                 "SimTime: " + time + "\n" +
                                                                 "Event: " + event.getClass() + "\n" +
                                                                 "Target: " + this.getClass(), ite.getCause());

            logger.error("While invoking an event handler, an exception occured", newE);
        }
    }


    /**
     * A convenience mechanism to schedule an event on oneself. The time of
     * the event will be checked for consistency prior to scheduling the event.
     *
     * @param time The time at which the event is to be executed.
     * @param event The event to schedule for future execution.
     */
    protected final void scheduleEvent(final SimTime time, final Event event) {
        simEngine.scheduleEvent(modelId, time, event);
    }


    /**
     * This helper method takes a {@code Class} representing an {@link Event} and returns
     * the reflected {@code Method} that represents the most appropriate handler implementation.
     * The type search operates recursively and returns the most specific handler available.
     *
     * @param eventType The type of event for which a handler is desired.
     *
     * @return The actual {@code Method} to use to handle the event, or {@code null} if none is found.
     */
    private Method findHandler(final Class eventType) {

        Method handler = null;

        // first try to get a handler with the same type
        List<Method> handlers = eventHandlers.get(eventType);

        if (handlers == null) {

            Set<Class> supers = new HashSet<Class>();

            if (eventType.getSuperclass() != null) {
                supers.add(eventType.getSuperclass());
            }

            Collections.addAll(supers, eventType.getInterfaces());

            // if that doesn't work, look for handlers of the super types
            for (Class superType : supers) {

                handler = findHandler(superType);

                if (handler != null) {

                    // cache the result since it was not previously known
                    handlers = new ArrayList<Method>();
                    handlers.add(handler);

                    eventHandlers.put(eventType, handlers);
                    break;
                }
            }
        }
        else {
            handler = handlers.get(0);
        }

        return handler;
    }


    /**
     * This internal method does all the introspection for annotated methods to be used
     * as event handlers. The result is stored in a field.  Handlers in subclasses that
     * handle the same exact same event type as a super class will take precedent.
     */
    private void reflectOnEventHandlers() {

        // first class processed is this class
        Class currClass = this.getClass();

        do {

            // now add methods that are event handlers from base to leaf so that methods
            // lower down the tree will replace those already added in the previous iteration
            for (Method method : currClass.getDeclaredMethods()) {

                EventHandler anot = method.getAnnotation(EventHandler.class);

                if (anot != null) {

                    Class[] params = method.getParameterTypes();
                    Class eventType = null;
                    boolean badHandler = true;

                    if (params.length == 2) {

                        // check param types to be sure it is a valid handler
                        if (SimTime.class.isAssignableFrom(params[0]) &&
                            Event.class.isAssignableFrom(params[1])) {

                            // infer the event type based on method param
                            eventType = params[1];
                            badHandler = false;
                        }
                    }

                    if ((eventType == null) || badHandler) {

                        throw new RuntimeException("Event handlers must take up 2 arguments, a SimTime and an Event " +
                                                   "to process. The type of Event being handled will " +
                                                   "be infered from the Event parameter type.");
                    }

                    // cache the handler
                    if (!eventHandlers.containsKey(eventType)) {
                        eventHandlers.put(eventType, new ArrayList<Method>());
                    }

                    eventHandlers.get(eventType).add(method);
                }
            }

            // advance to the next super class
            currClass = currClass.getSuperclass();
        }
        while(currClass != null && !currClass.isInterface());
    }


    protected final PropagationModel getCommModel() {
        return commModel;
    }


    protected final SimEngine getSimEngine() {
        return simEngine;
    }


    /** {@inheritDoc} */
    @Override
    public final int getModelId() {
        return modelId;
    }


    @Inject
    public final void setModelId(@Named("model-id") final int id) {

        if (!isInitialized()) {
            this.modelId = id;
        }
    }


    @Inject
    public final void setCommModel(@GlobalScope final PropagationModel commModel) {

        if (!isInitialized()) {
            this.commModel = commModel;
        }
    }


    @Inject
    public final void setSimEngine(@GlobalScope final SimEngine engine) {
        this.simEngine = engine;
    }
}
