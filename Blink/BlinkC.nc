#include "Timer.h"

module BlinkC {
  uses interface Timer<TMilli> as Timer0;
  uses interface Timer<TMilli> as Timer1;
  uses interface Timer<TMilli> as Timer2;
  uses interface Leds;
  uses interface Boot;
}
implementation
{

  uint8_t counter = 0;

  event void Boot.booted()
  {
    call Timer0.startPeriodic( 250 );
    call Timer1.startPeriodic( 500 );
    call Timer2.startPeriodic( 1000 );
  }

  event void Timer0.fired()
  {
    dbg("BlinkC", "Timer 0 fired @ %s.\n", sim_time_string());

    counter++;
    call Leds.set(counter);
  }

  event void Timer1.fired()
  {
    //dbg("BlinkC", "Timer 1 fired @ %s \n", sim_time_string());
    //call Leds.led1Toggle();
  }

  event void Timer2.fired()
  {
    //dbg("BlinkC", "Timer 2 fired @ %s.\n", sim_time_string());
    //call Leds.led2Toggle();
  }
}
