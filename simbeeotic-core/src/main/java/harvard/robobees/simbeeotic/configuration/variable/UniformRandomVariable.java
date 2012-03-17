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


import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * A looping variable that uses a random number generator to determine the value at each iteration. The generator can be
 * seeded and configured to return values between a certain range. The variable returns random double numbers only, not
 * integers. Numbers are uniformally distributed over the input range.
 *
 * @author bkate
 */
public class UniformRandomVariable extends RandomVariable {


    public UniformRandomVariable(String seed, String minValue, String maxValue,
                                 String numDraws, String firstDraw, boolean externalSeed) {

        super(seed, minValue, maxValue, numDraws, firstDraw, externalSeed);
    }


    /**
     * Gets a list of randomly generated values, using a generator configured to the input parameters.
     *
     * @return A list of output values (that can be parsed into a double value).
     */
    @Override
    protected List<String> calculateValues() throws VariableCalculationException {


        List<String> results = new ArrayList<String>();

        // required values
        if ((getMinValue() == null) || (getMaxValue() == null)) {
            throw new VariableCalculationException("'uniform-random' variable needs an upper and lower bound.");
        }

        int numDraws;

        try {
            numDraws = intParam(getNumDraws());
        }
        catch (VariableCalculationException e) {
            throw new VariableCalculationException("num-draws parameter is not optional", e);
        }

        int firstDraw = intParam(getFirstDraw(), 0);    //get first draw (how many to skip)

        /*
        * Min and max values are optional for normal random looping variables.  They allow outliers to be discarded.
        */
        double minValue = doubleParam(getMinValue());
        double maxValue = doubleParam(getMaxValue());

        long seed = longParam(getSeed());
        Random rand = new Random(seed);

        // swap upper and lower if need be
        if (minValue > maxValue) {
            double temp = minValue;
            minValue = maxValue;
            maxValue = temp;
        }

        double diff = maxValue - minValue;

        // generate numbers
        double value;

        for (int i = 0; i < numDraws + firstDraw - 1; i++) {

            value = minValue + rand.nextDouble() * diff;

            if (i >= firstDraw - 1) {     //firstDraw uses 1 based indexing.  i.e. firstDraw=1 should skip nothing
                results.add(Double.toString(value));
            }
        }

        return results;
    }

}

