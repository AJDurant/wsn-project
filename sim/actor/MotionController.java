package ajdurant.wsn.actor;

import ajdurant.wsn.lib.ModelData;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.Token;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class MotionController extends TypedAtomicActor {

    public MotionController(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
        // Inputs
        trigger = new TypedIOPort(this, "trigger", true, false);
        
        // Outputs
        nextLoc = new TypedIOPort(this, "nextLoc", false, true);
        nextLoc.setTypeEquals(typeLocation);
        new SingletonParameter(nextLoc, "_showName").setExpression("true");
        
        consumption = new TypedIOPort(this, "consumption", false, true);
        consumption.setTypeEquals(BaseType.DOUBLE);
        new SingletonParameter(consumption, "_showName").setExpression("true");
        
        modelData = new ModelData(container, "");
    }

    // Inputs
    public TypedIOPort trigger;
    
    // Outputs
    public TypedIOPort nextLoc;
    public TypedIOPort consumption;
    
    private ModelData modelData;
    
    @Override
    public void fire() throws IllegalActionException
    {
        super.fire();
        
        if (trigger.getWidth() > 0 && trigger.hasToken(0)) {
            modelData.clearToken(trigger);
            
            ArrayToken currentLocation = (ArrayToken) modelData.getVariable("currentLocation");
            ArrayToken targetLocation = (ArrayToken) modelData.getVariable("targetLocation");
            
            DoubleToken currentX = (DoubleToken) currentLocation.getElement(0);
            DoubleToken currentY = (DoubleToken) currentLocation.getElement(1);
            
            DoubleToken targetX = (DoubleToken) targetLocation.getElement(0);
            DoubleToken targetY = (DoubleToken) targetLocation.getElement(1);
            
            if(checkLocationChangeNeeded(currentX, currentY, targetX, targetY)) {
                modelData.setVariable("inMotion", new BooleanToken(true));
                moveTowardTarget(currentX, currentY, targetX, targetY);
                consumePower();
            } else {
                modelData.setVariable("inMotion", new BooleanToken(false));
            }
        }
        
    }
    
    private boolean checkLocationChangeNeeded(DoubleToken currentX, DoubleToken currentY, DoubleToken targetX, DoubleToken targetY) throws IllegalActionException {
        
        return (currentX.isCloseTo(targetX, 1.0).and(currentY.isCloseTo(targetY, 1.0))).not().booleanValue();
    }
    
    private void moveTowardTarget(DoubleToken currentX, DoubleToken currentY, DoubleToken targetX, DoubleToken targetY) throws IllegalActionException {
        
        if (targetX.isLessThan(currentX).booleanValue()) {
            currentX = (DoubleToken) currentX.subtract(DoubleToken.ONE);
        } else if (targetX.isGreaterThan(currentX).booleanValue()) {
            currentX = (DoubleToken) currentX.add(DoubleToken.ONE);
        }
        
        if (targetY.isLessThan(currentY).booleanValue()) {
            currentY = (DoubleToken) currentY.subtract(DoubleToken.ONE);
        } else if (targetY.isGreaterThan(currentY).booleanValue()) {
            currentY = (DoubleToken) currentY.add(DoubleToken.ONE);
        }
        
        ArrayToken nextLocation = new ArrayToken(new Token[] {currentX, currentY});
        
        nextLoc.send(0, nextLocation);
    }
    
    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower() throws IllegalActionException {
        DoubleToken consumptionRate = (DoubleToken) modelData.getVariable("motionPowerRate");
        consumption.send(0, consumptionRate);
    }
    
    private static ArrayType typeLocation = new ArrayType(BaseType.DOUBLE, 2);
}
