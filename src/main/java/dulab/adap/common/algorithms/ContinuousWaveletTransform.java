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
package dulab.adap.common.algorithms;

import dulab.adap.datamodel.Ridgeline;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.fill;
import java.util.HashMap;
import java.lang.String;



/**
 *
 * @author owen myers
 */
public class ContinuousWaveletTransform {
    // all scales will mbe measured in units of scans (indecies)
    private double smallScale;
    private double largeScale;
    private double incrementScale;
    private ArrayList<Double> arrScales = new ArrayList<Double>();
    private HashMap<Double,Integer> mapScaleToIndex = new HashMap<Double,Integer>();
    private String waveletType;
    private double[] signal;
    private double[] x;
    private double avgXSpace;
    private double[][] allCoefficients;
    private ArrayList<Ridgeline> ridgeLineArr = new ArrayList<Ridgeline>();
    
    
 
    
    // how far in each direction from the current point do we need to grab data for a succesful wavelet transform?
    // This number is the factor we multiply by the scale. 5 should be good because this is the estimated compact support
    int scaleCoefHowFarOut = 5;
    
    public double[][] returnAllCoefficients(){
        return allCoefficients;
    }
    
    public ContinuousWaveletTransform(double smallScaleIn,
                                        double largeScaleIn,
                                        double incrementScaleIn){
        smallScale = smallScaleIn;
        largeScale = largeScaleIn;
        incrementScale = incrementScaleIn;
        
        int index = 0;
        for (double curScale=smallScale; curScale<=largeScale; curScale+=incrementScale){
            arrScales.add(curScale);
            mapScaleToIndex.put(curScale,index);
            index += 1;
        }
        
    
    }
    // returns two arrays, one of the lower bounds of the peaks and one of the upper bounds of the peaks.
    public double[][] findBoundries(){
        double [][] boundsAndBestCoef = new double[3][];
        for (int i=0; i<3;i++){
            boundsAndBestCoef[i]= new double[x.length];
            fill(boundsAndBestCoef[i],0.0);
        }
       
        
        int count = 0;
        for (Ridgeline curRL : ridgeLineArr){
            int bestIndex = curRL.getBestIndex();
            // this is the actuale scale, not the index of the best scale.
            double bestScale = curRL.getBestScale();
            double bestCoefficient = curRL.getMaxCor();
            
            int curRightBound = bestIndex+(int) Math.round(bestScale);
            if (curRightBound>=x.length){
                curRightBound = x.length-1;
            }
            int curLeftBound = bestIndex-(int) Math.round(bestScale);
            if (curLeftBound<0){
                curLeftBound = 0;
            }
            
            boundsAndBestCoef[0][count] = curLeftBound;
            boundsAndBestCoef[1][count] = curRightBound;
            boundsAndBestCoef[2][count] = bestCoefficient;
            
            count+=1;
        }
        return boundsAndBestCoef;
    }
    
    public void filterRidgelines(){
        ArrayList<Ridgeline> filteredRidgelines = new ArrayList<Ridgeline>();
        
        for (int i=0; i<ridgeLineArr.size(); i++){
            Ridgeline curRL = ridgeLineArr.get(i);
            int ridgeLength = curRL.getRidgeLength();
            int NScales = curRL.totalNumberOfScales;

            // When we make this CWT more general this check should be in terms of some precentage of the total number of scales.
            // Unless you are always dividing the scale range by 10.
            if (ridgeLength<(NScales-3)){
                continue;
            }
            filteredRidgelines.add(curRL);
            
        }
        ridgeLineArr = filteredRidgelines;


    }
    
