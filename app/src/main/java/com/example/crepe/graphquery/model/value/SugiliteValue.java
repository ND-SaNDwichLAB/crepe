package com.example.crepe.graphquery.model.value;

import com.example.crepe.graphquery.SugiliteData;

/**
 * @author toby
 * @date 11/14/18
 * @time 12:53 AM
 */
public interface SugiliteValue<T>  {
    T evaluate(SugiliteData sugiliteData);
    String getReadableDescription();

}
