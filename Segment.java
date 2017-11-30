
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.StringTokenizer;

public class Segment implements Serializable, Comparable<Segment>{
	private static final long serialVersionUID = 20120714L;
	
	//---------------------------- remain
	String segName;
    int segHillID;
    int SegOrder;
    int SegDegree;
    int tokenBy;
    int segLength;
    List<Segment> upstreams;//(pointers)
    boolean outlet;//for toString()
    boolean normMode;
    boolean Toutput;
    boolean Toutflow; //segment has file.outflow -> outflow mode in next load/run
    
    //---------------------------- for binary file
    File segFile; 
    transient SegmentCell seg;//(standing stock of stage variables)
    
    //---------------------------- for both modes
    transient double InputBuffer[][];//InputBuffer[1][7]=normal run; InputBuffer[?][11]=outflow run;
    transient int N;//just a counter
    transient int ENDINDEX;//number of rows in "InputBuffer"
    transient boolean processOn;
    transient boolean _isDaily;
    transient String _strTime;
    transient private FlowCell toDownStream;
    //---------------------------- for normal run
    transient private FlowCell fromUpStreams;
    transient private double additionalC[][];
    transient private DataOutputStream toOUTFLOW;
    transient private Writer toOUTPUT;
    //---------------------------- for outflow run
    File segOutflowFile;
    transient DataInputStream segOutflowStream;
    
    //--------------------------------------------------------------------
    //--------------------------------------------------------------------
    //--------------------------------------------------------------------
	
