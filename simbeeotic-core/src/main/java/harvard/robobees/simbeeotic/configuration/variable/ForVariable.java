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
package harvard.robobees.simbeeotic.configuration.variable;


import harvard.robobees.simbeeotic.util.DocUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * A looping variable that uses a loop to generate values between a lower bound (inclusive) and an upper bound
 * (inclusive) with a given step size.
 *
 * @author bkate
 */
public class ForVariable extends AbstractLoopingVariable {

    private String lower = null;
    private String upper = null;
    private String step = null;


    /**
     * Default constructor that takes the necessary looping arguments.
     *
     * @param lower The lower bound value (may be a reference).
     * @param upper The upper bound value (may be a reference).
     * @param step The step size value (may be a reference).
     */
    public ForVariable(String lower, String upper, String step) {

        this.lower = lower;
        this.upper = upper;
        this.step = step;

        // check for references
        addDepIfNeeded(lower);
        addDepIfNeeded(upper);
        addDepIfNeeded(step);
    }


    /**
     * Calculates a list of values using a loop from the lower value (inclusive) to the upper value (inclusive)
     * separated by the amount of the step size parameter.
     *
     * @return A list of values representing the output for the current parameter values.
     */
    protected List<String> calculateValues() throws VariableCalculationException {

        List<String> results = new ArrayList<String>();

        // check for unset params
        if ((lower == null) || (upper == null) || (step == null)) {
            throw new VariableCalculationException("The lower, upper, and step params must be set on " +
                                                   "the 'for' looping variable.");
        }

        double lowerVal = 0;
        double upperVal = 0;
        double stepVal = 0;

        try {

            // get the lower bound
            if (DocUtil.isPlaceholder(lower)) {
                lowerVal = Double.parseDouble(getDependencyValue(DocUtil.extractPlaceholderName(lower)));
            }
            else {
                lowerVal = Double.parseDouble(lower);
            }

            // get the upper bound
            if (DocUtil.isPlaceholder(upper)) {
                upperVal = Double.parseDouble(getDependencyValue(DocUtil.extractPlaceholderName(upper)));
            }
            else {
                upperVal = Double.parseDouble(upper);
            }

            // get the step size
            if (DocUtil.isPlaceholder(step)) {
                stepVal = Double.parseDouble(getDependencyValue(DocUtil.extractPlaceholderName(step)));
            }
            else {
                stepVal = Double.parseDouble(step);
            }
        }
        catch (NumberFormatException nfe) {
            throw new VariableCalculationException("The lower, upper, and step size must be double values in " +
                                                   "the 'for' looping variable. (" +
                                                   lower + "," + upper + "," + step + ")", nfe);
        }

        if (stepVal == 0.0) {
            throw new VariableCalculationException("Step value cannot be zero in 'for' looping variable.");
        }

        // check ordering of arguments
        if (((lowerVal > upperVal) && (stepVal > 0.0)) ||
            ((lowerVal < upperVal) && (stepVal < 0.0))) {

            throw new VariableCalculationException("Imporperly ordered bounds in 'for' looping variable.");
        }

        // generate values
        for (double i = lowerVal; i <= upperVal; i += stepVal) {
            results.add(Double.toString(i));
        }

        return results;
    }
}

