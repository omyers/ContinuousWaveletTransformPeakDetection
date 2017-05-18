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

import java.util.Comparator;

/**
 *
 * @author owenmyers
 */
public class SortAndKeepOriginalIndecies implements Comparator<Integer>{
    private final double[] dataArr;
    public SortAndKeepOriginalIndecies(double[] dataInArr)
    {
        this.dataArr =dataInArr;
    }
    
    public Integer[] makeArrOfIndecies(){
        Integer[] indecies = new Integer[dataArr.length];
        for (int i = 0; i < dataArr.length;i++){
            indecies[i] = i;
        }
        return indecies;
    }
    
    @Override
    public int compare(Integer index1, Integer index2)
    {
        if (dataArr[index2]>dataArr[index1]){
            return -1;
        }
        else if (dataArr[index2]<dataArr[index1]){
            return 1;
        }
        else{
            return 0;
        }
    }
    
    
}
