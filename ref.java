import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;

public class ref {
	/* final values */
	public static final double ONE = 1;
    
	/* output file titles */
    public static String customizedIndex="";
    public static String loadedBinIndex="";
    public static String loadedOutIndex="";
	public static final String FS = ",";
	public static final String title="day"+FS+"month"+FS+"Jday"+FS+"year"+FS+
    //bedcell
    "downstream"+FS+
    "depth"+FS+"width"+FS+"benthic area"+FS+"v"+FS+
    //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
    "lig_c"+FS+"dead_c"+FS+"cel_c"+FS+"other_c"+FS+"leach_c"+FS+
    "lig_n"+FS+"dead_n"+FS+"cel_n"+FS+"other_n"+FS+"leach_n"+FS+
    "lig_p"+FS+"dead_p"+FS+"cel_p"+FS+"other_p"+FS+"leach_p"+FS+
    "lig_cA"+FS+"dead_cA"+FS+"cel_cA"+FS+"other_cA"+FS+"leach_cA"+FS+"dead_nA"+FS+"dead_pA"+FS+
    "dead_nS0"+FS+"dead_nS1"+FS+"dead_nS2"+FS+
    "dead_pS0"+FS+"dead_pS1"+FS+"dead_pS2"+FS+
    //gw
    "GW[nh4]"+FS+"GW[no3]"+FS+"GW[op4]"+FS+
    "GW[nh4]A"+FS+"GW[no3]A"+FS+"GW[op4]A"+FS+
    "GW[nh4]SEGA"+FS+"GW[no3]SEGA"+FS+"GW[op4]SEGA"+FS+
    "GW[nh4]S0"+FS+"GW[nh4]S1"+FS+"GW[nh4]S2"+FS+
    "GW[no3]S0"+FS+"GW[no3]S1"+FS+"GW[no3]S2"+FS+
    "GW[po4]S0"+FS+"GW[po4]S1"+FS+"GW[po4]S2"+FS+
    //microbes
    "immobilizer"+FS+"GE"+FS+"resp_c"+FS+"resp_n"+FS+"net.nh4"+FS+"net.no3"+FS+"resp_p"+FS+"net.p"+FS+"micCA"+FS+"micNA"+FS+"micPA"+FS+
    "mic_NS0"+FS+"mic_NS1"+FS+"mic_NS2"+FS+"mic_PS0"+FS+"mic_PS1"+FS+"mic_PS2"+FS+
    "miner"+FS+"GE"+FS+"resp_c"+FS+"resp_n"+FS+"net.nh4"+FS+"net.no3"+FS+"resp_p"+FS+"net.p"+FS+"micCA"+FS+"micNA"+FS+"micPA"+FS+
    "mic_NS0"+FS+"mic_NS1"+FS+"mic_NS2"+FS+"mic_PS0"+FS+"mic_PS1"+FS+"mic_PS2"+FS+
    //FOM
    "fbom_c"+FS+"fbom_n"+FS+"fbom_p"+FS+ //<<-----
    "fbom_cA"+FS+"fbom_nA"+FS+"fbom_pA"+FS+
    "fbom_nS0"+FS+"fbom_nS1"+FS+"fbom_nS2"+FS+
    "fbom_pS0"+FS+"fbom_pS1"+FS+"fbom_pS2"+FS+
    //flowcell
    "Q"+FS+
    //solute
    "water_DO"+FS+"[nh4]"+FS+"[no3]"+FS+"[op4]"+FS+"[doc]"+FS+"[don]"+FS+"[dop]"+FS+
    "[nh4]A"+FS+"[no3]A"+FS+"[po4]A"+FS+
    "[nh4]SEGA"+FS+"[no3]SEGA"+FS+"[po4]SEGA"+FS+
    "[nh4]S0"+FS+"[nh4]S1"+FS+"[nh4]S2"+FS+
    "[no3]S0"+FS+"[no3]S1"+FS+"[no3]S2"+FS+
    "[po4]S0"+FS+"[po4]S1"+FS+"[po4]S2"+FS+
    //seston
    "seston_c"+FS+"seston_n"+FS+"seston_p"+FS+
    "seston_cA"+FS+"seston_nA"+FS+"seston_pA"+FS+
    "seston_nS0"+FS+"seston_nS1"+FS+"seston_nS2"+FS+
    "seston_pS0"+FS+"seston_pS1"+FS+"seston_pS2"; //<<-----
    
