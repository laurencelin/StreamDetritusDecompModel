/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.Date;
import java.io.File;

public class subNetwork implements Serializable,Comparable<subNetwork>{
	private static final long serialVersionUID = 20120714L;
	
	/* when this runs, it provides jobs to es in the network*/
    
	List<Segment> es_Segments; //	es_Segments.get(0) = outlet
    int es_NumOfCells;
    private List<Integer> es_AccCell;
    int networkDegree;
    String subName;
    boolean active;
    Segment outseg;
    
    transient int iSample;// <<---- data for calibration
    transient double boundary[][];
    transient double best[][];
    transient double trace[];
    transient int side[];
    transient int NumOfPara;
    
    transient private List<Callable<Exception>> ProcessQ;//
    transient private List<Future<Exception>> SafeCheck;//
    transient private int[][] optimized_simulation_setting;
    transient private int _ith_thread;
    transient private String _nowStr;
    transient private String _nowStrII;
    transient private boolean _nowDaily;
    transient private Date _now;
    transient private Annual_iterations iteration;
    
    /*-------------------------------------------------------------*/
    //initial
    public subNetwork(Segment seg){
    	System.out.printf("Creating subnet %s ...\n",seg.toString());
        
        es_Segments = new ArrayList<Segment>();
    	es_Segments.add(seg);
    	networkDegree = seg.SegDegree;
        subName=es_Segments.get(0).segName;
        active=true;
     }
    public subNetwork(Segment seg, boolean _active){
    	System.out.printf("Creating subnet %s ...\n",seg.toString());
        
        es_Segments = new ArrayList<Segment>();
    	es_Segments.add(seg);
    	networkDegree = seg.SegDegree;
        subName=es_Segments.get(0).segName;
        active=_active;
    }
    public subNetwork(Segment seg, boolean _active, String _dir){
    	System.out.printf("Creating subnet %s ...\n",seg.toString());
        
        es_Segments = new ArrayList<Segment>();
    	es_Segments.add(seg);
    	networkDegree = seg.SegDegree;
        subName=es_Segments.get(0).segName;
        active=_active;
    }
    
    public void makeSubNetwork(){
    	outseg = es_Segments.get(0);
    	es_Segments.addAll(outseg.drainNetwork(networkDegree));//<----------- all segments including normal-mode and outflow-mode; they will be loaded in their mode in run()
    	Collections.sort(es_Segments);
        //---> all OUTFLOW-mode segments will be at the buttom of the array
        //---> where is the outlet segment?
    }
    
