package ajdurant.wsn.actor;

import ajdurant.wsn.lib.WSNActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.RecordToken;
import ptolemy.data.Token;
import ptolemy.data.expr.SingletonParameter;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class MotionController extends WSNActor {

    public MotionController(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        
        // Inputs
        trigger = new TypedIOPort(this, "trigger", true, false);
        
        // Outputs
        nextLoc = new TypedIOPort(this, "nextLoc", false, true);
        nextLoc.setTypeEquals(typeLocation);
        new SingletonParameter(nextLoc, "_showName").setExpression("true");
        
        logger = new TypedIOPort(this, "logger", false, true);
        logger.setTypeEquals(typeRecordMotionLog);
        new SingletonParameter(logger, "_showName").setExpression("true");
    }

    // Inputs
    public TypedIOPort trigger;
    
    // Outputs
    public TypedIOPort nextLoc;
    public TypedIOPort logger;
    
    // Internals
    private DoubleToken totalDistanceTraveled;
    
    @Override
    public void initialize() {
        totalDistanceTraveled = new DoubleToken(0);
    }
    
    @Override
    public void fire() throws IllegalActionException
    {
        super.fire();
        
        if (trigger.getWidth() > 0 && trigger.hasToken(0)) {
            clearToken(trigger);
            
            ArrayToken currentLocation = (ArrayToken) getVariable("currentLocation");
            ArrayToken targetLocation = (ArrayToken) getVariable("targetLocation");
            
            DoubleToken currentX = (DoubleToken) currentLocation.getElement(0);
            DoubleToken currentY = (DoubleToken) currentLocation.getElement(1);
            
            DoubleToken targetX = (DoubleToken) targetLocation.getElement(0);
            DoubleToken targetY = (DoubleToken) targetLocation.getElement(1);
            
            if(checkLocationChangeNeeded(currentX, currentY, targetX, targetY)) {
                setVariable("inMotion", new BooleanToken(true));
                moveTowardTarget(currentX, currentY, targetX, targetY);
                consumePower();
            } else {
                setVariable("inMotion", new BooleanToken(false));
            }
        }
        
    }
    
    private boolean checkLocationChangeNeeded(DoubleToken currentX, DoubleToken currentY, DoubleToken targetX, DoubleToken targetY) throws IllegalActionException {
        
        return (currentX.isCloseTo(targetX, 1.0).and(currentY.isCloseTo(targetY, 1.0))).not().booleanValue();
    }
    
    private void moveTowardTarget(DoubleToken currentX, DoubleToken currentY, DoubleToken targetX, DoubleToken targetY) throws IllegalActionException {
        
        DoubleToken nextX = currentX;
        DoubleToken nextY = currentY;
        
        if (targetX.isLessThan(currentX).booleanValue()) {
            nextX = (DoubleToken) currentX.subtract(DoubleToken.ONE);
        } else if (targetX.isGreaterThan(currentX).booleanValue()) {
            nextX = (DoubleToken) currentX.add(DoubleToken.ONE);
        }
        
        if (targetY.isLessThan(currentY).booleanValue()) {
            nextY = (DoubleToken) currentY.subtract(DoubleToken.ONE);
        } else if (targetY.isGreaterThan(currentY).booleanValue()) {
            nextY = (DoubleToken) currentY.add(DoubleToken.ONE);
        }
        
        ArrayToken nextLocation = new ArrayToken(new Token[] {nextX, nextY});
        
        logTravel(currentX, currentY, nextX, nextY);
        
        nextLoc.send(0, nextLocation);
    }
    
    private void logTravel(DoubleToken currentX, DoubleToken currentY, DoubleToken nextX, DoubleToken nextY) throws IllegalActionException {
        
        DoubleToken distance = new DoubleToken( Math.sqrt( Math.pow((nextX.doubleValue() - currentX.doubleValue()), 2) + Math.pow((nextY.doubleValue() - currentY.doubleValue()), 2) ) );
        
        totalDistanceTraveled = (DoubleToken) totalDistanceTraveled.add(distance);
        
        ArrayToken currentLocation = new ArrayToken(new Token[] {currentX, currentY});
        ArrayToken nextLocation = new ArrayToken(new Token[] {nextX, nextY});
        
        Token[] motionLogTokens = {
                currentLocation,
                nextLocation,
                distance,
                totalDistanceTraveled
        };
        
        RecordToken motionLog = new RecordToken(labelsMotionLog, motionLogTokens);
        
        logger.send(0, motionLog);
    }
    
    /**
     * Send consumption to power module.
     * 
     * @throws IllegalActionException
     */
    protected void consumePower() throws IllegalActionException {
        consumePower("motionPowerRate");
    }
    
    private static String[] labelsMotionLog = {
        "currentLocation",
        "nextLocation",
        "distance",
        "totalDistanceTraveled"
    };
    protected static Type[] typesMotionLog = {
            typeLocation,
            typeLocation,
            BaseType.DOUBLE,
            BaseType.DOUBLE
        };
    protected static RecordType typeRecordMotionLog = new RecordType(labelsMotionLog, typesMotionLog);
    
}