	public static final String titleUnit="day"+FS+"month"+FS+"Jday"+FS+"year"+FS+
    //bedcell
    "(m)"+FS+
    "(m)"+FS+"(m)"+FS+"(m2)"+FS+" (m/t)"+FS+
    //CBOM
    "(mass/m2)"+FS+"(mass/m2)"+FS+"(mass/m2)"+FS+""+"(mass/m2)"+FS+"(mass/m2)"+FS+
    "(mass/m2)"+FS+"(mass/m2)"+FS+"(mass/m2)"+FS+"(mass/m2)"+FS+"(mass/m2)"+FS+
    "(mass/m2)"+FS+"(mass/m2)"+FS+"(mass/m2)"+FS+""+"(mass/m2)"+FS+"(mass/m2)"+FS+
    "(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    //gw
    "(mass/m3)"+FS+"(mass/m3)"+FS+"(mass/m3)"+FS+
    "(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+ //GW A & SEGA
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    //microbes
    "(mass/m2)"+FS+"(prop.)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    "(mass/m2)"+FS+"(prop.)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(mass/t)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    //FOM
    "(mass/m2)"+FS+"(mass/m2)"+FS+"(mass/m2)"+FS+
    "(days)"+FS+"(days)"+FS+"(days)"+FS+
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    //flowcell (working below)
    "(m3/s)"+FS+
    "(mass/m3)"+FS+"(mass/m3)"+FS+"(mass/m3)"+FS+"(mass/m3)"+FS+"(mass/m3)"+FS+"(mass/m3)"+FS+"(mass/m3)"+FS+
    "(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+"(days)"+FS+
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//[NH4] source
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//[NO3] source
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//[PO4] source
    "(mass/m3)"+FS+"(mass/m3)"+FS+"(mass/m3)"+FS+
    "(days)"+FS+"(days)"+FS+"(days)"+FS+
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)"+FS+//source
    "(prop.)"+FS+"(prop.)"+FS+"(prop.)";//source
    
	public static final SimpleDateFormat DefaultTimeFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat DefaultTimeFormatToFile = new SimpleDateFormat("DD"+FS+"yyyy");//min,HH,Jday,year
    public static final SimpleDateFormat DefaultTimeFormatToFileII = new SimpleDateFormat("dd"+FS+"MM"+FS+"DD"+FS+"yyyy");
    public static String strDefEND, strDefSTART;
	/* model constants and parameters*/
	public static final double gResp = 0.5;		//Moorhead & Singsabaugh 2006
    public static final double gResp_1 = ONE/gResp;
	public static final double C_O2 = 0.31875;//0.85 respiratory quotient=CO2/O2 (molar) --> 0.31875 = 0.85*(12 C)/(32 O2)
    public static final double O2_C = ONE/C_O2;
    
    
	/* leaf material CNP, cellulose and lignin */
    public static LeafType leafMatz;//<<---- used in the model (below are some pre-defined leaves)
