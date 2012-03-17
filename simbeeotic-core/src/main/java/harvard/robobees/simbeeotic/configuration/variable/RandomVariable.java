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


public abstract class RandomVariable extends AbstractLoopingVariable {

    private String minValue = null;
    private String maxValue = null;
    private String seed = null;
    private String numDraws = null;
    private String firstDraw = null;
    private boolean externallySeeded = false;


    /**
     * Default constructor - takes all the parameters needed to customize the generator.
     */
    public RandomVariable(String seed, String minValue, String maxValue, String numDraws, String firstDraw, boolean extSeed) {

        this.externallySeeded = extSeed;

        this.minValue = minValue;
        addDepIfNeeded(minValue);

        this.maxValue = maxValue;
        addDepIfNeeded(maxValue);

        this.seed = seed;

        if (!externallySeeded) {
            addDepIfNeeded(seed);
        }

        if (numDraws == null || "".equals(numDraws)) {      //guard
            throw new VariableCalculationException("num draws must be specified for all random looping variables, was null");
        }
        this.numDraws = numDraws;
        addDepIfNeeded(numDraws);

        this.firstDraw = firstDraw;
        addDepIfNeeded(firstDraw);

    }


    public void setSeed(String seed) {

        if (!externallySeeded) {

            throw new RuntimeException("The random variable '" + getName() + "' does not " +
                                       "have a RANDOM_STREAM seed source and cannot be re-seeded.");
        }

        // no need to check for dependencies here because
        // the new seed is drawn from the master RNG and is
        // not a placeholder
        this.seed = seed;

        // make sure that values are re-calculated after
        // the variable is re-seeded
        setDirty(true);
    }


    protected String getSeed() {
        return seed;
    }


    protected String getNumDraws() {
        return numDraws;
    }


    protected String getFirstDraw() {
        return firstDraw;
    }


    protected String getMinValue() {
        return minValue;
    }


    protected String getMaxValue() {
        return maxValue;
    }


    public boolean isExternallySeeded() {
        return externallySeeded;
    }
}

