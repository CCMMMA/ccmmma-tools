package it.uniparthenope.meteo.radar;

import com.mindprod.ledatastream.LEDataInputStream;

import java.io.IOException;

public class Header {
    private int[] header;
    private int size;


    private double seconds;

    private String dateString;
    private int index;
    private double elevation;
    private double range;
    private double rangeStep;
    private double azimutRes;
    private int encoder;
    private int dataFormat;

    public Header(LEDataInputStream ledis) throws IOException {
        size=ledis.readShort()-1;
        header=new int[size];
        for (int i=0;i<size;i++) {
            header[i] = ledis.readUnsignedShort();
        }

        dateString=String.format("%4d",header[0]);
        dateString+=String.format("%4d",header[1])+"Z";
        dateString+=String.format("%4d",header[2]);
        dateString=dateString.replace(" ","0");

        seconds=header[3]+(header[4]/1000.0);

        index=header[5];
        encoder=header[7];
        elevation=header[10];
        range=header[15]*100;
        rangeStep=header[16];
        azimutRes=header[17]/10;
        dataFormat=header[21];
    }

    public int getDataFormat() { return dataFormat; }
    public double getRange() { return range;}
    public double getAzimutRes() { return azimutRes;}
    public double getRangeStep() { return rangeStep;}
}
