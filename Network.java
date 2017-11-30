/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;


public class Network implements Serializable{
	private static final long serialVersionUID = 20120714L;
	
    String NetworkName;
    Map<String,Segment> networkMap; //<<----
    List<subNetwork> subnet;
    List<String> outlet;
    
    /*-------------------------------------------------------------*/
    //initial
    public Network(String _networkname){
    	NetworkName = _networkname;
    	networkMap = new HashMap<String,Segment>();
    	subnet = new ArrayList<subNetwork>();
    	outlet = new ArrayList<String>();
    	
    }
    public void run() throws Exception{
    	try{
    		ref.es = Executors.newFixedThreadPool(ref.NumOfProcessors); //<<------------
    		Collections.sort(subnet);//<<------------ this is for correctly generating the outflow files!!!
            
    		for(int i=subnet.size()-1; i>-1; i--){
                subnet.get(i).load();//-->subnetwork() --> makeSubNetwork(); gather all segments for a given subnet
                System.out.println(subnet.get(i).toString());
                
                //active subnet = simulation (normal mode) and non-active subnet = has outflow file (outflow mode)
                if(subnet.get(i).active){
                    //subnet.run()-->segment load() --> auto load segments into their modes;
                    //if a segment belongs to another subnet, it will be loaded as outflow mode (assume the outflow file already there).
                    subnet.get(i).run();
                    subnet.get(i).unload();//-->subnetwork
                }//active
    		}
        }catch(Exception e){throw e;}
    	finally{
    		ref.es.shutdown();
    	}
    	
    }
    public void getSubnetInfo() throws Exception{
    	try{
    		Collections.sort(subnet);//<<------------
            
    		for(int i=subnet.size()-1; i>-1; i--){
    			//subnet.get(i).load();//-->subnetwork
                subnet.get(i).makeSubNetwork();
        		System.out.println(subnet.get(i).toString());
        		//subnet.get(i).run();
                //subnet.get(i).unload();//-->subnetwork
    		}
        }catch(Exception e){throw e;}
    	
    }
    
    
    //main function
    public void addSegement(String segName, boolean _outlet) throws Exception{
    	networkMap.put(segName, new Segment(segName, _outlet));//<<---
    }
    public void addSegementData(String segid){
        subnet.add(new subNetwork(networkMap.get(segid)));//default active
    }//<<--- make subnetwork
    public void addSegementData(String segid, boolean _active){
        subnet.add(new subNetwork(networkMap.get(segid),_active));
    }//<<--- make subnetwork
    public void addSegementData(String segid, boolean _active, String dir){
        subnet.add(new subNetwork(networkMap.get(segid),_active,dir));
    }//<<--- make subnetwork
    public void addSegementCell(String segid, List<BedCell> _bed, LeafType lp, Time _timeQN, TimePattern _sprQ, TimePattern _sprNH4, TimePattern _sprNO3, TimePattern _sprPO4,TimePattern _sprDOC,TimePattern _sprDON, TimePattern _qLQ, TimePattern _qLNH4,TimePattern _qLNO3,TimePattern _qLPO4,TimePattern _qLDOC,TimePattern _qLDON) throws Exception{
        networkMap.get(segid).segLength=_bed.size();
    	networkMap.get(segid).seg = new SegmentCell(_bed, lp,_timeQN, _sprQ,_sprNH4,_sprNO3,_sprPO4,_sprDOC,_sprDON, _qLQ,_qLNH4,_qLNO3,_qLPO4,_qLDOC,_qLDON);
    	networkMap.get(segid).makeSegBinary();
    }
    public void addConnectivity(String upstream, String downstream){
    	//input this upstream-downstream relationship to networkMap
    	//System.out.println("adding "+upstream+"-->"+downstream);
    	if(networkMap.containsKey(downstream) && networkMap.containsKey(upstream)) 
    		networkMap.get(downstream).upstreams.add(networkMap.get(upstream));
    }
    public void makeSegOrder(){
    	for(Segment v:networkMap.values()){ v.order();}
    }
    public void makeSegDegree(){
    	for(String v:outlet) networkMap.get(v).SetDegree(0);
    }
    public void orderOutlet(int n, int m){
        System.out.printf("For orders %d - %d, \n",n,m);
        for(String v:outlet){networkMap.get(v).orderOutlet(n,m);}
        System.out.printf("\n");
    }
    public String toString(){
    	String _tmp="";
    	for(String v:networkMap.keySet()){
            _tmp+=networkMap.get(v).toString()+"\n";
        }
    		
    	//_tmp+="outlets = "+outlet.toString()+"\n";
    	//for(subNetwork v:subnet) _tmp+=v.toString()+"\n\n";
    	return _tmp;
    }
    //sub-class model element (mostly for holding, grouping, managing purpose)
        //*** simply the bed, approx is no longer needed.
    
    
}//end of main class
