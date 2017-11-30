/*
 * StreamObj is used as a unit of movable obj of stream. unit size is defined by time step.
 */
import java.io.Serializable;


public class FlowCell implements Serializable{
	private static final long serialVersionUID = 20120622L;
    double vol;//(=upstream+inflow-flowout)[cubic meter]
    //dissolved mass
    double dissolved[];//[0:DO, 1:NH4, 2:NO3, 3:PO4, 4:DOC, 5:DON, 6:DOP]
    
    //non-dissolved mass
    double nondissolved[];//[0:sestonC, 1:sestonN, 2:sestonP]
    
    
    //*** initials and functions
    public FlowCell(){
    	vol=0;
    	dissolved = new double[7];//JAVA default 0
        nondissolved = new double[3];//JAVA default 0;
        
    }
    public FlowCell(double _vol, double o2,double nh4, double no3, double po4){
    	vol=_vol;
        dissolved = new double[7];//JAVA default 0
        nondissolved = new double[3];//JAVA default 0;
        //GW source ini.
        dissolved[0]=o2;dissolved[1]=nh4;dissolved[2]=no3;dissolved[3]=po4;
    }
    public FlowCell(double _vol, double o2,
    		double nh4, double no3, double po4,  
    		double doc, double don, double dop,
    		double sestonc, double sestonn, double sestonp){
    	vol=_vol;
        dissolved = new double[7];//JAVA default 0
        nondissolved = new double[3];//JAVA default 0;
        
        dissolved[0]=o2;
        dissolved[1]=nh4;
        dissolved[2]=no3;
        dissolved[3]=po4;
    	dissolved[4]=doc;
        dissolved[5]=don;
        dissolved[6]=dop;
    	nondissolved[0]=sestonc;
        nondissolved[1]=sestonn;
        nondissolved[2]=sestonp;
    }
   
    public FlowCell(FlowCell _flow){
        vol=_flow.vol;
        dissolved = new double[_flow.dissolved.length];
        nondissolved = new double[3];
        
        dissolved[0]=_flow.dissolved[0];
        dissolved[1]=_flow.dissolved[1];
        dissolved[2]=_flow.dissolved[2];
        dissolved[3]=_flow.dissolved[3];
        dissolved[4]=_flow.dissolved[4];
        dissolved[5]=_flow.dissolved[5];
        dissolved[6]=_flow.dissolved[6];
        
        nondissolved[0]=_flow.nondissolved[0];
        nondissolved[1]=_flow.nondissolved[1];
        nondissolved[2]=_flow.nondissolved[2];
    }
    
    //return concentrations
    public double DO(){if(vol>0){return dissolved[0]/vol;}else{return 0;}}
    public double NH4(){if(vol>0){return dissolved[1]/vol;}else{return 0;}}
    public double NO3(){if(vol>0){return dissolved[2]/vol;}else{return 0;}}
    public double PO4(){if(vol>0){return dissolved[3]/vol;}else{return 0;}}
    public double NHprop(){
        double tn = dissolved[1]+dissolved[2];
    	if(tn>0){return dissolved[1]/tn;}else{return 0;}
    }
    public double NOprop(){
        double tn = dissolved[1]+dissolved[2];
    	if(tn>0){return dissolved[2]/tn;}else{return 0;}
    }
    
    
    public FlowCell add_(FlowCell _flow){
    	vol+=_flow.vol;
        
        //dissolved
        dissolved[0]+=_flow.dissolved[0];
        dissolved[1]+=_flow.dissolved[1];
        dissolved[2]+=_flow.dissolved[2];
        dissolved[3]+=_flow.dissolved[3];
        dissolved[4]+=_flow.dissolved[4];
        dissolved[5]+=_flow.dissolved[5];
        dissolved[6]+=_flow.dissolved[6];
        
                
        //non-dissolved
    	nondissolved[0]+=_flow.nondissolved[0];
        nondissolved[1]+=_flow.nondissolved[1];
        nondissolved[2]+=_flow.nondissolved[2];
        return this;
    }
    public FlowCell equal_(FlowCell _flow){
    	vol=_flow.vol;
        
        dissolved[0]=_flow.dissolved[0];
        dissolved[1]=_flow.dissolved[1];
        dissolved[2]=_flow.dissolved[2];
        dissolved[3]=_flow.dissolved[3];
        dissolved[4]=_flow.dissolved[4];
        dissolved[5]=_flow.dissolved[5];
        dissolved[6]=_flow.dissolved[6];
    	
        nondissolved[0]=_flow.nondissolved[0];
        nondissolved[1]=_flow.nondissolved[1];
        nondissolved[2]=_flow.nondissolved[2];
        
    	return this;
    }
    public FlowCell equal_(FlowCell _flow, boolean segReset){
    	vol=_flow.vol;
        
        dissolved[0]=_flow.dissolved[0];
        dissolved[1]=_flow.dissolved[1];
        dissolved[2]=_flow.dissolved[2];
        dissolved[3]=_flow.dissolved[3];
        dissolved[4]=_flow.dissolved[4];
        dissolved[5]=_flow.dissolved[5];
        dissolved[6]=_flow.dissolved[6];
    	
        nondissolved[0]=_flow.nondissolved[0];
        nondissolved[1]=_flow.nondissolved[1];
        nondissolved[2]=_flow.nondissolved[2];
    
    	return this;
    }
    public void setZero(){
    	vol=0;
        
        dissolved[0]=0;
        dissolved[1]=0;
        dissolved[2]=0;
        dissolved[3]=0;
        dissolved[4]=0;
        dissolved[5]=0;
        dissolved[6]=0;
            	
        nondissolved[0]=0;
        nondissolved[1]=0;
        nondissolved[2]=0;
        
    }
    //*** Outputs

    public String toString(){
        
        //standing stock 6
        //time 4
        //prop E
        
        if(vol >0 ){
            double vol_1 = ref.ONE/vol;
            return String.format("%.6f, %.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f, %.4f,%.4f,%.4f,%.4f,%.4f,%.4f, %E,%E,%E, %E,%E,%E, %E,%E,%E, %.6f,%.6f,%.6f, %.4f,%.4f,%.4f, %E,%E,%E, %E,%E,%E",
                                 
                                 vol*ref.SECOND_TO_timeStepRATE,
//                                 dissolved[0],
//                                 dissolved[1],
//                                 dissolved[2],
//                                 dissolved[3],
//                                 dissolved[4],
//                                 dissolved[5],
//                                 dissolved[6],
                                 
                                 dissolved[0]*vol_1,
                                 dissolved[1]*vol_1,
                                 dissolved[2]*vol_1,
                                 dissolved[3]*vol_1,
                                 dissolved[4]*vol_1,
                                 dissolved[5]*vol_1,
                                 dissolved[6]*vol_1,
                                 
                                 0.0,
                                 0.0,
                                 0.0,
                                 0.0,
                                 0.0,
                                 0.0,
                                 
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 
                                 nondissolved[0]*vol_1,
                                 nondissolved[1]*vol_1,
                                 nondissolved[2]*vol_1,
                                 
                                 0.0,
                                 0.0,
                                 0.0,
                                 
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0
                                 
                                 );
        }else{
            return String.format("%.6f, %.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f, %.4f,%.4f,%.4f,%.4f,%.4f,%.4f, %E,%E,%E, %E,%E,%E, %E,%E,%E, %.6f,%.6f,%.6f, %.4f,%.4f,%.4f, %E,%E,%E, %E,%E,%E",
                                 0.0,
                                 0.0,0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0,
                                 0.0,0.0,0.0
                                 );
        }//if
    }
    
}
