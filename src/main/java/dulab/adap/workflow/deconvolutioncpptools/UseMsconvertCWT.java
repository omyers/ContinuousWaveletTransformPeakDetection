/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dulab.adap.workflow.deconvolutioncpptools;
import dulab.adap.workflow.deconvolutioncpptools.NativeLoader;


/**
 *
 * @author owenmyers
 */
public class UseMsconvertCWT {
    static {
        //ystem.load("/Users/owenmyers/Google Drive/Xiuxia/CppWavelet/libadapwavelet.so");
        //System.load("/Users/owenmyers/Google Drive/Xiuxia/XCMSMZminProject/MZminStuff/FinalOurPeakPicking/peak_picking/owen-mod-mzmine2-master/target/classes/lib/macosx-x86_64/libadapwavelet.so");
        NativeLoader loader = new NativeLoader();
        loader.loadLibrary("adapwavelet");
        
        //System.load("/Users/owenmyers/Google Drive/Xiuxia/XCMSMZminProject/MZminStuff/FinalOurPeakPicking/peak_picking/adap-cpp-deconvolution/libadapwavelet.so");
    }
    
    // Too hard to return 2D array. Going to return both arrays as one. since there will not be more peaks than there are
    // data points in the intesity array, the left bound array will be the first N points of teh returned array 
    // and the right bound array will be the next N points where N is the length of intesities array.
    private native double[] findPeaks(double [] intensities, double [] scanTimes, int lengthArrays, double SNR, double RTtol);
    
    public static double[][] tryCallingCppFindPeaks(double [] intensities, double [] scanTimes, double SNR, double RTtol){
        int numPts = scanTimes.length;
        double [][] peakBounds = new double[3][numPts];
        

        double[] peakBounds1D = new UseMsconvertCWT().findPeaks(intensities, scanTimes, numPts,  SNR, RTtol);
        for (int i=0; i<numPts; i++){
            peakBounds[0][i]= peakBounds1D[i];
        }
        for (int i=numPts; i<(2*numPts); i++){
            peakBounds[1][i-numPts]= peakBounds1D[i];
        }
        for (int i=(2*numPts); i<(3*numPts); i++){
            peakBounds[2][i-2*numPts]= peakBounds1D[i];
        }
        return peakBounds;
    }
}
