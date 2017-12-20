/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jncregridder.util;

import java.util.Calendar;

/**
 *
 * @author raffaelemontella
 */
public class JulianDate {
     /**
  * Returns the Julian day number that begins at noon of
  * this day, Positive year signifies A.D., negative year B.C.
  * Remember that the year after 1 B.C. was 1 A.D.
  *
  * ref :
  *  Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
  */
  // Gregorian Calendar adopted Oct. 15, 1582 (2299161)
  public static int JGREG= 15 + 31*(10+12*1582);
  public static double HALFSECOND = 0.5;


  public static double toModJulian(Calendar cal) {
      return toJulian(cal)-get19680523();
  }
  public static double get19680523() {
      return toJulian(new int[] {1968, 5, 23,0 });
  }
  public static double get19700101() {
      return toJulian(new int[] {1970, 1, 1,0 });
  }
  
  public static double toJulian(Calendar cal) {
      int[] ymdh = {
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH)+1,
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR_OF_DAY)
      };
      return toJulian(ymdh);
  }
  
  public static double toJulian(int[] ymdh) {
   int year=ymdh[0];
   int month=ymdh[1]; // jan=1, feb=2,...
   int day=ymdh[2];
   int hour=ymdh[3];
   int julianYear = year;
   if (year < 0) julianYear++;
   int julianMonth = month;
   if (month > 2) {
     julianMonth++;
   }
   else {
     julianYear--;
     julianMonth += 13;
   }

   double julian = (java.lang.Math.floor(365.25 * julianYear)
        + java.lang.Math.floor(30.6001*julianMonth) + day + 1720995.0);
   if (day + 31 * (month + 12 * year) >= JGREG) {
     // change over to Gregorian calendar
     int ja = (int)(0.01 * julianYear);
     julian += 2 - ja + (0.25 * ja);
   }
   julian=Math.floor(julian)+hour/24.;
   
   return julian;
 }

 /**
 * Converts a Julian day to a calendar date
 * ref :
 * Numerical Recipes in C, 2nd ed., Cambridge University Press 1992
 */
 public static int[] fromJulian(double injulian) {
   int jalpha,ja,jb,jc,jd,je,year,month,day,hour=0;
   double hf=injulian-Math.round(injulian);
   hour=(int)Math.round(24*hf);
   double julian = injulian + HALFSECOND / 86400.0;
   ja = (int) julian;
   if (ja>= JGREG) {
     jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
     ja = ja + 1 + jalpha - jalpha / 4;
   }

   jb = ja + 1524;
   jc = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
   jd = 365 * jc + jc / 4;
   je = (int) ((jb - jd) / 30.6001);
   day = jb - jd - (int) (30.6001 * je);
   month = je - 1;
   if (month > 12) month = month - 12;
   year = jc - 4715;
   if (month > 2) year--;
   if (year <= 0) year--;

   return new int[] {year, month, day,hour};
  }

  public static void main(String args[]) {
   // FIRST TEST reference point
   System.out.println("Julian date for May 23, 1968 : "
      + toJulian( new int[] {1968, 5, 23 } ));
   // output : 2440000
   int results[] = fromJulian(toJulian(new int[] {1968, 5, 23 }));
   System.out.println
     ("... back to calendar : " + results[0] + " "
       + results[1] + " " + results[2]);

   // SECOND TEST today
   Calendar today = Calendar.getInstance();
   double todayJulian = toJulian
      (new int[]{today.get(Calendar.YEAR), today.get(Calendar.MONTH)+1,
       today.get(Calendar.DATE)});
   System.out.println("Julian date for today : " + todayJulian);
   results = fromJulian(todayJulian);
   System.out.println
      ("... back to calendar : " + results[0] + " " + results[1]
       + " " + results[2]);

   // THIRD TEST
   double date1 = toJulian(new int[]{2005,1,1});
   double date2 = toJulian(new int[]{2005,1,31});
   System.out.println("Between 2005-01-01 and 2005-01-31 : "
      + (date2 - date1) + " days");

   /*
      expected output :
         Julian date for May 23, 1968 : 2440000.0
         ... back to calendar 1968 5 23
         Julian date for today : 2453487.0
         ... back to calendar 2005 4 26
         Between 2005-01-01 and 2005-01-31 : 30.0 days
   */
   }
}
