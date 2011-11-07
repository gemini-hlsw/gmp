package edu.gemini.epics.api;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import org.apache.commons.lang.NotImplementedException;

import javax.transaction.NotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class DbrUtil
 *
 * @author Nicolas A. Barriga
 *         Date: 11/7/11
 */
public class DbrUtil {
    /**
     * Returns the values of the provided DBR as a list of Objects of the correct type.
     *
     * @param dbr
     * @return
     */
    public static List<?> extractValues(DBR dbr) {
        DBRType type = dbr.getType();
        if (type.isDOUBLE()) {
            List<Double> values = new ArrayList<Double>();
            Object objVal = dbr.getValue();
            double[] val = (double[]) objVal;
            for (double a : val) {
                values.add(a);
            }
            return values;
        } else if (type.isINT()) {
            List<Integer> values = new ArrayList<Integer>();
            Object objVal = dbr.getValue();
            int[] val = (int[]) objVal;
            for (int a : val) {
                values.add(a);
            }
            return values;
        } else if (type.isSTRING()) {
            List<String> values = new ArrayList<String>();
            Object objVal = dbr.getValue();
            String[] val = (String[]) objVal;
            Collections.addAll(values, val);
            return values;
        } else if (type.isFLOAT()) {
            List<Float> values = new ArrayList<Float>();
            Object objVal = dbr.getValue();
            float[] val = (float[]) objVal;
            for (float a : val) {
                values.add(a);
            }
            return values;
        } else if (type.isENUM()) {
            throw new NotImplementedException("Enum types are not supported by this class");
        } else {
            throw new NotImplementedException("Type " + dbr.getType() + " is not supported by this class");
        }
    }

}
