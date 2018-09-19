package it.uniparthenope.meteo;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jncregridder.util.InterpolatorException;
import jncregridder.util.NCRegridderException;
import jncregridder.wrf.WRFData;
import ucar.ma2.InvalidRangeException;


public class WRF2WRF {
    private double[] interpLevels = new double[] {1000.,950.,900.,850.,800.,750.,700.,650.,600.,550.,500.,450.,400.,350.,300.,250.,200.,150.,100};

    private String removeList="ZNU,ZNW,ZS,DZS,HFX_FORCE,LH_FORCE,TSK_FORCE,HFX_FORCE_TEND,LH_FORCE_TEND,TSK_FORCE_TEND,FNM,FNP,RDNW,RDN,DNW,DN,CFN,CFN1,THIS_IS_AN_IDEAL_RUN,RDX,RDY,RESM,ZETATOP,CF1,CF2,CF3,ITIMESTEP,XTIME,P_TOP,T00,P00,TLP,TISO,TLP_STRAT,P_STRAT,MAX_MSTFX,MAX_MSTFY,SAVE_TOPO_FROM_REAL,ISEEDARR_SPPT,ISEEDARR_SKEBS,ISEEDARR_RAND_PERTURB,ISEEDARRAY_SPP_CONV,ISEEDARRAY_SPP_PBL,ISEEDARRAY_SPP_LSM,BF,C1H,C2H,BH,C1F,C2F,C3H,C4H,C3F,C4F";
    private String addList="U10M,V10M,SLP,CRH";
    public static void main(String[] args) throws IOException, NCRegridderException, InterpolatorException {




        try {

            if (args.length != 2) {
                System.out.println("Usage:");
                System.out.println("WRF2WRF wrfFilenameIn wrfFilenameOut");
                System.exit(0);
            }
            new WRF2WRF(args[1],args[2]);

        } catch (InvalidRangeException ex) {
            Logger.getLogger(WRF2ROMS.class.getName()).log(Level.SEVERE, null, ex);
        }




    }

    public WRF2WRF(String wrfDataFilenameIn, String wrfDataFilenameOut) throws IOException, InvalidRangeException, NCRegridderException, InterpolatorException {

        WRFData wrfData = new WRFData(wrfDataFilenameIn,WRFData.INTERPMETHOD_DEFAULT,interpLevels);

    }
}