    public int degree(){return es_Segments.get(0).SegDegree;}
    public String name(){return es_Segments.get(0).segName;}
    private void optimization(){
    	
    	es_NumOfCells=0;
        es_AccCell = new ArrayList<Integer>();
    	for(Segment v:es_Segments){
    		if(v.tokenBy==networkDegree){
    			es_NumOfCells += v.segLength;//seg.bed.length;
            	es_AccCell.add(es_NumOfCells);
    		}else{break;}
    	}
        
        if(es_NumOfCells<ref.NumOfProcessors){
            System.out.println("--------------------<run-time configuration>");
            System.out.println("total is "+es_NumOfCells);
            
            int jth=0; _ith_thread=0;
            optimized_simulation_setting = new int[ref.NumOfProcessors][4];
            optimized_simulation_setting[_ith_thread][0]=0;//first seg
            optimized_simulation_setting[_ith_thread][1]=0;//first index
            optimized_simulation_setting[_ith_thread][2]=es_AccCell.size()-1;//last seg
            if(es_AccCell.size()==1){ optimized_simulation_setting[_ith_thread][3]=es_AccCell.get(es_AccCell.size()-1)-1; }
            else{ optimized_simulation_setting[_ith_thread][3]=es_AccCell.get(es_AccCell.size()-1)-es_AccCell.get(es_AccCell.size()-2)-1 ;}//last index (included)
            
            _ith_thread=1;
            while(_ith_thread<ref.NumOfProcessors){
                optimized_simulation_setting[_ith_thread][0]=-1;
                optimized_simulation_setting[_ith_thread][1]=-1;
                optimized_simulation_setting[_ith_thread][2]=-1;
                optimized_simulation_setting[_ith_thread][3]=-1;
                _ith_thread++;
            }
            
            
        }else{
            //when es_NumOfCells >= ref.NumOfProcessors
//            int rem = es_NumOfCells%ref.NumOfProcessors;
//            if(rem >0){rem = ref.NumOfProcessors - rem;}
//            System.out.println("--------------------<run-time configuration>");
//            int mu = (int)((es_NumOfCells+rem)/ref.NumOfProcessors);
//            System.out.println("total is "+es_NumOfCells+" and mu = "+mu);
            
            System.out.println("--------------------<run-time configuration>");
            int rem = es_NumOfCells%ref.NumOfProcessors;
            int mu = (int)((1.0*es_NumOfCells)/ref.NumOfProcessors);
            System.out.printf("total is %d and mu = %d and rem = %d\n",es_NumOfCells,mu,rem);

            int jth_segment=0;
            int _ith_thread=0;
            int _ith_threadSize=0;
            int _pth_threadSize=0;
            optimized_simulation_setting = new int[ref.NumOfProcessors][4];
            
            optimized_simulation_setting[_ith_thread][0]=0;
            optimized_simulation_setting[_ith_thread][1]=0;
            _ith_threadSize = mu; if(rem>0){_ith_threadSize++; rem--;}
            _pth_threadSize = 0;
            
            while(jth_segment<es_AccCell.size() && _ith_thread<ref.NumOfProcessors-1){
                
                if(es_AccCell.get(jth_segment) >= _ith_threadSize ){
                    
                    int __tmp=0; if(jth_segment>0) __tmp= es_AccCell.get(jth_segment-1);
                    optimized_simulation_setting[_ith_thread][2] = jth_segment;
                    optimized_simulation_setting[_ith_thread][3] = _ith_threadSize-__tmp-1;
                    
                    _ith_thread++;
                    _pth_threadSize = _ith_threadSize;
                    _ith_threadSize +=mu; if(rem>0){_ith_threadSize++; rem--;}
                    
                    if(es_AccCell.get(jth_segment) == _pth_threadSize){
                        jth_segment++;
                        optimized_simulation_setting[_ith_thread][0]=jth_segment; optimized_simulation_setting[_ith_thread][1]=0;
                    }else{
                        optimized_simulation_setting[_ith_thread][0]=jth_segment; optimized_simulation_setting[_ith_thread][1]=_pth_threadSize-__tmp;
                    }
                    
                }else jth_segment++;
            }//end of while loop
            
            jth_segment=es_AccCell.size()-1;
            int __tmp=0; if(jth_segment>0) __tmp=es_AccCell.get(jth_segment-1);
            optimized_simulation_setting[_ith_thread][2]=jth_segment;
            optimized_simulation_setting[_ith_thread][3]=es_AccCell.get(jth_segment)-1-__tmp;
            
            
//            int jth=0; _ith_thread=0;
//            optimized_simulation_setting = new int[ref.NumOfProcessors][4];
//            optimized_simulation_setting[_ith_thread][0]=0;
//            optimized_simulation_setting[_ith_thread][1]=0;
//            while(jth<es_AccCell.size()&&_ith_thread<ref.NumOfProcessors-1){
//                if(es_AccCell.get(jth) >= mu*(_ith_thread+1) ){
//                    int __last = mu*_ith_thread;
//                    int __tmp=0; if(jth>0) __tmp= es_AccCell.get(jth-1);
//                    optimized_simulation_setting[_ith_thread][2]=jth;
//                    optimized_simulation_setting[_ith_thread][3]=__last+mu-1-__tmp;
//                    _ith_thread++;
//                    if(es_AccCell.get(jth) == mu*_ith_thread){jth++; optimized_simulation_setting[_ith_thread][0]=jth; optimized_simulation_setting[_ith_thread][1]=0;}
//                    else{optimized_simulation_setting[_ith_thread][0]=jth; optimized_simulation_setting[_ith_thread][1]=__last+mu-__tmp;}
//                    
//                }else jth++;
//            }
//            jth=es_AccCell.size()-1;
//            int __tmp=0; if(jth>0) __tmp=es_AccCell.get(jth-1);
//            optimized_simulation_setting[_ith_thread][2]=jth;
//            optimized_simulation_setting[_ith_thread][3]=es_AccCell.get(jth)-1-__tmp;
            
		}
        
        for(int s=0; s<ref.NumOfProcessors; s++){
            
            System.out.printf("optimized_simulation_setting[%d] %d %d %d %d\n",s,
                    optimized_simulation_setting[s][0],optimized_simulation_setting[s][1],
                    optimized_simulation_setting[s][2],optimized_simulation_setting[s][3]);
        }
    }
    private void generateSimulationThreads()throws Exception{
        //ProcessQ = new ArrayList<Callable<Exception>>(ref.NumOfProcessors);
        ProcessQ.clear();
        /* loading callable (with seg run inside) to ProcessQ*/
        for(_ith_thread=0; _ith_thread<ref.NumOfProcessors; _ith_thread++){
            ProcessQ.add(
                         new Callable<Exception>(){
                             int startingSeg=optimized_simulation_setting[_ith_thread][0];
                             int startingSeg_i=optimized_simulation_setting[_ith_thread][1];
                             
                             int endingSeg=optimized_simulation_setting[_ith_thread][2];
                             int endingSeg_i=optimized_simulation_setting[_ith_thread][3];
                             
                             public Exception call(){
                                 try{
                                     if(startingSeg<0 || endingSeg<0 || startingSeg_i<0 || endingSeg_i<0){return null;}
                                     else if(startingSeg == endingSeg)
                                         //startingSeg = endingSeg
                                         es_Segments.get(startingSeg).run(startingSeg_i,endingSeg_i);
                                     //startingSeg < endingSeg
                                     else for(int i=startingSeg; i<endingSeg+1; i++){
                                         if(i==startingSeg) es_Segments.get(i).run(startingSeg_i);//from startingSeg_i to the end
                                         else if(i==endingSeg) es_Segments.get(i).run(0, endingSeg_i);//from beginning to endingSeg_i
                                         else es_Segments.get(i).run();//run the entire stream
                                     }
                                     
                                     //                     			System.out.println("-----------------------------");
                                     //                     			System.out.println("startingSeg = "+startingSeg);
                                     //                        		System.out.println("startingSeg_i = "+startingSeg_i);
                                     //                        		System.out.println("endingSeg = "+endingSeg);
                                     //                        		System.out.println("endingSeg_i = "+endingSeg_i);
                                     
                                     return null;
                                 }catch(Exception e){
                                     return e;
                                 }
                             }//end of call
                         });
        }//end of for loop
    }
    public void load()throws Exception{
        //this loading is to gather infor. on optimization not loading Seg.data
    	makeSubNetwork();
        optimization();
        ProcessQ = new ArrayList<Callable<Exception>>(ref.NumOfProcessors);
        
        if(!active){
            outseg.normMode=false;
            outseg.segOutflowFile = new File("Seg."+outseg.segName+ref.loadedOutIndex+".outflow");//
        }
        
    }
    public void unload()throws Exception{
    	//all runs are done.
        ProcessQ.clear();//<<--------- *clearing is less than new array <<necessary?>>
        ProcessQ=null;
    }
    