    public void buildRidgelines(){
        getCoefficientsForAllScales();


        // start from the largest scale and go to the smallest
        for (int i=arrScales.size()-1; i>=0; i--){
            double curScale = arrScales.get(i);
            int indexOfThisWaveletScale = mapScaleToIndex.get(curScale);
            double[] curCoefficients = allCoefficients[indexOfThisWaveletScale];
            
            
            Integer[] thisScaleBestMaxima = findMaximaForThisScale(curScale);
            
            
            for (int j=0; j<thisScaleBestMaxima.length;j++){
                boolean wasMatched = false;
                int curBestMaxLoc = thisScaleBestMaxima[j];
                for (int alpha=0; alpha<ridgeLineArr.size(); alpha++){
                    
                    boolean wasAdded = ridgeLineArr.get(alpha).tryAddPoint(
                                        curScale,
                                        curBestMaxLoc,
                                        curCoefficients[thisScaleBestMaxima[j]]);
                    
                    if (wasAdded) {wasMatched=true;}
                    
                }
                // if it was not added to at least one then make a new redge line
                if (!wasMatched){
                    
                    Ridgeline curStartRidge = new Ridgeline(curScale,
                                              thisScaleBestMaxima[j],
                                              curCoefficients[thisScaleBestMaxima[j]],
                                              arrScales.size());
                                              
                    ridgeLineArr.add(curStartRidge);
                }
            }   
        }
        writeRidgelines();
    }
    // returns the indecies of the location of the maxima
    public Integer[] findMaximaForThisScale(double waveletScale){
        //when we are removing points adjacent to the current maxima this is the number of points to go in either direction before stopping.
        int removeCutOff = (int) Math.round(waveletScale*2.5);
        
        ArrayList<Integer> maximaLocations = new ArrayList<Integer>();
        
        // sort and keep track of the original idecies
        
        int indexOfThisWaveletScale = mapScaleToIndex.get(waveletScale);
        double[] curCoefficients = allCoefficients[indexOfThisWaveletScale];
       
        SortAndKeepOriginalIndecies comparator = new SortAndKeepOriginalIndecies(curCoefficients);
        Integer[] indecies = comparator.makeArrOfIndecies();
        Arrays.sort(indecies,comparator);
        
        HashMap<Integer,Boolean> mapIndexToBoolRemain = new HashMap<Integer,Boolean>();
        for (int i = 0; i<indecies.length; i++){
            mapIndexToBoolRemain.put(indecies[i],true);
        }
        for (int i = indecies.length-1; i>=0; i--){
            if (mapIndexToBoolRemain.get(indecies[i])){
                int curLargestIndex = indecies[i];
                maximaLocations.add(curLargestIndex);
                // remove points. num points to right and left equal to current scale
                mapIndexToBoolRemain.put(curLargestIndex,false);
                for (int j=1; j<removeCutOff; j++){
                  
                    int curRemoveIndexRight = curLargestIndex+j;
                    int curRemoveIndexLeft = curLargestIndex-j;
                    if(curRemoveIndexLeft>=0){
                        mapIndexToBoolRemain.put(curRemoveIndexLeft,false);
                    }
                    if(curRemoveIndexRight<x.length){
                        mapIndexToBoolRemain.put(curRemoveIndexRight,false);
                    }
                }
                
            }
        }
        Integer[] toReturn = maximaLocations.toArray(new Integer[maximaLocations.size()]);
        //writeMaximaLocations(toReturn);
        
        return toReturn;
        
    }
    
    public void getCoefficientsForAllScales(){
        int NScales = arrScales.size();
        allCoefficients = new double[NScales][];
        int count = 0;
        for (Double curScale: arrScales){
            allCoefficients[count] = getCoefficientsForThisScale((double) curScale);
            count+= 1;
        }
        writeAllCoeffs();
    }
    
