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
package dulab.adap.workflow;

import dulab.adap.common.algorithms.machineleanring.Optimization;
import dulab.adap.common.types.MutableDouble;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.ReferenceComponent;
import dulab.adap.datamodel.Sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.exception.TooManyIterationsException;

/**
 * This class calculates similarity between two components by the formula
 * 
 *    score = weight * eicScore + (1 - weight) * spectrumScore
 * 
 * where eicScore is the maximum convolution of their chromatograms,
 *       spectrumScore is the discrete product of their spectra
 * 
 * @author aleksandrsmirnov
 */

public class Alignment {
    
    private static double processedPercent = 0.0;
    
    /**
     * This method performs time-shift optimization and calculates similarity 
     * as a linear combination of eicScore and spectrumScore
     * 
     * @param c1 First Component to compare
     * 
     * @param c2 Second Component to compare
     * 
     * @param shift initial value of the time-shift
     * 
     * @param params alignment parameters
     * 
     * @return a number from 0 (low similarity) to 1 (high similarity)
     */
    
    private static double getScore(final Component c1, final Component c2,
            MutableDouble shift, final AlignmentParameters params)
    {
        // Calculate EIC-similarity
        
        double eicScore = 0.0;
        
        switch (params.eicScore)
        {
            case AlignmentParameters.CROSS_CORRELATION:
                
                NavigableMap <Double, Double> chromatogram1 =
                        dulab.adap.common.algorithms.Math.scale(c1.getChromtogram(), 
                                1.0 / c1.getChromatogramNorm());

                NavigableMap <Double, Double> chromatogram2 =
                        dulab.adap.common.algorithms.Math.scale(c2.getChromtogram(), 
                                1.0 / c2.getChromatogramNorm());

                eicScore = Optimization.alignSignals(
                        chromatogram1, chromatogram2, shift, 
                        params.maxShift, params.optimizationParameters);
                
                break;
            
            default: // AlignmentParameters.RT_DIFFERENCE
                eicScore = 1 - Math.abs(c1.getRetTime() - c2.getRetTime()) / params.retTimeRange;
        }

//        double eicScore = 1 - Math.abs(c1.getRetTime() - c2.getRetTime()) / params.retTimeRange;
        
        // Calculate Spectrum-similarity
        
        NavigableMap <Double, Double> spectrum1 =
                dulab.adap.common.algorithms.Math.scale(c1.getSpectrum(), 
                        1.0 / c1.getSpectrumNorm());
        
        NavigableMap <Double, Double> spectrum2 =
                dulab.adap.common.algorithms.Math.scale(c2.getSpectrum(), 
                        1.0 / c2.getSpectrumNorm());
        
//        double spectrumScore = new PurityScore().call(
//                new SparseVector(spectrum1), 
//                new SparseVector(spectrum2));
//        spectrumScore = Math.cos(Math.PI * spectrumScore / 2);
        
        double spectrumScore = dulab.adap.common.algorithms.Math.discrete_dot_product(
                spectrum1, spectrum2, 0.1);
        
        final double p = params.scoreWeight;
        final double q = 1.0 - p;
        
        return p * eicScore + q * spectrumScore;
    }
    
    public static double getProcessedPercent() {
        return processedPercent;
    }
    
    /**
     * This method performs alignment of peaks in several samples
     * 
     * @param params Alignment parameters (see AlignmentParameters class for 
     *               more information)
     * 
     * @param samples Sample-objects to be aligned
     * 
     * @return a list of referenceComponent-objects representing aligned peaks
     */
    
