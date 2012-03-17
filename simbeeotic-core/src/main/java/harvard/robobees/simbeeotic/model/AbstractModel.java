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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import harvard.robobees.simbeeotic.SimEngine;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import org.w3c.dom.Document;


/**
 * A base convenience class for models. This implementation introduces a convenience
 * mechanism through which modelers can sepcify event handlers and have them
 * automatically invoked by the framework when events are processed.
 *
 * @author bkate
 */
public abstract class AbstractModel implements Model, TimerFactory {

    private int modelId;
    private String name = "";

    private Random rand;
    private SimTime currTime = new SimTime(0);
    private Document optionalConfig;
    private SimEngine simEngine;
    private Aggregator aggregator = new Aggregator();

    /**
     * A mapping of reflected methods to use for any given event type.  The list shall contain
     * handlers for the specific type mapped in order from subclass to the superclass implementations.
     */
    private Map<Class, List<Method>> eventHandlers = new HashMap<Class, List<Method>>();
    private boolean initialized = false;

    private Model parent = null;
    private Set<Model> children = new HashSet<Model>();

    private static Logger logger = Logger.getLogger(AbstractModel.class);


    protected AbstractModel() {

        // inspect the class and find any event handlers
        reflectOnEventHandlers();
    }


    /** {@inheritDoc} */
    public void initialize() {
        initialized = true;
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
    public final Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit) {
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
    public final Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit, long period, TimeUnit periodUnit) {

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

        checkpoint();

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
     * A method that is called prior to executing each event on the model.
     */
    protected void checkpoint() {
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

                        throw new RuntimeException("Event handlers must take 2 arguments, a SimTime and an Event " +
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


    /**
     * Gets the time if the event currently being processed.
     *
     * @return The current time.
     */
    protected final SimTime getCurrTime() {
        return currTime;
    }


    /**
     * Gets the simulation engine that coordinates the events forthis model.
     *
     * @return The simulation engine through which events can be scheduled and models can be located.
     */
    protected final SimEngine getSimEngine() {
        return simEngine;
    }


    /**
     * Gets the aggregator associated with this model instance.
     *
     * @return The Aggregator used to sum values.
     */
    protected final Aggregator getAggregator() {
        return aggregator;
    }


    /**
     * Returns the random number generator associated with this model. The generator
     * has been seeded deterministically so that it produces repeatable number streams
     * if a scenario is executed multiple times.
     *
     * @return The seeded random number begerator for this entity.
     */
    public Random getRandom() {
        return rand;
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

        if (parent instanceof AbstractModel) {
            aggregator.setParentAggregator(((AbstractModel)parent).getAggregator());
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
    public Document getCustomConfig() {
        return optionalConfig;
    }


    /** {@inheritDoc} */
    @Override
    public void setCustomConfig(Document doc) {
        optionalConfig = doc;
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


    protected final boolean isInitialized() {
        return initialized;
    }


    @Inject
    public final void setSimEngine(@GlobalScope final SimEngine engine) {

        if (!initialized) {
            this.simEngine = engine;
        }
    }


    @Inject
    public final void setRandomSeed(@Named("random-seed") final long seed) {

        if (!initialized) {
            this.rand = new Random(seed);
        }
    }


    // this is not really optional. we just made it optional here to avoid having it double-injected
    // when an instance of this class is used in a Guice child injector module.
    @Inject(optional = true)
    public final void setModelId(@Named("model-id") final int id) {

        if (!initialized) {
            this.modelId = id;
        }
    }


    @Inject(optional = true)
    public final void setModelName(@Named("model-name") final String name) {

        if (!initialized) {
            this.name = name;
        }
    }
}