    public double[] getCoefficientsForThisScale(double waveletScale){
        double[] coefficientsForThisScale = new double[x.length];
        for (int i=0; i<x.length; i++){
            
            double currentCoefficient = signalWaveletInnerProductOnePoint(i,waveletScale);
            coefficientsForThisScale[i] = currentCoefficient;
            
        }
        return coefficientsForThisScale;
    }
    
    
    // This cfunciton needs to be carful with two things: 1) the boundries of the signal
    // were it needs to either pad or pretend to pad values below off the boundry. 2) The location
    // of the wavelet has to be set correctly.
    // Note: waveletScale is in units of indecies NOT rt or anything else
    public double signalWaveletInnerProductOnePoint(int xIndexOfWaveletMax, double waveletScale){
        
        int leftBoundIntegrate = (int) Math.round(xIndexOfWaveletMax-scaleCoefHowFarOut*waveletScale-1.0);
        int rightBoundIntegrate = (int) Math.round(xIndexOfWaveletMax+scaleCoefHowFarOut*waveletScale+1.0);
        if (leftBoundIntegrate<0){
            leftBoundIntegrate=0;
        }
        if (rightBoundIntegrate>=x.length){
            rightBoundIntegrate=x.length-1;
        }
        
        double[] curX = new double[rightBoundIntegrate-leftBoundIntegrate +1];
        double[] curY = new double[rightBoundIntegrate-leftBoundIntegrate +1];
        double[] waveletY = new double[rightBoundIntegrate-leftBoundIntegrate +1];
        
        int curIndex = 0;
        for (int i=leftBoundIntegrate;i<=rightBoundIntegrate; i++){
            curX[curIndex] = x[i];
            curY[curIndex] = signal[i];
            // for the wavelt work in units of indecies because wavelt for  numbers smaller than one is not approriate.
            waveletY[curIndex] = rickerWavelet(x[i]-x[xIndexOfWaveletMax], (double) waveletScale);
            curIndex+=1;
        }
//        double[] doublePtsCurX = doubleTheNumberOfPtsX(curX);
//        double[] doublePtsDataY = doubleTheNumberOfPtsDataY(curY);
//        double[] doublePtsWavelet = doubleTheNumberOfPtsWavelet(waveletY, (double) waveletScale);
        
        //writeDataAndWavelet(curX,curY,waveletY);
        
        double innerProd = innerProduct(curX,curY,waveletY);
        return innerProd;
    }
    
    // This just takes an x value and the parameters of the wavelet and retuns the y value for that x
    public double rickerWavelet(double x,double scalParam){
        scalParam = scalParam*avgXSpace;
        double A = 2.0/Math.sqrt(3.0 * scalParam*Math.sqrt(Math.PI)) * (1.0-Math.pow(x, 2.0)/Math.pow(scalParam, 2.0));
        return Math.exp(-Math.pow(x, 2.0)/(2.0*Math.pow(scalParam, 2)))*A;
    }
    
    // This function can only take two arrays of equivelent length.
    // in the msconvert code they just add the wavelet * the intensity... Lets just do this 
    // for now to see if we can get the same results.
    public double innerProduct(double[] x, double[] arr1, double[] arr2){
        int l = arr1.length;
        double[] multArr = new double[arr1.length];
        for (int i=0; i < l; i++){
            multArr[i]= arr1[i]*arr2[i];
        }
        double sum = 0.0;
        for (int i=0; i < l; i++){
            sum+=multArr[i];
        }
        return sum;
//        // Because EICs can be messy best to just use trapazoidal rule
//        double area = 0.0;
//        for (int i=0; i < l-1; i++){
//            double curXSpace = x[i+1]-x[i];
//            // lowest height of the two adjacent points
//            double curYLow = Math.min(multArr[i+1],multArr[i]);
//            double curYHigh= Math.max(multArr[i+1],multArr[i]);
//            double triangleArea = 0.5*curXSpace*(curYHigh-curYLow);
//            // triangle area needs to be set as negative if the points are below zero
//            if (){
//                
//            }
//            double rectangleArea = curXSpace*curYLow;
//            
//        }
    }

    
    public void setSignal(double[] signalIn){
        signal = signalIn;
    }
    public void setX(double[] xIn){
        x = xIn;
        double curSumSpacing=0.0;
        for (int i=0; i <xIn.length-1; i++){
            curSumSpacing += xIn[i+1]-xIn[i];
        }
   
        avgXSpace = curSumSpacing/((double) (xIn.length-1));
    }
    
