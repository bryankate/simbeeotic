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


import harvard.robobees.simbeeotic.model.Event;
import harvard.robobees.simbeeotic.model.Model;

import java.util.List;


/**
 * An interface that defines the public functionality of the simulation engine
 * executive. This is the interface that models will use to schedule events
 * and locate other models.
 *
 * @author bkate
 */
public interface SimEngine {

    /**
     * Schedules an event to be processed in the future.
     *
     * @param modelId The ID of the model that is the target for the event.
     * @param time The simulation time at whicht he event should be executed.
     * @param event The event to execute at the given time.
     *
     * @return The handle of the scheduled event, which can be used to cancel the event.
     *         The handle will be an integer greater than or equal to one if the event
     *         was successfully scheduled, or less than one if the event could not be
     *         scheduled (due to the simulation being previously terminated).
     */
    public long scheduleEvent(int modelId, SimTime time, Event event);


    /**
     * Cancels the execution of an event, if it hasn't already been processed.
     *
     * @param eventId The ID of the event to cancel.
     */
    public void cancelEvent(long eventId);


    /**
     * If this method is invoked, the simulation engine will cancel all future events and
     * not accept any new requests to schedule events. This will have the effect of
     * terminating the scenario. This method may be called by a model in the context of
     * initialization (though that would be silly) or processing an event. The bahavior
     * of any other entity calling this method (including components) is undefined since
     * it is not guaranteed that the simulation engine implementation is thread safe.
     */
    public void requestScenarioTermination();


    /**
     * Finds a model by its identifier.
     *
     * @param ID The identifier of the model to locate.
     *
     * @return The model instance, if one exists by the given identifier.
     */
    public Model findModelById(int ID);


    /**
     * This is a convenience method to find a model by its name. An exception
     * is thrown if there is more than one model with the given name. This method can
     * be useful in reducing pointless boilerplate code when a modeler knows that
     * there is exactly one (or zero) instances of a model in the scenario
     * (like a weather model). If there is more than one, there is likely an error
     * in the scenario configuration or the user's expectations.
     *
     * @param name The name of the model being sought.
     *
     * @return The model that goes by the given name, or {@code null} if none exists.
     *
     * @throws RuntimeException If there is more than one model with the given name.
     */
    public Model findModelByName(String name);


    /**
     * This is a convenience method to find a model by its type. An exception
     * is thrown if there is more than one model of the given type. This method can
     * be useful in reducing pointless boilerplate code when a modeler knows that
     * there is exactly one (or zero) instances of a mdoel type in the scenario
     * (like a weather model). If there is more than one, there is likely an error
     * in the scenario configuration or the user's expectations.
     *
     * @param type The type of the model being sought.
     *
     * @return The model that implements the given type (cast to that type), or {@code null} if none exists.
     *
     * @throws RuntimeException If there is more than one model of the given type.
     */
    public <T> T findModelByType(Class<T> type);


    /**
     * Find the set of models with a given name. There may be more than one model
     * returned because we do not require unique model names (yet).
     *
     * @param name The name of the model to locate.
     *
     * @return The set of models with the given name, or an empty set if none exist.
     */
    public List<Model> findModelsByName(String name);


    /**
     * Find the set of models of a certain type.
     *
     * @param type The type of the model to locate.
     *
     * @return The set of models that are a givne type, or an empty set if none exist.
     */
    public <T> List<T> findModelsByType(Class<T> type);
}
