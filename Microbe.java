import java.io.Serializable;

public class Microbe implements Serializable{
	private static final long serialVersionUID = 20120706L;
	double cn, cp, nc, pc;
	double c;//biomass carbon
	
	transient double primaryC, primaryNC, primaryPC, primaryC_1;
	transient double secondN, secondP, secondNHR;
	transient double netN, netNO3, netNH4, netPO4;
	transient double mortalityC, respirationC, miningC, ggrowthC;
	transient double remaingRate,remaingRate2,remaingRate3, GE;
    
    transient double nflux,pflux, inverse;
    
    transient double detritusCAge,detritusNAge,detritusPAge;
    transient double mortalityCAgeFlux,mortalityNAgeFlux,mortalityPAgeFlux;
    transient double releaseNAgeFlux,releasePAgeFlux;
    
    transient double detritusCAge2,detritusNAge2,detritusPAge2;
    transient double mortalityCAgeFlux2,mortalityNAgeFlux2,mortalityPAgeFlux2;
    transient double releaseNAgeFlux2,releasePAgeFlux2;
    
    transient double detritusN0,detritusN1;
    transient double mortalityN0Flux,mortalityN1Flux;
    transient double releaseN0Flux,releaseN1Flux;
    
    transient double detritusP0,detritusP1;
    transient double mortalityP0Flux,mortalityP1Flux;
    transient double releaseP0Flux,releaseP1Flux;
    
//	transient double GE=0;//assimilated -> Growth and Growth-Respiration
//	transient double GEII=0;//assimilated -> Growth and total-Respiration ( = Growth-Respiration + Basal-Respiration )
 
    public Microbe(double _c, double _cn, double _cp){
        c=_c;
        
 	    cn=_cn; cp=_cp; nc=ref.ONE/cn; pc=ref.ONE/cp;
        mortalityC=0; respirationC=0; miningC=0; ggrowthC=0;
        netNO3=0; netNH4=0; netPO4=0;
        remaingRate=0;
    }
    public Microbe(Microbe m){
        c=m.c;
        
        cn=m.cn; cp=m.cp; nc=m.nc; pc=m.pc;
        mortalityC=0; respirationC=0; miningC=0; ggrowthC=0;
        netNO3=0; netNH4=0; netPO4=0;
   	  	remaingRate=0;
    }
   
    //**********************************************************
    //***                Heart of Decomposition              
    //**********************************************************
   
    //*** modify om and solutes
    public void ImmobilizeI(double GR, double BR, BedCell bed, FlowCell flow){
        
        //mortality
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material (Moorhead & Singsabaugh 2006; Paul and Clark 1997, Berg and McClaugherty 2003)
        //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];//dead?
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        remaingRate2 = primaryC; // <<----------------------
        primaryPC = bed.miner.c; // <<----------------------
        mortalityC = 0;//GR*0.5;
//        mortalityC *= 10;//ref.capacity_1;
//        mortalityC *=(c+bed.miner.c);
//        mortalityC /= primaryC;
        remaingRate3 = mortalityC; // <<----------------------
        mortalityC *= c;
    	this.c -= mortalityC;
        
        primaryNC = GR; // <<----------------------
        ggrowthC = GR*c;
        c *= (1+GR);
        
        
        double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= ref.mResp*c;
        MaxRc *= ref.gResp_1;
        
        if(MaxRc > 0){
            primaryC += flow.dissolved[4]*bed.benthicArea_1;
            primaryC_1=ref.ONE/primaryC;
        	primaryNC = bed.SCOM[1][0];
            primaryNC += bed.SCOM[1][1];
            primaryNC += bed.SCOM[1][2];
            primaryNC += bed.SCOM[1][3];
            primaryNC += bed.SCOM[1][4];
            primaryNC += flow.dissolved[5]*bed.benthicArea_1;
            primaryPC = bed.SCOM[2][0];
            primaryPC += bed.SCOM[2][1];
            primaryPC += bed.SCOM[2][2];
            primaryPC += bed.SCOM[2][3];
            primaryPC += bed.SCOM[2][4];
            primaryPC += flow.dissolved[6]*bed.benthicArea_1;
            primaryNC *= primaryC_1; primaryPC *= primaryC_1;
            
        	//k1/2 = 6 (mg/m3) and Umax=1.88 ugN/m2/s from Payn et al. 2005 (Ball creek) and Webster et al. 2009
      	  //dMass = BA * Monod kinetics (ugN/m2/s) * dt
            //with dt=30s and Umax=1.88, # = 1.88*30/1000 [0.0564, 0.0094]
            //with dt=30s and Umax <= Kh (at most), # = 6*30/1000 [0.18,0.03]
                //NH4
            secondNHR = 0.00188;
            secondNHR *= ref.timeStep;
            secondNHR = Math.min(secondNHR,flow.dissolved[1]*bed.benthicArea_1);
            secondNHR *= flow.dissolved[1];
            secondNHR /= (6*flow.vol+flow.dissolved[1]);
                //NO3
            secondN = 0.00188;
            secondN *= ref.timeStep;
            secondN = Math.min(secondN,flow.dissolved[2]*bed.benthicArea_1);
            secondN *= flow.dissolved[2];
            secondN /= (6*flow.vol+flow.dissolved[2]);
                //combine
            secondN += secondNHR;
            if(secondN>0){secondNHR /= secondN;}
            
            secondP = 0.001;
            secondP *= ref.timeStep;
            secondP = Math.min(secondP,flow.dissolved[3]*bed.benthicArea_1);
            secondP *= flow.dissolved[3];
            secondP /= (flow.vol+flow.dissolved[3]);
            
            double domc = Math.min(GR*this.c,MaxRc);
            double ncd = nc-primaryNC;
            double pcd = pc-primaryPC; 
            if(ncd>0) domc = Math.min(domc, secondN/ncd);
            if(pcd>0) domc = Math.min(domc, secondP/pcd);
            domc = Math.min(primaryC,domc);
            
            ggrowthC = domc;
            respirationC = this.c*ref.mResp;
            respirationC += ref.gResp*ggrowthC;
            GE = (ggrowthC-respirationC);
            GE /= domc;
            
            this.c += ggrowthC-respirationC;
            flow.dissolved[0] -= respirationC*ref.O2_C;
            netN = Math.min(secondN, ggrowthC*nc-domc*primaryNC);
            if(netN>0){netNH4 = secondNHR*netN; netNH4 -= respirationC*nc;}else{netNH4 = netN - respirationC*nc;}
            netNH4 *= bed.benthicArea;
            flow.dissolved[1] -= netNH4;
            if(netN>0){netNO3 = (1-secondNHR); netNO3 *= netN; netNO3*=bed.benthicArea; flow.dissolved[2] -= netNO3;}else{netNO3=0;}
            netPO4 = Math.min(secondP, ggrowthC*pc-domc*primaryPC)-respirationC*pc;
            netPO4 *= bed.benthicArea;
            flow.dissolved[3] -= netPO4;
                    
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            bed.SCOM[0][0]*=remaingRate;bed.SCOM[0][1]*=remaingRate;bed.SCOM[0][2]*=remaingRate;bed.SCOM[0][3]*=remaingRate;bed.SCOM[0][4]*=remaingRate;
            bed.SCOM[1][0]*=remaingRate;bed.SCOM[1][1]*=remaingRate;bed.SCOM[1][2]*=remaingRate;bed.SCOM[1][3]*=remaingRate;bed.SCOM[1][4]*=remaingRate;
            bed.SCOM[2][0]*=remaingRate;bed.SCOM[2][1]*=remaingRate;bed.SCOM[2][2]*=remaingRate;bed.SCOM[2][3]*=remaingRate;bed.SCOM[2][4]*=remaingRate;
            flow.dissolved[4]*=remaingRate; flow.dissolved[5]*=remaingRate; flow.dissolved[6]*=remaingRate;
            
        }else{
        	//anaerobic
        	respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
       	  	remaingRate=0; GE=0;
        }
        
//        respirationC=0; miningC=0; //ggrowthC=0;
//        netNO3=0; netNH4=0; netPO4=0;
//        remaingRate=0; GE=0;
        
        bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;
    }
    
