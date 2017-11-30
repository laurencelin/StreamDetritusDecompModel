import java.io.Serializable;

public class LeafType implements Serializable{
	private static final long serialVersionUID = 20120622L;
	String name;
	double leachCprop,leachNprop,leachPprop;	//<<--- good
	double otherCprop,otherNprop,otherPprop;
	double celluloseCprop,celluloseNprop,cellulosePprop;
	double ligninCprop,ligninNprop,ligninPprop;
    double nc, pc;
	
	public LeafType(String _name, double cn, double cp, double cel, double lig, double lc, double ln, double lp){
		name = _name;
        
        //carbon
		leachCprop=lc;
        celluloseCprop=cel;
		ligninCprop=lig;
        otherCprop= 1-cel-lig-lc;
		double llc = ref.ONE/(1-lc);
		
        nc = ref.ONE/cn;
		leachNprop=nc*ln;
        celluloseNprop = nc*(1-ln)*llc*cel;
        ligninNprop = nc*(1-ln)*llc*lig;
        otherNprop = nc*(1-ln)*llc*otherCprop;
		
        pc = ref.ONE/cp;
		leachPprop=pc*lp;
        cellulosePprop = pc*(1-lp)*llc*cel;
        ligninPprop = pc*(1-lp)*llc*lig;
        otherPprop = pc*(1-lp)*llc*otherCprop;
	}
	
    
    public LeafType(LeafType lt){
        name = lt.name;
        
        //carbon
		leachCprop=lt.leachCprop;
        celluloseCprop=lt.celluloseCprop;
		ligninCprop=lt.ligninCprop;
        otherCprop=lt.otherCprop;
		
        leachNprop=lt.leachNprop;
        celluloseNprop = lt.celluloseNprop;
        ligninNprop = lt.ligninNprop;
        otherNprop = lt.otherNprop;
        nc = lt.nc;
		
        leachPprop=lt.leachPprop;
        cellulosePprop = lt.cellulosePprop;
        ligninPprop = lt.ligninPprop;
        otherPprop = lt.otherPprop;
        pc = lt.pc;
    }
    
    public String toString(double cDensity){return cDensity+","+cDensity*nc+","+cDensity*pc;}
    public void toAll(double cDensity, double[][] holder){
		//[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
        
        //C
        holder[0][0]=cDensity*ligninCprop; holder[0][1]=0; holder[0][2]=cDensity*celluloseCprop;
        holder[0][3]=cDensity*otherCprop; holder[0][4]=cDensity*leachCprop;
        //N
        holder[1][0]=cDensity*ligninNprop; holder[1][1]=0; holder[1][2]=cDensity*celluloseNprop;
        holder[1][3]=cDensity*otherNprop; holder[1][4]=cDensity*leachNprop;
        //P
        holder[2][0]=cDensity*ligninPprop; holder[2][1]=0; holder[2][2]=cDensity*cellulosePprop;
        holder[2][3]=cDensity*otherPprop; holder[2][4]=cDensity*leachPprop;
        
   }//end of function
	
}
