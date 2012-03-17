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
package harvard.robobees.simbeeotic.configuration;


import harvard.robobees.simbeeotic.configuration.scenario.Scenario;
import harvard.robobees.simbeeotic.configuration.scenario.Variable;
import harvard.robobees.simbeeotic.configuration.scenario.Variables;
import harvard.robobees.simbeeotic.configuration.variable.AbstractLoopingVariable;
import harvard.robobees.simbeeotic.configuration.variable.LoopingVariableFactory;
import harvard.robobees.simbeeotic.configuration.variable.RandomVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * @author bkate
 */
public class VariationIterator implements Iterator<Variation>, Iterable<Variation> {

    // the number of repetitions that this controller should perform
    private int numRepetitions = 1;

    // looping controls
    private int curRepetition = 0;

    // the list of variable maps (one per execution)
    private List<Variation> variableMaps = new ArrayList<Variation>();

    private Iterator<Variation> backingIter;


    public VariationIterator(Scenario rawScenario) {

        List<AbstractLoopingVariable> variables = null;
        LoopingVariableFactory varFact = new LoopingVariableFactory();
        AbstractLoopingVariable masterSeed = varFact.newMasterSeedVariable(rawScenario.getMasterSeed());

        Variables loopingVars = null;

        if (rawScenario.getLooping() != null) {
            loopingVars = rawScenario.getLooping().getVariables();
        }

        if (loopingVars != null) {

            variables = new ArrayList<AbstractLoopingVariable>();

            for (Variable varDef : loopingVars.getVariable()) {
                variables.add(varFact.newVariable(varDef));
            }
        }
        else {
            variables = new ArrayList<AbstractLoopingVariable>();
        }

        // start parsing the looping variables
        List<AbstractLoopingVariable> ordered = new ArrayList<AbstractLoopingVariable>();
        List<AbstractLoopingVariable> deps = new ArrayList<AbstractLoopingVariable>();
        Set<String> names = new HashSet<String>();

        // bin the variables into those with and without dependencies
        for (AbstractLoopingVariable var : variables) {

            if (names.contains(var.getName())) {
                throw new InvalidScenarioException("Two looping variables have the same name: " + var.getName() + ".");
            }

            if (var.getDependencies().isEmpty()) {
                ordered.add(var);
            }
            else {
                deps.add(var);
            }

            names.add(var.getName());
        }

        // create an ordered list of variables - in order of execution that can ensure dependency resolution.
        // this algorithm goes through each variable that has a dependency and tries to insert it into the BACK
        // of the ordered list of variables. if the enabling variables are present, it is inserted, otherwise
        // it is skipped until the next iteration. if no variables are inserted on an iteration but the list of
        // variables is not exhausted, then the variables remaining cannot be inserted because of a missing dependency
        // or a cyclical relationship. as long as the variables are inserted into the back of the list, and the
        // variables are executed in list order, all dependencies will be resolved properly.
        while(deps.size() > 0) {

            List<AbstractLoopingVariable> inserted = new ArrayList<AbstractLoopingVariable>();

            for (AbstractLoopingVariable var : deps) {

                // determine if the variable can be inserted into the tree yet (if all its parents are present)
                boolean canInsert = true;

                // look if each dependency can be satisfied
                for (String dependency : var.getDependencies()) {

                    boolean found = false;

                    for (AbstractLoopingVariable enabler : ordered) {

                        if (enabler.getName().equals(dependency)) {
                            found = true;
                            break;
                        }
                    }

                    // did not find a required dependency in the ordered list
                    if (!found) {
                        canInsert = false;
                        break;
                    }
                }

                // all dependencies satisfied, insert it into the ordered list and add it to the list of inserted vars
                if (canInsert) {
                    ordered.add(var);
                    inserted.add(var);
                }
            }

            if (inserted.size() > 0) {

                // something was inserted on this iteration, remove it from consideration next time
                for (AbstractLoopingVariable var : inserted) {
                    deps.remove(var);
                }
            }
            else {

                // nothing was inserted from the list, so dependencies are unresolvable
                throw new InvalidScenarioException("Cannot resolve variable dependency. Either a dependency does " +
                                                   "not exist, or there is a cycle in the variable dependency graph.");
            }
        }

        // outer loop is for the master seeds
        for (String currMasterSeed : masterSeed.getValues()) {

            long seed = (long)Math.floor(Double.valueOf(currMasterSeed));
            Random rand = new Random(seed);
            List<Variation> finishedMaps = new LinkedList<Variation>();

            // set the seeds for all externally seeded random variables by
            // drawing from a master stream derived from the current master seed.
            for (AbstractLoopingVariable var : variables) {

                if (var instanceof RandomVariable) {

                    RandomVariable rVar = (RandomVariable)var;

                    if (rVar.isExternallySeeded()) {
                        rVar.setSeed("" + rand.nextLong());
                    }
                }
            }

            // parametrically combine the variables to define a list of variable maps
            for (AbstractLoopingVariable var : ordered) {

                // special case of the first variable
                if (finishedMaps.isEmpty()) {

                    // Make a map for each value of this variable
                    List<String> values = var.getValues();

                    for(String value : values) {

                        Map<String, String> nextVariableMap = new HashMap<String, String>();

                        nextVariableMap.put(var.getName(), value);
                        finishedMaps.add(new VariationImpl(seed, nextVariableMap));
                    }

                    continue;
                }

                List<Variation> newMaps = new ArrayList<Variation>();

                // go through each existing map and add the value from this variable to it
                for (Variation existing : finishedMaps) {

                    Set<String> varDeps = var.getDependencies();

                    // set the values of the dependencies of this variable from the values in the current existing map
                    for(String depName : varDeps) {
                        var.setDependencyValue(depName, existing.getVariables().get(depName));
                    }

                    // Get the values of this variable and make a variation of the existing map for each one
                    List<String> values = var.getValues();

                    if (values.size() == 1) {

                        // special case to avoid copying the map
                        existing.getVariables().put(var.getName(), values.get(0));
                        newMaps.add(existing);
                    }
                    else {

                        // make a copy of the existing map for each value
                        for (String value : values) {

                            Map<String, String> nextVariableMap = new HashMap<String, String>(existing.getVariables());

                            nextVariableMap.put(var.getName(), value);
                            newMaps.add(new VariationImpl(seed, nextVariableMap));
                        }
                    }
                }

                finishedMaps = newMaps;
            }

            // no variable maps defined, make an empty for the default context
            if (finishedMaps.isEmpty()) {
                finishedMaps.add(new VariationImpl(seed, new HashMap<String, String>()));
            }

            variableMaps.addAll(finishedMaps);
        }

        backingIter = variableMaps.iterator();
    }


    public int size() {
        return variableMaps.size() * numRepetitions;
    }


    /** {@inheritDoc} */
    public synchronized boolean hasNext() {
        return (backingIter.hasNext() || (curRepetition < numRepetitions ));
    }

    /** {@inheritDoc} */
    public Variation next() {

        if (!backingIter.hasNext() && hasNext()) {
            backingIter = variableMaps.iterator(); // reset position
        }

        Variation ret = backingIter.next();

        if (!backingIter.hasNext()) {
            this.curRepetition++;
        }

        return ret;
    }


    /** {@inheritDoc} */
    public void remove() {
        throw new UnsupportedOperationException("Can not remove scenario variations from the Iterator");
    }


    /**
     * This class is also iterable so it can easily be used from enhanced for loops
     */
    public Iterator<Variation> iterator() {
        return this;
    }


    private static final class VariationImpl implements Variation {

        private long seed;
        private Map<String, String> variables;


        private VariationImpl(long seed, Map<String, String> variables) {

            this.seed = seed;
            this.variables = variables;
        }


        public long getSeed() {
            return seed;
        }


        public Map<String, String> getVariables() {
            return variables;
        }
    }
}
