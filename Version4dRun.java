
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


public class Version4dRun {
	/*
	 * This is a user interface of the network network.
	 */
	
	// tools for reading files 
	static boolean gheader;
    static BufferedReader nbufRdr,cbufRdr;
    static File gfile;
    static String greadline;
    static StringTokenizer gtoken;
    static String str_;
    static String _FS_;
    
//	static boolean CREATEBINARY_rhessys;
//    static boolean CREATEBINARY_rhessysG;
//    static boolean CREATEBINARY_def;
//    static boolean DEBUG;
	static boolean SIMULATION;
	static Network network=null;
    static boolean TASK_regioning;
    static boolean TASK_networking;
    
    static String _SEGMENT_;
	static String _CONNECTIVITY_;
	static String _SUBNET_;
    static double SNfactor;
    static double SQfactor;
    static double LNfactor;
    static double LQfactor;
    static int SensOrder;
    static boolean SensAll;
	
		//called addSegement, makeSegOrder, and makeSegDegree > segmentConstruction
    public static void networkConstruction() throws Exception{
    	
    	//--------------------------------------------------------------------//
    	//*** set Default values in ref ***//
        //1993-1994 -> 1994
    	final String[] FallDates = {"2:12:1994","34:0:1994","54:12:1994","85:0:1994","122:0:1994","154:0:1994",
            "186:0:1994","223:12:1994","254:12:1994","273:0:1994","287:12:1994","301:0:1994",
            "316:12:1994","336:0:1994"};
    	double[] FallProps = {4.95526E-09,3.53947E-09,2.47763E-09,3.53947E-09,4.60131E-09,6.0171E-09,1.3096E-08,1.345E-08,
            3.07934E-08,3.50195E-08,2.48471E-07,2.9767E-07,5.55697E-08,1.13263E-08};
        ref.DefaultFallTime = new CyclicTime(FallDates,new SimpleDateFormat("DD:HH:yyyy"));
        ref.DefaultFallProp = new CyclicTimePattern(ref.DefaultFallTime, FallProps);
        ref.DefaultFallProp.scale_(ref.timeStep);
        
        
    	final String[] BlowDates = {"01-31:1993","02-28:1993","03-31:1993","04-30:1993",
            "05-31:1993","06-30:1993","07-31:1993","09-30:1993","10-31:1993","12-31:1993"};
    	double[] BlowProps = {1.34409E-08,2.08333E-08,6.04839E-09,4.86111E-09,4.7043E-09,7.63889E-09,
            6.72043E-09,2.08333E-09,1.34409E-09,2.01613E-09};
        ref.DefaultBlowTime = new CyclicTime(BlowDates,new SimpleDateFormat("MM-dd:yyyy"));
    	ref.DefaultBlowProp = new CyclicTimePattern(ref.DefaultBlowTime, BlowProps);
    	ref.DefaultBlowProp.scale_(ref.timeStep);
    	
        // ----> temperature <<-----------------------------------------------
        List<Date> tempDate = new ArrayList<Date>();
        List<Double> tempQ = new ArrayList<Double>();
        gfile = new File("temperature93-94.csv");
        nbufRdr = new BufferedReader(new FileReader(gfile));
        gheader = false;
        while((greadline = nbufRdr.readLine()) != null){
        	if(gheader) gheader=false;
        	else{
        		gtoken = new StringTokenizer(greadline,",");
        		
                str_ = gtoken.nextToken()+"-"+gtoken.nextToken()+"-"+gtoken.nextToken(); //yyyy-MM-dd;
        		ref.cal.setTime(ref.DefaultTimeFormat.parse(str_) );
        		tempDate.add(ref.cal.getTime());
        		tempQ.add(Math.pow(2,0.1*(Double.parseDouble(gtoken.nextToken())-25)) );
        	}
        }//end of while
        nbufRdr.close();
        gfile=null;
        ref.DefaultTempTime = new CyclicTime(tempDate);
        ref.DefaultQtemp = new CyclicTimePattern(ref.DefaultTempTime, tempQ);
        System.out.println("--------------------------------------<forming network structure>");
    	
    	//--------------------------------------------------------------------//
    	//*** check network file ***//
        if(_SEGMENT_==""){throw new Exception("No segment file");}
        if(_CONNECTIVITY_==""){throw new Exception("No connectivity file");}
        if(_SUBNET_==""){System.out.println("Default subnet is using.");}
            
        //--------------------------------------------------------------------//
        //*** create and initiate network network
        gtoken = new StringTokenizer(_SEGMENT_,".");
        network = new Network(gtoken.nextToken());
        
        //---------------------------------- making/connecting segment nodes ------------//
        Map<String,String> connectivity = new HashMap<String,String>();
        
        gfile = new File(_CONNECTIVITY_);
        gheader=true;
        nbufRdr = new BufferedReader(new FileReader(gfile));
        while((greadline = nbufRdr.readLine()) != null){
        	if(gheader) gheader=false;
        	else{
        		gtoken = new StringTokenizer(greadline," ");
        		str_=gtoken.nextToken();
        		String _str_ = gtoken.nextToken();
        		connectivity.put(str_, _str_);
                
        		if(_str_.compareTo("0")==0){
                    network.outlet.add(str_);
                    network.addSegement(str_,true);
                }else{
                    network.addSegement(str_,false);
                }
        	}
        }//end of while
        nbufRdr.close();
        gfile=null;
        
        
        //---------------------------------- outlet nodes ------------//
        List<String> potentialSubnet = new ArrayList<String>();//****
        List<Integer> potentialActive = new ArrayList<Integer>();
        
        if(_SUBNET_==""){
            //default
            potentialSubnet.addAll(network.outlet);
        }else{
            //the order of the subnet matters!
            gfile = new File(_SUBNET_);
            gheader=true;
            nbufRdr = new BufferedReader(new FileReader(gfile));
            while((greadline = nbufRdr.readLine()) != null){
                if(gheader) gheader=false;
                else{
                    gtoken = new StringTokenizer(greadline," ");
                    potentialSubnet.add(gtoken.nextToken());
                    potentialActive.add(Integer.parseInt(gtoken.nextToken()) );
                }
            }//end of while
            nbufRdr.close();
            gfile=null;
        }
        //---------------------------------- connecting segments ------------//
            // connecting segments (restriction::only one downstream segment)
        for(String v:connectivity.keySet()){ 
        	network.addConnectivity(v, connectivity.get(v));
        }
        
            // calculating stream orders (require this first before forming sub-network)
        network.makeSegOrder();
        network.makeSegDegree();
        
        
        
        if(SIMULATION){
            //---------------------------------- constructing/reading segments ------------//
            System.out.println("--------------------------------------<forming segments>");
            String operator;
            gfile = new File(_SEGMENT_);
            gheader=true;
            nbufRdr  = new BufferedReader(new FileReader(gfile));
            int NumOfFiles;
            while((greadline = nbufRdr.readLine()) != null){
                if(gheader) gheader=false;
                else{
                    gtoken = new StringTokenizer(greadline,",");// SP=" " -> ","
                    NumOfFiles = gtoken.countTokens();
                    str_=gtoken.nextToken();//segment ID
                    operator=gtoken.nextToken();//operator
                    System.out.printf("%s is %s\n",str_,operator);
                    
                    if(network.networkMap.containsKey(str_)){
                        if(operator.equals("loadBin")){
                            //load existing binary
                            network.networkMap.get(str_).resumefromBinary(new File(gtoken.nextToken()) );// .binary
                            network.networkMap.get(str_).dumpSeg();
                            //System.out.printf("%s has %d cells\n",str_,network.networkMap.get(str_).segLength);
                            gtoken.nextToken();// .channel
                            gtoken.nextToken();// .rhessys
                        }else if(operator.equals("Crhessys")){
                            //create binary
                            gtoken.nextToken();// .binary
                            segmentConstruction_(str_,1,new File(gtoken.nextToken()),new File(gtoken.nextToken()) );
                        }else if(operator.equals("CrhessysG")){
                            //create binary
                            gtoken.nextToken();// .binary
                            segmentConstruction_(str_,2,new File(gtoken.nextToken()),new File(gtoken.nextToken()) );
                        }else if(operator.equals("Cdef")){
                            //create binary
                            gtoken.nextToken();// .binary
                            segmentConstruction_(str_,0,new File(gtoken.nextToken()),new File(gtoken.nextToken()) );
                        }else if(operator.equals("Urhessys")){
                            //load existing binary
                            Segment updateSeg = network.networkMap.get(str_);
                            updateSeg.resumefromBinary(new File(gtoken.nextToken()) );// .binary
                            int segOrder = updateSeg.SegOrder;
                            gtoken.nextToken();// .channel
                            //-------------------------------------------------------//
                            Time inputTime;
                            TimePattern SpringQ,SpringNH,SpringNO,SpringPO, SpringDOC,SpringDON;
                            TimePattern qLQ,qLNH,qLNO,qLPO, qLDOC, qLDON;
                            
                            ///*** READING .RHESSys
                            ///*** 1    2     3   4          5          6           7
                            ///*** year month day sprN(gN/s) sprQ(m3/s) qLN(gN/s/m) qL(m3/s/m)
                            _FS_=" ";
                            List<Date> _tMarks = new ArrayList<Date>();
                            List<Double> _sprN = new ArrayList<Double>();
                            List<Double> _sprQ = new ArrayList<Double>();
                            List<Double> _sprDOC = new ArrayList<Double>();
                            List<Double> _sprDON = new ArrayList<Double>();
                            List<Double> _qLN = new ArrayList<Double>();
                            List<Double> _qLQ = new ArrayList<Double>();
                            List<Double> _qLDOC = new ArrayList<Double>();
                            List<Double> _qLDON = new ArrayList<Double>();
                            gheader=true;
                            cbufRdr  = new BufferedReader(new FileReader(new File(gtoken.nextToken())));
                            while((greadline = cbufRdr.readLine()) != null){
                                if(gheader){
                                    gheader=false;
                                }else{
                                    gtoken = new StringTokenizer(greadline,_FS_);
                                    
                                    //date
                                    str_ = gtoken.nextToken()+"-"+gtoken.nextToken()+"-"+gtoken.nextToken(); //yyyy-MM-dd;
                                    ref.cal.setTime(ref.DefaultTimeFormat.parse(str_) );
                                    _tMarks.add(ref.cal.getTime());
                                    
                                    //sprQ(m3/s) sprN(gN/s) qL(m3/s/m) qLN(gN/s/m) *** too high [NO3]
                                    
                                    if(SQfactor>0){_sprQ.add(SQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    else{_sprQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep
                                    if(SNfactor>0){_sprN.add(SNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                                    else{_sprN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep, gN -> mgN //*0.1 for correction
                                    
                                    _sprDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                    _sprDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                    
                                    if(LQfactor>0){_qLQ.add(LQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    else{_qLQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep
                                    if(LNfactor>0){_qLN.add(LNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                                    else{_qLN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep, gN -> mgN
                                    
                                    _qLDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                    _qLDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                }
                            }//end of while
                            cbufRdr.close();
                            System.out.print(" processing...");
                            
                            inputTime = new CyclicTime(_tMarks);//CyclicTime //LinearTime
                            SpringQ = new CyclicTimePattern(inputTime,_sprQ);	//m3/dt (already convert above)
                            SpringNH = SpringQ.scale(ref.DeafultSpringNH4C);
                            SpringNO = SpringQ.scale(ref.DeafultSpringNO3C);
                            SpringPO = SpringQ.scale(ref.DeafultSpringPO4C);
                            SpringDOC = SpringQ.scale(ref.DeafultSpringDOCC);
                            SpringDON = SpringQ.scale(ref.DeafultSpringDONC);
                            
                            qLQ = new CyclicTimePattern(inputTime,_qLQ);
                            if(segOrder==SensOrder || SensAll){
                                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                                qLNO = qLQ.scale(ref.DeafultLateralNO3C);
                                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                                qLDON = qLQ.scale(ref.DeafultLateralDONC);
                            }else{
                                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                                qLNO = qLQ.scale(ref.DeafultLateralNO3C);
                                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                                qLDON = qLQ.scale(ref.DeafultLateralDONC);
                            }
                            
                            System.out.print(" DONE\n");
                            
                            //----------------- updating
                            updateSeg.seg.springQ = SpringQ;
                            updateSeg.seg.springNH4 = SpringNH;
                            updateSeg.seg.springNO3 = SpringNO;
                            updateSeg.seg.springPO4 = SpringPO;
                            updateSeg.seg.springDOC = SpringDOC;
                            updateSeg.seg.springDON = SpringDON;
                            
                            updateSeg.seg.qLQ = qLQ;
                            updateSeg.seg.qLNH4 = qLNH;
                            updateSeg.seg.qLNO3 = qLNO;
                            updateSeg.seg.qLPO4 = qLPO;
                            updateSeg.seg.qLDOC = qLDOC;
                            updateSeg.seg.qLDON = qLDON;
                            
                            updateSeg.makeSegBinary();
                            
                        }
                        else if(operator.equals("UrhessysG")){
                            //load existing binary
                            Segment updateSeg = network.networkMap.get(str_);
                            updateSeg.resumefromBinary(new File(gtoken.nextToken()) );// .binary
                            int segOrder = updateSeg.SegOrder;
                            gtoken.nextToken();// .channel
                            //-------------------------------------------------------//
                            Time inputTime;
                            TimePattern SpringQ,SpringNH,SpringNO,SpringPO, SpringDOC,SpringDON;
                            TimePattern qLQ,qLNH,qLNO,qLPO, qLDOC,qLDON;
                            
                            ///*** READING .RHESSys
                            ///*** 1    2     3   4          5          6           7
                            ///*** year month day sprN(gN/s) sprQ(m3/s) qLN(gN/s/m) qL(m3/s/m)
                            _FS_=" ";
                            List<Date> _tMarks = new ArrayList<Date>();
                            List<Double> _sprN = new ArrayList<Double>();
                            List<Double> _sprQ = new ArrayList<Double>();
                            List<Double> _sprDOC = new ArrayList<Double>();
                            List<Double> _sprDON = new ArrayList<Double>();
                            List<Double> _qLN = new ArrayList<Double>();
                            List<Double> _qLQ = new ArrayList<Double>();
                            List<Double> _qLDOC = new ArrayList<Double>();
                            List<Double> _qLDON = new ArrayList<Double>();
                            gheader=true;
                            cbufRdr  = new BufferedReader(new FileReader(new File(gtoken.nextToken())));
                            while((greadline = cbufRdr.readLine()) != null){
                                if(gheader){
                                    gheader=false;
                                }else{
                                    gtoken = new StringTokenizer(greadline,_FS_);
                                    
                                    //date
                                    str_ = gtoken.nextToken()+"-"+gtoken.nextToken()+"-"+gtoken.nextToken(); //yyyy-MM-dd;
                                    ref.cal.setTime(ref.DefaultTimeFormat.parse(str_) );
                                    _tMarks.add(ref.cal.getTime());
                                    
                                    //sprQ(m3/s) sprN(gN/s) qL(m3/s/m) qLN(gN/s/m) *** too high [NO3]
                                    
                                    if(SQfactor>0){_sprQ.add(SQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    else{_sprQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep
                                    if(SNfactor>0){_sprN.add(SNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                                    else{_sprN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep, gN -> mgN //*0.1 for correction
                                    
                                    _sprDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                    _sprDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                    
                                    if(LQfactor>0){_qLQ.add(LQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    else{_qLQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep
                                    if(LNfactor>0){_qLN.add(LNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                                    else{_qLN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                                    //s -> timeStep, gN -> mgN
                                    
                                    _qLDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                    _qLDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                                }
                            }//end of while
                            cbufRdr.close();
                            System.out.print(" processing...");
                            
                            inputTime = new CyclicTime(_tMarks);
                            SpringQ = new CyclicTimePattern(inputTime,_sprQ);	//m3/dt (already convert above)
                            SpringNH = SpringQ.scale(ref.DeafultSpringNH4C);
                            SpringNO = new CyclicTimePattern(inputTime,_sprN);//<<-----
                            SpringPO = SpringQ.scale(ref.DeafultSpringPO4C);
                            SpringDOC = new CyclicTimePattern(inputTime,_sprDOC);
                            SpringDON = new CyclicTimePattern(inputTime,_sprDON);
                            
                            qLQ = new CyclicTimePattern(inputTime,_qLQ);
                            if(segOrder==SensOrder || SensAll){
                                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                                qLNO = new CyclicTimePattern(inputTime,_qLN);
                                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                                qLDON = qLQ.scale(ref.DeafultLateralDONC);
                            }else{
                                // mCLNO3/=totlength; mCLNH4/=totlength; mCLPO4/=totlength;
                                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                                qLNO = new CyclicTimePattern(inputTime,_qLN);
                                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                                qLDOC = new CyclicTimePattern(inputTime,_qLDOC);
                                qLDON = new CyclicTimePattern(inputTime,_qLDON);
                            }

                            
                            System.out.print(" DONE\n");
                            
                            //----------------- updating
                            updateSeg.seg.springQ = SpringQ;
                            updateSeg.seg.springNH4 = SpringNH;
                            updateSeg.seg.springNO3 = SpringNO;
                            updateSeg.seg.springPO4 = SpringPO;
                            updateSeg.seg.springDOC = SpringDOC;
                            updateSeg.seg.springDON = SpringDON;
                            
                            updateSeg.seg.qLQ = qLQ;
                            updateSeg.seg.qLNH4 = qLNH;
                            updateSeg.seg.qLNO3 = qLNO;
                            updateSeg.seg.qLPO4 = qLPO;
                            updateSeg.seg.qLDOC = qLDOC;
                            updateSeg.seg.qLDON = qLDON;
                            
                            updateSeg.makeSegBinary();
                            
                        }
                        
                    }else throw new Exception("connectivity does not match with network");
                }
            }//end of while
            nbufRdr.close();
            gfile=null;
            
            /*
             <------ back up ------>
            gfile = new File(_SEGMENT_);
            gheader=true;
            nbufRdr  = new BufferedReader(new FileReader(gfile));
            int NumOfFiles;
            while((greadline = nbufRdr.readLine()) != null){
                if(gheader) gheader=false;
                else{
                    gtoken = new StringTokenizer(greadline," ");
                    NumOfFiles = gtoken.countTokens();
                    str_=gtoken.nextToken();//segment ID
                    
                    if(network.networkMap.containsKey(str_)){
                        //assume both .channel and .rhessys files are there.
                        if(CREATEBINARY_rhessysG || CREATEBINARY_rhessys || CREATEBINARY_def){
                            //create binary
                            segmentConstruction_(str_,new File(gtoken.nextToken()),new File(gtoken.nextToken()) );
                            //passing .channel and .rhessys
                        }else{
                            //load existing binary
                            network.networkMap.get(str_).resumefromBinary(new File("Seg."+str_+".binary"+ref.loadedBinIndex) );
                        }
                        
                    }else throw new Exception("connectivity does not match with network");
                }
            }//end of while
            nbufRdr.close();
            gfile=null;
            */
            
            // forming subnetworks
            if(_SUBNET_==""){
                for(String v:potentialSubnet){network.addSegementData(v);}
            }else{
                for(int i=0; i<potentialSubnet.size(); i++){
                    if(potentialActive.get(i)>0) network.addSegementData(potentialSubnet.get(i),true);
                    else network.addSegementData(potentialSubnet.get(i),false);
                }
            }
            
        }
        else if(TASK_regioning){
            //---------------------------------------------- write out stream orders//
            // hillslope order
//            BufferedWriter toOUTPUT = new BufferedWriter(new FileWriter(network.NetworkName+".order"));
//            toOUTPUT.write("hill seg order outlet segID degree\n");
//            toOUTPUT.write(network.toString());
//            toOUTPUT.close();
            
            for(int i=1; i<21; i+=2){
                network.orderOutlet(i,i+1);
            }

            
        }
        else if(TASK_networking){
            for(String v:potentialSubnet){
                network.addSegementData(v);
            }
            
            network.getSubnetInfo();
        }

        
    }//end of function
    
    
    private static void segmentConstruction_(String segid, int operator, File channel, File rhessys) throws Exception{
    	
        int segOrder = network.networkMap.get(segid).SegOrder;
        
    	Time inputTime;
    	TimePattern SpringQ,SpringNH,SpringNO,SpringPO, SpringDOC, SpringDON;
        TimePattern qLQ,qLNH,qLNO,qLPO, qLDOC, qLDON;
        
    	LinearX streamDistance;
    	LinearY iniW, iniZ, iniQ, iniV, annualBOM;
    	List<BedCell> bed;
    	
        
        //-------------------------------------------------------//
        System.out.print(segid+": reading CHANNEL("+channel.getName()+")...");
    	///*** READING .CHANNEL
        ///*** 1       2      3           4            5          6           7         8                   9          10       11      12
        ///*** patchID hillID location(m) travelled(m) dDArea(m2) zslope(m/m) DArea(m2) BOM(mgC-AFDM m2yr)  width0(m)  v0(m/s)  z0(m)   q0(m)
    	if(channel.getName().endsWith(".csv")) _FS_=","; else _FS_=" ";
    	List<Double> _x = new ArrayList<Double>();
    	List<Double> _w0 = new ArrayList<Double>();
        List<Double> _z0 = new ArrayList<Double>();
        List<Double> _q0 = new ArrayList<Double>();
        List<Double> _v0 = new ArrayList<Double>();
        List<Double> _cbom0 = new ArrayList<Double>();

        List<Double> _detCN = new ArrayList<Double>();
    	List<Double> _detCP = new ArrayList<Double>();
        List<Double> _detCel = new ArrayList<Double>();
        List<Double> _detLig = new ArrayList<Double>();
        List<Double> _detLC = new ArrayList<Double>();
        List<Double> _detLN = new ArrayList<Double>();
        List<Double> _detLP = new ArrayList<Double>();
        List<Double> _immoCN = new ArrayList<Double>();
        List<Double> _immoCP = new ArrayList<Double>();
        List<Double> _mineCN = new ArrayList<Double>();
        List<Double> _mineCP = new ArrayList<Double>();
        List<Double> _clno3 = new ArrayList<Double>();//lateral input (static; not time series)
        List<Double> _clnh4 = new ArrayList<Double>();//lateral input (static; not time series)
        List<Double> _clpo4 = new ArrayList<Double>();//lateral input (static; not time series)
        
		double mdetCN=0.0, mdetCP=0.0, mdetCel=0.0, mdetLig=0.0, mdetLC=0.0, mdetLN=0.0, mdetLP=0.0;
        double mImmoCN=0.0, mImmoCP=0.0;
        double mMineCN=0.0, mMineCP=0.0;
        double mCLNO3=0.0, mCLNH4=0.0, mCLPO4=0.0;
        LeafType localdet;
        
		gheader=true;
		cbufRdr  = new BufferedReader(new FileReader(channel));
        while((greadline = cbufRdr.readLine()) != null){
        	if(gheader) gheader=false;
        	else{
        		gtoken = new StringTokenizer(greadline,_FS_);
        		
        		if(gtoken.countTokens()>2){
        			gtoken.nextToken();//1 patch ID
            		gtoken.nextToken();//2 hill ID
                    
                    _x.add(Double.parseDouble(gtoken.nextToken())); //3 location
                    gtoken.nextToken();//4 travelled
                    gtoken.nextToken();//5 dArea
                    gtoken.nextToken();//6 zslope
                    gtoken.nextToken();//7 DArea
                    
                    _cbom0.add(Double.parseDouble(gtoken.nextToken()));//8 BOM0 (annual)
                    
            		_w0.add(Double.parseDouble(gtoken.nextToken()));//9 width0
                    _v0.add(Double.parseDouble(gtoken.nextToken()));//10 v0
            		_z0.add(Double.parseDouble(gtoken.nextToken()));//11 depth0
                    _q0.add(Double.parseDouble(gtoken.nextToken()));//12 Q0
            		
                    _detCN.add(Double.parseDouble(gtoken.nextToken()));//13 detritusCN
            		_detCP.add(Double.parseDouble(gtoken.nextToken()));//14 detritusCP
                    _detCel.add(Double.parseDouble(gtoken.nextToken()));//15 detritusCel
                    _detLig.add(Double.parseDouble(gtoken.nextToken()));//16 detritusLig
                    _detLC.add(Double.parseDouble(gtoken.nextToken()));//17 leachC
                    _detLN.add(Double.parseDouble(gtoken.nextToken()));//18 leachN
                    _detLP.add(Double.parseDouble(gtoken.nextToken()));//19 leachP
                    _immoCN.add(Double.parseDouble(gtoken.nextToken()));//20 immobilizerCN
                    _immoCP.add(Double.parseDouble(gtoken.nextToken()));//21 immobilizerCP
                    _mineCN.add(Double.parseDouble(gtoken.nextToken()));//22 minerCN
                    _mineCP.add(Double.parseDouble(gtoken.nextToken()));//23 minerCP
                    
                    _clnh4.add(Double.parseDouble(gtoken.nextToken()));//24 lateralNH4[]
                    _clno3.add(Double.parseDouble(gtoken.nextToken()));//25 lateralNO3[]
                    _clpo4.add(Double.parseDouble(gtoken.nextToken()));//26 lateralPO4[]
                    
        		}else{
        			gtoken.nextToken();//END
        			_x.add(Double.parseDouble(gtoken.nextToken())); //location (a)
        		}
        	}//skip gheader
        }//end of while
        cbufRdr.close();
        System.out.print(" processing...");
        
        _cbom0.add(_cbom0.get(_cbom0.size()-1));// value represent 
        _w0.add(_w0.get(_w0.size()-1));//<--(a) // value represent point at boundary
        _v0.add(_v0.get(_v0.size()-1));//<--(a) // value represent point at boundary
        _z0.add(_z0.get(_z0.size()-1));//<--(a) // value represent point at boundary
        _q0.add(_q0.get(_q0.size()-1));//<--(a) // value represent point at boundary
        
        
        
        double celllength,cellcbom;
        double totlength=0, totcbom=0;
        for(int i=1; i<_x.size(); i++){
            celllength = _x.get(i)-_x.get(i-1);
            totlength += celllength;
            cellcbom = celllength*_cbom0.get(i-1)*0.5*(_w0.get(i)+_w0.get(i-1));
            totcbom += cellcbom;
            
            mdetCN += cellcbom*_detCN.get(i-1);//13 detritusCN
            mdetCP += cellcbom*_detCP.get(i-1);//14 detritusCP
            mdetCel += cellcbom*_detCel.get(i-1);//15 detritusCel
            mdetLig += cellcbom*_detLig.get(i-1);//16 detritusLig
            mdetLC += cellcbom*_detLC.get(i-1);//17 leachC
            mdetLN += cellcbom*_detLN.get(i-1);//18 leachN
            mdetLP += cellcbom*_detLP.get(i-1);//19 leachP
            mImmoCN += cellcbom*_immoCN.get(i-1);//20 immobilizerCN
            mImmoCP += cellcbom*_immoCP.get(i-1);//21 immobilizerCP
            mMineCN += cellcbom*_mineCN.get(i-1);//22 minerCN
            mMineCP += cellcbom*_mineCP.get(i-1);//23 minerCP
        
            mCLNH4 += celllength*_clnh4.get(i-1);//24 lateralNH4[]
            mCLNO3 += celllength*_clno3.get(i-1);//25 lateralNO3[]
            mCLPO4 += celllength*_clpo4.get(i-1);//26 lateralPO4[]
        }
        
        mdetCN/=totcbom; mdetCP/=totcbom; mdetCel/=totcbom; mdetLig/=totcbom; mdetLC/=totcbom; mdetLN/=totcbom; mdetLP/=totcbom;
        mImmoCN/=totcbom; mImmoCP/=totcbom;
        mMineCN/=totcbom; mMineCP/=totcbom;
        mCLNH4/=totlength; mCLNO3/=totlength; mCLPO4/=totlength;
        
        //System.out.printf("mCLNH4 = %f mCLNO3 = %f mCLPO4 = %f\n",mCLNH4,mCLNO3,mCLPO4);
        
        localdet = new LeafType("", mdetCN, mdetCP, mdetCel, mdetLig, mdetLC, mdetLN, mdetLP);
        
        
        //-------------------------------------------------------//
        if(operator==2){
            //-------------------------------------------------------//
            System.out.print(segid+": reading RHESSys("+rhessys.getName()+")... (rhessysg)");
            ///*** READING .RHESSys
            ///*** 1    2     3   4          5          6           7
            ///*** year month day sprN(gN/s) sprQ(m3/s) qLN(gN/s/m) qL(m3/s/m)
            if(rhessys.getName().endsWith(".csv")) _FS_=","; else _FS_=" ";
            List<Date> _tMarks = new ArrayList<Date>();
            List<Double> _sprN = new ArrayList<Double>();
            List<Double> _sprQ = new ArrayList<Double>();
            List<Double> _sprDOC = new ArrayList<Double>();
            List<Double> _sprDON = new ArrayList<Double>();
            List<Double> _qLN = new ArrayList<Double>();
            List<Double> _qLQ = new ArrayList<Double>();
            List<Double> _qLDOC = new ArrayList<Double>();
            List<Double> _qLDON = new ArrayList<Double>();
            gheader=true;
            cbufRdr  = new BufferedReader(new FileReader(rhessys));
            while((greadline = cbufRdr.readLine()) != null){
                if(gheader){
                    gheader=false;
                }else{
                    gtoken = new StringTokenizer(greadline,_FS_);
                    
                    //date
                    str_ = gtoken.nextToken()+"-"+gtoken.nextToken()+"-"+gtoken.nextToken(); //yyyy-MM-dd;
                    ref.cal.setTime(ref.DefaultTimeFormat.parse(str_) );
                    _tMarks.add(ref.cal.getTime());
                    
                    //sprQ(m3/s) sprN(gN/s) qL(m3/s/m) qLN(gN/s/m) *** too high [NO3]
                    
                    if(SQfactor>0){_sprQ.add(SQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    else{_sprQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep
                    if(SNfactor>0){_sprN.add(SNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                    else{_sprN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep, gN -> mgN //*0.1 for correction
                    
                    _sprDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                    _sprDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                    
                    
                    if(LQfactor>0){_qLQ.add(LQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    else{_qLQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep
                    if(LNfactor>0){_qLN.add(LNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                    else{_qLN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep, gN -> mgN
                    
                    _qLDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                    _qLDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                }
            }//end of while
            cbufRdr.close();
            System.out.print(" processing...");
            
            inputTime = new CyclicTime(_tMarks);
            SpringQ = new CyclicTimePattern(inputTime,_sprQ);	//m3/dt (already convert above)
            SpringNH = SpringQ.scale(ref.DeafultSpringNH4C);// no RHESSys input
            SpringNO = new CyclicTimePattern(inputTime,_sprN);
            SpringPO = SpringQ.scale(ref.DeafultSpringPO4C);// no RHESSys input
            SpringDOC = new CyclicTimePattern(inputTime,_sprDOC);
            SpringDON = new CyclicTimePattern(inputTime,_sprDON);
            
            qLQ = new CyclicTimePattern(inputTime,_qLQ);
            if(segOrder==SensOrder || SensAll){
                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                qLNO = new CyclicTimePattern(inputTime,_qLN);
                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                qLDON = qLQ.scale(ref.DeafultLateralDONC);
            }else{
                // mCLNO3/=totlength; mCLNH4/=totlength; mCLPO4/=totlength;
                qLNH = qLQ.scale(mCLNH4);// no RHESSys input
                qLNO = new CyclicTimePattern(inputTime,_qLN);
                qLPO = qLQ.scale(mCLPO4);// no RHESSys input
                qLDOC = new CyclicTimePattern(inputTime,_qLDOC);
                qLDON = new CyclicTimePattern(inputTime,_qLDON);
            }
            
            
            System.out.print(" DONE\n");
        }
        else if(operator==1){
            //-------------------------------------------------------//
            System.out.print(segid+": reading RHESSys("+rhessys.getName()+")... (rhessysq)");
            ///*** READING .RHESSys
            ///*** 1    2     3   4          5          6           7          8               9
            ///*** year month day sprN(gN/s) sprQ(m3/s) qLN(gN/s/m) qL(m3/s/m) litterC(kgC/m2) litterN(kgN/m2)
            if(rhessys.getName().endsWith(".csv")) _FS_=","; else _FS_=" ";
            List<Date> _tMarks = new ArrayList<Date>();
            List<Double> _sprN = new ArrayList<Double>();
            List<Double> _sprQ = new ArrayList<Double>();
            List<Double> _sprDOC = new ArrayList<Double>();
            List<Double> _sprDON = new ArrayList<Double>();
            List<Double> _qLN = new ArrayList<Double>();
            List<Double> _qLQ = new ArrayList<Double>();
            List<Double> _qLDOC = new ArrayList<Double>();
            List<Double> _qLDON = new ArrayList<Double>();
            gheader=true;
            cbufRdr  = new BufferedReader(new FileReader(rhessys));
            while((greadline = cbufRdr.readLine()) != null){
                if(gheader){
                    gheader=false;
                }else{
                    gtoken = new StringTokenizer(greadline,_FS_);
                    
                    //date
                    str_ = gtoken.nextToken()+"-"+gtoken.nextToken()+"-"+gtoken.nextToken(); //yyyy-MM-dd;
                    ref.cal.setTime(ref.DefaultTimeFormat.parse(str_) );
                    _tMarks.add(ref.cal.getTime());
                    
                    //sprQ(m3/s) sprN(gN/s) qL(m3/s/m) qLN(gN/s/m) *** too high [NO3]
                    
                    if(SQfactor>0){_sprQ.add(SQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    else{_sprQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep
                    if(SNfactor>0){_sprN.add(SNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                    else{_sprN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep, gN -> mgN //*0.1 for correction
                    
                    _sprDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                    _sprDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                    
                    if(LQfactor>0){_qLQ.add(LQfactor*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    else{_qLQ.add(ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep
                    if(LNfactor>0){_qLN.add(LNfactor*1000*ref.timeStep*Double.parseDouble(gtoken.nextToken())); }
                    else{_qLN.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));}
                    //s -> timeStep, gN -> mgN
                    
                    _qLDOC.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                    _qLDON.add(1000*ref.timeStep*Double.parseDouble(gtoken.nextToken()));//gN -> mgN
                }
            }//end of while
            cbufRdr.close();
            System.out.print(" processing...");
            
            inputTime = new CyclicTime(_tMarks);//CyclicTime //LinearTime
            SpringQ = new CyclicTimePattern(inputTime,_sprQ);	//m3/dt (already convert above)
            SpringNH = SpringQ.scale(ref.DeafultSpringNH4C);
            SpringNO = SpringQ.scale(ref.DeafultSpringNO3C);
            SpringPO = SpringQ.scale(ref.DeafultSpringPO4C);
            SpringDOC = SpringQ.scale(ref.DeafultSpringDOCC);
            SpringDON = SpringQ.scale(ref.DeafultSpringDONC);
            
            qLQ = new CyclicTimePattern(inputTime,_qLQ);
            if(segOrder==SensOrder || SensAll){
                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                qLNO = qLQ.scale(ref.DeafultLateralNO3C);
                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                qLDON = qLQ.scale(ref.DeafultLateralDONC);
                System.out.printf("[%d]Sens[%d]...",segOrder,SensOrder);
            }else{
                qLNH = qLQ.scale(mCLNH4);
                qLNO = qLQ.scale(mCLNO3);
                qLPO = qLQ.scale(mCLPO4);
                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                qLDON = qLQ.scale(ref.DeafultLateralDONC);
                System.out.printf("[%d]local...",segOrder);
            }
            
            System.out.print(" DONE\n");
            
        }
        else if(operator==0){
            //-------------------------------------------------------//
            List<Date> _tMarks = new ArrayList<Date>();
            _tMarks.add(ref.STARTDATE);
            _tMarks.add(ref.ENDDATE);
            
            inputTime = new CyclicTime(_tMarks);
            SpringQ = new StableTimePattern(1.0*ref.timeStep);	//m3/dt
            SpringNH = SpringQ.scale(ref.DeafultSpringNH4C);
            SpringNO = SpringQ.scale(ref.DeafultSpringNO3C);
            SpringPO = SpringQ.scale(ref.DeafultSpringPO4C);
            SpringDOC = SpringQ.scale(ref.DeafultSpringDOCC);
            SpringDON = SpringQ.scale(ref.DeafultSpringDONC);
            
            qLQ = new StableTimePattern(0.01*ref.timeStep);
            if(segOrder==SensOrder || SensAll){
                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                qLNO = qLQ.scale(ref.DeafultLateralNO3C);
                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                qLDON = qLQ.scale(ref.DeafultLateralDONC);
            }else{
                // mCLNO3/=totlength; mCLNH4/=totlength; mCLPO4/=totlength;
                qLNH = qLQ.scale(mCLNH4);
                qLNO = qLQ.scale(mCLNO3);
                qLPO = qLQ.scale(mCLPO4);
                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                qLDON = qLQ.scale(ref.DeafultLateralDONC);
            }
        }
        else{
            //-------------------------------------------------------//
            List<Date> _tMarks = new ArrayList<Date>();
            _tMarks.add(ref.STARTDATE);
            _tMarks.add(ref.ENDDATE);
            
            inputTime = new CyclicTime(_tMarks);
            SpringQ = new StableTimePattern(0.0*ref.timeStep);	//m3/dt
            SpringNH = SpringQ.scale(ref.DeafultSpringNH4C);
            SpringNO = SpringQ.scale(ref.DeafultSpringNO3C);
            SpringPO = SpringQ.scale(ref.DeafultSpringPO4C);
            SpringDOC = SpringQ.scale(ref.DeafultSpringDOCC);
            SpringDON = SpringQ.scale(ref.DeafultSpringDONC);
            
            qLQ = new StableTimePattern(0.01*ref.timeStep);
            if(segOrder==SensOrder || SensAll){
                qLNH = qLQ.scale(ref.DeafultLateralNH4C);
                qLNO = qLQ.scale(ref.DeafultLateralNO3C);
                qLPO = qLQ.scale(ref.DeafultLateralPO4C);
                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                qLDON = qLQ.scale(ref.DeafultLateralDONC);
            }else{
                // mCLNO3/=totlength; mCLNH4/=totlength; mCLPO4/=totlength;
                qLNH = qLQ.scale(mCLNH4);
                qLNO = qLQ.scale(mCLNO3);
                qLPO = qLQ.scale(mCLPO4);
                qLDOC = qLQ.scale(ref.DeafultLateralDOCC);
                qLDON = qLQ.scale(ref.DeafultLateralDONC);
            }
        }
        
        //-------------------------------------------------------//
        //iniW, iniZ, iniQ, iniV, annualBOM
        bed = new ArrayList<BedCell>();
        streamDistance = new LinearX(_x);
        annualBOM  = new LinearY(streamDistance,_cbom0);
    	iniW = new LinearY(streamDistance,_w0);
        iniV  = new LinearY(streamDistance,_v0);
    	iniZ = new LinearY(streamDistance,_z0);
    	iniQ = new LinearY(streamDistance,_q0);
    	
        //contructing high-resolution channel
    	System.out.print(" constructing bedcells...");
    	double SegLength = _x.get(_x.size()-1);
        double currentX=0, travelX;
        double dv,w,z,aom;
        
        //searching time
        inputTime.searchIndex(ref.STARTDATE);
        ref.DefaultFallTime.searchIndex(ref.STARTDATE);
        ref.DefaultBlowTime.searchIndex(ref.STARTDATE);
        
        double mm = ref.DefaultFallProp.at(ref.DefaultFallTime);
        mm += ref.DefaultBlowProp.at(ref.DefaultBlowTime);
        mm *= 0.01;//need to count for BOM0
        
        while(currentX<SegLength){
        	/* based on drainage area */
            dv=0; w=0; z=0; aom=0;
            travelX=currentX;
            for(int i=0; i<ref.timeStep; i++){
                dv += iniV.at(travelX);
                w += iniW.at(travelX);
                z += iniZ.at(travelX);
                aom += annualBOM.at(travelX);//density
                travelX = currentX+dv;
                //System.out.printf("i = %d dv = %f and travelX = %f Seglength = %f\n",i,dv,travelX,SegLength);
                
            }
            w*=ref.timeStep_1;
            z*=ref.timeStep_1;
            aom*=ref.timeStep_1;
            
            //System.out.printf("left point at(%f) w = %f z = %f aom = %f\n\n",currentX,w,z,aom);
            
        	if(segOrder==SensOrder || SensAll){
                bed.add(new BedCell(
                                    0.5*(currentX+travelX), //location
                                    dv,//length
                                    w,//full width
                                    w,//current width
                                    z,//current depth
                                    
                                    1.0,//%DO sat.
                                    ref.DeafultSpringNH4C,false,//NH4 conc
                                    ref.DeafultSpringNO3C,false,//NO3 conc
                                    ref.DeafultSpringPO4C,false,//PO4 conc
                                    ref.DefaultAnnualC,//aom,
                                    new Microbe(mm*ref.DefaultAnnualC,ref.fungal_cn,ref.fungal_cp),
                                    new Microbe(mm*ref.DefaultAnnualC,ref.bacterial_cn,ref.bacterial_cp),
                                    null, null));
            }else{
                bed.add(new BedCell(
                                    0.5*(currentX+travelX), //location
                                    dv,//length
                                    w,//full width
                                    w,//current width
                                    z,//current depth
                                    
                                    1.0,//%DO sat.
                                    ref.DeafultSpringNH4C,false,//NH4 conc [false = conc; true = mass]
                                    ref.DeafultSpringNO3C,false,//NO3 conc
                                    ref.DeafultSpringPO4C,false,//PO4 conc
                                    aom,//<<-----
                                    new Microbe(mm*aom,mImmoCN,mImmoCP),
                                    new Microbe(mm*aom,mMineCN,mMineCP),
                                    null, null));
            }
        	currentX=travelX;
        }//end of while
        System.out.print("make cells["+bed.size()+"] DONE\n");
        
        //segmentCell store Spring information, but only order 1 segment uses Spring in simulation
        if(segOrder==SensOrder || SensAll){
            network.addSegementCell(segid, bed, ref.leafMatz, inputTime,SpringQ,SpringNH,SpringNO,SpringPO,SpringDOC,SpringDON,  qLQ,qLNH,qLNO,qLPO,qLDOC,qLDON);
            
        }else{
            network.addSegementCell(segid, bed, localdet, inputTime,SpringQ,SpringNH,SpringNO,SpringPO,SpringDOC,SpringDON,  qLQ,qLNH,qLNO,qLPO,qLDOC,qLDON);
            
        }
    	System.out.println(segid+" is created.");
    	
    }
    
    
    //time java -cp Pattern_4jan2013.jar:cmdParser_4jan2013.jar:TimePatternC_04jun2013.jar:. -jar version4dII_26sep2013.jar segment=pg.segment connect=pg.connectivity subnet=pg.subnet cpu=8 -a immo=3 mine=3 immoDays=2.260348 mineDays=2.534168 nitri=0.001456261 dnitri=8.649275e-05 start=2003-01-01 end=2010-12-31 -s -obinary index=ini0
    //time java -cp Pattern_4jan2013.jar:cmdParser_4jan2013.jar:TimePatternC_04jun2013.jar:. -jar version4dII_26sep2013.jar segment=pg.segment connect=pg.connectivity subnet=pg.subnet cpu=8 -regioning
    //time java -cp Pattern_4jan2013.jar:cmdParser_4jan2013.jar:TimePatternC_04jun2013.jar:. -jar version4dII_26sep2013.jar segment=pg.segment connect=pg.connectivity subnet=pg.subnet cpu=8 -networking
	public static void main(String[] args) {
    	try{
            System.out.println("==========================================");
    		System.out.println("This is a main run from version 4d.");
        	cmdParser parser = new cmdParser();
            //...simulation controls
        	parser.flag.put("-s",false);//simulation
//        	parser.flag.put("-c",false);
//          parser.flag.put("-crhessys",false);
//        	parser.flag.put("-crhessysg", false);
            parser.flag.put("-obinary", false);//making ending binary
            parser.flag.put("-oallseg", false);//print all segments rather than the most downstream
            parser.flag.put("-oallcell", false);
            
            parser.flag.put("-regioning", false);
            parser.flag.put("-networking", false);
        	//...process controls
        	parser.flag.put("-a", false);//turn on all process
        	parser.flag.put("-n", false);//turn on nitrification and denitrification
            parser.flag.put("-sp", false);//turn on spatial variation
        	parser.flag.put("-d", false);//turn on decomposition: leach, microbial proces, fragmentation
        	parser.flag.put("-f", false);//turn on fine organic matter: deposition and entrainment
            parser.flag.put("-sensall", false);
            
        	/* advection, inflow, storage exchange and leaffall are default ON */
        	//...input and runtime controls
        	parser.intOption.put("cpu", 4);
        	parser.intOption.put("tstep", 180);
            parser.intOption.put("immo", 0);
            parser.intOption.put("mine", 0);
            parser.intOption.put("Sens",0);
            
            parser.txtOption.put("start", "2005-09-22");//yyyy-MM-dd (JAVA set it 0h 0m 0s)
        	parser.txtOption.put("end", "2006-09-21");//yyyy-MM-dd = 2006-09-21
        	parser.txtOption.put("segment", "ws14.segment");
        	parser.txtOption.put("connect", "ws14.connectivity");
            parser.txtOption.put("subnet", "");
        	parser.txtOption.put("index", "");
            parser.txtOption.put("loadBin", "");
            parser.txtOption.put("loadOut", "");
            //...parameter controls (working)
            parser.doubleOption.put("capacity",10.0);
            parser.doubleOption.put("immoDays",1.8);//<<----number of days *(calibrated with temp in version 5c)*
            parser.doubleOption.put("SimmoCN",7.208571);//<<---- mgC/mgN (not molar ratio)
            parser.doubleOption.put("SimmoCP",188.124);//<<---- mgC/mgP (not molar ratio)
            parser.doubleOption.put("mineDays",2.4);//<<----number of days *(calibrated with temp in version 5c)*
            parser.doubleOption.put("SmineCN",4.746857);//<<---- mgC/mgN (not molar ratio)
            parser.doubleOption.put("SmineCP",20.17239);//<<---- mgC/mgP (not molar ratio)
            parser.doubleOption.put("detritusAnnual",163500.0);//<<---- mgC-AFDM
            parser.doubleOption.put("SdetritusCN",93.47936246);//<<---- mgC/mgN (not molar ratio)
            parser.doubleOption.put("SdetritusCP",490.116652);//<<---- mgC/mgP (not molar ratio)
            parser.doubleOption.put("SdetritusCel",0.20377305);
            parser.doubleOption.put("SdetritusLig",0.16277558);
            parser.doubleOption.put("SleachC",0.15);
            parser.doubleOption.put("SleachN",0.4781994);
            parser.doubleOption.put("SleachP",0.6561182);
            parser.doubleOption.put("springNH4",0.0);
            parser.doubleOption.put("springNO3",25.0);
            parser.doubleOption.put("springPO4",2.0);
            parser.doubleOption.put("springDOC",0.0);
            parser.doubleOption.put("springDON",0.0);
            parser.doubleOption.put("SlateralNH4",0.0);
            parser.doubleOption.put("SlateralNO3",25.0);
            parser.doubleOption.put("SlateralPO4",2.0);
            parser.doubleOption.put("SlateralDOC",0.0);
            parser.doubleOption.put("SlateralDON",0.0);
            parser.doubleOption.put("cSNf",-1.0);
            parser.doubleOption.put("cLNf",-1.0);
            parser.doubleOption.put("cSQf",-1.0);
            parser.doubleOption.put("cLQf",-1.0);
            parser.doubleOption.put("ARatio",1.25);
            parser.doubleOption.put("nitri",0.0002339181);//second time unit
            parser.doubleOption.put("dnitri",4.783307e-05);//second time unit
            
            parser.doubleOption.put("dMAX",10.0);
        	parser.parse(args);
        	
        	//----------------------------------------------------
            //...set time-dependent parameters
            ref.immobilizer_days = parser.doubleOption.get("immoDays");//*
            ref.miner_days = parser.doubleOption.get("mineDays");
            ref.micmax = parser.doubleOption.get("dMAX");
            //...set time-independent paramters
            ref.capacity_1 = 100.0;
            ref.capacity_1 /= parser.doubleOption.get("capacity");
            ref.fungal_cn = parser.doubleOption.get("SimmoCN");
            ref.fungal_cp = parser.doubleOption.get("SimmoCP");
            ref.bacterial_cn = parser.doubleOption.get("SmineCN");
            ref.bacterial_cp = parser.doubleOption.get("SmineCP");
            
            ref.DefaultAnnualC = parser.doubleOption.get("detritusAnnual");
            ref.leafMatz = new LeafType("sens",
                                        parser.doubleOption.get("SdetritusCN"),
                                        parser.doubleOption.get("SdetritusCP"),
                                        parser.doubleOption.get("SdetritusCel"),
                                        parser.doubleOption.get("SdetritusLig"),
                                        parser.doubleOption.get("SleachC"),
                                        parser.doubleOption.get("SleachN"),
                                        parser.doubleOption.get("SleachP"));
            ref.DeafultSpringNH4C = parser.doubleOption.get("springNH4");
            ref.DeafultSpringNO3C = parser.doubleOption.get("springNO3");
            ref.DeafultSpringPO4C = parser.doubleOption.get("springPO4");
            ref.DeafultSpringDOCC = parser.doubleOption.get("springDOC");
            ref.DeafultSpringDONC = parser.doubleOption.get("springDON");
            ref.DeafultLateralNH4C = parser.doubleOption.get("SlateralNH4");
            ref.DeafultLateralNO3C = parser.doubleOption.get("SlateralNO3");
            ref.DeafultLateralPO4C = parser.doubleOption.get("SlateralPO4");
            ref.DeafultLateralDOCC = parser.doubleOption.get("SlateralDOC");
            ref.DeafultLateralDONC = parser.doubleOption.get("SlateralDON");
            SensOrder = parser.intOption.get("Sens");
            SensAll = parser.flag.get("-sensall");
            
            SNfactor = parser.doubleOption.get("cSNf");//rescale spring input
            SQfactor = parser.doubleOption.get("cSQf");//rescale spring input
            LNfactor = parser.doubleOption.get("cLNf");//rescale lateral input
            LQfactor = parser.doubleOption.get("cLQf");//rescale lateral input
            ref.DefaultSegPara[11]=parser.doubleOption.get("ARatio");//<<-----------
            ref.DefaultSegPara[6]=parser.doubleOption.get("nitri");//<<-----------
            ref.DefaultSegPara[7]=parser.doubleOption.get("dnitri");//<<-----------
            
            ref.customizedIndex = parser.txtOption.get("index");
            if(ref.customizedIndex.length()>0){ref.customizedIndex="."+ref.customizedIndex;}
            ref.loadedBinIndex = parser.txtOption.get("loadBin");
            if(ref.loadedBinIndex.length()>0){ref.loadedBinIndex="."+ref.loadedBinIndex;}
            ref.loadedOutIndex = parser.txtOption.get("loadOut");
            if(ref.loadedOutIndex.length()>0){ref.loadedOutIndex="."+ref.loadedOutIndex;}
            
            //...set microbial simulation mode
            ref.immobilizer_mode=parser.intOption.get("immo");
            ref.miner_mode=parser.intOption.get("mine");

            //...set time step and simulation time
        	ref.setTimeSet(parser.intOption.get("tstep")); //<<---------
        	Calendar cal = Calendar.getInstance();
        	cal.setTime(ref.DefaultTimeFormat.parse(parser.txtOption.get("start")));
        	ref.STARTDATE = cal.getTime();
            ref.strDefSTART = ref.DefaultTimeFormat.format(ref.STARTDATE);
        	cal.setTime(ref.DefaultTimeFormat.parse(parser.txtOption.get("end")));
        	ref.ENDDATE = cal.getTime();
            ref.strDefEND = ref.DefaultTimeFormat.format(ref.ENDDATE);
        	//...set process simulation
            ref.binary=parser.flag.get("-obinary");
            ref.printAllSeg=parser.flag.get("-oallseg");
            ref.printAllCell=parser.flag.get("-oallcell");
            ref.Core_SP=parser.flag.get("-sp");
        	if(parser.flag.get("-d") || parser.flag.get("-a")){
        		ref.Core_Decomp = true;
            	ref.Core_FragementMac = true;
            	ref.Core_leaching = true;
        	}else{
        		ref.Core_Decomp = false;
            	ref.Core_FragementMac = false;
            	ref.Core_leaching = false;
        	}
        	if(parser.flag.get("-n") || parser.flag.get("-a")){
        		ref.Core_Denitrification=true;
            	ref.Core_Nitrification=true;
        	}else{
        		ref.Core_Denitrification=false;
            	ref.Core_Nitrification=false;
        	}
        	if(parser.flag.get("-f") || parser.flag.get("-a")){
        		ref.Core_Deposition=true;
            	ref.Core_Entrainment=true;
        	}else{
        		ref.Core_Deposition=false;
            	ref.Core_Entrainment=false;
        	}
            //...set simulation runtime and input files
        	ref.NumOfProcessors = parser.intOption.get("cpu");
        	_SEGMENT_ = parser.txtOption.get("segment");///<<<<<<<
        	_CONNECTIVITY_ = parser.txtOption.get("connect");///<<<<<<<
            _SUBNET_ = parser.txtOption.get("subnet");///<<<<<<<
            
//            CREATEBINARY_def = parser.flag.get("-c");
//        	CREATEBINARY_rhessysG = parser.flag.get("-crhessysg");
//            CREATEBINARY_rhessys = parser.flag.get("-crhessys");
        	SIMULATION = parser.flag.get("-s");
            TASK_regioning = parser.flag.get("-regioning"); 
            TASK_networking = parser.flag.get("-networking");
            
            
            //...run simulation
            networkConstruction();
        	if(SIMULATION){
                
        		System.out.print(ref.Values()+"\n");
                System.out.printf("running in %d-%d decomposition mode from %s to %s\n",ref.immobilizer_mode,ref.miner_mode, ref.STARTDATE,ref.ENDDATE);
                System.out.printf("network = %s and connect = %s\n",_SEGMENT_,_CONNECTIVITY_);
                System.out.println("--------------------------------------<running simulation...");
                network.run();
            }
        
        	System.out.println("it's done.");
        	System.out.println("==========================================");

    	}catch(Exception e){
			System.err.println("Version4d(Main) received: ("+e+") ");
			e.printStackTrace();
		}
    }//end of main function
}