    public static List <ReferenceComponent> run(
            final AlignmentParameters params,
            final List <Sample> samples)
    {
        List <ReferenceComponent> result = new ArrayList <> ();
        
        final int sampleCount = samples.size();
        if (sampleCount == 0) return result;
        
        // Find number of samples per tag
        Map <String, Integer> samplePerTagCount = new HashMap <> ();
        
        for (final Sample s : samples) {
            String tag = s.getTag();
            samplePerTagCount.put(tag, 
                    1 + samplePerTagCount.getOrDefault(tag, 0));
        }
        
        // Create a long list of all components
        
        List <Component> allComponents = new ArrayList();
        for (final Sample s : samples) {
            List <Component> components = s.getComponents();
            
            // Sort components by retention time
            Collections.sort(components, (c1, c2) ->
                    Double.compare(c1.getRetTime(), c2.getRetTime()));
                    //c1.getRetTime() > c2.getRetTime() ? 1 : -1);
                    //(int) (c1.getRetTime() - c2.getRetTime()));
            
            // Add all components to allComponents
            allComponents.addAll(s.getComponents());
        }
        
        // Sort allComponents by intensity in descending order
        Collections.sort(allComponents, (c1, c2) ->
                -Double.compare(c1.getIntensity(), c2.getIntensity()));
                //c2.getIntensity() > c1.getIntensity() ? 1 : -1);
                //(int) (c2.getIntensity() - c1.getIntensity()));
        
        final double processedStep = (double) samples.size() / allComponents.size();
        processedPercent = 0.0;
        
        List <Component> chosenComponents = new ArrayList <> (sampleCount);
        
        // Loop over allComponents
        for (final Component component : allComponents) {
            
            if (component.getAlignedStatus()) continue;
            
            System.out.println(component.toString());
            
            // ------------------------------------------------
            // Find components within retTimeRange and mzRange,
            // that are not aligned yet
            // ------------------------------------------------
            
            findSimilarComponents(component, samples, chosenComponents, 
                    new ArrayList <> (), params);
            
            // Check the number of similar components per tag.
            // Stop if there is no enough components
            
            Map <String, Integer> componentCount = new HashMap <> ();
            for (int j = 0; j < sampleCount; ++j) {
                final String tag = samples.get(j).getTag();
                if (chosenComponents.get(j) != null)
                    componentCount.put(tag, 
                            1 + componentCount.getOrDefault(tag, 0));
            }
            
            boolean isContinue = false;
            for (Map.Entry <String, Integer> entry : componentCount.entrySet()) 
            {
                final String tag = entry.getKey();
                int number = entry.getValue();
                double ratio = 1.0 * number / samplePerTagCount.get(tag);
                
                if (ratio > params.sampleCountRatio) {
                    isContinue = true;
                    break;
                }
            }
            
            if (!isContinue) continue;
            
            
            
            // Phase 2. For each chosen component, calculate the average
            //          similarity to all other chosen components
            
            Component bestComponent = null;
            double bestScore = 0.0;
            int bestSampleID = 0;
            
            List <Component> secondPhaseChosenComponents = 
                    new ArrayList <> (sampleCount);
            
            List <Component> bestChosenComponents = 
                    new ArrayList <> (sampleCount);
            
            List <Double> bestShifts = new ArrayList <> ();
            List <Double> shifts = new ArrayList <> ();
            
            for (int i = 0; i < sampleCount; ++i)
            {
                Component refComponent = chosenComponents.get(i);
                
                if (refComponent == null) continue;
                
                shifts.clear();
                
                double score = findSimilarComponents(refComponent, samples, 
                        secondPhaseChosenComponents, shifts, params);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestComponent = refComponent;
                    bestSampleID = samples.get(i).getID();
                    bestChosenComponents.clear();
                    bestChosenComponents.addAll(secondPhaseChosenComponents);
                    bestShifts.clear();
                    bestShifts.addAll(shifts);
                }
            }
            
            if (bestComponent == null) continue; // best component wasn't found
            
            // -----------------------------------------------
            // Apply shifts and set up the reference component
            // -----------------------------------------------
            
            bestComponent.setAsReference();
            ReferenceComponent refComponent = 
                    new ReferenceComponent(bestComponent, bestScore);
            refComponent.setSampleID(bestSampleID);
            
            for (int j = 0; j < sampleCount; ++j) {
                Component chosenComponent = bestChosenComponents.get(j);
                if (chosenComponent == null) continue; // skip null-values
                
                if (chosenComponent != bestComponent) {
                    chosenComponent.setShift(bestShifts.get(j));
                    refComponent.addComponent(chosenComponent, 
                            samples.get(j).getID());
                } else {
                    refComponent.setSampleID(samples.get(j).getID());
                    refComponent.addComponent(chosenComponent, 
                            samples.get(j).getID());
                }
            }
            
            result.add(refComponent);
            
            processedPercent += processedStep;
        }
        
        return result;
    }
 
    
    private static double findSimilarComponents(final Component refComponent,
            final List <Sample> samples, 
            final List <Component> chosenComponents,
            final List <Double> shifts,
            final AlignmentParameters params)
    {
        final int sampleCount = samples.size();
        
        // double mz = refComponent.getMZ();
        double retTime = refComponent.getRetTime();

        List <List <Component>> closeComponents = new ArrayList <> (sampleCount);

        for (final Sample s : samples) {
            List <Component> closeComponentsInSample = new ArrayList <> ();

            for (final Component c : s.getComponents()) {
                if (c.getRetTime() > retTime + params.retTimeRange) break;

                if (c.getRetTime() > retTime - params.retTimeRange
                        && !c.getAlignedStatus())
                {
                    closeComponentsInSample.add(c);
                }
            }

            closeComponents.add(closeComponentsInSample);
        }

        
        // ----------------------------------------------------------
        // In each sample, find the component that is most similar to 
        // refComponent
        // ----------------------------------------------------------

        chosenComponents.clear();
        shifts.clear();
        double totalScore = 0.0;

        for (List <Component> components : closeComponents) 
        {
            double bestScore = 0.0;
            double bestShift = 0.0;
            Component chosenComponent = null;

            for (final Component c : components)
            {    
                if (c.getAlignedStatus()) continue;
                
                MutableDouble shift = new MutableDouble(
                        refComponent.getRetTime() - c.getRetTime());
                double score;

                try {
                    score = getScore(refComponent, c, shift, params);
                }
                catch (OutOfRangeException | TooManyIterationsException e) {
                    continue;
                }

                if (score > params.scoreTolerance && score > bestScore) {
                    bestScore = score;
                    bestShift = shift.get();
                    chosenComponent = c;
                }
            }
            
            chosenComponents.add(chosenComponent);
            shifts.add(bestShift);
            totalScore += bestScore;
        }
        
        return totalScore / sampleCount;
    }
}
