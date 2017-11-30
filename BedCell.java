/*
 * StreamBedObj is used as a unit of non-movable object of streams. unit size is defined as ...
 */
import java.io.Serializable;

public class BedCell implements Serializable{
	private static final long serialVersionUID = 20120721L;
	//location
    double xlocation; //center
    double length, width, depth;
    double cellArea;//fullbank benthic area (full bank width * v)
    double benthicArea_cellArea;//ratop
    double benthicArea;//current benthic area (width * v)
    double benthicArea_1;
    
    //variables below are in density unit
    
    //organisms & detritus
    double annualCBOM;
    Microbe immobilizer,miner;
    
    //organic matter arrays
    double[][] SCOM;    //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
    double[] fbom;      //[C,N,P]
    
    //solutions
    double[] GWdissolved; //[0:DO, 1:NH4, 2:NO3, 3:PO4, 4:DOC, 5:DON, 6:DOP]
    double[] _GWdissolved; //[0:DO, 1:NH4, 2:NO3, 3:PO4, 4:DOC, 5:DON, 6:DOP]
    FlowCell flow, _flow;
    
    //private CNP leachout, riseup;
    transient double leachoutc, leachoutn, leachoutp;
    transient double risec, risen, risep;
    transient double fallc, falln, fallp;
    transient double inverse;
    
    public BedCell(
    		double _xlocation,
    		double LENGTH, double WIDTH, double w0, double z0,
    		double satO2, double nh, boolean nhMass, double no, boolean noMass, double po, boolean poMass,
    		double _annualcbom, Microbe _immo,Microbe _miner, 
    		double[][] wetM, double[] _fbom){
        
    	xlocation = _xlocation;
    	length = LENGTH;
    	width = WIDTH;
        depth = z0;
        
    	cellArea = LENGTH*WIDTH;
    	benthicArea = w0*LENGTH;//dynamic
    	benthicArea_1 = ref.ONE/benthicArea;
        benthicArea_cellArea = benthicArea/cellArea;
            	
    	//conc -> mass
        double vol = cellArea*depth;
        double nh_; if(nhMass){nh_=nh;}else{nh_=nh*vol;}
        double no_; if(noMass){no_=no;}else{no_=no*vol;}
        double po_; if(poMass){po_=po;}else{po_=po*vol;}
        flow = new FlowCell(vol, satO2*ref.DefaultSegPara[4]*vol, nh_,no_,po_);
        _flow = new FlowCell(flow);
        
        GWdissolved = new double[flow.dissolved.length];
        _GWdissolved = new double[flow.dissolved.length];
        if(flow.vol>0){
            for(int i=0; i<flow.dissolved.length; i++){GWdissolved[i]=flow.dissolved[i]/flow.vol;}
        }else{
            for(int i=0; i<flow.dissolved.length; i++){GWdissolved[i]=0;}
        }
        
        
        
        //density
        annualCBOM = _annualcbom;
        immobilizer = new Microbe(_immo);//<
        miner = new Microbe(_miner);//<
        
        SCOM=new double[3][5];
        fbom=new double[3];
        //assume fresh leaves
        if(wetM==null){}else{for(int i=0;i<3;i++){for(int j=0;j<5;j++){SCOM[i][j]=wetM[i][j];}}}
        if(_fbom==null){}else{for(int i=0; i<fbom.length; i++){fbom[i]=_fbom[i];}}
    }
    
    
    public void addOMDensity(double[][] inputC){
        //assume fresh leaves
        for(int i=0;i<3;i++){
            for(int j=0;j<5;j++){
                SCOM[i][j]+=annualCBOM*inputC[i][j];
            }//j
    	}//i
    }
    public void leachOutTo(FlowCell flow, double leachR){
        // leaching DOM must be from detritus, no where else in this model
        // thus, no need to track the scources of DOM
        // immobilized N and P do not get leached out
        
    	//[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
    	leachoutc=SCOM[0][4]*leachR; SCOM[0][4]-=leachoutc; leachoutc*=benthicArea;
    	leachoutn=SCOM[1][4]*leachR; SCOM[1][4]-=leachoutn; leachoutn*=benthicArea;
    	leachoutp=SCOM[2][4]*leachR; SCOM[2][4]-=leachoutp; leachoutp*=benthicArea;
    	
    	//[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
    	
        flow.dissolved[4]+=leachoutc;
    	flow.dissolved[5]+=leachoutn;
    	flow.dissolved[6]+=leachoutp;
    }
    public void fomUpDown(FlowCell flow, double riseR, double fallR){
        double fallR_ = fallR*benthicArea_1;

    	//rise per unit area
        risec=fbom[0]*riseR;
        risen=fbom[1]*riseR;
        risep=fbom[2]*riseR;
       
        fallc=flow.nondissolved[0]*fallR_;
        falln=flow.nondissolved[1]*fallR_;
        fallp=flow.nondissolved[2]*fallR_;

        //------------------------------------------------ FBOM
        //fbom rise
        fbom[0]-=risec;
        fbom[1]-=risen;
        fbom[2]-=risep;
        //fbom receive
        fbom[0]+=fallc;
        fbom[1]+=falln;
        fbom[2]+=fallp;
        
        //------------------------------------------------ seston
        //seston fall
        flow.nondissolved[0]-=fallc*benthicArea;
        flow.nondissolved[1]-=falln*benthicArea;
        flow.nondissolved[2]-=fallp*benthicArea;
        
        //seston receive
        flow.nondissolved[0]+=risec*benthicArea;
    	flow.nondissolved[1]+=risen*benthicArea;
        flow.nondissolved[2]+=risep*benthicArea;

    }