    public Segment(String name) throws Exception{
		segName=name;
        StringTokenizer _token_ = new StringTokenizer(name,"-");
        segHillID = Integer.parseInt(_token_.nextToken());
        
		SegOrder=0;
		SegDegree=0;
		tokenBy=0;
        segLength=0;
        outlet=false;
        normMode=true;
        Toutput=false;
        Toutflow=false;
		
		upstreams =new ArrayList<Segment>();
		fromUpStreams = new FlowCell();
        additionalC = new double[3][5];
		_strTime="";
		_isDaily=false;
		
		processOn=false;
		
		toOUTPUT=null;
		segOutflowFile=null;
		toOUTFLOW=null;
		
		seg=null;
		segFile=null;
		
		//segInputFile=null;
		InputBuffer=null;
	}
    public Segment(String name, boolean _outlet) throws Exception{
		segName=name;
        StringTokenizer _token_ = new StringTokenizer(name,"-");
        segHillID = Integer.parseInt(_token_.nextToken());
        
		SegOrder=0;
		SegDegree=0;
		tokenBy=0;
        segLength=0;
        outlet=_outlet;
        normMode=true;
        Toutput=false;
        Toutflow=false;
		
		upstreams =new ArrayList<Segment>();
		fromUpStreams = new FlowCell();
        additionalC = new double[3][5];
		_strTime="";
		_isDaily=false;
		
		processOn=false;
		
		toOUTPUT=null;
		segOutflowFile=null;
		toOUTFLOW=null;
		
		seg=null;
		segFile=null;
		
		//segInputFile=null;
		InputBuffer=null;
	}
	public void resumefromBinary(File _segfile) throws Exception{
        segFile = _segfile;
        ObjectInputStream segStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(segFile)));
        seg = (SegmentCell)segStream.readObject();
        segStream.close();
        segLength = seg.bed.length;
        //seg=null;
    }
    public void dumpSeg(){
        seg=null;
    }
    
    //-----------------------------  output functions
    public boolean setOutput() throws Exception{
		toOUTPUT = new BufferedWriter(new FileWriter("Seg."+segName+ref.customizedIndex+"."+ref.strDefSTART+"_"+ref.strDefEND+".csv"));//buffer
        toOUTPUT.write(ref.title+"\n");
        toOUTPUT.write(ref.titleUnit+"\n");
		Toutput=true;
		return true;
	}
	private synchronized void toBuff(String str) throws Exception{
    	toOUTPUT.write(str+"\n");
    }
    public boolean setOutflow() throws Exception{
		toOUTFLOW = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("Seg."+segName+ref.customizedIndex+"."+ref.strDefSTART+"_"+ref.strDefEND+".outflow")));
        Toutflow=true;
		return true;
	}
    public synchronized void toOUTFLOWbuffer() throws Exception{
        // "!seg.Flow_switch" is needed. 
    	if(!seg.Flow_switch){toDownStream=seg.bed[seg.bed.length-1].flow;}//<----
    	else{toDownStream=seg.bed[seg.bed.length-1]._flow;}
        
		toOUTFLOW.writeDouble(toDownStream.vol);
		//[0:DO, 1:NH4, 2:NO3, 3:PO4, 4:DOC, 5:DON, 6:DOP]
		toOUTFLOW.writeDouble(toDownStream.dissolved[0]);
		toOUTFLOW.writeDouble(toDownStream.dissolved[1]);
		toOUTFLOW.writeDouble(toDownStream.dissolved[2]);
		toOUTFLOW.writeDouble(toDownStream.dissolved[3]);
		toOUTFLOW.writeDouble(toDownStream.dissolved[4]);
		toOUTFLOW.writeDouble(toDownStream.dissolved[5]);
		toOUTFLOW.writeDouble(toDownStream.dissolved[6]);
		
		//[0:sestonC, 1:sestonN, 2:sestonP]
		toOUTFLOW.writeDouble(toDownStream.nondissolved[0]);
		toOUTFLOW.writeDouble(toDownStream.nondissolved[1]);
		toOUTFLOW.writeDouble(toDownStream.nondissolved[2]);
    }
    
    //-----------------------------  load functions (frequently used in both modes)
    private boolean loadOUTFLOW() throws Exception{
		//this function will be called daily
		if(!normMode){
			try{
				ENDINDEX=0;
				for(int i=0; i<ref.timeStep_IN_DAY; i++){
					InputBuffer[i][0]=segOutflowStream.readDouble();//flow
					
					InputBuffer[i][1]=segOutflowStream.readDouble();//DO
					InputBuffer[i][2]=segOutflowStream.readDouble();//NH4
					InputBuffer[i][3]=segOutflowStream.readDouble();//NO3
					InputBuffer[i][4]=segOutflowStream.readDouble();//PO4
					InputBuffer[i][5]=segOutflowStream.readDouble();//DOC
					InputBuffer[i][6]=segOutflowStream.readDouble();//DON
					InputBuffer[i][7]=segOutflowStream.readDouble();//DOP
					
					InputBuffer[i][8]=segOutflowStream.readDouble();//sestonC
					InputBuffer[i][9]=segOutflowStream.readDouble();//sestonN
					InputBuffer[i][10]=segOutflowStream.readDouble();//sestonP
                    ENDINDEX++;
				}
                //System.out.println(segName+" load segOUTFLOWStream at"+_strTime);
			}catch(EOFException e){
                //it could get error when the last day is not a day long
                System.out.println(segName+" got error in reading outflow file("+_strTime+"). "+e);
				segOutflowStream.close();
				segOutflowStream=null;
				//System.out.printf("%s got segOutflowStream closed [%d] at %s\n",segName, ENDINDEX, _strTime);
				return false;
			}
			return true;
		}
		else{return false;}
	}
    public void initiateLoad() throws Exception{
		processOn=false;
        _strTime="";
        _isDaily=false;
        N=-1;
        InputBuffer=null;
        toDownStream = new FlowCell();
        
        if(normMode){
            //------------- normal-run
            ObjectInputStream segStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(segFile)));
            seg = (SegmentCell)segStream.readObject();
            segStream.close();
            InputBuffer = new double[1][18];
            /*{
             0:SpringQ 1:springNH4 2:SpringNO3 3:springPO4
             4:qLQ 5:qLNH4 6:qLNO3 7:qLPO4 
             8:CBOM 9:immGR 10:minGR 11:mR 
             12:nitri 13:dnitri
             14,15 spring DOC DON
             16,17 lateral DOC DON
             }*/
        }else{
            //------------- outflow-run
			segOutflowStream = new DataInputStream(new BufferedInputStream(new FileInputStream(segOutflowFile)));
            InputBuffer = new double[ref.timeStep_IN_DAY][11];
			loadOUTFLOW();
        }
        
	}
    public boolean reloadTimeSeries(Date t)throws Exception{
        //always: upstream, regressions(fall,spring,qL)
        if(normMode){
            fromUpStreams.setZero();
            for(Segment v: upstreams){fromUpStreams.add_(v.outFlow());}

            /*{
             0:SpringQ 1:springNH4 2:SpringNO3 3:springPO4
             4:qLQ 5:qLNH4 6:qLNO3 7:qLPO4
             8:CBOM 9:immGR 10:minGR 11:mR
             12:nitri 13:dnitri
             }*/
            seg.timeQN.searchIndex(t); //[1461]
            InputBuffer[0][0]=seg.springQ.at(seg.timeQN);
            InputBuffer[0][1]=seg.springNH4.at(seg.timeQN);
            InputBuffer[0][2]=seg.springNO3.at(seg.timeQN);
            InputBuffer[0][3]=seg.springPO4.at(seg.timeQN);
            InputBuffer[0][14]=seg.springDOC.at(seg.timeQN);
            InputBuffer[0][15]=seg.springDON.at(seg.timeQN);
            
            InputBuffer[0][4]=seg.qLQ.at(seg.timeQN);
            InputBuffer[0][5]=seg.qLNH4.at(seg.timeQN);
            InputBuffer[0][6]=seg.qLNO3.at(seg.timeQN);
            InputBuffer[0][7]=seg.qLPO4.at(seg.timeQN);
            InputBuffer[0][16]=seg.qLDOC.at(seg.timeQN);
            InputBuffer[0][17]=seg.qLDON.at(seg.timeQN);
            
            if(processOn){
                //CBOM input
                InputBuffer[0][8]=ref.DefaultFallProp.at(ref.DefaultFallTime);
                InputBuffer[0][8]+=ref.DefaultBlowProp.at(ref.DefaultBlowTime);
                seg.leaftype.toAll(InputBuffer[0][8],additionalC);
                
                //microbe growth rate
                InputBuffer[0][9]=ref.DefaultQtemp.at(ref.DefaultTempTime);
                InputBuffer[0][10]=InputBuffer[0][9];
                InputBuffer[0][11]=InputBuffer[0][9];
                InputBuffer[0][12]=InputBuffer[0][9];
                InputBuffer[0][13]=InputBuffer[0][9];
                
                InputBuffer[0][9]*=ref.immobilizer_growth;//immoGR
                InputBuffer[0][10]*=ref.miner_growth;//mineGR
                InputBuffer[0][11]*=ref.mResp;//mR [should it be ref or seg?]
                InputBuffer[0][12]*=ref.DefaultSegPara[6];//nitri
                //InputBuffer[0][13]*=ref.DefaultSegPara[7];//dnitri
                //InputBuffer[0][13]*=1.770591;
            }
        }
        else if(processOn){
            //ENDINDEX = number of rows
            N++;//--->time step shifted by one because N.ini=0 and this reload function is call before "process", right! However, when processOn=F, N=-1 --> out of array
            if(N>=ENDINDEX){
                loadOUTFLOW();
                N=0;
            }
        }
        return true;
    }
    public void flowcellSwap(){
        if(normMode){seg.Flow_switch = !seg.Flow_switch;}
    }

    //-----------------------------  unload functions (frequently used in both modes)
    //unload <> dead end, but more like reset for next load in normal-run
    public void makeSegBinary()throws Exception{
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("Seg."+segName+".binary"+ref.customizedIndex)));
    	oos.writeObject(seg);
    	oos.close();
    	seg=null;
        segFile=new File("Seg."+segName+".binary"+ref.customizedIndex);
    }
    public void unload() throws Exception{
    	if(normMode){
            seg=null;
            segFile=null;
            Toutflow=false;
            Toutput=false;
            
            //unload toOUTPUT
            if(toOUTPUT!=null){
                toOUTPUT.flush();
                toOUTPUT.close();
            }
            //unload toOUTFLOW
            if(toOUTFLOW!=null){
                toOUTFLOW.flush();
                toOUTFLOW.close();
                segOutflowFile = new File("Seg."+segName+ref.customizedIndex+"."+ref.strDefSTART+"_"+ref.strDefEND+".outflow");
                normMode=false;
            }

        }else{
            //unload outflow streambuffer
            InputBuffer=null;
            if(segOutflowStream!=null){segOutflowStream.close(); segOutflowStream=null;}
            //outflow file pointer is still there. 
        }
    }
    public void unloadSave() throws Exception{
    	if(normMode){
            //unload the current segCell to file as binary with time tag
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("Seg."+segName+".binary"+ref.customizedIndex+"."+ref.strDefEND)));
            oos.writeObject(seg);
            oos.close();
            
            seg=null;
            segFile=null;
            Toutflow=false;
            Toutput=false;
            
            //unload toOUTPUT
            if(toOUTPUT!=null){
                toOUTPUT.flush();
                toOUTPUT.close();
            }
            //unload toOUTFLOW
            if(toOUTFLOW!=null){
                toOUTFLOW.flush();
                toOUTFLOW.close();
                segOutflowFile = new File("Seg."+segName+ref.customizedIndex+"."+ref.strDefSTART+"_"+ref.strDefEND+".outflow");
                normMode=false;
            }
        }
        else{
            InputBuffer=null;
            if(segOutflowStream!=null){segOutflowStream.close(); segOutflowStream=null;}
        }
    }
    
    //-----------------------------  run functions
    public FlowCell outFlow() throws Exception{
		if(normMode){
			//seg is loaded
			if(seg.Flow_switch) return seg.bed[seg.bed.length-1].flow;//need !?
	    	else return seg.bed[seg.bed.length-1]._flow;
		}else if(N<0){
            //when processOn=F; "N" here is for countdown from initial period.
			toDownStream.vol=InputBuffer[0][0];//flow
			
			toDownStream.dissolved[0]=InputBuffer[0][1];//DO
			toDownStream.dissolved[1]=InputBuffer[0][2];//NH4
			toDownStream.dissolved[2]=InputBuffer[0][3];//NO3
			toDownStream.dissolved[3]=InputBuffer[0][4];//PO4
			toDownStream.dissolved[4]=InputBuffer[0][5];//DOC
			toDownStream.dissolved[5]=InputBuffer[0][6];//DON
			toDownStream.dissolved[6]=InputBuffer[0][7];//DOP
			
			toDownStream.nondissolved[0]=InputBuffer[0][8];//sestonC
			toDownStream.nondissolved[1]=InputBuffer[0][9];//sestonN
			toDownStream.nondissolved[2]=InputBuffer[0][10];//sestonP
			return toDownStream;
		}else{
            //when N >=0
            //read in the saved timeseries
			toDownStream.vol=InputBuffer[N][0];//flow
			
			toDownStream.dissolved[0]=InputBuffer[N][1];//DO
			toDownStream.dissolved[1]=InputBuffer[N][2];//NO3
			toDownStream.dissolved[2]=InputBuffer[N][3];//NH4
			toDownStream.dissolved[3]=InputBuffer[N][4];//PO4
			toDownStream.dissolved[4]=InputBuffer[N][5];//DOC
			toDownStream.dissolved[5]=InputBuffer[N][6];//DON
			toDownStream.dissolved[6]=InputBuffer[N][7];//DOP
			
			toDownStream.nondissolved[0]=InputBuffer[N][8];//sestonC
			toDownStream.nondissolved[1]=InputBuffer[N][9];//sestonC
			toDownStream.nondissolved[2]=InputBuffer[N][10];//sestonC
			return toDownStream;
        }
	}
    
    //simulation run, generate output files
	public boolean run() throws Exception{
		return run(0,seg.bed.length-1);
	}
	public boolean run(int _spt) throws Exception{
		return run(_spt,seg.bed.length-1);
	}
	public boolean run(int _spt, int _ept) throws Exception{
        if(_spt<0 || _ept<0) return true;
        
        FlowCell presentflow,presentUpflow,nextflow;
        double lateralinflow;
        double vol_1;
        double nh2no;
        double rea;
        double toGW;
        double toFlow;
        double r;
        double dc;
        double toFBOMc,toFBOMn,toFBOMp;
        double toFBON_mic,toFBOP_mic;
        
		//true -> ref to flow ; false -> ref to _flow
        for(int j=_spt; j<=_ept; j++){
        	//true -> ref to flow ; false -> ref to _flow
        	if(seg.Flow_switch){presentflow = seg.bed[j].flow; nextflow=seg.bed[j]._flow;}
        	else{presentflow = seg.bed[j]._flow; nextflow=seg.bed[j].flow; }
        	
        	//--------------------<< sample at now before the next time step; sample every cell>>
            if(processOn && _isDaily && Toutput && (ref.printAllCell||j==segLength-1)){
                toBuff(_strTime+","+seg.bed[j].toString()+","+ presentflow.toString());
            }
            
        	
            //--------------------<< transport with inflow--spatial >>
            if(j>0){
            	//middle of channel / downstream
            	if(seg.Flow_switch){presentUpflow = seg.bed[j-1].flow;}else{presentUpflow = seg.bed[j-1]._flow;}
            	nextflow.equal_(presentUpflow);
                
                lateralinflow = (seg.bed[j].xlocation-seg.bed[j-1].xlocation);
                nextflow.vol += lateralinflow*InputBuffer[0][4]; // water
                
                // no DO inflow
                
                //NH4 inflow. (flux)
                nextflow.dissolved[1] += lateralinflow*InputBuffer[0][5];
                //nitrate inflow
                nextflow.dissolved[2] += lateralinflow*InputBuffer[0][6]; //nitrate inflow source[1]
                //PO4 inflow.
                nextflow.dissolved[3] += lateralinflow*InputBuffer[0][7]; // phosphate
                //DOC inflow.
                nextflow.dissolved[4] += lateralinflow*InputBuffer[0][16]; // phosphate
                //DON inflow.
                nextflow.dissolved[5] += lateralinflow*InputBuffer[0][17]; // phosphate
                
                /*{
                 0:SpringQ 1:springNH4 2:SpringNO3 3:springPO4
                 4:qLQ 5:qLNH4 6:qLNO3 7:qLPO4
                 8:CBOM 9:immGR 10:minGR 11:mR
                 12:nitri 13:dnitri
                 14,15 spring DOC DON
                 16,17 lateral DOC DON
                 }*/
            }
            else{
            	if(SegOrder==1){
            		//spring
                    nextflow.setZero();
                    
                    nextflow.vol = InputBuffer[0][0];//volume
                    nextflow.dissolved[0] = ref.DefaultSegPara[4]*nextflow.vol;//100% DO
                    nextflow.dissolved[1] = InputBuffer[0][1];//ammonium
                    nextflow.dissolved[2] = InputBuffer[0][2];//nitrate
                    nextflow.dissolved[3] = InputBuffer[0][3];//phosphate
                    nextflow.dissolved[4] = InputBuffer[0][14];//DOC
                    nextflow.dissolved[5] = InputBuffer[0][15];//DON
                    
                    lateralinflow =  seg.bed[j].xlocation;
                    nextflow.vol += lateralinflow*InputBuffer[0][4]; //flow
                    nextflow.dissolved[1] += lateralinflow*InputBuffer[0][5];// NH4
                    nextflow.dissolved[2] += lateralinflow*InputBuffer[0][6];//6 NO3
                    nextflow.dissolved[3] += lateralinflow*InputBuffer[0][7];//7 PO4
                    nextflow.dissolved[4] += lateralinflow*InputBuffer[0][16];//7 DOC
                    nextflow.dissolved[5] += lateralinflow*InputBuffer[0][17];//7 DON
            	}
                else{
            		//upstream
            		nextflow.equal_(fromUpStreams,true);//call the seg reset equal_
                    
                    lateralinflow =  seg.bed[j].xlocation;
                    nextflow.vol += lateralinflow*InputBuffer[0][4]; //flow
                    nextflow.dissolved[1] += lateralinflow*InputBuffer[0][5];// NH4
                    nextflow.dissolved[2] += lateralinflow*InputBuffer[0][6];//6 NO3
                    nextflow.dissolved[3] += lateralinflow*InputBuffer[0][7];//7 PO4
                    nextflow.dissolved[4] += lateralinflow*InputBuffer[0][16];//7 DOC
                    nextflow.dissolved[5] += lateralinflow*InputBuffer[0][17];//7 DON
                }
            }
            //--------------------<< update water depth >>
            seg.bed[j].depth = nextflow.vol;
            seg.bed[j].depth *= seg.bed[j].benthicArea_1;
            
            //--------------------<< storage exchange >> //nextflow.vol>0
            if(nextflow.vol>0){
                vol_1 = ref.ONE/nextflow.vol;
                //[0:DO, 1:NH4, 2:NO3, 3:PO4, 4:DOC, 5:DON, 6:DOP]
                
                /* DO */
                if(seg.bed[j].GWdissolved[0]>0 || nextflow.dissolved[0]>0){
                    //from water column
                    toGW = ref.DefaultSegPara[8]*nextflow.dissolved[0];//*vol_1;
                    nextflow.dissolved[0] -= toGW; toGW*=vol_1; toGW*=ref.DefaultSegPara[11];
                    //from storage
                    toFlow = ref.DefaultSegPara[9]*seg.bed[j].GWdissolved[0];//*nextflow.vol;
                    seg.bed[j].GWdissolved[0] -= toFlow; toFlow*=nextflow.vol; toFlow*=ref.DefaultSegPara[12];
                    
                    seg.bed[j].GWdissolved[0]+=toGW;
                    nextflow.dissolved[0]+=toFlow;
                    
                }
                
                /* NH4 */
                if(seg.bed[j].GWdissolved[1]>0 || nextflow.dissolved[1]>0){
                    
                    //substracting
                    toGW = ref.DefaultSegPara[8]*nextflow.dissolved[1];//*vol_1;
                    nextflow.dissolved[1] -= toGW; toGW*=vol_1; toGW*=ref.DefaultSegPara[11];
                    
                    toFlow = ref.DefaultSegPara[9]*seg.bed[j].GWdissolved[1];//*nextflow.vol;
                    seg.bed[j].GWdissolved[1] -= toFlow; toFlow*=nextflow.vol; toFlow*=ref.DefaultSegPara[12];
                    
                    seg.bed[j].GWdissolved[1]+=toGW;
                    nextflow.dissolved[1]+=toFlow;
                }
                
                /* NO3 */
                if(seg.bed[j].GWdissolved[2]>0 || nextflow.dissolved[2]>0){
                    
                    //substracting
                    toGW = ref.DefaultSegPara[8]*nextflow.dissolved[2];//*vol_1;
                    nextflow.dissolved[2] -= toGW; toGW*=vol_1; toGW*=ref.DefaultSegPara[11];
                
                    toFlow = ref.DefaultSegPara[9]*seg.bed[j].GWdissolved[2];//*nextflow.vol;
                    seg.bed[j].GWdissolved[2] -= toFlow; toFlow*=nextflow.vol; toFlow*=ref.DefaultSegPara[12];
                    
                    seg.bed[j].GWdissolved[2]+=toGW;
                    nextflow.dissolved[2]+=toFlow;
                }
                
                /* PO4 */
                if(seg.bed[j].GWdissolved[3]>0 || nextflow.dissolved[3]>0){
                    
                    toGW = ref.DefaultSegPara[8]*nextflow.dissolved[3];//*vol_1;
                    nextflow.dissolved[3] -= toGW; toGW*=vol_1; toGW*=ref.DefaultSegPara[11];
                    
                    toFlow = ref.DefaultSegPara[9]*seg.bed[j].GWdissolved[3];//*nextflow.vol;
                    seg.bed[j].GWdissolved[3] -= toFlow; toFlow*=nextflow.vol; toFlow*=ref.DefaultSegPara[12];
                    
                    seg.bed[j].GWdissolved[3]+=toGW;
                    nextflow.dissolved[3]+=toFlow;
                }
                
                /* DOC */
                if(seg.bed[j].GWdissolved[4]>0 || nextflow.dissolved[4]>0){
                    
                    toGW = ref.DefaultSegPara[8]*nextflow.dissolved[4];//*vol_1;
                    nextflow.dissolved[4] -= toGW; toGW*=vol_1; toGW*=ref.DefaultSegPara[11];
                    
                    toFlow = ref.DefaultSegPara[9]*seg.bed[j].GWdissolved[4];//*nextflow.vol;
                    seg.bed[j].GWdissolved[4] -= toFlow; toFlow*=nextflow.vol; toFlow*=ref.DefaultSegPara[12];
                    
                    seg.bed[j].GWdissolved[4]+=toGW;
                    nextflow.dissolved[4]+=toFlow;
                }
                /* DON */
                if(seg.bed[j].GWdissolved[5]>0 || nextflow.dissolved[5]>0){
                    
                    toGW = ref.DefaultSegPara[8]*nextflow.dissolved[5];//*vol_1;
                    nextflow.dissolved[5] -= toGW; toGW*=vol_1; toGW*=ref.DefaultSegPara[11];
                    
                    toFlow = ref.DefaultSegPara[9]*seg.bed[j].GWdissolved[5];//*nextflow.vol;
                    seg.bed[j].GWdissolved[5] -= toFlow; toFlow*=nextflow.vol; toFlow*=ref.DefaultSegPara[12];
                    
                    seg.bed[j].GWdissolved[5]+=toGW;
                    nextflow.dissolved[5]+=toFlow;
                }
                /* DOP */
                if(seg.bed[j].GWdissolved[6]>0 || nextflow.dissolved[6]>0){
                    
                    toGW = ref.DefaultSegPara[8]*nextflow.dissolved[6];//*vol_1;
                    nextflow.dissolved[6] -= toGW; toGW*=vol_1; toGW*=ref.DefaultSegPara[11];
                    
                    toFlow = ref.DefaultSegPara[9]*seg.bed[j].GWdissolved[6];//*nextflow.vol;
                    seg.bed[j].GWdissolved[6] -= toFlow; toFlow*=nextflow.vol; toFlow*=ref.DefaultSegPara[12];
                    
                    seg.bed[j].GWdissolved[6]+=toGW;
                    nextflow.dissolved[6]+=toFlow;
                    
                }
            }
            
            //--------------------<< rearation >>
            rea = ref.DefaultSegPara[5]*(ref.DefaultSegPara[4]-nextflow.DO());//concentration
            nextflow.dissolved[0]+=rea*nextflow.vol;
            
            //working below!!!
            if(processOn){
                //--------------------<< CBOM leaffall and microbe+ >>
                seg.bed[j].addOMDensity(additionalC);
                
            	//--------------------<< FBOM rise and seston deposit >>
                if(ref.Core_Deposition && ref.Core_Entrainment){
                	seg.bed[j].fomUpDown(nextflow, ref.DefaultSegPara[2],ref.DefaultSegPara[3] );
                }
                else if(ref.Core_Deposition){
                	seg.bed[j].fomUpDown(nextflow, 0.0,ref.DefaultSegPara[3] );
                }
                else if(ref.Core_Entrainment){
                	seg.bed[j].fomUpDown(nextflow, ref.DefaultSegPara[2],0.0 );
                }
                
                
                //--------------------<< CBOM leaching >>
                if(ref.Core_leaching){seg.bed[j].leachOutTo(nextflow,ref.DefaultSegPara[10]);}
                //--------------------<< DOC photo-decay >>
//                nh2no = nextflow.dissolved[4]*InputBuffer[0][12];//decay rate from cmd line
//                nextflow.dissolved[1]-=nh2no;
//                nextflow.dissolved[2]+=nh2no;
//                nh2no = nextflow.dissolved[5]*InputBuffer[0][12];//decay rate from cmd line
//                nh2no = nextflow.dissolved[6]*InputBuffer[0][12];//decay rate from cmd line
                
                //--------------------<< CBOM microbial-decomposition >>
                if(ref.Core_Decomp){
                	switch(ref.immobilizer_mode){
                        case 1: seg.bed[j].immobilizer.ImmobilizeI(InputBuffer[0][9],InputBuffer[0][11],seg.bed[j],nextflow);
                			break;
                        case 2:seg.bed[j].immobilizer.ImmobilizeII(InputBuffer[0][9],InputBuffer[0][11],seg.bed[j],nextflow);
                			break;
                        case 3:seg.bed[j].immobilizer.ImmobilizeIII(InputBuffer[0][9],InputBuffer[0][11],seg.bed[j],nextflow);
                			break;
                        case 4:seg.bed[j].immobilizer.ImmobilizeIII_noDOC(InputBuffer[0][9],InputBuffer[0][11],seg.bed[j],nextflow);
                            break;
                        default: break;
                	}
                	switch(ref.miner_mode){
                        case 1: seg.bed[j].miner.MiningI(InputBuffer[0][10],InputBuffer[0][11],seg.bed[j],nextflow);
                			break;
                        case 2:seg.bed[j].miner.MiningII(InputBuffer[0][10],InputBuffer[0][11],seg.bed[j],nextflow);
                			break;
                        case 3:seg.bed[j].miner.MiningIII(InputBuffer[0][10],InputBuffer[0][11],seg.bed[j],nextflow);
                			break;
                        case 4:seg.bed[j].miner.MiningIII_noDOC(InputBuffer[0][10],InputBuffer[0][11],seg.bed[j],nextflow);
                            break;
                        default: break;
                	}
                }
                
                //--------------------<< CBOM macro-invertebrate comsuption and fragmentation >>
                if(ref.Core_FragementMac){
                    r = 1-ref.DefaultSegPara[1];
                	dc = ref.fbomRemainCoef*ref.DefaultSegPara[1];
                    //double toFBON_mic,toFBOP_mic;
                    
                    //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
                    toFBOMc = seg.bed[j].SCOM[0][0]; seg.bed[j].SCOM[0][0]*=r;
                    toFBOMc += seg.bed[j].SCOM[0][1]; seg.bed[j].SCOM[0][1]*=r;//dead
                    toFBOMc += seg.bed[j].SCOM[0][2]; seg.bed[j].SCOM[0][2]*=r;
                    toFBOMc += seg.bed[j].SCOM[0][3]; seg.bed[j].SCOM[0][3]*=r;
                    toFBOMc += seg.bed[j].SCOM[0][4]; seg.bed[j].SCOM[0][4]*=r;
                    toFBOMc *= dc;
                    
                    toFBOMn = seg.bed[j].SCOM[1][0]; seg.bed[j].SCOM[1][0]*=r;
                    toFBOMn += seg.bed[j].SCOM[1][1]; seg.bed[j].SCOM[1][1]*=r;//dead
                    toFBOMn += seg.bed[j].SCOM[1][2]; seg.bed[j].SCOM[1][2]*=r;
                    toFBOMn += seg.bed[j].SCOM[1][3]; seg.bed[j].SCOM[1][3]*=r;
                    toFBOMn += seg.bed[j].SCOM[1][4]; seg.bed[j].SCOM[1][4]*=r;
                    toFBOMn *= dc;
                    
                    toFBOMp = seg.bed[j].SCOM[2][0]; seg.bed[j].SCOM[2][0]*=r;
                    toFBOMp += seg.bed[j].SCOM[2][1]; seg.bed[j].SCOM[2][1]*=r;//dead
                    toFBOMp += seg.bed[j].SCOM[2][2]; seg.bed[j].SCOM[2][2]*=r;
                    toFBOMp += seg.bed[j].SCOM[2][3]; seg.bed[j].SCOM[2][3]*=r;
                    toFBOMp += seg.bed[j].SCOM[2][4]; seg.bed[j].SCOM[2][4]*=r;
                    toFBOMp *= dc;
                    
                        //adding ...
                    seg.bed[j].fbom[0] += toFBOMc;
                	seg.bed[j].fbom[1] += toFBOMn;
                	seg.bed[j].fbom[2] += toFBOMp;
                        
                        //add the microbes
                	dc=ref.fbomRemainCoef*seg.bed[j].immobilizer.c*ref.DefaultSegPara[1];
                	toFBON_mic = dc*seg.bed[j].immobilizer.nc;
                    toFBOP_mic = dc*seg.bed[j].immobilizer.pc;
                	seg.bed[j].fbom[0] += dc;
                    seg.bed[j].fbom[1] += toFBON_mic;
                	seg.bed[j].fbom[2] += toFBOP_mic;
                    seg.bed[j].immobilizer.c*=r;//r is defined above
                    
                	
                	dc=ref.fbomRemainCoef*seg.bed[j].miner.c*ref.DefaultSegPara[1];
                	toFBON_mic += dc*seg.bed[j].miner.nc;
                	toFBOP_mic += dc*seg.bed[j].miner.pc;
                	seg.bed[j].fbom[0] += dc;
                    seg.bed[j].fbom[1] += toFBON_mic;
                	seg.bed[j].fbom[2] += toFBOP_mic;
                    seg.bed[j].miner.c*=r;//r is defined above
                    
                }
                
                //--------------------<< Nitri and Denitri processes in the water column >>
                if(ref.Core_Nitrification){
                    nh2no = nextflow.dissolved[1]*InputBuffer[0][12];//InputBuffer[0][12] = nitrification rate = (T) * timestep * commandline(1/s)
                    if(nh2no<nextflow.dissolved[1]){
                        nextflow.dissolved[1]-=nh2no;
                        nextflow.dissolved[2]+=nh2no;
                    }else{
                        nextflow.dissolved[2]+=nextflow.dissolved[1];
                        nextflow.dissolved[1] = 0.0;
                    }
                    
                }
                if(ref.Core_Denitrification){
                    if(ref.Core_SP){
                        seg.bed[j].GWdissolved[2]*=Math.max(1-ref.DefaultSegPara[7]*InputBuffer[0][13], 0.0);
                        //ref.DefaultSegPara[7]*InputBuffer[0][13] = denitrification rate =  [timestep * commandline(1/s)] * (T)
                    }else{
                        //Mulholland et al. 2008, 2009 <<----- check **
                        nh2no = ref.timeStep*7.433713e-05;//time
                        nh2no*= InputBuffer[0][13];//temperature
                        nh2no*= 1.770591;// temperature correction from def 25 to def 16.8, why?
                        nh2no*= Math.pow(seg.bed[j].GWdissolved[2],0.5821);//conc
                        if(nh2no>=seg.bed[j].GWdissolved[2]){
                            seg.bed[j].GWdissolved[2]=0.0;
                        }else{
                            seg.bed[j].GWdissolved[2]-=nh2no;
                        }
                    }
                }
            }//end of processOn
            
            
            
        }//end of for loop
        
        
        return true;
    }
    
    
	//-----------------------------  common functions
    public BedCell lastBed(){
        return seg.bed[seg.bed.length-1];
    }
    
	public int order(){
    	if(SegOrder>0) return SegOrder;
    	else if(upstreams.size()==1){
    		SegOrder = upstreams.get(0).order();
    		return SegOrder;
    	}else if(upstreams.size()>1){
    		//int _tmp[] = new int[upstreams.size()];
            int _tmp;
    		int _max=0,_min=10000;
    		for(int i=0; i<upstreams.size(); i++){
    			//_tmp[i]=upstreams.get(i).order();
    			//if(_tmp[i]>_max) _max=_tmp[i];
    			//if(_tmp[i]<_min) _min=_tmp[i];
                
                _tmp=upstreams.get(i).order();
    			if(_tmp>_max) _max=_tmp;
    			if(_tmp<_min) _min=_tmp;
    		}
    		if(_max==_min){SegOrder=_max+1;return SegOrder;}
    		else{SegOrder=_max;return SegOrder;}
    	}else{
    		SegOrder=1;
    		return SegOrder;
    	}
    }//stream order
    public void SetDegree(int n){
    	this.SegDegree=n;
    	for(Segment v:upstreams){v.SetDegree(n+1);}
    }//outlet=0, upstream +1
    public List<Segment> drainNetwork(int setBy){
    	List<Segment> _net_ = new ArrayList<Segment>();
    	if(this.tokenBy>setBy) return _net_;
    	else if(upstreams.size()==1){
    		this.tokenBy=setBy;
    		_net_.add(upstreams.get(0));
    		_net_.addAll(upstreams.get(0).drainNetwork(setBy));
    		return _net_;
    		
    	}else if(upstreams.size()>1){
    		this.tokenBy=setBy;
    		for(Segment v: upstreams){
    			_net_.add(v);
    			_net_.addAll(v.drainNetwork(setBy));
    		}
    		return _net_;
    		
    	}else{
    		this.tokenBy=setBy;
    		return _net_;
    	}
    }//key to makesubnetwork
    public void orderOutlet(int n){
        if(SegOrder<=n){System.out.println(segName);}
        else{
            for(Segment v:upstreams){v.orderOutlet(n);}
        }
    }
    public void orderOutlet(int n, int m){
        if(SegOrder<=m && SegOrder>=n){System.out.println(segName);}
        else if(SegOrder>m){
            for(Segment v:upstreams){v.orderOutlet(n,m);}
        }
    }
    
    public int compareTo(Segment seg){
    	if(this.tokenBy > seg.tokenBy){return 1;}
    	else if(this.tokenBy < seg.tokenBy){return -1;}
    	else {return 0;}

    }
    public String toString(boolean info){
        return segName+"("+SegDegree+"-deg):[order "+SegOrder+"]:(token "+tokenBy+")";
    	//if(outlet){return segHillID+" "+segHillID+" "+SegOrder+" 1 "+segName+" "+SegDegree;}
        //else{return segHillID+" "+segHillID+" "+SegOrder+" 0 "+segName+" "+SegDegree;}
    }
    public String toString(){
        //return segName+"("+SegDegree+"-deg):[order "+SegOrder+"]:(token "+tokenBy+")";
    	//if(outlet){return segHillID+" "+segHillID+" "+SegOrder+" 1 "+segName+" "+SegDegree;}
        //else{return segHillID+" "+segHillID+" "+SegOrder+" 0 "+segName+" "+SegDegree;}
        
        // segment order degree taken outlet segid hillid
        if(outlet){return String.format("%s %d %d %d %d %d %d",segName,SegOrder,SegDegree,tokenBy,1,segHillID,segHillID);}
        else{return String.format("%s %d %d %d %d %d %d",segName,SegOrder,SegDegree,tokenBy,0,segHillID,segHillID);}
    }
    
}





