/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

public class Annual_iterations {
	private Calendar current, _cal;
	private Date beginning;
	private Date ending;
	
	//private int _t0S, _t0M, _t0H, _t0D, _t0Mo;
	//private Date OneHourBefEnd,OneDayBefEnd,OneMoBefEnd;
	
	private boolean running;
	private boolean ready;
	private boolean isHourly;
	private boolean isDaily;
//	private boolean isMonthly;
//	private boolean isYearly;

	/*----------------------------------------------------*/
    public Annual_iterations(Date d0, Date d1){
    	
    	_cal = Calendar.getInstance();
    	_cal.setTime(d1);
    	ending = _cal.getTime();
       	_cal.setTime(d0);
    	beginning = _cal.getTime();
    	
    	current = Calendar.getInstance();
    	current.setTime(d0);
    	
        running = false;
        ready = false;
        isHourly = false;
    	isDaily = false;
//    	isMonthly = false;
//    	isYearly = false;
    }
    public void setIniIterations(int ini){
    	current.add(Calendar.MILLISECOND, -ini*ref.MILLISECONDS_IN_timeStep);
    }
    public void reset(){
    	current.setTime(beginning);
    	
        running = false;
        ready = false;
        isHourly = false;
    	isDaily = false;
//    	isMonthly = false;
//    	isYearly = false;
    }
    
    /*----------------------------------------------------*/
    public synchronized void setStart(){
    	if(!running){
    		running=true; 
    		_cal.setTime(beginning);
    		if(current.compareTo(_cal)>=0 ){
                ready=true;
                isHourly = (current.get(Calendar.SECOND)==0) && (current.get(Calendar.MINUTE)==0);
    			isDaily = isHourly && (current.get(Calendar.HOUR_OF_DAY)==0);
            }//
    	}
    }
    public synchronized void Next(){
    	if(running){
    		//increase time by one timeStep;
    		current.add(Calendar.MILLISECOND, ref.MILLISECONDS_IN_timeStep);
    		
    		//checking ready
            if(!ready){
                _cal.setTime(beginning);
                if(current.compareTo(_cal)>=0){ready=true;}
            }
    		//checking ending [beginning, ending]
            if(ready){
                _cal.setTime(ending);
                if(current.compareTo(_cal)>0){ready=false; running=false;}
                //time away from t0 since Next()
                isHourly = (current.get(Calendar.SECOND)==0) && (current.get(Calendar.MINUTE)==0);
    			isDaily = isHourly && (current.get(Calendar.HOUR_OF_DAY)==0);
            }
    		
    		//System.out.println(ref.DefaultTimeFormatToFile.format(current.getTime()) );
    	} 
    }
    public synchronized Date Now(){
    	//return _now;
    	return current.getTime();
    }
    public synchronized int year(){return current.get(Calendar.YEAR);}
    public synchronized boolean isLeap(){return current.get(Calendar.YEAR)%4==0;}
    public synchronized int nthIteration(){
    	return (int)((current.getTimeInMillis()-beginning.getTime())/ref.MILLISECONDS_IN_timeStep);
    }
    
    
    public synchronized boolean isRunning(){return running;}
    public synchronized boolean isLast(){return current.getTime().equals(ending);}
    public synchronized boolean isReady(){
    	if(running) return ready;
    	else return false;
    }
    public synchronized boolean isHourly(){return isHourly;}
    public synchronized boolean isDaily(){return isDaily;}

    
    public String startTime(SimpleDateFormat _format){
    	return _format.format(beginning);
    }
    public String endTime(SimpleDateFormat _format){
    	return _format.format(ending);
    }
    
    
}