    //"run" --> "simulation"; run(){1. check fitTF -> calibration; 2. make simulation}
    public void run()throws Exception{
        System.out.println(this.name()+" is running simulation");
        generateSimulationThreads();
        simulation();
    }
    public void simulation()throws Exception{
    	try{
    		//set up timer or reset clock
    		iteration = new Annual_iterations(ref.STARTDATE,ref.ENDDATE);
    		iteration.setStart();
        	
            //----------------------------------------------------
    		//1) load all segments (including outflow mode) and 2) set files
            for(Segment v:es_Segments){
                v.initiateLoad();
                v.processOn=true;
                if(ref.printAllSeg && v.normMode){v.setOutput();}
            }
            if(!ref.printAllSeg){outseg.setOutput();}
        	outseg.setOutflow();
            System.out.println("starting the time loop...");
            //----------------------------------------------------
            
            //Core execution
            while(iteration.isRunning()){
                //still preparing
            	_now = iteration.Now();
            	_nowStr = ref.DefaultTimeFormatToFileII.format(_now);
            	_nowDaily = iteration.isDaily();
            
                //ref.timeseries....
                ref.DefaultFallTime.searchIndex(_now);
                ref.DefaultBlowTime.searchIndex(_now);
                ref.DefaultTempTime.searchIndex(_now);
                
            	for(Segment v:es_Segments){
            		v._isDaily=_nowDaily;
            		v._strTime=_nowStr;
                    v.reloadTimeSeries(_now);//for all segments
            	}
            	
                //actual processing from present to next time
        		SafeCheck = ref.es.invokeAll(ProcessQ);// ** only the normal segments are processed. 
        		for(Future<Exception> v: SafeCheck){ if(v.get()!=null){throw v.get();}}
        		outseg.toOUTFLOWbuffer();//----->write .outflow
                
                for(Segment v:es_Segments){v.flowcellSwap();}
                
                    //set the clock to next time
                iteration.Next();
                //stream network is on its next stage
                               
            }//end of while loop
            
            //----------------------------------------------------
            if(ref.binary) for(Segment v:es_Segments){v.unloadSave();}
            else for(Segment v:es_Segments){v.unload();}
            
        }catch(Exception e){
            throw new Exception("subNetwork "+es_Segments.get(0).segName+" (iteration="+iteration.Now()+")::"+e);
        }
    }
    
  
    public int compareTo(subNetwork sub){
    	if(this.networkDegree > sub.networkDegree){return 1;}
    	else if(this.networkDegree < sub.networkDegree){return -1;}
    	else {return 0;}

    }
    public String toString(){
        String _tmp="";
        int subid = es_Segments.get(0).segHillID;
        int subpro = es_Segments.get(0).tokenBy;
        //subnet segment order degree taken outlet segid hillid
        for(Segment v:es_Segments){
            _tmp+=subName+" "+subpro+" "+v.toString()+" "+subid+"\n";
        }
        
        
//    	String _tmp="<subNetwork - "+es_Segments.get(0).segName+">("+networkDegree+"-deg)\n";
//    	for(Segment v:es_Segments) 
//    		_tmp+=v.toString(true)+"\n";
    	return _tmp;
    }
    
    
}//end of main class