    public double[] doubleTheNumberOfPtsX(double[] xIn)
    {
        double[] xOut = new double[xIn.length*2-1];
        for (int i=1; i<xIn.length-1; i++){
            double addInX = (xIn[i]+xIn[i+1])/2.0;
            xOut[2*i]=xIn[i];
            xOut[2*i+1]=addInX;
        }
        return xOut;
    }
    public double[] doubleTheNumberOfPtsDataY(double[] yIn){
        double[] yOut = new double[yIn.length*2-1];
        for (int i=1; i<yIn.length-1; i++){
            double addInX = (yIn[i]+yIn[i+1])/2.0;
            yOut[2*i]=yIn[i];
            yOut[2*i+1]=addInX;
        }
        return yOut;
    }
//    public double[] doubleTheNumberOfPtsWavelet(double[] waveletY,double[] doublePtsCurX){
//        double[] wavOut = new double[waveletY.length*2-1];
//        for (int i=1; i<waveletY.length-1; i++){
//            double addInX = rickerWavelet(i-xIndexOfWaveletMax, (double) waveletScale);
//            wavOut[2*i]=waveletY[i];
//            wavOut[2*i+1]=addInX;
//        }
//        return wavOut;
//    }
    
//    // Currently this can only be 'ricker'. 
//    public void setWaveletType(String waveletTypeIn){
//        waveletType = waveletTypeIn;
//    }
    private void writeDataAndWavelet(double[] x,double[] y,double[] wav){
        try{
            PrintWriter writer = new PrintWriter("look_at_wavelet_and_data.txt", "UTF-8");
            for(int i = 0; i < x.length; i++){
                writer.println(String.valueOf(x[i])+" "+String.valueOf(y[i])+" "+String.valueOf(wav[i]));
            }
            writer.close();
        } catch (IOException e){
            System.out.println("problem writinglook_at_wavelet_and_data.txt");
        }
    }
    private void writeAllCoeffs(){
        try{
            PrintWriter writer = new PrintWriter("look_at_java_all_coefs.txt", "UTF-8");
            for(int i = 0; i < allCoefficients.length; i++){
                int j;
                for( j = 0; j < allCoefficients[0].length; j++){
                    writer.print(String.valueOf(allCoefficients[i][j])+" ");
                }
                writer.print("\n");
            }
            writer.close();
        } catch (IOException e){
            System.out.println("problem writing look_at_java_all_coefs.txt");
        }
    }
    private void writeMaximaLocations(Integer[] maxIndecies){
        try{
            PrintWriter writer = new PrintWriter("look_at_maxima_positions.txt", "UTF-8");
            for(int i = 0; i < maxIndecies.length; i++){
                writer.print(String.valueOf(maxIndecies[i].toString()+" "));
            }
            writer.close();
        } catch (IOException e){
            System.out.print("problem writing look_at_maxima_positions.txt");
        }
    }
    private void writeRidgelines(){
        //make a blank matrix of zeros and fill with the locations of the ridgelines
        double[][] ridgeArr = new double[arrScales.size()][];
        
        for (int i =0; i<arrScales.size(); i++){
            ridgeArr[i]= new double[x.length];
            fill(ridgeArr[i],0.0);
        }
        
        for(int i = 0; i < ridgeLineArr.size(); i++){
            Ridgeline curRL = ridgeLineArr.get(i);
            for (int j=0; j<curRL.scales_.size();j++){
                double curScale = curRL.scales_.get(j);
                int curIndex = curRL.indecies_.get(j);
                ridgeArr[mapScaleToIndex.get(curScale)][curIndex] = i+100;
            }
        }
        
        try{
            PrintWriter writer = new PrintWriter("look_at_ridgelines.txt", "UTF-8");
            for(int i = 0; i<arrScales.size(); i ++){
                for (int j = 0; j < x.length; j++){
                    writer.print(String.valueOf(ridgeArr[i][j])+" ");
                }
                writer.print("\n");
            }
            writer.close();
        } catch (IOException e){
            System.out.print("problem writing look_at_ridgelines.txt");
        }
    }
}
