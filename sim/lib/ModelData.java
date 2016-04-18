package ajdurant.wsn.lib;

import java.util.HashMap;

import ptolemy.actor.NoTokenException;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.Token;
import ptolemy.data.expr.ModelScope;
import ptolemy.data.expr.Variable;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import ptolemy.kernel.util.Workspace;

public class ModelData extends TypedAtomicActor {

    public ModelData(CompositeEntity container, String name)
        throws NameDuplicationException, IllegalActionException
    {
        super(container, name);
        
        attribute = new HashMap<String, Attribute>();
        attributeVersion = new HashMap<String, Long>();
    }
    
    private HashMap<String, Attribute> attribute;
    private HashMap<String, Long> attributeVersion;
    
    /** Clone the actor into the specified workspace.
     *  @param workspace The workspace for the new object.
     *  @return A new actor.
     *  @exception CloneNotSupportedException If a derived class contains
     *   an attribute that cannot be cloned.
     */
    @Override
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        ModelData newObject = (ModelData) super.clone(workspace);

        attribute = new HashMap<String, Attribute>();
        attributeVersion = new HashMap<String, Long>();

        return newObject;
    }
    
    /**
     * Discard Token from the given port.
     * @param port Port to remove token from.
     * @throws NoTokenException
     * @throws IllegalActionException
     */
    public void clearToken(TypedIOPort port) throws NoTokenException, IllegalActionException {
        port.get(0);
    }

    /** Return the (presumably Settable) attribute modified by this
     *  actor.  This is the attribute in the container of this actor
     *  with the name given by the variableName attribute.  If no such
     *  attribute is found, then this method creates a new variable in
     *  the actor's container with the correct name.  This method
     *  gets write access on the workspace.
     *  
     *  Modified from ptolemy.actor.lib.SetVariable
     *  
     *  @exception IllegalActionException If the variable cannot be found.
     *  @return The attribute modified by this actor.
     */
    private Attribute getModifiedVariable(String variableNameValue) throws IllegalActionException {
        if (attributeVersion.containsKey(variableNameValue)) {
            if (_workspace.getVersion() == attributeVersion.get(variableNameValue)) {
                return attribute.get(variableNameValue);
            }
        }
        NamedObj container = getContainer();

        if (container == null) {
            throw new IllegalActionException(this, "No container.");
        }

        attribute.put(variableNameValue, null);

        if (!variableNameValue.equals("")) {
            // Look for the variableName anywhere in the hierarchy
            Attribute _attribute = ModelScope.getScopedAttribute(null, container,
                    variableNameValue);
            if (_attribute == null) {
                try {
                    workspace().getWriteAccess();

                    // container might be null, so create the variable
                    // in the container of this actor.
                    _attribute = new Variable(getContainer(), variableNameValue);
                } catch (IllegalActionException ex) {
                    throw new IllegalActionException(this, ex,
                            "Failed to create Variable \"" + variableNameValue
                            + "\" in " + getContainer().getFullName()
                            + ".");
                } catch (NameDuplicationException ex) {
                    throw new InternalErrorException(ex);
                } finally {
                    workspace().doneWriting();
                }
            }
            attributeVersion.put(variableNameValue, _workspace.getVersion());
            attribute.put(variableNameValue, _attribute);
        }
        return attribute.get(variableNameValue);
    }
    
    /** Set the value of the associated container's variable.
     * 
     * Modified from ptolemy.actor.lib.SetVariable
     * 
     *  @param variableNameValue The name of the variable to set.
     *  @param value The new value.
     *  @throws IllegalActionException
     */
    public void setVariable(String variableNameValue, Token value) throws IllegalActionException {
        Attribute variable = getModifiedVariable(variableNameValue);

        if (variable instanceof Variable) {
            Token oldToken = ((Variable) variable).getToken();

            if (oldToken == null || !oldToken.equals(value)) {
                ((Variable) variable).setToken(value);

                // NOTE: If we don't call validate(), then the
                // change will not propagate to dependents.
                ((Variable) variable).validate();
            }
        } else if (variable instanceof Settable) {
            ((Settable) variable).setExpression(value.toString());

            // NOTE: If we don't call validate(), then the
            // change will not propagate to dependents.
            ((Settable) variable).validate();
        } else {
            throw new IllegalActionException(this,
                    "Cannot set the value of the variable " + "named: "
                            + variableNameValue);
        }
    }
    
    /**
     * 
     * @param variableNameValue The name of the variable to get.
     * @return The value.
     * @throws IllegalActionException
     */
    public Token getVariable(String variableNameValue) {
        Attribute variable;
        try {
            variable = getModifiedVariable(variableNameValue);
            return ((Variable) variable).getToken();
        } catch (IllegalActionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