//	public static final LeafType HWC_mix = new LeafType("HWC_mix",93.47936246,490.116652,0.20377305,0.16277558,0.15,0.4781994,0.6561182);
//	//C:N [Carreiro and Sinsabaugh 2000], C:P ?? Ce% [Carreiro and Sinsabaugh 2000], L% [Carreiro and Sinsabaugh 2000, Melillo and Aber 1982]
//	public static final LeafType RedMaple= new LeafType("RedMaple",68.4,444.4951,0.1038,0.0976,0.15,0.4781994,0.6561182);
//	public static final LeafType RedOak= new LeafType("RedOak",54.7,444.4951,0.2002,0.2607,0.15,0.4781994,0.6561182);
//	public static final LeafType Dogwood= new LeafType("Dogwood",58.2,444.4951,0.1081,0.0607,0.15,0.4781994,0.6561182);
//	//C:N [hunter et al 2003], C:P ?? Ce% [hunter et al. 2003], L% [hunter et al. 2003]
//	public static final LeafType Rhododendron= new LeafType("Rhododendron",165.397,444.4951,0.21115,0.11005,0.15,0.4781994,0.6561182);
//	public static final LeafType YellowPoplar= new LeafType("YellowPoplar",90.327,444.4951,0.18365,0.09565,0.15,0.4781994,0.6561182);
//	public static final LeafType ChestnutOak= new LeafType("ChestnutOak",87.002,444.4951,0.17952,0.11571,0.15,0.4781994,0.6561182);
//	//C:N [SuberKropp and Godshalk 1976], C:P ?? Ce% [SuberKropp and Godshalk 1976], L% [SuberKropp and Godshalk 1976]
//	public static final LeafType Hickory= new LeafType("Hickory",57.91506,444.4951,0.1506,0.1001,0.15,0.4781994,0.6561182);
//	public static final LeafType WhiteOak= new LeafType("WhiteOak",120.7243,444.4951,0.1839,0.1220,0.15,0.4781994,0.6561182);
//	//C:N [Moorhead and Sinsabaugh 2006], C:P ?? Ce% [Moorhead and Sinsabaugh 2006??], L% [Moorhead and Sinsabaugh 2006]
//	public static final LeafType Hemlock= new LeafType("Hemlock",103.2702,444.4951,0.3960,0.2060,0.15,0.4781994,0.6561182);
//	//C:N [Johansson 1995], C:P [Johansson 1995] Ce% [Johansson 1995], L% [Johansson 1995] (White Birch)
//	public static final LeafType Birch= new LeafType("Birch",84.03361,634.5849,0.2125,0.2835,0.15,0.4781994,0.6561182);
//	public static final LeafType Spruce= new LeafType("Spruce",161.7251,577.7564,0.2881,0.3183,0.15,0.4781994,0.6561182);
//	public static final LeafType Pine= new LeafType("Pine",219.7802,1548.387,0.2992,0.2592,0.15,0.4781994,0.6561182);
//	//C:N [Gessner and Chauvet 1994], C:P [Gessner and Chauvet 1994] Ce% [?], L% [Gessner and Chauvet 1994]
//	public static final LeafType Sycamore =new LeafType("Sycamore",164.8352,2277.04,0,0.3090,0.15,0.4781994,0.6561182);
//	//C:N [Chauvet 1987], C:P [Chauvet 1987] Ce% [Chauvet 1987], L% [Chauvet 1987]
//	public static final LeafType Poplar =new LeafType("Poplar",41.93548,735.8491,0.2230,0.2320,0.15,0.4781994,0.6561182);
//	public static final LeafType Alder =new LeafType("Alder",19.27273,673.0159,0.1500,0.1240,0.15,0.4781994,0.6561182);
//	public static final LeafType Willow =new LeafType("Willow",25.42683,571.2329,0.1850,0.2000,0.15,0.4781994,0.6561182);
	
	/* global model objects */
	/* model controls */
	public static int immobilizer_mode;
	public static int miner_mode;
    public static double fungal_cn,fungal_cp,fungal_np;
	public static double bacterial_cn,bacterial_cp,bacterial_np;
    public static double capacity_1;
    public static boolean Core_print_results;
	public static boolean Core_leaching;
	public static boolean Core_Decomp;
	public static boolean Core_FragementMac;
	public static boolean Core_Entrainment;
	public static boolean Core_Deposition;
	public static boolean Core_Nitrification;
	public static boolean Core_Denitrification;
    public static boolean Core_SP;
    public static boolean printAllSeg;
    public static boolean printAllCell;
    public static boolean binary;
	public static int NumOfProcessors=4;	    
	public static ExecutorService es;			
	public static Date STARTDATE, ENDDATE; 
	public static Calendar cal = Calendar.getInstance();
    	
	/* leaf-fall daily patterns */
	public static double DefaultAnnualC;
    public static double DeafultSpringNH4C;
    public static double DeafultSpringNO3C;
    public static double DeafultSpringPO4C;
    public static double DeafultSpringDOCC;
    public static double DeafultSpringDONC;
    public static double DeafultLateralNH4C;
    public static double DeafultLateralNO3C;
    public static double DeafultLateralPO4C;
    public static double DeafultLateralDOCC;
    public static double DeafultLateralDONC;
    
    public static Time DefaultFallTime;//HWC <--- calibrated in Version4Run
	public static Time DefaultBlowTime;//HWC <--- calibrated in Version4Run
    public static Time DefaultTempTime;//HWC <--- calibrated in Version4Run
	public static TimePattern DefaultFallProp;//HWC <--- calibrated in Version4Run
	public static TimePattern DefaultBlowProp;//HWC <--- calibrated in Version4Run
    public static TimePattern DefaultQtemp;//HWC <--- calibrated in Version4Run
	
	/*------------------------------------------------------------------------------------------------------*/
	/* timeStep related variables */
	
	/* time units */ 
	public static int timeStep;
    public static double timeStep_1;
	public static int timeStep_IN_HOUR;
	public static int timeStep_IN_DAY;
	public static int timeStep_IN_YEAR;
	public static int timeStep_IN_LEAP;
    public static double SECOND_TO_timeStepRATE;
	public static double HOURLY_TO_timeStepRATE;
	public static double DAILY_TO_timeStepRATE;
	public static double YEARLY_TO_timeStepRATE;
	public static double timeStep_TO_SECONDRATE;
	public static int MILLISECONDS_IN_MINUTE;
	public static int MILLISECONDS_IN_HOUR;
	public static int MILLISECONDS_IN_DAY;
	public static int MILLISECONDS_IN_timeStep;
	    
	//respiration coefficient (0.01 day-1, [0.01-0.05 day-1] Moorhead & Singsabaugh 2006); gResp~GE(0.5)
	public static double mResp;//3.472222e-06;
    //public static double mResp_1;
    public static double immobilizer_growth;
    public static double miner_growth;
    public static double immobilizer_days=0.0;
    public static double miner_days=0.0;
    public static double fbomRemainCoef=1.0;
    public static double micmax;
    //public static double minemax=0.1;
    public static double DefaultSegPara[] = new double[13];//{0,0,0, 0,0,0, 0,0,0, 0,0};
	/*
	//								i	rate (30s)				rate (1s)
	//    double fragRate;			0                           7.666667e-09
	//    double fragRateMac;		1	2.3e-07					7.666667e-09
	//    double *fbomRiseRate;		2	0.0003					1e-05
	//    double *sestonFallRate;	3	0.201756805807623		0.006725227
	//    double MaxSOsaturation; 	4	13100.0					13100.0 (mg/m3)
	//    double reaerationRate;	5	0.05					0.001666667
	//    double deNRate;			7	0.001434992				4.783307e-05
	//    double NRate;				6	0.00701754385964913		0.0002339181
	//    double exchangeRate;		8	0.0057					0.00019
	//    double exchangeRate2;		9	1.25*0.0057				1.25*0.00019
	//	  double leachingRate;		10	0.02					0.0006666667
    //    double cAreaRatio;        11  1.25 (set by cmd)
    //    double cAreaRatio_1       12
	 */
    
    //Math.log(2)/(caliPara[i]*ref.timeStep_IN_DAY)
	
	public static void setTimeSet(int TimeStepInSecond){
		timeStep = TimeStepInSecond;
        timeStep_1 = Math.pow(timeStep,-1);
		timeStep_IN_HOUR = (int)(3600/timeStep);
		timeStep_IN_DAY = (int)(3600*24/timeStep);
		timeStep_IN_YEAR = (int)(3600*24*365/timeStep);
		timeStep_IN_LEAP = (int)(3600*24*366/timeStep);
        SECOND_TO_timeStepRATE = Math.pow(timeStep,-1);
		HOURLY_TO_timeStepRATE = Math.pow(timeStep_IN_HOUR, -1);
		DAILY_TO_timeStepRATE = Math.pow(timeStep_IN_DAY, -1);
		YEARLY_TO_timeStepRATE = Math.pow(timeStep_IN_YEAR, -1);
		timeStep_TO_SECONDRATE = Math.pow(timeStep, -1);
		MILLISECONDS_IN_MINUTE=1000*60;
		MILLISECONDS_IN_HOUR=1000*60*60;
		MILLISECONDS_IN_DAY=1000*60*60*24;
		MILLISECONDS_IN_timeStep=1000*timeStep;
		
        //immomax/=timeStep_IN_DAY;
        //minemax/=timeStep_IN_DAY;
        micmax/=timeStep_IN_DAY;
        
		mResp=1.157407e-07*timeStep;
        //mResp_1 = ONE/mResp;
        if(immobilizer_days>0){immobilizer_growth=Math.log(2)/(immobilizer_days*timeStep_IN_DAY);}else{immobilizer_growth=0;}
        if(miner_days>0){miner_growth=Math.log(2)/(miner_days*timeStep_IN_DAY);}else{miner_growth=0;}
        

        /*
         //								i	rate (30s)				rate (1s)
         //    double fragRate;			0	2.3e-07					7.666667e-09
         //    double fragRateMac;		1	2.3e-07					7.666667e-09
         //    double *fbomRiseRate;	2	0.0003					1e-05
         //    double *sestonFallRate;	3	0.201756805807623		0.006725227
         //    double MaxSOsaturation; 	4	13100.0					13100.0 (mg/m3)
         //    double reaerationRate;	5	0.05					0.001666667
         //    double deNRate;			7	0.001434992				4.783307e-05
         //    double NRate;			6	0.00701754385964913		0.0002339181
         //    double exchangeRate;		8	0.0057					0.00019
         //    double exchangeRate2;	9	1.25*0.0057				1.25*0.00019
         //	   double leachingRate;		10	0.02					0.0006666667
         //    double cAreaRatio;       11  1.25 (set by cmd)
         //    double cAreaRatio_1      12  
         */
		DefaultSegPara[0]=7.666667e-09*timeStep;
		DefaultSegPara[1]=7.666667e-09*timeStep;
		DefaultSegPara[2]=1e-05*timeStep;
		DefaultSegPara[3]=0.006725227*timeStep;
		DefaultSegPara[4]=13100.0;
		DefaultSegPara[5]=0.001666667*timeStep;
		DefaultSegPara[6]*=timeStep;//<<----------- set in command line
		DefaultSegPara[7]*=timeStep;//<<----------- set in command line
		DefaultSegPara[8]=0.00019*timeStep;
		DefaultSegPara[9]=1.25*0.00019*timeStep;
		DefaultSegPara[10]=0.0006666667*timeStep;
		DefaultSegPara[12]=ONE/DefaultSegPara[11]; //<<----------- (11) set in command line
	}
	
	
	public static String Values(){
		String _str_ = "All Rates below are based on timestep!\n"+
                "timestep = "+timeStep+"\n"+
                "from: "+DefaultTimeFormatToFile.format(STARTDATE)+" to: "+DefaultTimeFormatToFile.format(ENDDATE)+"\n"+
				"max. Immobilizer growth rate = "+immobilizer_growth+"\n"+
				"max. Miner growth rate = "+miner_growth+"\n"+
				"deN rate = "+DefaultSegPara[7]+"\n"+
				"frag rate = "+DefaultSegPara[1]+"\n"+
                "fom rise rate = "+DefaultSegPara[2]+"\n"+
                "fom fall rate = "+DefaultSegPara[3]+"\n"+
                "exchange 1 = "+DefaultSegPara[8]+"\n"+
                "exchange 2 = "+DefaultSegPara[9]+"\n"+
                "leaching rate = "+DefaultSegPara[10];
        /*
         //								i	rate (30s)				rate (1s)
         //    double fragRate;			0	2.3e-07					7.666667e-09
         //    double fragRateMac;		1	2.3e-07					7.666667e-09
         //    double *fbomRiseRate;	2	0.0003					1e-05
         //    double *sestonFallRate;	3	0.201756805807623		0.006725227
         //    double MaxSOsaturation; 	4	13100.0					13100.0 (mg/m3)
         //    double reaerationRate;	5	0.05					0.001666667
         //    double deNRate;			6	0.001434992				4.783307e-05
         //    double NRate;			7	0.00701754385964913		0.0002339181
         //    double exchangeRate;		8	0.0057					0.00019
         //    double exchangeRate2;	9	1.25*0.0057				1.25*0.00019
         //	  double leachingRate;		10	0.02					0.0006666667
         */
        
		return _str_;
	}
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
