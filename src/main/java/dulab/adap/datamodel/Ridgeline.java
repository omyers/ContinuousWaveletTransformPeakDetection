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

import java.util.ArrayList;

/**
 *
 * @author owen myers
 */
public class Ridgeline {
    public int totalNumberOfScales;
    
    // These are public for priting reasons and debuging. When everything is
    // done they can be made private.
    public ArrayList<Double> scales_ = new ArrayList<Double>();
    public ArrayList<Integer> indecies_ = new ArrayList<Integer>();
    
    // Correlation values
    private ArrayList<Double> corValues_ = new ArrayList<Double>();
    private int curRunningGap_;
    
    public Ridgeline(double firstScale,
                     int firstIndex,
                     double corValue,
                     int NScales){
        scales_.add(firstScale);
        indecies_.add(firstIndex);
        corValues_.add(corValue);
        curRunningGap_ = 0;
        totalNumberOfScales = NScales;
    }
    public int getRunningGapNum(){
        return curRunningGap_;
    }
    public int getRidgeLength(){
        return scales_.size();
    }
    public double getRidgeStartScale(){
        return scales_.get(0);
    }
    public double getRidgeEndScale(){
        int l = scales_.size();
        return scales_.get(l-1);
    }
    public int getBestIndex(){
        int curBestInd=-1;
        double maxCorVal=Double.NEGATIVE_INFINITY;
        for (int i=0; i< indecies_.size();i++){
            double curCor = corValues_.get(i);
            if (curCor>maxCorVal){
                maxCorVal = curCor;
                curBestInd = indecies_.get(i);
            }
        }
        return curBestInd;
    }
    public double getBestScale(){
        double curBestScale=-1.0;
        double maxCorVal=Double.NEGATIVE_INFINITY;
        for (int i=0; i< scales_.size();i++){
            double curCor = corValues_.get(i);
            if (curCor>maxCorVal){
                maxCorVal = curCor;
                curBestScale = scales_.get(i);
            }
        }
        return curBestScale;
    }
    public double getMaxCor(){
        double maxCorVal=0.0;
        for (int i=0; i< corValues_.size();i++){
            double curCor = corValues_.get(i);
            if (curCor>maxCorVal){
                maxCorVal = curCor;
            }
        }
        return maxCorVal;
    }
    public boolean tryAddPoint(double scale, int index, double corValue)
    {
        // see if where this index is in relation to the last added
        int lastAddedInd = indecies_.get(indecies_.size()-1);
        int indexDiff = Math.abs(lastAddedInd - index);

        int indexTol = (int) Math.round(findIndexTolFromScale(scale));



        // Need to see if something has already been added for this scale
        boolean haveThisScaleAlready = false;
        double epsilon = 0.000000001;
        if ((scales_.get(scales_.size()-1)<=(scale+epsilon))&&(scales_.get(scales_.size()-1)>=(scale-epsilon))){
            haveThisScaleAlready = true;
        }
        if (!haveThisScaleAlready ){

            // times 2 for pluss minus tollerance
            if (indexDiff<(2*indexTol)){

                scales_.add(scale);
                indecies_.add(index);
                corValues_.add(corValue);
                curRunningGap_ = 0;
                return true;
            }
            else{
                curRunningGap_++;
                return false;
            }

        }
        else{
            // two things to check 
            // 1) is it closer in endex to previous?
            // 2) is it larger or smaller correlation value
            // For now lets just take the closest point unless this the first scale still.
            // If it is the first scale then lets pick the largest value.
            if (scales_.size() > 1){
                //Lets try taking the largest one instead
                //int prevCor = corValues_[corValues_.size()-1];
                //int curCor = corValue;


                //if (curCor>prevCor){
                //    indecies_[indecies_.size()-1]=index;
                //    corValues_[indecies_.size()-1]=corValue;
                //    return true;
                //}


                int prevIndexDiff = Math.abs(indecies_.get(indecies_.size()-2)-indecies_.get(indecies_.size()-1));
                int curIndexDiff = Math.abs(indecies_.get(indecies_.size()-2)-index);

                if (prevIndexDiff>curIndexDiff ){
                    indecies_.set(indecies_.size()-1,index);
                    corValues_.set(indecies_.size()-1,corValue);
                    return true;
                }
            }
            else {
                // only compare magnitued if they are close points
                if (indexDiff<(2*indexTol)){
                    double prevCor = corValues_.get(0);
                    if (corValue>prevCor){
                        indecies_.set(indecies_.size()-1,index);
                        corValues_.set(indecies_.size()-1,corValue);
                        return true;
                    }
                }
            }
        }

        return false;
    }
    
    public double findIndexTolFromScale(double scale)
    {
        // This is probably too simple but it apears to work well enough. Use for now ans then look into
        // correct value

        // window size for scale = 1 is [-5,5]
        //return scale*5;
        //
        //I think above is much to big. Going to try this
        return scale;
        //return 2;
    }
        
    
    
    
}