    public void ImmobilizeII(double GR, double BR, BedCell bed, FlowCell flow){
        
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material (Moorhead & Singsabaugh 2006; Paul and Clark 1997, Berg and McClaugherty 2003)
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        mortalityC = GR;
        mortalityC *= ref.capacity_1;
        mortalityC *= c;
        mortalityC *=(c+bed.miner.c);
        mortalityC /= primaryC;
        this.c-=mortalityC;
        
    	double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= BR*c;
        MaxRc *= ref.gResp_1;
        
        //<<------------------------------------------------ "decay"
        // idea is that  [V/(k2+C2)] / [V/(k3+C3)] = (k3+C3)/(k2+C2)
        // note that primaryC, primaryNC, primaryPC are for C:N:P ratios calculation
    	if(MaxRc > 0){
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
        	double rc3 = 1000000+bed.SCOM[0][0];
//            double rc23 = rc3; rc23 /= (10000+bed.SCOM[0][1]+bed.SCOM[0][2]);
//            double rc13 = rc3; rc13 /= (1000+bed.SCOM[0][3]+bed.SCOM[0][4]+flow.dissolved[4]*bed.benthicArea_1);
            double rc23 = rc3; rc23 /= (10000+bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            double rc13 = rc3; rc13 /= (1000+bed.SCOM[0][3]+bed.SCOM[0][4]);
            
            //something is wrong here, CN of combined food cannot exceed the rich CN (29)
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
        	primaryC=bed.SCOM[0][0];
            primaryC += rc23*(bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            primaryC += rc13*(bed.SCOM[0][3]+bed.SCOM[0][4]);
            primaryC_1=ref.ONE/primaryC;
            
            primaryNC=bed.SCOM[1][0];
            primaryNC += rc23*(bed.SCOM[1][1]+bed.SCOM[1][2]+flow.dissolved[5]*bed.benthicArea_1);
            primaryNC += rc13*(bed.SCOM[1][3]+bed.SCOM[1][4]);
            primaryNC*=primaryC_1;
            
            primaryPC=bed.SCOM[2][0];
            primaryPC += rc23*(bed.SCOM[2][1]+bed.SCOM[2][2]+flow.dissolved[6]*bed.benthicArea_1);
            primaryPC += rc13*(bed.SCOM[2][3]+bed.SCOM[2][4]);
            primaryPC*=primaryC_1;

        	secondNHR = 0.00188;
            secondNHR *= ref.timeStep;
            secondNHR = Math.min(secondNHR,flow.dissolved[1]*bed.benthicArea_1);
            secondNHR *= flow.dissolved[1];
            secondNHR /= (6*flow.vol+flow.dissolved[1]);
            secondN = 0.00188;
            secondN *= ref.timeStep;
            secondN = Math.min(secondN,flow.dissolved[2]*bed.benthicArea_1);
            secondN *= flow.dissolved[2];
            secondN /= (6*flow.vol+flow.dissolved[2]);
            secondN += secondNHR;
            if(secondN>0){secondNHR /= secondN;}

            secondP = 0.001;
            secondP *= ref.timeStep;
            secondP = Math.min(secondP,flow.dissolved[3]*bed.benthicArea_1);
            secondP *= flow.dissolved[3];
            secondP /= (flow.vol+flow.dissolved[3]);
            
        	//<<------------------------------------------------ "processing"
            double domc = Math.min(GR*this.c,MaxRc);
            double ncd = nc-primaryNC;
            double pcd = pc-primaryPC;
            if(ncd>0){domc = Math.min(domc, secondN/ncd);}
            if(pcd>0){domc = Math.min(domc, secondP/pcd);}
            domc = Math.min(primaryC,domc);

            //// how to correct domc*primaryC_1; <= 0.08 d-1
            
            
            ggrowthC = domc;
            respirationC = this.c*BR;
            respirationC += ref.gResp*ggrowthC;
            GE = (ggrowthC-respirationC);
            GE /= domc;
            
            //<<------------------------------------------------ "respiration" & "growth" 
            //this.c+=ggrowthC-respirationC;
            
                //------ "respiration"
            this.c-=respirationC;
            
                //------ DO
            flow.dissolved[0] -= respirationC*ref.O2_C;
            
                //------ N
            netN = ggrowthC*nc;
            netN -= domc*primaryNC;
            netN = Math.min(secondN, netN);
            
            if(netN>0){
                //uptake NH4 and NO3
                netNO3 = (1-secondNHR); netNO3 *= netN;
                netNH4 = secondNHR*netN;
                    //------ taking up NO3
                netNO3*=bed.benthicArea;
                flow.dissolved[2] -= netNO3;
                    //------ taking up/releasing NH4
                netNH4 -= respirationC*nc;
                netNH4 *= bed.benthicArea;
                flow.dissolved[1] -= netNH4;
            }else{
                //releasing NH4
                netNO3=0;
                netNH4 = netN;
                    //------ releasing NH4
                netNH4 -= respirationC*nc;
                netNH4 *= bed.benthicArea;
                flow.dissolved[1] -= netNH4;
            }
            

                //------ P
            netPO4 = ggrowthC*pc;
            netPO4 -= domc*primaryPC;
            netPO4 = Math.min(secondP, netPO4);
            if(netPO4>0){
                //uptake PO4
                    //------ taking up/releasing PO4
                netPO4 -= respirationC*pc;
                netPO4 *= bed.benthicArea;
                flow.dissolved[3] -= netPO4;
            }else{
                //release PO4
                    //------ releasing PO4
                netPO4 -= respirationC*pc;
                netPO4 *= bed.benthicArea;
                flow.dissolved[3] -= netPO4;
            }
            

                //------ finalizing
            this.c+=ggrowthC;
            
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            remaingRate2 = 1; remaingRate2 -= rc23*domc*primaryC_1;
            remaingRate3 = 1; remaingRate3 -= rc13*domc*primaryC_1;
            
            bed.SCOM[0][0]*=remaingRate;//lignin
            bed.SCOM[0][1]*=remaingRate2;//dead
            bed.SCOM[0][2]*=remaingRate2;//cell
            bed.SCOM[0][3]*=remaingRate3;//other
            bed.SCOM[0][4]*=remaingRate3;//leach
            
            bed.SCOM[1][0]*=remaingRate;//lignin 
            bed.SCOM[1][1]*=remaingRate2;//dead
            bed.SCOM[1][2]*=remaingRate2;//cell
            bed.SCOM[1][3]*=remaingRate3;//other
            bed.SCOM[1][4]*=remaingRate3;//leach
            
            bed.SCOM[2][0]*=remaingRate;//lignin
            bed.SCOM[2][1]*=remaingRate2;//dead
            bed.SCOM[2][2]*=remaingRate2;//cell
            bed.SCOM[2][3]*=remaingRate3;//other
            bed.SCOM[2][4]*=remaingRate3;//leach
            /*dom*/
            flow.dissolved[4]*=remaingRate3;
            flow.dissolved[5]*=remaingRate3;
            flow.dissolved[6]*=remaingRate3;
        }else{
        	//anaerobic
        	respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
       	  	remaingRate=0; GE=0;
        }
    	
        //<<------------------------------------------------ "update the death to OM"
        bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;
    }
    
    public void ImmobilizeIII(double GR, double BR, BedCell bed, FlowCell flow){
        
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material (Moorhead & Singsabaugh 2006; Paul and Clark 1997, Berg and McClaugherty 2003)
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        mortalityC = GR;
        mortalityC *= ref.capacity_1;
        mortalityC *= c;
        mortalityC *=(c+bed.miner.c);
        mortalityC /= primaryC;
        this.c-=mortalityC;
        
    	double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= BR*c;
        MaxRc *= ref.gResp_1;
        
        //<<------------------------------------------------ "decay"
    	if(MaxRc > 0){
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
            double rc3 = 1000000+bed.SCOM[0][0];
//            double rc23 = rc3; rc23/=(10000+bed.SCOM[0][1]+bed.SCOM[0][2]);
//            if(bed.SCOM[0][0]/(bed.SCOM[0][1]+bed.SCOM[0][2]+bed.SCOM[0][0])>=0.7){rc23=0.4285714;}
//            double rc13 = rc3; rc13/=(1000+bed.SCOM[0][3]+bed.SCOM[0][4]+flow.dissolved[4]*bed.benthicArea_1);//assume all DOC lab
            double rc23 = rc3; rc23/=(10000+bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            if(bed.SCOM[0][0]/(bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1+bed.SCOM[0][0])>=0.7){rc23=0.4285714;}
            double rc13 = rc3; rc13/=(1000+bed.SCOM[0][3]+bed.SCOM[0][4]);
            double rmax; //*= ref.micmax;
            //rmax=rc1; rmax+=rc2; rmax+=rc3;
            //rmax*=c; rmax*=ref.micmax;
            
            
            //something is wrong here, CN of combined food cannot exceed the rich CN (29)
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
        	primaryC=bed.SCOM[0][0];
            primaryC += rc23*(bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            primaryC += rc13*(bed.SCOM[0][3]+bed.SCOM[0][4]);
            primaryC_1=ref.ONE/primaryC;
            //rmax = primaryC * ref.micmax;
            
            primaryNC=bed.SCOM[1][0];
            primaryNC += rc23*(bed.SCOM[1][1]+bed.SCOM[1][2]+flow.dissolved[5]*bed.benthicArea_1);
            primaryNC += rc13*(bed.SCOM[1][3]+bed.SCOM[1][4]);
            primaryNC*=primaryC_1;
            
            primaryPC=bed.SCOM[2][0];
            primaryPC += rc23*(bed.SCOM[2][1]+bed.SCOM[2][2]+flow.dissolved[6]*bed.benthicArea_1);
            primaryPC += rc13*(bed.SCOM[2][3]+bed.SCOM[2][4]);
            primaryPC*=primaryC_1;
            
        	secondNHR = 0.00188;
            secondNHR *= ref.timeStep;
            secondNHR = Math.min(secondNHR,flow.dissolved[1]*bed.benthicArea_1);
            secondNHR *= flow.dissolved[1];
            secondNHR /= (6*flow.vol+flow.dissolved[1]);
            secondN = 0.00188;
            secondN *= ref.timeStep;
            secondN = Math.min(secondN,flow.dissolved[2]*bed.benthicArea_1);
            secondN *= flow.dissolved[2];
            secondN /= (6*flow.vol+flow.dissolved[2]);
            secondN += secondNHR;
            if(secondN>0){secondNHR /= secondN;}
            
            secondP = 0.001;
            secondP *= ref.timeStep;
            secondP = Math.min(secondP,flow.dissolved[3]*bed.benthicArea_1);
            secondP *= flow.dissolved[3];
            secondP /= (flow.vol+flow.dissolved[3]);
            
        	//<<------------------------------------------------ "processing"
            rmax = c*ref.micmax;
            double domc = Math.min(GR*this.c,MaxRc); domc = Math.min(rmax,domc);
            double ncd = nc-primaryNC;
            double pcd = pc-primaryPC;
            if(ncd>0){domc = Math.min(domc, secondN/ncd);}
            if(pcd>0){domc = Math.min(domc, secondP/pcd);}
            domc = Math.min(primaryC,domc);
            
            //// how to correct domc*primaryC_1; <= 0.08 d-1
            
            
            ggrowthC = domc;
            respirationC = this.c*BR;
            respirationC += ref.gResp*ggrowthC;
            GE = (ggrowthC-respirationC);
            GE /= domc;
            
            //<<------------------------------------------------ "respiration" & "growth"
            //this.c+=ggrowthC-respirationC;
            
            //------ "respiration"
            this.c-=respirationC;
            
            //------ DO
            flow.dissolved[0] -= respirationC*ref.O2_C;
            
            //------ N
            netN = ggrowthC*nc;
            netN -= domc*primaryNC;
            netN = Math.min(secondN, netN);
            
            if(netN>0){
                //uptake NH4 and NO3
                netNO3 = (1-secondNHR); netNO3 *= netN;
                netNH4 = secondNHR*netN;
                //------ taking up NO3
                netNO3*=bed.benthicArea;
                flow.dissolved[2] -= netNO3;
                //------ taking up/releasing NH4
                netNH4 -= respirationC*nc;
                netNH4 *= bed.benthicArea;
                flow.dissolved[1] -= netNH4;
            }else{
                //releasing NH4
                netNO3=0;
                netNH4 = netN;
                //------ releasing NH4
                netNH4 -= respirationC*nc;
                netNH4 *= bed.benthicArea;
                flow.dissolved[1] -= netNH4;
            }
            
            
            //------ P
            netPO4 = ggrowthC*pc;
            netPO4 -= domc*primaryPC;
            netPO4 = Math.min(secondP, netPO4);
            if(netPO4>0){
                //uptake PO4
                //------ taking up/releasing PO4
                netPO4 -= respirationC*pc;
                netPO4 *= bed.benthicArea;
                flow.dissolved[3] -= netPO4;
            }else{
                //release PO4
                //------ releasing PO4
                netPO4 -= respirationC*pc;
                netPO4 *= bed.benthicArea;
                flow.dissolved[3] -= netPO4;
            }
            
            
            //------ finalizing
            this.c+=ggrowthC;
            
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            remaingRate2 = 1; remaingRate2 -= rc23*domc*primaryC_1;
            remaingRate3 = 1; remaingRate3 -= rc13*domc*primaryC_1;
            
            bed.SCOM[0][0]*=remaingRate;//lignin
            bed.SCOM[0][1]*=remaingRate2;//dead
            bed.SCOM[0][2]*=remaingRate2;//cell
            bed.SCOM[0][3]*=remaingRate3;//other
            bed.SCOM[0][4]*=remaingRate3;//leach
            
            bed.SCOM[1][0]*=remaingRate;//lignin
            bed.SCOM[1][1]*=remaingRate2;//dead
            bed.SCOM[1][2]*=remaingRate2;//cell
            bed.SCOM[1][3]*=remaingRate3;//other
            bed.SCOM[1][4]*=remaingRate3;//leach
            
            bed.SCOM[2][0]*=remaingRate;//lignin
            bed.SCOM[2][1]*=remaingRate2;//dead
            bed.SCOM[2][2]*=remaingRate2;//cell
            bed.SCOM[2][3]*=remaingRate3;//other
            bed.SCOM[2][4]*=remaingRate3;//leach
            /*dom*/
            flow.dissolved[4]*=remaingRate3;
            flow.dissolved[5]*=remaingRate3;
            flow.dissolved[6]*=remaingRate3;
        }else{
        	//anaerobic
        	respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
       	  	remaingRate=0; GE=0;
        }
    	
        //<<------------------------------------------------ "update the death to OM"
        bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;
    }
    
    public void ImmobilizeIII_noDOC(double GR, double BR, BedCell bed, FlowCell flow){
        
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material (Moorhead & Singsabaugh 2006; Paul and Clark 1997, Berg and McClaugherty 2003)
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        mortalityC = GR;
        mortalityC *= ref.capacity_1;
        mortalityC *= c;
        mortalityC *=(c+bed.miner.c);
        mortalityC /= primaryC;
        this.c-=mortalityC;
        
        double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= BR*c;
        MaxRc *= ref.gResp_1;
        
        //<<------------------------------------------------ "decay"
        if(MaxRc > 0){
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
            double rc3 = 1000000+bed.SCOM[0][0];
            double rc23 = rc3; rc23/=(10000+bed.SCOM[0][1]+bed.SCOM[0][2]);
            if(bed.SCOM[0][0]/(bed.SCOM[0][1]+bed.SCOM[0][2]+bed.SCOM[0][0])>=0.7){rc23=0.4285714;}
            double rc13 = rc3; rc13/=(1000+bed.SCOM[0][3]+bed.SCOM[0][4]);
            double rmax; //*= ref.micmax;
            //rmax=rc1; rmax+=rc2; rmax+=rc3;
            //rmax*=c; rmax*=ref.micmax;
            
            
            //something is wrong here, CN of combined food cannot exceed the rich CN (29)
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
            primaryC=bed.SCOM[0][0];
            primaryC += rc23*(bed.SCOM[0][1]+bed.SCOM[0][2]);
            primaryC += rc13*(bed.SCOM[0][3]+bed.SCOM[0][4]);
            primaryC_1=ref.ONE/primaryC;
            //rmax = primaryC * ref.micmax;
            
            primaryNC=bed.SCOM[1][0];
            primaryNC += rc23*(bed.SCOM[1][1]+bed.SCOM[1][2]);
            primaryNC += rc13*(bed.SCOM[1][3]+bed.SCOM[1][4]);
            primaryNC*=primaryC_1;
            
            primaryPC=bed.SCOM[2][0];
            primaryPC += rc23*(bed.SCOM[2][1]+bed.SCOM[2][2]);
            primaryPC += rc13*(bed.SCOM[2][3]+bed.SCOM[2][4]);
            primaryPC*=primaryC_1;
            
            secondNHR = 0.00188;
            secondNHR *= ref.timeStep;
            secondNHR = Math.min(secondNHR,flow.dissolved[1]*bed.benthicArea_1);//NH4
            secondNHR *= flow.dissolved[1];
            secondNHR /= (6*flow.vol+flow.dissolved[1]);
            secondN = 0.00188;
            secondN *= ref.timeStep;
            secondN = Math.min(secondN,flow.dissolved[2]*bed.benthicArea_1);//NO3
            secondN *= flow.dissolved[2];
            secondN /= (6*flow.vol+flow.dissolved[2]);
            secondN += secondNHR;
            if(secondN>0){secondNHR /= secondN;}
            
            secondP = 0.001;
            secondP *= ref.timeStep;
            secondP = Math.min(secondP,flow.dissolved[3]*bed.benthicArea_1);
            secondP *= flow.dissolved[3];
            secondP /= (flow.vol+flow.dissolved[3]);
            
            //<<------------------------------------------------ "processing"
            rmax = c*ref.micmax;
            double domc = Math.min(GR*this.c,MaxRc); domc = Math.min(rmax,domc);
            double ncd = nc-primaryNC;
            double pcd = pc-primaryPC;
            if(ncd>0){domc = Math.min(domc, secondN/ncd);}
            if(pcd>0){domc = Math.min(domc, secondP/pcd);}
            domc = Math.min(primaryC,domc);
            
            //// how to correct domc*primaryC_1; <= 0.08 d-1
            
            
            ggrowthC = domc;
            respirationC = this.c*BR;
            respirationC += ref.gResp*ggrowthC;
            GE = (ggrowthC-respirationC);
            GE /= domc;
            
            //<<------------------------------------------------ "respiration" & "growth"
            //this.c+=ggrowthC-respirationC;
            
            //------ "respiration"
            this.c-=respirationC;
            
            //------ DO
            flow.dissolved[0] -= respirationC*ref.O2_C;
            
            //------ N
            netN = ggrowthC*nc;
            netN -= domc*primaryNC;
            netN = Math.min(secondN, netN);
            
            if(netN>0){
                //uptake NH4 and NO3
                netNO3 = (1-secondNHR); netNO3 *= netN;
                netNH4 = secondNHR*netN;
                //------ taking up NO3
                netNO3*=bed.benthicArea;
                flow.dissolved[2] -= netNO3;
                //------ taking up/releasing NH4
                netNH4 -= respirationC*nc;
                netNH4 *= bed.benthicArea;
                flow.dissolved[1] -= netNH4;
            }else{
                //releasing NH4
                netNO3=0;
                netNH4 = netN;
                //------ releasing NH4
                netNH4 -= respirationC*nc;
                netNH4 *= bed.benthicArea;
                flow.dissolved[1] -= netNH4;
            }
            
            
            //------ P
            netPO4 = ggrowthC*pc;
            netPO4 -= domc*primaryPC;
            netPO4 = Math.min(secondP, netPO4);
            if(netPO4>0){
                //uptake PO4
                //------ taking up/releasing PO4
                netPO4 -= respirationC*pc;
                netPO4 *= bed.benthicArea;
                flow.dissolved[3] -= netPO4;
            }else{
                //release PO4
                //------ releasing PO4
                netPO4 -= respirationC*pc;
                netPO4 *= bed.benthicArea;
                flow.dissolved[3] -= netPO4;
            }
            
            
            //------ finalizing
            this.c+=ggrowthC;
            
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            remaingRate2 = 1; remaingRate2 -= rc23*domc*primaryC_1;
            remaingRate3 = 1; remaingRate3 -= rc13*domc*primaryC_1;
            
            bed.SCOM[0][0]*=remaingRate;//lignin
            bed.SCOM[0][1]*=remaingRate2;//dead
            bed.SCOM[0][2]*=remaingRate2;//cell
            bed.SCOM[0][3]*=remaingRate3;//other
            bed.SCOM[0][4]*=remaingRate3;//leach
            
            bed.SCOM[1][0]*=remaingRate;//lignin
            bed.SCOM[1][1]*=remaingRate2;//dead
            bed.SCOM[1][2]*=remaingRate2;//cell
            bed.SCOM[1][3]*=remaingRate3;//other
            bed.SCOM[1][4]*=remaingRate3;//leach
            
            bed.SCOM[2][0]*=remaingRate;//lignin
            bed.SCOM[2][1]*=remaingRate2;//dead
            bed.SCOM[2][2]*=remaingRate2;//cell
            bed.SCOM[2][3]*=remaingRate3;//other
            bed.SCOM[2][4]*=remaingRate3;//leach
            /*dom*/
            flow.dissolved[4]*=remaingRate3;
            flow.dissolved[5]*=remaingRate3;
            flow.dissolved[6]*=remaingRate3;
        }else{
            //anaerobic
            respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
            remaingRate=0; GE=0;
        }
        
        //<<------------------------------------------------ "update the death to OM"
        bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;
    }
    
  
    public void MiningI(double GR, double BR, BedCell bed, FlowCell flow){
        //calculate all available resources from the water column 
    	
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        
        mortalityC = GR;
        mortalityC *= ref.capacity_1;
        mortalityC *= c;
        mortalityC *=(c+bed.immobilizer.c);
        mortalityC /= primaryC;
        //this.c-=mortalityC;
        
    	double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= ref.mResp*c;

        c = GR;
        //this.c *= (1+GR);
        //this.c *= 1.0000001;
        /*
    	if(MaxRc > 0){
        	//[0:age*mass, 1-3:dead, 4-6:other, 7-9:cellulose, 10-12:lignin, 13-15:leach]
        	primaryC += flow.dissolved[4]*bed.benthicArea_1;
            primaryC_1=ref.ONE/primaryC;
            
        	primaryNC = bed.SCOM[1][0];
            primaryNC += bed.SCOM[1][1];
            primaryNC += bed.SCOM[1][2];
            primaryNC += bed.SCOM[1][3];
            primaryNC += bed.SCOM[1][4];
            primaryNC += flow.dissolved[5]*bed.benthicArea_1;
            
            primaryPC = bed.SCOM[2][0];
            primaryPC += bed.SCOM[2][1];
            primaryPC += bed.SCOM[2][2];
            primaryPC += bed.SCOM[2][3];
            primaryPC += bed.SCOM[2][4];
            primaryPC += flow.dissolved[6]*bed.benthicArea_1;
            
            primaryNC *= primaryC_1; primaryPC *= primaryC_1;
        	
            double domc=0, row=0;
            double rowp = cp*primaryPC, rown = cn*primaryNC;
            double domcp = Math.min(GR*this.c/rowp,MaxRc/(rowp*ref.gResp+1-rowp));
            double domcn = Math.min(GR*this.c/rown,MaxRc/(rown*ref.gResp+1-rown));
            
            row=Math.min(rowp, rown); domc=Math.max(domcp, domcn);
            domc = Math.min(domc, primaryC);
            
            ggrowthC = row*domc;
            respirationC = this.c*ref.mResp;
            respirationC += ref.gResp*ggrowthC;
            miningC=(1-row)*domc;
            GE=GR*c;
            remaingRate2 = rowp;
            remaingRate3 = rown;
            //GE = (ggrowthC-respirationC);
            //GE /= domc;
            
            this.c+=ggrowthC-respirationC;
            
            flow.dissolved[0] -= (respirationC+miningC)*ref.O2_C;
            netN = Math.min(0, ggrowthC*nc-domc*primaryNC);
            netNH4 = netN - respirationC*nc;
            netNH4 *= bed.benthicArea;
            flow.dissolved[1] -= netNH4;
            netNO3=0;
            netPO4 = Math.min(0, ggrowthC*pc-domc*primaryPC)-respirationC*pc;
            netPO4 *= bed.benthicArea;
            flow.dissolved[3] -= netPO4;
            
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            bed.SCOM[0][0]*=remaingRate;bed.SCOM[0][1]*=remaingRate;bed.SCOM[0][2]*=remaingRate;bed.SCOM[0][3]*=remaingRate;bed.SCOM[0][4]*=remaingRate;
            bed.SCOM[1][0]*=remaingRate;bed.SCOM[1][1]*=remaingRate;bed.SCOM[1][2]*=remaingRate;bed.SCOM[1][3]*=remaingRate;bed.SCOM[1][4]*=remaingRate;
            bed.SCOM[2][0]*=remaingRate;bed.SCOM[2][1]*=remaingRate;bed.SCOM[2][2]*=remaingRate;bed.SCOM[2][3]*=remaingRate;bed.SCOM[2][4]*=remaingRate;
            flow.dissolved[4]*=remaingRate; flow.dissolved[5]*=remaingRate; flow.dissolved[6]*=remaingRate;
        }else{
        	//anaerobic
        	respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
       	  	remaingRate=0; GE=0;
        }
        */
        
        respirationC=0; miningC=0; ggrowthC=0;
        netNO3=0; netNH4=0; netPO4=0;
        remaingRate=0; GE=0;
        
    	//bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;

    }
    
    public void MiningII(double GR, double BR, BedCell bed, FlowCell flow){
        //calculate all available resources from the water column 
    	
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        mortalityC = GR;
        mortalityC *= ref.capacity_1;
        mortalityC *= c;
        mortalityC *=(c+bed.immobilizer.c);
        mortalityC /= primaryC;
        this.c-=mortalityC;
        
        //double MaxRc = ref.C_O2*flow.dissolved[0];
        double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= BR*c;
        
        //<<------------------------------------------------ "decay"
        if(MaxRc > 0){
        	double rc3 = 100000+bed.SCOM[0][0];
//            double rc23 = rc3; rc23 /= (10000+bed.SCOM[0][1]+bed.SCOM[0][2]);
//            double rc13 = rc3; rc13 /= (1000+bed.SCOM[0][3]+bed.SCOM[0][4]+flow.dissolved[4]*bed.benthicArea_1);
            double rc23 = rc3; rc23 /= (10000+bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            double rc13 = rc3; rc13 /= (1000+bed.SCOM[0][3]+bed.SCOM[0][4]);
            
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
        	primaryC=bed.SCOM[0][0];
            primaryC += rc23*(bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            primaryC += rc13*(bed.SCOM[0][3]+bed.SCOM[0][4]);
            primaryC_1=ref.ONE/primaryC;
            
            primaryNC=bed.SCOM[1][0];
            primaryNC += rc23*(bed.SCOM[1][1]+bed.SCOM[1][2]+flow.dissolved[5]*bed.benthicArea_1);
            primaryNC += rc13*(bed.SCOM[1][3]+bed.SCOM[1][4]);
            primaryNC*=primaryC_1;
            
            primaryPC=bed.SCOM[2][0];
            primaryPC += rc23*(bed.SCOM[2][1]+bed.SCOM[2][2]+flow.dissolved[6]*bed.benthicArea_1);
            primaryPC += rc13*(bed.SCOM[2][3]+bed.SCOM[2][4]);
            primaryPC*=primaryC_1; 

            //<<------------------------------------------------ "processing" 
            double domc=0, row=0;
            double rowp = cp*primaryPC, rown = cn*primaryNC;
            if(rowp>1){rowp=1.0;}
            if(rown>1){rown=1.0;}
            double domcp = Math.min(GR*this.c/rowp,MaxRc/(rowp*ref.gResp+1-rowp));
            double domcn = Math.min(GR*this.c/rown,MaxRc/(rown*ref.gResp+1-rown));
            
            row=Math.min(rowp, rown); domc=Math.max(domcp, domcn);
            domc = Math.min(domc, primaryC);
            
            ggrowthC = row*domc;
            respirationC = this.c*BR;
            respirationC += ref.gResp*ggrowthC;
            miningC=(1-row)*domc;
            GE = (ggrowthC-respirationC);
            GE /= domc;
            
            //<<------------------------------------------------ "respiration" & "growth" 
            //this.c+=ggrowthC-respirationC;
            
                //------ "respiration"
            this.c-=respirationC;

                //------ DO
            flow.dissolved[0] -= (respirationC+miningC)*ref.O2_C;
            
                //------ N release
            netN = Math.min(0, ggrowthC*nc-domc*primaryNC);
            netNH4 = netN;
            netNO3=0;
                    //------ releasing
            netNH4-=respirationC*nc;
            netNH4 *= bed.benthicArea;
            flow.dissolved[1] -= netNH4;
            
                //------ P release (still inf? how?)
            netPO4 = Math.min(0, ggrowthC*pc-domc*primaryPC);
                    //------ releasing
            netPO4-=respirationC*pc;
            netPO4 *= bed.benthicArea;
            flow.dissolved[3] -= netPO4;
                //------ finalizing
            this.c+=ggrowthC;
            
            //[0:age*mass, 1-3:dead, 4-6:other, 7-9:cellulose, 10-12:lignin, 13-15:leach]
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            remaingRate2 = 1; remaingRate2 -= rc23*domc*primaryC_1;
            remaingRate3 = 1; remaingRate3 -= rc13*domc*primaryC_1;
            
            bed.SCOM[0][0]*=remaingRate;//lignin
            bed.SCOM[0][1]*=remaingRate2;//dead
            bed.SCOM[0][2]*=remaingRate2;//cell
            bed.SCOM[0][3]*=remaingRate3;//other
            bed.SCOM[0][4]*=remaingRate3;//leach
            
            bed.SCOM[1][0]*=remaingRate;//lignin
            bed.SCOM[1][1]*=remaingRate2;//dead
            bed.SCOM[1][2]*=remaingRate2;//cell
            bed.SCOM[1][3]*=remaingRate3;//other
            bed.SCOM[1][4]*=remaingRate3;//leach
            
            bed.SCOM[2][0]*=remaingRate;//lignin
            bed.SCOM[2][1]*=remaingRate2;//dead
            bed.SCOM[2][2]*=remaingRate2;//cell
            bed.SCOM[2][3]*=remaingRate3;//other
            bed.SCOM[2][4]*=remaingRate3;//leach
            /*dom*/
            flow.dissolved[4]*=remaingRate3;
            flow.dissolved[5]*=remaingRate3;
            flow.dissolved[6]*=remaingRate3;
        }else{
        	//anaerobic
        	respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
       	  	remaingRate=0; GE=0;
        }
        
        //<<------------------------------------------------ "update the death to OM"
        bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;
    }
    
    public void MiningIII(double GR, double BR, BedCell bed, FlowCell flow){
        //calculate all available resources from the water column
    	
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        mortalityC = GR;
        mortalityC *= ref.capacity_1;
        mortalityC *= c;
        mortalityC *=(c+bed.immobilizer.c);
        mortalityC /= primaryC;
        this.c-=mortalityC;
        
        //double MaxRc = ref.C_O2*flow.dissolved[0];
        double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= BR*c;
        
        //<<------------------------------------------------ "decay"
        if(MaxRc > 0){
            double rc3 = 100000+bed.SCOM[0][0];
//            double rc23 = rc3; rc23/=(10000+bed.SCOM[0][1]+bed.SCOM[0][2]);
//            if(bed.SCOM[0][0]/(bed.SCOM[0][1]+bed.SCOM[0][2]+bed.SCOM[0][0])>=0.7){rc23=0.4285714;}
//            double rc13 = rc3; rc13/=(1000+bed.SCOM[0][3]+bed.SCOM[0][4]+flow.dissolved[4]*bed.benthicArea_1);//assume all DOC lab
            double rc23 = rc3; rc23/=(10000+bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            if(bed.SCOM[0][0]/(bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1+bed.SCOM[0][0])>=0.7){rc23=0.4285714;}
            double rc13 = rc3; rc13/=(1000+bed.SCOM[0][3]+bed.SCOM[0][4]);
            double rmax;
            //rmax=rc1; rmax+=rc2; rmax+=rc3;
            //rmax*=c; rmax*=ref.micmax;
            
            
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
        	primaryC=bed.SCOM[0][0];
            primaryC += rc23*(bed.SCOM[0][1]+bed.SCOM[0][2]+flow.dissolved[4]*bed.benthicArea_1);
            primaryC += rc13*(bed.SCOM[0][3]+bed.SCOM[0][4]);
            primaryC_1=ref.ONE/primaryC;
            //rmax = primaryC * ref.micmax;
            
            primaryNC=bed.SCOM[1][0];
            primaryNC += rc23*(bed.SCOM[1][1]+bed.SCOM[1][2]+flow.dissolved[5]*bed.benthicArea_1);
            primaryNC += rc13*(bed.SCOM[1][3]+bed.SCOM[1][4]);
            primaryNC*=primaryC_1;
            
            primaryPC=bed.SCOM[2][0];
            primaryPC += rc23*(bed.SCOM[2][1]+bed.SCOM[2][2]+flow.dissolved[6]*bed.benthicArea_1);
            primaryPC += rc13*(bed.SCOM[2][3]+bed.SCOM[2][4]);
            primaryPC*=primaryC_1;
            
            //<<------------------------------------------------ "processing"
            rmax = c*ref.micmax;
            double domc=0, row=0;
            double rowp = cp*primaryPC, rown = cn*primaryNC;
            if(rowp>1){rowp=1.0;}
            if(rown>1){rown=1.0;}
            double domcp = Math.min(GR*this.c/rowp,MaxRc/(rowp*ref.gResp+1-rowp));
            double domcn = Math.min(GR*this.c/rown,MaxRc/(rown*ref.gResp+1-rown));
            
            row=Math.min(rowp, rown); domc=Math.max(domcp, domcn);
            domc = Math.min(domc, primaryC); domc = Math.min(domc, rmax);
            
            ggrowthC = row*domc;
            respirationC = this.c*BR;
            respirationC += ref.gResp*ggrowthC;
            miningC=(1-row)*domc;
            GE = (ggrowthC-respirationC);
            GE /= domc;
            
            //<<------------------------------------------------ "respiration" & "growth"
            //this.c+=ggrowthC-respirationC;
            
            //------ "respiration"
            this.c-=respirationC;
            
            //------ DO
            flow.dissolved[0] -= (respirationC+miningC)*ref.O2_C;
            
            //------ N release
            netN = Math.min(0, ggrowthC*nc-domc*primaryNC);
            netNH4 = netN;
            netNO3=0;
            //------ releasing
            netNH4-=respirationC*nc;
            netNH4 *= bed.benthicArea;
            flow.dissolved[1] -= netNH4;
            
            //------ P release (still inf? how?)
            netPO4 = Math.min(0, ggrowthC*pc-domc*primaryPC);
            //------ releasing
            netPO4-=respirationC*pc;
            netPO4 *= bed.benthicArea;
            flow.dissolved[3] -= netPO4;
            //------ finalizing
            this.c+=ggrowthC;
            
            //[0:age*mass, 1-3:dead, 4-6:other, 7-9:cellulose, 10-12:lignin, 13-15:leach]
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            remaingRate2 = 1; remaingRate2 -= rc23*domc*primaryC_1;
            remaingRate3 = 1; remaingRate3 -= rc13*domc*primaryC_1;
            
            bed.SCOM[0][0]*=remaingRate;//lignin
            bed.SCOM[0][1]*=remaingRate2;//dead
            bed.SCOM[0][2]*=remaingRate2;//cell
            bed.SCOM[0][3]*=remaingRate3;//other
            bed.SCOM[0][4]*=remaingRate3;//leach
            
            bed.SCOM[1][0]*=remaingRate;//lignin
            bed.SCOM[1][1]*=remaingRate2;//dead
            bed.SCOM[1][2]*=remaingRate2;//cell
            bed.SCOM[1][3]*=remaingRate3;//other
            bed.SCOM[1][4]*=remaingRate3;//leach
            
            bed.SCOM[2][0]*=remaingRate;//lignin
            bed.SCOM[2][1]*=remaingRate2;//dead
            bed.SCOM[2][2]*=remaingRate2;//cell
            bed.SCOM[2][3]*=remaingRate3;//other
            bed.SCOM[2][4]*=remaingRate3;//leach
            /*dom*/
            flow.dissolved[4]*=remaingRate3;
            flow.dissolved[5]*=remaingRate3;
            flow.dissolved[6]*=remaingRate3;
        }else{
        	//anaerobic
        	respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
       	  	remaingRate=0; GE=0;
        }
        
        //<<------------------------------------------------ "update the death to OM"
        bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;
    }
    
    public void MiningIII_noDOC(double GR, double BR, BedCell bed, FlowCell flow){
        //calculate all available resources from the water column
        
        //possible GrossGrowth and aerobic respiration given DO (energy for GrossGrowth and maintenance)
        //always 10% or less microbial biomass on leaf material
        primaryC = bed.SCOM[0][0];
        primaryC += bed.SCOM[0][1];
        primaryC += bed.SCOM[0][2];
        primaryC += bed.SCOM[0][3];
        primaryC += bed.SCOM[0][4];
        mortalityC = GR;
        mortalityC *= ref.capacity_1;
        mortalityC *= c;
        mortalityC *=(c+bed.immobilizer.c);
        mortalityC /= primaryC;
        this.c-=mortalityC;
        
        //double MaxRc = ref.C_O2*flow.dissolved[0];
        double MaxRc = ref.C_O2*flow.dissolved[0];
        MaxRc -= BR*c;
        
        //<<------------------------------------------------ "decay"
        if(MaxRc > 0){
            double rc3 = 100000+bed.SCOM[0][0];
            double rc23 = rc3; rc23/=(10000+bed.SCOM[0][1]+bed.SCOM[0][2]);
            if(bed.SCOM[0][0]/(bed.SCOM[0][1]+bed.SCOM[0][2]+bed.SCOM[0][0])>=0.7){rc23=0.4285714;}
            double rc13 = rc3; rc13/=(1000+bed.SCOM[0][3]+bed.SCOM[0][4]);
            double rmax;
            //rmax=rc1; rmax+=rc2; rmax+=rc3;
            //rmax*=c; rmax*=ref.micmax;
            
            
            //[C,N,P][0:lig, 1:dead, 2:cell, 3:other, 4:leach]
            primaryC=bed.SCOM[0][0];
            primaryC += rc23*(bed.SCOM[0][1]+bed.SCOM[0][2]);
            primaryC += rc13*(bed.SCOM[0][3]+bed.SCOM[0][4]);
            primaryC_1=ref.ONE/primaryC;
            //rmax = primaryC * ref.micmax;
            
            primaryNC=bed.SCOM[1][0];
            primaryNC += rc23*(bed.SCOM[1][1]+bed.SCOM[1][2]);
            primaryNC += rc13*(bed.SCOM[1][3]+bed.SCOM[1][4]);
            primaryNC*=primaryC_1;
            
            primaryPC=bed.SCOM[2][0];
            primaryPC += rc23*(bed.SCOM[2][1]+bed.SCOM[2][2]);
            primaryPC += rc13*(bed.SCOM[2][3]+bed.SCOM[2][4]);
            primaryPC*=primaryC_1;
            
            //<<------------------------------------------------ "processing"
            rmax = c*ref.micmax;
            double domc=0, row=0;
            double rowp = cp*primaryPC, rown = cn*primaryNC;
            if(rowp>1){rowp=1.0;}
            if(rown>1){rown=1.0;}
            double domcp = Math.min(GR*this.c/rowp,MaxRc/(rowp*ref.gResp+1-rowp));
            double domcn = Math.min(GR*this.c/rown,MaxRc/(rown*ref.gResp+1-rown));
            
            row=Math.min(rowp, rown); domc=Math.max(domcp, domcn);
            domc = Math.min(domc, primaryC); domc = Math.min(domc, rmax);
            
            ggrowthC = row*domc;
            respirationC = this.c*BR;
            respirationC += ref.gResp*ggrowthC;
            miningC=(1-row)*domc;
            GE = (ggrowthC-respirationC);
            GE /= domc;
            
            //<<------------------------------------------------ "respiration" & "growth"
            //this.c+=ggrowthC-respirationC;
            
            //------ "respiration"
            this.c-=respirationC;
            
            //------ DO
            flow.dissolved[0] -= (respirationC+miningC)*ref.O2_C;
            
            //------ N release
            netN = Math.min(0, ggrowthC*nc-domc*primaryNC);
            netNH4 = netN;
            netNO3=0;
            //------ releasing
            netNH4-=respirationC*nc;
            netNH4 *= bed.benthicArea;
            flow.dissolved[1] -= netNH4;
            
            //------ P release (still inf? how?)
            netPO4 = Math.min(0, ggrowthC*pc-domc*primaryPC);
            //------ releasing
            netPO4-=respirationC*pc;
            netPO4 *= bed.benthicArea;
            flow.dissolved[3] -= netPO4;
            //------ finalizing
            this.c+=ggrowthC;
            
            //[0:age*mass, 1-3:dead, 4-6:other, 7-9:cellulose, 10-12:lignin, 13-15:leach]
            remaingRate = 1; remaingRate -= domc*primaryC_1;
            remaingRate2 = 1; remaingRate2 -= rc23*domc*primaryC_1;
            remaingRate3 = 1; remaingRate3 -= rc13*domc*primaryC_1;
            
            bed.SCOM[0][0]*=remaingRate;//lignin
            bed.SCOM[0][1]*=remaingRate2;//dead
            bed.SCOM[0][2]*=remaingRate2;//cell
            bed.SCOM[0][3]*=remaingRate3;//other
            bed.SCOM[0][4]*=remaingRate3;//leach
            
            bed.SCOM[1][0]*=remaingRate;//lignin
            bed.SCOM[1][1]*=remaingRate2;//dead
            bed.SCOM[1][2]*=remaingRate2;//cell
            bed.SCOM[1][3]*=remaingRate3;//other
            bed.SCOM[1][4]*=remaingRate3;//leach
            
            bed.SCOM[2][0]*=remaingRate;//lignin
            bed.SCOM[2][1]*=remaingRate2;//dead
            bed.SCOM[2][2]*=remaingRate2;//cell
            bed.SCOM[2][3]*=remaingRate3;//other
            bed.SCOM[2][4]*=remaingRate3;//leach
            /*dom*/
            flow.dissolved[4]*=remaingRate3;
            flow.dissolved[5]*=remaingRate3;
            flow.dissolved[6]*=remaingRate3;
        }else{
            //anaerobic
            respirationC=0; miningC=0; ggrowthC=0;
            netNO3=0; netNH4=0; netPO4=0;
            remaingRate=0; GE=0;
        }
        
        //<<------------------------------------------------ "update the death to OM"
        bed.SCOM[0][1]+=mortalityC;bed.SCOM[1][1]+=mortalityC*nc;bed.SCOM[2][1]+=mortalityC*pc;
    }
    //**********************************************************//
    
    
    public String toString(){
        return String.format("%E,%E, %E,%E,%E,%E,%E,%E, %.4f,%.4f,%.4f, %.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                             //"%E,%E, %E,%E,%E,%E,%E,%E, %E,%E,%E, %E,%E,%E,%E,%E,%E",
                             c, GE,
                             (respirationC+miningC)*ref.SECOND_TO_timeStepRATE,//second as time unit, in unit area
                             respirationC*nc*ref.SECOND_TO_timeStepRATE,//second as time unit, in unit area
                             netNH4*ref.SECOND_TO_timeStepRATE,//second as time unit, in full bed
                             netNO3*ref.SECOND_TO_timeStepRATE,//second as time unit, in full bed
                             respirationC*pc*ref.SECOND_TO_timeStepRATE,//second as time unit, in unit area
                             netPO4*ref.SECOND_TO_timeStepRATE,//second as time unit, in full bed
                             0.0,0.0,0.0,
                             0.0,0.0,0.0,
                             0.0,0.0,0.0
                             );
//        return String.format("%E,%E, %E,%E,%E,%E,%E,%E, %.4f,%.4f,%.4f, %.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
//                             //"%E,%E, %E,%E,%E,%E,%E,%E, %E,%E,%E, %E,%E,%E,%E,%E,%E",
//                             c, GE,
//                             (respirationC+miningC)*ref.SECOND_TO_timeStepRATE,
//                             respirationC*nc*ref.SECOND_TO_timeStepRATE,
//                             netNH4*ref.SECOND_TO_timeStepRATE,
//                             netNO3*ref.SECOND_TO_timeStepRATE,
//                             respirationC*pc*ref.SECOND_TO_timeStepRATE,
//                             netPO4*ref.SECOND_TO_timeStepRATE,
//                             age[0]*ref.DAILY_TO_timeStepRATE,age[1]*ref.DAILY_TO_timeStepRATE,age[2]*ref.DAILY_TO_timeStepRATE,
//                             scN[0],scN[1],1-scN[0]-scN[1],
//                             scP[0],scP[1],1-scP[0]-scP[1]
//                             );
    }

}
