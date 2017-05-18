/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package dulab.adap.datamodel;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Comparator;

/**
 * Structure PeakInfo contains all information about a peak
 * 
 * @author aleksandrsmirnov
 */
public class PeakInfo 
        implements Comparator<PeakInfo>, Comparable<PeakInfo>, Serializable 
{
    private static final DecimalFormat DECIMAL = new DecimalFormat("#.00");
    
    public double retTime;
    public double mzValue;
    public double intensity; // Intensity
    public double retTimeStart;
    public double retTimeEnd;

    public int peakID;
    public int peakIndex; // pkInd
    public int leftApexIndex; // LBound
    public int rightApexIndex; //RBound
    public int leftPeakIndex; // lboundInd
    public int rightPeakIndex; // rboundInd
//    public int offset;
//    public boolean isShared;
    public double signalToNoiseRatio;
    public double coeffOverArea;
    
    // ------------------------------------------------------------------------
    // ----- Construtors ------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public PeakInfo() {}
    
    public PeakInfo(final int peakIndex) {
        this.peakIndex = peakIndex;
    }
    
    public PeakInfo(final PeakInfo info) {
        retTime = info.retTime;
        mzValue = info.mzValue;
        intensity = info.intensity;
        retTimeStart = info.retTimeStart;
        retTimeEnd = info.retTimeEnd;
        
        peakID = info.peakID;
        peakIndex = info.peakIndex;
        leftApexIndex = info.leftApexIndex;
        rightApexIndex = info.rightApexIndex;
        leftPeakIndex = info.leftPeakIndex;
        rightPeakIndex = info.rightPeakIndex;
//        offset = info.offset;
//        isShared = info.isShared;
        signalToNoiseRatio = info.signalToNoiseRatio;
//        sharpness = info.sharpness;
//        coeffOverArea = info.coeffOverArea;
    }
    
    // ------------------------------------------------------------------------
    // ----- Properties -------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public PeakInfo mzValue(final double mz) {
        this.mzValue = mz;
        return this;
    }
    
    public PeakInfo peakID(final int id) {
        this.peakID = id;
        return this;
    }
    
    // ------------------------------------------------------------------------
    // ----- Methods ----------------------------------------------------------
    // ------------------------------------------------------------------------
    
    @Override
    public int compare(final PeakInfo info1, final PeakInfo info2) {
        if (info1.peakIndex < info2.peakIndex)
            return -1;
        else if (info1.peakIndex == info2.peakIndex)
            return 0;
        return 1;
    }
    
    @Override
    public int compareTo(final PeakInfo info) {
        if (this.peakIndex < info.peakIndex)
            return -1;
        else if (this.peakIndex == info.peakIndex)
            return 0;
        return 1;
    }
    
    public static PeakInfo merge(final PeakInfo info1, final PeakInfo info2)
    {
        if (info1.mzValue != info2.mzValue)
            throw new IllegalArgumentException("Cannot merge PeakInfo with different m/z-values");
        
        PeakInfo result = new PeakInfo();
        
        result.mzValue = info1.mzValue;
        
        if (info1.intensity > info2.intensity) {
            result.intensity = info1.intensity;
            result.peakIndex = info1.peakIndex;
        }
        else {
            result.intensity = info2.intensity;
            result.peakIndex = info2.peakIndex;
        }
        
        result.leftApexIndex = 
                Integer.min(info1.leftApexIndex, info2.leftApexIndex);
        result.leftPeakIndex = 
                Integer.min(info1.leftPeakIndex, info2.leftPeakIndex);
        
        result.rightApexIndex =
                Integer.max(info1.rightApexIndex, info2.rightApexIndex);
        result.rightPeakIndex =
                Integer.max(info1.rightPeakIndex, info2.rightPeakIndex);
        
        return result;
    }
    
    @Override
    public String toString() {
        return "m/z " + DECIMAL.format(mzValue) 
                + " @ " + DECIMAL.format(retTime) + " min.";
    }
}
