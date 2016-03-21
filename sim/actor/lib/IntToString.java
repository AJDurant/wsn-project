package ajdurant.wsn.actor.lib;

import ptolemy.actor.lib.conversions.Converter;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class IntToString extends Converter {

	public IntToString(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		input.setTypeEquals(BaseType.INT);
        output.setTypeEquals(BaseType.STRING);
	}
	
    /** Consume one IntToken and generate a StringToken.
    *
    *  @exception IllegalActionException If thrown while getting
    *  or sending a token.
    */
   @Override
   public void fire() throws IllegalActionException {
       super.fire();
       int inputValue = ((IntToken) input.get(0)).intValue();

       String value = Integer.toString(inputValue);

       output.send(0, new StringToken(value));
   }

   /** Return false if the input port has no token, otherwise return
    *  what the superclass returns (presumably true).
    *  @exception IllegalActionException If there is no director.
    */
   @Override
   public boolean prefire() throws IllegalActionException {
       if (!input.hasToken(0)) {
           return false;
       }

       return super.prefire();
   }

}
