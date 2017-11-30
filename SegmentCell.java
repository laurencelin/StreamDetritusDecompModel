
import java.io.Serializable;
import java.util.List;

public class SegmentCell implements Serializable{
	private static final long serialVersionUID = 20120724L;
	
    /* system variables for network model to control*/
    
	boolean Flow_switch;//_switch
	
	//String segName;
    BedCell[] bed;
    LeafType leaftype;
    
        // time series
    Time timeQN;
    TimePattern springQ;
    TimePattern springNH4;
    TimePattern springNO3;
    TimePattern springPO4;
    TimePattern springDOC;// added for DOC
    TimePattern springDON;// added for DON
    // DOP?
    
    TimePattern qLQ;
    TimePattern qLNH4;
    TimePattern qLNO3;
    TimePattern qLPO4;
    TimePattern qLDOC;// added for DOC
    TimePattern qLDON;// added for DOC
    // DOP?
    
    //initial
    public SegmentCell(List<BedCell> _bed, LeafType lp, Time _timeQN, TimePattern _sprQ, TimePattern _sprNH4, TimePattern _sprNO3, TimePattern _sprPO4, TimePattern _sprDOC, TimePattern _sprDON, TimePattern _qLQ, TimePattern _qLNH4,TimePattern _qLNO3,TimePattern _qLPO4,TimePattern _qLDOC,TimePattern _qLDON){
        
    	Flow_switch=true;
    	bed = _bed.toArray(new BedCell[0]);
        
        //local detritus info
        leaftype=lp;
        
        //local time series
        timeQN = _timeQN;
        
        springQ = _sprQ;
        springNH4 = _sprNH4;
        springNO3 = _sprNO3;
        springPO4 = _sprPO4;
        springDOC = _sprDOC;
        springDON = _sprDON;
        
        qLQ = _qLQ;
        qLNH4 = _qLNH4;
        qLNO3 = _qLNO3;
        qLPO4 = _qLPO4;
        qLDOC = _qLDOC;
        qLDON = _qLDON;
    }
    
    
 
    

    
}


