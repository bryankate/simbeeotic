package harvard.robobees.simbeeotic.configuration.variable;


import harvard.robobees.simbeeotic.configuration.scenario.Variable;
import harvard.robobees.simbeeotic.configuration.scenario.ConstantLoopingVariable;
import harvard.robobees.simbeeotic.configuration.scenario.UniformRandomLoopingVariable;
import harvard.robobees.simbeeotic.configuration.scenario.NormalRandomLoopingVariable;
import harvard.robobees.simbeeotic.configuration.scenario.EachLoopingVariable;
import harvard.robobees.simbeeotic.configuration.scenario.ForLoopingVariable;
import harvard.robobees.simbeeotic.configuration.scenario.MasterSeed;


/**
 * A factory class that can generate AbstractLoopingVariable instances from structured document configurations.
 *
 * @author bkate
 */
public final class LoopingVariableFactory {

    /**
     * Returns a variable that acts as the Master seed variable.
     *
     * @param def The definition of the master seed from the scenario.
     *
     * @return A variable that can be used to getnerate seed values.
     */
    public AbstractLoopingVariable newMasterSeedVariable(final MasterSeed def) {

        if (def == null) {
            throw new RuntimeException("Master seed definition cannot be null.");
        }

        if (def.getConstant() != null) {
            return new ConstantVariable("" + def.getConstant().getValue());
        }
        else {

            if (def.getVariable().getFor() != null) {
                return makeFor(def.getVariable().getFor());
            }
            else if (def.getVariable().getEach() != null) {
                return makeEach(def.getVariable().getEach());
            }
            else if (def.getVariable().getUniformRandom() != null) {
                return makeUniformRandom(def.getVariable().getUniformRandom());
            }
            else {
                return makeNormalRandom(def.getVariable().getNormalRandom());
            }
        }
    }


    /**
     * Instantiates a looping variable from the given name and scenario.
     *
     * @param definition The variable definition - parsed in as an object.
     *
     * @return A looping variable instance corresponding to the type in the definition.
     */
    public AbstractLoopingVariable newVariable(final Variable definition) {

        if (definition == null) {
            throw new RuntimeException("Looping variable definition is null.");
        }

        String name = definition.getName();

        if ((name == null) || (name.length() == 0)) {
            throw new RuntimeException("Looping variable definition must contain a name.");
        }

        AbstractLoopingVariable result;

        // try to make a specific type of variable
        if (definition.getConstant() != null) {
            result = makeConstant(definition.getConstant());

        }
        else if (definition.getNormalRandom() != null) {
            result = makeNormalRandom(definition.getNormalRandom());

        }
        else if (definition.getUniformRandom() != null) {
            result = makeUniformRandom(definition.getUniformRandom());

        }
        else if (definition.getEach() != null) {
            result = makeEach(definition.getEach());

        }
        else if (definition.getFor() != null) {
            result = makeFor(definition.getFor());

        }
        else {
            throw new RuntimeException("Invalid looping variable definition.");
        }
        result.setName(name);

        return result;
    }


    /**
     * Parses the parameters needed to make a ConstantVariable from the scenario element.
     *
     * @param definition The variable scenario element.
     *
     * @return A looping variable instance that uses the parsed parameters.
     */
    private AbstractLoopingVariable makeConstant(final ConstantLoopingVariable definition) {
        return new ConstantVariable(definition.getValue());
    }


    /**
     * Parses the parameters needed to make a UniformRandomVariable from the scenario element.
     *
     * @param definition The variable scenario element.
     *
     * @return A looping variable instance that uses the parsed parameters.
     */
    private AbstractLoopingVariable makeUniformRandom(final UniformRandomLoopingVariable definition) {

        AbstractLoopingVariable result;

        switch (definition.getSeedSource()) {

            case USER:

                if (definition.getSeed() == null) {
                    throw new RuntimeException("Random variable has USER seed source but no seed specified");
                }

                result = new UniformRandomVariable(definition.getSeed(), definition.getFrom(), definition.getTo(),
                                                   definition.getNumDraws(), definition.getFirstDraw(), false);
                break;

            case RANDOM_STREAM:

                result = new UniformRandomVariable(null, definition.getFrom(), definition.getTo(),
                                                   definition.getNumDraws(), definition.getFirstDraw(), true);
                break;

            case DEFAULT:
            default:

                result = new UniformRandomVariable("1", definition.getFrom(), definition.getTo(),
                                                   definition.getNumDraws(), definition.getFirstDraw(), false);
        }

        return result;
    }


    /**
     * Parses the parameters needed to make a NormalRandomVariable from the scenario element.
     *
     * @param definition The variable scenario element.
     *
     * @return A looping variable instance that uses the parsed parameters.
     */
    private AbstractLoopingVariable makeNormalRandom(final NormalRandomLoopingVariable definition) {

        AbstractLoopingVariable result;

        switch (definition.getSeedSource()) {

            case USER:

                if (definition.getSeed() == null) {
                    throw new RuntimeException("Random variable has USER seed source but no seed specified");
                }

                result = new NormalRandomVariable(definition.getSeed(), definition.getFrom(), definition.getTo(),
                                                  definition.getNumDraws(), definition.getFirstDraw(), definition.getMean(), false);
                break;

            case RANDOM_STREAM:

                result = new NormalRandomVariable(null, definition.getFrom(), definition.getTo(),
                                                  definition.getNumDraws(), definition.getFirstDraw(), definition.getMean(), true);
                break;

            case DEFAULT:
            default:

                result = new NormalRandomVariable("1", definition.getFrom(), definition.getTo(), 
                                                  definition.getNumDraws(), definition.getFirstDraw(), definition.getMean(), false);
        }


        return result;
    }


    /**
     * Parses the parameters needed to make an EachVariable from the scenario element.
     *
     * @param definition The variable scenario element.
     *
     * @return A looping variable instance that uses the parsed parameters.
     */
    private AbstractLoopingVariable makeEach(final EachLoopingVariable definition) {

        //TODO handle external resource for pick list
        return new EachVariable(definition.getValue(), definition.getFirstDraw(), definition.getNumDraws());
    }


    /**
     * Parses the parameters needed to make a ForVariable from the scenario element.
     *
     * @param definition The variable scenario element.
     *
     * @return A looping variable instance that uses the parsed parameters.
     */
    private AbstractLoopingVariable makeFor(final ForLoopingVariable definition) {
        return new ForVariable(definition.getFrom(), definition.getTo(), definition.getStep());
    }

}

