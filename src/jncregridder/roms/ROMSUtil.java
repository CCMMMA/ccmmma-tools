package jncregridder.roms;

public class ROMSUtil {
    public static void rotate(double[][] u, double[][] v, double[][] angle, double missingValue) {
        double rotU, rotV;

        // Fpr each j (eta axis)...
        for (int j=0;j<v.length;j++) {
            // For each i (xi axis)...
            for (int i=0;i<u[j].length;i++) {
                // Check if all velues are not NaN and not a missing value
                if (
                        Double.isNaN(u[j][i])==false &&
                                Double.isNaN(v[j][i])==false &&
                                Double.isNaN(angle[j][i]) == false &&
                                u[j][i] != missingValue &&
                                v[j][i] != missingValue &&
                                angle[j][i] != missingValue
                ) {
                    /* Old
                    rotU = (u[j][i] * Math.cos(angle[j][i]) - v[j][i] * Math.sin(angle[j][i]));
                    rotV = (v[j][i] * Math.cos(angle[j][i]) + u[j][i] * Math.sin(angle[j][i]));
                    */

                    /*

                    https://www.myroms.org/forum/viewtopic.php?f=3&t=295

                    Fortran:
                    u(XI,ETA)=u(LON,LAT)*cos(angle(i,j))+v(LON,LAT)*sin(angle(i,j))
                    v(XI,ETA)=u(LON,LAT)*sin(angle(i,j))-v(LON,LAT)*cos(angle(i,j))

                    C:
                    u(ETA,XI)=u(LAT,LON)*cos(angle(j,i))+v(LAT,LON)*sin(angle(j,i))
                    v(ETA,XI)=u(LAT,LON)*sin(angle(j,i))-v(LAT,LON)*cos(angle(j,i))
                    */

                    rotU = (u[j][i] * Math.cos(angle[j][i]) + v[j][i] * Math.sin(angle[j][i]));
                    rotV = (v[j][i] * Math.cos(angle[j][i]) - u[j][i] * Math.sin(angle[j][i]));

                    u[j][i]=rotU;
                    v[j][i]=rotV;
                }
            }

        }

    }
}
