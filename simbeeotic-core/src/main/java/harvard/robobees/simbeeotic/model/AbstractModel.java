package harvard.robobees.simbeeotic.model;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import harvard.robobees.simbeeotic.SimEngine;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;


/**
 * A base convenience class for models. This implementation introduces a convenience
 * mechanism through which modelers can sepcify event handlers and have them
 * automatically invoked by the framework when events are processed.
 *
 * @author bkate
 */
public abstract class AbstractModel implements Model {

    private int modelId;
    private String name = "";

    private SimTime currTime = new SimTime(0);
    private SimEngine simEngine;

    /**
     * A mapping of reflected methods to use for any given event type.  The list shall contain
     * handlers for the specific type mapped in order from subclass to the superclass implementations.
     */
    private Map<Class, List<Method>> eventHandlers = new HashMap<Class, List<Method>>();

    private Model parent = null;
    private Set<Model> children = new HashSet<Model>();


    private static Logger logger = Logger.getLogger(AbstractModel.class);


    protected AbstractModel() {

        // inspect the class and find any event handlers
        reflectOnEventHandlers();
    }


    /**
     * Creates a new timer that will fire at a future point in time.
     *
     * @param callback The callback that will be made when the timer fires.
     * @param offset The offset from the current time at which the timer should fire. This
     *               value must be greater than or equal to zero.
     * @param offsetUnit The time unit in which the offset is measured.
     *
     * @return The timer object, which can be used to reset or cancel the firing.
     */
    protected final Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit) {
        return createTimer(callback, offset, offsetUnit, 0, TimeUnit.NANOSECONDS);
    }


    /**
     * Creates a new timer that will fire at a periodic rate starting at a future point in time.
     *
     * @param callback The callback that will be made when the timer fires.
     * @param offset The offset from the current time at which the timer should fire. This
     *               value must be greater than or equal to zero.
     * @param offsetUnit The time unit in which the offset is measured.
     * @param period The rate at which this timer fires. If the value is less than or equal to zero
     *               then the behavior is identical to {@link #createTimer(TimerCallback, long, java.util.concurrent.TimeUnit)}
     * @param periodUnit The time unit in which the period is measured.
     *
     * @return The timer object, which can be used to reset or cancel the firing.
     */
    protected final Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit, long period, TimeUnit periodUnit) {

        if ((modelId < 0) || (simEngine == null)) {
            throw new RuntimeModelingException("The model is not properly initialized.");
        }

        if (offset < 0) {
            throw new RuntimeModelingException("The timer offset cannot be less than 0 - that is in the past!");
        }

        return new Timer(modelId, simEngine, callback, new SimTime(currTime, offset, offsetUnit), period, periodUnit);
    }


    @EventHandler
    public final void handleTimerEvent(SimTime time, TimerEvent event) {
        event.getTimer().fire(time);
    }


    /**
     * {@inheritDoc}
     *
     * This implementation uses reflection and annotations to locate the
     * appropriate event handler for a given {@link Event} type.
     */
    @Override
    public final void processEvent(final SimTime time, final Event event) {

        // find the most appropriate handler for this event type (and cache it)
        Method handler = findHandler(event.getClass());

        if (handler == null) {
            throw new EventNotSupportedException("No handler was found for an event of type: " + event.getClass());
        }

        try {

            currTime = time;

            // invoke the custom handler for this event type
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
            throw newE;
        }
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


    protected final SimTime getCurrTime() {
        return currTime;
    }


    protected final SimEngine getSimEngine() {
        return simEngine;
    }


    /**
     * Gets the parent model of this model.
     *
     * @return The parent model of this model, or {@code null} if this model is a root.
     */
    protected final Model getParentModel() {
        return parent;
    }


    /**
     * {@inheritDoc}
     *
     * If custom inspection of the child model is desired then subclasses
     * can override this method and perform this inspection.
     */
    @Override
    public void setParentModel(Model parent) {

        if (this.parent != null) {
            throw new RuntimeException("Parent model already set!");
        }

        this.parent = parent;
    }


    /**
     * Gets the child models of this model.
     *
     * @return The child models, or an empty set if none exist.
     */
    protected final Set<Model> getChildModels() {
        return Collections.unmodifiableSet(children);
    }


    /**
     * {@inheritDoc}
     *
     * If custom inspection of the child model is desired then subclasses
     * can override this method and perform this inspection.
     */
    @Override
    public void addChildModel(Model child) {
        children.add(child);
    }


    /** {@inheritDoc} */
    @Override
    public final int getModelId() {
        return modelId;
    }


    /** {@inheritDoc} */
    @Override
    public final String getName() {
        return name;
    }


    @Inject
    public final void setSimEngine(@GlobalScope final SimEngine engine) {
        this.simEngine = engine;
    }


    // this is not really optional. we just made it optional here to avoid having it double-injected
    // when an instance of this class is used in a Guice child injector module.
    @Inject(optional = true)
    public final void setModelId(@Named("model-id") final int id) {
        this.modelId = id;
    }


    @Inject(optional = true)
    public final void setModelName(@Named("model-name") final String name) {
        this.name = name;
    }
}