    public double submergeOMDensity(){
        return SCOM[0][0]+SCOM[0][1]+SCOM[0][2]+SCOM[0][3]+SCOM[0][4];
    }
    public String toString(){
 
        //standing stock 6
        //time 4
        //prop E
    	return String.format("%.4f, %.4f,%.4f,%.4f,%.4f, %.6f,%.6f,%.6f,%.6f,%.6f, %.6f,%.6f,%.6f,%.6f,%.6f, %.6f,%.6f,%.6f,%.6f,%.6f, %.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f, %E,%E,%E, %E,%E,%E, %.6f,%.6f,%.6f, %.4f,%.4f,%.4f, %.4f,%.4f,%.4f, %E,%E,%E, %E,%E,%E, %E,%E,%E, %s,%s, %.6f,%.6f,%.6f, %.4f,%.4f,%.4f, %E,%E,%E, %E,%E,%E",
                             
                             xlocation,
                             depth,width,benthicArea,length,
                             
                             SCOM[0][0],SCOM[0][1],SCOM[0][2],SCOM[0][3],SCOM[0][4],
                             SCOM[1][0],SCOM[1][1],SCOM[1][2],SCOM[1][3],SCOM[1][4],
                             SCOM[2][0],SCOM[2][1],SCOM[2][2],SCOM[2][3],SCOM[2][4],
                             
                             0.0,
                             0.0,
                             0.0,
                             0.0,
                             0.0,
                             0.0,
                             0.0,

                             0.0,0.0,0.0,
                             0.0,0.0,0.0,
                             
                             GWdissolved[1],GWdissolved[2],GWdissolved[3],
                             0.0,
                             0.0,
                             0.0,
                             0.0,
                             0.0,
                             0.0,
                             0.0,0.0,0.0,
                             0.0,0.0,0.0,
                             0.0,0.0,0.0,
                             
                             immobilizer.toString(),
                             miner.toString(),
                             
                             fbom[0],fbom[1],fbom[2],
                             0.0,
                             0.0,
                             0.0,
                             0.0,0.0,0.0,
                             0.0,0.0,0.0
                             );
 
    }
 
}
